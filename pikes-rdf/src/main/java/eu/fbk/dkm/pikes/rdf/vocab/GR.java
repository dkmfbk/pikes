package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class GR {

    public static final String PREFIX = "gr";

    public static final String NAMESPACE = "http://purl.org/goodrelations/v1#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // TERMS

    public static final IRI PRICE_SPECIFICATION = createIRI("PriceSpecification");

    public static final IRI HAS_CURRENCY = createIRI("hasCurrency");

    public static final IRI HAS_CURRENCY_VALUE = createIRI("hasCurrencyValue");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private GR() {
    }

}
