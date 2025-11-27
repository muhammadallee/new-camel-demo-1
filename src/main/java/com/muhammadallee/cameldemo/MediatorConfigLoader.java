package com.muhammadallee.cameldemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class MediatorConfigLoader {

    @Bean
    public MediatorConfig mediatorConfig(ObjectMapper mapper) throws Exception {
        return mapper.readValue(
                new ClassPathResource("route-config.json").getInputStream(),
                MediatorConfig.class
        );
    }
}