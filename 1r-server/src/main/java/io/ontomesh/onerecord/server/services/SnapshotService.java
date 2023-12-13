package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.api.IdProvider;
import io.ontomesh.onerecord.server.api.OntoMeshId;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.SnapshotNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SnapshotService extends AbstractLogisticsObjectService {
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyDDmm'T'hhmmss'Z'")
            .withZone(ZoneId.from(ZoneOffset.UTC));
    private final ValueFactory valueFactory = SimpleValueFactory.getInstance();

    public SnapshotService(LogisticsObjectRepository repository,
                           IdProvider idProvider) {
        super(repository, idProvider);
    }

    /**
     * Clone the Model
     *
     * @return logisticsObject
     */
    public LogisticsObject createSnapshot(LogisticsObject logisticsObject) {
        Model oldModel = logisticsObject.model();
        String snapshotAt = dateTimeFormatter.format(Instant.now());
        Literal at = Values.literal(snapshotAt);
        IRI snapshotId = valueFactory.createIRI(logisticsObject.iri().stringValue(), "?at=" + snapshotAt);
        Model newModel = new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .setNamespace(CARGO.PREFIX, CARGO.NAMESPACE)
                .subject(snapshotId)
                .add(OntoMesh.hasSnapshotDate, at)
                .add(RDF.TYPE, logisticsObject.type())
                .build();

        for (Statement statement : oldModel) {
            if (statement.getPredicate().isIRI() &&
                    isTypePredicate(statement.getPredicate()) == 0) {
                continue;
            }
            Statement newStatement = Values.getValueFactory()
                    .createStatement(snapshotId, statement.getPredicate(), statement.getObject());
            newModel.add(newStatement);
        }
        newModel.add(snapshotId, API.hasLogisticsObject, valueFactory.createLiteral(logisticsObject.iri().stringValue()));

        LogisticsObject snapshot = new LogisticsObject(snapshotId, logisticsObject.type(), newModel);
        this.repository.persist(snapshot.iri(), snapshot.model());
//        print(snapshot, RDFFormat.JSONLD);
        return snapshot;
    }

    public LogisticsObject findSnapshot(IRI loIRI, String at) {
        IRI snapshotIRI = valueFactory.createIRI(loIRI.stringValue(), "?at=" + at);
        return this.repository.findLoById(snapshotIRI,
                        (iri) -> OntoMeshId.isValidIri(iri, idProvider.getLogisticsObjectBaseIRI()))
                .orElseThrow(() -> new SnapshotNotFoundException("Snapshot not exists."));
    }
}
