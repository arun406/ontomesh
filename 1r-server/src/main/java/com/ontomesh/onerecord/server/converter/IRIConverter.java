package com.ontomesh.onerecord.server.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

@Converter
public class IRIConverter implements AttributeConverter<IRI, String> {
    @Override
    public String convertToDatabaseColumn(IRI iri) {
        return iri != null ? iri.stringValue() : null;
    }

    @Override
    public IRI convertToEntityAttribute(String s) {
        return s != null ? Values.iri(s) : null;
    }
}
