package io.ontomesh.onerecord.server.util;

import io.ontomesh.core.rdf.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.common.LogOutputStream;
import io.ontomesh.onerecord.server.common.SemanticWebMediaType;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.InvalidApiRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.event.Level;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for performing RDF operations on a Model.
 *
 * @author arun
 */
@Slf4j
public class RDFUtil {

    private static final LogOutputStream logOutputStream = new LogOutputStream(log, Level.INFO);

    /**
     * Create IRI.
     *
     * @param iriString iri
     * @return IRI
     */
    public static IRI createIRI(String iriString) {
        ValueFactory vf = SimpleValueFactory.getInstance();
        try {
            // Try to convert the string to a URI first.
            URI uri = new URI(iriString);

            // If the URI is absolute, then return it as an IRI.
            if (uri.isAbsolute()) {
                return vf.createIRI(uri.toString());
            }
            // Otherwise, the URI is relative, so throw an exception.
            else {
                throw new IllegalArgumentException("Relative URIs are not supported: " + iriString);
            }
        } catch (URISyntaxException e) {
            // If the string is not a valid URI, then try to interpret it as a relative IRI.
            return vf.createIRI(RDF.NAMESPACE + iriString);
        }
    }

    /**
     * create IRI.
     *
     * @param uri uri
     * @return IRI
     */
    public static IRI createIRI(URI uri) {
        ValueFactory vf = SimpleValueFactory.getInstance();
        // Try to convert the string to a URI first.

        // If the URI is absolute, then return it as an IRI.
        if (uri.isAbsolute()) {
            return vf.createIRI(uri.toString());
        }
        // Otherwise, the URI is relative, so throw an exception.
        else {
            throw new IllegalArgumentException("Relative URIs are not supported: " + uri.toString());
        }
    }

    /**
     * Returns the type.
     *
     * @param model   Model.
     * @param subject subject.
     * @return IRI.
     */
    public static IRI getType(Model model, IRI subject) {
        return Models.objectIRI(model.filter(subject, RDF.TYPE, null))
                .orElseThrow(() -> new RuntimeException("Type is not available in the model."));
    }

    /**
     * readRdfToGraph.
     *
     * @param inputStream inputStream
     * @param rdfFormat   RDF Format.
     * @param baseUrl     Base Url.
     * @return collections.
     * @throws IOException
     */
    public static Collection<Statement> readRdfToGraph(final InputStream inputStream, final RDFFormat rdfFormat,
                                                       final String baseUrl) throws IOException {
        final RDFParser rdfParser = Rio.createParser(rdfFormat);
        final StatementCollector collector = new StatementCollector();
        rdfParser.setRDFHandler(collector);
        rdfParser.parse(inputStream, baseUrl);
        return collector.getStatements();
    }


    /**
     * Converts the Model to Collection of Statements
     *
     * @param model model
     * @return collection.
     */
    public static Collection<Statement> modelToCollection(Model model) {
        return new HashSet<>(model);
    }

    public static Model collectionToModel(Collection<Statement> statements) {
        DynamicModel model = new DynamicModelFactory().createEmptyModel();
        model.addAll(statements);
        return model;
    }

    /**
     * @param inputStream     rdf input stream.
     * @param inputRDFFormat  rdf format
     * @param outputRDFFormat rdf format
     * @param baseUrl         base url of the server.
     * @return rdf string
     * @throws IOException exception
     */
    public static String readRdfToString(InputStream inputStream, RDFFormat inputRDFFormat,
                                         RDFFormat outputRDFFormat, String baseUrl) throws IOException {

        Collection<Statement> statements = RDFUtil.readRdfToGraph(inputStream, inputRDFFormat, baseUrl);
        Model model = collectionToModel(statements);

        return new ModelToStringConverter(outputRDFFormat).convert(model);
    }

