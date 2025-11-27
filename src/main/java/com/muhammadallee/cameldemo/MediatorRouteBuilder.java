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

        // -------------------------------------------------------------------
        // 1. GLOBAL EXCEPTION HANDLER — MUST COME FIRST!
        // -------------------------------------------------------------------
        onException(JMSException.class)
                .maximumRedeliveries(0)
                .handled(true)

                // async REST call
                .wireTap("seda:notifyFailure")
                // send to DLQ dynamically
//                .toD("rabbitmq://"
//                        + "${header.rabbitHost}"
//                        + ":" + "${header.rabbitPort}"
//                        + "/" + "${header.dlqName}"
//                        + "?username=${header.rabbitUser}"
//                        + "&password=${header.rabbitPass}"
//                        //+ "&vhost=${header.rabbitVhost}"
//                )
                .log("Failed to enqueue to WebLogic. Routed to DLQ ${header.dlqName}");

        // -------------------------------------------------------------------
        // async REST notification route
        // -------------------------------------------------------------------
        from("seda:notifyFailure")
                .routeId("asyncFailureNotifier")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .doTry()
                .toD("${header.failureRestUrl}?httpMethod=POST&throwExceptionOnFailure=false")
                .doCatch(Exception.class)
                .log("Async REST call failed: ${exception.message}")
                .end();


        // -------------------------------------------------------------------
        // 2. NOW YOU CAN DEFINE ROUTES
        // -------------------------------------------------------------------
        for (SystemConfig sys : config.getSystems()) {

            // create JMS component for this system
            JmsComponent jms = JmsComponent.jmsComponent(weblogicCFs.get(sys.getSystemName()));
            jms.setDestinationResolver(new JndiDestinationResolver());
            getContext().addComponent("jms-" + sys.getSystemName(), jms);

            for (RouteConfig route : sys.getRoutes()) {

                String rabbitUri = String.format(
                        "rabbitmq://%s:%d/%s?queue=%s&username=%s&password=%s&routingKey=%s&autoAck=false&autoDelete=false",
                        rabbitProps.getHost(),
                        rabbitProps.getPort(),
                        route.getRabbitExchangeName(), // <-- Use a real or default exchange name (e.g., 'default' or an empty string)
                        route.getRabbitSourceQueue(), // <-- Queue name is passed as the 'queue' parameter
                        rabbitProps.getUsername(),
                        rabbitProps.getPassword(),
                        route.getRabbitRoutingKey() // <-- Include routing key
                );

//                String rabbitUri = String.format(
//                        "rabbitmq://%s:%d/%s?username=%s&password=%s&autoAck=false",
//                        rabbitProps.getHost(),
//                        rabbitProps.getPort(),
//                        route.getRabbitExchangeName(),
//                        route.getRabbitSourceQueue(),
//                        rabbitProps.getUsername(),
//                        rabbitProps.getPassword()
//                        //,rabbitProps.getVirtualHost()
//                );

                String dlqName = route.getRabbitSourceQueue() + rabbitProps.getDlqSuffix();

                from(rabbitUri)
                        .routeId("route_" + sys.getSystemName() + "_" + route.getRabbitSourceQueue())

                        // set headers used by exception handler
                        //.setHeader("dlqName", constant(dlqName))
                        .setHeader("failureRestUrl", constant(config.getGlobal().getFailureRestEndpoint()))
                        .setHeader("rabbitHost", constant(rabbitProps.getHost()))
                        .setHeader("rabbitPort", constant(rabbitProps.getPort()))
                        .setHeader("rabbitUser", constant(rabbitProps.getUsername()))
                        .setHeader("rabbitPass", constant(rabbitProps.getPassword()))
                        //.setHeader("rabbitVhost", constant(rabbitProps.getVirtualHost()))

                        .log("Received message from " + route.getRabbitSourceQueue())

                        // WebLogic enqueue
                        .to("jms-" + sys.getSystemName() + ":queue:" + route.getWeblogicDestination())

                        .log("Forwarded to WebLogic queue " + route.getWeblogicDestination())

                        // manual ack
                        .setHeader("rabbitmq.ack", constant(true));
            }
        }
    }
}
