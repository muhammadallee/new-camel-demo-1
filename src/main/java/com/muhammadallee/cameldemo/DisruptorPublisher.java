package com.muhammadallee.cameldemo;

import com.lmax.disruptor.RingBuffer;
import com.muhammadallee.cameldemo.FailureEvent;

public final class DisruptorPublisher {

    private final RingBuffer<FailureEvent> ringBuffer;

    public DisruptorPublisher(RingBuffer<FailureEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void publish(FailureEvent data) {
        long seq = ringBuffer.next();
        try {
            FailureEvent evt = ringBuffer.get(seq);

            evt.bridgeId = data.bridgeId;
            evt.timestampNanos = data.timestampNanos;
            evt.sourceQueue = data.sourceQueue;
            evt.targetQueue = data.targetQueue;
            evt.payload = data.payload;
            evt.headers = data.headers;
            evt.errorMessage = data.errorMessage;
            evt.exceptionType = data.exceptionType;

        } finally {
            ringBuffer.publish(seq);
        }
    }
}
