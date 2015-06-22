package eu.fbk.dkm.pikes.resources;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

/**
 * English number parsing and spelling methosds.
 * <p>
 * This code is based on 'numword' by Dr. Georg Fischer, https://github.com/gfis/numword.
 * </p>
 */
public final class NumberSpeller {

    private static final String[] ORDINALS = new String[] { "zeroth", "first", "second", "third",
            "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", "eleventh",
            "twelfth" };

    private static final String[] WORD_N = new String[] { "zero", "one", "two", "three", "four",
            "five", "six", "seven", "eight", "nine" };

    private static final String[] WORD_N0 = new String[] { "", "ten", "twenty", "thirty", "forty",
            "fifty", "sixty", "seventy", "eighty", "ninety" };

    private static final String[] WORD_1N = new String[] { "ten", "eleven", "twelve", "thirteen",
            "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen" };

    private static final String[] WORD_N000 = new String[] { "", "mil", "bil", "tril", "quadril",
            "quintil", "sextil", "septil", "octil", "nonil", "decil", "undecil", "duodecil",
            "tredecil", "quattuordecil", "quindecil", "sexdecil", "septendecil", "octodecil",
            "novemdecil", "vigintil" };

    private static final Map<String, String> MORPH_MAP;

    static {
        final Map<String, String> map = Maps.newHashMap();

        map.put("h1", "hundred");
        map.put("h2", "hundred");
        map.put("h3", "hundred");
        map.put("h4", "hundred");

        map.put("t1", "thousand");
        map.put("t2", "thousand");
        map.put("t3", "thousand");
        map.put("t4", "thousand");

        map.put("m1", "lion");
        map.put("m2", "lions");
        map.put("m3", "lions");
        map.put("m4", "lions");

        map.put("p0", " ");
        map.put("p1", "-");
        map.put("p2", "s");
        map.put("p3", "and");

        for (int i = 0; i < WORD_N.length; i++) {
            map.put(String.valueOf(i), WORD_N[i]);
        }
        for (int i = 1; i < WORD_N0.length; i++) {
            map.put(String.valueOf(i * 10), WORD_N0[i]);
        }
        for (int i = 1; i < WORD_1N.length; i++) {
            map.put(String.valueOf(i + 10), WORD_1N[i]);
        }
        for (int i = 2; i < WORD_N000.length; i++) {
            map.put("e" + String.valueOf(i + 100).substring(1, 3), WORD_N000[i]);
        }

        MORPH_MAP = ImmutableMap.copyOf(map);
    }

