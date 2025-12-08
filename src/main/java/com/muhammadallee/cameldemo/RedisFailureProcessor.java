package com.muhammadallee.cameldemo;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisFailureProcessor {

    private final RedisCommands<String, String> redis;
    private final DlqPublisher dlqPublisher;

    public RedisFailureProcessor(StatefulRedisConnection<String, String> conn,
                                 DlqPublisher dlqPublisher) {
        this.redis = conn.sync();
        this.dlqPublisher = dlqPublisher;
    }

    @Scheduled(fixedDelay = 3000) // TODO: Quartz - for cluster awareness? and make it configurable
    public void process() {

        for (int i = 0; i < 500; i++) {

            String bridgeId = redis.rpop("bridge:failure:queue");
            if (bridgeId == null) return;

            String key = "bridge:failure:" + bridgeId;

            try {
                // TODO: REST call here
                redis.del(key);
            } catch (Exception e) {
                long retry = redis.hincrby(key, "retryCount", 1);
                if (retry > 50) {
                    dlqPublisher.publish("DLQ", redis.hget(key, "payload").getBytes());
                    redis.del(key);
                } else {
                    redis.lpush("bridge:failure:queue", bridgeId);
                }
            }
        }
    }
}
