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

    static {
        ValueFactory factory = SimpleValueFactory.getInstance();
        hasSnapshotDate = factory.createIRI(API.NAMESPACE, "hasSnapshotDate");
        hasDetails = factory.createIRI(API.NAMESPACE, "details");
        hasAttribute = factory.createIRI(API.NAMESPACE, "attribute");
        hasLogisticsEvents = factory.createIRI(API.NAMESPACE, "hasLogisticsEvents");
        Collection = factory.createIRI(API.NAMESPACE, "Collection");
        hasItems = factory.createIRI(API.NAMESPACE, "hasItems");
        hasTotalItems = factory.createIRI(API.NAMESPACE, "hasTotalItems");
    }
}
