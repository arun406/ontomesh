package com.ontomesh.onerecord.server.util;

import com.ontomesh.onerecord.server.api.SemanticWebMediaType;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class RDFUtil {

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


    public static void print(Model model) {
        if (model == null) {
            return;
        }
        Rio.write(model, System.out, RDFFormat.TURTLE);
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

}
