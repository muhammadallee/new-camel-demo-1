package com.muhammadallee.cameldemo;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.util.Map;

@Component
public class MediatorRouteBuilder extends RouteBuilder {

    @Autowired
    MediatorConfig config;

    @Autowired
    RabbitProperties rabbitProps;

    @Autowired
    Map<String, ConnectionFactory> weblogicCFs;

    @Override
    public void configure() throws Exception {

        // ================================================================
        // GLOBAL EXCEPTION HANDLER
        // ================================================================
        onException(JMSException.class)
                .maximumRedeliveries(0)
                .handled(true)
                .wireTap("seda:notifyFailure")
                .log("Failed to enqueue to WebLogic for message from RabbitMQ");

        // ================================================================
        // ASYNC FAILURE NOTIFIER
        // ================================================================
        from("seda:notifyFailure")
                .routeId("asyncFailureNotifier")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .toD("${header.failureRestUrl}?httpMethod=POST&throwExceptionOnFailure=false");


        // ================================================================
        // MAIN ROUTES
        // ================================================================
        for (SystemConfig sys : config.getSystems()) {

            // JMS component for WebLogic
            JmsComponent jms = JmsComponent.jmsComponent(weblogicCFs.get(sys.getSystemName()));
            jms.setDestinationResolver(new JndiDestinationResolver());
            getContext().addComponent("jms-" + sys.getSystemName(), jms);

            for (RouteConfig route : sys.getRoutes()) {

                // ============================================================
                // BUILD RABBITMQ URI FROM ROUTE-LEVEL TUNING
                // ============================================================
                String rabbitUri =
                        "rabbitmq://%s:%d/%s"
                                + "?queue=%s"
                                + "&username=%s"
                                + "&password=%s"
                                + "&routingKey=%s"
                                + "&autoAck=false"
                                + "&acknowledgeMode=MANUAL"
                                + "&automaticRecoveryEnabled=true"

                                // tuning parameters
                                + "&concurrentConsumers=%d"
                                + "&maxConcurrentConsumers=%d"
                                + "&prefetchCount=%d"
                                + "&threadPoolSize=%d"
                                + "&channelCacheSize=%d"

                                // queue/exchange flags
                                + "&queueDurable=%b"
                                + "&exchangeDurable=%b"
                                + "&autoDelete=%b"

                                // DLX/DLQ
                                + "&deadLetterExchange=%s"
                                + "&deadLetterRoutingKey=%s";

                rabbitUri = String.format(
                        rabbitUri,
                        rabbitProps.getHost(),
                        rabbitProps.getPort(),
                        route.getRabbitExchangeName(),
                        route.getRabbitSourceQueue(),
                        rabbitProps.getUsername(),
                        rabbitProps.getPassword(),
                        route.getRabbitRoutingKey(),

                        // tuning
                        route.getConcurrentConsumers(),
                        route.getMaxConcurrentConsumers(),
                        route.getPrefetchCount(),
                        route.getThreadPoolSize(),
                        route.getChannelCacheSize(),

                        // queue flags
                        route.isDurable(),
                        route.isDurable(),
                        route.isAutoDelete(),

                        // DLX/DLQ
                        route.getDlxName(),
                        route.getDlqRoutingKey()
                );

                // ============================================================
                // ROUTE: RABBITMQ -> WEBLOGIC JMS
                // ============================================================
                from(rabbitUri)
                        .routeId("route_" + sys.getSystemName() + "_" + route.getRabbitSourceQueue())

                        .log("Received message from RabbitMQ queue: " + route.getRabbitSourceQueue())

                        .to("jms-" + sys.getSystemName() + ":queue:" + route.getWeblogicDestination())

                        .log("Forwarded to WebLogic queue: " + route.getWeblogicDestination())

                        // MANUAL ACK
                        .setHeader("rabbitmq.ack", constant(true));
            }
        }
    }
}
