package com.yxy.reactive;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
@Log4j2
class ReactiveBasicTest {
    //错误处理方法
    @Test
    void test_00_0() {
        //错误处理方法一 打印错误的日志 其他继续
        Flux.range(-2, 5).map(val -> {
            int i = 1 / val;
            return val;
        }).onErrorContinue((ex, val) -> {
            if (ex instanceof RuntimeException) {
                log.error("ex:{},val:{}", ex, val);
            }
        }).subscribe(System.out::println);
        //-2
        //-1
        //2020-10-06 00:39:12.470 ERROR 16124 --- [           main] com.yxy.reactive.ReactiveTests           : ex:java.lang.ArithmeticException: / by zero,val:0
        //1
        //2
    }

    //错误处理方法
    @Test
    void test_00_1() {
        //错误处理方法二
        Flux.range(-2, 5).map(val -> {
            int i = 1 / val;
            return val;
        }).onErrorResume(ex ->
                {
                    //生成新的流
                    return Flux.range(-2, 5);
                }
        ).subscribe(System.out::println);
        //-2
        //-1 到此为止 后面就不输出了
        //-2
        //-1
        //0
        //1
        //2
    }

    //错误处理方法
    @Test
    void test_00_2() {
        //错误处理方法三
        Flux.range(-2, 5).map(val -> {
            int i = 1 / val;
            return val;
        }).onErrorReturn(0).subscribe(System.out::println);
        //-2
        //-1
        //0
    }

    //错误处理方法
    @Test
    void test_00_3() {
        //错误处理方法四
        Flux.range(-2, 5).map(val -> {
            int i = 1 / val;
            return val;
        }).onErrorMap(throwable -> {
            if (throwable instanceof RuntimeException) {
                return new ArithmeticException();
            } else {
                return new ClassCastException();
            }
        }).subscribe(System.out::println);
        //-2
        //-1
        //reactor.core.Exceptions$ErrorCallbackNotImplemented: java.lang.ArithmeticException
    }

    //window与flatMap
    @Test
    void test_01() {
        //window与flatMap
        Flux<String> stringFlux = Flux.just("A", "B", "C", "D", "E", "F");
        Flux<Flux<String>> stringFlux1 = stringFlux.window(2);
        Flux<String> stringFlux2 = stringFlux1.flatMap(flux -> flux.map(String::toLowerCase));
        stringFlux2.subscribe();
    }

    //concatMap是有序的flatMap
    @Test
    void test_02() throws InterruptedException {
        //concatMap是有序的flatMap
        Flux<String> stringFlux = Flux.just("A", "B", "C", "D", "E", "F", "G", "H");
        Flux<Flux<String>> stringFlux2 = stringFlux.window(3);

        stringFlux2.flatMap(flux -> flux.map(String::toLowerCase).delayElements(Duration.ofMillis(200)))
                .subscribe(x -> System.out.print("->" + x));//->a->g->d->b->h->e->f->c

        stringFlux2.concatMap(flux -> flux.map(String::toLowerCase).delayElements(Duration.ofMillis(200)))
                .subscribe(x -> System.out.print("->" + x));//->a->b->c->d->e->f->g->h

        Thread.sleep(2000);
    }

    //Flux值的后续处理 基本上类似于Stream
    @SuppressWarnings("UnnecessaryLocalVariable")
    @Test
    void test_03() {
        Flux<String> stringFlux = Flux.just("A", "B", "C", "D", "E", "F");
        //普通的list
        Mono<List<String>> list = stringFlux.collectList();
        list.subscribe(System.out::print);//[A, B, C, D, E, F]

        //普通的map
        Mono<Map<String, String>> map = stringFlux.collectMap(Function.identity());
        map.subscribe(System.out::print);//{A=A, B=B, C=C, D=D, E=E, F=F}

        //统计出现的次数
        Mono<Map<String, Long>> count = stringFlux.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        count.subscribe(System.out::print);//{A=1, B=1, C=1, D=1, E=1, F=1}

        //一个结果处理两次, 再做成一个结果返回
        Mono<Map<String, Collection<String>>> flatMap = Mono.just(stringFlux)
                .flatMap(it -> {
                            //zip的tuple
                            Mono<Tuple2<List<String>, Set<String>>> tuple = Mono.zip(it.collectList(), it.collect(Collectors.toSet()));
                            //tuple内部处理
                            Mono<Map<String, Collection<String>>> mapMono = tuple.map(tuple2 -> {
                                List<String> t1 = tuple2.getT1();
                                Set<String> t2 = tuple2.getT2();
                                //结果做成map
                                Map<String, Collection<String>> collectionMap = Map.of("list", t1, "set", t2);
                                return collectionMap;
                            });
                            return mapMono;
                        }
                );
        flatMap.subscribe();
    }

