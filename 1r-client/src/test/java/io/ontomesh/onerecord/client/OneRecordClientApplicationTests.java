package io.ontomesh.onerecord.client;

import io.ontomesh.core.enums.ProtocolType;
import io.ontomesh.onerecord.client.messaging.NotificationFunctionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

import java.util.Map;

@SpringBootTest
public class OneRecordClientApplicationTests {

    @Test
    void contextLoads() {
    }


    @Test
    public void testNotificationMessage() {

        MessageHeaders messageHeaders = new MessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE, "text/plain"));
        String payload = """
                {
                    "@context": {
                        "api": "https://onerecord.iata.org/ns/api#"
                    },
                    "@type": "api:Notification",
                    "api:hasEventType": {
                        "@id": "api:LOGISTICS_OBJECT_CREATED"
                    },
                    "api:hasLogisticsObject": {
                        "@id": "https://1r.example.com/logistics-objects/1a8ded38-1804-467c-a369-81a411416b7c"
                    },
                    "api:hasLogisticsObjectType": {
                        "@type": "http://www.w3.org/2001/XMLSchema#anyURI",
                        "@value": "https://onerecord.iata.org/ns/cargo#Piece"
                    },
                    "api:isTriggeredBy": {
                        "@id": "https://1r.example.com/action-requests/599fea49-7287-42af-b441-1fa618d2aaed"
                    }
                }
                """;
        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("protocol", ProtocolType.EMAIL.getValue())
                .setHeader(KafkaHeaders.KEY, null)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
                .setHeader("from", null)
                .setHeader("to", "http://server.ontomesh.io:8085")
                .build();

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                TestChannelBinderConfiguration.getCompleteConfiguration(
                        OneRecordClientApplication.class, NotificationFunctionConfiguration.class))
                .run("--spring.cloud.function.definition=consumeNotification")) {

            InputDestination source = context.getBean(InputDestination.class);
            source.send(message);
        }
    }
}
