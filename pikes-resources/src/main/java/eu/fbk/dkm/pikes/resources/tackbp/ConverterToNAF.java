package eu.fbk.dkm.pikes.resources.tackbp;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.CharStreams;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import eu.fbk.rdfpro.util.IO;
import eu.fbk.utils.core.CommandLine;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import ixa.kaflib.KAFDocument;

/**
 * Converts the TAC KBP corpus (2011 format) to NAF.
 *
 * @author Francesco Corcoglioniti <corcoglio@fbk.eu> (created 2017-09-22)
 */
public final class ConverterToNAF {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterToNAF.class);

    private static final SimpleDateFormat XML_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final SimpleDateFormat NAF_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static String DEFAULT_URL = "http://pikes.fbk.eu/tackbp/%s";

    private static final DocumentBuilder DOCUMENT_BUILDER;

    private static final StanfordCoreNLP TOKENIZE_PIPELINE;

    static {
        try {
            DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            final Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit");
            props.setProperty("tokenize.americanize", "false");
            props.setProperty("tokenize.normalizeParentheses", "false");
            props.setProperty("tokenize.normalizeOtherBrackets", "false");
            props.setProperty("tokenize.escapeForwardSlashAsterisk", "false");
            props.setProperty("tokenize.untokenizable", "noneKeep");
            props.setProperty("tokenize.asciiQuotes", "true");
            props.setProperty("tokenize.normalizeSpace", "false");
            TOKENIZE_PIPELINE = new StanfordCoreNLP(props);

        } catch (final Throwable ex) {
            throw new Error(ex);
        }
    }

    public static void main(final String... args) {
        try {
            // Parse command line
            final CommandLine cmd = CommandLine.parser().withName("tackbp-converter-to-naf")
                    .withHeader(
                            "Generates input and gold NAFs for the TAC KBP corpus (2011 format)")
                    .withOption("t", "txt",
                            "the TAC KBP '.txt' file containing article texts "
                                    + "(e.g., tac2011test_docs.txt)",
                            "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("k", "key",
                            "the TAC KBP '.key' file containing expected results "
                                    + "(e.g., tac2011test_wiki2011.key)",
                            "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("n", "naf", "the FOLDER where to write NAFs", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("c", "conll",
                            "the FILE where to write the gold NERC data in the CONLL format",
                            "FILE", CommandLine.Type.FILE, true, false, false)
                    .withOption("a", "aida",
                            "the FILE where to write the gold EL data in the AIDA format", "FILE",
                            CommandLine.Type.FILE, true, false, false)
                    .withOption("u", "url-template", "URL template (with %s for the document ID)",
                            "URL", CommandLine.Type.STRING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            // Read options
            final Path txtPath = cmd.getOptionValue("t", Path.class);
            final Path keyPath = cmd.getOptionValue("k", Path.class);
            final Path nafPath = cmd.getOptionValue("n", Path.class);
            final Path conllPath = cmd.getOptionValue("c", Path.class);
            final Path aidaPath = cmd.getOptionValue("a", Path.class);
            final String urlTemplate = cmd.getOptionValue("u", String.class, DEFAULT_URL);

            // Parse input .txt and .key files
            final List<Document> documents = parse(txtPath, keyPath);

            // Generate NAFs, CONLL and AIDA
            generate(nafPath, conllPath, aidaPath, documents, urlTemplate);

        } catch (final Throwable ex) {
            // Handle failure
            CommandLine.fail(ex);
        }
    }

    private static List<Document> parse(final Path txtPath, final Path keyPath)
            throws IOException {

        // Parse queries from the .key file
        final Multimap<String, Query> queries = HashMultimap.create();
        final Multiset<String> nercClasses = HashMultiset.create();
        int nilCount = 0;
        try (Reader in = IO.utf8Reader(IO.buffer(IO.read(keyPath.toString())))) {
            for (final String line : CharStreams.readLines(in)) {

                // Extract fields from current row
                final String[] fields = line.split("\t");
                final String docId = fields[0];
                final String queryId = fields[1];
                final String queryText = fields[3];
                final String expectedNelId = fields[4];
                final String expectedNercId = fields[5];

                // Create and index query
                final Query query = new Query(docId, queryId, queryText, expectedNercId,
                        expectedNelId);
                queries.put(docId, query);

                // Update statistics
                nercClasses.add(expectedNercId);
                nilCount += query.expectedNelUri == null ? 1 : 0;
            }
        }
        final int numQueries = queries.size();

        // Parse documents from the .txt file, associating them to corresponding queries
        final Map<String, Document> documents = Maps.newHashMap();
        try (Reader in = IO.utf8Reader(IO.buffer(IO.read(txtPath.toString())))) {
            for (final String line : CharStreams.readLines(in)) {

                // Extract fields from current row
                final String[] fields = line.split("\t");
                final String docId = fields[0];
                final String queryId = fields[1];
                final String queryText = fields[3];
                final String docText = fields[4];

                // Handle two cases to deal with documents having multiple queries
                Document document = documents.get(docId);
                if (document == null) {
                    // Create document when first encountered, associating it to all its queries
                    final Collection<Query> docQueries = queries.get(docId);
                    if (docQueries.isEmpty()) {
                        LOGGER.warn("No matching entry(ies) in .key file for document " + docId);
                    }
                    document = new Document(docId, docText, queries.get(docId));
                    documents.put(docId, document);

                } else {
                    // Verify that the document text is the same
                    if (!document.docXml.equals(docText)) {
                        LOGGER.warn("Different texts for document " + docId);
                    }
                }

                // Check there is a corresponding .key query entry for the current .txt row
                final Query matchingQuery = document.queries.stream()
                        .filter(q -> q.queryId.equals(queryId)).findFirst().orElse(null);
                if (matchingQuery == null) {
                    LOGGER.warn("No entry in .key file for document " + docId + " and query "
                            + queryId);
                } else if (!matchingQuery.queryText.equals(queryText)) {
                    LOGGER.warn("Different query text for .txt and .key files for query " + queryId
                            + ": " + queryText + " - " + matchingQuery.queryText);
                }

                // Remove matching query, so that at the end we can detect unreferenced queries
                if (matchingQuery != null) {
                    queries.remove(docId, matchingQuery);
                }
            }
        }

        // Check there are no unreferenced queries in the .key file
        if (!queries.values().isEmpty()) {
            final StringBuilder builder = new StringBuilder(
                    "There are .key query entries not referenced in the .txt file:");
            for (final Query query : queries.values()) {
                builder.append(" ").append(query.docId).append(" ").append(query.queryId);
            }
            LOGGER.warn(builder.toString());
        }

        // Log parse results / statistics
        LOGGER.info("Parsed {} query entries for {} documents, {} NILs, {} NERC classes: {}",
                numQueries, documents.size(), nilCount, nercClasses.elementSet().size(),
                Joiner.on(", ").join(nercClasses.entrySet().stream()
                        .map(e -> e.getElement() + ":" + e.getCount()).toArray()));

        // Sort documents
        final List<Document> sortedDocuments = Lists.newArrayList(documents.values());
        sortedDocuments.sort((d1, d2) -> d1.docId.compareTo(d2.docId));

        // Return parsed documents
        return sortedDocuments;
    }

    private static void generate(final Path nafPath, final Path conllPath, final Path aidaPath,
            final Iterable<Document> documents, final String urlTemplate) throws IOException {

        // Writers for CONLL and AIDA data
        Writer conllWriter = null;
        Writer aidaWriter = null;

        try {
            // Open CONLL and AIDA files for writing
            conllWriter = IO.utf8Writer(IO.buffer(IO.write(conllPath.toString())));
            aidaWriter = IO.utf8Writer(IO.buffer(IO.write(aidaPath.toString())));

            // Generate and emit one NAF at a time
            int nafCount = 0;
            for (final Document document : documents) {

                // Create NAF document
                final KAFDocument naf = new KAFDocument("en", "v3");

                // Set text
                naf.setRawText(Joiner.on("\n").join(document.docTokens.stream()
                        .map(l -> Joiner.on(" ").join(l)).collect(Collectors.toList())));

                // Set title, date, ID, source/type in the fileDesc structure
                final KAFDocument.FileDesc fileDesc = naf.createFileDesc();
                fileDesc.title = document.docTitle;
                fileDesc.creationtime = NAF_DATE_FORMAT.format(document.docDate);
                fileDesc.filename = document.docId;
                fileDesc.filetype = document.docSource + "/" + document.docType;

                // Set URI and ID in the public structure
                final KAFDocument.Public aPublic = naf.createPublic();
                aPublic.uri = String.format(urlTemplate, document.docId);
                aPublic.publicId = document.docId;

                // Write NAF to file
                final Path outFile = nafPath.resolve(document.docId + ".naf");
                naf.save(outFile.toFile());

                // Emit document start tags for both CONLL and AIDA files
                conllWriter.write("-DOCSTART- " + document.docId + " O O\n");
                aidaWriter.write("-DOCSTART- (" + document.docId + ")\n");

                // Iterate over text sentences in the document
                for (final List<String> sentence : document.docTokens) {

                    // Skip empty sentences
                    if (sentence.isEmpty()) {
                        continue;
                    }

                    // Locate mentions of queries inside the sentence tokens
                    final Query[] mentions = new Query[sentence.size()];
                    for (final Query query : document.queries) {
                        final List<String> queryTokens = ImmutableList
                                .copyOf(Iterables.concat(tokenize(query.queryText)));
                        outer: for (int i = 0; i < sentence.size(); ++i) {
                            for (int j = 0; j < queryTokens.size(); ++j) {
                                if (mentions[i + j] != null || !sentence.get(i + j)
                                        .equalsIgnoreCase(queryTokens.get(j))) {
                                    continue outer;
                                }
                            }
                            for (int j = 0; j < queryTokens.size(); ++j) {
                                mentions[i + j] = query;
                            }
                        }
                    }

                    // Emit sentence to both CONLL and AIDA files
                    for (int i = 0; i < sentence.size(); ++i) {

                        // Determine NERC tag for current token
                        String nercTag = "O";
                        if (mentions[i] != null) {
                            final boolean b = i > 0 && mentions[i - 1] != null
                                    && mentions[i - 1] != mentions[i] && mentions[i
                                            - 1].expectedNercClass == mentions[i].expectedNercClass;
                            nercTag = (b ? "B-" : "I-")
                                    + mentions[i].expectedNercClass.toUpperCase();
                        }

                        // Determine EL tag for current token
                        String elTag = null;
                        String elAnchor = null;
                        if (mentions[i] != null) {
                            final boolean b = i == 0 || mentions[i - 1] != mentions[i];
                            elTag = b ? "B" : "I";
                            elAnchor = sentence.get(i);
                            for (int j = i - 1; j >= 0 && mentions[j] == mentions[i]; --j) {
                                elAnchor = sentence.get(j) + " " + elAnchor;
                            }
                            for (int j = i + 1; j < sentence.size()
                                    && mentions[j] == mentions[i]; ++j) {
                                elAnchor = elAnchor + " " + sentence.get(j);
                            }
                        }

                        // Emit current token to both CONLL and AIDA files
                        final String token = sentence.get(i);
                        conllWriter.write(token + " - - " + nercTag + "\n");
                        if (nafCount < 58000) {
                            aidaWriter.write(token + (elTag == null ? ""
                                    : "\t" + elTag + "\t" + elAnchor + "\t"
                                            + (mentions[i].expectedNelUri == null ? "--NME--"
                                                    : mentions[i].expectedNelId + "\t"
                                                            + mentions[i].expectedNelUri
                                                            + "\t0\t/m/x"))
                                    + "\n");
                        }
                    }

                    // Write empty line to separate sentences both CONLL and AIDA files
                    conllWriter.write("\n");
                    aidaWriter.write("\n");
                }

                // Increase number of processed NAFs
                ++nafCount;
            }

            // Log results
            LOGGER.info("{} NAF files emitted in {}", nafCount, nafPath);

        } finally {
            // Close CONLL and AIDA output files
            IO.closeQuietly(conllWriter);
            IO.closeQuietly(aidaWriter);
        }
    }

    private static List<List<String>> tokenize(final String string) {

        // Tokenize
        final Annotation annotation = new Annotation(string);
        TOKENIZE_PIPELINE.annotate(annotation);

        // Convert to list of string tokens
        final List<List<String>> tokens = Lists.newArrayList();
        for (final CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
            final List<String> sentenceTokens = Lists.newArrayList();
            tokens.add(sentenceTokens);
            for (final CoreLabel token : sentence.get(TokensAnnotation.class)) {
                final String text = ascii(token.originalText());
                // have to further split tokens embedding a space (e.g., a phone number) as there
                // is no way to encode them in the CONLL format, and if we try using a
                // non-breakable space then the AIDA evaluator will explode
                for (final String t : text.split("\\s+")) {
                    if (!Strings.isNullOrEmpty(t)) {
                        sentenceTokens.add(t);
                    }
                }
            }
        }
        return tokens;
    }

    private static String ascii(final String string) {
        final StringBuilder builder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            final char ch = string.charAt(i);
            if (ch >= 32 && ch < 127) {
                builder.append(ch);
            } else if (ch == '©') {
                builder.append("(c)");
            } else if (ch == '™') {
                builder.append("(tm)");
            } else if (ch == '®') {
                builder.append("(r)");
            } else if (ch == '•' || ch == '·') {
                builder.append("*");
            } else if (ch == 'Ø') {
                builder.append("0");
            } else if (ch == '‑') {
                builder.append("-");
            } else if (ch == '´') {
                builder.append("'");
            } else if (ch == '¨') {
                builder.append("\"");
            } else if (ch == '¸' || ch == '，') {
                builder.append(",");
            } else {
                final String s = Normalizer.normalize("" + ch, Normalizer.Form.NFD);
                for (final char c : s.toCharArray()) {
                    if (c <= '\u007F') {
                        builder.append(c);
                    } else {
                        // builder.append(' '); // TODO: uncomment
                    }
                }
            }
        }
        final String result = builder.toString();
        if (!result.equals(string)) {
            LOGGER.warn("Normalized {} to {}", string, result);
        }
        return StringEscapeUtils.unescapeXml(result);
    }

    private static final class Document {

        final String docId;

        final String docXml;

        final Date docDate;

        final String docSource;

        final String docType;

        final String docTitle;

        final List<List<String>> docTokens;

        final List<Query> queries;

        public Document(final String docId, final String docXml, final Iterable<Query> queries) {

            // Check and store parameters
            this.docId = Objects.requireNonNull(docId);
            this.docXml = Objects.requireNonNull(docXml);
            this.queries = ImmutableList.copyOf(queries);

            try {
                // Parse XML
                final org.w3c.dom.Document document = DOCUMENT_BUILDER
                        .parse(new InputSource(new StringReader(docXml)));

                // Extract and check document ID embedded in the XML
                final NodeList docIdNodes = document.getElementsByTagName("DOCID");
                if (docIdNodes.getLength() == 1) {
                    final String parsedDocId = docIdNodes.item(0).getTextContent().trim();
                    if (!docId.equals(parsedDocId)) {
                        LOGGER.warn("DOCID XML element " + parsedDocId
                                + " does not match ID of document " + docId);
                    }
                }

                // Extract document date from the XML
                Date date = new Date();
                final NodeList dateNodes = document.getElementsByTagName("DATETIME");
                if (dateNodes.getLength() == 1) {
                    final String dateStr = dateNodes.item(0).getTextContent().trim();
                    try {
                        date = XML_DATE_FORMAT.parse(dateStr);
                    } catch (final Throwable ex) {
                        LOGGER.warn("Could not parse <DATETIME> value " + dateStr);
                    }
                } else {
                    LOGGER.warn("No <DATETIME> XML element for document " + docId);
                }
                this.docDate = date;

                // Extract document source and type from <DOCTYPE>
                final NodeList doctypeNodes = document.getElementsByTagName("DOCTYPE");
                if (doctypeNodes.getLength() == 1) {
                    final Element doctypeElement = (Element) doctypeNodes.item(0);
                    this.docSource = toNormalCase(doctypeElement.getAttribute("SOURCE").trim());
                    this.docType = toNormalCase(doctypeElement.getTextContent().trim());
                } else {
                    LOGGER.warn("No <DOCTYPE> XML element for document " + docId);
                    this.docSource = "";
                    this.docType = "";
                }

                // Extract document title from the XML. Map all uppercase to all lowercase
                final NodeList headlineNodes = document.getElementsByTagName("HEADLINE");
                if (headlineNodes.getLength() == 1) {
                    this.docTitle = toNormalCase(headlineNodes.item(0).getTextContent().trim());
                } else {
                    this.docTitle = "";
                    LOGGER.warn("No <HEADLINE> XML element for document " + docId);
                }

                // Extract document text from the XML, by concatenating all <P> paragraphs
                final List<List<String>> tokens = Lists.newArrayList();
                if (!this.docTitle.isEmpty()) {
                    tokens.addAll(tokenize(toSentence(this.docTitle)));
                }
                collectText(tokens, document.getElementsByTagName("TEXT"));
                this.docTokens = ImmutableList.copyOf(tokens);
                if (this.docTokens.size() <= 1) {
                    LOGGER.warn("No text extracted for document " + docId);
                }

            } catch (final Throwable ex) {
                Throwables.throwIfUnchecked(ex);
                throw new RuntimeException(ex);
            }
        }

        private static void collectText(final List<List<String>> tokens, final NodeList nodes) {
            for (int i = 0; i < nodes.getLength(); ++i) {
                final Node node = nodes.item(i);
                if (node instanceof Text) {
                    final String text = node.getTextContent().trim();
                    if (!text.isEmpty()) {
                        final List<List<String>> paragraph = tokenize(text);
                        tokens.add(ImmutableList.of()); // empty list for paragraph separator
                        tokens.addAll(paragraph);
                    }
                } else {
                    collectText(tokens, node.getChildNodes());
                }
            }
        }

        private static String toNormalCase(final String string) {
            final String result = string.toUpperCase().equals(string) ? string.toLowerCase()
                    : string;
            return result;
        }

        private static String toSentence(String string) {
            string = StringUtils.capitalize(string).trim();
            return string.endsWith(".") ? string : string + ".";
        }

    }

    private static final class Query {

        private static final Pattern NIL_PATTERN = Pattern.compile("NIL[0-9]+");

        final String docId;

        final String queryId;

        final String queryText;

        final String expectedNercClass;

        final String expectedNelId;

        @Nullable
        final String expectedNelUri;

        public Query(final String docId, final String queryId, final String queryText,
                final String expectedNercClass, final String expectedNelId) {

            this.docId = Objects.requireNonNull(docId);
            this.queryId = Objects.requireNonNull(queryId);
            this.queryText = Objects.requireNonNull(queryText);
            this.expectedNercClass = Objects.requireNonNull(expectedNercClass).replace("GPE",
                    "LOC");
            this.expectedNelId = Objects.requireNonNull(expectedNelId);
            this.expectedNelUri = NIL_PATTERN.matcher(expectedNelId).matches() ? null
                    : "http://dbpedia.org/resource/" + expectedNelId;
        }

    }

}
