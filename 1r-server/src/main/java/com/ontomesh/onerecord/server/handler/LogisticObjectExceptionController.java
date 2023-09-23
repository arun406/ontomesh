package com.ontomesh.onerecord.server.handler;

import com.ontomesh.onerecord.server.api.SemanticWebMediaType;
import com.ontomesh.onerecord.server.exception.*;
import com.ontomesh.onerecord.server.model.ErrorModelsRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.OutputStream;

import static com.ontomesh.onerecord.server.util.RDFUtil.getRDFByteStream;

@Slf4j
@ControllerAdvice
public class LogisticObjectExceptionController {

    /**
     * constructor
     *
     * @param errorModelsRepo
     */
    public LogisticObjectExceptionController(ErrorModelsRepo errorModelsRepo) {
        this.errorModelsRepo = errorModelsRepo;
    }

    @ExceptionHandler(value = LogisticsObjectNotSupportedException.class)
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            LogisticsObjectNotSupportedException exception) {

        Model errorModel = errorModelsRepo.getLogisticsObjectNotSupportedErrorModel(exception.getMessage());
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = InvalidApiRequestException.class)
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            InvalidApiRequestException exception) {

        Model errorModel = errorModelsRepo.getInvalidApiRequestErrorModel(exception.getMessage());
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = InvalidChangeOperationException.class)
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            InvalidChangeOperationException exception) {
        Model errorModel = errorModelsRepo.getInvalidChangeRequestErrorModel(exception.getIri());
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = LogisticsObjectURINotMatchedException.class)
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            LogisticsObjectURINotMatchedException exception) {
        Model errorModel = errorModelsRepo.getLogisticsObjectURINotMatchedErrorModel(exception.getIri());
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = SubjectNotFoundException.class)
    public ResponseEntity<String> exception(HttpServletRequest request, SubjectNotFoundException exception) {

        Model errorModel = errorModelsRepo.getSubjectNotFoundErrorModel();
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {HttpMediaTypeNotAcceptableException.class})
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            HttpMediaTypeNotAcceptableException exception) {

        MediaType accept = getAcceptHeader(request);
        Model errorModel = errorModelsRepo.getMediaTypeNotAcceptableErrorModel(accept);
        return getResponse(accept, errorModel, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(value = {AlreadyExistsException.class})
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            AlreadyExistsException exception) {
        Model errorModel = errorModelsRepo.getAlreadyExistsErrorModel(exception.getIri(), exception.getLoType());
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value = {RDFParseException.class})
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            RDFParseException exception) {

        Model errorModel = errorModelsRepo.getInvalidRDFModelErrorModel();
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<String> exception(HttpServletRequest request,
                                            @RequestHeader(HttpHeaders.CONTENT_TYPE) MediaType contentType,
                                            HttpMediaTypeNotSupportedException exception) {

        Model errorModel = errorModelsRepo.getMediaTypeNotSupportedErrorModel(contentType);
        return getResponse(getAcceptHeader(request), errorModel, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    public final ErrorModelsRepo errorModelsRepo;


    public HttpHeaders getErrorResponseHeaders() {
        return getErrorResponseHeaders(SemanticWebMediaType.APPLICATION_JSON_LD);
    }

    public static HttpHeaders getErrorResponseHeaders(MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        return headers;
    }

    public MediaType getAcceptHeader(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader(HttpHeaders.ACCEPT);
        if (!StringUtils.hasText(header)) {
            header = SemanticWebMediaType.APPLICATION_JSON_LD_VALUE;
        }
        return MediaType.valueOf(header);
    }

    protected ResponseEntity<String> getResponse(MediaType accept, Model errorModel, HttpStatus httpStatus) {
        OutputStream outputStream = getRDFByteStream(errorModel, accept);
        return new ResponseEntity<>(outputStream.toString(), getErrorResponseHeaders(accept),
                httpStatus);
    }

}
