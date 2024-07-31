package io.ontomesh.events.processor.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


@Configuration
@ConfigurationProperties
@PropertySources({
        @PropertySource(value = "classpath:application-server.yml", factory = MultipleYamlPropertySourceFactory.class)})
public class MultipleYamlConfiguration {
    private LoIdConfig loIdConfig;
}
