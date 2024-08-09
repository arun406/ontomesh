package io.ontomesh.core.config;

import io.ontomesh.core.properties.ClientProperties;
import io.ontomesh.core.properties.ServerProperties;
import io.ontomesh.core.properties.SubscriptionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OneRecordConfig {

    @Bean
    public ServerProperties serverProperties() {
        return new ServerProperties();
    }


    @Bean
    public ClientProperties clientProperties() {
        return new ClientProperties();
    }

    @Bean
    public SubscriptionProperties subscriptionProperties() {
        return new SubscriptionProperties();
    }
}
