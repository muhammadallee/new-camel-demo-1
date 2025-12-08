package com.muhammadallee.cameldemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class WeblogicConnectionFactoryConfig {

    @Bean
    public Map<String, ConnectionFactory> weblogicConnectionFactories(MediatorConfig cfg) throws Exception {
        Map<String, ConnectionFactory> factories = new HashMap<>();

        for (SystemConfig sys : cfg.getSystems()) {
            InitialContext ctx = getCtx();
            ConnectionFactory cf =
                    (ConnectionFactory) ctx.lookup(sys.getWeblogicConnectionFactory());

            //ConnectionFactory targetCF = getTargetConnectionFactory(cf);
            factories.put(sys.getSystemName(), cf);
        }
        return factories;
    }

//    @Bean
//    public ConnectionFactory getTargetConnectionFactory(ConnectionFactory cf) throws Exception {
//        // 1. Look up the UNSECURED Connection Factory (TargetCF)
//
//        // 2. Wrap it with the Spring Adapter to enforce credentials on the connection
//        UserCredentialsConnectionFactoryAdapter securedCF = new UserCredentialsConnectionFactoryAdapter();
//        securedCF.setTargetConnectionFactory(rawCF);
//
//        // Set the credentials (using the values that are successfully injected for JNDI)
//        securedCF.setUsername("test_user");
//        securedCF.setPassword("test_pwd");
//
//        return securedCF; // This ConnectionFactory now carries the user/pass
//    }

    private static InitialContext getCtx() throws NamingException {
        Properties env = new Properties();
        //env.put(Context.INITIAL_CONTEXT_FACTORY, initialFactory);

        // --- CRITICAL ADDITIONS FOR SECURITY ---
        env.put(Context.SECURITY_PRINCIPAL, "test_user");
        env.put(Context.SECURITY_CREDENTIALS, "test_pwd");

        InitialContext ic = new InitialContext(env);

        return ic;
    }
}
