package io.ontomesh.onerecord.server.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Converter
public class IRIListConverter implements AttributeConverter<List<IRI>, String> {

    private static final String SPLIT_CHAR = ";";

    @Override
    public String convertToDatabaseColumn(List<IRI> iris) {
        return iris != null ? iris.stream().map(Value::stringValue)
                .collect(Collectors.joining(SPLIT_CHAR)) : "";
    }

    @Override
    public List<IRI> convertToEntityAttribute(String string) {
        return string != null ? Arrays.stream(string.split(SPLIT_CHAR))
                .map(Values::iri)
                .collect(Collectors.toList()) : emptyList();
    }
}
