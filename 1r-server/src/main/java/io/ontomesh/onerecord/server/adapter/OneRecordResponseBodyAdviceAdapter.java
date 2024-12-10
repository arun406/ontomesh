package io.ontomesh.onerecord.server.adapter;

import io.ontomesh.onerecord.server.domain.ErrorObject;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
//@RestControllerAdvice
public class OneRecordResponseBodyAdviceAdapter implements ResponseBodyAdvice<Object> {

    private final HttpServletRequest httpServletRequest;

    public OneRecordResponseBodyAdviceAdapter(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getGenericParameterType() == LogisticsObject.class
                || (returnType.getGenericParameterType() == ErrorObject.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        if (httpServletRequest.getMethod().equals("PATCH")) {
//            IRI changeRequestIRI = (IRI) httpServletRequest.getAttribute("changeRequestIRI");

            if (body instanceof LogisticsObject logisticsObject) {
//                if (changeRequestIRI != null) {
//                    this.auditTrailService.updateChangeRequestStatus(changeRequestIRI, REQUEST_ACCEPTED);
//                }
                log.debug("response iri {}, content type {}", logisticsObject.iri(), selectedContentType);
                return logisticsObject;
            } else if (body instanceof ErrorObject errorObject) {
                log.debug("response iri {}, http status {}", errorObject.iri(), errorObject.httpStatus());
//                if (changeRequestIRI != null) {
//                    this.auditTrailService.updateChangeRequestStatus(changeRequestIRI, REQUEST_REJECTED);
//                    this.auditTrailService.addError(changeRequestIRI, errorObject);
//                }
                response.setStatusCode(errorObject.httpStatus());
                return errorObject;
            }
        }
        return body;
    }
}
