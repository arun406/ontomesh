package io.ontomesh.onerecord.server.adapter;

import io.ontomesh.onerecord.server.domain.ErrorObject;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.AuditTrailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.iata.onerecord.api.API.REQUEST_ACCEPTED;
import static org.iata.onerecord.api.API.REQUEST_REJECTED;

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
        boolean result = returnType.getGenericParameterType() == LogisticsObject.class
                || (returnType.getGenericParameterType() == ErrorObject.class);
        return result;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        log.debug("in beforeBodyWrite");
        if (httpServletRequest.getMethod().equals("PATCH")) {
            log.debug("Change Request IRI: {}", httpServletRequest.getAttribute("changeRequestIRI"));
            IRI changeRequestIRI = (IRI) httpServletRequest.getAttribute("changeRequestIRI");
            if (body instanceof LogisticsObject logisticsObject) {
                this.auditTrailService.updateChangeRequestStatus(changeRequestIRI, REQUEST_ACCEPTED);
                log.debug("response iri {}, content type {}", logisticsObject.iri(), selectedContentType);
                return logisticsObject;
            } else if (body instanceof ErrorObject errorObject) {
                log.debug("response iri {}, http status {}", errorObject.iri(), errorObject.httpStatus());
                this.auditTrailService.updateChangeRequestStatus(changeRequestIRI, REQUEST_REJECTED);
                this.auditTrailService.addError(changeRequestIRI, errorObject);
                response.setStatusCode(errorObject.httpStatus());
                return errorObject;
            }
        }
        return body;
    }
}
