package com.yxy.reactive;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Created by Nuclear on 2020/10/7
 */
@SpringBootTest
@Log4j2
public class ReactiveAdvancedTest {

    private Logger logger = LoggerFactory.getLogger(ReactiveAdvancedTest.class);

    //Flux和Mono互转
    @SuppressWarnings("ReactorUnusedPublisher")
    @Test
    void test_00() throws InterruptedException {
        //mono转flux 不可行方法
        Flux<List<Integer>> listFlux = Flux.from(Mono.just(List.of(1, 2, 3)));

        Mono<Flux<Integer>> monoFlux = Mono.just(List.of(1, 2, 3)).map(Flux::fromIterable);

        //mono转flux可行方法 具体来说是2种方法
        Flux<Integer> flux1 = Mono.just(List.of(1, 2, 3)).flatMapIterable(integers -> integers);

        Flux<Integer> flux2 = Mono.just(List.of(1, 2, 3)).flatMapMany(integers -> Flux.fromIterable(integers));
        Flux<Integer> flux3 = Mono.just(List.of(1, 2, 3)).flatMapMany(Flux::fromIterable);//上面的简化

        //flux转mono
        Mono<Integer> mono = Mono.from(Flux.range(1, 5));//这个只保留第一个元素

        Mono<List<Integer>> monoList = Flux.range(1, 5).collectList();

        Thread.sleep(3000);
    }

    //无数据或者有数据都会触发doOnSuccess
    @Test
    void test_01_00() throws InterruptedException {
        //#A-> A
        Mono.just("A").doOnSuccess(value -> System.out.print("#" + value + "-> ")).subscribe(System.out::println);
        //#null->
        Mono.empty().doOnSuccess(value -> System.out.print("#" + value + "-> ")).subscribe(System.out::println);
        //java.lang.ArithmeticException: / by zero
        Mono.just(1 / 0).doOnSuccess(value -> System.out.print("#" + value + "-> ")).subscribe(System.out::println);
        Thread.sleep(3000);
    }

    //只有有数据才会触发doOnNext
    @Test
    void test_01_01() throws InterruptedException {
        //#B->B
        Mono.just("B").doOnNext(value -> System.out.print("#" + value + "->")).subscribe(System.out::println);
        //没有任何输出
        Mono.empty().doOnNext(value -> System.out.print("#" + value + "->")).subscribe(System.out::println);
        //java.lang.ArithmeticException: / by zero
        Mono.just(1 / 0).doOnNext(value -> System.out.print("#" + value + "->")).subscribe(System.out::println);
        Thread.sleep(3000);
    }

    //doFinally必然触发
    @Test
    void test_01_02() throws InterruptedException {
        //C#onComplete->
        Mono.just("C").doFinally(value -> System.out.println("#" + value + "->")).subscribe(System.out::print);
        //#onComplete->
        Mono.empty().doFinally(value -> System.out.println("#" + value + "->")).subscribe(System.out::println);
        //#onError->
        //reactor.core.Exceptions$ErrorCallbackNotImplemented: java.lang.ArithmeticException: / by zero
        Mono.just("C").map(x -> 1 / 0).doFinally(value -> System.out.print("#" + value + "->")).subscribe(System.out::println);
        Thread.sleep(3000);
    }

    //doOnError只有在发生异常时才触发
    @Test
    void test_01_03() throws InterruptedException {
        //D
        Mono.just("D").doOnError(value -> System.out.println("#" + value + "->")).subscribe(System.out::println);
        //无任何输出
        Mono.empty().doOnError(value -> System.out.println("#" + value + "->")).subscribe(System.out::println);
        //#java.lang.ArithmeticException: / by zero->
        Mono.just("D").map(x -> 1 / 0).doOnError(value -> System.out.print("#" + value + "->")).subscribe(System.out::println);
        Thread.sleep(3000);
    }

    //doOnEach相当于onError | (onSubscribe onNext* (onError | onComplete)?)结合体
    @Test
    void test_01_04() throws InterruptedException {
        //#doOnEach_onNext(E)->#onComplete()->E
        Mono.just("E").doOnEach(value -> System.out.print("#" + value + "->")).subscribe(System.out::println);
        //#onComplete()->
        Mono.empty().doOnEach(value -> System.out.println("#" + value + "->")).subscribe(System.out::println);
        //#onError(java.lang.ArithmeticException: / by zero)->
        Mono.just("E").map(x -> 1 / 0).doOnEach(value -> System.out.print("#" + value + "->")).subscribe(System.out::println);
        Thread.sleep(3000);
    }

