package com.ontomesh.onerecord.server.model;

import com.ontomesh.onerecord.server.api.IdProvider;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ErrorModelsRepo {
    private final IdProvider idProvider;

    public ErrorModelsRepo(IdProvider idProvider) {
        this.idProvider = idProvider;
    }

    /**
     * Returns the Model for Subject Not Found Error
     *
     * @return
     */
    public Model getSubjectNotFoundErrorModel() {
        ModelBuilder builder = new ModelBuilder();

        builder.setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Subject not found"));

        return builder.build();
    }

    public Model getMediaTypeNotSupportedErrorModel(MediaType contentType) {
        ModelBuilder builder = new ModelBuilder();
        IRI details = idProvider.createInternalIri().getIri();
        builder.setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Media Type Not Supported"))
                .add("api:details", details)
                .subject(details)
                .add(RDF.TYPE, "api:Details")
                .add("api:hasCode", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .add("api:attribute", "Content-Type")
                .add("api:hasResource", getResourceType())
                .add("api:hasMessage", "Content-Type '" + contentType + "' is not supported.");

        return builder.build();
    }

    public Model getMediaTypeNotAcceptableErrorModel(MediaType mediaType) {
        ModelBuilder builder = new ModelBuilder();

        IRI details = idProvider.createInternalIri().getIri();
        builder.setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Media Type Not Supported"))
                .add("api:details", details)
                .subject(details)
                .add(RDF.TYPE, "api:Details")
                .add("api:hasCode", HttpStatus.NOT_ACCEPTABLE.value())
                .add("api:attribute", "Accept")
                .add("api:hasResource", getResourceType())
                .add("api:hasMessage", "Accept '" + mediaType + "' is not supported.");

        return builder.build();
    }

    public Model getAlreadyExistsErrorModel(IRI iri, String resourceType) {
        IRI details = idProvider.createInternalIri().getIri();
        return new ModelBuilder().setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Logistic Object Already Exists"))
                .add("api:details", details)
                .subject(details)
                .add(RDF.TYPE, "api:ErrorDetails")
                .add("api:hasCode", HttpStatus.CONFLICT.value())
                .add("api:attribute", "@id")
                .add("api:hasResource", resourceType)
                .add("api:hasMessage", iri.toString() + " is already exists.")
                .build();

    }

    private static String getResourceType() {
        return "CompanyInformation";
    }

    public Model getInvalidRDFModelErrorModel() {
        return new ModelBuilder()
                .setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Invalid RDF model"))
                .build();
    }

    public Model getInvalidApiRequestErrorModel(String message) {
        IRI details = idProvider.createInternalIri().getIri();
        return new ModelBuilder().setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Missing query parameter"))
                .add("api:details", details)
                .subject(details)
                .add(RDF.TYPE, "api:ErrorDetails")
                .add("api:hasCode", HttpStatus.BAD_REQUEST.value())
                .add("api:attribute", "topic")
                .add("api:hasMessage", message)
                .build();
    }

    public Model getLogisticsObjectNotSupportedErrorModel(String message) {
        IRI details = idProvider.createInternalIri().getIri();
        return new ModelBuilder().setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Logistics Object Type not supported"))
                .add("api:details", details)
                .subject(details)
                .add(RDF.TYPE, "api:ErrorDetails")
                .add("api:hasCode", HttpStatus.BAD_REQUEST.value())
                .add("api:attribute", "topic")
                .add("api:hasMessage", message)
                .build();
    }

    public Model getInvalidChangeRequestErrorModel(IRI resource) {
        IRI details = idProvider.createInternalIri().getIri();
        return new ModelBuilder().setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Logistics Events can not be updated"))
                .add("api:details", details)
                .subject(details)
                .add(RDF.TYPE, "api:ErrorDetails")
                .add("api:hasCode", HttpStatus.BAD_REQUEST.value())
                .add("api:hasMessage", "Logistics Events of a Logistics Object can not be updated using PATCH")
                .add("api:hasResource", resource)
                .build();
    }

    public Model getLogisticsObjectURINotMatchedErrorModel(IRI resource) {
        IRI details = idProvider.createInternalIri().getIri();
        return new ModelBuilder().setNamespace("api", "https://onerecord.iata.org/api/")
                .subject(idProvider.createInternalIri().getIri())
                .add(RDF.TYPE, "api:Error")
                .add("api:hasTitle", Values.literal("Logistics Object URI does not match"))
                .add("api:details", details)
                .subject(details)
                .add(RDF.TYPE, "api:ErrorDetails")
                .add("api:hasCode", HttpStatus.BAD_REQUEST.value())
                .add("api:hasMessage", "LogisticsObject URI in Change does not match requested Logistics Object URI")
                .add("api:hasResource", resource)
                .build();
    }
}
