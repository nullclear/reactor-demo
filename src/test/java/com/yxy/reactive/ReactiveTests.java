package com.yxy.reactive;

import com.yxy.reactive.model.Person;
import com.yxy.reactive.utils.UUIDUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
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

    //Stream应用: 自定义收集器
    @Test
    void test_03_0() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        String s = List.of(strings).stream().map(String::hashCode)
                .collect(new Supplier<String>() {
                    @Override
                    public String get() {
                        return new String();//提供初始化的值
                    }
                }, new BiConsumer<String, Integer>() {
                    @Override
                    public void accept(String string, Integer i) {
                        //string就是上面get()生成的值 , i就是前面map里的hashcode
                        //注意 这是没有返回值的, 所以必须用 引用类型才行
                    }
                }, new BiConsumer<String, String>() {
                    @Override
                    public void accept(String strings, String string) {
                        //strings就是最后返回的结果
                        //string是前面所有map过后的string
                    }
                });
    }

    //Stream应用: 自定义收集器 最后生成list 源码中四个R代表同一种类型
    @Test
    void test_03_1() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        ArrayList<String> list = List.of(strings).stream().map(String::hashCode)
                .collect(new Supplier<ArrayList<String>>() {
                    @Override
                    public ArrayList<String> get() {
                        return new ArrayList<String>();
                    }
                }, new BiConsumer<ArrayList<String>, Integer>() {
                    @Override
                    public void accept(ArrayList<String> objects, Integer e) {
                        objects.add(String.valueOf(e));
                    }
                }, new BiConsumer<ArrayList<String>, ArrayList<String>>() {
                    @Override
                    public void accept(ArrayList<String> objects, ArrayList<String> c) {
                        objects.addAll(c);//c代表前面的objects
                    }
                });
        System.out.println(list);
        //[116, 104, 105, 115, 105, 109, 112, 108, 101, 109, 101, 110, 116, 97, 116, 105, 111, 110, 97, 115, 115, 117, 109, 101,
        // 115, 116, 104, 97, 116, 116, 104, 101, 99, 111, 110, 99, 117, 114, 114, 101, 110, 116, 109, 97, 112, 99, 97, 110, 110,
        // 111, 116, 99, 111, 110, 116, 97, 105, 110, 110, 117, 108, 108, 118, 97, 108, 117, 101, 115, 97, 110, 100, 114, 101, 116,
        // 117, 114, 110, 105, 110, 103, 110, 117, 108, 108, 117, 110, 97, 109, 98, 105, 103, 117, 111, 117, 115, 108, 121, 109, 101,
        // 97, 110, 115, 116, 104, 101, 107, 101, 121, 105, 115, 97, 98, 115, 101, 110, 116]
    }

    //Stream应用: 按照字符聚集, value收集器默认为list
    @Test
    void test_03_2() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        Map<String, List<String>> map = List.of(strings).stream().collect(Collectors.groupingBy(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s;
            }
        }));
        System.out.println(map);
        //{a=[a, a, a, a, a, a, a, a, a, a, a], b=[b, b], c=[c, c, c, c], d=[d], e=[e, e, e, e, e, e, e, e, e, e, e], g=[g, g], h=[h, h, h, h],
        // i=[i, i, i, i, i, i, i], k=[k], l=[l, l, l, l, l, l, l], m=[m, m, m, m, m, m], n=[n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n],
        // o=[o, o, o, o, o], p=[p, p], r=[r, r, r, r], s=[s, s, s, s, s, s, s, s, s], t=[t, t, t, t, t, t, t, t, t, t, t, t], u=[u, u, u, u, u, u, u, u, u], v=[v], y=[y, y]}
    }

    //Stream应用: 按照字符聚集, value为统计数量, 默认为HashMap
    @Test
    void test_03_3() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        Map<String, Long> map = List.of(strings).stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        //{a=11, b=2, c=4, d=1, e=11, g=2, h=4, i=7, k=1, l=7, m=6, n=16, o=5, p=2, r=4, s=9, t=12, u=9, v=1, y=2}
    }

    //Stream应用: 按照字符聚集, value为统计字符出现次数, 可以改收集器, Map类型
    @Test
