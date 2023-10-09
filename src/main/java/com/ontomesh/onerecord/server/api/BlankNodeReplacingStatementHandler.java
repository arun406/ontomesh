package com.ontomesh.onerecord.server.api;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;


@Component
@RequestScope
public class BlankNodeReplacingStatementHandler extends AbstractRDFHandler {
    private final Model result = new DynamicModelFactory().createEmptyModel();

    private final IdProvider idProvider;

    private final Map<String, IRI> iriMap = new HashMap<>();

    public BlankNodeReplacingStatementHandler(IdProvider idProvider) {
        this.idProvider = idProvider;
    }

    public Model getModel() {
        return this.result;
    }

    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        result.setNamespace(prefix, uri);
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        Resource subject = st.getSubject();
        Value object = st.getObject();

        if (st.getSubject().isBNode()) {
            subject = mapBNode((BNode) subject);
        }

        if (st.getObject().isBNode()) {
            object = mapBNode((BNode) object);
        }

        result.add(subject, st.getPredicate(), object, st.getContext());
    }

    private Resource mapBNode(BNode bNode) {
        return iriMap.computeIfAbsent(
                bNode.stringValue(), s -> idProvider.createInternalIri().iri()
        );
    }
}
