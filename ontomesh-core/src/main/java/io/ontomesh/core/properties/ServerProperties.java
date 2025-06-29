package io.ontomesh.core.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onerecord.server")
@Data
@ToString
public class ServerProperties {
    private String internalIriScheme;
    private String url;
    private String identifier;
}
