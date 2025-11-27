//package com.muhammadallee.cameldemo;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.Resource;
//
//@Configuration
//public class EndpointConfigLoader {
//
//    @Value("classpath:route-config.json")
//    private Resource endpointsJson;
//
//    @Bean
//    public JsonNode endpointConfiguration() {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            return mapper.readTree(endpointsJson.getInputStream());
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to load endpoint configuration from JSON file.", e);
//        }
//    }
//}