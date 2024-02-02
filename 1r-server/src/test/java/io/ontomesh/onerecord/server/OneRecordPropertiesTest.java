package io.ontomesh.onerecord.server;

import io.ontomesh.onerecord.server.config.OneRecordProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class OneRecordPropertiesTest {

    @Autowired
    private OneRecordProperties oneRecordProperties;

    @Test
    public void testProperties() {
        log.info("{}", oneRecordProperties.toString());
    }
}
