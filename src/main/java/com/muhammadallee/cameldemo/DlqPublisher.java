package com.muhammadallee.cameldemo;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.BytesMessage;

@Component
public class DlqPublisher {

    private final JmsTemplate jmsTemplate;

    public DlqPublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publish(String dlqName, byte[] payload) {
        jmsTemplate.send(dlqName,
                session -> {
                    BytesMessage msg = session.createBytesMessage();
                    msg.writeBytes(payload);
                    return msg;
                }
        );
    }
}
