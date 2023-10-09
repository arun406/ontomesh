package com.ontomesh.onerecord.server.handler;

import com.ontomesh.onerecord.server.api.BlankNodeReplacingStatementHandler;
import com.ontomesh.onerecord.server.api.IdProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

@Slf4j
@RestControllerAdvice
public class OneRecordRequestBodyAdviceAdapter extends RequestBodyAdviceAdapter {

    public OneRecordRequestBodyAdviceAdapter(BlankNodeReplacingStatementHandler statementHandler, IdProvider idProvider) {
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
        return body;
    }


}
