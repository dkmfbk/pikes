package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.Lexicon;
import eu.fbk.dkm.utils.Stemming;
import eu.fbk.rdfpro.util.Environment;
import eu.fbk.rdfpro.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class SubjectivityLexicon extends Lexicon<SubjectivityLexicon.Lexeme> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectivityLexicon.class);

    private static SubjectivityLexicon instance = null;

    public static synchronized void setInstance(@Nullable final SubjectivityLexicon instance) {
        SubjectivityLexicon.instance = instance;
    }

    public static synchronized SubjectivityLexicon getInstance() {
        if (instance == null) {
            final String location = Objects.firstNonNull(
                    Environment.getProperty("subjectivity.lexicon.home"),
                    "eu.fbk.dkm.pikes.resources.SubjectivityLexicon.tsv");
            try {
                instance = Lexicon.readFrom(SubjectivityLexicon.class, Lexeme.class, location);
            } catch (final Throwable ex) {
                throw new Error("Could not read default subjectivity lexicon at " + location, ex);
            }
        }
        return instance;
    }

    public static SubjectivityLexicon index(final String resourceFile) throws IOException {

        final Map<String, Lexeme> lexemes = Maps.newHashMap();
        try (BufferedReader reader = new BufferedReader(IO.utf8Reader(IO.buffer(IO
                .read(resourceFile))))) {

            String line;
            while ((line = reader.readLine()) != null) {

                String word = null;
                String pos = null;
                Polarity polarity = null;
                boolean stemmed = false;
                boolean strong = false;

                for (final String token : line.split("\\s+")) {
                    final int index = token.indexOf('=');
                    if (index < 0) {
                        LOGGER.warn("Could not parse token '" + token + "'");
                        continue;
                    }
                    final String key = token.substring(0, index).trim();
                    final String value = token.substring(index + 1).trim();
                    if (key.equals("type")) {
                        strong = value.toLowerCase().contains("strong");
                    } else if (key.equals("word1")) {
                        word = value;
                    } else if (key.equals("pos1")) {
                        final String posValue = value.toLowerCase();
                        if (posValue.equals("adj")) {
                            pos = "G";
                        } else if (posValue.equals("adverb")) {
                            pos = "A";
                        } else if (posValue.equals("noun")) {
                            pos = "N";
                        } else if (posValue.equals("verb")) {
                            pos = "V";
                        } else {
                            pos = null;
                        }
                    } else if (key.equals("stemmed1")) {
                        stemmed = value.equalsIgnoreCase("y");
                    } else if (key.equals("priorpolarity")) {
                        // There is a single value 'weakneg' that we normalize to 'negative'
                        polarity = value.equalsIgnoreCase("weakneg") ? Polarity.NEGATIVE
                                : Polarity.valueOf(value.toUpperCase());
                    }
                }

                if (word == null || polarity == null) {
                    LOGGER.warn("Could not parse line (ignoring it):\n" + line);
                } else {
                    final String lemma = stemmed ? null : word;
                    final String stem = stemmed ? Stemming.stem(null, word) : null;
                    final Token token = Token.create(lemma, stem, pos);
                    final String id = word + (stemmed ? "_stemmed" : "")
                            + (pos == null ? "" : "_" + pos.toLowerCase());
                    final Lexeme lexeme = new Lexeme(id, ImmutableList.of(token), polarity, strong);
                    final Lexeme oldLexeme = lexemes.put(id, lexeme);
                    if (oldLexeme != null) {
                        if (lexeme.getTokens().equals(oldLexeme.getTokens())
                                && lexeme.getPolarity().equals(oldLexeme.getPolarity())
                                && lexeme.isStrong() == oldLexeme.isStrong()) {
                            LOGGER.debug("Ignoring duplicate lexeme:\n  " + oldLexeme);
                        } else {
                            LOGGER.warn("Found conflicting lexemes (first one selected):\n  (1) "
                                    + lexeme + "\n  (2) " + oldLexeme);
                        }
                    }
                }
            }
        }

        return new SubjectivityLexicon(lexemes.values());
    }

    public static void main(final String... args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("index-subjectivity-lexicon")
                    .withHeader("Processes the original file of the subjectivity lexicon, " //
                            + "producing a TSV file with an indexed version of it that can " //
                            + "be used with the eu.fbk.dkm.pikes.resources.SubjectivityLexicon Java API class.")
                    .withOption("i", "input", "the input file name", "FILE", CommandLine.Type.FILE_EXISTING,
                            true, false, true)
                    .withOption("o", "output", "the output file name", "FILE", CommandLine.Type.FILE, true,
                            false, true) //
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            final File inputFile = cmd.getOptionValue("i", File.class);
            final File outputFile = cmd.getOptionValue("o", File.class);

            final SubjectivityLexicon lexicon = index(inputFile.getAbsolutePath());
            lexicon.writeTo(outputFile.getAbsolutePath());

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

    public SubjectivityLexicon(final Iterable<Lexeme> lexemes) {
        super(lexemes);
    }

    public static final class Lexeme extends Lexicon.Lexeme {

        private final Polarity polarity;

        private final boolean strong;

        public Lexeme(final String id, final Iterable<Token> tokens, final Polarity polarity,
                final boolean strong) {
            super(id, tokens);
            this.polarity = Preconditions.checkNotNull(polarity);
            this.strong = strong;
        }

        protected Lexeme(final String id, final Iterable<Token> tokens,
                final Map<String, String> properties) {
            // for use with reflection
            this(id, tokens, Polarity.valueOf(properties.get("polarity").toUpperCase()), Boolean
                    .valueOf(properties.get("strong").toLowerCase()));
        }

        @Override
        protected Map<String, String> getProperties() {
            return ImmutableMap.of("polarity", this.polarity.toString(), "strong",
                    Boolean.toString(this.strong));
        }

        public Polarity getPolarity() {
            return this.polarity;
        }

        public boolean isStrong() {
            return this.strong;
        }

    }

    public enum Polarity {

        NEUTRAL,

        POSITIVE,

        NEGATIVE,

        BOTH

    }

}
