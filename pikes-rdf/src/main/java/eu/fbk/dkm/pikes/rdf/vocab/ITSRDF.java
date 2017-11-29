package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class ITSRDF {


    public static final String PREFIX = "itsrdf";

    public static final String NAMESPACE = "http://www.w3.org/2005/11/its/rdf#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);


    //OBJECT PROPERTIES

    public static final IRI TA_IDENT_REF = createIRI("taIdentRef");
    public static final IRI TERM_INFO_REF = createIRI("termInfoRef");


    //ANNOTATION PROPERTIES

    public static final IRI TA_CLASS_REF = createIRI("taClassRef");
    public static final IRI TA_PROP_REF = createIRI("taPropRef");


    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private ITSRDF() {
    }
}
