package com.muhammadallee.cameldemo;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisFailureStore {

    private final RedisTemplate<String, Object> redis;
    private static final int MAX_RETRIES = 5;


    public RedisFailureStore(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    public void persist(FailureEvent event) {
        String eventKey = eventKey(event.getBridgeId());
        redis.opsForValue().set(eventKey, event);
        redis.expire(eventKey, Duration.ofDays(30));

        redis.opsForList().leftPush(
                pendingQueue(event.getSourceQueue()),
                event.getBridgeId()
        );
    }

    /* ---------------- Load ---------------- */

    public FailureEvent load(String bridgeId) {
        return (FailureEvent) redis.opsForValue()
                .get(eventKey(bridgeId));
    }

    /* ---------------- Retry Logic ---------------- */

    public boolean incrementRetry(FailureEvent event) {
        event.incrementRetry();
        redis.opsForValue().set(eventKey(event.getBridgeId()), event);
        return event.getRetryCount() <= MAX_RETRIES;
    }

    /* ---------------- Queue Ops ---------------- */

    public String popPending(String sourceQueue) {
        Object v = redis.opsForList()
                .rightPop(pendingQueue(sourceQueue));
        return v == null ? null : v.toString();
    }

    public void requeue(FailureEvent event) {
        redis.opsForList().leftPush(
                pendingQueue(event.getSourceQueue()),
                event.getBridgeId()
        );
    }

    /* ---------------- Final State ---------------- */

    public void markInDlq(String bridgeId) {
        redis.opsForSet().add("bridge:dlq", bridgeId);
    }

    /* ---------------- Keys ---------------- */

    private String eventKey(String bridgeId) {
        return "bridge:event:" + bridgeId;
    }

    private String pendingQueue(String sourceQueue) {
        return "bridge:pending:" + sourceQueue;
    }
}
