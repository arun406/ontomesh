package com.ontomesh.onerecord.server.util;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.base.Charsets;
import lombok.Getter;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;
import org.iata.onerecord.cargo.CARGO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonldUtil {

    @Getter
    public final static Map<String, Map<String, String>> context = new HashMap<>();

    static {
        context.put("@context", new HashMap<>());
        context.get("@context").put(CARGO.PREFIX, CARGO.NAMESPACE);
        context.get("@context").put(API.PREFIX, API.NAMESPACE);
    }

    /**
     * JSON LD to String.
     *
     * @param jsonString Json LD as String
     * @param pretty     pretty print the json.
     * @param type       RDF Type.
     * @return json ld string.
     * @throws IOException exception
     */
    public static String jsonLdToString(String jsonString, boolean pretty, IRI type) throws IOException {
        Object jsonObject = createJsonObject(jsonString);
        Map<String, Object> framedJson = getFramedJson(jsonObject, new HashMap<>());
        framedJson = removeGraphArray(framedJson, type);
        if (pretty) {
            return JsonUtils.toPrettyString(framedJson);
        } else {
            return JsonUtils.toString(framedJson);
        }
    }

    /**
     * @param framedJson Framed Json.
     * @param type       RDF Type.
     * @return Graph Array.
     */
    public static Map<String, Object> removeGraphArray(Map<String, Object> framedJson, IRI type) {
        List<Map<String, Object>> graph = (List<Map<String, Object>>) framedJson.get("@graph");
        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> objectMap : graph) {
            Object o = objectMap.get("@type");
            if (o != null && ((String) o).equals(type.toString())) {
                result = objectMap;
                break;
            }
        }
        return result;
    }

    /**
     * Get Framed Json.
     *
     * @param json    Json Object.
     * @param context RDF Context Map
     * @return Framed Json Map.
     */
    private static Map<String, Object> getFramedJson(Object json, Map<String, Map<String, String>> context) {
        JsonLdOptions opts = getJsonLdOptions();
        return JsonLdProcessor.frame(json, context, opts);
    }

    public static JsonLdOptions getJsonLdOptions() {
        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(true);
        opts.setOmitGraph(false);
        opts.setEmbed(true);
        return opts;
    }

    /**
     * Creates Json Object from Json String.
     *
     * @param jsonLd json ld string
     * @return JSON object.
     */
    private static Object createJsonObject(String jsonLd) {

        try {
            InputStream inputStream =
                    new ByteArrayInputStream(jsonLd.getBytes(Charsets.UTF_8));
            return JsonUtils.fromInputStream(inputStream);

        } catch (IOException e) {
            throw new RuntimeException("Unable to create object from json ld");
        }
    }
}
