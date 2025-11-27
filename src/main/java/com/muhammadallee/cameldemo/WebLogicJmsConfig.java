//package com.muhammadallee.cameldemo;
//
//import java.util.Properties;
//import javax.jms.ConnectionFactory;
//import javax.naming.Context; // <-- NEW IMPORT ADDED
//import javax.naming.NamingException;
//
//import org.apache.camel.component.jms.JmsComponent;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jms.support.destination.JndiDestinationResolver;
//import org.springframework.jndi.JndiObjectFactoryBean;
//import org.springframework.jndi.JndiTemplate;
//
//@Configuration
//public class WebLogicJmsConfig {
//
//    // Inject the provider URL from application.properties or environment variables
//    @Value("${weblogic.jms.provider-url}")
//    private String wlProviderUrl;
//
//    // --- WebLogic JNDI Configuration ---
//    private static final String WL_INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory";
//    private static final String WL_CF_JNDI_NAME = "jms/TargetCF"; // The default CF name
//
//    /**
//     * Programmatically creates the JNDI template necessary for looking up
//     * resources on the WebLogic Server. This bean is used by the
//     * DynamicConnectionFactoryRegistrar for all other CF lookups.
//     */
//    @Bean
//    public JndiTemplate weblogicJndiTemplate() {
//        Properties environment = new Properties();
//        // FIX: Use standard JNDI constants from javax.naming.Context
//        environment.put(Context.INITIAL_CONTEXT_FACTORY, WL_INITIAL_CONTEXT_FACTORY);
//        environment.put(Context.PROVIDER_URL, this.wlProviderUrl);
//
//        return new JndiTemplate(environment);
//    }
//
//    /**
//     * Looks up the default ConnectionFactory from WebLogic using the
//     * JndiTemplate created above. This bean is used by all flows that
//     * OMIT the 'connectionFactoryJndi' property in endpoints.json.
//     */
//    @Bean("defaultWeblogicConnectionFactory")
//    public ConnectionFactory defaultWeblogicConnectionFactory(JndiTemplate weblogicJndiTemplate) throws NamingException {
//        JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
//        factoryBean.setJndiTemplate(weblogicJndiTemplate);
//        factoryBean.setJndiName(WL_CF_JNDI_NAME);
//
//        factoryBean.afterPropertiesSet();
//
//        return (ConnectionFactory) factoryBean.getObject();
//    }
//
//    /**
//     * Defines the Camel weblogicJMS component using the default ConnectionFactory.
//     * Custom CFs are injected dynamically via the route URI (e.g., "?connectionFactory=#cfHighPriorityBean").
//     */
//    @Bean("weblogicJMS")
//    public JmsComponent weblogicJMS(ConnectionFactory defaultWeblogicConnectionFactory) {
//        JmsComponent component = JmsComponent.jmsComponentAutoAcknowledge(defaultWeblogicConnectionFactory);
//
//        // Use the JndiTemplate for resolving destination queues (JMS/WebLogic/TARGET)
//        JndiDestinationResolver resolver = new JndiDestinationResolver();
//        resolver.setJndiTemplate(weblogicJndiTemplate());
//        component.setDestinationResolver(resolver);
//
//        // Configuration for the JmsComponent
//        component.setTransacted(false);
//        component.setAcknowledgementModeName("CLIENT_ACKNOWLEDGE");
//        component.setCacheLevel(5);
//
//        return component;
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
////
////import org.apache.camel.component.jms.JmsComponent;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.jndi.JndiObjectFactoryBean;
////
////import javax.jms.ConnectionFactory;
////
////@Configuration
////public class JmsConfig {
////    private static final String JMS_CF_JNDI_NAME = "TargetCF";
////
////    // Assuming you have a ConnectionFactory registered in JNDI
////    // Spring Boot will often auto-detect and expose the JNDI CF if you set the properties above.
////
////    // Manually register the JMS component named "wmqJMS" (or any name you choose)
//////    @Bean("weblogicJMS")
//////    public JmsComponent weblogicJMS(ConnectionFactory connectionFactory) {
//////        JmsComponent component = new JmsComponent();
//////        component.setConnectionFactory(connectionFactory);
//////        return component;
//////    }
////
////
////    @Bean(name = "weblogicConnectionFactory")
////    public JndiObjectFactoryBean connectionFactory() {
////        JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
////        // Set the JNDI name as configured in WebLogic
////        factory.setJndiName(JMS_CF_JNDI_NAME);
////        // Ensures the JNDI name is resolved relative to the webapp's context (java:comp/env)
////        factory.setResourceRef(true);
////        factory.setProxyInterface(ConnectionFactory.class);
////        return factory;
////    }
////
////    /**
////     * 2. Configure the Camel JmsComponent using the manually looked-up bean.
////     * Note: We use the injected ConnectionFactory directly.
////     */
////    @Bean("weblogicJMS")
////    public JmsComponent weblogicJMS(ConnectionFactory weblogicConnectionFactory) {
////        JmsComponent component = JmsComponent.jmsComponentAutoAcknowledge(weblogicConnectionFactory);
////        component.setTransacted(true);
////        component.setAcknowledgementModeName("AUTO_ACKNOWLEDGE");
////        component.setCacheLevel(5);
////
////        // CRITICAL: Force the component to use JNDI for destination lookup
////        component.setDestinationResolver(new org.springframework.jms.support.destination.JndiDestinationResolver());
////        // You can add further configuration here if needed, e.g., error handling
////        return component;
////    }
////}