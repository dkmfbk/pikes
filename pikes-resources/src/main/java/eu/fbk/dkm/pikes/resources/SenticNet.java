package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.CommandLine.Type;
import eu.fbk.dkm.utils.Lexicon;
import eu.fbk.rdfpro.AbstractRDFHandler;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.tql.TQL;
import eu.fbk.rdfpro.util.Environment;
import eu.fbk.rdfpro.util.Statements;
import org.openrdf.model.*;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SenticNet extends Lexicon<SenticNet.Lexeme> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenticNet.class);

    private static final String NS_CONCEPT = "http://sentic.net/api/en/concept/";

    private static final URI PROP_APTITUDE = Statements.VALUE_FACTORY
            .createURI("http://sentic.net/apiaptitude");

    private static final URI PROP_ATTENTION = Statements.VALUE_FACTORY
            .createURI("http://sentic.net/apiattention");

    private static final URI PROP_PLEASENTNESS = Statements.VALUE_FACTORY
            .createURI("http://sentic.net/apipleasantness");

    private static final URI PROP_POLARITY = Statements.VALUE_FACTORY
            .createURI("http://sentic.net/apipolarity");

    private static final URI PROP_SENSITIVITY = Statements.VALUE_FACTORY
            .createURI("http://sentic.net/apisensitivity");

    private static final URI PROP_SEMANTICS = Statements.VALUE_FACTORY
            .createURI("http://sentic.net/apisemantics");

    private static final URI PROP_TEXT = Statements.VALUE_FACTORY
            .createURI("http://sentic.net/apitext");

    private static SenticNet instance = null;

    public static synchronized void setInstance(@Nullable final SenticNet instance) {
        SenticNet.instance = instance;
    }

    public static synchronized SenticNet getInstance() {
        if (instance == null) {
            final String location = Objects.firstNonNull(
                    Environment.getProperty("senticnet.home"), "eu.fbk.dkm.pikes.resources.SenticNet.tsv");
            try {
                instance = Lexicon.readFrom(SenticNet.class, Lexeme.class, location);
            } catch (final Throwable ex) {
                throw new Error("Could not read default subjectivity lexicon at " + location, ex);
            }
        }
        return instance;
    }

    @Nullable
    public static String idFor(@Nullable final Value value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof URI)) {
            throw new IllegalArgumentException("Not a concept URI: " + value);
        }
        final URI uri = (URI) value;
        if (!uri.getNamespace().equals(NS_CONCEPT)) {
            throw new IllegalArgumentException("Unexpected namespace for concept URI: " + value);
        }
        return uri.getLocalName();
    }

    @Nullable
    public static URI uriFor(@Nullable final String id) {
        return id == null ? null : Statements.VALUE_FACTORY.createURI(NS_CONCEPT, id);
    }

    public static SenticNet index(final String resourceFile) throws IOException {

        TQL.register();

        final Map<String, LexemeData> data = Maps.newHashMap();
        try {
            RDFSources.read(false, true, null, null, resourceFile).emit(new AbstractRDFHandler() {

                @Override
                public void handleStatement(final Statement statement) throws RDFHandlerException {

                    final Resource subj = statement.getSubject();
                    final URI pred = statement.getPredicate();
                    final Value obj = statement.getObject();

                    try {
                        if (pred.equals(PROP_APTITUDE)) {
                            getLexemeData(subj).aptitude = ((Literal) obj).floatValue();
                        } else if (pred.equals(PROP_ATTENTION)) {
                            getLexemeData(subj).attention = ((Literal) obj).floatValue();
                        } else if (pred.equals(PROP_PLEASENTNESS)) {
                            getLexemeData(subj).pleasentness = ((Literal) obj).floatValue();
                        } else if (pred.equals(PROP_POLARITY)) {
                            getLexemeData(subj).polarity = ((Literal) obj).floatValue();
                        } else if (pred.equals(PROP_SENSITIVITY)) {
                            getLexemeData(subj).sensitivity = ((Literal) obj).floatValue();
                        } else if (pred.equals(PROP_SEMANTICS)) {
                            getLexemeData(subj).semantics.add(idFor(obj));
                        } else if (pred.equals(PROP_TEXT)) {
                            getLexemeData(subj).text = obj.stringValue();
                        }
                    } catch (final Throwable ex) {
                        LOGGER.warn("Could not process statement: " + statement, ex);
                    }
                }

                private LexemeData getLexemeData(final Resource subject) {
                    final String id = idFor(subject);
                    LexemeData lexemeData = data.get(id);
                    if (lexemeData == null) {
                        lexemeData = new LexemeData(id);
                        data.put(id, lexemeData);
                    }
                    return lexemeData;
                }

            }, 1);

        } catch (final RDFHandlerException ex) {
            Throwables.propagateIfPossible(ex.getCause() == null ? ex : ex.getCause(),
                    IOException.class);
            Throwables.propagate(ex);
        }

        final List<Lexeme> lexemes = Lists.newArrayList();
        for (final LexemeData lexemeData : data.values()) {
            final Lexeme lexeme = lexemeData.toLexeme();
            if (lexeme == null) {
                LOGGER.warn("Could not create lexeme for ID " + lexemeData.id);
            } else {
                lexemes.add(lexeme);
            }
        }

        return new SenticNet(lexemes);
    }

    public static void main(final String... args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("index-senticnet-lexicon")
                    .withHeader("Processes the RDF data of eu.fbk.dkm.pikes.resources.SenticNet, " //
                            + "producing a TSV file with an indexed version of it that can " //
                            + "be used with the eu.fbk.dkm.pikes.resources.SenticNet Java API class.")
                    .withOption("i", "input", "the input file name", "FILE", Type.FILE_EXISTING,
                            true, false, true)
                    .withOption("o", "output", "the output file name", "FILE", Type.FILE, true,
                            false, true) //
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            final File inputFile = cmd.getOptionValue("i", File.class);
            final File outputFile = cmd.getOptionValue("o", File.class);

            final SenticNet lexicon = index(inputFile.getAbsolutePath());
            lexicon.writeTo(outputFile.getAbsolutePath());

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

    public SenticNet(final Iterable<Lexeme> lexemes) {
        super(lexemes);
    }

    public Lexeme get(final Value id) {
        return get(idFor(id));
    }

    public static final class Lexeme extends Lexicon.Lexeme {

        private final float aptitude;

        private final float attention;

        private final float pleasentness;

        private final float polarity;

        private final float sensitivity;

        private final String[] semantics;

        public Lexeme(final String id, final Iterable<Token> tokens, final float aptitude,
                final float attention, final float pleasentness, final float polarity,
                final float sensitivity, final String... semantics) {

            super(id, tokens);

            this.aptitude = aptitude;
            this.attention = attention;
            this.pleasentness = pleasentness;
            this.polarity = polarity;
            this.sensitivity = sensitivity;
            this.semantics = semantics.clone();

            for (int i = 0; i < semantics.length; ++i) {
                semantics[i] = semantics[i].intern();
            }
        }

        protected Lexeme(final String id, final Iterable<Token> tokens,
                final Map<String, String> properties) {
            // for use with reflection
            this(id, tokens, Float.parseFloat(properties.getOrDefault("aptitude", "0")), //
                    Float.parseFloat(properties.getOrDefault("attention", "0")), //
                    Float.parseFloat(properties.getOrDefault("pleasentness", "0")), //
                    Float.parseFloat(properties.getOrDefault("polarity", "0")), //
                    Float.parseFloat(properties.getOrDefault("sensitivity", "0")), //
                    properties.getOrDefault("semantics", "").split("\\|"));
        }

        @Override
        protected Map<String, String> getProperties() {
            return ImmutableMap.<String, String>builder()
                    .put("aptitude", Float.toString(this.aptitude))
                    .put("attention", Float.toString(this.attention))
                    .put("pleasentness", Float.toString(this.pleasentness))
                    .put("polarity", Float.toString(this.polarity))
                    .put("sensitivity", Float.toString(this.sensitivity))
                    .put("semantics", Joiner.on('|').join(this.semantics)).build();
        }

        public float getAptitude() {
            return this.aptitude;
        }

        public float getAttention() {
            return this.attention;
        }

        public float getPleasentness() {
            return this.pleasentness;
        }

        public float getPolarity() {
            return this.polarity;
        }

        public float getSensitivity() {
            return this.sensitivity;
        }

        public List<String> getSemantics() {
            return ImmutableList.copyOf(this.semantics);
        }

    }

    private static final class LexemeData {

        String id;

        @Nullable
        String text;

        float aptitude;

        float attention;

        float pleasentness;

        float polarity;

        float sensitivity;

        List<String> semantics;

        LexemeData(final String id) {
            this.id = id;
            this.semantics = Lists.newArrayList();
        }

        @Nullable
        Lexeme toLexeme() {
            if (this.text == null) {
                return null;
            }
            final List<Token> tokens = Lists.newArrayList();
            for (final String word : this.text.split("\\s+")) {
                tokens.add(Token.create(word.toLowerCase(), null, null));
            }
            final String[] semantics = this.semantics.toArray(new String[this.semantics.size()]);
            return new Lexeme(this.id, tokens, this.aptitude, this.attention, this.pleasentness,
                    this.polarity, this.sensitivity, semantics);
        }

    }

}
