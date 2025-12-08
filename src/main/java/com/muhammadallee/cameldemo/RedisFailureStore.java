package com.muhammadallee.cameldemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RedisFailureStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TTL_SECONDS = 30 * 24 * 60 * 60;

    private final RedisCommands<String, String> redis;

    public RedisFailureStore(StatefulRedisConnection<String, String> conn) {
        this.redis = conn.sync();
    }

    public void store(FailureEvent event) throws Exception {


        String key = "bridge:failure:" + event.getBridgeId();

        Map<String, String> map = new HashMap<>();
        map.put("payload", MAPPER.writeValueAsString(event.getPayload()));
        map.put("headers", MAPPER.writeValueAsString(event.getHeaders()));
        map.put("sourceQueue", event.getSourceQueue());
        map.put("targetQueue", event.getTargetQueue());
        map.put("error", event.getErrorMessage());
        map.put("retryCount", "0");

        // HSET bridge:failures <bridgeId> <json>
        // LPUSH bridge:queue <bridgeId>
        // EXPIRE bridge:failures 2592000
        redis.multi();
        redis.hmset(key, map);
        redis.expire(key, TTL_SECONDS);
        redis.lpush("bridge:failure:queue", event.getBridgeId());
        redis.exec();
    }
}
