package com.ontomesh.onerecord.server.util;

import com.ontomesh.onerecord.server.api.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author arun
 */
public class RDFUtil {


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

    public static IRI getType(Model model, IRI subject) {
        return Models.objectIRI(model.filter(subject, RDF.TYPE, null))
                .orElseThrow();
    }

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
     * @param model
     * @return
     */
    public static Collection<Statement> modelToCollection(Model model) {
        return model.stream().collect(Collectors.toSet());
    }

    /**
     * @param inputStream
     * @param inputRDFFormat
     * @param outputRDFFormat
     * @param baseUrl
     * @return
     * @throws IOException
     */
    public static String readRdfToString(InputStream inputStream, RDFFormat inputRDFFormat,
                                         RDFFormat outputRDFFormat, String baseUrl) throws IOException {
        Collection<Statement> statements = RDFUtil.readRdfToGraph(inputStream, inputRDFFormat, baseUrl);
        Model model = new ModelBuilder().build();
        model.addAll(statements);
        
        return new ModelToStringConverter(outputRDFFormat).convert(model);
    }

    /**
     * @param mediaType
     * @return
     */
    public static RDFFormat getFormat(MediaType mediaType) {

        if (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_JSON_LD)) {
            return RDFFormat.JSONLD;
        } else if (mediaType.isCompatibleWith(SemanticWebMediaType.APPLICATION_TURTLE)
                || mediaType.isCompatibleWith(SemanticWebMediaType.TEXT_TURTLE)) {
            return RDFFormat.TURTLE;
        }
        throw new RuntimeException("Content-Type or Accept  '" + mediaType + "'is not supported");
    }

    public static RDFFormat getDefaultFormat() {
        return RDFFormat.JSONLD;
    }

    /**
     * @param model
     * @param accept
     * @return
     */
    public static OutputStream getRDFByteStream(Model model, MediaType accept) {
        OutputStream outputStream = new ByteArrayOutputStream();
        RDFFormat format = RDFUtil.getFormat(accept);
        Rio.write(model, outputStream, format);
        return outputStream;
    }

    /**
     * @param model
     * @return
     */

    public static OutputStream getRDFByteStream(Model model) {
        OutputStream outputStream = new ByteArrayOutputStream();
        RDFFormat format = RDFFormat.JSONLD;
        Rio.write(model, outputStream, format);
        return outputStream;
    }

    public static void print(LogisticsObject logisticsObject) {
        print(logisticsObject.model());
    }

    public static void print(LogisticsObject logisticsObject, RDFFormat format) {
        print(logisticsObject.model(), format);
    }

    public static void print(Model model) {
        if (model == null) {
            return;
        }
        Rio.write(model, System.out, RDFFormat.TURTLE);
    }

    public static void print(Model model, RDFFormat format) {
        if (model == null) {
            return;
        }
        Rio.write(model, System.out, format);
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

    public static RDFParser getParser(RDFFormat rdfFormat, RDFHandler rdfHandler) {
        RDFParser parser = Rio.createParser(rdfFormat);
        parser.setRDFHandler(rdfHandler);
        return parser;
    }
}
