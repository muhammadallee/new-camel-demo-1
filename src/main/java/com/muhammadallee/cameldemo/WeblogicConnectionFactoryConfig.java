package com.muhammadallee.cameldemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WeblogicConnectionFactoryConfig {

    @Bean
    public Map<String, ConnectionFactory> weblogicConnectionFactories(MediatorConfig cfg) throws Exception {
        Map<String, ConnectionFactory> factories = new HashMap<>();

        for (SystemConfig sys : cfg.getSystems()) {
            InitialContext ctx = new InitialContext();
            ConnectionFactory cf =
                    (ConnectionFactory) ctx.lookup(sys.getWeblogicConnectionFactory());

            factories.put(sys.getSystemName(), cf);
        }
        return factories;
    }
}
