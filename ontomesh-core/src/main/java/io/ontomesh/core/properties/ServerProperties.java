package io.ontomesh.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "onerecord.server"
)
@Data
public class ServerProperties {
    private String host;
    private int port;
    private String scheme;
    private String internalIriScheme;
    private String url;
}
