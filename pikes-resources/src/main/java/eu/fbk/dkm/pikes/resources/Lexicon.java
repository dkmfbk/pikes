package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import eu.fbk.rdfpro.util.IO;
import ixa.kaflib.Dep;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class Lexicon<T extends Lexicon.Lexeme> extends AbstractSet<T> {

	private final Map<String, T> idIndex;

	private final Multimap<String, T> lemmaIndex;

	private final Multimap<String, T> stemIndex;

	public Lexicon(final Iterable<? extends T> lexemes) {

		final ImmutableMap.Builder<String, T> idBuilder = ImmutableMap.builder();
		final ImmutableMultimap.Builder<String, T> lemmaBuilder = ImmutableMultimap.builder();
		final ImmutableMultimap.Builder<String, T> stemBuilder = ImmutableMultimap.builder();

		for (final T lexeme : Ordering.natural().immutableSortedCopy(lexemes)) {
			idBuilder.put(lexeme.getId(), lexeme);
			for (final Token token : lexeme.getTokens()) {
				if (token.getLemma() != null) {
					lemmaBuilder.put(token.getLemma(), lexeme);
				}
				if (token.getStem() != null) {
					stemBuilder.put(token.getStem(), lexeme);
				}
			}
		}

		this.idIndex = idBuilder.build();
		this.lemmaIndex = lemmaBuilder.build();
		this.stemIndex = stemBuilder.build();
	}

	public static <T extends Lexeme, L extends Lexicon<T>> L create(final Class<L> lexiconClass,
																	final Iterable<T> lexemes) {

		for (final Constructor<?> constructor : lexiconClass.getDeclaredConstructors()) {
			final Class<?>[] argTypes = constructor.getParameterTypes();
			if (argTypes.length == 1 && argTypes[0].isAssignableFrom(List.class)) {
				try {
					constructor.setAccessible(true);
					return lexiconClass
							.cast(constructor.newInstance(ImmutableList.copyOf(lexemes)));
				} catch (final InvocationTargetException ex) {
					throw Throwables.propagate(ex.getCause());
				} catch (final IllegalAccessException | InstantiationException ex) {
					throw new IllegalArgumentException("Class " + lexiconClass.getName()
							+ " could not be instantiated", ex);
				}
			}
		}
		throw new IllegalArgumentException("No suitable constructor for class "
				+ lexiconClass.getName());
	}

	public static <T extends Lexeme, L extends Lexicon<T>> L readFrom(final Class<L> lexiconClass,
																	  final Class<T> lexemeClass, final String location) throws IOException {

		// Try to open a file at the location specified, falling back to search on the classpath
		Reader reader = null;
		try {
			reader = IO.utf8Reader(IO.buffer(IO.read(location)));
		} catch (final Throwable ex) {
			final URL url = lexiconClass.getResource(location);
			if (url == null) {
				Throwables.propagateIfPossible(ex, IOException.class);
				Throwables.propagate(ex);
			}
			reader = IO.utf8Reader(url.openStream());
		}

		// Read the lexicon from the opened reader
		try {
			return readFrom(lexiconClass, lexemeClass, reader);
		} finally {
			reader.close();
		}
	}

	public static <T extends Lexeme, L extends Lexicon<T>> L readFrom(final Class<L> lexiconClass,
																	  final Class<T> lexemeClass, final Reader reader) throws IOException {
		final List<T> lexemes = Lists.newArrayList();
		final BufferedReader in = reader instanceof BufferedReader ? (BufferedReader) reader
				: new BufferedReader(reader);
		String line;
		while ((line = in.readLine()) != null) {
			T token = Lexeme.parse(lexemeClass, line);
			if (token == null) {
				continue;
			}
			lexemes.add(token);
		}
		return create(lexiconClass, lexemes);
	}

	@Override
	public int size() {
		return this.idIndex.size();
	}

	@Override
	public boolean contains(final Object object) {
		if (object instanceof Lexeme) {
			final Lexeme lexeme = (Lexeme) object;
			return this.idIndex.get(lexeme.getId()) == lexeme;
		}
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return this.idIndex.values().iterator();
	}

	public T get(final String id) {
		return this.idIndex.get(id);
	}

	public Set<T> match(final KAFDocument document, final Term term) {
		return ImmutableSet.copyOf(match(document, ImmutableSet.of(term)).get(term));
	}

	public Multimap<Term, T> match(final KAFDocument document, final Iterable<Term> terms) {
		Preconditions.checkNotNull(document);
		final Set<Term> termSet = ImmutableSet.copyOf(terms);
		final Multimap<Term, T> result = HashMultimap.create();
		for (final Term term : termSet) {
			final String lemma = term.getLemma();
			final String stem = Stemming.stem(null, lemma);
			for (final T lexeme : ImmutableSet.copyOf(Iterables.concat(
					this.lemmaIndex.get(term.getLemma()), this.stemIndex.get(stem)))) {
				if (lexeme.match(document, termSet, term)) {
					result.put(term, lexeme);
				}
			}
		}
		return result;
	}

	public void writeTo(final String location) throws IOException {
		try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(location)))) {
			writeTo(writer);
		}
	}

	public <A extends Appendable> A writeTo(final A out) throws IOException {
		for (final Lexeme lexeme : this.idIndex.values()) {
			lexeme.toString(out);
			out.append('\n');
		}
		return out;
	}

	public static class Lexeme implements Comparable<Lexeme> {

		private static final Map<Class<?>, Constructor<?>> CACHED_CONSTRUCTORS = Maps
				.newConcurrentMap();

		private final String id;

		private final List<Token> tokens;

		public Lexeme(final String id, final Iterable<Token> tokens) {
			this.id = Preconditions.checkNotNull(id);
			this.tokens = ImmutableList.copyOf(tokens);
		}

		public static <T extends Lexeme> T create(final Class<T> lexemeClass, final String id,
												  final Iterable<Token> tokens, @Nullable final Map<String, String> properties) {

			// Normalize tokens and properties
			final List<Token> actualTokens = ImmutableList.copyOf(tokens);
			final Map<String, String> actualProperties = properties == null ? ImmutableMap.of()
					: ImmutableMap.copyOf(properties);

			// Locate a suitable constructor, using a cache to speed up the process
			Constructor<?> constructor = CACHED_CONSTRUCTORS.get(lexemeClass);
			if (constructor == null) {
				for (final Constructor<?> candidate : lexemeClass.getDeclaredConstructors()) {
					final Class<?>[] argTypes = candidate.getParameterTypes();
					if (argTypes.length >= 2 && argTypes[0].isAssignableFrom(String.class)
							&& argTypes[1].isAssignableFrom(List.class)) {
						if (argTypes.length == 3 && argTypes[2].isAssignableFrom(Map.class)) {
							constructor = candidate;
							break;
						}
						else if (argTypes.length == 2 && constructor == null) {
							constructor = candidate;
						}
					}
				}
				if (constructor == null) {
					throw new IllegalArgumentException("No suitable constructor for class "
							+ lexemeClass.getName());
				}
				constructor.setAccessible(true);
				CACHED_CONSTRUCTORS.put(lexemeClass, constructor);
			}

			try {
				// Invoke the constructor and return the created object
				if (constructor.getParameterCount() == 2) {
					return lexemeClass.cast(constructor.newInstance(id, actualTokens));
				}
				else {
					return lexemeClass.cast(constructor.newInstance(id, actualTokens,
							actualProperties));
				}
			} catch (final InvocationTargetException ex) {
				throw Throwables.propagate(ex.getCause());
			} catch (final IllegalAccessException | InstantiationException ex) {
				throw new IllegalArgumentException("Class " + lexemeClass.getName()
						+ " could not be instantiated", ex);
			}
		}

		@Nullable
		public static <T extends Lexeme> T parse(final Class<T> lexemeClass, final String string) {
			final String[] fields = string.split("\t");
			if (fields.length < 2) {
				return null;
			}
			final String id = fields[0].trim();
			final List<Token> tokens = Lists.newArrayList();
			for (final String tokenStr : fields[1].substring("tokens=".length()).split(",")) {
				tokens.add(Token.parse(tokenStr.trim(), id));
			}
			final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
			for (int i = 2; i < fields.length; ++i) {
				final String fieldStr = fields[i];
				final int index = fieldStr.indexOf('=');
				if (index < 0) {
					final String key = fieldStr.trim().intern();
					final String value = "true";
					builder.put(key, value);
				}
				else {
					final String key = fieldStr.substring(0, index).trim().intern();
					final String value = fieldStr.substring(index + 1).trim().intern();
					builder.put(key, value);
				}
			}
			return create(lexemeClass, id, tokens, builder.build());
		}

		protected Map<String, String> getProperties() {
			return ImmutableMap.of(); // could be overridden by subclasses to add other properties
		}

		public final String getId() {
			return this.id;
		}

		public final List<Token> getTokens() {
			return this.tokens;
		}

		public final boolean match(final KAFDocument document, final Iterable<Term> terms,
								   final Term head) {
			final Term[][] solutions = matchRecursive(document, terms, head);
			if (solutions != null) {
				outer:
				for (final Term[] solution : solutions) {
					for (int i = 0; i < this.tokens.size(); ++i) {
						if (solution[i] == null) {
							continue outer;
						}
					}
					return true;
				}
			}
			return false;
		}

		@Nullable
		private Term[][] matchRecursive(final KAFDocument document, final Iterable<Term> terms,
										final Term head) {

			// Use a counter to keep track of possible combinations
			int combinations = 0;

			// Try to match the head against the different tokens. Abort if not possible
			final List<Integer> indexes = Lists.newArrayList();
			for (int i = 0; i < this.tokens.size(); ++i) {
				if (this.tokens.get(i).match(head)) {
					indexes.add(i);
					++combinations;
				}
			}
			if (combinations == 0) {
				return null;
			}

			// Match children
			final List<Dep> deps = document.getDepsFromTerm(head);
			final Term[][][] childSolutions = new Term[deps.size()][][];
			for (int i = 0; i < deps.size(); ++i) {
				final Term child = deps.get(i).getTo();
				if (Iterables.contains(terms, child)) {
					childSolutions[i] = matchRecursive(document, terms, child);
					combinations *= childSolutions[i] == null ? 1 : childSolutions[i].length + 1;
				}
			}

			// Combine solutions
			final Term[] solution = new Term[this.tokens.size()];
			final List<Term[]> solutions = Lists.newArrayList();
			outer:
			for (int i = 0; i < combinations; ++i) {
				Arrays.fill(solution, null);
				solution[indexes.get(i % indexes.size())] = head;
				int index = i / indexes.size();
				for (int j = 0; j < deps.size(); ++j) {
					if (childSolutions[j] != null) {
						final int len = childSolutions[j].length;
						final int num = index % (len + 1);
						index = index / (len + 1);
						if (num != len) {
							final Term[] childSolution = childSolutions[j][num];
							for (int k = 0; k < this.tokens.size(); ++k) {
								if (childSolution[k] != null) {
									if (solution[k] != null) {
										continue outer;
									}
									solution[k] = childSolution[k];
								}
							}
						}
					}
				}
				int offset = Integer.MIN_VALUE;
				for (int k = 0; k < this.tokens.size(); ++k) {
					final Term term = solution[k];
					if (term != null) {
						if (term.getOffset() < offset) {
							continue outer;
						}
						offset = term.getOffset();
					}
				}
				solutions.add(solution.clone());
			}
			return solutions.toArray(new Term[solutions.size()][]);
		}

		@Override
		public final int compareTo(final Lexeme lexeme) {
			return this.id.compareTo(lexeme.id);
		}

		@Override
		public final boolean equals(final Object object) {
			if (object == this) {
				return true;
			}
			if (!(object instanceof Lexeme)) {
				return false;
			}
			final Lexeme other = (Lexeme) object;
			return this.id.equals(other.id);
		}

		@Override
		public final int hashCode() {
			return this.id.hashCode();
		}

		public final <T extends Appendable> T toString(final T out) throws IOException {
			out.append(this.id);
			out.append("\ttokens=");
			for (int i = 0; i < this.tokens.size(); ++i) {
				out.append(i == 0 ? "" : ",");
				this.tokens.get(i).toString(out);
			}
			final Map<String, String> properties = getProperties();
			for (final String name : Ordering.natural().immutableSortedCopy(properties.keySet())) {
				final String value = properties.get(name);
				out.append('\t').append(name).append('=').append(value);
			}
			return out;
		}

		@Override
		public final String toString() {
			try {
				return toString(new StringBuilder()).toString();
			} catch (final IOException ex) {
				throw new Error(ex);
			}
		}

	}

	public static final class Token {

		public static final Token WILDCARD = new Token(null, null, null);

		@Nullable
		private final String lemma;

		@Nullable
		private final String stem;

		@Nullable
		private final String pos;

		private Token(@Nullable final String lemma, @Nullable final String stem,
					  @Nullable final String pos) {
			this.lemma = lemma == null ? null : lemma.toLowerCase().intern();
			this.stem = stem == null ? null : stem.toLowerCase().intern();
			this.pos = pos == null ? null : pos.toUpperCase().intern();
		}

		public static Token create(@Nullable final String lemma, @Nullable final String stem,
								   @Nullable final String pos) {
			return lemma == null && stem == null && pos == null ? WILDCARD //
					: new Token(lemma, stem, pos);
		}

		public static Token parse(final String string) {
			return parse(string, null);
		}

		public static Token parse(final String string, @Nullable String altLemma) {
			final String[] fields = string.split("\\|");
			String lemma = fields[0].trim();
			final String stem = fields[1].trim();
			final String pos = fields[2].trim();
			if (".".equals(lemma)) {
				lemma = altLemma;
			}
			return create("*".equals(lemma) ? null : lemma, "*".equals(stem) ? null : stem,
					"*".equals(pos) ? null : pos);
		}

		@Nullable
		public String getLemma() {
			return this.lemma;
		}

		@Nullable
		public String getStem() {
			return this.stem;
		}

		@Nullable
		public String getPos() {
			return this.pos;
		}

		public boolean match(@Nullable final Term term) {
			return term != null
					&& (this.pos == null || this.pos.equalsIgnoreCase(term.getPos()) || this.pos
					.equals(term.getMorphofeat()))
					&& (this.lemma == null || this.lemma.equalsIgnoreCase(term.getLemma()))
					&& (this.stem == null || this.stem.equalsIgnoreCase(Stemming.stem(null,
					term.getStr())));
		}

		@Override
		public boolean equals(final Object object) {
			if (object == this) {
				return true;
			}
			if (!(object instanceof Token)) {
				return false;
			}
			final Token other = (Token) object;
			return Objects.equal(this.lemma, other.lemma) && Objects.equal(this.stem, other.stem)
					&& Objects.equal(this.pos, other.pos);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.lemma, this.stem, this.pos);
		}

		public <T extends Appendable> T toString(final T out) throws IOException {
			out.append(Objects.firstNonNull(this.lemma, "*"));
			out.append("|");
			out.append(Objects.firstNonNull(this.stem, "*"));
			out.append("|");
			out.append(Objects.firstNonNull(this.pos, "*"));
			return out;
		}

		@Override
		public String toString() {
			try {
				return toString(new StringBuilder()).toString();
			} catch (final IOException ex) {
				throw new Error(ex);
			}
		}

	}

}
