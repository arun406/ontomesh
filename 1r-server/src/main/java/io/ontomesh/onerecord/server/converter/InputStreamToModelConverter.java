package io.ontomesh.onerecord.server.converter;

import io.ontomesh.onerecord.server.handler.BlankNodeReplacingStatementHandler;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class InputStreamToModelConverter implements Converter<InputStream, Model> {
    private final RDFFormat rdfFormat;
    private final BlankNodeReplacingStatementHandler statementHandler;

    public InputStreamToModelConverter(RDFFormat rdfFormat, BlankNodeReplacingStatementHandler statementHandler) {
        this.rdfFormat = rdfFormat;
        this.statementHandler = statementHandler;
    }

    @Override
    public Model convert(InputStream source) {
        RDFParser rdfParser = getParser();
        try {
            rdfParser.parse(source);
        } catch (IOException e) {
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
