//package com.muhammadallee.cameldemo;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jms.JmsException;
//import org.springframework.stereotype.Component;
//
//@Component
//public class MyRouteControllerBuilder extends RouteBuilder {
//    @Autowired
//    private JsonNode endpointConfiguration;
//
//    /**
//     * Converts a JNDI name (e.g., jms/CF_HIGH_PRIORITY) into a safe Spring Bean Name (cfHighPriorityBean).
//     * Must match the logic in DynamicConnectionFactoryRegistrar.
//     */
//    private String sanitizeJndiNameToBeanName(String jndiName) {
//        String sanitized = jndiName.replaceAll("[^a-zA-Z0-9]", "");
//        return "cf" + sanitized.substring(0, 1).toUpperCase() + sanitized.substring(1) + "Bean";
//    }
//
//    @Override
//    public void configure() throws Exception {
//
//        // --- Load Global Configuration ---
//        JsonNode globalConfig = endpointConfiguration.get("globalConfig");
//
//        final String AUTH_TOKEN_URL = globalConfig.get("globalOAuthTokenUri").asText();
//        final String CLIENT_SECRET = globalConfig.get("globalClientSecret").asText();
//        final String CLIENT_ID = globalConfig.get("globalClientId").asText();
//        final int ASYNC_PROCESSORS = globalConfig.get("defaultAsyncPoolSize").asInt(100);
//        final String GLOBAL_NOTIFICATION_API = globalConfig.get("globalNotificationApiUri").asText();
//        final String GLOBAL_OAUTH_SCOPE = globalConfig.get("globalOAuthScope").asText();
//
//
//        // --- 1. GLOBAL EXCEPTION AND ERROR ROUTE DEFINITIONS (MUST BE FIRST) ---
//
//        JsonNode flows = endpointConfiguration.get("flows");
//        if (flows == null || !flows.isArray()) {
//            throw new IllegalArgumentException("JSON configuration must contain a 'flows' array.");
//        }
//
//        for (JsonNode flow : flows) {
//            String flowId = flow.get("flowId").asText();
//            String targetJndiName = flow.at("/destination/jndiName").asText();
//            String failureRoute = "direct:callRestOnJmsFailure_" + flowId;
//
//            // Catch all exceptions (including JmsException) and route to final failure path.
//            onException(Exception.class)
//                    .onWhen(header("CamelRouteId").isEqualTo("AsyncJmsSender_" + flowId))
//                    .maximumRedeliveries(0)
//                    .handled(true)
//                    .log(org.apache.camel.LoggingLevel.ERROR, "Flow " + flowId + ": JMS delivery failed for " + targetJndiName + ". Routing to failure handler.")
//                    .to(failureRoute)
//                    .end();
//        }
//
//
//        // --- 2. OAUTH TOKEN RETRIEVAL SUB-ROUTE ---
//        from("direct:getOAuthToken")
//                .routeId("GetOAuthToken")
//                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
//                .setBody(simple("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_ID))
//                .to(AUTH_TOKEN_URL)
//                .unmarshal().json()
//                .transform().jsonpath("$.access_token")
//                .setHeader("OAuthToken", body())
//                .setBody(constant(null));
//
//
//        // --- 3. DYNAMICALLY CONFIGURE ALL MAIN AND FAILURE ROUTES ---
//
//        for (JsonNode flow : flows) {
//            String flowId = flow.get("flowId").asText();
//            String exchangeName = flow.at("/source/exchangeName").asText();
//            String inputQueue = flow.at("/source/queueName").asText();
//            String routingKey = flow.at("/source/routingKey").asText();
//            int consumers = flow.at("/source/concurrentConsumers").asInt(10);
//            String targetJndiName = flow.at("/destination/jndiName").asText();
//            String connectionFactoryJndi = flow.at("/destination/connectionFactoryJndi").asText();
//
//            String sedaUri = "seda:asyncProcessor_" + flowId;
//            String failureRoute = "direct:callRestOnJmsFailure_" + flowId;
//
//            String RABBIT_URI = "spring-rabbitmq:" + exchangeName +
//                    "?queues=" + inputQueue +
//                    "&routingKey=" + routingKey +
//                    "&concurrentConsumers=" + consumers;
//
//            String WEBLOGIC_JMS_URI = "weblogicJMS:" + targetJndiName;
//
//            if (connectionFactoryJndi != null && !connectionFactoryJndi.isEmpty()) {
//                // IMPORTANT CHANGE: Reference the dynamically generated bean using the '#' prefix
//                String beanName = sanitizeJndiNameToBeanName(connectionFactoryJndi);
//                WEBLOGIC_JMS_URI += "?connectionFactory=#" + beanName;
//            }
//
//
//            // c) Final manual failure route
//            from(failureRoute)
//                    .routeId("JmsFailureToOAuthRest_" + flowId)
//                    .log("Flow " + flowId + ": JMS Failure. Calling global notification API.")
//
//                    .doTry()
//                    .setHeader("OriginalBody", body())
//                    .to("direct:getOAuthToken")
//                    .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
//                    .setHeader("Authorization", simple("Bearer ${header.OAuthToken}"))
//                    .setHeader("X-OAuth-Scope", constant(GLOBAL_OAUTH_SCOPE))
//                    .setBody(header("OriginalBody"))
//                    .toD(GLOBAL_NOTIFICATION_API)
//                    .log("Flow " + flowId + ": Successfully notified monitoring service.")
//                    .doCatch(Exception.class)
//                    .log(org.apache.camel.LoggingLevel.ERROR, "Flow " + flowId + ": FAILED to call notification REST API. Proceeding to DLQ: ${exception.message}")
//                    .endDoTry()
//
//                    .process(e -> e.getMessage().setHeader("rabbitmq.reject", true))
//                    .log("Flow " + flowId + ": Message rejected, routed by broker to DLX/DLQ.");
//
//
//            // --- 4. Main RabbitMQ Consumer Route (FAST HANDOVER) ---
//            from(RABBIT_URI)
//                    .routeId("RabbitMQFastConsumer_" + flowId)
//                    .log("Flow " + flowId + ": Received message from RabbitMQ. Handover to SEDA.")
//
//                    .to(sedaUri + "?concurrentConsumers=" + ASYNC_PROCESSORS + "&waitForTaskToComplete=Never");
//
//
//            // --- 5. SEDA Asynchronous Processing Route (BLOCKING JMS CALL) ---
//            from(sedaUri)
//                    .routeId("AsyncJmsSender_" + flowId)
//
//                    .log("Flow " + flowId + ": Attempting AS-IS send to WebLogic JMS: " + targetJndiName)
//
//                    .to(WEBLOGIC_JMS_URI)
//
//                    // SUCCESS ACK
//                    .process(e -> e.getMessage().setHeader("rabbitmq.ack", true))
//                    .log("Flow " + flowId + ": Message successfully sent and ACKed.");
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
////package com.muhammadallee.cameldemo;
////
////import org.apache.camel.builder.RouteBuilder;
////import org.springframework.stereotype.Component;
////
////@Component
////public class MyRouteControllerBuilder extends RouteBuilder {
////
////    @Override
////    public void configure() throws Exception {
////        restConfiguration()
////                .component("servlet") // Or "jetty", "netty-http", etc.
////                .contextPath("/")
////                ;//.port(8080); // Optional: specify a port if not using default or Spring Boot config
////
////        rest("/") // Define a base path for your REST services
////                .get("/hello") // Define a GET endpoint at /api/hello
////                .to("direct:processHello") // Route to a direct endpoint for processing
////                .post("/data") // Define a POST endpoint at /api/data
////                .type(String.class) // Optional: specify input type for automatic unmarshalling
////                .to("direct:processData");
////
////        from("direct:processHello")
////                .transform().constant("Hello from Camel REST!"); // Simple response
////
////        from("direct:processData")
////                .log("Received data: ${body}")
////                // Add your business logic here
////                .transform().constant("Data processed successfully!");
////    }
////}
