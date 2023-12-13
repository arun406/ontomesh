package com.ontomesh.onerecord.server.common;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.iata.onerecord.api.API;

public class OntoMesh {
    public static final String LO_TYPE_COMPANY_INFORMATION = "https://onerecord.iata.org/api/CompanyInformation";
    public static final String LO_TYPE_HEADER = "LO-type";

    /**
     * RequestStatus
     * <p>
     * {@code https://onerecord.iata.org/ns/api#RequestStatus}.
     *
     * @see <a href="https://onerecord.iata.org/ns/api#RequestStatus">RequestStatus</a>
     */
    public static final IRI hasSnapshotDate;

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
     * @see <a href="https://onerecord.iata.org/ns/api#hasAttribute">hasAttribute</a>
     */
    public static final IRI hasAttribute;

    /**
     * hasLogisticsEvents
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasLogisticsEvents}.
     * <p>
     * Logistics Events
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasLogisticsEvents">hasLogisticsEvents</a>
     */
    public static final IRI hasLogisticsEvents;

    /**
     * Collection
     * <p>
     * {@code https://onerecord.iata.org/ns/api#Collection}.
     * <p>
     * Collection
     *
     * @see <a href="https://onerecord.iata.org/ns/api#Collection">Collection</a>
     */
    public static final IRI Collection;

    /**
     * hasItems
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasItem}.
     * <p>
     * hasItems
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasItem">hasItems</a>
     */
    public static final IRI hasItems;

    /**
     * hasTotalItems
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasTotalItems}.
     * <p>
     * hasTotalItems
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasTotalItems">hasTotalItems</a>
     */
    public static final IRI hasTotalItems;

    /**
     * hasEventType
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventType}.
     * <p>
     * hasEventType
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventType">hasEventType</a>
     */
    public static IRI hasEventType;
    /**
     * hasEventStatusNew
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventStatusNew}.
     * <p>
     * hasEventStatusNew
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatusNew">hasEventStatusNew</a>
     */
    public static IRI hasEventStatusNew;

    /**
     * hasEventStatusFailed
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventStatusFailed}.
     * <p>
     * hasEventStatusNew
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatusFailed">hasEventStatusFailed</a>
     */
    public static IRI hasEventStatusFailed;


    /**
     * hasEventStatusPending
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventStatusPending}.
     * <p>
     * hasEventStatusPending
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatusPending">hasEventStatusPending</a>
     */
    public static IRI hasEventStatusPending;

    /**
     * hasEventStatusPublished
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventStatusPublished}.
     * <p>
     * hasEventStatusPublished
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatusPublished">hasEventStatusPublished</a>
     */
    public static IRI hasEventStatusPublished;

    /**
     * hasEventStatusProcessed
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventStatusProcessed}.
     * <p>
     * hasEventStatusProcessed
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatusProcessed">hasEventStatusProcessed</a>
     */
    public static IRI hasEventStatusProcessed;

    /**
     * hasEventStatusConsumed
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventStatusConsumed}.
     * <p>
     * hasEventStatusConsumed
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatusConsumed">hasEventStatusConsumed</a>
     */
    public static IRI hasEventStatusConsumed;
    /**
     * hasEventStatus
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasEventStatus}.
     * <p>
     * hasTotalItems
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatus">hasEventStatus</a>
     */
    public static IRI hasEventStatus;
    /**
     * hasTriggeredBy
     * <p>
     * {@code https://onerecord.iata.org/ns/api#hasTriggeredBy}.
     * <p>
     * hasTriggeredBy
     *
     * @see <a href="https://onerecord.iata.org/ns/api#hasEventStatus">hasTriggeredBy</a>
     */
    public static IRI hasTriggeredBy;


    static {
        ValueFactory factory = SimpleValueFactory.getInstance();
        hasSnapshotDate = factory.createIRI(API.NAMESPACE, "hasSnapshotDate");
        hasDetails = factory.createIRI(API.NAMESPACE, "details");
        hasAttribute = factory.createIRI(API.NAMESPACE, "attribute");
        hasLogisticsEvents = factory.createIRI(API.NAMESPACE, "hasLogisticsEvents");
        Collection = factory.createIRI(API.NAMESPACE, "Collection");
        hasItems = factory.createIRI(API.NAMESPACE, "hasItems");
        hasTotalItems = factory.createIRI(API.NAMESPACE, "hasTotalItems");
        hasEventType = factory.createIRI(API.NAMESPACE, "hasEventType");
        hasTriggeredBy = factory.createIRI(API.NAMESPACE, "hasTriggeredBy");
        hasEventStatus = factory.createIRI(API.NAMESPACE, "hasEventStatus");
        hasEventStatusConsumed = factory.createIRI(API.NAMESPACE, "hasEventStatusConsumed");
        hasEventStatusProcessed = factory.createIRI(API.NAMESPACE, "hasEventStatusProcessed");
        hasEventStatusPublished = factory.createIRI(API.NAMESPACE, "hasEventStatusPublished");
        hasEventStatusPending = factory.createIRI(API.NAMESPACE, "hasEventStatusPending");
        hasEventStatusNew = factory.createIRI(API.NAMESPACE, "hasEventStatusNew");
    }
}
