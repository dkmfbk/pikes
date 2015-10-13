package eu.fbk.dkm.pikes.eval;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

public class EVAL {

    public static final String PREFIX = "eval";

    public static final String NAMESPACE = "http://pikes.fbk.eu/ontologies/eval#";

    public static final Namespace NS = new NamespaceImpl(PREFIX, NAMESPACE);

    public static final URI SENTENCE = createURI("Sentence");

    public static final URI NODE = createURI("Node");

    public static final URI ENTITY = createURI("Entity");

    public static final URI FRAME = createURI("Frame");

    public static final URI QUALITY = createURI("Quality");

    public static final URI VERB = createURI("Verb");

    public static final URI KNOWLEDGE_GRAPH = createURI("KnowledgeGraph");

    public static final URI METADATA = createURI("metadata");

    public static final URI DENOTED_BY = createURI("denotedBy");

    public static final URI MAPPED_TO = createURI("mappedTo");

    public static final URI ASSOCIABLE_TO = createURI("associableTo");

    public static final URI NOT_ASSOCIABLE_TO = createURI("notAssociableTo");

    private static URI createURI(final String localName) {
        return ValueFactoryImpl.getInstance().createURI(NAMESPACE, localName);
    }

    private EVAL() {
    }

}
