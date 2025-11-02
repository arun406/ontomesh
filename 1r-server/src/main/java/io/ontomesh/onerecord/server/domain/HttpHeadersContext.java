package io.ontomesh.onerecord.server.domain;

import org.springframework.http.MediaType;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

public class HttpHeadersContext {
    private MediaType contentType;
    private Locale contentLanguage;
    private Instant lastModified;
    private URI location;
    private String type;
    private Integer revision;
    private Integer latestRevision;

    public MediaType getContentType() { return contentType; }
    public void setContentType(MediaType contentType) { this.contentType = contentType; }

    public Locale getContentLanguage() { return contentLanguage; }
    public void setContentLanguage(Locale contentLanguage) { this.contentLanguage = contentLanguage; }

    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }

    public URI getLocation() { return location; }
    public void setLocation(URI location) { this.location = location; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }

    public Integer getLatestRevision() { return latestRevision; }
    public void setLatestRevision(Integer latestRevision) { this.latestRevision = latestRevision; }
} 