//people.stream().collect(groupingBy(Person::getCity, mapping(Person::getLastName, toSet())));
    void test_03_4() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        HashMap<String, Long> map = List.of(strings).stream().collect(Collectors.groupingBy(s -> s, HashMap::new, Collectors.mapping(Function.identity(), Collectors.counting())));
        System.out.println(map);
        //{a=11, b=2, c=4, d=1, e=11, g=2, h=4, i=7, k=1, l=7, m=6, n=16, o=5, p=2, r=4, s=9, t=12, u=9, v=1, y=2}
    }

    //Stream应用: 按照字符聚集, value再收集, 可以改收集器, Map类型
    @Test
    void test_03_5() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        HashMap<String, Set<String>> map = List.of(strings).stream().collect(Collectors.groupingBy(s -> s, HashMap::new, Collectors.mapping(Function.identity(), Collectors.toSet())));
        System.out.println(map);
        //{a=[a], b=[b], c=[c], d=[d], e=[e], g=[g], h=[h], i=[i], k=[k], l=[l], m=[m], n=[n], o=[o], p=[p], r=[r], s=[s], t=[t], u=[u], v=[v], y=[y]}
    }

    //Stream应用: 支流操作 统计 以下四种效果一样
    @Test
    void test_03_6() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        long count = List.of(strings).stream().map(String::hashCode).count();
        long count1 = List.of(strings).stream().map(String::hashCode).map(Function.identity()).count();
        Long count2 = List.of(strings).stream().map(String::hashCode).collect(Collectors.counting());
        Long count3 = List.of(strings).stream().map(String::hashCode).collect(Collectors.mapping(Function.identity(), Collectors.counting()));
    }

    //Stream应用: 总的统计
    @Test
    void test_03_7() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");
        IntSummaryStatistics summaryStatistics = List.of(strings).stream().mapToInt(String::hashCode).summaryStatistics();
        System.out.println(summaryStatistics.getAverage());//108.4396551724138
        System.out.println(summaryStatistics.getMax());//121
        System.out.println(summaryStatistics.getMin());//97
        System.out.println(summaryStatistics.getCount());//116
        System.out.println(summaryStatistics.getSum());//12579
    }

    //Map的merge方法
    @Test
    void test_04_0() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("孙悟空", "花果山");
        hashMap.merge("孙悟空", " 水帘洞", new BiFunction<String, String, String>() {
            @Override
            public String apply(String oldValue, String str) {
                return oldValue.concat(str);
            }
        });
        hashMap.merge("猪八戒", "天河", String::concat);
        System.out.println(hashMap);
        //{孙悟空=花果山 水帘洞, 猪八戒=天河}

        HashMap<String, Long> map = new HashMap<>();
        map.put("tom", 1L);
        // map.merge("tom", 13L, Long::sum);
        map.merge("jerry", -6L, new BiFunction<Long, Long, Long>() {
            @Override
            public Long apply(Long a, Long b) {
                return Long.sum(a, b);
            }
        });
        System.out.println(map);
        //{tom=14, jerry=-6}
    }

    //Map的merge方法统计字符出现的次数
    @Test
    void test_04_1() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");

        HashMap<String, Long> hashMap = new HashMap<>();
        for (String s : strings) {
            hashMap.merge(s, 1L, Long::sum);
        }
        System.out.println(hashMap);
        //{a=11, b=2, c=4, d=1, e=11, g=2, h=4, i=7, k=1, l=7, m=6, n=16, o=5, p=2, r=4, s=9, t=12, u=9, v=1, y=2}
    }

    //Map的merge方法统计字符
    @Test
    void test_04_2() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");

        HashMap<String, ArrayList<String>> hashMap = new HashMap<>();
        for (String s : strings) {
            hashMap.merge(s, new ArrayList<>(List.of(s)), new BiFunction<ArrayList<String>, ArrayList<String>, ArrayList<String>>() {
                @Override
                public ArrayList<String> apply(ArrayList<String> old, ArrayList<String> value) {
                    old.addAll(value);
                    return old;
                }
            });
        }
        System.out.println(hashMap);
        //{a=[a, a, a, a, a, a, a, a, a, a, a], b=[b, b], c=[c, c, c, c], d=[d], e=[e, e, e, e, e, e, e, e, e, e, e], g=[g, g],
        // h=[h, h, h, h], i=[i, i, i, i, i, i, i], k=[k], l=[l, l, l, l, l, l, l], m=[m, m, m, m, m, m],
        // n=[n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n], o=[o, o, o, o, o], p=[p, p], r=[r, r, r, r], s=[s, s, s, s, s, s, s, s, s],
        // t=[t, t, t, t, t, t, t, t, t, t, t, t], u=[u, u, u, u, u, u, u, u, u], v=[v], y=[y, y]}
    }

    //Map的computeIfAbsent 统计字符
    @Test
    void test_05_0() {
        //生成字符数组
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");

        HashMap<String, ArrayList<String>> hashMap = new HashMap<>();
        //计算加入list
        for (String s : strings) {
            hashMap.computeIfAbsent(s, k -> new ArrayList<>()).add(s);
        }
        System.out.println(hashMap);
        //{a=[a, a, a, a, a, a, a, a, a, a, a], b=[b, b], c=[c, c, c, c], d=[d], e=[e, e, e, e, e, e, e, e, e, e, e], g=[g, g],
        // h=[h, h, h, h], i=[i, i, i, i, i, i, i], k=[k], l=[l, l, l, l, l, l, l], m=[m, m, m, m, m, m],
        // n=[n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n], o=[o, o, o, o, o], p=[p, p], r=[r, r, r, r], s=[s, s, s, s, s, s, s, s, s],
        // t=[t, t, t, t, t, t, t, t, t, t, t, t], u=[u, u, u, u, u, u, u, u, u], v=[v], y=[y, y]}
    }

    //Map的computeIfAbsent 统计字符出现的次数
    @Test
    void test_05_1() {
        //生成字符数组
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");

        HashMap<String, AtomicLong> hashMap = new HashMap<>();
        //计算加入list
        for (String s : strings) {
            hashMap.computeIfAbsent(s, k -> new AtomicLong(1L)).incrementAndGet();
        }
        System.out.println(hashMap);
        //{a=12, b=3, c=5, d=2, e=12, g=3, h=5, i=8, k=2, l=8, m=7, n=17, o=6, p=3, r=5, s=10, t=13, u=10, v=2, y=3}
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
