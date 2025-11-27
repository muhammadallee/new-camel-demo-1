package com.muhammadallee.cameldemo;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // Make sure this is a configuration class
public class CamelServletConfig {

    // IMPORTANT: The name of this bean MUST match the component name in restConfiguration()
    // It is critical that the name is "servlet"
    @Bean
    public ServletRegistrationBean camelServlet() {
        ServletRegistrationBean registration = new ServletRegistrationBean(
                new CamelHttpTransportServlet() // The actual Camel servlet class
        );
        registration.setName("servlet"); // Set the bean name to "servlet"
        registration.setLoadOnStartup(1);
        return registration;
    }
}