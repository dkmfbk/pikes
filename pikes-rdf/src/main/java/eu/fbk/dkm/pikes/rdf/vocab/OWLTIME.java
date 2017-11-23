package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class OWLTIME {

    public static final String PREFIX = "owltime";

    public static final String NAMESPACE = "http://www.w3.org/TR/owl-time#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // CLASSES

    public static final IRI DATE_TIME_DESCRIPTION = createIRI("DateTimeDescription");

    public static final IRI DATE_TIME_INTERVAL = createIRI("DateTimeInterval");

    public static final IRI DAY_OF_WEEK_CLASS = createIRI("DayOfWeek");

    public static final IRI DURATION_DESCRIPTION = createIRI("DurationDescription");

    public static final IRI INSTANT = createIRI("Instant");

    public static final IRI INTERVAL = createIRI("Interval");

    public static final IRI PROPER_INTERVAL = createIRI("ProperInterval");

    public static final IRI TEMPORAL_ENTITY = createIRI("TemporalEntity");

    public static final IRI TEMPORAL_UNIT = createIRI("TemporalUnit");

    // PROPERTIES

    public static final IRI AFTER = createIRI("after");

    public static final IRI BEFORE = createIRI("before");

    public static final IRI DAY = createIRI("day");

    public static final IRI DAY_OF_WEEK = createIRI("dayOfWeek");

    public static final IRI DAY_OF_YEAR = createIRI("dayOfYear");

    public static final IRI DAYS = createIRI("days");

    public static final IRI HAS_BEGINNING = createIRI("hasBeginning");

    public static final IRI HAS_DATE_TIME_DESCRIPTION = createIRI("hasDateTimeDescription");

    public static final IRI HAS_DURATION_DESCRIPTION = createIRI("hasDurationDescription");

    public static final IRI HAS_END = createIRI("hasEnd");

    public static final IRI HOUR = createIRI("hour");

    public static final IRI HOURS = createIRI("hours");

    public static final IRI IN_DATE_TIME = createIRI("inDateTime");

    public static final IRI INSIDE = createIRI("inside");

    public static final IRI INTERVAL_AFTER = createIRI("intervalAfter");

    public static final IRI INTERVAL_BEFORE = createIRI("intervalBefore");

    public static final IRI INTERVAL_CONTAINS = createIRI("intervalContains");

    public static final IRI INTERVAL_DIRING = createIRI("intervalDuring");

    public static final IRI INTERVAL_EQUALS = createIRI("intervalEquals");

    public static final IRI INTERVAL_FINISHED_BY = createIRI("intervalFinishedBy");

    public static final IRI INTERVAL_FINISHES = createIRI("intervalFinishes");

    public static final IRI INTERVAL_MEETS = createIRI("intervalMeets");

    public static final IRI INTERVAL_MET_BY = createIRI("intervalMetBy");

    public static final IRI INTERVAL_OVERLAPPED_BY = createIRI("intervalOverlappedBy");

    public static final IRI INTERVAL_OVERLAPS = createIRI("intervalOverlaps");

    public static final IRI INTERVAL_STARTED_BY = createIRI("intervalStartedBy");

    public static final IRI INTERVAL_STARTS = createIRI("intervalStarts");

    public static final IRI IN_XSD_DATE_TIME = createIRI("inXSDDateTime");

    public static final IRI MINUTE = createIRI("minute");

    public static final IRI MINUTES = createIRI("minutes");

    public static final IRI MONTH = createIRI("month");

    public static final IRI MONTHS = createIRI("months");

    public static final IRI SECOND = createIRI("second");

    public static final IRI SECONDS = createIRI("seconds");

    public static final IRI TIME_ZONE = createIRI("timeZone");

    public static final IRI UNIT_TYPE = createIRI("unitType");

    public static final IRI WEEK = createIRI("week");

    public static final IRI WEEKS = createIRI("weeks");

    public static final IRI XSD_DATE_TIME = createIRI("xsdDateTime");

    public static final IRI YEAR = createIRI("year");

    public static final IRI YEARS = createIRI("years");

    // INDIVIDUALS

    public static final IRI UNIT_SECOND = createIRI("unitSecond");

    public static final IRI UNIT_MINUTE = createIRI("unitMinute");

    public static final IRI UNIT_HOUR = createIRI("unitHour");

    public static final IRI UNIT_DAY = createIRI("unitDay");

    public static final IRI UNIT_WEEK = createIRI("unitWeek");

    public static final IRI UNIT_MONTH = createIRI("unitMonth");

    public static final IRI UNIT_YEAR = createIRI("unitYear");

    public static final IRI MONDAY = createIRI("Monday");

    public static final IRI TUESDAY = createIRI("Tuesday");

    public static final IRI WEDNESDAY = createIRI("Wednesday");

    public static final IRI THURSDAY = createIRI("Thursday");

    public static final IRI FRIDAY = createIRI("Friday");

    public static final IRI SATURDAY = createIRI("Saturday");

    public static final IRI SUNDAY = createIRI("Sunday");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private OWLTIME() {
    }

}
