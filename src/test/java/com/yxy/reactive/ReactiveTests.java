package com.yxy.reactive;

import com.yxy.reactive.model.Person;
import com.yxy.reactive.utils.UUIDUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
class ReactiveTests {

    //Stream应用: 单词转成单词长度输出
    @Test
    void test_00() {
        Stream.of("apple", "banana", "orange", "grape").map(String::length).forEach(System.out::println);
        // 5 6 6 5
    }

    //Stream应用: 过滤单词 转成大写字母 收集成List
    @Test
    void test_01() {
        Stream<String> stream = Stream.of("one", "two", "three", "four")
                .filter(e -> e.length() > 3)
                .peek(e -> System.out.print("Filtered value: " + e + " -> "))
                .map(String::toUpperCase)
                .peek(e -> System.out.println("Upper value: " + e));
        //Filtered value: three -> Upper value: THREE
        //Filtered value: four -> Upper value: FOUR
        System.out.println(stream.collect(Collectors.toList()));
        //[THREE, FOUR]
    }

    //Stream应用: 根据性别聚集person
    @Test
    void test_02_0() {
        ArrayList<Person> people = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Person person = new Person();
            person.setName(UUIDUtil.shortUUID());
            if (i % 2 == 0) {
                person.setSex("Male");
            } else {
                person.setSex("Female");
            }
            people.add(person);
        }
        ConcurrentMap<String, List<Person>> map = people.stream().collect(Collectors.groupingByConcurrent(Person::getSex));
        System.out.println(map);
        //{Male=[{"name":"sdp8agzO","sex":"Male"}, {"name":"pAWPToaZ","sex":"Male"}, {"name":"T4VJA39I","sex":"Male"}, {"name":"gWMSqWoo","sex":"Male"}, {"name":"X9aLWtQC","sex":"Male"}],
        //Female=[{"name":"EvqxL27S","sex":"Female"}, {"name":"av2koMJR","sex":"Female"}, {"name":"yB36ntTU","sex":"Female"}, {"name":"a4Gv0IsI","sex":"Female"}, {"name":"QChi4MV1","sex":"Female"}]}
    }

    //Stream应用: 收集成name-person的map结构
    @Test
    void test_02_1() {
        ArrayList<Person> people = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Person person = new Person();
            person.setName(UUIDUtil.shortUUID());
            if (i % 2 == 0) {
                person.setSex("Male");
            } else {
                person.setSex("Female");
            }
            people.add(person);
        }
        ConcurrentMap<String, Person> concurrentMap = people.stream().collect(Collectors.toConcurrentMap(Person::getName, Function.identity()));
        System.out.println(concurrentMap);
        //{xKHtLJJP={"name":"xKHtLJJP","sex":"Male"}, LWJN94Fc={"name":"LWJN94Fc","sex":"Male"}, ZPonnuX8={"name":"ZPonnuX8","sex":"Female"},
        // 4aFlSc7d={"name":"4aFlSc7d","sex":"Female"}, EFxII4fo={"name":"EFxII4fo","sex":"Male"}, M0aaqtw0={"name":"M0aaqtw0","sex":"Male"},
        // g7gvxOZe={"name":"g7gvxOZe","sex":"Female"}, yE7ftBvP={"name":"yE7ftBvP","sex":"Female"}, HCOtQlG0={"name":"HCOtQlG0","sex":"Male"},
        // bc4k7rhN={"name":"bc4k7rhN","sex":"Female"}}
    }

    //Stream应用: 根据性别聚集 内部继续根据name聚集 理论上可以无限聚集
    @Test
    void test_02_2() {
        ArrayList<Person> people = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Person person = new Person();
            person.setName(String.valueOf(i % 3));
            if (i % 2 == 0) {
                person.setSex("Male");
            } else {
                person.setSex("Female");
            }
            people.add(person);
        }
        ConcurrentMap<String, Map<String, List<Person>>> collect = people.stream().collect(Collectors.groupingByConcurrent(Person::getSex, Collectors.groupingBy(Person::getName)));
        System.out.println(collect);
        //{Male={0=[{"name":"0","sex":"Male"}, {"name":"0","sex":"Male"}],
        // 1=[{"name":"1","sex":"Male"}],
        // 2=[{"name":"2","sex":"Male"}, {"name":"2","sex":"Male"}]},
        // Female={0=[{"name":"0","sex":"Female"}, {"name":"0","sex":"Female"}],
        // 1=[{"name":"1","sex":"Female"}, {"name":"1","sex":"Female"}],
        // 2=[{"name":"2","sex":"Female"}]}}
    }

    @Test
    void test_03_0() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        List.of(strings).stream().map(String::hashCode).collect(StringBuilder::new, new BiConsumer<StringBuilder, Integer>() {
            @Override
            public void accept(StringBuilder stringBuilder, Integer s) {
            }
        }, new BiConsumer<StringBuilder, StringBuilder>() {
            @Override
            public void accept(StringBuilder stringBuilder, StringBuilder stringBuilder2) {
            }
        });
    }

    @Test
    void test_03_1() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        Map<String, Long> map = List.of(strings).stream().collect(Collectors.groupingBy(s -> s, Collectors.mapping(Function.identity(), Collectors.counting())));
    }

    @Test
    void test_03_2() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        HashMap<String, Long> map = List.of(strings).stream().collect(Collectors.groupingBy(s -> s, HashMap::new, Collectors.mapping(Function.identity(), Collectors.counting())));
        System.out.println(map);
        //{a=11, b=2, c=4, d=1, e=11, g=2, h=4, i=7, k=1, l=7, m=6, n=16, o=5, p=2, r=4, s=9, t=12, u=9, v=1, y=2}
    }

    @Test
    void test_04() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("孙悟空", "花果山");
        hashMap.merge("孙悟空", " 水帘洞", String::concat);
        hashMap.merge("猪八戒", "天河", String::concat);
        System.out.println(hashMap);
        //{孙悟空=花果山 水帘洞, 猪八戒=天河}

        HashMap<String, Long> map = new HashMap<>();
        map.put("tom", 1L);
        // map.merge("tom", 13L, Long::sum);
        map.merge("jerry", -6L, Long::sum);
        System.out.println(map);
        //{tom=14, jerry=-6}
    }

    @Test
    void test_05() {

    }

    @Test
    void test_06() {

    }

    @Test
    void test_07() {

    }

    @Test
    void test_08() {

    }

    @Test
    void test_09() {

    }

    @Test
    void test_10() {

    }

    @Test
    void test_11() {

    }

    @Test
    void test_12() {

    }

    @Test
    void test_13() {

    }

    @Test
    void test_14() {

    }

    @Test
    void test_15() {

    }

    @Test
    void test_16() {

    }

    @Test
    void test_17() {

    }

    @Test
    void test_18() {

    }

    @Test
    void test_19() {

    }
}
