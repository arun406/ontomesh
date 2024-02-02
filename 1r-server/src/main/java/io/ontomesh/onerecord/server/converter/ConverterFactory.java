package io.ontomesh.onerecord.server.converter;

import io.ontomesh.onerecord.server.api.enums.RDFConverterType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ConverterFactory {
    private final Map<RDFConverterType, Converter> converters = new HashMap<>();


    public ConverterFactory(@Qualifier("stringToJsonLdConverter") StringToModelConverter stringToJsonLdConverter,
                            @Qualifier("stringToTurtleConverter") StringToModelConverter stringToTurtleConverter,
                            @Qualifier("modelToTurtleStringConverter") ModelToStringConverter turtleToStringConverter,
                            @Qualifier("modelToJsonLdStringConverter") ModelToStringConverter jsonLdToStringConverter) {
        converters.put(RDFConverterType.STRING_TO_JSONLD_CONVERTER, stringToJsonLdConverter);
        converters.put(RDFConverterType.STRING_TO_TURTLE_CONVERTER, stringToTurtleConverter);
        converters.put(RDFConverterType.TURTLE_TO_STRING_CONVERTER, turtleToStringConverter);
        converters.put(RDFConverterType.JSONLD_TO_STRING_CONVERTER, jsonLdToStringConverter);
    }

    public Converter get(RDFConverterType converterType) {
        Converter converter = converters.get(converterType);
        if (converter == null) {
            throw new IllegalArgumentException("Unknown converter: " + converterType);
        }
        return converter;
    }
}
