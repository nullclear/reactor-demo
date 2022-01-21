package com.yxy.reactive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by Nuclear on 2020/12/14
 */
@SpringBootTest
public class ReactiveOtherTest {

    private static final String END = "END";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);

    AtomicInteger counter = new AtomicInteger(0);

    TextField textField = new TextField();

    Flux<String> publisher;

    @BeforeEach
    public void beforeEach() {
        AtomicReference<ScheduledFuture<?>> reference = new AtomicReference<>();
        // 输入任务，模拟文本框不停地输入
        Runnable inputTask = () -> {
            int i = counter.getAndIncrement();
            if (i < 10) {
                textField.input(Objects.toString(i));
            } else if (i == 10) {
                textField.input(END);
            } else if (i == 11) {
                logger.warn("Input Task will be canceled");
            } else {
                reference.get().cancel(Boolean.FALSE);
                logger.warn("Input Task has been canceled");
            }
        };
        // 延迟200ms启动，200毫秒执行一次
        reference.set(pool.scheduleWithFixedDelay(inputTask, 200, 200, TimeUnit.MILLISECONDS));

        publisher = Flux.create(emitter -> {
            // 监听器，监听文本输入内容，推到发射器
            Consumer<String> listener = text -> {
                if (END.equals(text)) {
                    emitter.complete();
                } else {
                    emitter.next(text);
                }
            };
            // 添加监听器
            textField.addListener(listener);

            // 废弃处理
            emitter.onDispose(textField::removeListener);
        });
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testNormal() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        // 订阅推送流
        publisher.subscribe(text -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
                logger.info("handle {}", text);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, e -> latch.countDown(), latch::countDown);
        latch.await();
    }

    @Test
    public void testBackPressure() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Subscription> reference = new AtomicReference<>();
        // 自定义订阅器
        Subscriber<String> subscriber = new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                reference.set(subscription);
            }

            @Override
            protected void hookOnNext(String text) {
                logger.info("handle {}", text);
            }

            @Override
            protected void hookOnComplete() {
                latch.countDown();
            }

            @Override
            protected void hookOnError(Throwable throwable) {
                latch.countDown();
            }
        };

        // 背压策略：
        // 1.缓冲区
        // 2.丢弃
        // 3.最新
        publisher.onBackpressureBuffer().subscribe(subscriber);

        // 推送者和处理者分别进行任务，处理能力由处理者自己决定
        Subscription subscription = reference.get();
        while (latch.getCount() != 0) {
            subscription.request(1);
            TimeUnit.MILLISECONDS.sleep(1000);
        }
        latch.await();
    }

    /**
     * 文本框
     */
    public static class TextField {

        private static final Consumer<String> DEFAULT_LISTENER = text -> {
        };

        private Consumer<String> listener = DEFAULT_LISTENER;

        protected final Logger logger = LoggerFactory.getLogger(getClass());

        public void addListener(Consumer<String> listener) {
            logger.info("addListener");
            this.listener = listener;
        }

        public void removeListener() {
            logger.info("removeListener");
            listener = DEFAULT_LISTENER;
        }

        public void input(String text) {
            listener.accept(text);
        }
    }
}