    //Flux获取元素
    @Test
    void test_04() {
        //获取前五个
        Flux.range(1, 10).take(5).subscribe(System.out::println);
        //获取10毫秒内的数据
        Flux.range(1, 1000).take(Duration.ofMillis(10)).subscribe(System.out::println);
        //一直获取到5为止
        Flux.range(1, 100).takeUntil(i -> i == 5).subscribe(System.out::println);
        //获取最后五个
        Flux.range(1, 100).takeLast(5).subscribe(System.out::println);
        //当流中的数据<50时执行,如果一开始就＞=50,则无法执行
        Flux.range(1, 100).takeWhile(i -> i < 50).subscribe(System.out::println);
    }

    //Flux与Mono的zip
    @Test
    void test_05() {
        Flux<String> str1 = Flux.just("A", "B", "C", "D", "E", "F", "G", "H");
        Flux<String> str2 = Flux.just("1", "2", "3", "4", "5", "6", "7");
        Flux<String> str3 = Flux.just("@", "#", "$", "%");
        str1.zipWith(str2).subscribe(x -> System.out.print("-" + x));
        //-[A,1]-[B,2]-[C,3]-[D,4]-[E,5]-[F,6]-[G,7]
        //一一对应,多余的的不参与zip
        System.out.println();

        Flux<Tuple2<String, String>> zip2 = Flux.zip(str1, str2);
        zip2.subscribe(x -> System.out.print("-" + x));
        //-[A,1]-[B,2]-[C,3]-[D,4]-[E,5]-[F,6]-[G,7]
        System.out.println();

        Flux<Tuple3<String, String, String>> zip3 = Flux.zip(str1, str2, str3);
        zip3.subscribe(x -> System.out.print("-" + x));
        //-[A,1,@]-[B,2,#]-[C,3,$]-[D,4,%]
        System.out.println();

        //Mono的整合,返回Object对象,可以用于将参数传入vo,最后传回去vo
        Mono<Integer> mono = Mono.zip(Mono.just(1), Mono.just(2), Integer::sum);//3
        mono.subscribe(System.out::println);
    }

    //buffer 与 window 的区别
    @Test
    void test_06() {
        //buffer里是list
        Flux<String> str1 = Flux.just("A", "B", "C", "D", "E", "F", "G", "H");
        Flux<List<String>> buffer = str1.buffer(2);
        buffer.subscribe(x -> System.out.print("->" + x));
        //->[A, B]->[C, D]->[E, F]->[G, H]
        System.out.println();

        //window里是flux
        Flux<Flux<String>> window = str1.window(2);//每2个装一起
        window.subscribe(x -> System.out.print("->" + x));
        //->UnicastProcessor->UnicastProcessor->UnicastProcessor->UnicastProcessor
        //因为里面还是Flux
        window.flatMap(flux -> flux).subscribe(x -> System.out.print("->" + x));
        //->A->B->C->D->E->F->G->H
        window.subscribe(x -> x.subscribe(y -> System.out.print("->" + y)));
        //->A->B->C->D->E->F->G->H
    }

