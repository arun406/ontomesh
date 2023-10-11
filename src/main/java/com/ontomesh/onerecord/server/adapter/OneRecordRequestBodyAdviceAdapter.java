package com.ontomesh.onerecord.server.adapter;

import com.ontomesh.onerecord.server.api.BlankNodeReplacingStatementHandler;
import com.ontomesh.onerecord.server.api.IdProvider;
import com.ontomesh.onerecord.server.domain.LogisticsObject;
import com.ontomesh.onerecord.server.services.AuditTrailService;
import com.ontomesh.onerecord.server.util.URIUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;

@Slf4j
@RestControllerAdvice
public class OneRecordRequestBodyAdviceAdapter extends RequestBodyAdviceAdapter {

    private final BlankNodeReplacingStatementHandler statementHandler;
    private final IdProvider idProvider;
    private final AuditTrailService auditTrailService;
    private final HttpServletRequest httpServletRequest;


    public OneRecordRequestBodyAdviceAdapter(BlankNodeReplacingStatementHandler statementHandler, IdProvider idProvider, AuditTrailService auditTrailService, HttpServletRequest httpServletRequest) {
        this.statementHandler = statementHandler;
        this.idProvider = idProvider;
        this.auditTrailService = auditTrailService;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        log.debug("@supports");
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        log.debug("@afterBodyRead");
        log.debug("request url : {}", httpServletRequest.getRequestURL());
        if (body instanceof LogisticsObject logisticsObject && httpServletRequest.getMethod().startsWith("PATCH")) {
            log.debug("IRI of the change request : {}", logisticsObject.iri());
            IRI changeRequestIRI = this.auditTrailService.saveChangeRequest(logisticsObject);
            log.debug("Change Request IRI :{}", changeRequestIRI);
            httpServletRequest.setAttribute("changeRequestIRI", changeRequestIRI);
        }
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        log.debug("In handleEmptyBody() method of {}", getClass().getSimpleName());

        return body;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        log.debug("In beforeBodyRead() method of {}", getClass().getSimpleName());
        return inputMessage;
    }
}
