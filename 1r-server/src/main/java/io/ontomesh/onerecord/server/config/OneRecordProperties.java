package io.ontomesh.onerecord.server.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ToString
@Data
@Configuration
@ConfigurationProperties(prefix = "onerecord")
public class OneRecordProperties {

    private Server server;
    private Client client;

    @ToString
    @Data
    public static class Server {
        private String host;
        private int port;
        private String scheme;
        private String internalIriScheme;
        private Subscription subscription;
    }

    @ToString
    @Data
    public static class Subscription {
        private boolean sendLogisticsObjectBody;
        private boolean subscribeToLogisticsEvents;
        private boolean notifyRequestStatusChange;
        private String[] includeSubscriptionEventType;
    }

    @ToString
    @Data
    public static class Client {
        private String id;
        private String eventsTopic;
        private String eventTopicGroupId;
    }
}
