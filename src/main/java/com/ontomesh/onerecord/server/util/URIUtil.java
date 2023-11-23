package com.ontomesh.onerecord.server.util;

import java.net.URI;

public class URIUtil extends org.eclipse.rdf4j.model.util.URIUtil {

    public static URI addPath(URI uri, String path) {
        String newPath;
        if (path.startsWith("/")) newPath = path.replaceAll("//+", "/");
        else if (uri.getPath().endsWith("/")) newPath = uri.getPath() + path.replaceAll("//+", "/");
        else newPath = uri.getPath() + "/" + path.replaceAll("//+", "/");

        return uri.resolve(newPath).normalize();
    }

    public static URI getURI(String uri) {
        return URI.create(uri);
    }
}
