package com.muhammadallee.cameldemo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter @Setter
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitProperties {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String virtualHost;
    private String dlqSuffix;
}