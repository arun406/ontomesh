package io.ontomesh.onerecord.server.common;

import org.springframework.http.MediaType;

public class SemanticWebMediaType extends MediaType {

    public static final MediaType APPLICATION_JSON_LD = new MediaType("application", "ld+json");
    public static final MediaType APPLICATION_TURTLE = new MediaType("application", "turtle");
    public static final MediaType TEXT_TURTLE = new MediaType("text", "turtle");
    public static final String APPLICATION_JSON_LD_VALUE = "application/ld+json";
    public static final String APPLICATION_TURTLE_VALUE = "application/turtle";
    public static final String TEXT_TURTLE_VALUE = "text/turtle";


    public SemanticWebMediaType(String type) {
        super(type);
    }
}
