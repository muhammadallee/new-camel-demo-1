package com.muhammadallee.cameldemo;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import java.util.Map;
import java.util.UUID;

@Component
public class MediatorRouteBuilder extends RouteBuilder {

    @Autowired
    private RedisFailureStore redisRepo;

    @Autowired
    private DiskFailureStore diskStore;

    @Autowired
    private  Map<String, ConnectionFactory> weblogicCFs;

    @Autowired
    MediatorConfig mediatorConfig;


    @Override
    public void configure() {

        onException(Exception.class)
                .handled(true)
                .process(exchange -> {

                    if (Boolean.TRUE.equals(exchange.getProperty("ACKED"))) {
                        return;
                    }

                    String bridgeId = UUID.randomUUID().toString();
                    exchange.setProperty("bridgeId", bridgeId);

                    FailureEvent event = FailureEvent.fromExchange(exchange, bridgeId);

                    boolean persisted = false;
                    try {
                        redisRepo.persist(event);
                        persisted = true;
                    } catch (Exception redisEx) {
                        diskStore.append(event);
                        persisted = true;
                    }

                    if (persisted) {
                        RabbitMQHelper.nackWithoutRequeue(exchange);
                        exchange.setProperty("ACKED", true);
                    }
                });

        weblogicCFs.forEach((name, cf) -> {
            JmsComponent jms = JmsComponent.jmsComponent(cf);
            jms.setDestinationResolver(new JndiDestinationResolver());
            getContext().addComponent("jms-" + name, jms);
        });

        for (SystemConfig system : mediatorConfig.getSystems()) {
            for (RouteConfig route : system.getRoutes()) {
                setWLRoute(system, route);
            }
        }

    }

    private void setWLRoute(SystemConfig system, RouteConfig route) {
        from("rabbitmq://localhost:5672/" + route.getRabbitExchangeName()
                + "?queue=" + route.getRabbitSourceQueue()
                + "&autoAck=false&autoDelete=false")
                .routeId("rabbit-to-wls-" + system.getSystemName())
                .process(exchange -> {
                    exchange.getIn().setHeader("bridgeId",
                            UUID.randomUUID().toString());
                })
                .to("jms-" + system.getSystemName()
                        + ":queue:" + route.getWeblogicDestination()
                        + "?deliveryPersistent=true")
                .process(exchange -> {

                    if (!Boolean.TRUE.equals(exchange.getProperty("ACKED"))) {
                        RabbitMQHelper.ack(exchange);
                        exchange.setProperty("ACKED", true);
                    }
                });
    }
}
