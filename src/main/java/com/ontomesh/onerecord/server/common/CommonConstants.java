package com.ontomesh.onerecord.server.common;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.iata.onerecord.api.API;

public class CommonConstants {
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
     * @see <a href="https://onerecord.iata.org/ns/api#hasDetails">hasAttribute</a>
     */
    public static final IRI hasAttribute;

    static {
        ValueFactory factory = SimpleValueFactory.getInstance();
        hasSnapshotDate = factory.createIRI(API.NAMESPACE, "hasSnapshotDate");
        hasDetails = factory.createIRI(API.NAMESPACE, "details");
        hasAttribute = factory.createIRI(API.NAMESPACE, "attribute");
    }
}
