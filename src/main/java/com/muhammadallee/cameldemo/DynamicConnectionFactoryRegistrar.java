//package com.muhammadallee.cameldemo;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
//import org.springframework.beans.factory.support.RootBeanDefinition;
//import org.springframework.context.EnvironmentAware;
//import org.springframework.core.env.Environment;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.ResourceLoader;
//import org.springframework.jndi.JndiObjectFactoryBean;
//import org.springframework.stereotype.Component;
//
//import java.io.InputStream;
//import java.util.HashSet;
//import java.util.Set;
//
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver; // <-- UPDATED IMPORT
//
//import javax.jms.ConnectionFactory; // Added missing import for ConnectionFactory
//
///**
// * Reads the endpoints.json during Spring context initialization and dynamically
// * registers a dedicated JndiObjectFactoryBean for every unique WebLogic
// * Connection Factory (CF) JNDI name found in the configuration.
// */
//@Component
//public class DynamicConnectionFactoryRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
//
//    private Environment environment;
//
//    // We must manually load the config file before the BeanFactory is fully set up
//    private static final String CONFIG_FILE_PATH = "classpath:config/endpoints.json";
//    private static final String TEMPLATE_BEAN_NAME = "weblogicJndiTemplate";
//    private static final String DEFAULT_CF_JNDI = "jms/TargetCF";
//
//    @Override
//    public void setEnvironment(Environment environment) {
//        this.environment = environment;
//    }
//
//    @Override
//    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
//        Set<String> cfJndiNames = extractUniqueConnectionFactoryJndiNames();
//
//        for (String jndiName : cfJndiNames) {
//            // Only register dynamic beans for specific CFs, not the default one
//            if (jndiName.equals(DEFAULT_CF_JNDI)) {
//                continue;
//            }
//
//            String beanName = sanitizeJndiNameToBeanName(jndiName);
//            registerConnectionFactoryBean(registry, beanName, jndiName);
//        }
//
//        // Ensure the default ConnectionFactory type is known if it was skipped above
//        if (!registry.containsBeanDefinition(DEFAULT_CF_JNDI)) {
//            // No need to explicitly register, as WebLogicJmsConfig will do it,
//            // but we ensure the JndiTemplate dependency is correctly handled
//            // by setting it as an argument in the other methods.
//        }
//    }
//
//    @Override
//    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//        // Required for BeanDefinitionRegistryPostProcessor contract, but implementation is in the other method.
//    }
//
//    /**
//     * Reads the endpoints.json file manually to extract the required CF JNDI names.
//     */
//    private Set<String> extractUniqueConnectionFactoryJndiNames() {
//        Set<String> jndiNames = new HashSet<>();
//        ObjectMapper mapper = new ObjectMapper();
//
//        try {
//            // FIX: Use the concrete implementation PathMatchingResourcePatternResolver
//            ResourceLoader loader = new PathMatchingResourcePatternResolver();
//            Resource resource = loader.getResource(CONFIG_FILE_PATH);
//
//            try (InputStream is = resource.getInputStream()) {
//                JsonNode root = mapper.readTree(is);
//                JsonNode flows = root.get("flows");
//
//                if (flows != null && flows.isArray()) {
//                    for (JsonNode flow : flows) {
//                        JsonNode cfNode = flow.at("/destination/connectionFactoryJndi");
//                        if (cfNode != null && !cfNode.isNull() && !cfNode.asText().isEmpty()) {
//                            jndiNames.add(cfNode.asText());
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            // Log this exception: It means the configuration file could not be read.
//            System.err.println("FATAL: Could not read configuration file for dynamic bean registration: " + e.getMessage());
//        }
//        return jndiNames;
//    }
//
//    /**
//     * Registers a new Spring bean definition for a Connection Factory lookup.
//     */
//    private void registerConnectionFactoryBean(BeanDefinitionRegistry registry, String beanName, String jndiName) {
//        if (registry.containsBeanDefinition(beanName)) {
//            return; // Avoid duplicate registration
//        }
//
//        // Define the bean using JndiObjectFactoryBean
//        RootBeanDefinition beanDefinition = new RootBeanDefinition(JndiObjectFactoryBean.class);
//
//        // Use the existing JndiTemplate bean to configure this lookup.
//        // This links the dynamically created CF bean to the T3 configuration in WebLogicJmsConfig.java.
//        // We get the JndiTemplate definition by name (TEMPLATE_BEAN_NAME).
//        beanDefinition.getPropertyValues().add("jndiTemplate", registry.getBeanDefinition(TEMPLATE_BEAN_NAME));
//
//        // Set the specific JNDI name for the Connection Factory
//        beanDefinition.getPropertyValues().add("jndiName", jndiName);
//
//        // Required Type for injection
//        beanDefinition.getPropertyValues().add("expectedType", ConnectionFactory.class);
//
//        registry.registerBeanDefinition(beanName, beanDefinition);
//        System.out.println("Dynamically registered Connection Factory bean: #" + beanName + " for JNDI: " + jndiName);
//    }
//
//    /**
//     * Converts a JNDI name (e.g., jms/CF_HIGH_PRIORITY) into a safe Spring Bean Name (cfHighPriorityBean).
//     */
//    private String sanitizeJndiNameToBeanName(String jndiName) {
//        String sanitized = jndiName.replaceAll("[^a-zA-Z0-9]", "");
//        // Lowercase first letter and prepend a prefix for clarity
//        return "cf" + sanitized.substring(0, 1).toUpperCase() + sanitized.substring(1) + "Bean";
//    }
//}