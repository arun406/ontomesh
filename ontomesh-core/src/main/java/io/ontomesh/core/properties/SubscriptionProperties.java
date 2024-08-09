package io.ontomesh.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "onerecord.server.subscriptions"
)
@Data
public class SubscriptionProperties {
    private boolean sendLogisticsObjectBody;
    private boolean subscribeToLogisticsEvents;
    private boolean notifyRequestStatusChange;
    private String[] includeSubscriptionEventType;
}