    /**
     * Parses a cardinal number from the string supplied.
     *
     * @param text
     *            string to be parsed
     * @return parsed cardinal number; null on failure
     */
    @Nullable
    public static Long parseCardinal(final String text) {
        int offset = 0;
        final StringBuilder result = new StringBuilder();
        int triple = 0; // current value of triple
        result.delete(0, result.length()); // clear buffer
        final StringBuffer particle = new StringBuffer(32);
        boolean found = false;
        final boolean liard = MORPH_MAP.get("m3") != null; // whether there is a special
        // postfix "liard"yy
        int prefixLen = 1;
        while (prefixLen > 0) { // any number morphem found
            prefixLen = 0;
            String prefixKey = "";
            final Iterator/* <1.5 */<String>/* 1.5> */iter = MORPH_MAP.keySet().iterator();
            while (iter.hasNext()) { // search over all defined morphems
                final String key = iter.next();
                final String value = MORPH_MAP.get(key);
                if (value.length() > prefixLen && text.startsWith(value, offset)) { // remember
                    // this
                    prefixKey = key;
                    prefixLen = value.length();
                }
            } // while
            if (prefixLen > 0) { // any number morphem found
                found = true;
                final char ch0 = prefixKey.charAt(0);
                if (ch0 == 'p') { // meaningless particle - but only if behind other morphem
                    if (result.length() > 0 || triple != 0) {
                        particle.append(text.substring(offset, offset + prefixLen));
                    } else { // particle at start - no number word found
                        found = false;
                        prefixLen = 0;
                    }
                } else if (ch0 != '-' && !Character.isDigit(ch0)) { // key with encoded meaning
                    particle.delete(0, particle.length()); // number follows - forget the
                    // particles
                    switch (ch0) {
                    case 'e': // million, (milliard), billion, ...
                        // look whether "lion"xx or "liard"yy follows
                        if (text.substring(offset + prefixLen).startsWith(MORPH_MAP.get("m1"))
                                || text.substring(offset + prefixLen).startsWith(
                                        MORPH_MAP.get("m2"))
                                || liard
                                && (text.substring(offset + prefixLen).startsWith(
                                        MORPH_MAP.get("m3")) || text.substring(offset + prefixLen)
                                        .startsWith(MORPH_MAP.get("m4")))) { // "mil"
                            // +
                            // "lion"xx
                            if (triple == 0) {
                                // "million" instead of "one million" - should not occur
                                triple = 1;
                            }
                            int exponent = Integer.parseInt(prefixKey.substring(1)) - 1;
                            // exactly 2 digits behind 'e'
                            final StringBuffer part = new StringBuffer(32);
                            part.append(String.valueOf(triple));
                            while (exponent > 0) {
                                if (liard) { // German billion
                                    part.append("000000");
                                } else { // US billion
                                    part.append("000");
                                }
                                exponent--;
                            } // while exponent
                            if (!liard
                                    || liard
                                    && (text.substring(offset + prefixLen).startsWith(
                                            MORPH_MAP.get("m3")) || text.substring(
                                            offset + prefixLen).startsWith(MORPH_MAP.get("m4")))) { // liard
                                // postfix
                                part.append("000"); // same as "lion" * 1000
                            }
                            if (result.length() > part.length()) {
                                // replace trailing zeroes
                                result.replace(result.length() - part.length(), result.length(),
                                        part.toString());
                            } else {
                                result.append(part);
                            }
                            triple = 0;

                        } else { // prefix found, but not "lion/liard"
                            prefixLen = 0;
                        }
                        break;
                    case 'h': // hundred
                        if (triple > 0) {
                            triple *= 100;
                        } else { // missing "one" hundred
                            triple = 100;
                        }
                        break;
                    case 'k': // special Klingon exponents - not yet implemented ???
                        break;
                    case 'l': // million(s) (only in case 1000 same as prefix of 10**6: sp, pt,
                        // eo)
                    {
                        if (triple == 0) {
                            // "million" instead of "one million" - should not occur
                            triple = 1;
                        }
                        final StringBuffer part = new StringBuffer(32);
                        part.append(String.valueOf(triple));
                        part.append("000000");
                        if (prefixKey.compareTo("l3") >= 0) { // milliard(s)
                            part.append("000");
                        }
                        if (result.length() > part.length()) {
                            // replace trailing zeroes
                            result.replace(result.length() - part.length(), result.length(),
                                    part.toString());
                        } else {
                            result.append(part);
                        }
                        triple = 0;
                    }
                        break;
                    // case 'p': handled separately above
                    // break;
                    case 't': // thousand
                        if (triple == 0) {
                            triple = 1;
                        }
                        final String part = String.valueOf(triple) + "000";
                        if (result.length() > part.length()) {
                            // replace trailing zeroes
                            result.replace(result.length() - part.length(), result.length(), part);
                        } else {
                            result.append(part);
                        }
                        triple = 0;
                        break;
                    default: // unknown key
                        break;
                    } // switch ch0
                } else { // key with direct numeric meaning:
                    particle.delete(0, particle.length()); // number follows - forget the
                    // particles
                    // units, *10, +10, *100
                    triple += Integer.parseInt(prefixKey); // exceptions should not occur
                }
                offset += prefixLen;
            } // number morphem found
        } // while match
        if (found) {
            if (triple == 0) {
                if (result.length() == 0) { // a single zero
                    result.append("0");
                }
            } else {
                if (result.length() == 0) { // < 1000
                    result.append(String.valueOf(triple));
                } else {
                    final String part = String.valueOf(triple);
                    result.replace(result.length() - part.length(), result.length(), part);
                }
            }
        }
        // return offset;
        return result.length() == 0 ? null : Long.parseLong(result.toString());
    }

