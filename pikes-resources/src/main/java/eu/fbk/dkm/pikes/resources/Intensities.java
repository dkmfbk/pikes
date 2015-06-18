package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import eu.fbk.dkm.utils.Lexicon;
import eu.fbk.rdfpro.util.Environment;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.Map;

public final class Intensities extends Lexicon<Intensities.Lexeme> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Intensities.class);

	private static Intensities instance = null;

	public static synchronized void setInstance(@Nullable final Intensities instance) {
		Intensities.instance = instance;
	}

	public static synchronized Intensities getInstance() {
		if (instance == null) {
			final String location = Objects.firstNonNull(
					Environment.getProperty("intensities.home"),
					"intensities.tsv");
			try {
				instance = Lexicon.readFrom(Intensities.class, Lexeme.class, location);
			} catch (final Throwable ex) {
				throw new Error("Could not read default intensity lexicon at " + location, ex);
			}
		}
		return instance;
	}

	public static void main(final String... args) throws Exception {

		Intensities lexicon = getInstance();

		String fileName = "/Users/alessio/Documents/Resources/database.eu.fbk.dkm.pikes.resources.mpqa.2.0/NAF-parsed/20010907_00.16.28-8800.naf";
		KAFDocument document = KAFDocument.createFromFile(new File(fileName));
		Multimap<Term, Lexeme> lexemeMultimap = lexicon.match(document, document.getTerms());

		for (Term term : lexemeMultimap.keys()) {
			Collection<Lexeme> lexemes = lexemeMultimap.get(term);
			for (Lexeme lexeme : lexemes) {
				System.out.println(lexeme);
			}
		}

//        try {
//            final CommandLine cmd = CommandLine
//                    .parser()
//                    .withName("index-subjectivity-lexicon")
//                    .withHeader("Processes the original file of the subjectivity lexicon, " //
//                            + "producing a TSV file with an indexed version of it that can " //
//                            + "be used with the eu.fbk.dkm.pikes.resources.SubjectivityLexicon Java API class.")
//                    .withOption("i", "input", "the input file name", "FILE", Type.FILE_EXISTING,
//                            true, false, true)
//                    .withOption("o", "output", "the output file name", "FILE", Type.FILE, true,
//                            false, true) //
//                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
//                    .parse(args);
//
//            final File inputFile = cmd.getOptionValue("i", File.class);
//            final File outputFile = cmd.getOptionValue("o", File.class);
//
//            final eu.fbk.dkm.pikes.resources.Intensities lexicon = index(inputFile.getAbsolutePath());
//            lexicon.writeTo(outputFile.getAbsolutePath());
//
//        } catch (final Throwable ex) {
//            CommandLine.fail(ex);
//        }
	}

	public Intensities(final Iterable<Lexeme> lexemes) {
		super(lexemes);
	}

	public static final class Lexeme extends Lexicon.Lexeme {

		private final Type type;

		public Lexeme(final String id, final Iterable<Token> tokens, final Type type) {
			super(id, tokens);
			this.type = Preconditions.checkNotNull(type);
		}

		protected Lexeme(final String id, final Iterable<Token> tokens,
						 final Map<String, String> properties) {
			// for use with reflection
			this(id, tokens, Type.valueOf(properties.get("type").toUpperCase()));
		}

		@Override
		protected Map<String, String> getProperties() {
			return ImmutableMap.of("type", this.type.toString());
		}

		public Type getType() {
			return this.type;
		}

	}

	public enum Type {

		INTENSIFIER,
		DIMINISHER,
		MODAL

	}

}
