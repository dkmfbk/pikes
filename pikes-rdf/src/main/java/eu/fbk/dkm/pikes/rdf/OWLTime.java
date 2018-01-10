package eu.fbk.dkm.pikes.rdf;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import eu.fbk.dkm.pikes.rdf.vocab.OWLTIME;
import eu.fbk.rdfpro.util.Statements;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.regex.Pattern;

public class OWLTime {

    private static int parseOptInt(final String string) {
        if (string.contains("X")) {
            return -1;
        }
        return Integer.parseInt(string);
    }

    private static String formatOptInt(final int value, final boolean fourDigits) {
        return fourDigits ? value == -1 ? "XXXX" : String.format("%04d", value)
                : value == -1 ? "XX" : String.format("%02d", value);
    }

    private static void emit(final RDFHandler handler, final Resource subj, final IRI pred,
            final Object obj, final Resource ctx) throws RDFHandlerException {
        final Value o = Statements.convert(obj, Value.class);
        if (subj != null && pred != null && o != null) {
            if (ctx == null) {
                handler.handleStatement(Statements.VALUE_FACTORY.createStatement(subj, pred, o));
            } else {
                handler.handleStatement(Statements.VALUE_FACTORY.createStatement(subj, pred, o,
                        ctx));
            }
        }
    }

    public static final class Interval {

        private static final Pattern DATE_TIME_PATTERN = Pattern
                .compile("(?:PRESENT_REF|PAST_REF|FUTURE_REF|[0-9X]{4}"
                        + "(?:-(?:[0-9X]{2}|W[0-9X]{2}|SP|SU|FA|WI)(?:-(?:[0-9X]{2}|WE))?)?)?"
                        + "T?(?:MO|MI|AF|EV|NI|PM|DT|[0-9X]{2}(?:\\:[0-9X]{2}(?:\\:[0-9X]{2})?)?)?");

        private static final Interval UNKNOWN = new Interval(null, null);

        @Nullable
        private final DateTime begin;

        @Nullable
        private final DateTime end;

        private Interval(@Nullable final DateTime begin, @Nullable final DateTime end) {
            this.begin = begin;
            this.end = end;
        }

        public static Interval create(@Nullable final DateTime dateTime) {
            return dateTime == null ? UNKNOWN : new Interval(dateTime, dateTime);
        }

        public static Interval create(@Nullable final DateTime begin, @Nullable final DateTime end) {
            return begin == null && end == null ? UNKNOWN : new Interval(begin, end);

        }

        public static Interval create(@Nullable final Interval begin, @Nullable final Interval end) {
            return create(begin == null ? null : begin.getBegin(),
                    end == null ? null : end.getEnd());
        }

