package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Simple Event Model (SEM) Ontology.
 *
 * @see <a href="http://semanticweb.cs.vu.nl/2009/11/sem/">vocabulary specification</a>
 */
public final class SEM {

    /** Recommended prefix for the vocabulary namespace: "sem". */
    public static final String PREFIX = "sem";

    /** Vocabulary namespace: "http://semanticweb.cs.vu.nl/2009/11/sem/". */
    public static final String NAMESPACE = "http://semanticweb.cs.vu.nl/2009/11/sem/";

    /** Immutable {@link Namespace} constant for the vocabulary namespace. */
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // CLASSES

    /** Class sem:Actor. */
    public static final IRI ACTOR = createIRI("Actor");

    /** Class sem:ActorType. */
    public static final IRI ACTOR_TYPE = createIRI("ActorType");

    /** Class sem:Authority. */
    public static final IRI AUTHORITY = createIRI("Authority");

    /** Class sem:Constraint. */
    public static final IRI CONSTRAINT = createIRI("Constraint");

    /** Class sem:Core. */
    public static final IRI CORE = createIRI("Core");

    /** Class sem:Event. */
    public static final IRI EVENT = createIRI("Event");

    /** Class sem:EventType. */
    public static final IRI EVENT_TYPE = createIRI("EventType");

    /** Class sem:Object. */
    public static final IRI OBJECT = createIRI("Object");

    /** Class sem:Place. */
    public static final IRI PLACE = createIRI("Place");

    /** Class sem:PlaceType. */
    public static final IRI PLACE_TYPE = createIRI("PlaceType");

    /** Class sem:Role. */
    public static final IRI ROLE = createIRI("Role");

    /** Class sem:RoleType. */
    public static final IRI ROLE_TYPE = createIRI("RoleType");

    /** Class sem:Temporary. */
    public static final IRI TEMPORARY = createIRI("Temporary");

    /** Class sem:Time. */
    public static final IRI TIME = createIRI("Time");

    /** Class sem:TimeType. */
    public static final IRI TIME_TYPE = createIRI("TimeType");

    /** Class sem:Type. */
    public static final IRI TYPE = createIRI("Type");

    /** Class sem:View. */
    public static final IRI VIEW = createIRI("View");

    // PROPERTIES

    /** Property sem:accordingTo. */
    public static final IRI ACCORDING_TO = createIRI("accordingTo");

    /** Property sem:actorType. */
    public static final IRI ACTOR_TYPE_PROPERTY = createIRI("actorType");

    /** Property sem:eventProperty. */
    public static final IRI EVENT_PROPERTY = createIRI("eventProperty");

    /** Property sem:eventType. */
    public static final IRI EVENT_TYPE_PROPERTY = createIRI("eventType");

    /** Property sem:hasActor. */
    public static final IRI HAS_ACTOR = createIRI("hasActor");

    /** Property sem:hasBeginTimeStamp. */
    public static final IRI HAS_BEGIN_TIME_STAMP = createIRI("hasBeginTimeStamp");

    /** Property sem:hasEarliestBeginTimeStamp. */
    public static final IRI HAS_EARLIEST_BEGIN_TIME_STAMP = createIRI("hasEarliestBeginTimeStamp");

    /** Property sem:hasEarliestEndTimeStamp. */
    public static final IRI HAS_EARLIEST_END_TIME_STAMP = createIRI("hasEarliestEndTimeStamp");

    /** Property sem:hasEndTimeStamp. */
    public static final IRI HAS_END_TIME_STAMP = createIRI("hasEndTimeStamp");

    /** Property sem:hasLatestBeginTimeStamp. */
    public static final IRI HAS_LATEST_BEGIN_TIME_STAMP = createIRI("hasLatestBeginTimeStamp");

    /** Property sem:hasLatestEndTimeStamp. */
    public static final IRI HAS_LATEST_END_TIME_STAMP = createIRI("hasLatestEndTimeStamp");

    /** Property sem:hasPlace. */
    public static final IRI HAS_PLACE = createIRI("hasPlace");

    /** Property sem:hasSubEvent. */
    public static final IRI HAS_SUB_EVENT = createIRI("hasSubEvent");

    /** Property sem:hasSubType. */
    public static final IRI HAS_SUB_TYPE = createIRI("hasSubType");

    /** Property sem:hasTime. */
    public static final IRI HAS_TIME = createIRI("hasTime");

    /** Property sem:hasTimeStamp. */
    public static final IRI HAS_TIME_STAMP = createIRI("hasTimeStamp");

    /** Property sem:placeType. */
    public static final IRI PLACE_TYPE_PROPERTY = createIRI("placeType");

    /** Property sem:roleType. */
    public static final IRI ROLE_TYPE_PROPERTY = createIRI("roleType");

    /** Property sem:subEventOf. */
    public static final IRI SUB_EVENT_OF = createIRI("subEventOf");

    /** Property sem:subTypeOf. */
    public static final IRI SUB_TYPE_OF = createIRI("subTypeOf");

    /** Property sem:timeType. */
    public static final IRI TIME_TYPE_PROPERTY = createIRI("timeType");

    /** Property sem:type. */
    public static final IRI TYPE_PROPERTY = createIRI("type");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private SEM() {
    }

}
