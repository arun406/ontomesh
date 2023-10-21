package com.ontomesh.onerecord.server.exception.handler;

import com.ontomesh.onerecord.server.handler.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;

public abstract class AbstractBadRequestExceptionHandler<T> extends AbstractExceptionHandler<T> {
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