    //doFirst会在最前面执行 倒序最前
    @Test
    void test_01_05() throws InterruptedException {
        //1
        //2
        //3
        Mono.empty().doFirst(() -> System.out.println("3"))
                .doFirst(() -> System.out.println("2"))
                .doFirst(() -> System.out.println("1")).subscribe(System.out::println);

        //one
        //two
        //three
        //java.lang.ArithmeticException: / by zero
        Mono.just("F").map(x -> 1 / 0)
                .doFirst(() -> System.out.println("three"))
                .doFirst(() -> System.out.println("two"))
                .doFirst(() -> System.out.println("one")).subscribe(System.out::println);
        Thread.sleep(3000);
    }

    //doOnTerminate相当于doOnSuccess 和 doOnError
    //either by completing with a value,completing empty or completing with an error
    @Test
    void test_01_06() throws InterruptedException {
        //#1->G
        Mono.just("G").doOnTerminate(() -> System.out.print("#1->")).subscribe(System.out::println);
        //#2
        Mono.empty().doOnTerminate(() -> System.out.println("#2")).subscribe(System.out::println);
        //#3
        Mono.just("G").map(x -> 1 / 0).doOnTerminate(() -> System.out.println("#3")).subscribe(System.out::println);
        Thread.sleep(3000);
    }

    //defaultIfEmpty没法处理 wrapper 后的数据
    @Test
    void test_02() {
        Mono.just(Mono.just(1)).map(i -> Mono.empty()).defaultIfEmpty(Mono.just(2)).subscribe(System.out::println);//MonoEmpty

        Mono.just(Mono.empty()).map(i -> Mono.just(1)).defaultIfEmpty(Mono.just(2)).subscribe(System.out::println);//MonoJust

        Mono.just(Mono.empty()).map(i -> Mono.just(1)).defaultIfEmpty(Mono.just(2)).flatMap(x -> x).subscribe(System.out::println);//1

        Mono.just(Mono.justOrEmpty((String) null)).map(i -> Mono.just(1)).defaultIfEmpty(Mono.just(2)).flatMap(x -> x).subscribe(System.out::println);//1

        Mono.justOrEmpty((String) null).map(i -> i).defaultIfEmpty("2").subscribe(System.out::println);//2
    }

    @Test
    void test_03_01() {
        Mono.empty().switchIfEmpty(Mono.just("switchIfEmpty"))
                .map(it -> {
                    System.out.println(it);
                    throw new RuntimeException();
                }).subscribe(System.out::println);
        //输出switchIfEmpty
        //抛出异常
    }

    @Test
    void test_03_02() {
        Mono.empty().map(it -> {
            System.out.println(it);
            throw new RuntimeException();
        }).switchIfEmpty(Mono.just("switchIfEmpty")).subscribe(System.out::println);
        //输出switchIfEmpty
        //没有抛出异常
    }

    @Test
    void test_03_03() {
        Mono.just("just").switchIfEmpty(Mono.just("switchIfEmpty"))
                .map(it -> {
                    System.out.println(it);
                    throw new RuntimeException();
                }).subscribe(System.out::println);
        //输出just
        //抛出异常
    }

    @Test
    void test_03_04() {
        Mono.just("just")
                .map(it -> {
                    System.out.println(it);
                    throw new RuntimeException();
                })
                .switchIfEmpty(Mono.just("switchIfEmpty")).subscribe(System.out::println);
        //输出just
        //抛出异常
    }

    @Test
    void test_03_05() {
        Mono.just(Mono.empty()).switchIfEmpty(Mono.just(Mono.just("switchIfEmpty")))
                .map(x -> {
                    System.out.println(x);
                    throw new RuntimeException();
                }).subscribe(System.out::println);
        //输出MonoEmpty
        //抛出异常
    }

    @Test
    void test_03_06() {
        Mono.just(Mono.empty())
                .map(x -> {
                    System.out.println(x);
                    throw new RuntimeException();
                }).switchIfEmpty(Mono.just("switchIfEmpty")).subscribe(System.out::println);
        //输出MonoEmpty
        //抛出异常
    }

