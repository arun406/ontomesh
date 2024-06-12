package io.ontomesh.onerecord.server.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.ontomesh.onerecord.server.domain.ActionRequest;

import java.io.IOException;

public class ActionRequestSerializer extends StdSerializer<ActionRequest> {

    public ActionRequestSerializer() {
        this(null);
    }

    public ActionRequestSerializer(Class<ActionRequest> t) {
        super(t);
    }

    @Override
    public void serialize(ActionRequest actionRequest, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("@id", actionRequest.iri().toString());
        jsonGenerator.writeStringField("status", actionRequest.status().toString());
        jsonGenerator.writeStringField("requestedAt", actionRequest.requestedAt().toString());
        jsonGenerator.writeEndObject();
    }
}
