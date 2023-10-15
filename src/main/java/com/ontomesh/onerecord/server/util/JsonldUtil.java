package com.ontomesh.onerecord.server.util;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.base.Charsets;
import org.eclipse.rdf4j.model.IRI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonldUtil {

    public static Map<String, Map<String, String>> context = new HashMap<>();

    static {
        context.put("@context", new HashMap<>());
        context.get("@context").put("cargo", "https://onerecord.iata.org/ns/cargo#");
        context.get("@context").put("api", "https://onerecord.iata.org/ns/api#");
    }

    /**
     * @param jsonString
     * @param pretty
     * @param type
     * @return
     * @throws IOException
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
     * @param framedJson
     * @param type
     * @return
     */
    public static Map<String, Object> removeGraphArray(Map<String, Object> framedJson, IRI type) {
        List<Map<String, Object>> graph = (List<Map<String, Object>>) framedJson.get("@graph");
        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> objectMap : graph) {
            Object o = objectMap.get("@type");
            if (((String) o).equals(type.toString())) {
                result = objectMap;
                break;
            }
        }
        return result;
    }

    /**
     * @param json
     * @param context
     * @return
     */
    public static Map<String, Object> getFramedJson(Object json, Map<String, Map<String, String>> context) {
        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(false);
        opts.setOmitGraph(true);
        opts.setEmbed(true);
        opts.setFrameExpansion(true);
        return JsonLdProcessor.frame(json, context, opts);
    }

    public static Object createJsonObject(String jsonLd) {
        Object o = null;

        try {
            InputStream inputStream =
                    new ByteArrayInputStream(jsonLd.getBytes(Charsets.UTF_8));
            o = JsonUtils.fromInputStream(inputStream);

        } catch (IOException e) {
            throw new RuntimeException("Unable to create object from json ld");
        }
        return o;
    }
}
