package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class SUMO {

    public static final String PREFIX = "sumo";

    public static final String NAMESPACE = "http://www.ontologyportal.org/SUMO.owl#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public static final IRI ENTITY = createIRI("Entity");

    public static final IRI PROCESS = createIRI("Process");

    public static final IRI RELATION = createIRI("Relation");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private SUMO() {
    }

}
