package com.muhammadallee.cameldemo;

import com.rabbitmq.client.Channel;
import org.apache.camel.Exchange;

public final class RabbitMQHelper {

    private RabbitMQHelper() {}

    public static void ack(Exchange exchange) throws Exception {
        Channel channel = exchange.getIn()
                .getHeader("rabbitmq.CHANNEL", Channel.class);

        Long tag = exchange.getIn()
                .getHeader("rabbitmq.DELIVERY_TAG", Long.class);

        if (channel != null && tag != null) {
            channel.basicAck(tag, false);
        }
        //exchange.getIn().setHeader("rabbitmq.ack", constant(true));
    }

    public static void nackWithoutRequeue(Exchange exchange) throws Exception {
        Channel channel = exchange.getIn()
                .getHeader("rabbitmq.CHANNEL", Channel.class);

        Long tag = exchange.getIn()
                .getHeader("rabbitmq.DELIVERY_TAG", Long.class);

        if (channel != null && tag != null) {
            channel.basicNack(tag, false, false);
        }

//        exchange.getIn().setHeader("rabbitmq.ack", false);
//        exchange.getIn().setHeader("rabbitmq.requeue", false);
    }
}
