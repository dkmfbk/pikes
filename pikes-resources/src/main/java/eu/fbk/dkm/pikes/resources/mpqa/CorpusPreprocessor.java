package eu.fbk.dkm.pikes.resources.mpqa;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.rdfpro.util.IO;
import ixa.kaflib.KAFDocument;
import org.openrdf.model.impl.URIImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CorpusPreprocessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorpusPreprocessor.class);

	private static final String NEWLINE = "&#10;";
	private static final String DEFAULT_DOCS_LIST = "doclist.all";
	private static final String DEFAULT_NAF_DIR = "NAF";
	public static final String DEFAULT_NAMESPACE = "http://eu.fbk.dkm.pikes.resources.mpqa.cs.pitt.edu/corpora/mpqa_corpus/";
	public static final String DEFAULT_ANNOTATION_TSV = "annotations.tsv";
	public static final String DEFAULT_ANNOTATION_HTML = "annotations.html";

	private static final String[] DSA_FIELDS = new String[]{"implicit", "insubstantial",
			"polarity", "intensity", "expression-intensity", "annotation-uncertain",
			"subjective-uncertain"};

	private static final String[] TSV_FIELDS = new String[]{"sentiment", "intensity",
			"attitude", "target", "source", "source-local", "sentence", "dsa-implicit",
			"dsa-insubstantial", "dsa-polarity", "dsa-intensity", "dsa-expression-intensity",
			"dsa-annotation-uncertain", "dsa-subjective-uncertain", "type", "id", "expression"};

	private static final String[] MULTI_FIELDS = new String[]{"nested-source", "attitude-link"};

	public static void main(final String[] args) throws IOException, XMLStreamException {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("corpus-preprocessor")
					.withHeader(
							"Produces NAF files, a TSV file with sentiment annotations "
									+ "and an HTML file with annotated sentences "
									+ "starting from the MPQA v.2 corpus")
					.withOption("i", "input-path", "the base path of the MPQA corpus", "DIR",
							CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("f", "filelist",
							String.format("the file with the docs filenames (relative to input path), default [basedir]/%s", DEFAULT_DOCS_LIST), "FILE",
							CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("o", "output",
							String.format("the output path where to save produced files, default [basedir]/%s", DEFAULT_NAF_DIR),
							"DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
					.withOption("n", "namespace",
							String.format("the namespace for generating document URIs, default %s", DEFAULT_NAMESPACE),
							"NS", CommandLine.Type.STRING, true, false, false)
					.withOption("doc", "doc", "Check only one document", "URL", CommandLine.Type.STRING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputPath = cmd.getOptionValue("i", File.class);

			File outputPath = new File(inputPath.getAbsolutePath() + File.separator + DEFAULT_NAF_DIR);
			if (cmd.hasOption("o")) {
				outputPath = cmd.getOptionValue("o", File.class);
			}
			if (!outputPath.exists()) {
				outputPath.mkdirs();
			}

			File filelist = new File(inputPath.getAbsolutePath() + File.separator + DEFAULT_DOCS_LIST);
			if (cmd.hasOption("f")) {
				filelist = cmd.getOptionValue("f", File.class);
			}

			String namespace = DEFAULT_NAMESPACE;
			if (cmd.hasOption("n")) {
				namespace = cmd.getOptionValue("n", String.class);
			}

			String checkOneDoc = cmd.getOptionValue("doc", String.class);

			preprocess(inputPath, outputPath, filelist, namespace, checkOneDoc);

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}

	public static final void preprocess(@Nullable final File inputPath,
										@Nullable final File outputPath, final File fileList, final String namespace,
										@Nullable final String checkOneDoc)
			throws IOException {

		final List<String> filenames = Files.readLines(fileList, Charsets.UTF_8);

		Writer tsvWriter = null;
		Writer htmlWriter = null;

		try {
			tsvWriter = write(resolve(inputPath, DEFAULT_ANNOTATION_TSV));
			htmlWriter = write(resolve(inputPath, DEFAULT_ANNOTATION_HTML));

			htmlWriter.write("<html>\n<head>\n<style type=\"text/css\">\n");
			htmlWriter.write(".counter { background-color: black; color: white; "
					+ "font-size: 80%; font-weight: bold; padding-left: 10px; "
					+ "padding-right: 10px; margin-right: 10px;}\n");
			htmlWriter.write(".pos { background-color: #95FF4F }\n");
			htmlWriter.write(".neg { background-color: #FF9797 }\n");
			htmlWriter.write(".source { color: black; font-weight: bold }\n");
			htmlWriter.write(".target { color: blue; font-weight: bold }\n");
			htmlWriter.write("</style>\n</head>\n<body>\n");

			int fileCounter = 0;
			final AtomicInteger sentenceCounter = new AtomicInteger(0);
			for (final String filename : filenames) {
				LOGGER.info("Processing document {}/{}: {}", ++fileCounter, filenames.size(),
						filename);

				final String name = filename.replace('/', '_');
				final String documentURI = namespace + name;

				final RecordSet metadata = RecordSet.readFromFile(resolve(inputPath, "meta_anns/"
						+ filename));
				final RecordSet annotations = RecordSet.readFromFile(resolve(inputPath,
						"man_anns/" + filename + "/gateman.eu.fbk.dkm.pikes.resources.mpqa.lre.2.0"));
				final RecordSet sentences = RecordSet.readFromFile(resolve(inputPath, "man_anns/"
						+ filename + "/gatesentences.eu.fbk.dkm.pikes.resources.mpqa.2.0"));
				final String text = fixText(documentURI,
						readText(resolve(inputPath, "docs/" + filename)), sentences);
				final File nafFile = resolve(outputPath, name + ".naf");

				if (checkOneDoc != null && !checkOneDoc.equals(documentURI)) {
					continue;
				}

				if (!text.isEmpty() && !annotations.getRecords().isEmpty()) {
					emitNAF(documentURI, text, metadata, nafFile);
					emitAnnotations(documentURI, text, annotations, sentences, tsvWriter,
							htmlWriter, sentenceCounter);
				}
			}

			htmlWriter.write("</body>\n</html>");

		} finally {
			IO.closeQuietly(tsvWriter);
			IO.closeQuietly(htmlWriter);
		}
	}

	private static void emitNAF(final String documentURI, final String text,
								final RecordSet metadata, final File nafFile) {

		final String source = metadata.getRecordValue("meta_source", "-");
		final String description = metadata.getRecordValue("meta_description", "-");
		final String createTime = metadata.getRecordValue("meta_create_time", null);
		final String mediaFile = metadata.getRecordValue("meta_media_file", null);
		final String mediaType = metadata.getRecordValue("meta_media_type", null);
		final String title = metadata.getRecordValue("meta_title", "-");
		final String country = metadata.getRecordValue("meta_country", "-");
		final String topic = metadata.getRecordValue("meta_topic", "-").toLowerCase();

		final KAFDocument document = new KAFDocument("en", "v3");

		final StringBuilder builder = new StringBuilder();
		int index = 0;
		for (; index < text.length(); ++index) {
			if (Character.isWhitespace(text.charAt(index))) {
				builder.append("&nbsp;");
			}
			else {
				break;
			}
		}
		builder.append(text.substring(index));

		document.setRawText(builder.toString());

		document.createPublic();
		document.getPublic().publicId = new URIImpl(documentURI).getLocalName();
		document.getPublic().uri = documentURI;

		document.createFileDesc();
		document.getFileDesc().author = source + " / " + description;
		document.getFileDesc().creationtime = createTime;
		document.getFileDesc().filename = mediaFile;
		document.getFileDesc().filetype = mediaType;
		document.getFileDesc().title = title + " (" + topic + " / " + country + ")";

		document.save(nafFile.getAbsolutePath());
	}

	private static void emitAnnotations(final String documentURI, final String text,
										final RecordSet annotations, final RecordSet sentences, final Writer tsvWriter,
										final Writer htmlWriter, final AtomicInteger counter) throws IOException {

		// Agents
		HashMap<String, Record> agentRecords = new HashMap<>();
		HashMultimap<String, Record> lastRecords = HashMultimap.create();

		for (final Record agentRecord : annotations.getRecords("GATE_agent")) {
			String sourceString = agentRecord.getValue("nested-source");
			if (sourceString != null) {
				List<String> sources = parseList(sourceString);
				if (sources.size() > 0) {
					String last = sources.get(sources.size() - 1);
					lastRecords.put(last, agentRecord);
				}
			}

			String id = agentRecord.getValue("id");
			if (id == null) {
				continue;
			}
			agentRecords.put(id, agentRecord);
		}

		// Attitudes
		for (final Record thisRecord : annotations.getRecords("GATE_attitude")) {

			final Multimap<Span, String> highlights = HashMultimap.create();
			final Multimap<String, String> fields = HashMultimap.create();

			fields.put("type", "attitude");
			String id = thisRecord.getValue("id");
			if (id != null) {
				fields.put("id", id);
			}

			final Set<String> otherSources = Sets.newHashSet();
			final Set<String> otherTargets = Sets.newHashSet();

			// Extract sentiment value. Skip if absent
			String sentiment = thisRecord.getValue("attitude-type");
			if (sentiment == null || !sentiment.startsWith("sentiment-")) {
				continue;
			}
			sentiment = sentiment.substring("sentiment-".length());
			fields.put("sentiment", sentiment);

			// Extract attitude intensity and span
			final Span expressionSpan = thisRecord.getSpan().align(text);
			fields.put("expression", expressionSpan.toString());
			fields.put("intensity", thisRecord.getValue("intensity"));
			highlights.put(expressionSpan, "pos".equals(sentiment) ? "pos" : "neg");

			// Extract sentence
			Span sentenceSpan = getSentenceSpan(thisRecord, sentences, fields, text, documentURI);
			if (sentenceSpan == null) {
				continue;
			}

			// Extract target span
			final String targetID = thisRecord.getValue("target-link");
			if (targetID != null) {
				final Record targetRecord = annotations.getRecord("GATE_target", "id", targetID);
				if (targetRecord != null) {
					final Span span = targetRecord.getSpan().align(text);
					span.check(text, documentURI);
					fields.put("target", span.toString());
					if (sentenceSpan.contains(span)) {
						highlights.put(span, "target");
					}
					else {
						if (sentenceSpan.overlaps(span)) {
							LOGGER.warn("Target span " + span
									+ " only overlapping with sentence span " + sentenceSpan
									+ " in " + documentURI);
						}
						otherTargets.add(span.apply(text));
					}
				}
			}

			// Extract dsa attributes
			final String attitudeID = thisRecord.getValue("id");
			if (attitudeID != null) {
				final Record dsaRecord = annotations.getRecord("GATE_direct-subjective", "attitude-link", attitudeID);
				if (dsaRecord != null) {
					for (final String name : DSA_FIELDS) {
						String value = dsaRecord.getValue(name);
						if (value != null) {
							fields.put("dsa-" + name, value);
						}
					}

					final String nestedSource = dsaRecord.getValue("nested-source");
					addSources(nestedSource, agentRecords, lastRecords, sentenceSpan, fields, documentURI, text);
				}
			}

			// Emit TSV record
			tsvWriter.append(getTsvString(documentURI, fields));

			// Print debug
			LOGGER.debug(fields.get("type").toString());
			LOGGER.debug(fields.toString());
			for (String expression : fields.get("expression")) {
				LOGGER.debug(expression);
				Span span = new Span(expression);
				LOGGER.debug(span.apply(text));
			}
			System.out.println();

			// Emit HTML sentence
			htmlWriter.append("<p>");
			htmlWriter.append("<span class=\"counter\" title=\"");
			htmlWriter.append("document: ").append(HtmlEscapers.htmlEscaper().escape(documentURI))
					.append(NEWLINE);
			if (!otherSources.isEmpty()) {
				htmlWriter.append("other sources: ").append(Joiner.on(" | ").join(otherSources))
						.append(NEWLINE);
			}
			if (!otherTargets.isEmpty()) {
				htmlWriter.append("other targets: ").append(Joiner.on(" | ").join(otherTargets))
						.append(NEWLINE);
			}
			for (final String name : TSV_FIELDS) {
				final List<String> values = Lists.newArrayList();
				for (final String value : fields.get(name)) {
					if (value != null) {
						values.add(value);
					}
				}
				if (!values.isEmpty()) {
					htmlWriter.append(name).append(": ").append(Joiner.on(" | ").join(values))
							.append(NEWLINE);
				}
			}
			htmlWriter.append("\">" + counter.incrementAndGet() + "</span> ");
			final List<Span> spans = sentenceSpan.split(highlights.keySet());
			for (final Span span : spans) {
				final Set<String> cssClasses = Sets.newHashSet();
				for (final Map.Entry<Span, String> entry : highlights.entries()) {
					if (entry.getKey().contains(span)) {
						cssClasses.add(entry.getValue());
					}
				}
				if (!cssClasses.isEmpty()) {
					htmlWriter.append("<span class=\"").append(Joiner.on(" ").join(cssClasses))
							.append("\">");
				}
				htmlWriter.append(span.apply(text));
				if (!cssClasses.isEmpty()) {
					htmlWriter.append("</span>");
				}
			}
			htmlWriter.append("</p>\n\n");
		}

		// Objective
		for (final Record thisRecord : annotations.getRecords("GATE_objective-speech-event")) {

			final Multimap<String, String> fields = HashMultimap.create();
			fields.put("type", "objective");
			String id = thisRecord.getValue("id");
			if (id != null) {
				fields.put("id", id);
			}

			final Span expressionSpan = thisRecord.getSpan().align(text);
			fields.put("expression", expressionSpan.toString());

			// Extract sentence
			Span sentenceSpan = getSentenceSpan(thisRecord, sentences, fields, text, documentURI);
			if (sentenceSpan == null) {
				continue;
			}

			// Holder
			String sources = thisRecord.getValue("nested-source");
			addSources(sources, agentRecords, lastRecords, sentenceSpan, fields, documentURI, text);

			// Emit TSV string
			tsvWriter.append(getTsvString(documentURI, fields));

			// Print debug
			LOGGER.debug(fields.get("type").toString());
			LOGGER.debug(fields.toString());
			for (String expression : fields.get("expression")) {
				LOGGER.debug(expression);
				Span span = new Span(expression);
				LOGGER.debug(span.apply(text));
			}
			System.out.println();
		}

		// Expressive
		for (final Record thisRecord : annotations.getRecords("GATE_expressive-subjectivity")) {
			final Multimap<String, String> fields = HashMultimap.create();
			fields.put("type", "expressive");
			String id = thisRecord.getValue("id");
			if (id != null) {
				fields.put("id", id);
			}

			final Span expressionSpan = thisRecord.getSpan().align(text);
			fields.put("expression", expressionSpan.toString());

			// Sentence
			Span sentenceSpan = getSentenceSpan(thisRecord, sentences, fields, text, documentURI);
			if (sentenceSpan == null) {
				continue;
			}

			for (final String name : DSA_FIELDS) {
				String value = thisRecord.getValue(name);
				if (value != null) {
					fields.put("dsa-" + name, value);
				}
			}

			// Holder
			String sources = thisRecord.getValue("nested-source");
			addSources(sources, agentRecords, lastRecords, sentenceSpan, fields, documentURI, text);

			// Emit TSV string
			tsvWriter.append(getTsvString(documentURI, fields));

			// Print debug
			LOGGER.debug(fields.get("type").toString());
			LOGGER.debug(fields.toString());
			for (String expression : fields.get("expression")) {
				LOGGER.debug(expression);
				Span span = new Span(expression);
				LOGGER.debug(span.apply(text));
			}
			System.out.println();
		}

		// Subjective
		for (final Record thisRecord : annotations.getRecords("GATE_direct-subjective")) {

			final Multimap<String, String> fields = HashMultimap.create();
			fields.put("type", "subjective");
			String id = thisRecord.getValue("id");
			if (id != null) {
				fields.put("id", id);
			}

			final Span expressionSpan = thisRecord.getSpan().align(text);
			fields.put("expression", expressionSpan.toString());

			// Sentence
			Span sentenceSpan = getSentenceSpan(thisRecord, sentences, fields, text, documentURI);
			if (sentenceSpan == null) {
				continue;
			}

			for (final String name : DSA_FIELDS) {
				String value = thisRecord.getValue(name);
				if (value != null) {
					fields.put("dsa-" + name, value);
				}
			}

			// Holder
			String sources = thisRecord.getValue("nested-source");
			addSources(sources, agentRecords, lastRecords, sentenceSpan, fields, documentURI, text);

			// Emit TSV string
			tsvWriter.append(getTsvString(documentURI, fields));

			// Print debug
			LOGGER.debug(fields.get("type").toString());
			LOGGER.debug(fields.toString());
			for (String expression : fields.get("expression")) {
				LOGGER.debug(expression);
				Span span = new Span(expression);
				LOGGER.debug(span.apply(text));
			}
			System.out.println();
		}
	}

	private static Span getSentenceSpan(Record record, RecordSet sentences, Multimap<String, String> fields, String text, String documentURI) {

		Span ret = null;
		Span okSpan = record.getSpan().align(text);

		if (okSpan.end == 0 || okSpan.begin == okSpan.end) {
			return ret;
		}

		for (final Record sentenceRecord : sentences.getRecords()) {
			final Span span = sentenceRecord.getSpan();
			if (span.contains(okSpan)) {
				ret = span;
				fields.put("sentence", okSpan.toString());
				break;
			}
		}

		if (ret == null) {
			LOGGER.warn("Could not locate sentence for span {} in {}", okSpan.toString(), documentURI);
		}

		return ret;
	}

	private static void addSources(String nestedSource, HashMap<String, Record> agentRecords,
								   HashMultimap<String, Record> lastRecords, Span sentenceSpan,
								   Multimap<String, String> fields, String documentURI, String text) {
		addSources(nestedSource, agentRecords, lastRecords, sentenceSpan, fields, documentURI, text, null, null);
	}

	private static void addSources(String nestedSource, HashMap<String, Record> agentRecords,
								   HashMultimap<String, Record> lastRecords, Span sentenceSpan,
								   Multimap<String, String> fields, String documentURI, String text,
								   @Nullable Multimap<Span, String> highlights,
								   @Nullable Set<String> otherSources) {
		if (nestedSource != null) {
			List<String> sources = parseList(nestedSource);

			if (sources.size() > 0) {
				String last = sources.get(sources.size() - 1);

				addSourceFromRecord(agentRecords.get(last), sentenceSpan, fields, documentURI, text, highlights, otherSources);
				for (Record record : lastRecords.get(last)) {
					addSourceFromRecord(record, sentenceSpan, fields, documentURI, text, highlights, otherSources);
				}
			}
		}

	}

	private static void addSourceFromRecord(Record record, Span sentenceSpan, Multimap<String, String> fields,
											String documentURI, String text) {
		addSourceFromRecord(record, sentenceSpan, fields, documentURI, text, null, null);
	}

	private static void addSourceFromRecord(Record record, Span sentenceSpan, Multimap<String, String> fields,
											String documentURI, String text,
											@Nullable Multimap<Span, String> highlights,
											@Nullable Set<String> otherSources) {
		if (record == null) {
			return;
		}

		final Span span = record.getSpan().align(text);
		if (span.end == 0) {
			return;
		}

		span.check(text, documentURI);
		fields.put("source", span.toString());
		if (sentenceSpan.contains(span)) {
			fields.put("source-local", span.toString());
			if (highlights != null) {
				highlights.put(span, "source");
			}
		}
		else {
			if (sentenceSpan.overlaps(span)) {
				LOGGER.warn("Source span " + span
						+ " only overlapping with sentence span "
						+ sentenceSpan + " in " + documentURI);
			}
			if (otherSources != null) {
				otherSources.add(span.apply(text));
			}
		}

	}

	private static List<String> parseList(String sourceString) {
		List<String> ret = new ArrayList<>();

		String[] parts = sourceString.split(",");
		for (String part : parts) {
			part = part.trim();
			if (part.length() > 0) {
				ret.add(part);
			}
		}

		return ret;
	}

	private static CharSequence getTsvString(String documentURI, Multimap<String, String> fields) {
		StringBuilder ret = new StringBuilder();

		ret.append("document=").append(documentURI);
		for (final String name : TSV_FIELDS) {
			final List<String> values = Lists.newArrayList();
			for (final String value : fields.get(name)) {
				if (value != null) {
					values.add(value);
				}
			}
			if (!values.isEmpty()) {
				ret.append("\t").append(name).append("=")
						.append(Joiner.on("|").join(values).replace('\t', ' '));
			}
		}
		ret.append("\n");

		return ret.toString();
	}

	private static String readText(@Nullable final File file) throws IOException {
		if (file == null || !file.exists()) {
			return "";
		}
		try (Reader reader = RecordSet.read(file)) {
			return CharStreams.toString(reader);
		}
	}

	private static Writer write(final File file) throws IOException {
		Files.createParentDirs(file);
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
				Charsets.UTF_8));
	}

	private static File resolve(@Nullable final File base, final String name) {
		final File actualBase = base != null ? base : new File(System.getProperty("user.dir"));
		return actualBase.toPath().resolve(Paths.get(name)).toFile();
	}

	private static String fixText(final String documentURI, String text, final RecordSet sentences) {

		// For six documents, offsets in the MPQA annotation files are not aligned with the
		// text. The following code fixes the issue by reshaping the text so that it becomes
		// properly aligned with the offset
		if (documentURI.endsWith("xbank_wsj_0583")) {
			text = text.substring(0, 2263) + "   " + text.substring(2263);
		}
		else if (documentURI.endsWith("ula_IZ-060316-01-Trans-1")) {
			text = text.substring(0, 10174) + text.substring(10176);
		}
		else if (documentURI.endsWith("ula_AFGP-2002-600175-Trans")) {
			text = text.substring(0, 7903) + " " + text.substring(7906);
		}
		else if (documentURI.endsWith("ula_chapter-10")) {
			text = text.substring(0, 46929) + text.substring(46932);
		}
		else if (documentURI.endsWith("ula_AFGP-2002-600002-Trans")) {
			text = text.substring(0, 9902) + text.substring(9905, 9938) + text.substring(9941);
		}

		final List<Span> sentenceSpans = Lists.newArrayList();
		for (final Record sentenceRecord : sentences.getRecords()) {
			sentenceSpans.add(sentenceRecord.getSpan());
		}
		Collections.sort(sentenceSpans);

		// Remove <tag> markup (not much) and all newlines
		final StringBuilder builder = new StringBuilder(text);
		boolean insideTag = false;
		for (int i = 0; i < builder.length(); ++i) {
			final char c = builder.charAt(i);
			if (c == '<') {
				insideTag = true;
				builder.setCharAt(i, ' ');
			}
			else if (c == '>') {
				insideTag = false;
				builder.setCharAt(i, ' ');
			}
			else if (insideTag || c == '\n' || c == '\r' || c == '\t') {
				builder.setCharAt(i, ' ');
			}
		}

		for (int i = 0; i < sentenceSpans.size() - 1; ++i) {
			final Span first = sentenceSpans.get(i);
			final Span next = sentenceSpans.get(i + 1);

			// Check sentence boundary, logging a warning if it seems wrong
			if (next.begin >= first.end) {
				boolean allAlpha = true;
				for (int j = first.end - 1; j <= next.begin; ++j) {
					allAlpha = allAlpha && Character.isLetterOrDigit(builder.charAt(j));
				}
				if (allAlpha) {
					LOGGER.warn("Boundary between " + first + " and " + next
							+ " could be wrong in " + documentURI + " ("
							+ text.substring(first.end - 1, next.begin + 1) + ")");
				}
			}

			// Erase text between annotated sentences
			for (int j = first.end; j < next.begin; ++j) {
				builder.setCharAt(j, ' ');
			}

			// Add newlines between sentences so to pass gold sentence splitting to Stanford
			if (next.begin > first.end) {
				builder.setCharAt(next.begin - 1, '\n');
			}
			else {
				builder.setCharAt(isDelim(builder.charAt(next.begin)) ? next.begin
						: next.begin - 1, '\n');
			}
		}

		return builder.toString();
		// return text;
	}

	public static boolean isWord(final char c) {
		return " \t\n\r,;:!?".indexOf(c) < 0;
	}

	public static boolean isDelim(final char c) {
		return " \t\n\r,;:!?.()[]<>~`'\"-".indexOf(c) >= 0;
	}

}