        public static Interval parseTimex(final String value) {

            // Allocate variables for the various date/time components (-1 = unknown)
            int century = -1;
            int decade = -1;
            int year = -1;
            String season = null;
            int month = -1;
            int week = -1;
            boolean weekend = false;
            int day = -1;
            int hour = -1;
            int minute = -1;
            int second = -1;

            // Normalize the value and split it in its date and time parts
            final String normValue = value.trim().toUpperCase();
            if (normValue.isEmpty() || !DATE_TIME_PATTERN.matcher(normValue).matches()) {
                return null;
            }
            final int timeIndex = normValue.endsWith("_REF") ? -1 : normValue.indexOf('T');
            final String timePart = timeIndex >= 0 ? normValue.substring(timeIndex + 1) : null;
            final String datePart = timeIndex >= 0 ? normValue.substring(0, timeIndex) : normValue
                    .contains(":") ? null : normValue;

            // Process the date part
            if (!Strings.isNullOrEmpty(datePart)) {
                if (datePart.equals("PRESENT_REF") || datePart.equals("PAST_REF")
                        || datePart.equals("FUTURE_REF")) {
                    // TODO
                } else {
                    final String[] tokens = datePart.split("-");
                    if (Character.isDigit(tokens[0].charAt(0))
                            && !Character.isDigit(tokens[0].charAt(tokens[0].length() - 1))) {
                        if (tokens[0].length() == 4 && Character.isDigit(tokens[0].charAt(1))) {
                            if (Character.isDigit(tokens[0].charAt(2))) {
                                decade = Integer.parseInt(tokens[0].substring(0, 3)) * 10;
                            } else {
                                century = Integer.parseInt(tokens[0].substring(0, 2)) * 100;
                            }
                        } else {
                            return null;
                        }
                    } else {
                        year = parseOptInt(tokens[0]);
                        if (tokens.length >= 2) {
                            if (!Character.isDigit(tokens[1].charAt(tokens[1].length() - 1))) {
                                season = tokens[1]; // SP SU FA WI
                            } else if (tokens[1].charAt(0) == 'W') {
                                week = parseOptInt(tokens[1].substring(1));
                                if (tokens.length >= 3 && tokens[2].equals("WE")) {
                                    weekend = true;
                                }
                            } else {
                                month = parseOptInt(tokens[1]);
                                day = tokens.length >= 3 ? parseOptInt(tokens[2]) : -1;
                            }
                        }
                    }
                }
            }

            // Process the time part
            if (timePart != null) {
                if (timePart.equals("MO") || timePart.equals("MI") || timePart.equals("AF")
                        || timePart.equals("EV") || timePart.equals("NI") || timePart.equals("PM")
                        || timePart.equals("DT")) {
                    // TODO: handle periods of day
                } else {
                    final String[] tokens = timePart.split(":");
                    hour = parseOptInt(tokens[0]);
                    if (tokens.length >= 2) {
                        minute = parseOptInt(tokens[1]);
                        if (tokens.length >= 3) {
                            second = parseOptInt(tokens[2]);
                        }
                    }
                }
            }

            // Build the interval
            final String fieldMsg = "Unexpected date/time field(s)";
            if (century != -1) {
                Preconditions.checkArgument(decade == -1 && year == -1 && season == null
                        && month == -1 && week == -1 && !weekend && day == -1 && hour == -1
                        && minute == -1 && second == -1, fieldMsg);
                final DateTime begin = DateTime.create(century, 1, -1, 1, -1, -1, -1);
                final DateTime end = DateTime.create(century + 99, 12, -1, 31, -1, -1, -1);
                return create(begin, end);

            } else if (decade != -1) {
                Preconditions.checkArgument(year == -1 && season == null && month == -1
                        && week == -1 && !weekend && day == -1 && hour == -1 && minute == -1
                        && second == -1, fieldMsg);
                final DateTime begin = DateTime.create(decade, 1, -1, 1, -1, -1, -1);
                final DateTime end = DateTime.create(decade + 9, 12, -1, 31, -1, -1, -1);
                return create(begin, end);

            } else if (season != null) {
                Preconditions.checkArgument(month == -1 && week == -1 && !weekend && day == -1
                        && hour == -1 && minute == -1 && second == -1, fieldMsg);
                // for simplicity sake, we consider here the following season boundaries: March
                // 20, June 21, September 22 and December 21
                DateTime begin;
                DateTime end;
                if (season.equals("SP")) {
                    begin = DateTime.create(year - 1, 12, -1, 21, -1, -1, -1);
                    end = DateTime.create(year, 3, -1, 19, -1, -1, -1);
                } else if (season.equals("SU")) {
                    begin = DateTime.create(year, 3, -1, 20, -1, -1, -1);
                    end = DateTime.create(year, 6, -1, 20, -1, -1, -1);
                } else if (season.equals("FA")) {
                    begin = DateTime.create(year, 6, -1, 21, -1, -1, -1);
                    end = DateTime.create(year, 9, -1, 21, -1, -1, -1);
                } else if (season.equals("WI")) {
                    begin = DateTime.create(year, 9, -1, 22, -1, -1, -1);
                    end = DateTime.create(year, 12, -1, 29, -1, -1, -1);
                } else {
                    throw new IllegalArgumentException("Unexpected season ID: " + season);
                }
                return create(begin, end);

            } else if (weekend) {
                Preconditions.checkArgument(year != -1 && week != -1 && month == -1 && day == -1
                        && hour == -1 && minute == -1 && second == -1, fieldMsg);
                final GregorianCalendar c = new GregorianCalendar();
                c.setFirstDayOfWeek(Calendar.MONDAY);
                c.setMinimalDaysInFirstWeek(1);
                c.set(Calendar.YEAR, year);
                c.set(Calendar.WEEK_OF_YEAR, week);
                c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                final DateTime begin = DateTime.create(c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH) + 1, c.get(Calendar.WEEK_OF_YEAR),
                        c.get(Calendar.DAY_OF_MONTH), -1, -1, -1);
                c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                final DateTime end = DateTime.create(c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH) + 1, c.get(Calendar.WEEK_OF_YEAR),
                        c.get(Calendar.DAY_OF_MONTH), -1, -1, -1);
                return create(begin, end);

            } else {
                return create(DateTime.create(year, month, week, day, hour, minute, second));
            }
        }

        public boolean isDateTimeInterval() {
            return this.begin != null && this.end != null && this.begin.equals(this.end);
        }

        @Nullable
        public DateTime getBegin() {
            return this.begin;
        }

        @Nullable
        public DateTime getEnd() {
            return this.end;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof Interval)) {
                return false;
            }
            final Interval other = (Interval) object;
            return Objects.equals(this.begin, other.begin) && Objects.equals(this.end, other.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.begin, this.end);
        }

        public IRI toRDF(final RDFHandler handler, final String namespace, final Resource ctx)
                throws RDFHandlerException {
            final IRI iri = toIRI(namespace);
            if (isDateTimeInterval()) {
                final IRI iriDesc = this.begin.toRDF(handler, namespace, ctx);
                emit(handler, iri, OWLTIME.HAS_DATE_TIME_DESCRIPTION, iriDesc, ctx);
                emit(handler, iri, RDF.TYPE, OWLTIME.DATE_TIME_INTERVAL, ctx);
            } else {
                final IRI beginIRI = this.begin == null ? null : create(this.begin).toRDF(handler,
                        namespace, ctx);
                final IRI endIRI = this.end == null ? null : create(this.end).toRDF(handler,
                        namespace, ctx);
                emit(handler, iri, OWLTIME.INTERVAL_STARTED_BY, beginIRI, ctx);
                emit(handler, iri, OWLTIME.INTERVAL_FINISHED_BY, endIRI, ctx);
            }
            emit(handler, iri, RDF.TYPE, OWLTIME.PROPER_INTERVAL, ctx);
            emit(handler, iri, RDFS.LABEL, toString(), ctx);
            return iri;
        }

        public IRI toIRI(final String namespace) {
            final String localName = toString().replace(" - ", "_").replace(':', '.');
            return Statements.VALUE_FACTORY.createIRI(namespace, localName);
        }

        @Override
        public String toString() {
            if (isDateTimeInterval()) {
                return this.begin.toString();
            } else {
                return (this.begin == null ? "null" : this.begin.toString()) + " - "
                        + (this.end == null ? "null" : this.end.toString());
            }
        }

    }

    public static final class DateTime {

        private final int year;

        private final int month;

        private final int week;

        private final int day;

        private final int dayOfWeek;

        private final int hour;

        private final int minute;

        private final int second;

        private DateTime(final int year, final int month, final int week, final int day,
                final int dayOfWeek, final int hour, final int minute, final int second) {
            this.year = year;
            this.month = month;
            this.week = week;
            this.day = day;
            this.dayOfWeek = dayOfWeek;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }

        public static DateTime create(final int year, int month, int week, final int day,
                final int hour, final int minute, final int second) {

            // Derive month from week, or week from month and day; also derive day of week
            int dayOfWeek = -1;
            if (year != -1 && (week != -1 || month != -1 && day != -1)) {
                final GregorianCalendar c = new GregorianCalendar();
                c.setFirstDayOfWeek(Calendar.MONDAY);
                c.setMinimalDaysInFirstWeek(1);
                c.set(Calendar.YEAR, year);
                if (month != -1 && day != -1) {
                    c.set(Calendar.MONTH, month - 1);
                    c.set(Calendar.DAY_OF_MONTH, day);
                    dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                    if (week == -1) {
                        week = c.get(Calendar.WEEK_OF_YEAR);
                    }
                } else { // week != -1
                    if (day != -1) {
                        c.set(Calendar.DAY_OF_MONTH, day);
                        for (int i = 0; i < 12; ++i) {
                            c.set(Calendar.MONTH, i);
                            if (c.get(Calendar.WEEK_OF_YEAR) == week) {
                                month = i + 1;
                                dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                                break;
                            }
                        }
                    } else if (week == 1) {
                        month = 1;
                    } else {
                        c.set(Calendar.WEEK_OF_YEAR, week);
                        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                        month = c.get(Calendar.MONTH) + 1;
                    }
                }
            }

            return new DateTime(year, month, week, day, dayOfWeek, hour, minute, second);
        }

        public int getYear() {
            return this.year;
        }

        public int getMonth() {
            return this.month;
        }

        public int getWeek() {
            return this.week;
        }

        public int getDay() {
            return this.day;
        }

        public int getDayOfWeek() {
            return this.dayOfWeek;
        }

        public int getHour() {
            return this.hour;
        }

        public int getMinute() {
            return this.minute;
        }

        public int getSecond() {
            return this.second;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof DateTime)) {
                return false;
            }
            final DateTime other = (DateTime) object;
            return this.year == other.year && this.month == other.month && this.week == other.week
                    && this.day == other.day && this.hour == other.hour
                    && this.minute == other.minute && this.second == other.second;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.year, this.month, this.week, this.day, this.hour,
                    this.minute, this.second);
        }

        public IRI toRDF(final RDFHandler handler, final String namespace,
                @Nullable final Resource ctx) throws RDFHandlerException {

            // TODO: this implementation allows for missing fields: should we allow them?

            // Emit RDF
            final IRI iri = toIRI(namespace);
            IRI unitType = null;
            emit(handler, iri, RDF.TYPE, OWLTIME.DATE_TIME_DESCRIPTION, ctx);
            if (this.year != -1) {
                emit(handler, iri, OWLTIME.YEAR, this.year, ctx);
                unitType = OWLTIME.UNIT_YEAR;
            }
            if (this.month != -1) {
                emit(handler, iri, OWLTIME.MONTH, this.month, ctx);
                unitType = OWLTIME.UNIT_MONTH;
            }
            if (this.week != -1) {
                emit(handler, iri, OWLTIME.WEEK, this.week, ctx);
                unitType = OWLTIME.UNIT_WEEK;
            }
            if (this.day != -1) {
                emit(handler, iri, OWLTIME.DAY, this.day, ctx);
                unitType = OWLTIME.UNIT_DAY;
            }
            if (this.dayOfWeek != -1) {
                IRI dayIRI;
                if (this.dayOfWeek == Calendar.MONDAY) {
                    dayIRI = OWLTIME.MONDAY;
                } else if (this.dayOfWeek == Calendar.TUESDAY) {
                    dayIRI = OWLTIME.TUESDAY;
                } else if (this.dayOfWeek == Calendar.WEDNESDAY) {
                    dayIRI = OWLTIME.WEDNESDAY;
                } else if (this.dayOfWeek == Calendar.THURSDAY) {
                    dayIRI = OWLTIME.THURSDAY;
                } else if (this.dayOfWeek == Calendar.FRIDAY) {
                    dayIRI = OWLTIME.FRIDAY;
                } else if (this.dayOfWeek == Calendar.SATURDAY) {
                    dayIRI = OWLTIME.SATURDAY;
                } else {
                    dayIRI = OWLTIME.SUNDAY;
                }
                emit(handler, iri, OWLTIME.DAY_OF_WEEK, dayIRI, ctx);
            }
            if (this.hour != -1) {
                emit(handler, iri, OWLTIME.HOUR, this.hour, ctx);
                unitType = OWLTIME.UNIT_HOUR;
            }
            if (this.minute != -1) {
                emit(handler, iri, OWLTIME.MINUTE, this.minute, ctx);
                unitType = OWLTIME.UNIT_MINUTE;
            }
            if (this.second != -1) {
                emit(handler, iri, OWLTIME.SECOND, this.second, ctx);
                unitType = OWLTIME.UNIT_SECOND;
            }
            emit(handler, iri, OWLTIME.UNIT_TYPE, unitType, ctx);
            return iri;
        }

        public IRI toIRI(final String namespace) {
            return Statements.VALUE_FACTORY.createIRI(namespace, toString().replace(':', '.')
                    + "_desc");
        }

        @Override
        public String toString() {
            // Build the label
            final StringBuilder builder = new StringBuilder();
            final boolean hasDate = this.year != -1 || this.month != -1 || this.week != -1
                    || this.day != -1;
            final boolean hasTime = this.hour != -1 || this.minute != -1 || this.second != -1;
            if (hasDate) {
                builder.append(formatOptInt(this.year, true));
                if (this.week != -1 && this.day == -1) {
                    builder.append("-W").append(formatOptInt(this.week, false));
                } else if (hasTime || this.month != -1 || this.day != -1) {
                    builder.append('-').append(formatOptInt(this.month, false));
                    if (hasTime || this.day != -1) {
                        builder.append('-').append(formatOptInt(this.day, false));
                    }
                }
            }
            if (hasTime) {
                if (hasDate) {
                    builder.append('T');
                }
                builder.append(formatOptInt(this.hour, false));
                if (this.minute != -1 || this.second != -1) {
                    builder.append(':').append(formatOptInt(this.minute, false));
                    if (this.second != -1) {
                        builder.append(':').append(formatOptInt(this.second, false));
                    }
                }
            }
            return builder.toString();
        }

    }

    public static final class Duration {

        private static final Pattern DURATION_PATTERN = Pattern
                .compile("P(?:[0-9X]+Y)?(?:[0-9X]+M)?(?:[0-9X]+W)?(?:[0-9X]+D)?"
                        + "(?:T(?:[0-9X]+H)?(?:[0-9X]+M)?(?:[0-9X]+S)?)?");

        private final int years;

        private final int months;

        private final int weeks;

        private final int days;

        private final int hours;

        private final int minutes;

        private final int seconds;

        private Duration(final int years, final int months, final int weeks, final int days,
                final int hours, final int minutes, final int seconds) {
            this.years = years;
            this.months = months;
            this.weeks = weeks;
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
        }

        public static Duration create(final int years, final int months, final int weeks,
                final int days, final int hours, final int minutes, final int seconds) {
            Preconditions.checkArgument(years >= 0);
            Preconditions.checkArgument(months >= 0);
            Preconditions.checkArgument(weeks >= 0);
            Preconditions.checkArgument(days >= 0);
            Preconditions.checkArgument(hours >= 0);
            Preconditions.checkArgument(minutes >= 0);
            Preconditions.checkArgument(seconds >= 0);
            return new Duration(years, months, weeks, days, hours, minutes, seconds);
        }

        @Nullable
        public static Duration parseTimex(final String value) {

            final String normalizedValue = value.trim().toUpperCase();
            if (!DURATION_PATTERN.matcher(value).matches()) {
                return null;
            }

            int years = 0;
            int months = 0;
            int weeks = 0;
            int days = 0;
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            boolean unknown = true;

            try {
                Preconditions.checkArgument(normalizedValue.startsWith("P"));
                int start = 1;
                boolean insideTime = false;
                for (int i = 1; i < normalizedValue.length(); ++i) {
                    final char ch = normalizedValue.charAt(i);
                    if (ch == 'T') {
                        insideTime = true;
                        start = i + 1;
                    } else if (!Character.isDigit(ch) && ch != 'X') {
                        final int v = parseOptInt(normalizedValue.substring(start, i));
                        unknown &= v == -1;
                        if (v > 0) {
                            if (ch == 'Y') {
                                years = v;
                            } else if (ch == 'M' && !insideTime) {
                                months = v;
                            } else if (ch == 'W') {
                                weeks = v;
                            } else if (ch == 'D') {
                                days = v;
                            } else if (ch == 'H') {
                                hours = v;
                            } else if (ch == 'M' && insideTime) {
                                minutes = v;
                            } else if (ch == 'S') {
                                seconds = v;
                            } else {
                                throw new IllegalArgumentException("Invalid flag " + ch + " in "
                                        + value);
                            }
                        }
                        start = i + 1;
                    }
                }
            } catch (final Throwable ex) {
                throw new IllegalArgumentException("Invalid duration string: '" + value + "'", ex);
            }

            // TODO: we currently ignore expressions such as PXD (some days) and limit ourselves
            // to precisely quantified expressions. Still, it would be important to represent this
            // kind of information in a fuzzy way (e.g. P1D <= PXD <= P10D)
            if (unknown) {
                return null;
            }

            return create(years, months, weeks, days, hours, minutes, seconds);
        }

        public int getYears() {
            return this.years;
        }

        public int getMinutes() {
            return this.minutes;
        }

        public int getWeeks() {
            return this.weeks;
        }

        public int getDays() {
            return this.days;
        }

        public int getHours() {
            return this.hours;
        }

        public int getMonths() {
            return this.months;
        }

        public int getSeconds() {
            return this.seconds;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof Duration)) {
                return false;
            }
            final Duration other = (Duration) object;
            return this.years == other.years && this.months == other.months
                    && this.weeks == other.weeks && this.days == other.days
                    && this.hours == other.hours && this.minutes == other.minutes
                    && this.seconds == other.seconds;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.years, this.months, this.weeks, this.days, this.hours,
                    this.minutes, this.seconds);
        }

        public IRI toRDF(final RDFHandler handler, final String namespace,
                @Nullable final Resource ctx) throws RDFHandlerException {
            final IRI iri = toIRI(namespace);
            emit(handler, iri, RDF.TYPE, OWLTIME.DURATION_DESCRIPTION, ctx);
            if (this.years > 0) {
                emit(handler, iri, OWLTIME.YEARS, this.years, ctx);
            }
            if (this.months > 0) {
                emit(handler, iri, OWLTIME.MONTHS, this.months, ctx);
            }
            if (this.weeks > 0) {
                emit(handler, iri, OWLTIME.WEEKS, this.weeks, ctx);
            }
            if (this.days > 0) {
                emit(handler, iri, OWLTIME.YEARS, this.days, ctx);
            }
            if (this.hours > 0) {
                emit(handler, iri, OWLTIME.HOURS, this.hours, ctx);
            }
            if (this.minutes > 0) {
                emit(handler, iri, OWLTIME.MINUTES, this.minutes, ctx);
            }
            if (this.seconds > 0) {
                emit(handler, iri, OWLTIME.SECONDS, this.seconds, ctx);
            }
            emit(handler, iri, RDFS.LABEL, toString(), ctx);
            return iri;
        }

        public IRI toIRI(final String namespace) {
            return Statements.VALUE_FACTORY.createIRI(namespace, toString() + "_desc");
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("P");
            if (this.years > 0) {
                builder.append(this.years).append('Y');
            }
            if (this.months > 0) {
                builder.append(this.months).append('M');
            }
            if (this.weeks > 0) {
                builder.append(this.weeks).append('W');
            }
            if (this.days > 0) {
                builder.append(this.days).append('D');
            }
            if (this.hours > 0 || this.minutes > 0 || this.seconds > 0) {
                builder.append('T');
            }
            if (this.hours > 0) {
                builder.append(this.hours).append('H');
            }
            if (this.minutes > 0) {
                builder.append(this.minutes).append('M');
            }
            if (this.seconds > 0) {
                builder.append(this.seconds).append('S');
            }
            return builder.toString();
        }

    }

}
