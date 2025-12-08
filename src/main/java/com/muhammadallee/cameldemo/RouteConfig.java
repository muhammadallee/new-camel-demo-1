package com.muhammadallee.cameldemo;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RouteConfig {
    private String rabbitSourceQueue;
    private String rabbitExchangeName;
    private String rabbitRoutingKey;
    private String weblogicDestination;
    private int concurrentConsumers;
    private int maxConcurrentConsumers;
    private int prefetchCount;
    private int threadPoolSize;
    private int channelCacheSize;

    private boolean durable;
    private boolean exclusive;
    private boolean autoDelete;

    private String dlqName;
    private String dlxName;
    private String dlqRoutingKey;


}
