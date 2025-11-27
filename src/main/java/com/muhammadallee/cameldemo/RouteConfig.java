package com.muhammadallee.cameldemo;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RouteConfig {
    private String rabbitSourceQueue;
    private String rabbitExchangeName;
    private String rabbitRoutingKey;
    private String weblogicDestination;


}
