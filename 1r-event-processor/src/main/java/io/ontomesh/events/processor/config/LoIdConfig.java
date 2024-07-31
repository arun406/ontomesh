package io.ontomesh.events.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server")
public class LoIdConfig {
    private String internalIriScheme;

    public String getInternalIriScheme() {
        return internalIriScheme;
    }

    public void setInternalIriScheme(String internalIriScheme) {
        this.internalIriScheme = internalIriScheme;
    }
}
