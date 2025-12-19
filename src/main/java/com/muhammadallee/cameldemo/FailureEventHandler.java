package com.muhammadallee.cameldemo;

import com.lmax.disruptor.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class FailureEventHandler implements EventHandler<FailureEvent> {

    private final RedisFailureStore redis;
    private final DiskFailureStore disk;

    public FailureEventHandler(RedisFailureStore redis, DiskFailureStore disk) {
        this.redis = redis;
        this.disk = disk;
    }

    public void onEvent(FailureEvent e, long seq, boolean end) {
        try {
            redis.persist(e);
        } catch (Exception ex) {
            disk.append(e);
        }
    }
}