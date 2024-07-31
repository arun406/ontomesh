package io.ontomesh.events.processor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EventProcessorApplication.class)
@Import({TestChannelBinderConfiguration.class})
class ApplicationTests {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Test
    void contextLoads() {
        MessageHeaders messageHeaders = new MessageHeaders(
                Map.of("x-name", "arun", MessageHeaders.CONTENT_TYPE, "application/ld+json"));

        GenericMessage<byte[]> message = new GenericMessage<>("hello".getBytes(), messageHeaders);
        input.send(message);
        assertThat(output.receive().getPayload()).isEqualTo("hello".getBytes());
    }

}
