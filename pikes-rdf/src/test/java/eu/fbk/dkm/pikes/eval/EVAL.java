package eu.fbk.dkm.pikes.eval;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class EVAL {

    public static final String PREFIX = "eval";

    public static final String NAMESPACE = "http://pikes.fbk.eu/ontologies/eval#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public static final IRI SENTENCE = createIRI("Sentence");

    public static final IRI NODE = createIRI("Node");

    public static final IRI ENTITY = createIRI("Entity");

    public static final IRI FRAME = createIRI("Frame");

    public static final IRI QUALITY = createIRI("Quality");

    public static final IRI VERB = createIRI("Verb");

    public static final IRI KNOWLEDGE_GRAPH = createIRI("KnowledgeGraph");

    public static final IRI METADATA = createIRI("metadata");

    public static final IRI DENOTED_BY = createIRI("denotedBy");

    public static final IRI MAPPED_TO = createIRI("mappedTo");

    public static final IRI ASSOCIABLE_TO = createIRI("associableTo");

    public static final IRI NOT_ASSOCIABLE_TO = createIRI("notAssociableTo");

    public static final IRI CLASSIFIABLE_AS = createIRI("classifiableAs");

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private EVAL() {
    }

}
