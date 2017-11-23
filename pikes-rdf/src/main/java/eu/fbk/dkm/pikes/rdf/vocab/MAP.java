package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class MAP {

    public static final String PREFIX = "map";

    public static final String NAMESPACE = "http://dkm.fbk.eu/ontologies/mapping#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public static final IRI FROM = createIRI("from");

    public static final IRI FROM_NS = createIRI("fromNS");

    public static final IRI FROM_PATTERN = createIRI("fromPattern");

    public static final IRI TO = createIRI("to");

    public static final IRI TO_NS = createIRI("toNS");

    public static final IRI TO_PATTERN = createIRI("toPattern");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private MAP() {
    }

}
