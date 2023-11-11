package com.ontomesh.onerecord.server.api;

import com.ontomesh.onerecord.server.common.OntoMesh;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.exception.AlreadyExistsException;
import com.ontomesh.onerecord.server.exception.InvalidAddressException;
import com.ontomesh.onerecord.server.services.LogisticsObjectService;
import com.ontomesh.onerecord.server.util.RDFUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class BaseController {

    @Autowired
    public IdProvider idProvider;
    @Autowired
    public LogisticsObjectService logisticsObjectService;
    private final SimpleValueFactory instance = SimpleValueFactory.getInstance();

    private final IRI hasPredefinedIri = instance.createIRI("https://www.ontomesh.com/onerecord#", "hasId");

    public void removeMarkerPredicate(LogisticsObject logisticsObject) {
        logisticsObject.model().remove(logisticsObject.iri(), hasPredefinedIri, null);
    }

    protected String getLoType(LogisticsObject logisticsObject) {
        Model model = logisticsObject.model();
        IRI iri = logisticsObject.iri();
        return RDFUtil.getType(model, iri).stringValue();
    }

    protected OntoMeshId getLoId(String uuid) {
        return idProvider.getUriForLoId(uuid);
    }

    protected void checkForDuplicateIRI(LogisticsObject logisticsObject) {
        IRI iri = logisticsObject.iri();
        // Check for proper address path.
        if (!iri.toString().contains(idProvider.getLogisticsObjectBaseIRI().toString())) {
            throw new InvalidAddressException(iri, idProvider.getLogisticsObjectBaseIRI());
        }

        // Check if IRI is already taken.
        boolean exists = logisticsObjectService.isExists(iri);
        if (exists)
            throw new AlreadyExistsException(iri, getLoType(logisticsObject));
    }

    protected boolean isIdPresent(LogisticsObject logisticsObject) {
        // Remove predicate temporarily introduced by LogisticsObjectBodyReader. Information is still part
        // of metadata.

        Optional<Literal> hasPredefIri =
                Models.objectLiteral(logisticsObject.model().filter(logisticsObject.iri(), hasPredefinedIri, null));
        return hasPredefIri.map(Literal::booleanValue).orElse(false);
    }


    protected HttpHeaders getResponseHeaders(URI loURI, String loType, MediaType contentType) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(OntoMesh.LO_TYPE_HEADER, OntoMesh.LO_TYPE_COMPANY_INFORMATION);
        responseHeaders.setContentType(contentType);
        responseHeaders.add("Type", loType);
        responseHeaders.setLocation(loURI);
        return responseHeaders;
    }


    public HttpHeaders getResponseHeaders(LogisticsObject logisticsObject, MediaType contentType) {
        URI loURI = URI.create(logisticsObject.iri().stringValue());
        String loType = getLoType(logisticsObject);
        return getResponseHeaders(loURI, loType, contentType);
    }

    protected HttpHeaders getResponseHeaders(MediaType contentType) {

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(contentType);
        responseHeaders.setContentLanguage(Locale.ENGLISH);
        return responseHeaders;
    }

    protected HttpHeaders getResponseHeaders(HttpServletRequest httpServletRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(httpServletRequest.getHeader(HttpHeaders.ACCEPT)));
        headers.setLocation(URI.create(httpServletRequest.getRequestURL().toString()));
        headers.add("Revision-Header", UUID.randomUUID().toString());
        headers.add(OntoMesh.LO_TYPE_HEADER, OntoMesh.LO_TYPE_COMPANY_INFORMATION);
        return headers;
    }

    /**
     * @param logisticsObject
     */
    public void addRevisionToLO(LogisticsObject logisticsObject) {
        logisticsObject.model().add(logisticsObject.iri(), API.hasRevision, Values.literal(1));
    }
}
