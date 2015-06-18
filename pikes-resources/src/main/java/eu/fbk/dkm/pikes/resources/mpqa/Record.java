package eu.fbk.dkm.pikes.resources.mpqa;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 24/03/15.
 */
public final class Record {

    private final String line;

    private final Span span;

    private final String name;

    private final String value;

    private final Map<String, String> valueMap;

    private static Pattern attribPatt = Pattern.compile("^\\s*[a-zA-Z0-9_-]+=");
    private static Pattern endPatt = Pattern.compile("^\\s*$");
    private static final Logger LOGGER = LoggerFactory.getLogger(Record.class);

    public Record(final String line) {

        final String[] tokens = line.trim().split("\t");

        this.line = line;
        this.span = new Span(tokens[1]);
        this.name = tokens[3].trim();
        this.value = tokens.length < 5 ? "" : Joiner.on(" ").join(
                Arrays.asList(tokens).subList(4, tokens.length));
        this.valueMap = Maps.newHashMap();

        final List<String> entries = Lists.newArrayList();
        int start = -1;
        boolean insideQuote = false;
        for (int i = 0; i < this.value.length(); ++i) {
            final char c = this.value.charAt(i);
            if (insideQuote) {
                if (c == '\"') {
                    final String fromNow = this.value.substring(i + 1);
                    final Matcher attribMatcher = attribPatt.matcher(fromNow);
                    final Matcher endMatcher = endPatt.matcher(fromNow);
                    if (attribMatcher.find() || endMatcher.find()) {
                        insideQuote = false;
                    }
                }
            } else {
                if (c == '\"') {
                    insideQuote = true;
                } else if (start >= 0 && Character.isWhitespace(c)) {
                    entries.add(this.value.substring(start, i));
                    start = -1;
                } else if (start < 0) {
                    start = i;
                }
            }
        }
        if (start >= 0) {
            entries.add(this.value.substring(start));
        }

        for (final String entry : entries) {
            final int index = entry.indexOf("=");
            if (index >= 0) {
                final String key = entry.substring(0, index).trim();
                String value = entry.substring(index + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                this.valueMap.put(key, value);
            }
        }
    }

    public Span getSpan() {
        return this.span;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public Map<String, String> getValueMap() {
        return this.valueMap;
    }

    @Nullable
    public String getValue(final String key) {
        return this.valueMap.get(key);
    }

    //
    // public Map<String, String> getValueMap() {
    // return this.valueMap;
    // }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Record)) {
            return false;
        }
        final Record other = (Record) object;
        return this.span.equals(other.span) && this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.span, this.name);
    }

    @Override
    public String toString() {
        return this.line;
    }

    public static Ordering<Record> comparator(final String attribute) {
        return new Ordering<Record>() {

            @Override
            public int compare(final Record left, final Record right) {
                final String leftValue = left.getValue(attribute);
                final String rightValue = right.getValue(attribute);
                int result = Ordering.natural().nullsLast().compare(leftValue, rightValue);
                if (result == 0) {
                    result = left.getSpan().compareTo(right.getSpan());
                    if (result == 0) {
                        result = left.getName().compareTo(right.getName());
                    }
                }
                return result;
            }

        };
    }

}
