package io.ontomesh.onerecord.server.services.logisticsobjects.snapshot;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.common.OntoMeshId;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import io.ontomesh.onerecord.server.services.logisticsobjects.AbstractLogisticsObjectService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
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

    private final ValueFactory valueFactory = SimpleValueFactory.getInstance();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyDDmm'T'hhmmss'Z'")
            .withZone(ZoneId.from(ZoneOffset.UTC));

    public SnapshotService(LogisticsObjectRepository repository, IdProvider idProvider) {
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

        IRI snapshotId = valueFactory.createIRI(logisticsObject.iri().stringValue(), String.format("?at=%s", snapshotAt));

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
        this.getRepository().persist(snapshot.model());

        return snapshot;
    }

    /**
     * find the logistics object snapshot
     *
     * @param loIRI
     * @param at
     * @return
     */
    public LogisticsObject findSnapshot(IRI loIRI, String at) {

        IRI snapshotIRI = valueFactory.createIRI(loIRI.stringValue(), String.format("?at=%s", at));

        return this.getRepository()
                .findLoById(snapshotIRI, (iri) -> OntoMeshId.isValidIri(iri, getIdProvider().getLogisticsObjectBaseIRI()))
                .orElseThrow(() -> new LogisticsObjectNotFoundException(snapshotIRI));
    }
}
