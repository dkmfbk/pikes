package eu.fbk.dkm.pikes.raid;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import eu.fbk.dkm.pikes.naflib.Corpus;
import eu.fbk.dkm.pikes.naflib.OpinionPrecisionRecall;
import eu.fbk.dkm.pikes.rdf.RDFGenerator;
import eu.fbk.dkm.pikes.rdf.Renderer;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import eu.fbk.utils.core.Range;
import eu.fbk.utils.svm.Util;
import eu.fbk.dkm.pikes.rdf.vocab.KS_OLD;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Tracker;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.Polarity;
import ixa.kaflib.Term;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.StreamSupport;

public final class Analyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Analyzer.class);

    private static final Mustache INDEX_TEMPLATE = loadTemplate(Analyzer.class.getSimpleName()
            + ".index.html");

    private static final Mustache SENTENCE_TEMPLATE = loadTemplate(Analyzer.class.getSimpleName()
            + ".sentence.html");

    private final Set<String> goldLabels;

    @Nullable
    private final Set<String> testLabels;

    @Nullable
    private final Extractor extractor;

    @Nullable
    private final Path reportPath;

    @Nullable
    private final List<Map<String, Object>> reportModel;

    @Nullable
    private final Renderer reportRenderer;

    private final Component[] components;

    private final OpinionPrecisionRecall.Evaluator evaluator;

    private final ReadWriteLock lock;

    private Analyzer(final Iterable<String> goldLabels,
            @Nullable final Iterable<String> testLabels, @Nullable final Extractor extractor,
            @Nullable final Path reportPath, final Component... components) {

        Preconditions.checkNotNull(goldLabels);
        Preconditions.checkArgument(Iterables.size(goldLabels) > 0);
        Preconditions.checkArgument(extractor != null || testLabels != null);
        Preconditions.checkArgument(extractor == null || testLabels == null);
        Preconditions.checkArgument(testLabels == null || Iterables.size(testLabels) > 0);

        Renderer renderer = null;
        if (reportPath != null) {
            final List<IRI> nodeTypes = ImmutableList.<IRI>builder()
                    .addAll(Renderer.DEFAULT_NODE_TYPES).add(KS_OLD.ATTRIBUTE).build();
            final Map<Object, String> colorMap = ImmutableMap.<Object, String>builder()
                    .putAll(Renderer.DEFAULT_COLOR_MAP).build();
            final Map<Object, String> styleMap = ImmutableMap
                    .<Object, String>builder()
                    .putAll(Renderer.DEFAULT_STYLE_MAP)
                    .put(KS_OLD.ATTRIBUTE, "fontname=\"helvetica-oblique\"")
                    .put(KS_OLD.POSITIVE_OPINION, "fontcolor=green4 fontname=\"helvetica-bold\"")
                    .put(Statements.VALUE_FACTORY.createIRI(KS_OLD.POSITIVE_OPINION + "-from"),
                            "color=green4 fontcolor=green4 penwidth=0.5")
                    .put(KS_OLD.NEGATIVE_OPINION, "fontcolor=red4 fontname=\"helvetica-bold\"")
                    .put(Statements.VALUE_FACTORY.createIRI(KS_OLD.NEGATIVE_OPINION + "-from"),
                            "color=red4 fontcolor=red4 penwidth=0.5")
                    .put(KS_OLD.NEUTRAL_OPINION, "fontcolor=ivory4 fontname=\"helvetica-bold\" ")
                    .put(Statements.VALUE_FACTORY.createIRI(KS_OLD.NEUTRAL_OPINION + "-from"),
                            "color=ivory4 fontcolor=ivory4 penwidth=0.5").build();
            renderer = Renderer.builder().withNodeTypes(nodeTypes).withColorMap(colorMap)
                    .withStyleMap(styleMap).withNodeNamespaces(ImmutableSet.of()).build();
        }

        this.goldLabels = Sets.newHashSet(goldLabels);
        this.testLabels = testLabels == null ? null : Sets.newHashSet(testLabels);
        this.extractor = extractor;
        this.reportPath = reportPath;
        this.reportModel = reportPath == null ? null : Lists.newArrayList();
        this.reportRenderer = renderer;
        this.components = ImmutableSet.copyOf(components).toArray(new Component[0]);
        this.evaluator = OpinionPrecisionRecall.evaluator();
        this.lock = new ReentrantReadWriteLock();
    }

    public Analyzer add(final KAFDocument document) {

        this.lock.readLock().lock();
        try {
            synchronized (document) {
                doAdd(document);
            }
        } finally {
            this.lock.readLock().unlock();
        }
        return this;
    }

    public Analyzer add(final Iterable<KAFDocument> documents) {

        this.lock.readLock().lock();
        try {
            StreamSupport.stream(documents.spliterator(), true).forEach(document -> {
                Preconditions.checkNotNull(document);
                synchronized (document) {
                    doAdd(document);
                }
            });
        } finally {
            this.lock.readLock().unlock();
        }
        return this;
    }

    public OpinionPrecisionRecall complete() {

        this.lock.writeLock().lock();
        try {
            return doComplete();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private synchronized void doAdd(final KAFDocument document) {

        // Retrieve gold opinions from the NAF
        final List<Opinion> goldOpinions = Lists.newArrayList();
        for (final String label : this.goldLabels) {
            goldOpinions.addAll(document.getOpinions(label));
        }

        // Retrieve test opinions, either from the NAF or by running the Extractor
        final List<Opinion> testOpinions;
        if (this.extractor != null) {
            if (this.components.length == Component.values().length) {
                this.extractor.extract(document, "_test", this.components);
            } else {
                this.extractor.refine(document, this.goldLabels, "_test", this.components);
            }
            testOpinions = document.getOpinions("_test");
        } else {
            testOpinions = Lists.newArrayList();
            for (final String label : this.testLabels) {
                testOpinions.addAll(document.getOpinions(label));
            }
        }

        // Keep only selected components of the opinions
        // Component.retain(goldOpinions, this.components);
        // Component.retain(testOpinions, this.components);

        // Update the evaluation results
        this.evaluator.add(goldOpinions, testOpinions);

        // Update the report, if enabled
        if (this.reportPath != null) {
            final Multimap<Integer, Opinion> goldMap = toMultimap(goldOpinions);
            final Multimap<Integer, Opinion> testMap = toMultimap(testOpinions);
            for (int sentenceID = 1; sentenceID <= document.getNumSentences(); ++sentenceID) {
                if (goldMap.containsKey(sentenceID) || testMap.containsKey(sentenceID)) {
                    final String file = new File(document.getPublic().publicId).getName() + "_"
                            + sentenceID + ".html";
                    final Model model = RDFGenerator.DEFAULT.generate(document,
                            ImmutableList.of(sentenceID));
                    final StringBuilder sentenceMarkup = new StringBuilder();
                    final StringBuilder sentenceParsing = new StringBuilder();
                    final StringBuilder sentenceGraph = new StringBuilder();
                    try {
                        renderOpinions(sentenceMarkup, document, sentenceID,
                                goldMap.get(sentenceID), testMap.get(sentenceID));
                        this.reportRenderer.renderParsing(sentenceParsing, document, model,
                                sentenceID);
                        this.reportRenderer.renderGraph(sentenceGraph, QuadModel.wrap(model),
                                Renderer.Algorithm.NEATO);
                        runTemplate(this.reportPath.resolve(file).toFile(), SENTENCE_TEMPLATE,
                                ImmutableMap.of("markup", sentenceMarkup, "parsing",
                                        sentenceParsing, "graph", sentenceGraph));
                    } catch (final IOException ex) {
                        Throwables.propagate(ex);
                    }
                    final Map<String, Object> sentenceModel = Maps.newHashMap();
                    sentenceModel.put("file", file);
                    sentenceModel.put("document", document.getPublic().publicId);
                    sentenceModel.put("sentence", sentenceID);
                    sentenceModel.put("markup", sentenceMarkup);
                    synchronized (this.reportModel) {
                        this.reportModel.add(sentenceModel);
                    }
                }
            }
        }
    }

    private OpinionPrecisionRecall doComplete() {

        // Complete reports, if enabled
        if (this.reportPath != null) {
            try {
                Collections.sort(this.reportModel, (final Map<String, Object> m1,
                        final Map<String, Object> m2) -> {
                    final String d1 = m1.get("document").toString();
                    final String d2 = m2.get("document").toString();
                    int result = d1.compareTo(d2);
                    if (result == 0) {
                        final int s1 = Integer.parseInt(m1.get("sentence").toString());
                        final int s2 = Integer.parseInt(m2.get("sentence").toString());
                        result = s1 - s2;
                    }
                    return result;
                });
                String file = null;
                int fileCounter = 0;
                for (final Map<String, Object> map : this.reportModel) {
                    final String curFile = map.get("document").toString();
                    fileCounter += curFile.equals(file) ? 0 : 1;
                    file = curFile;
                    map.put("id", "S" + fileCounter + "." + map.get("sentence"));
                }
                runTemplate(this.reportPath.resolve("index.html").toFile(), INDEX_TEMPLATE,
                        ImmutableMap.of("sentences", this.reportModel));
                final String css = Resources.toString(
                        Analyzer.class.getResource(Analyzer.class.getSimpleName() + ".css"),
                        Charsets.UTF_8);
                Files.write(css, this.reportPath.resolve("index.css").toFile(), Charsets.UTF_8);

            } catch (final IOException ex) {
                Throwables.propagate(ex);
            }
        }

        // Return precision / recall stats
        return this.evaluator.getResult();
    }

    private static void renderOpinions(final Appendable out, final KAFDocument document,
            final int sentenceID, final Iterable<Opinion> goldOpinions,
            final Iterable<Opinion> testOpinions) throws IOException {

        // Extract the terms of the sentence.
        final List<Term> sentenceTerms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(
                document.getSentenceTerms(sentenceID));
        final Range sentenceRange = Range.enclose(NAFUtils.rangesFor(document, sentenceTerms));
        final String text = document.getRawText().replace("&nbsp;", " ");

        // Align gold and test opinions
        final Opinion[][] pairs = Util.align(Opinion.class, goldOpinions, testOpinions, true,
                true, true, OpinionPrecisionRecall.matcher());

        // Identify the ranges of text to highlight in the sentence
        for (final Opinion[] pair : pairs) {

            // Retrieve gold and test opinions (possibly null)
            final Opinion goldOpinion = pair[0];
            final Opinion testOpinion = pair[1];

            // Create sets for the different types of text spans to be highlighted
            final Set<Term> headTerms = Sets.newHashSet();
            final Set<Range> targetGoldRanges = Sets.newHashSet();
            final Set<Range> targetTestRanges = Sets.newHashSet();
            final Set<Range> holderGoldRanges = Sets.newHashSet();
            final Set<Range> holderTestRanges = Sets.newHashSet();
            final Set<Range> expGoldRanges = Sets.newHashSet();
            final Set<Range> expTestRanges = Sets.newHashSet();
            Polarity goldPolarity = null;
            Polarity testPolarity = null;

            // Process gold opinion (if any)
            if (goldOpinion != null) {
                if (goldOpinion.getOpinionTarget() != null) {
                    final List<Term> t = goldOpinion.getOpinionTarget().getSpan().getTargets();
                    targetGoldRanges.addAll(NAFUtils.rangesFor(document, t));
                    headTerms.addAll(NAFUtils.extractHeads(document, null, t, NAFUtils
                            .matchExtendedPos(document, "NN", "PRP", "JJP", "DTP", "WP", "VB")));
                }
                if (goldOpinion.getOpinionHolder() != null) {
                    final List<Term> h = goldOpinion.getOpinionHolder().getSpan().getTargets();
                    holderGoldRanges.addAll(NAFUtils.rangesFor(document, h));
                    headTerms.addAll(NAFUtils.extractHeads(document, null, h,
                            NAFUtils.matchExtendedPos(document, "NN", "PRP", "JJP", "DTP", "WP")));
                }
                if (goldOpinion.getOpinionExpression() != null) {
                    final List<Term> e = goldOpinion.getOpinionExpression().getSpan().getTargets();
                    expGoldRanges.addAll(NAFUtils.rangesFor(document, e));
                    headTerms.addAll(NAFUtils.extractHeads(document, null, e,
                            NAFUtils.matchExtendedPos(document, "NN", "VB", "JJ", "R")));
                    goldPolarity = Polarity.forExpression(goldOpinion.getOpinionExpression());
                }
            }

            // Process test opinion (if any)
            if (testOpinion != null) {
                if (testOpinion.getOpinionTarget() != null) {
                    final List<Term> t = testOpinion.getOpinionTarget().getSpan().getTargets();
                    targetTestRanges.addAll(NAFUtils.rangesFor(document, t));
                }
                if (testOpinion.getOpinionHolder() != null) {
                    final List<Term> h = testOpinion.getOpinionHolder().getSpan().getTargets();
                    holderTestRanges.addAll(NAFUtils.rangesFor(document, h));
                }
                if (testOpinion.getOpinionExpression() != null) {
                    final List<Term> e = testOpinion.getOpinionExpression().getSpan().getTargets();
                    expTestRanges.addAll(NAFUtils.rangesFor(document, e));
                    testPolarity = Polarity.forExpression(testOpinion.getOpinionExpression());
                }
            }

            // Split the sentence range based on the highlighted ranges identified before
            final List<Range> headRanges = NAFUtils.rangesFor(document, headTerms);
            @SuppressWarnings("unchecked")
            final List<Range> ranges = sentenceRange.split(ImmutableSet.copyOf(Iterables
                    .<Range>concat(targetGoldRanges, targetTestRanges, holderGoldRanges,
                            holderTestRanges, expGoldRanges, expTestRanges, headRanges)));

            // Emit the HTML
            out.append("<p class=\"opinion\">");
            out.append("<span class=\"opinion-id\" title=\"Test label: ")
                    .append(testOpinion == null ? "-" : testOpinion.getLabel())
                    .append(", gold label: ")
                    .append(goldOpinion == null ? "-" : goldOpinion.getLabel()).append("\">")
                    .append(testOpinion == null ? "-" : testOpinion.getId()).append(" / ")
                    .append(goldOpinion == null ? "-" : goldOpinion.getId()).append("</span>");
            for (final Range range : ranges) {
                final boolean targetGold = range.containedIn(targetGoldRanges);
                final boolean targetTest = range.containedIn(targetTestRanges);
                final boolean holderGold = range.containedIn(holderGoldRanges);
                final boolean holderTest = range.containedIn(holderTestRanges);
                final boolean expGold = range.containedIn(expGoldRanges);
                final boolean expTest = range.containedIn(expTestRanges);
                final boolean head = range.containedIn(headRanges);
                int spans = 0;
                if (holderGold || holderTest) {
                    ++spans;
                    final String css = (holderGold ? "hg" : "") + " " + (holderTest ? "ht" : "");
                    out.append("<span class=\"").append(css).append("\">");
                }
                if (targetGold || targetTest) {
                    ++spans;
                    final String css = (targetGold ? "tg" : "") + " " + (targetTest ? "tt" : "");
                    out.append("<span class=\"").append(css).append("\">");
                }
                if (expGold || expTest) {
                    ++spans;
                    final String css = (expGold ? "eg" + goldPolarity.ordinal() : "") + " "
                            + (expTest ? "et" + testPolarity.ordinal() : "");
                    out.append("<span class=\"").append(css).append("\">");
                }
                if (head) {
                    ++spans;
                    out.append("<span class=\"head\">");
                }
                out.append(text.substring(range.begin(), range.end()));
                for (int i = 0; i < spans; ++i) {
                    out.append("</span>");
                }
            }
            out.append("</p>");
        }
    }

    private static Multimap<Integer, Opinion> toMultimap(final Iterable<Opinion> opinions) {
        final Multimap<Integer, Opinion> map = HashMultimap.create();
        for (final Opinion opinion : opinions) {
            int sentenceID = 1;
            if (opinion.getExpressionSpan() != null && !opinion.getExpressionSpan().isEmpty()) {
                sentenceID = opinion.getExpressionSpan().getTargets().get(0).getSent();
            } else if (opinion.getHolderSpan() != null && !opinion.getHolderSpan().isEmpty()) {
                sentenceID = opinion.getHolderSpan().getTargets().get(0).getSent();
            } else if (opinion.getTargetSpan() != null && !opinion.getTargetSpan().isEmpty()) {
                sentenceID = opinion.getHolderSpan().getTargets().get(0).getSent();
            }
            map.put(sentenceID, opinion);
        }
        return map;
    }

    private static Mustache loadTemplate(final String name) {
        try {
            final DefaultMustacheFactory factory = new DefaultMustacheFactory();
            return factory.compile(new InputStreamReader(Analyzer.class.getResource(name)
                    .openStream(), Charsets.UTF_8), name);
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

    private static void runTemplate(final File file, final Mustache template, final Object model)
            throws IOException {
        try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(file.getAbsolutePath())))) {
            template.execute(writer, model);
        }
    }

    public static Analyzer create(final Iterable<String> goldLabels,
            final Iterable<String> testLabels, @Nullable final Path reportPath,
            final Component... components) {
        return new Analyzer(goldLabels, testLabels, null, reportPath, components);
    }

    public static Analyzer create(final Iterable<String> goldLabels, final Extractor extractor,
            @Nullable final Path reportPath, final Component... components) {
        return new Analyzer(goldLabels, null, extractor, reportPath, components);
    }

    public static void main(final String... args) {

        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("fssa-analyze")
                    .withHeader(
                            "Analyze the output of an opinion extractor, "
                                    + "possibly emitting a per-sentence HTML report.")
                    .withOption("p", "properties", "a sequence of key=value properties, used to " //
                            + "select and configure the trainer", "PROPS", Type.STRING, true,
                            false, false)
                    .withOption("c", "components", "the opinion components to consider: " //
                            + "(e)xpression, (h)older, (t)arget, (p)olarity", "COMP", Type.STRING,
                            true, false, false)
                    .withOption("l", "labels", "the labels of gold opinions to consider, comma " //
                            + "separated  (no spaces)", "LABELS", Type.STRING, true, false, false)
                    .withOption("b", "test-labels",
                            "the labels of pre-existing test opinions to consider, comma " //
                                    + "separated  (no spaces)", "LABELS", Type.STRING, true,
                            false, false)
                    .withOption("m", "model",
                            "the extractor model, in case opinion extraction is done on the fly",
                            "FILE", Type.FILE_EXISTING, true, false, false)
                    .withOption("r", "recursive",
                            "recurse into subdirectories of specified input paths")
                    .withOption("@", "list",
                            "interprets input as list of file names, one per line")
                    .withOption("o", "output", "the output path where to emit optional reports",
                            "DIR", Type.DIRECTORY, true, false, false)
                    .withOption(null, "wordnet", "wordnet dict path", "PATH",
                            Type.DIRECTORY_EXISTING, true, false, false)
                    .withFooter(
                            "Zero or more input paths can be specified, corresponding either "
                                    + "to NAF files or directories that are scanned for NAF "
                                    + "files. If the list is empty, an input NAF file will be "
                                    + "read from the standard input. Exactly one option among -m "
                                    + "and -b must be specified.")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            // Extract options
            final Properties properties = Util.parseProperties(cmd.getOptionValue("p",
                    String.class, ""));
            final Component[] components = Component.forLetters(
                    cmd.getOptionValue("c", String.class, "")).toArray(new Component[0]);
            final Set<String> goldLabels = ImmutableSet.copyOf(Splitter.on(',').omitEmptyStrings()
                    .split(cmd.getOptionValue("l", String.class, "")));
            final Set<String> testLabels = ImmutableSet.copyOf(Splitter.on(',').omitEmptyStrings()
                    .split(cmd.getOptionValue("b", String.class, "")));
            final Path modelPath = cmd.getOptionValue("m", Path.class);
            final boolean recursive = cmd.hasOption("r");
            final boolean list = cmd.hasOption("@");
            final Path outputPath = cmd.getOptionValue("o", Path.class, null);
            final List<Path> inputPaths = Lists.newArrayList(cmd.getArgs(Path.class));
            if (!testLabels.isEmpty() && modelPath != null) {
                throw new IllegalArgumentException("Both option -m and -b were specified");
            }

            final String wordnetPath = cmd.getOptionValue("wordnet", String.class);
            if (wordnetPath != null) {
                WordNet.setPath(wordnetPath);
            }

            // Identify input
            final List<Path> files = Util.fileMatch(inputPaths, ImmutableList.of(".naf",
                    ".naf.gz", ".naf.bz2", ".naf.xz", ".xml", ".xml.gz", ".xml.bz2", ".xml.xz"),
                    recursive, list);
            final Iterable<KAFDocument> documents = files != null ? Corpus.create(false, files)
                    : ImmutableList.of(NAFUtils.readDocument(null));

            // Setup the opinion extractor, if enabled
            final Extractor extractor = modelPath == null ? null //
                    : Extractor.readFrom(modelPath, properties);

            // Setup the analyzer
            final Analyzer analyzer = extractor != null ? create(goldLabels, extractor,
                    outputPath, components) : create(goldLabels, testLabels, outputPath,
                    components);

            // Perform the extraction
            final Tracker tracker = new Tracker(LOGGER, null, //
                    "Processed %d NAF files (%d NAF/s avg)", //
                    "Processed %d NAF files (%d NAF/s, %d NAF/s avg)");
            tracker.start();
            StreamSupport.stream(documents.spliterator(), false).forEach(
                    (final KAFDocument document) -> {
                        analyzer.add(document);
                        tracker.increment();
                    });
            tracker.end();

            // Complete the analysis
            final OpinionPrecisionRecall opr = analyzer.complete();
            LOGGER.info("Measured performances:\n{}", opr);

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

}