    @Test
    void test_03_07() {
        Object o = null;
        Mono.justOrEmpty(o).defaultIfEmpty(Mono.just(1))
                .subscribe(System.out::println);
        //输出MonoJust
    }

    @Test
    void test_03_08() {
        Object o = null;
        Mono.justOrEmpty(o).switchIfEmpty(Mono.just(1))
                .subscribe(System.out::println);
        //输出1
    }

    //switch的特殊测试
    @RepeatedTest(value = 2)
    void test_03_09() {
        Mono.just("1").map(s -> {
            System.out.println("2");
            return s;
        }).switchIfEmpty(method()).subscribe(System.out::println);
    }

    private static Mono<String> method() {
        System.out.println("3");//会直接被调用，原理未知
        return Mono.just("4").map(s -> {
            System.out.println(s);
            return s;
        });
    }

    //内存分页
    @Test
    void test_04() {
        Flux.range(1, 5).skip(1).collectList().subscribe(x -> System.out.println("->" + x));
        //->[2, 3, 4, 5]
        Flux.range(1, 5).skip(1).take(1).collectList().subscribe(x -> System.out.println("->" + x));
        //->[2]
        Flux.range(1, 5).skip(5).collectList().subscribe(x -> System.out.println("->" + x));
        //->[]
        Flux.range(1, 5).skip(2).take(5).collectList().subscribe(x -> System.out.println("->" + x));
        //->[3, 4, 5]
        Flux.range(1, 5).skip(6).take(5).collectList().subscribe(x -> System.out.println("->" + x));
        //->[]

        int num = 2;
        int size = 5;
        Flux.range(0, 99).skip((num - 1) * size).take(size).collectList().subscribe(x -> System.out.println("->" + x));
        //->[5, 6, 7, 8, 9]
    }

    //内部的mono与flux必须订阅才能执行
    @Test
    void test_05() {
        Mono.just("A").doOnNext(s -> Mono.just(1).map(it -> {
            System.out.println(it);
            return it;
        })).subscribe();
        //没有任何输出

        Mono.just("B").doOnNext(s -> Mono.just(2).map(it -> {
            System.out.println(it);
            return it;
        }).subscribe()).subscribe();
        //输出2

        //内部不订阅不会输出
        Mono.just("C").map(it -> {
            Mono.just(1).map(i -> {
                System.out.println(it);
                return i;
            });
            return it;
        }).subscribe();
    }

    @Test
    void test_06_01() {
        Mono.just("repeat").repeat(2, () -> true).subscribe(System.out::println);
        //repeat
        //repeat
        //repeat
    }

    @Test
    void test_06_02() {
        //原来一个，再取3个，一共四个
        Mono.just("repeat").repeatWhen(longFlux -> longFlux.take(3)).subscribe(System.out::println);
        //repeat
        //repeat
        //repeat
        //repeat

        //如果repeatWhen直接返回empty，那什么都输出不了
        Mono.just("empty").repeatWhen(longFlux -> Mono.empty()).subscribe(System.out::println);

        //当empty的时候不重复，不empty就重复
        Mono.just("repeatEmpty").repeatWhen(longFlux -> Flux.range(1, 5).flatMap(i -> {
            if ((i & 1) == 1) {
                return Mono.<Integer>empty();
            } else {
                return Mono.just(i);//2 4
            }
        })).subscribe(System.out::println);
        //repeatEmpty
        //repeatEmpty
        //repeatEmpty
    }

    @Test
    void test_06_03() throws InterruptedException {
        Flux.interval(Duration.ofMillis(250))
                .map(input -> {
                    if (input < 3) return "tick " + input;
                    throw new RuntimeException("boom");
                })
                .elapsed()
                .retry(2)
                .subscribe(System.out::println, System.err::println);
        Thread.sleep(5100);
    }

    @Test
    void test_07_01() {
        try {
            handle().map(x -> {
                throw new RuntimeException();
            }).subscribe();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.out.println(count);
    }

    private int count = 0;

    Mono<Integer> handle() {
        return find().map(i -> i * 2);
    }

    Mono<Integer> find() {
        return Mono.<Integer>empty().switchIfEmpty(Mono.just(1).doOnNext(i -> {
            Mono.just(i).map(integer -> {
                try {
                    System.out.println(Thread.currentThread().getName());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count = i;
                return count;
            }).subscribe();
        }));
    }
}
