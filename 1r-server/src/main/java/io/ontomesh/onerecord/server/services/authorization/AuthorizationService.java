package io.ontomesh.onerecord.server.services.authorization;

import io.ontomesh.onerecord.server.common.IdProvider;
import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.exception.LogisticsObjectNotFoundException;
import io.ontomesh.onerecord.server.repository.LogisticsObjectRepository;
import io.ontomesh.onerecord.server.services.logisticsobjects.AbstractLogisticsObjectService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthorizationService extends AbstractLogisticsObjectService {

    /* Query to fetch the Authorization acl */
    private static final String GET_AUTHORIZATION_QUERY = "SELECT DISTINCT ?subject ?property ?value\n" +
            "WHERE { " +
            "  ?subject a <http://www.w3.org/ns/auth/acl#Authorization> ." +
            "  ?subject ?property ?value " +
            "  FILTER(?subject = ?aclId)" +
            "}";

    private static final String DELETE_AUTHORIZATION_QUERY = "DELETE {" +
            "  ?id ?p ?v ." +
            "}" +
            "WHERE {" +
            "  ?id a <http://www.w3.org/ns/auth/acl#Authorization> ." +
            "  ?id ?p ?v . " +
            "  FILTER (?id = ?aclId)" +
            "}";

    private static final String ASK_AUTHORIZATION_QUERY = "PREFIX acl: <http://www.w3.org/ns/auth/acl#>\n" +
            "\n" +
            "ASK {\n" +
            "  ?authorization a acl:Authorization ;\n" +
            "                 acl:agent ?agent ;\n" +
            "                 acl:accessTo ?resource ;\n" +
            "                 acl:mode ?mode . \n" +
            "}\n";

    protected AuthorizationService(LogisticsObjectRepository repository, IdProvider idProvider) {
        super(repository, idProvider);
    }

    /**
     * Get Authorization ACL by IRI.
     *
     * @param iri
     * @return
     */
    public LogisticsObject getACL(IRI iri) {

        TupleQuery tupleQuery = getRepository().getConnection().prepareTupleQuery(GET_AUTHORIZATION_QUERY);
        tupleQuery.setBinding("aclId", iri);
        // Execute the query and get the results
        TupleQueryResult result = tupleQuery.evaluate();

        if (!result.hasNext()) {
            throw new LogisticsObjectNotFoundException(iri, "Authorization acl not found.");
        }
        // Create a default model using ModelFactory
        Model model = new LinkedHashModelFactory().createEmptyModel();
        // Iterate over the result set and convert to RDF triples
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            IRI predicate = null;
            Value property = bindingSet.getValue("property");
            if (property.isIRI()) {
                predicate = (IRI) property;
            }
            Value value = bindingSet.getValue("value");
            // Add the triple to the model
            model.add(iri, predicate, value);
        }

        return new LogisticsObject(iri, model);
    }


    /**
     * Delete Authorization ACL by IRI
     *
     * @param iri
     */
    public void deleteACL(IRI iri) {
        // Prepare the query
        Update update = getRepository().getConnection()
                .prepareUpdate(QueryLanguage.SPARQL, DELETE_AUTHORIZATION_QUERY);
        // set binding
        update.setBinding("aclId", iri);
        // execute
        update.execute();

    }

    public boolean hasAccessToResource(IRI agent, IRI resource, IRI mode) {
        BooleanQuery booleanQuery = getRepository().getConnection()
                .prepareBooleanQuery(QueryLanguage.SPARQL, ASK_AUTHORIZATION_QUERY);
        booleanQuery.setBinding("agent", agent);
        booleanQuery.setBinding("resource", resource);
        booleanQuery.setBinding("mode", mode);
        boolean result = booleanQuery.evaluate();

        log.debug("Does the access exist ? {} ", result);
        return result;
    }


}
