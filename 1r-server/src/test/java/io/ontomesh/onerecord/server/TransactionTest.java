package io.ontomesh.onerecord.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@SpringBootTest
@EnableTransactionManagement
public class TransactionTest {
    @Autowired
    private SpringTransactionTest test;

    @Test
    public void testTransaction() {
        test.update(
                Values.iri("http://1r.ontomesh.io/logistics-objects/adf4abf1-a212-4354-be7e-e86931158c14"),
                Values.iri("https://onerecord.iata.org/ns/cargo#coload"), Values.literal(true), Values.literal(false));
    }
}
