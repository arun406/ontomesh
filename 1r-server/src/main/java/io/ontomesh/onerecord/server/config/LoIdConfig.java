package io.ontomesh.onerecord.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "lo")
public class LoIdConfig {
    String host;
    int port;
    String scheme;
    String internalIriScheme;
}
