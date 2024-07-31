package io.ontomesh.onerecord.server;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.common.OntoMesh;
import io.ontomesh.onerecord.server.converter.LogisticsObjectConverterService;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.SnapshotService;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@SpringBootTest
@Slf4j
public class SnapshotTest {

    private final IdProvider idProvider;
    private final SnapshotService snapshotService;
    private final LogisticsObjectConverterService converterService;

    public SnapshotTest(IdProvider idProvider, SnapshotService snapshotService, LogisticsObjectConverterService converterService) {
        this.idProvider = idProvider;
        this.snapshotService = snapshotService;
        this.converterService = converterService;
    }

    @BeforeAll
    static void setup() {
        log.info("@BeforeAll - executes once before all test methods in this class");
    }

    @BeforeEach
    void init() {
        log.info("@BeforeEach - executes before each test method in this class");
    }

    @AfterEach
    void tearDown() {
        log.info("@AfterEach - executed after each test method.");
    }

    @AfterAll
    static void done() {
        log.info("@AfterAll - executed after all test methods.");
    }

    @Test
    public void testSnapshot() throws IOException {
        File file = ResourceUtils.getFile("classpath:piecelo.jsonld");
        InputStream stream = new FileInputStream(file);

        LogisticsObject logisticsObject = converterService.getLogisticsObject(stream, RDFFormat.JSONLD);
        LogisticsObject snapshot = snapshotService.createSnapshot(logisticsObject);

        RDFUtil.print(snapshot.model(), RDFFormat.TURTLE);
    }

    @DisplayName("Single test successful")
    @Test
    void testSingleSuccessTest() throws IOException {
        File file = ResourceUtils.getFile("classpath:piecelo.jsonld");
        InputStream stream = new FileInputStream(file);

        LogisticsObject logisticsObject = converterService.getLogisticsObject(stream, RDFFormat.JSONLD);

        IRI snapshotId = idProvider.createUniqueLoUri().iri();
        log.debug("snapshot id {}", snapshotId);

        Model newModel = new ModelBuilder()
                .setNamespace("cargo", CARGO.NAMESPACE)
                .subject(snapshotId)
                .add(RDF.TYPE, CARGO.Piece)
                .build();
        //copy

        for (Statement statement : logisticsObject.model()) {
            if (statement.getPredicate().isIRI() &&
                    isEqual(statement.getPredicate()) == 0) {
                continue;
            }

//            Resource subject = statement.getSubject();
            IRI predicate = statement.getPredicate();
//            Object object = statement.getObject();
            // Create a new statement with the same subject, predicate, and object.
            Statement newStatement = Values.getValueFactory().createStatement(snapshotId, predicate, statement.getObject());

            newModel.add(newStatement);
        }
        newModel.add(snapshotId, API.hasLogisticsObject, logisticsObject.iri());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyDDmm'T'hhmmss")
                .withZone(ZoneId.from(ZoneOffset.UTC));
        String dateTime = dateTimeFormatter.format(Instant.now());
        log.debug("snapshot datetime : {}", dateTime);
        Literal at = Values.literal(dateTime);
        newModel.add(snapshotId, OntoMesh.hasSnapshotDate, at);
        RDFUtil.print(logisticsObject.model(), RDFFormat.TURTLE);
//        RDFUtil.print(logisticsObject.model(), RDFFormat.JSONLD);
        System.out.println("--------");
        RDFUtil.print(newModel, RDFFormat.TURTLE);
//        RDFUtil.print(newModel, RDFFormat.JSONLD);
        log.info("Success");

    }

    private int isEqual(IRI iri1) {
        return iri1.toString().compareTo(RDF.TYPE.toString());
    }
}
