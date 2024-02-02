package io.ontomesh.onerecord.server.converter;

import io.ontomesh.onerecord.server.handler.DefaultStatementHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class StringToModelConverter implements Converter<String, Model> {

    private RDFFormat rdfFormat;

    private DefaultStatementHandler statementHandler;

    public StringToModelConverter(RDFFormat rdfFormat, DefaultStatementHandler statementHandler) {
        this.rdfFormat = rdfFormat;
        this.statementHandler = statementHandler;
    }

    private RDFParser getParser() {
        RDFParser parser = Rio.createParser(rdfFormat);
        parser.setRDFHandler(statementHandler);
        return parser;
    }

    @Override
    public Model convert(String source) {
        RDFParser rdfParser = getParser();
        try {
            rdfParser.parse(IOUtils.toInputStream(source, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("error parsing the string", e);
            throw new RuntimeException(e.getMessage());
        }
        return statementHandler.getModel();
    }
}
