package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class GAF {

    public static final String PREFIX = "gaf";

    public static final String NAMESPACE = "http://groundedannotationframework.org/gaf#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // PROPERTIES

    public static final IRI DENOTED_BY = createIRI("denotedBy");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private GAF() {
    }

}
