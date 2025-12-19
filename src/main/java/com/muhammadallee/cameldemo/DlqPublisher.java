package com.muhammadallee.cameldemo;

import org.apache.camel.ProducerTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.TextMessage;

@Component
public class DlqPublisher {

    //private final JmsTemplate jmsTemplate;

    private final ProducerTemplate producerTemplate;

    public DlqPublisher(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    public void publish(FailureEvent event) {
        producerTemplate.sendBodyAndHeaders(
                "rabbitmq://dlq-exchange"
                        + "?queue=DLQ"
                        + "&autoAck=true",
                event.getPayload(),
                event.getHeaders()
        );
    }
}
