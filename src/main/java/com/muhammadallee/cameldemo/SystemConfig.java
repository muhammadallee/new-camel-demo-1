package com.muhammadallee.cameldemo;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SystemConfig {
    private String systemName;
    private String weblogicConnectionFactory;
    private List<RouteConfig> routes;
}
