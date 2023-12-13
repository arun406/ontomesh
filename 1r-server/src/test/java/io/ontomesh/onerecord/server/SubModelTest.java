package io.ontomesh.onerecord.server;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import io.ontomesh.onerecord.server.util.JsonldUtil;
import io.ontomesh.onerecord.server.converter.ModelToStringConverter;
import io.ontomesh.onerecord.server.util.RDFUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.iata.onerecord.api.API;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@SpringBootTest
@Slf4j
public class SubModelTest {


    @Value("classpath:action-request.jsonld")
    Resource actionRequest;

    @Test
    public void extractSubModel() throws IOException {
        InputStream inputStream = actionRequest.getInputStream();
        Model actionRequest = Rio.parse(inputStream, RDFFormat.JSONLD);

        Model model = RDFUtil.getSubModel(actionRequest, API.hasChange);
//        RDFUtil.print(model, RDFFormat.TURTLE);

        ModelToStringConverter modelToStringConverter = new ModelToStringConverter(RDFFormat.JSONLD);
        String rdfString = modelToStringConverter.convert(model);
        IRI iri = RDFUtil.getRootIri(model);
        log.debug("IRI : {}", iri);
        IRI type = RDFUtil.getType(model, iri);
        rdfString = JsonldUtil.jsonLdToString(rdfString, true, type);
        Object compact = JsonLdProcessor.compact(JsonUtils.fromString(rdfString), JsonldUtil.getContext(), new JsonLdOptions());
        rdfString = JsonUtils.toPrettyString(compact);
        log.debug("{}", rdfString);
    }


}
