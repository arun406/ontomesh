package io.akvj.onerecord;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;

import java.io.IOException;

public class Tester {
    public static void main(String[] args) throws IOException {
        Model model1 = new TreeModel();
        Model model2 = new TreeModel();

// Add some statements to model1.
        model1.add(Values.iri("http://example.org/Alice"), FOAF.NAME, Values.literal("Alice"));
        model1.add(Values.iri("http://example.org/Bob"), FOAF.NAME, Values.literal("Bob"));

// Create a new statement with the subject `statement1` and the predicate `FOAF.FRIENDS`.
//        Statement statement1 = Values.getValueFactory().createStatement(Values.iri("http://example.org/Carol"), FOAF.KNOWS, Values.getValueFactory().(model1));

// Add statement1 to model2.
//        model2.add(statement1);
    }
}
