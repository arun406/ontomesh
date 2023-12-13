package io.ontomesh.onerecord.server;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.base.Charsets;
import io.ontomesh.onerecord.server.api.IdProvider;
import io.ontomesh.onerecord.server.handler.BlankNodeReplacingStatementHandler;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.iata.onerecord.api.API;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class OneRecordServerApplicationTests {

    @Test
    void contextLoads() {
    }

    private final RDFParser rdfParser = Rio.createParser(RDFFormat.JSONLD);

    @Autowired
    private IdProvider idProvider;

    BlankNodeReplacingStatementHandler statementHandler;

    public void init() {
        System.out.println("init");
        statementHandler = new BlankNodeReplacingStatementHandler(idProvider);
        rdfParser.setRDFHandler(statementHandler);
    }

    @Test
    public void loadChangeRequest() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("example-1.jsonld")) {
            init();
            rdfParser.parse(in);
            Model model = statementHandler.getModel();
            Rio.write(model, System.out, RDFFormat.TURTLE);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void loadAuditRequest() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("audit-example-1.jsonld")) {
            init();
            rdfParser.parse(in);
            Model model = statementHandler.getModel();
            Rio.write(model, System.out, RDFFormat.TURTLE);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createAuditRequest() throws IOException {
        Model audit;
        init();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("audit-example-1.jsonld")) {
            rdfParser.parse(in);

            audit = statementHandler.getModel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Model changeRequest;
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("example-1.jsonld")) {
            rdfParser.parse(in);
            changeRequest = statementHandler.getModel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        audit.add(Values.iri("https://1r.example.com/action-requests/6b948f9b-b812-46ed-be39-4501453da99b"), API.hasChange, Values.iri("https://1r.example.com/logistics-objects/1a8ded38-1804-467c-a369-81a411416b7c/Change"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Rio.write(audit, outputStream, RDFFormat.TURTLE);
        // Convert the ByteArrayOutputStream to an InputStream.
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Close the ByteArrayOutputStream.
        outputStream.close();
        System.out.println("***********-----------------***************");
        System.out.println(getPrettyJsonLdString(readRdfToString(inputStream, RDFFormat.TURTLE, RDFFormat.JSONLD, "")));
    }

    public static String modelToString(Collection<Statement> model,
                                       RDFFormat outf) {
        StringWriter out = new StringWriter();
        RDFWriter writer = Rio.createWriter(outf, out);
        try {
            writer.startRDF();
            for (Statement st : model) {
                writer.handleStatement(st);
            }
            writer.endRDF();
        } catch (RDFHandlerException e) {
            throw new RuntimeException(e);
        }
        return out.getBuffer().toString();
    }

    public static String readRdfToString(InputStream in, RDFFormat inf,
                                         RDFFormat outf, String baseUrl) {
        Collection<Statement> myGraph = null;
        myGraph = readRdfToGraph(in, inf, baseUrl);
        return modelToString(myGraph, outf);
    }

    public static Collection<Statement> readRdfToGraph(
            final InputStream inputStream, final RDFFormat inf,
            final String baseUrl) {
        try {
            final RDFParser rdfParser = Rio.createParser(inf);
            final StatementCollector collector = new StatementCollector();
            rdfParser.setRDFHandler(collector);
            rdfParser.parse(inputStream, baseUrl);
            return collector.getStatements();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getPrettyJsonLdString(String rdfGraphAsJson) {
        try {
            //@formatter:off
            return JsonUtils
                    .toPrettyString(
                            removeGraphArray(
                                    getFramedJson(
                                            createJsonObject(
                                                    rdfGraphAsJson))));
            //@formatter:on
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> removeGraphArray(Map<String, Object> framedJson) {
        List<Map<String, Object>> graph = (List<Map<String, Object>>) framedJson.get("@graph");
        return graph.get(0);
    }

    private static Map<String, Object> getFramedJson(Object json) {
        try {
            return JsonLdProcessor.frame(json, getFrame(), new JsonLdOptions());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Map> getFrame() {
        Map<String, Map> context = new HashMap<>();
        context.put("@context", new HashMap<>());
        context.get("@context").put("cargo", "https://onerecord.iata.org/ns/cargo#");
        context.get("@context").put("api", "https://onerecord.iata.org/ns/api#");
        return context;
    }

    private static Object createJsonObject(String ld) {
        try (InputStream inputStream =
                     new ByteArrayInputStream(ld.getBytes(Charsets.UTF_8))) {
            Object jsonObject = JsonUtils.fromInputStream(inputStream);
            return jsonObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
