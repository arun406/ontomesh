package io.ontomesh.onerecord.server.domain;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

public class HttpHeadersBuilder {
    private final HttpHeaders headers;

    // Constructor initializes the HttpHeaders object
    public HttpHeadersBuilder() {
        this.headers = new HttpHeaders();
    }

    // Set the Content-Type header
    public HttpHeadersBuilder contentType(MediaType mediaType) {
        headers.setContentType(mediaType);
        return this;
    }

    // Set the Content-Language header
    public HttpHeadersBuilder contentLanguage(Locale locale) {
        headers.setContentLanguage(locale);
        return this;
    }

    // Set the Last-Modified header
    public HttpHeadersBuilder lastModified(Instant instant) {
        headers.setLastModified(instant.toEpochMilli());
        return this;
    }

    // Set the Location header
    public HttpHeadersBuilder location(URI location) {
        headers.setLocation(location);
        return this;
    }

    // Add a custom "Type" header
    public HttpHeadersBuilder type(String type) {
        headers.add("Type", type);
        return this;
    }

    // Add the "Revision" header (snapshot version)
    public HttpHeadersBuilder revision(int revision) {
        headers.add("Revision", String.valueOf(revision));
        return this;
    }

    // Add the "Latest-Revision" header (timestamp of the latest revision)
    public HttpHeadersBuilder latestRevision(int latestRevision) {
        headers.add("Latest-Revision", String.valueOf(latestRevision));
        return this;
    }

    // Build and return the HttpHeaders object
    public HttpHeaders build() {
        return headers;
    }

    // Centralized static method for building headers from a context object
    public static HttpHeaders buildFromContext(HttpHeadersContext ctx) {
        HttpHeadersBuilder builder = new HttpHeadersBuilder();
        if (ctx.getContentType() != null) builder.contentType(ctx.getContentType());
        if (ctx.getContentLanguage() != null) builder.contentLanguage(ctx.getContentLanguage());
        if (ctx.getLastModified() != null) builder.lastModified(ctx.getLastModified());
        if (ctx.getLocation() != null) builder.location(ctx.getLocation());
        if (ctx.getType() != null) builder.type(ctx.getType());
        if (ctx.getRevision() != null) builder.revision(ctx.getRevision());
        if (ctx.getLatestRevision() != null) builder.latestRevision(ctx.getLatestRevision());
        return builder.build();
    }
}
