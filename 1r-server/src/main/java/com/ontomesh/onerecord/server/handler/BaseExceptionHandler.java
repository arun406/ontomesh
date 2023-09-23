package com.ontomesh.onerecord.server.handler;

import com.ontomesh.onerecord.server.api.SemanticWebMediaType;
import com.ontomesh.onerecord.server.model.ErrorModelsRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.eclipse.rdf4j.model.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.OutputStream;

import static com.ontomesh.onerecord.server.util.RDFUtil.getRDFByteStream;

@Data
@Component
public abstract class BaseExceptionHandler {


}
