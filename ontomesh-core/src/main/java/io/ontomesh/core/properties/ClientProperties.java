package io.ontomesh.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "onerecord.client"
)
@Data
public class ClientProperties {
    private String id;
    private String eventsTopic;
    private String eventTopicGroupId;
}
