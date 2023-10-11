package com.ontomesh.onerecord.server.adapter;

import com.ontomesh.onerecord.server.api.LogisticsObjectController;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.services.AuditTrailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@ControllerAdvice
public class OneRecordResponseBodyAdviceAdapter implements ResponseBodyAdvice<Object> {

    private final HttpServletRequest httpServletRequest;
    private final AuditTrailService auditTrailService;

    public OneRecordResponseBodyAdviceAdapter(HttpServletRequest httpServletRequest, AuditTrailService auditTrailService) {
        this.httpServletRequest = httpServletRequest;
        this.auditTrailService = auditTrailService;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.getContainingClass() == LogisticsObjectController.class) {
            return true;
        }
        return false;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        if (httpServletRequest.getMethod().equals("PATCH")) {
            log.debug("Change Request IRI: {}", httpServletRequest.getAttribute("changeRequestIRI"));
            IRI changeRequestIRI = Values.iri((String) httpServletRequest.getAttribute("changeRequestIRI"));
            auditTrailService.updateChangeRequestStatus(changeRequestIRI);
        }
        if (body instanceof LogisticsObject logisticsObject) {
            log.debug("response iri {}, content type {}", logisticsObject.iri(), selectedContentType);
            return logisticsObject;
        }

        return body;
    }
}
