package eu.fbk.dkm.pikes.rdf.vocab;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.NamespaceImpl;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;

/**
 *
 * @see https://catalog.ldc.upenn.edu/docs/LDC2005T33/BBN-Types-Subtypes.html
 */
public final class BBN {

    public static final String PREFIX = "bbn";

    public static final String NAMESPACE = "http://dkm.fbk.eu/ontologies/bbn#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    private static final Map<String, IRI> INDEX;

    // RESOURCE LAYER

    public static final IRI PERSON = createIRI("Resource");

    public static final IRI NORP = createIRI("NORP");

    public static final IRI FACILITY = createIRI("Facility");

    public static final IRI ORGANIZATION = createIRI("Organization");

    public static final IRI GPE = createIRI("GPE");

    public static final IRI LOCATION = createIRI("Location");

    public static final IRI PRODUCT = createIRI("Product");

    public static final IRI DATE = createIRI("Date");

    public static final IRI TIME = createIRI("Time");

    public static final IRI PERCENT = createIRI("Percent");

    public static final IRI MONEY = createIRI("Money");

    public static final IRI QUANTITY = createIRI("Quantity");

    public static final IRI ORDINAL = createIRI("Ordinal");

    public static final IRI CARDINAL = createIRI("Cardinal");

    public static final IRI EVENT = createIRI("Event");

    public static final IRI PLANT = createIRI("Plant");

    public static final IRI ANIMAL = createIRI("Animal");

    public static final IRI SUBSTANCE = createIRI("Substance");

    public static final IRI DISEASE = createIRI("Disease");

    public static final IRI WORK_OF_ART = createIRI("WorkOfArt");

    public static final IRI LAW = createIRI("Law");

    public static final IRI LANGUAGE = createIRI("Language");

    public static final IRI CONTACT_INFO = createIRI("ContactInfo");

    public static final IRI GAME = createIRI("Game");

    // UTILITY METHODS

    @Nullable
    public static IRI resolve(@Nullable final String bbnString) {
        if (bbnString == null) {
            return null;
        }
        return INDEX.get(bbnString.toLowerCase().replaceAll("[^a-z]+", ""));
    }

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private BBN() {
    }

    static {
        final Map<String, IRI> map = Maps.newHashMap();
        map.put("person", PERSON);
        map.put("per", PERSON);
        map.put("norp", NORP);
        map.put("facility", FACILITY);
        map.put("fac", FACILITY);
        map.put("organization", ORGANIZATION);
        map.put("organisation", ORGANIZATION);
        map.put("org", ORGANIZATION);
        map.put("gpe", GPE);
        map.put("location", LOCATION);
        map.put("loc", LOCATION);
        map.put("product", PRODUCT);
        map.put("date", DATE);
        map.put("time", TIME);
        map.put("percent", PERCENT);
        map.put("money", MONEY);
        map.put("quantity", QUANTITY);
        map.put("ordinal", ORDINAL);
        map.put("cardinal", CARDINAL);
        map.put("event", EVENT);
        map.put("plant", PLANT);
        map.put("animal", ANIMAL);
        map.put("substance", SUBSTANCE);
        map.put("disease", DISEASE);
        map.put("workofart", WORK_OF_ART);
        map.put("law", LAW);
        map.put("language", LANGUAGE);
        map.put("contactinfo", CONTACT_INFO);
        map.put("game", GAME);
        INDEX = ImmutableMap.copyOf(map);
    }

}
