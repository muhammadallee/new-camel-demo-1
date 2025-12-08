package com.muhammadallee.cameldemo;


import com.lmax.disruptor.RingBuffer;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import java.util.Map;
import java.util.UUID;

@Component
public class MediatorRouteBuilder extends RouteBuilder {

    private final DisruptorPublisher disruptorPublisher;
    private final Map<String, ConnectionFactory> weblogicCFs;

    public MediatorRouteBuilder(
            RingBuffer<FailureEvent> ringBuffer,
            Map<String, ConnectionFactory> weblogicCFs) {

        this.disruptorPublisher = new DisruptorPublisher(ringBuffer);
        this.weblogicCFs = weblogicCFs;
    }

    @Override
    public void configure() {

        // ---------------- GLOBAL FAILURE HANDLER ----------------
        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(0)
                .process(exchange -> {

                    String bridgeId = exchange.getIn()
                            .getHeader("BridgeId", String.class);

                    if (bridgeId == null) {
                        bridgeId = UUID.randomUUID().toString();
                        exchange.getIn().setHeader("BridgeId", bridgeId);
                    }

                    Exception ex = exchange.getProperty(
                            Exchange.EXCEPTION_CAUGHT, Exception.class);

                    FailureEvent failureEvent = new FailureEvent();
                    failureEvent.bridgeId = bridgeId;
                    failureEvent.timestampNanos = System.nanoTime();
                    failureEvent.sourceQueue = exchange.getIn()
                            .getHeader("rabbitmq.QUEUE_NAME", String.class);
                    failureEvent.targetQueue = exchange.getIn()
                            .getHeader("jms.destination", String.class);
                    failureEvent.payload = exchange.getIn().getBody(byte[].class);
                    failureEvent.headers = exchange.getIn().getHeaders();
                    failureEvent.errorMessage = ex != null ? ex.getMessage() : "UNKNOWN";
                    failureEvent.exceptionType = ex != null ? ex.getClass().getName() : "UNKNOWN";

                    disruptorPublisher.publish(failureEvent);

                    RabbitMQHelper.nackWithoutRequeue(exchange);
                });

        // ---------------- JMS COMPONENT ----------------
        weblogicCFs.forEach((name, cf) -> {
            JmsComponent jms = JmsComponent.jmsComponent(cf);
            jms.setDestinationResolver(new JndiDestinationResolver());
            getContext().addComponent("jms-" + name, jms);
        });

        // ---------------- MAIN ROUTE ----------------
        from("rabbitmq://localhost:5672/exchange?"
                + "queue=SOURCE_Q"
                + "&autoAck=false"
                + "&prefetchCount=500")
                .routeId("rabbit-to-weblogic")

                .process(e -> {
                    e.getIn().setHeader("BridgeId",
                            UUID.randomUUID().toString());
                })

                // Persistent JMS send
                .to("jms:queue:TARGET_Q"
                        + "?deliveryPersistent=true")

                // ACK only after JMS success
                .process(RabbitMQHelper::ack);
    }
}
