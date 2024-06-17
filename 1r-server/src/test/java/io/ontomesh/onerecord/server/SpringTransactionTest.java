package io.ontomesh.onerecord.server;

import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpringTransactionTest {

    private final LogisticsObjectRepository repository;


    public SpringTransactionTest(LogisticsObjectRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void update(IRI subject, IRI predicate, Value oldValue, Value newValue) {
        int delete = this.repository.delete(subject, predicate, oldValue);
        this.repository.add(subject, predicate, newValue);
        if (delete == 1)
            throw new RuntimeException();
    }

}
