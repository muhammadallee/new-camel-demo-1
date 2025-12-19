package com.muhammadallee.cameldemo;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Exchange;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public final class FailureEvent implements Serializable {

    private static final long serialVersionUID = -2787549518589824597L;

    public String bridgeId;
    public byte[] payload;
    public Map<String, Object> headers;
    public String sourceQueue;
    public String targetQueue;
    public String errorMessage;
    public long timestampNanos;
    public String exceptionType;
    public int retryCount;
    public Date createdAt;

    public FailureEvent() {}

    public FailureEvent(byte[] payload, Map<String, Object> headers, String bridgeId, String sourceQueue, String targetQueue, String exceptionType, long timestampNanos, String errorMessage) {
        this.bridgeId = bridgeId;
        this.payload = payload;
        this.headers = headers;
        this.sourceQueue = sourceQueue;
        this.targetQueue = targetQueue;
        this.errorMessage = errorMessage;
        this.timestampNanos = timestampNanos;
        this.exceptionType = exceptionType;
        this.retryCount = 0;
        this.createdAt = new Date();
    }

    public static FailureEvent fromExchange(Exchange exchange, String bridgeId) {
        Exception ex = exchange.getProperty(
                Exchange.EXCEPTION_CAUGHT, Exception.class
        );

        return new FailureEvent(
                exchange.getIn().getBody(byte[].class),
                new HashMap<>(exchange.getIn().getHeaders()),
                exchange.getIn().getHeader("BridgeId", String.class),
                exchange.getIn().getHeader("rabbitmq.QUEUE_NAME", String.class),
                exchange.getIn().getHeader("jms.destination", String.class),
                ex != null ? ex.getMessage() : "Unknown error",
                System.nanoTime(),
                ex != null ? ex.getMessage() : "null");
    }

    public void incrementRetry() {
        ++this.retryCount;
    }
}
