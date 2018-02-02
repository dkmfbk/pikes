package eu.fbk.dkm.pikes.rdf.vocab;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import eu.fbk.rdfpro.vocab.VOID;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the KEM Core vocabulary.
 */
public class KEM {

    /** Recommended prefix for the vocabulary namespace: "kem". */
    public static final String PREFIX = "kem";

    /** Vocabulary namespace: "http://knowledgestore.fbk.eu/ontologies/kem/core#". */
    public static final String NAMESPACE = "http://knowledgestore.fbk.eu/ontologies/kem/core#";

    /** Immutable {@link Namespace} constant for the vocabulary namespace. */
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // CLASSES

    /** Class kem:Graph. */
    public static final IRI GRAPH = createIRI("Graph");

    /** Class kem:Mention. */
    public static final IRI MENTION = createIRI("Mention");

    /** Class kem:Fragment. */
    public static final IRI FRAGMENT = createIRI("Fragment");

    /** Class kem:Resource. */
    public static final IRI RESOURCE = createIRI("Resource");

    /** Class kem:Annotation. */
    public static final IRI ANNOTATION = createIRI("Annotation");

    /** Class kem:Instance. */
    public static final IRI INSTANCE = createIRI("Instance");

    /** Class kem:SemanticAnnotation. */
    public static final IRI SEMANTIC_ANNOTATION = createIRI("SemanticAnnotation");

    /** Class kem:CompositeFragment. */
    public static final IRI COMPOSITE_FRAGMENT = createIRI("CompositeFragment");

    /** Class kem:CompositeResource. */
    public static final IRI COMPOSITE_RESOURCE = createIRI("CompositeResource");

    // OBJECT PROPERTIES

    /** Object property kem:conveys. */
    public static final IRI CONVEYS = createIRI("conveys");

    /** Object property kem:hasAnnotation. */
    public static final IRI HAS_ANNOTATION = createIRI("hasAnnotation");

    /** Object property kem:substantiates. */
    public static final IRI SUBSTANTIATES = createIRI("substantiates");

    /** Object property kem:fragmentOf. */
    public static final IRI FRAGMENT_OF = createIRI("fragmentOf");

    /** Object property kem:hasPart. */
    public static final IRI HAS_PART = createIRI("hasPart");

    /** Object property kem:hasComponent. */
    public static final IRI HAS_COMPONENT = createIRI("hasComponent");

    /** Object property kem:hasResourceAnnotation. */
    public static final IRI HAS_RESOURCE_ANNOTATION = createIRI("hasResourceAnnotation");

    /** Object property kem:involves. */
    public static final IRI INVOLVES = createIRI("involves");

    /** Object property kem:involvesSubjectOf. */
    public static final IRI INVOLVES_SUBJECT_OF = createIRI("involvesSubjectOf");

    /** Object property kem:subject. */
    public static final IRI SUBJECT = createIRI("subject");

    /** Object property kem:involvesReferentOf. */
    public static final IRI INVOLVES_REFERENT_OF = createIRI("involvesReferentOf");

    /** Object property kem:refersTo. */
    public static final IRI REFERS_TO = createIRI("refersTo");

    /** Object property kem:isAbout. */
    public static final IRI IS_ABOUT = createIRI("isAbout");


    // ALL TERMS

    /** Set of terms defined in this vocabulary. */
    public static Set<IRI> TERMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(GRAPH,
            MENTION, FRAGMENT, RESOURCE, ANNOTATION, INSTANCE, SEMANTIC_ANNOTATION,
            COMPOSITE_FRAGMENT, COMPOSITE_RESOURCE, CONVEYS, HAS_ANNOTATION, SUBSTANTIATES,
            FRAGMENT_OF, HAS_PART, HAS_COMPONENT, HAS_RESOURCE_ANNOTATION, INVOLVES,
            INVOLVES_SUBJECT_OF, SUBJECT, INVOLVES_REFERENT_OF, REFERS_TO, IS_ABOUT)));

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private KEM() {
    }

}
