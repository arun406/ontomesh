package com.ontomesh.onerecord.server.model;

import com.ontomesh.onerecord.server.api.IdProvider;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.iata.onerecord.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ErrorModelsRepo {

    /**
     * Details
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasDetails}.
     * <p>
     * Details of the error
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasDetails">hasDetails</a>
     */
    public static final IRI hasDetails;

    /**
     * hasAttribute
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasAttribute}.
     * <p>
     * Error details attribute
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasDetails">hasAttribute</a>
     */
    public static final IRI hasAttribute;

    static {
        ValueFactory factory = SimpleValueFactory.getInstance();
        hasDetails = factory.createIRI(API.NAMESPACE, "details");
        hasAttribute = factory.createIRI(API.NAMESPACE, "attribute");
    }

    private final IdProvider idProvider;

    public ErrorModelsRepo(IdProvider idProvider) {
        this.idProvider = idProvider;
    }

    /**
     * Returns the Model for Subject Not Found Error
     *
     * @param errorIRI
     * @return
     */
    public Model getSubjectNotFoundErrorModel(IRI errorIRI) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Subject not found"))
                .build();

    }

    public Model getMediaTypeNotSupportedErrorModel(MediaType contentType, IRI errorIRI, IRI detailsIRI, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Media Type Not Supported"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(hasAttribute, "Content-Type")
                .add(API.hasResource, getResourceType())
                .add(API.hasMessage, "Content-Type '" + contentType + "' is not supported.")
                .build();

    }

    public Model getMediaTypeNotAcceptableErrorModel(MediaType mediaType, IRI errorIRI, IRI detailsIRI, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Media Type Not Supported"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, hasDetails)
                .add(API.hasCode, status.value())
                .add(hasAttribute, "Accept")
                .add(API.hasResource, getResourceType())
                .add(API.hasMessage, "Accept '" + mediaType + "' is not supported.")
                .build();

    }

    public Model getAlreadyExistsErrorModel(IRI iri, String resourceType, IRI errorIRI, IRI detailsIRI, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistic Object Already Exists"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(hasAttribute, "@id")
                .add(API.hasResource, resourceType)
                .add(API.hasMessage, iri.toString() + " is already exists.")
                .build();
    }

    private static String getResourceType() {
        return "CompanyInformation";
    }

    public Model getInvalidRDFModelErrorModel(IRI errorIRI) {
        return new ModelBuilder()
                .setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Invalid RDF model"))
                .build();
    }

    public Model getInvalidApiRequestErrorModel(IRI errorIRI, IRI detailsIRI, String message, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Missing query parameter"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(hasAttribute, "topic")
                .add(API.hasMessage, message)
                .build();
    }

    public Model getLogisticsObjectNotSupportedErrorModel(IRI errorIRI, IRI errorDetailsIRI, String message,
                                                          HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Object Type not supported"))
                .add(hasDetails, errorDetailsIRI)
                .subject(errorDetailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(hasAttribute, "topic")
                .add(API.hasMessage, message)
                .build();
    }

    public Model getInvalidChangeRequestErrorModel(IRI resource, IRI errorIRI, IRI detailsIRI, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Events can not be updated"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(API.hasMessage, "Logistics Events of a Logistics Object can not be updated using PATCH")
                .add(API.hasResource, resource)
                .build();
    }

    public Model getLogisticsObjectURINotMatchedErrorModel(IRI resource, IRI errorIRI, IRI detailsIRI, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Object URI does not match"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(API.hasMessage, "LogisticsObject URI in Change does not match requested Logistics Object URI")
                .add(API.hasResource, resource)
                .build();
    }

    public Model getLogisticsObjectRevisionNotMatchedErrorModel(IRI resource, IRI errorIRI, IRI detailsIRI, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Logistics Object revision does not match"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(API.hasMessage, "LogisticsObject revision in Change does not match requested Logistics Object revision")
                .add(API.hasResource, resource)
                .build();
    }

    public Model getRevisionMissingErrorModel(IRI resource, IRI errorIRI, IRI detailsIRI, HttpStatus status) {
        return new ModelBuilder().setNamespace(API.PREFIX, API.NAMESPACE)
                .subject(errorIRI)
                .add(RDF.TYPE, API.Error)
                .add(API.hasTitle, Values.literal("Revision is missing in Change request"))
                .add(hasDetails, detailsIRI)
                .subject(detailsIRI)
                .add(RDF.TYPE, API.ErrorDetail)
                .add(API.hasCode, status.value())
                .add(API.hasMessage, "Revision is missing in Change request")
                .add(API.hasResource, resource)
                .build();
    }
}
