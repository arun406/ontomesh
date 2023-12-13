package io.ontomesh.onerecord.client.converter;

import io.ontomesh.onerecord.client.handlers.BlankNodeReplacingStatementHandler;
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

    private final RDFFormat rdfFormat;
    private final BlankNodeReplacingStatementHandler statementHandler;

    public StringToModelConverter(RDFFormat rdfFormat, BlankNodeReplacingStatementHandler statementHandler) {
        this.rdfFormat = rdfFormat;
        this.statementHandler = statementHandler;
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

    private RDFParser getParser() {
        RDFParser parser = Rio.createParser(rdfFormat);
        parser.setRDFHandler(statementHandler);
        return parser;
    }
}
