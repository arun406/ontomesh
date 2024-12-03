package io.ontomesh.onerecord.server.adapter;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

@Slf4j
@RestControllerAdvice
public class OneRecordRequestBodyAdviceAdapter extends RequestBodyAdviceAdapter {

    private final HttpServletRequest httpServletRequest;

    /**
     * @param httpServletRequest
     */
    public OneRecordRequestBodyAdviceAdapter(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @Transactional
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

        log.debug("request url : {}", httpServletRequest.getRequestURL());
        if (body instanceof LogisticsObject changeObject && httpServletRequest.getMethod().startsWith("PATCH")) {
            log.debug("IRI of the change request : {}", changeObject.iri());
//            IRI changeRequestIRI = this.auditTrailService.saveChangeRequest(changeObject);
//            IRI iri = this.auditTrailService.saveOrUpdate(changeRequestIRI, Values.iri(httpServletRequest.getRequestURL().toString()));
//            log.debug("Audit IRI :{}", changeRequestIRI);
//            httpServletRequest.setAttribute("changeRequestIRI", changeRequestIRI);
        }

        return body;
    }
}
