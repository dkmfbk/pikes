package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class NWR {

    public static final String PREFIX = "nwr";

    public static final String NAMESPACE = "http://www.newsreader-project.eu/ontologies/";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public static final IRI PERSON = createIRI("PERSON");

    public static final IRI ORGANIZATION = createIRI("ORGANIZATION");

    public static final IRI LOCATION = createIRI("LOCATION");

    public static final IRI MISC = createIRI("MISC");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private NWR() {
    }

}
