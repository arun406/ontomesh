package io.ontomesh.onerecord.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "one-record.server")
public class LoIdConfig {
    String host;
    int port;
    String scheme;
    String internalIriScheme;
}
