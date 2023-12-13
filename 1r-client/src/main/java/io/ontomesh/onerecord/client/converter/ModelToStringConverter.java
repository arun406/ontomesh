package io.ontomesh.onerecord.client.converter;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.core.convert.converter.Converter;

import java.io.StringWriter;

public class ModelToStringConverter implements Converter<Model, String> {

    private final RDFFormat rdfFormat;

    public ModelToStringConverter(RDFFormat rdfFormat) {
        this.rdfFormat = rdfFormat;
    }

    @Override
    public String convert(Model source) {
        StringWriter out = new StringWriter();
        RDFWriter writer = Rio.createWriter(rdfFormat, out);
        writer.startRDF();
        for (Statement st : source) {
            writer.handleStatement(st);
        }
        writer.endRDF();
        return out.getBuffer().toString();
    }
}
