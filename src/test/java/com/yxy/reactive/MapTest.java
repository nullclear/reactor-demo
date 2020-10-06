package com.yxy.reactive;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

/**
 * 测试map的高级api
 * Created by Nuclear on 2020/10/7
 */
@SpringBootTest
@Log4j2
public class MapTest {
    //Map的merge方法
    @Test
    void test_00_0() {
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

    //Map的merge方法统计字符出现的次数 这个可以考虑
    @Test
    void test_00_1() {
        String str = "This implementation assumes that the ConcurrentMap cannot contain null values and returning null unambiguously means the key is absent";
        String[] strings = str.replaceAll(" ", "").toLowerCase().split("");

        HashMap<String, Long> hashMap = new HashMap<>();
        for (String s : strings) {
            hashMap.merge(s, 1L, Long::sum);
        }
        System.out.println(hashMap);
        //{a=11, b=2, c=4, d=1, e=11, g=2, h=4, i=7, k=1, l=7, m=6, n=16, o=5, p=2, r=4, s=9, t=12, u=9, v=1, y=2}
    }

    //Map的merge方法统计字符 最好不要用这个 很别扭
    @Test
    void test_00_2() {
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
}