    /**
     * Returns the word for a number in some language. This method is the heart of the package. It
     * assumes the "normal" european numbering system derived from latin. The entire number is
     * splitted into triples of digits: hundreds, tens, and ones. These are spelled in order,
     * joined by some morphemes like "and", and "s" for plural. The words for ones, tens, for
     * 10..19 and sometimes for the hundreds are stored in language specific arrays.
     *
     * @param number
     *            a sequence of digit characters, maybe interspersed with non-digits (spaces,
     *            punctuation).
     * @return number word
     */
    public static String spellCardinal(final int num) {

        String number = Integer.toString(num);

        final int maxLog = (WORD_N000.length - 1) * 3;

        final StringBuilder result = new StringBuilder();
        final StringBuffer buffer = new StringBuffer(1024);
        // ensure length is a multiple of 'lenTuple'
        final String nullTuple = "000";
        buffer.append(nullTuple);
        int position = 0;
        while (position < number.length()) { // remove non-digits
            final char ch = number.charAt(position);
            if (Character.isDigit(ch)) {
                buffer.append(ch);
            }
            position++;
        }
        final int realLog = buffer.length() - 3; // -3 because of "000" above
        // trim size to multiples of 'lenTuple'
        number = buffer.toString().substring(buffer.length() % 3);

        if (realLog <= maxLog) { // number can be spelled in this language
            position = 0;
            final boolean nullOnly = number.equals(nullTuple);

            while (position < number.length()) { // process all triples

                final int digitN00 = number.charAt(position++) - '0';
                final int digitN0 = number.charAt(position++) - '0';
                final int digitN = number.charAt(position++) - '0';
                final boolean singleTuple = digitN00 + digitN0 == 0 && digitN == 1;
                final boolean zeroTuple = digitN00 + digitN0 == 0 && digitN == 0;
                final int logTuple = (number.length() - position) / 3; // 1 for 10**3, 2 for

                // hundreds
                switch (digitN00) {
                case 0:
                    break;
                default:
                    result.append(" ").append(WORD_N[digitN00]);
                    result.append(" ").append(MORPH_MAP.get("h1"));
                    if (digitN0 != 0 || digitN != 0) {
                        result.append(" ").append(MORPH_MAP.get("p3"));
                    }
                    break;
                } // switch 100

                // tens and ones
                switch (digitN0) {
                case 0:
                    if (nullOnly) {
                        result.append(" ").append(WORD_N[0]);
                    } else if (digitN > 0) {
                        result.append(" ").append(WORD_N[digitN]);
                    }
                    break;
                case 1:
                    result.append(" ").append(WORD_1N[digitN]);
                    break;
                default:
                    result.append(" ").append(WORD_N0[digitN0]);
                    if (digitN >= 1) {
                        result.append(MORPH_MAP.get("p1")); // "-"
                        result.append(WORD_N[digitN]);
                    }
                    break;
                }

                // append thousand, million ... */
                if (!zeroTuple) {
                    switch (logTuple) {
                    case 0: // no thousands
                        break;
                    case 1:
                        result.append(" ").append(MORPH_MAP.get("t1"));
                        break;
                    default:
                        result.append(" ").append(WORD_N000[logTuple]);
                        result.append(MORPH_MAP.get("m1")); // lion
                        if (!singleTuple) {
                            result.append(MORPH_MAP.get("p2")); // two million"s"
                        }
                        break;
                    }
                }
            }

            result.delete(0, 1); // remove any initial separator

        } else {
            result.append(number + " >= 1");
            for (int pos = 0; pos < maxLog; pos++) {
                result.append('0');
            }
        }

        return result.substring(0, 1).equals(" ") ? result.substring(1).toString() : result
                .toString();
    }

    @Nullable
    public static Long parseOrdinal(final String string) {
        final String s = string.trim();
        final int l = s.length();
        for (int i = 0; i < ORDINALS.length; ++i) {
            if (s.endsWith(ORDINALS[i])) {
                return parseCardinal(s.substring(0, l - ORDINALS[i].length()) + spellCardinal(i));
            }
        }
        if (s.endsWith("ieth")) {
            return parseCardinal(s.substring(0, l - 4) + "y");
        }
        return parseCardinal(s.substring(0, l - 2));
    }

    public static String spellOrdinal(final int ordinal) {
        if (ordinal <= 12) {
            return ORDINALS[ordinal];
        }
        if (ordinal % 100 >= 20 && ordinal % 10 == 0) {
            final String string = spellCardinal(ordinal);
            return string.substring(0, string.length() - 1) + "ieth";
        }
        if (ordinal > 20 && ordinal % 10 != 0) {
            final String string = spellCardinal(ordinal / 10 * 10);
            return string + "-" + ORDINALS[ordinal % 10];
        }
        return spellCardinal(ordinal) + "th";
    }

    public static boolean isOrdinal(final String string) {
        return string.endsWith("th") || string.endsWith("first") || string.endsWith("second")
                || string.endsWith("third");
    }

    @Nullable
    public static Double parse(final String string) {

        final String s = string.trim();
        final StringBuilder n = new StringBuilder();
        int i = 0;
        for (; i < s.length(); ++i) {
            final char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == 'e' || c == 'E') {
                n.append(c);
            } else if (c == ',') {
                n.append('.');
            } else if (c != ' ' && c != '+' && c != '\'') {
                break;
            }
        }

        Double multiplier = null;
        if (n.length() > 0) {
            try {
                multiplier = Double.valueOf(n.toString());
            } catch (final NumberFormatException ex) {
                // ignore
            }
        }

        final String str = s.substring(i);
        final Long num = isOrdinal(str) ? parseOrdinal(str) : parseCardinal(str);
        if (num == null) {
            return multiplier;
        } else if (multiplier == null) {
            return num.doubleValue();
        } else {
            return num * multiplier;
        }
    }

}
