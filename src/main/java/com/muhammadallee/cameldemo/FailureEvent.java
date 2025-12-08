package com.muhammadallee.cameldemo;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Exchange;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public final class FailureEvent implements Serializable {

    private static final long serialVersionUID = -2787549518589824597L;

    public byte[] payload;
    public Map<String, Object> headers;
    public String bridgeId;
    public String sourceQueue;
    public String targetQueue;
    public String errorMessage;
    public long timestampNanos;
    public String exceptionType;
    public FailureEvent() {}

//    public static FailureEvent from(Exchange exchange) {
//        Exception ex = exchange.getProperty(
//                Exchange.EXCEPTION_CAUGHT, Exception.class
//        );
//
//        return new FailureEvent(
//                exchange.getIn().getBody(byte[].class),
//                new HashMap<>(exchange.getIn().getHeaders()),
//                exchange.getIn().getHeader("BridgeId", String.class),
//                exchange.getIn().getHeader("rabbitmq.QUEUE_NAME", String.class),
//                exchange.getIn().getHeader("jms.destination", String.class),
//                ex != null ? ex.getMessage() : "Unknown error",
//                System.nanoTime(),
//                ex.getMessage());
//    }
}
