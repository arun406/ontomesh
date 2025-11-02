package io.ontomesh.onerecord.server.api.advice;

import io.ontomesh.onerecord.server.common.SemanticWebConstants;
import io.ontomesh.onerecord.server.domain.HttpHeadersBuilder;
import io.ontomesh.onerecord.server.domain.HttpHeadersContext;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Locale;

/**
 * Centralizes and standardizes response header creation for all REST controllers.
 * Sets headers like Content-Language, Last-Modified, Content-Type, Type, Revision, and Latest-Revision
 * based on endpoint URL and response body type.
 */
@Slf4j
@Order(1)
@ControllerAdvice(basePackages = "io.ontomesh.onerecord.server.api")
public class OntomeshResponseHeaderAdvice implements ResponseBodyAdvice<Object> {

    /**
     * Only apply to @RestController classes in the specified package.
     */
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        Class<?> declaringClass = returnType.getDeclaringClass();
        for (Annotation annotation : declaringClass.getAnnotations()) {
            if (annotation.annotationType().equals(RestController.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets standard and custom headers for all REST responses.
     * Uses URL matching for endpoints that do not return a LogisticsObject, and inspects the response body for domain objects.
     */
    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        HttpHeadersContext ctx = new HttpHeadersContext();
        ctx.setContentLanguage(Locale.US);
        ctx.setLastModified(Instant.now());
        ctx.setContentType(selectedContentType);

        Method method = returnType.getMethod();
        String path = request.getURI().getPath();
        if (method != null) {
            String methodName = method.getName();
            // Set Type header based on endpoint URL for endpoints that do not return a LogisticsObject
            if (path.startsWith("/access-delegations")) {
                ctx.setType(org.iata.onerecord.api.API.AccessDelegation.stringValue());
                log.debug("[HeaderAdvice] Set Type header for endpoint {}: {}", path, ctx.getType());
            } else if (path.startsWith("/authorizations-acls")) {
                ctx.setType(SemanticWebConstants.AUTHORIZATION_TYPE);
                log.debug("[HeaderAdvice] Set Type header for endpoint {}: {}", path, ctx.getType());
            } else if (path.startsWith("/subscriptions")) {
                ctx.setType(org.iata.onerecord.api.API.SubscriptionRequest.stringValue());
                log.debug("[HeaderAdvice] Set Type header for endpoint {}: {}", path, ctx.getType());
            } else if (path.startsWith("/action-requests") && path.contains("update")) {
                ctx.setType(org.iata.onerecord.api.API.ChangeRequest.stringValue());
                log.debug("[HeaderAdvice] Set Type header for endpoint {}: {}", path, ctx.getType());
            }
            // Set Location header for save/create/subscribe methods if present in ResponseEntity
            if (methodName.startsWith("save") || methodName.startsWith("create") || methodName.startsWith("subscribe")) {
                if (body instanceof ResponseEntity<?> entity) {
                    if (entity.getHeaders().getLocation() != null) {
                        ctx.setLocation(entity.getHeaders().getLocation());
                        log.debug("[HeaderAdvice] Set Location header for endpoint {}: {}", path, ctx.getLocation());
                    }
                }
            }
        }

        // If the response is a LogisticsObject, set custom headers (Type, Revision, Latest-Revision)
        Object actualBody = (body instanceof ResponseEntity) ? ((ResponseEntity<?>) body).getBody() : body;
        if (actualBody instanceof LogisticsObject logisticsObject) {
            if (logisticsObject.type() != null) {
                ctx.setType(logisticsObject.type().stringValue());
                log.debug("[HeaderAdvice] Set Type header from LogisticsObject for endpoint {}: {}", path, ctx.getType());
            }
            if (logisticsObject.revision() != null) {
                ctx.setRevision(logisticsObject.revision().intValue());
                log.debug("[HeaderAdvice] Set Revision header from LogisticsObject for endpoint {}: {}", path, ctx.getRevision());
            }
            if (logisticsObject.getLatestRevision() != null) {
                ctx.setLatestRevision(logisticsObject.getLatestRevision());
                log.debug("[HeaderAdvice] Set Latest-Revision header from LogisticsObject for endpoint {}: {}", path, ctx.getLatestRevision());
            }
        }

        // Build and set headers
        HttpHeaders headers = HttpHeadersBuilder.buildFromContext(ctx);
        headers.forEach((key, values) -> {
            for (String value : values) {
                response.getHeaders().add(key, value);
                log.debug("[HeaderAdvice] Added header '{}' = '{}' for endpoint {}", key, value, path);
            }
        });
        return body;
    }
} 