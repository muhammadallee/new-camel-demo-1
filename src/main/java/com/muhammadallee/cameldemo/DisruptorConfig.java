package com.muhammadallee.cameldemo;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class DisruptorConfig {

    @Bean
    public Disruptor<FailureEvent> failureDisruptor(/*FailureEventHandler handler*/) {

        int ringSize = 65536; // TODO: Make it configurable

//        ThreadFactory tf = r -> {
//            Thread t = new Thread(r, "failure-disruptor");
//            t.setDaemon(true);
//            return t;
//        };

        Disruptor<FailureEvent> disruptor =
                new Disruptor<>(
                        FailureEvent::new,
                        ringSize,
                        Executors.defaultThreadFactory(),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy()
                );

        // NOTE: handler wiring happens here
        disruptor.handleEventsWith((event, seq, endOfBatch) -> {
            // NO-OP placeholder
            // Real handler = Redis writer / DB writer
        });

        //disruptor.handleEventsWith(handler);

        disruptor.start();
        return disruptor;
    }

    @Bean
    public RingBuffer<FailureEvent> failureRingBuffer(
            Disruptor<FailureEvent> disruptor) {
        return disruptor.getRingBuffer();
    }
}
