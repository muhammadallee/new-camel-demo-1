package com.muhammadallee.cameldemo;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(
                RedisURI.builder()
                        .withHost("localhost")
                        .withPort(6379)
                        .withTimeout(java.time.Duration.ofSeconds(2))
                        .build()
        );
    }

    @Bean
    public StatefulRedisConnection<String, String> redisConnection(RedisClient client) {
        return client.connect();
    }
}
