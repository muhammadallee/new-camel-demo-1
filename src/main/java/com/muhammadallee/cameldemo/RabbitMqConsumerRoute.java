//package com.muhammadallee.cameldemo;
//
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.jms.JmsException;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RabbitMqConsumerRoute extends RouteBuilder {
//
//    // Define the queue name you want to consume from
//    public static final String INPUT_QUEUE = "test_q";
//    private static final String EXCHANGE_NAME = "test_exchange";
//    private static final String ROUTING_KEY = "test_rkey";
//
//    private static final String OUTPUT_QUEUE = "TargetQ";
//    private static final String FAILURE_ROUTE = "direct:callRestOnJmsFailure";
//
//    // NOTE: URIs should ideally be read from application.properties
//    private static final String NOTIFICATION_API = "http://localhost:3000/api";
//    private static final String AUTH_TOKEN_URL = "http://localhost:3001/auth/token";
//    private static final String CLIENT_SECRET = "cs";
//    private static final String CLIENT_ID = "ci";
//
//    // Configuration for High Concurrency and Broker-Side Retries
//    private static final int RABBITMQ_CONSUMERS = 20; // High concurrency on input
//    private static final int ASYNC_PROCESSORS = 100; // High concurrency for blocking JMS call
//
//    @Override
//    public void configure() throws Exception {
//
//        // --- 1. Broker-Side Retry Handling on JMS Failure ---
//        // Instead of thread-blocking retry, we NACK the message to RabbitMQ.
//        // The RabbitMQ broker/DLX mechanism handles the actual delay and retry count.
//        onException(JmsException.class)
//                // CRITICAL: Ensure Camel does NOT internally retry (maximumRedeliveries(0))
//                .maximumRedeliveries(0)
//                .handled(true)
//                .log("JMS delivery failed. NACKing message to RabbitMQ for broker retry via DLX.")
//                // CRITICAL: Set rejection flag for the spring-rabbitmq component
//                .setHeader("rabbitmq.reject", constant(true))
//                .end();
//
//        // --- 2. Final Failure Path (After Broker Retries are Exhausted) ---
//        // This is where RabbitMQ DLX routes the message after max broker retries.
//        // NOTE: You need a separate Camel route consuming the final "Dead Letter Queue"
//        // that receives messages from the RabbitMQ DLX after 5 retries.
//        // For demonstration, we keep your original direct:callRestOnJmsFailure which
//        // should instead be consuming from the final DLQ.
//        // We will repurpose it for the internal failure flow for simplicity.
//        onException(Exception.class)
//                .handled(true)
//                .to(FAILURE_ROUTE)
//                .end();
//
//        // --- 3. Main RabbitMQ Consumer Route (FAST HANDOVER) ---
//        // a) Use manual ACK (autoAck=false) and high concurrency (concurrentConsumers).
//        from("spring-rabbitmq:" + EXCHANGE_NAME +
//                "?queues=" + INPUT_QUEUE +
//                "&routingKey=" + ROUTING_KEY +
//                "&concurrentConsumers=" + RABBITMQ_CONSUMERS  // Tweak 1: High Concurrency
//                )//"&autoAck=false") // Tweak 2: Manual Acknowledgement
//                .routeId("RabbitMQFastConsumer")
//
//                .log(">>> Received message from RabbitMQ. Fast handover to SEDA.")
//
//                // Tweak 3: ASYNCHRONOUS HANDOVER
//                // SEDA immediately releases the RabbitMQ consumer thread for the next message.
//                .to("seda:asyncProcessor?concurrentConsumers=" + ASYNC_PROCESSORS + "&waitForTaskToComplete=Never");
//
//
//        // --- 4. SEDA Asynchronous Processing Route (BLOCKING JMS CALL) ---
//        from("seda:asyncProcessor")
//                .routeId("AsyncJmsSender")
//
//                .process(exchange -> {
//                    // Core Business Logic
//                    String messageBody = exchange.getIn().getBody(String.class);
//                    System.out.println("Processing RabbitMQ Message: " + messageBody);
//                    exchange.getIn().setBody("Processed: " + messageBody);
//                })
//
//                // CRITICAL: Send the message to the WebLogic Queue (blocking call)
//                .to("weblogicJMS:" + OUTPUT_QUEUE)
//
//                // Tweak 4: SUCCESS ACK
//                // If the JMS call succeeds, explicitly ACK the message to RabbitMQ.
//                // This is necessary because autoAck=false.
//                .process(e -> e.getMessage().setHeader("rabbitmq.ack", true))
//                .log("Message successfully sent to WebLogic JMS Queue & ACKed.");
//
//
//        // --- 5. Failure Route (DLC Target for Broker Final Failure) ---
//        // This route is triggered by the onException handler for non-JMS errors.
//        // If the DLX pattern is fully implemented, this route would consume from the DLQ.
//        from(FAILURE_ROUTE)
//                .routeId("JmsFailureToOAuthRest")
//                .log("FINAL FAILURE: After all retries/failures. Calling protected REST endpoint.")
//
//                // CRITICAL: Save the original body before token retrieval overwrites it
//                .setHeader("OriginalBody", body())
//
//                // Call the route that handles OAuth token retrieval
//                .to("direct:getOAuthToken")
//                .log("Retrieved OAuth Token. Proceeding to API call.")
//
//                // --- STEP 2: Call Protected API with Token ---
//                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
//                .setHeader("Authorization", simple("Bearer ${header.OAuthToken}"))
//
//                // Restore the original message body for the API call payload
//                .setBody(header("OriginalBody"))
//
//                // Call the final protected endpoint
//                .toD(NOTIFICATION_API)
//                .log("Successfully called protected REST endpoint on JMS failure.");
//
//        // --- 6. OAuth Token Retrieval Sub-Route (Keep As Is, it's efficient) ---
//        from("direct:getOAuthToken")
//                .routeId("GetOAuthToken")
//                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
//
//                // Construct the request body for Client Credentials Grant
//                .setBody(simple("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET))
//
//                // Call the Authorization Server's Token Endpoint
//                .to(AUTH_TOKEN_URL)
//
//                // Extract the access_token from the JSON response
//                .unmarshal().json()
//                .transform().jsonpath("$.access_token")
//
//                // Store the token in a header
//                .setHeader("OAuthToken", body())
//
//                // Clear the body
//                .setBody(constant(null));
//    }
//}
//
////package com.muhammadallee.cameldemo;
////
////import org.apache.camel.Exchange;
////import org.apache.camel.ExchangePattern;
////import org.apache.camel.builder.RouteBuilder;
////import org.springframework.jms.JmsException;
////import org.springframework.stereotype.Component;
////
////@Component
////public class RabbitMqConsumerRoute extends RouteBuilder {
////
////    // Define the queue name you want to consume from
////    public static final String INPUT_QUEUE = "test_q";
////    private static final String EXCHANGE_NAME = "test_exchange";
////    private static final String ROUTING_KEY = "test_rkey";
////
////
////    private static final String OUTPUT_QUEUE = "TargetQ";
////
////    private static final String FAILURE_ROUTE = "direct:callRestOnJmsFailure";
////    private static final String NOTIFICATION_API = "http://localhost:3000/api";
////    private static final String AUTH_TOKEN_URL = "http://localhost:3001/auth/token";
////    private static final String CLIENT_SECRET = "cs";
////    private static final String CLIENT_ID = "ci";
////
////    @Override
////    public void configure() throws Exception {
////
////        // --- 1. Set up Dead Letter Channel (DLC) for JMS failures ---
////        // Any exception of type JmsException will stop redelivery attempts
////        // and send the message to the FAILURE_ROUTE.
////        onException(JmsException.class)
////                // 1. Set max attempts (5 retries = 6 total attempts: 1 original + 5 redeliveries)
////                .maximumRedeliveries(5)
////
////                // 2. Set a brief delay (e.g., 5 seconds) between retry attempts
////                .redeliveryDelay(5000L)
////
////                // 3. Log the retry attempts (optional, but helpful)
////                .onRedelivery(exchange -> {
////                    // Log details of the failed attempt
////                    log.warn("JMS send failed. Retrying attempt: ${header.CamelRedeliveryCounter} of 5. Error: ${exception.message}");
////                })
////
////                // 4. Mark the exception as handled only after retries fail
////                .handled(true)
////
////                // 5. Redirect the message to the failure route ONLY after all retries are done
////                .to(FAILURE_ROUTE);
////
////        // 1. Define the component URI
////        // spring-rabbitmq component uses the Spring Boot connection factory automatically.
////        // It consumes from the queue defined by the 'queue' option.
////        //from("spring-rabbitmq:test_exchange?queue=" + INPUT_QUEUE + "&routingKey=test_rkey")
////        // Use 'queues' (plural) to specify the queue name(s)
////        from("spring-rabbitmq:" + EXCHANGE_NAME + "?queues=" + INPUT_QUEUE + "&routingKey=" + ROUTING_KEY)
////                .routeId("RabbitMQConsumerRoute")
////                // ...
////                .log(">>> Received message from RabbitMQ: ${body}")
////                .process(exchange -> {
////                    // 2. Access the message body and headers
////                    String messageBody = exchange.getIn().getBody(String.class);
////
////                    // You can perform your core business logic here
////                    System.out.println("Processing RabbitMQ Message: " + messageBody);
////
////                    // Example: Transform or send to another endpoint
////                    exchange.getIn().setBody("Processed: " + messageBody);
////                })
////                // 3. Send the processed message to a final destination (e.g., a direct route or log)
////                //.to("log:rabbitmq-processor?showAll=true");
////                // CRITICAL: Send the message to the WebLogic Queue
////                // The format is: [componentName]:queue:[QueueJNDIName]
////                .to("weblogicJMS:" + OUTPUT_QUEUE)
////                .log("Message successfully sent to WebLogic JMS Queue.");
////
////// --- 2. Failure Route (DLC Target) ---
////        from(FAILURE_ROUTE)
////                .routeId("JmsFailureToOAuthRest")
////                // Now this log confirms the final failure after 5 retries.
////                .log("FINAL FAILURE: WebLogic JMS failed after 5 retries. Calling protected REST endpoint.")
////
////                // CRITICAL: Save the original body before token retrieval overwrites it
////                .setHeader("OriginalBody", body())
////
////                // Call the route that handles OAuth token retrieval
////                .to("direct:getOAuthToken")
////                .log("Retrieved OAuth Token. Proceeding to API call.")
////
////                // --- STEP 2: Call Protected API with Token ---
////                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
////                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
////                .setHeader("Authorization", simple("Bearer ${header.OAuthToken}"))
////
////                // Restore the original message body for the API call payload
////                .setBody(header("OriginalBody"))
////
////                // Call the final protected endpoint
////                .toD(NOTIFICATION_API) // Use toD if the URL contains dynamic elements, or to() otherwise
////                .log("Successfully called protected REST endpoint on JMS failure.");
////
////        // --- 3. OAuth Token Retrieval Sub-Route ---
////        from("direct:getOAuthToken")
////                .routeId("GetOAuthToken")
////                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
////                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
////
////                // Construct the request body for Client Credentials Grant
////                .setBody(simple("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET))
////
////                // Call the Authorization Server's Token Endpoint
////                .to(AUTH_TOKEN_URL)
////
////                // Extract the access_token from the JSON response
////                .unmarshal().json()
////                .transform().jsonpath("$.access_token")
////
////                // Store the token in a header
////                .setHeader("OAuthToken", body())
////
////                // Clear the body to prevent the token string from being used as the main payload
////                .setBody(constant(null));
////    }
////}