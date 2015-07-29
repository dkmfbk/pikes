package eu.fbk.dkm.pikes.rdf.vocab;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 *
 * @see https://catalog.ldc.upenn.edu/docs/LDC2005T33/BBN-Types-Subtypes.html
 */
public final class BBN {

    public static final String PREFIX = "bbn";

    public static final String NAMESPACE = "http://dkm.fbk.eu/ontologies/bbn#";

    public static final Namespace NS = new NamespaceImpl(PREFIX, NAMESPACE);

    private static final Map<String, URI> INDEX;

    // RESOURCE LAYER

    public static final URI PERSON = createURI("Resource");

    public static final URI NORP = createURI("NORP");

    public static final URI FACILITY = createURI("Facility");

    public static final URI ORGANIZATION = createURI("Organization");

    public static final URI GPE = createURI("GPE");

    public static final URI LOCATION = createURI("Location");

    public static final URI PRODUCT = createURI("Product");

    public static final URI DATE = createURI("Date");

    public static final URI TIME = createURI("Time");

    public static final URI PERCENT = createURI("Percent");

    public static final URI MONEY = createURI("Money");

    public static final URI QUANTITY = createURI("Quantity");

    public static final URI ORDINAL = createURI("Ordinal");

    public static final URI CARDINAL = createURI("Cardinal");

    public static final URI EVENT = createURI("Event");

    public static final URI PLANT = createURI("Plant");

    public static final URI ANIMAL = createURI("Animal");

    public static final URI SUBSTANCE = createURI("Substance");

    public static final URI DISEASE = createURI("Disease");

    public static final URI WORK_OF_ART = createURI("WorkOfArt");

    public static final URI LAW = createURI("Law");

    public static final URI LANGUAGE = createURI("Language");

    public static final URI CONTACT_INFO = createURI("ContactInfo");

    public static final URI GAME = createURI("Game");

    // UTILITY METHODS

    @Nullable
    public static URI resolve(@Nullable final String bbnString) {
        if (bbnString == null) {
            return null;
        }
        return INDEX.get(bbnString.toLowerCase().replaceAll("[^a-z]+", ""));
    }

    private static URI createURI(final String localName) {
        return ValueFactoryImpl.getInstance().createURI(NAMESPACE, localName);
    }

    private BBN() {
    }

    static {
        final Map<String, URI> map = Maps.newHashMap();
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
