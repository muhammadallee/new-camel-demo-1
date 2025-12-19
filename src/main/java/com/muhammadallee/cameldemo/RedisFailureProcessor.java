package com.muhammadallee.cameldemo;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisFailureProcessor {

    private final RedisFailureStore redisStore;
    private final DlqPublisher dlqPublisher;

    private final String NODE_ID = System.getenv().getOrDefault("HOSTNAME", "local");

    public RedisFailureProcessor(RedisFailureStore rediStore,
                                 DlqPublisher dlqPublisher) {
        this.redisStore = rediStore;
        this.dlqPublisher = dlqPublisher;
    }

    @Scheduled(fixedDelay = 3000)
    public void process() {

        String queueKey = "bridge:pending:source";
        String sourceQueue = "source"; // resolve dynamically if needed
        String bridgeId = redisStore.popPending(sourceQueue);
        if (bridgeId == null) return;

        FailureEvent event = redisStore.load(bridgeId);
        if (event == null) return;

        try {
            // call REST retry endpoint here
            throw new RuntimeException("Simulated REST failure");

        } catch (Exception e) {

            boolean retryAllowed = redisStore.incrementRetry(event);

            if (retryAllowed) {
                redisStore.requeue(event);
            } else {
                dlqPublisher.publish(event);
                redisStore.markInDlq(bridgeId);
            }
        }
    }
}