    //背压 没搞懂
    @Test
    void test_07() throws InterruptedException {
        Flux<Integer> flux = Flux.range(1, 5000);
        flux.subscribe(new Subscriber<>() {
            Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer data) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
        Thread.sleep(1000);
    }

    //调度器
    @Test
    void test_08() {
        Flux.range(1, 10).parallel()//不写,默认cpu的核心数量
                .runOn(Schedulers.parallel())//不写这个没法跑
                .subscribe(x -> log.info("->:{}", x));
        Schedulers.parallel();//并行线程
        Schedulers.immediate();//当前线程
        Schedulers.single();//单一线程
        Schedulers.elastic();//弹性线程池
    }

    //merge 与 concat 的合并顺序的区别
    @Test
    void test_09() throws InterruptedException {
        Flux<Long> longFlux = Flux.interval(Duration.ofMillis(100)).take(10);
        Flux<Long> longFlux2 = Flux.interval(Duration.ofMillis(100)).take(10);
        Flux<Long> merge = Flux.merge(longFlux, longFlux2);
        Flux<Long> concat = Flux.concat(longFlux, longFlux2);
        merge.subscribe(x -> System.out.print("->" + x));
        //->0->0->1->1->2->2->3->3->4->4->5->5->6->6->7->7->8->8->9->9
        concat.subscribe(x -> System.out.print("->" + x));
        //->0->1->2->3->4->5->6->7->8->9->0->1->2->3->4->5->6->7->8->9
        Thread.sleep(2000);
    }

    //单步校验
    @Test
    void test_10() {
        Flux<Integer> range = Flux.range(1, 5);
        range.subscribe(x -> System.out.print("->" + x));
        StepVerifier.create(range)
                .expectNext(1)
                .expectNext(2)
                .expectNext(3)
                .expectNext(4)
                .expectNext(5).expectComplete().verify();
    }

    //buffer的相关方法
    @Test
    void test_11() throws InterruptedException {
        //普通的产生数据
        Mono.create(sink -> sink.success("Hello")).subscribe(System.out::println);
        //Hello

        //产生10个数据 每5个运输一次
        Flux.range(1, 10).buffer(5).subscribe(System.out::println);
        //[1, 2, 3, 4, 5]
        //[6, 7, 8, 9, 10]

        //每100毫米产生一个数据 每500毫米运输一次
        Flux.interval(Duration.ofMillis(100)).buffer(Duration.ofMillis(500)).subscribe(System.out::println);
        //[0, 1, 2, 3]
        //[4, 5, 6, 7, 8]
        //[9, 10, 11, 12, 13]
        //[14, 15, 16, 17, 18]
        //[19, 20, 21, 22, 23, 24]
        //[25, 26, 27, 28]
        //[29, 30, 31, 32, 33]
        //[34, 35, 36, 37, 38]
        //[39, 40, 41, 42, 43]
        //[44, 45, 46, 47, 48]

        //直到产生偶数才运输之前的数据
        Flux.range(1, 10).bufferUntil(i -> i % 2 == 0).subscribe(System.out::println);
        //[1, 2]
        //[3, 4]
        //[5, 6]
        //[7, 8]
        //[9, 10]

        //只运输偶数
        Flux.range(1, 10).bufferWhile(i -> i % 2 == 0).subscribe(System.out::println);
        //[2]
        //[4]
        //[6]
        //[8]
        //[10]
        Thread.sleep(5000);
    }

    //生成数据 create 与 generate 的区别
    @Test
    void test_12() {
        Flux.generate(sink -> {
            sink.next("Hello");
            sink.complete();
        }).subscribe(System.out::println);
        //Hello

        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            sink.next(value);//必须加,不加继续
            if (list.size() == 10) {
                sink.complete();//不加不停止
            }
            return list;
        }).subscribe(x -> System.out.print("->" + x));
        //->18->95->81->25->27->96->60->95->83->19

        System.out.println();
        Flux.generate(ArrayList::new, (list, sink) -> {
            for (int i = 0; i < 10; i++) {
                int value = random.nextInt(100);
                list.add(value);
            }
            sink.next(list);
            sink.complete();
            return list;
        }).subscribe(x -> System.out.print("->" + x));
        //->[5, 35, 14, 74, 92, 47, 16, 24, 83, 38]

        System.out.println();
        Flux.generate(() -> 1, (i, sink) -> {
            sink.next(i++);
            if (i == 11) {
                sink.complete();
            }
            return i;
        }).subscribe(x -> System.out.print("->" + x));
        //->1->2->3->4->5->6->7->8->9->10
        //每一次都输出,如果写成和下面一样,输出->1就没了

        System.out.println();
        Flux.create(sink -> {
            for (int i = 0; i < 10; i++) {
                sink.next(i);
            }
            sink.complete();
        }).subscribe(x -> System.out.print("->" + x));
        //->0->1->2->3->4->5->6->7->8->9
        //create()方法与 generate()方法的不同之处在于所使用的是 FluxSink 对象。
        // FluxSink 支持同步和异步的消息产生，并且可以在一次调用中产生多个元素。
        //在一次调用中就产生了全部的 10 个元素。

        System.out.println();
        Flux.create(sink -> {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                int value = random.nextInt(100);
                list.add(value);
            }
            sink.next(list);
            sink.complete();
        }).subscribe(System.out::println);
        //[81, 33, 69, 34, 92, 89, 36, 94, 57, 71]
    }

    //同步的世界
    @Test
    void test_13() {
        Flux.range(1, 10).toStream();
        Flux.range(1, 10).toIterable();
        Flux.range(1, 10).blockFirst();
        Flux.range(1, 10).blockLast();
        Mono.just(1).block();
        Mono.just(1).blockOptional();
    }
}