    /**
     * Returns RDF Type using Media type.
     *
     * @param mediaType Media Type
     * @return RDF Type
     */
    public static RDFFormat getFormat(MediaType mediaType) {

        if (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)) {
            return RDFFormat.JSONLD;
        } else if (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_TURTLE)
                || mediaType.isCompatibleWith(SemanticWebMediaType.TEXT_TURTLE)) {
            return RDFFormat.TURTLE;
        }
        throw new RuntimeException(STR."Content-Type or Accept  '\{mediaType}'is not supported");
    }

    public static RDFFormat getDefaultFormat() {
        return RDFFormat.JSONLD;
    }

    /**
     * Convert model to Byte Stream
     *
     * @param model     model.
     * @param mediaType media type
     * @return Byte Array Stream.
     */
    public static OutputStream getRDFByteStream(Model model, MediaType mediaType) {
        OutputStream outputStream = new ByteArrayOutputStream();
        RDFFormat format = RDFUtil.getFormat(mediaType);
        Rio.write(model, outputStream, format);
        return outputStream;
    }

    /**
     * Convert model to Byte Stream
     *
     * @param model model.
     * @return stream.
     */

    public static OutputStream getRDFByteStream(Model model) {
        OutputStream outputStream = new ByteArrayOutputStream();
        RDFFormat format = getDefaultFormat();
        Rio.write(model, outputStream, format);
        return outputStream;
    }

    public static void print(LogisticsObject logisticsObject) {
        print(logisticsObject.model());
    }

    /**
     * Print
     *
     * @param logisticsObject logistics object.
     * @param format          RDF Format.
     */
    public static void print(LogisticsObject logisticsObject, RDFFormat format) {
        print(logisticsObject.model(), format);
    }

    public static void print(Model model) {
        if (model == null) {
            return;
        }
        Rio.write(model, logOutputStream, RDFFormat.TURTLE);
    }

    public static void print(Model model, RDFFormat format) {
        if (model == null) {
            return;
        }
        Rio.write(model, logOutputStream, format);
    }

    public static Set<IRI> getObjects(IRI subject, IRI predicate, Model model) {
        Set<IRI> iris = findObjects(subject, predicate, model);
        if (iris.isEmpty()) {
            throw new NoSuchElementException();
        }
        return iris;
    }

    public static Set<IRI> findObjects(IRI subject, IRI predicate, Model model) {
        return Models.objectIRIs(model.filter(subject, predicate, null));
    }

    public static IRI getObject(IRI subject, IRI predicate, Model model) {
        return findObject(subject, predicate, model)
                .orElseThrow(NoSuchElementException::new);
    }

    public static Optional<IRI> findObject(IRI subject, IRI predicate, Model model) {
        return Models.objectIRI(model.filter(subject, predicate, null));
    }

    public static Literal getObjectLiteral(IRI subject, IRI predicate, Model model) {
        return findObjectLiteral(subject, predicate, model)
                .orElseThrow(NoSuchElementException::new);
    }

    public static Optional<Literal> findObjectLiteral(IRI subject, IRI predicate, Model model) {
        return Models.objectLiteral(model.filter(subject, predicate, null));
    }


    public static IRI getRootIri(Model model) {
        // set of all subject IRIs
        Set<IRI> subjectIris = Models.subjectIRIs(model);
        // set of all object IRIs
        Set<IRI> objectIris = Models.objectIRIs(model);

        // subtract set of objects from set of subjects
        subjectIris.removeAll(objectIris);

        // if more
        if (subjectIris.size() != 1) {
            throw new InvalidApiRequestException(STR."Invalid number of IRIs found, expected 1 got: \{subjectIris.stream()
                    .map(IRI::stringValue)
                    .collect(Collectors.joining(",", "[", "]"))}"
            );
        }

        return subjectIris.iterator().next();
    }

    public static IRI getObjectIRI(Model model, IRI subject, IRI predicate, boolean raiseException) {
        if (!raiseException) {
            return Models.objectIRI(model.filter(subject, predicate, null))
                    .orElse(null);
        } else {
            return Models.objectIRI(model.filter(subject, predicate, null))
                    .orElseThrow(() -> new RuntimeException("property not found."));
        }
    }

    /**
     * returns the sub model of a model.
     *
     * @param baseModel model
     * @param predicate predicate
     * @return model
     */
    public static Model getSubModel(Model baseModel, IRI predicate) {
        IRI id = RDFUtil.getRootIri(baseModel);
        Iterable<Statement> statements = baseModel.getStatements(id, predicate, null);
        Model subModel = findRecursively(IterableUtils.toList(statements), baseModel).orElse(null);
        if (subModel != null && !subModel.isEmpty()) {
            subModel.remove(RDFUtil.getRootIri(baseModel), predicate, null);
        }
        return subModel;
    }

    private static Optional<Model> findRecursively(List<Statement> statements, Model input) {

        Model model = new DynamicModelFactory().createEmptyModel();
//        model.addAll(statements);
        List<Statement> list = traverse(statements, new ArrayList<>(), input);
        model.addAll(list);
        return Optional.of(model);
    }

    private static List<Statement> traverse(List<Statement> statements, List<Statement> newStatements, Model model) {
        statements.forEach(statement -> {
            if (newStatements.contains(statement)) {
                return;
            }
            newStatements.add(statement);
            if (statement.getObject().isIRI()) {
                Iterable<Statement> bNodeStatements = model
                        .getStatements((org.eclipse.rdf4j.model.Resource) statement.getObject(), null, null);
                traverse(IterableUtils.toList(bNodeStatements), newStatements, model);
            }
        });
        return newStatements;
    }
}
