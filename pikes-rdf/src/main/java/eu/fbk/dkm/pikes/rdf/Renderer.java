package eu.fbk.dkm.pikes.rdf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.util.LatchedWriter;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.Files;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ixa.kaflib.Coref;
import ixa.kaflib.Dep;
import ixa.kaflib.Entity;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.Polarity;
import ixa.kaflib.Predicate;
import ixa.kaflib.Predicate.Role;
import ixa.kaflib.Span;
import ixa.kaflib.Term;

import eu.fbk.dkm.pikes.naflib.OpinionPrecisionRecall;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.utils.Range;
import eu.fbk.dkm.utils.Util;
import eu.fbk.dkm.utils.vocab.GAF;
import eu.fbk.dkm.utils.vocab.KS;
import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.dkm.utils.vocab.NWR;
import eu.fbk.dkm.utils.vocab.OWLTIME;
import eu.fbk.dkm.utils.vocab.SUMO;
import eu.fbk.rdfpro.util.Environment;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.Namespaces;
import eu.fbk.rdfpro.util.Options;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.rdfpro.util.Tracker;

public class Renderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

    private static final String NEWLINE = "&#10;";

    public static final Set<URI> DEFAULT_NODE_TYPES = ImmutableSet.of(KS.ENTITY, KS.ATTRIBUTE);

    // public static final Set<String> DEFAULT_NODE_NAMESPACES = ImmutableSet
    // .of("http://dbpedia.org/resource/");

    public static final Set<String> DEFAULT_NODE_NAMESPACES = ImmutableSet.of();

    public static final Map<Object, String> DEFAULT_COLOR_MAP = ImmutableMap
            .<Object, String>builder() //
            .put("node", "#F0F0F0") //
            .put(NWR.PERSON, "#FFC8C8") //
            .put(NWR.ORGANIZATION, "#FFFF84") //
            .put(NWR.LOCATION, "#A9C5EB") //
            .put(KS.ATTRIBUTE, "#EEBBEE") //
            // .put(KS.MONEY, "#EEBBEE") //
            // .put(KS.FACILITY, "#FFC65B") //
            // .put(KS.PRODUCT, "#FFC65B") //
            // .put(KS.WORK_OF_ART, "#FFC65B") //
            .put(SUMO.PROCESS, "#CFE990") //
            .put(SUMO.RELATION, "#FFFFFF") //
            .put(OWLTIME.INTERVAL, "#B4D1B6") //
            .put(OWLTIME.DATE_TIME_INTERVAL, "#B4D1B6") //
            .put(OWLTIME.PROPER_INTERVAL, "#B4D1B6") //
            .put(NWR.MISC, "#D1BAA2") //
            // .put(KS.LAW, "#D1BAA2") //
            .build();

    public static final Map<Object, String> DEFAULT_STYLE_MAP = ImmutableMap.of();

    public static final Mustache DEFAULT_TEMPLATE = loadTemplate("Renderer.html");

    public static final List<String> DEFAULT_RANKED_NAMESPACES = ImmutableList.of(
            "http://www.newsreader-project.eu/ontologies/propbank/",
            "http://www.newsreader-project.eu/ontologies/nombank/");

    public static final Renderer DEFAULT = Renderer.builder().build();

    // Accepts Mustache, URL, File, String (url / filename / template)

    private static final Mustache loadTemplate(final Object spec) {
        Preconditions.checkNotNull(spec);
        try {
            if (spec instanceof Mustache) {
                return (Mustache) spec;
            }
            final DefaultMustacheFactory factory = new DefaultMustacheFactory();
            // factory.setExecutorService(Environment.getPool()); // BROKEN
            URL url = spec instanceof URL ? (URL) spec : null;
            if (url == null) {
                try {
                    url = Renderer.class.getResource(spec.toString());
                } catch (final Throwable ex) {
                    // ignore
                }
            }
            if (url == null) {
                final File file = spec instanceof File ? (File) spec : new File(spec.toString());
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
            if (url != null) {
                return factory.compile(new InputStreamReader(url.openStream(), Charsets.UTF_8),
                        url.toString());
            } else {
                return factory.compile(new StringReader(spec.toString()),
                        Hash.murmur3(spec.toString()).toString());
            }
        } catch (final IOException ex) {
            throw new IllegalArgumentException("Could not create Mustache template for " + spec);
        }
    }

    private final Set<URI> nodeTypes;

    private final Set<String> nodeNamespaces;

    private final Ordering<Value> valueComparator;

    private final Ordering<Statement> statementComparator;

    private final URI denotedByProperty;

    private final Map<Object, String> colorMap;

    private final Map<Object, String> styleMap;

    private Mustache template;

    private Map<String, ?> templateParameters;

    private Renderer(final Builder builder) {
        this.nodeTypes = builder.nodeTypes == null ? DEFAULT_NODE_TYPES //
                : ImmutableSet.copyOf(builder.nodeTypes);
        this.nodeNamespaces = builder.nodeNamespaces == null ? DEFAULT_NODE_NAMESPACES
                : ImmutableSet.copyOf(builder.nodeNamespaces);
        this.valueComparator = new ValueOrdering(
                builder.rankedNamespaces == null ? DEFAULT_RANKED_NAMESPACES
                        : builder.rankedNamespaces);
        this.statementComparator = new Ordering<Statement>() {

            @Override
            public int compare(final Statement first, final Statement second) {
                final Comparator<Value> vc = Renderer.this.valueComparator;
                int result = vc.compare(first.getSubject(), second.getSubject());
                if (result == 0) {
                    result = vc.compare(first.getPredicate(), second.getPredicate());
                    if (result == 0) {
                        result = vc.compare(first.getObject(), second.getObject());
                        if (result == 0) {
                            result = vc.compare(first.getContext(), second.getContext());
                        }
                    }
                }
                return result;
            }

        };
        this.denotedByProperty = Objects.firstNonNull(builder.denotedByProperty, GAF.DENOTED_BY);
        this.colorMap = builder.colorMap == null ? DEFAULT_COLOR_MAP : ImmutableMap
                .copyOf(builder.colorMap);
        this.styleMap = builder.styleMap == null ? DEFAULT_STYLE_MAP : ImmutableMap
                .copyOf(builder.styleMap);
        this.template = Objects.firstNonNull(builder.template, DEFAULT_TEMPLATE);
        this.templateParameters = builder.templateParameters;
    }

    public void renderAll(final Appendable out, final KAFDocument document, final Model model,
            @Nullable final Object template, @Nullable final Map<String, ?> templateParameters)
            throws IOException {

        final long ts = System.currentTimeMillis();
        final KAFDocument doc = document;
        final long[] times = new long[8];

        final List<Map<String, Object>> sentencesModel = Lists.newArrayList();
        for (int i = 1; i <= doc.getNumSentences(); ++i) {
            final Map<String, Object> sm = Maps.newHashMap();
            sm.put("id", i);
            sm.put("markup", new Renderable(doc, model, i, times, Renderable.SENTENCE_TEXT));
            sm.put("parsing", new Renderable(doc, model, i, times, Renderable.SENTENCE_PARSING));
            sm.put("graph", new Renderable(doc, model, i, times, Renderable.SENTENCE_GRAPH));
            sentencesModel.add(sm);
        }

        final Map<String, Object> documentModel = Maps.newHashMap();
        documentModel.put("title", doc.getPublic().uri);
        documentModel.put("sentences", sentencesModel);
        documentModel.put("metadata", new Renderable(doc, model, -1, times, Renderable.METADATA));
        documentModel.put("mentions", new Renderable(doc, model, -1, times, Renderable.MENTIONS));
        documentModel.put("triples", new Renderable(doc, model, -1, times, Renderable.TRIPLES));
        documentModel.put("graph", new Renderable(doc, model, -1, times, Renderable.GRAPH));
        documentModel.put("naf", new Renderable(doc, model, -1, times, Renderable.NAF));

        documentModel.putAll(this.templateParameters);
        if (templateParameters != null) {
            documentModel.putAll(templateParameters);
        }

        final Mustache actualTemplate = template != null ? loadTemplate(template) : this.template;
        if (out instanceof Writer) {
            Writer outWriter;
            outWriter = actualTemplate.execute((Writer) out, documentModel);
            if (outWriter instanceof LatchedWriter) {
                ((LatchedWriter) outWriter).await();
                outWriter.flush();
            }
        } else {
            final StringWriter writer = new StringWriter();
            actualTemplate.execute(writer, documentModel).close();
            out.append(writer.toString());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Done in {} ms ({} text, {} parsing, {} graphs, {} metadata, "
                    + "{} mentions, {} triples, {} naf)", System.currentTimeMillis() - ts,
                    times[Renderable.SENTENCE_TEXT], times[Renderable.SENTENCE_PARSING],
                    times[Renderable.SENTENCE_GRAPH] + times[Renderable.GRAPH],
                    times[Renderable.METADATA], times[Renderable.MENTIONS],
                    times[Renderable.TRIPLES], times[Renderable.NAF]);
        }
    }

    public void renderGraph(final Appendable out, final Model model, final Algorithm algorithm)
            throws IOException {

        // TODO: handle sentences

        Process process = null;
        File dotFile = null;

        try {
            dotFile = File.createTempFile("graphviz-", ".dot");
            dotFile.deleteOnExit();

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    dotFile), Charsets.UTF_8))) {
                emitDotGraph(writer, model);
            }

            process = new ProcessBuilder().command(
                    Objects.firstNonNull(algorithm, Algorithm.NEATO).name().toLowerCase(),
                    dotFile.getAbsolutePath(), "-Tsvg").start();

            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            final BufferedReader err = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));

            Environment.getPool().submit(new Runnable() {

                @Override
                public void run() {
                    String line;
                    try {
                        while ((line = err.readLine()) != null) {
                            LOGGER.error("[" + algorithm + "] " + line);
                        }
                    } catch (final IOException ex) {
                        LOGGER.error("[" + algorithm + "] " + ex.getMessage(), ex);
                    }
                }

            });

            boolean insideHeader = true;
            String line;
            while ((line = in.readLine()) != null) {
                if (insideHeader && line.startsWith("<svg")) {
                    insideHeader = false;
                }
                if (!insideHeader) {
                    out.append(line).append('\n');
                }
            }
            process.waitFor();

        } catch (final Throwable ex) {
            Throwables.propagateIfPossible(ex, IOException.class);
            Throwables.propagate(ex);

        } finally {
            if (dotFile != null) {
                dotFile.delete();
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void emitDotGraph(final Appendable out, final Model model) throws IOException {

        // Identify graph nodes based on specified node types
        final Set<Resource> nodes = Sets.newHashSet();
        for (final Value value : Iterables.concat(model.subjects(), model.objects())) {
            if (value instanceof URI
                    && Renderer.this.nodeNamespaces.contains(((URI) value).getNamespace())) {
                nodes.add((URI) value);
            }
        }
        for (final Statement statement : model.filter(null, RDF.TYPE, null)) {
            if (Renderer.this.nodeTypes.contains(statement.getObject())) {
                nodes.add(statement.getSubject());
            }
        }

        // Emit header
        out.append("digraph \"\"\n{\n");
        out.append("graph [nodesep=1, ranksep=.02, pad=0, margin=0, pack=clust, width=1000];\n");
        out.append("node  [shape=plaintext, height=0, width=0, fontname=helvetica, fontsize=10];\n");
        out.append("edge  [arrowsize=.5, fontsize=8, fontname=helvetica, len=1.5];\n\n");

        // Emit nodes
        for (final Resource node : nodes) {
            final String id = hash(node);
            final Set<Value> types = model.filter(node, RDF.TYPE, null).objects();
            out.append(id).append(" [");
            out.append("label=<<table border=\"0\" cellborder=\"0\" cellpadding=\"0\" bgcolor=\"");
            out.append(select(this.colorMap, types, "#FFFFFF"));
            out.append("\" href=\"\"><tr><td>");
            out.append(format(node));
            out.append("</td></tr></table>>");
            out.append(" tooltip=\"");
            final Set<Resource> expandedNodes = Sets.newHashSet();
            if (!emitDotTooltip(out, model, node, 0, nodes, expandedNodes)) {
                out.append(" ");
            }
            out.append("\" ");
            out.append(select(this.styleMap, types, ""));
            out.append("];\n");
        }

        // Emit edges
        final Set<List<Value>> encounteredEdges = Sets.newHashSet();
        for (final Statement statement : model) {

            // Ensure that subject and object are both nodes and the edge was not emitted before
            if (!(statement.getObject() instanceof Resource)) {
                continue;
            }
            final Resource sourceNode = statement.getSubject();
            final Resource targetNode = (Resource) statement.getObject();
            if (!nodes.contains(sourceNode) || !nodes.contains(targetNode)
                    || !encounteredEdges.add(ImmutableList.of(sourceNode, targetNode))) {
                continue;
            }
            final String sourceId = hash(sourceNode);
            final String targetId = hash(targetNode);

            // Retrieve the predicates associated to the edge
            final List<URI> properties = Renderer.this.valueComparator.sortedCopy(model.filter(
                    sourceNode, null, targetNode).predicates());

            // Select edge style
            final List<URI> keys = Lists.newArrayList(properties);
            for (final Value sourceType : model.filter(sourceNode, RDF.TYPE, null).objects()) {
                if (sourceType instanceof URI) {
                    keys.add(new URIImpl(sourceType.stringValue() + "-from"));
                }
            }
            for (final Value targetType : model.filter(targetNode, RDF.TYPE, null).objects()) {
                if (targetType instanceof URI) {
                    keys.add(new URIImpl(targetType.stringValue() + "-to"));
                }
            }

            // Emit the edge
            out.append("  ").append(sourceId).append(" -> ").append(targetId).append(" [");
            out.append("label=<<table border=\"0\" cellborder=\"0\" cellpadding=\"0\" href=\"\" tooltip=\"");
            out.append(Joiner.on(" | ").join(format(properties)));
            out.append("\"><tr><td>");
            out.append(format(properties.get(0)));
            out.append("</td></tr></table>>");
            out.append(" ");
            out.append(select(this.styleMap, keys, ""));
            out.append("];\n");
        }

        // Emit footer
        out.append("}");
    }

    private boolean emitDotTooltip(final Appendable out, final Model model, final Resource node,
            final int indent, final Set<Resource> excludedNodes, final Set<Resource> expandedNodes)
            throws IOException {
        boolean notEmpty = false;
        for (final URI pred : Renderer.this.valueComparator.sortedCopy(model.filter(node, null,
                null).predicates())) {
            expandedNodes.add(node);
            for (final Value object : Renderer.this.valueComparator.sortedCopy(model.filter(node,
                    pred, null).objects())) {
                if (!excludedNodes.contains(object)) {
                    out.append(Strings.repeat("    ", indent));
                    out.append(format(pred));
                    out.append(" ");
                    out.append(format(object));
                    final boolean expanded = expandedNodes.contains(object);
                    if (expanded) {
                        out.append(" [...]");
                    }
                    out.append(NEWLINE);
                    notEmpty = true;
                    if (object instanceof Resource && !expanded
                            && !pred.equals(this.denotedByProperty)) {
                        emitDotTooltip(out, model, (Resource) object, indent + 1, excludedNodes,
                                expandedNodes);
                    }
                }
            }
        }
        return notEmpty;
    }

    public void renderText(final Appendable out, final KAFDocument document,
            final Iterable<Term> terms, final Model model) throws IOException {

        final List<Term> termList = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
        final Set<Term> termSet = ImmutableSet.copyOf(termList);
        if (termList.isEmpty()) {
            return;
        }

        final Markable[] markables = Markable.extract(termList, model, this.colorMap);

        final Map<Term, Set<Coref>> corefs = Maps.newHashMap();
        for (final Coref coref : document.getCorefs()) {
            for (final Span<Term> span : coref.getSpans()) {
                for (final Term term : span.getTargets()) {
                    if (termSet.contains(term)) {
                        Set<Coref> set = corefs.get(term);
                        if (set == null) {
                            set = Sets.newHashSet();
                            corefs.put(term, set);
                        }
                        set.add(coref);
                    }
                }
            }
        }

        Markable markable = null;

        int index = NAFUtils.getBegin(termList.get(0));
        final int end = Integer.MAX_VALUE;

        List<Coref> lastCorefs = ImmutableList.of();
        for (int i = 0; i < termList.size(); ++i) {

            final Term term = termList.get(i);
            final int termOffset = term.getOffset();
            final int termLength = NAFUtils.getLength(term);
            final int termBegin = Math.max(termOffset, index);
            final int termEnd = Math.min(termOffset + termLength, end);
            final List<Coref> termCorefs = document.getCorefsByTerm(term);

            if (termBegin > index) {
                final List<Coref> sameCorefs = Lists.newArrayList(lastCorefs);
                sameCorefs.retainAll(termCorefs);
                out.append(sameCorefs.isEmpty() ? " " : "<span class=\"txt_coref\"> </span>");
            }

            if (markable == null) {
                markable = markables[i];
                if (markable != null) {
                    out.append("<span style=\"background-color: ").append(markable.color)
                            .append("\">");
                }
            }

            out.append("<span class=\"txt_term_tip");
            for (final Coref coref : termCorefs) {
                if (coref.getSpans().size() > 1) {
                    out.append(" txt_coref");
                    break;
                }
            }
            out.append("\" title=\"");
            emitTermTooltip(out, document, term);
            out.append("\">");
            out.append(term.getForm());
            out.append("</span>");

            if (markable != null && term == markable.terms.get(markable.terms.size() - 1)) {
                out.append("</span>");
                markable = null;
            }

            index = termEnd;
            lastCorefs = termCorefs;
        }

        if (markable != null) {
            out.append("</span>");
        }
    }

    public void renderText(final Appendable out, final KAFDocument document,
            final Iterable<Term> terms) throws IOException {

        final List<Term> termList = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
        final Set<Term> termSet = ImmutableSet.copyOf(termList);
        if (termList.isEmpty()) {
            return;
        }

        final Map<Term, Set<Coref>> corefs = Maps.newHashMap();
        for (final Coref coref : document.getCorefs()) {
            for (final Span<Term> span : coref.getSpans()) {
                for (final Term term : span.getTargets()) {
                    if (termSet.contains(term)) {
                        Set<Coref> set = corefs.get(term);
                        if (set == null) {
                            set = Sets.newHashSet();
                            corefs.put(term, set);
                        }
                        set.add(coref);
                    }
                }
            }
        }

        List<Term> mentionTerms = ImmutableList.of();
        String mentionType = null;

        int index = NAFUtils.getBegin(termList.get(0));
        final int end = Integer.MAX_VALUE;

        List<Coref> lastCorefs = ImmutableList.of();
        for (final Term term : termList) {

            final int termOffset = NAFUtils.getBegin(term);
            final int termLength = NAFUtils.getLength(term);
            final int termBegin = Math.max(termOffset, index);
            final int termEnd = Math.min(termOffset + termLength, end);
            final List<Coref> termCorefs = document.getCorefsByTerm(term);

            if (termBegin > index) {
                final List<Coref> sameCorefs = Lists.newArrayList(lastCorefs);
                sameCorefs.retainAll(termCorefs);
                out.append(sameCorefs.isEmpty() ? " " : "<span class=\"txt_coref\"> </span>");
            }

            if (mentionType == null) {
                final List<Predicate> predicates = document.getPredicatesByTerm(term);
                if (!predicates.isEmpty()) {
                    mentionTerms = predicates.get(0).getTerms();
                    mentionType = "evn";
                } else {
                    final List<Entity> entities = document.getEntitiesByTerm(term);
                    if (!entities.isEmpty()) {
                        mentionTerms = entities.get(0).getTerms();
                        mentionType = Objects.firstNonNull(entities.get(0).getType(), "misc")
                                .toLowerCase();
                    }
                }
                if (mentionType != null && term == mentionTerms.get(0)) {
                    out.append("<span class=\"txt_bg_").append(mentionType).append("\">");
                }
            }

            out.append("<span class=\"txt_term_tip");
            for (final Coref coref : termCorefs) {
                if (coref.getSpans().size() > 1) {
                    out.append(" txt_coref");
                    break;
                }
            }
            out.append("\" title=\"");
            emitTermTooltip(out, document, term);
            out.append("\">");
            out.append(term.getForm());
            out.append("</span>");

            if (mentionType != null && term == mentionTerms.get(mentionTerms.size() - 1)) {
                out.append("</span>");
                mentionType = null;
            }

            index = termEnd;
            lastCorefs = termCorefs;
        }

        if (mentionType != null) {
            out.append("</span>");
        }
    }

    public void renderOpinions(final Appendable out, final KAFDocument document,
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

    public void renderParsing(final Appendable out, final KAFDocument document,
            @Nullable final Model model, final int sentence) throws IOException {
        new ParsingRenderer(out, document, model, sentence).render(true, true, true);
    }

    private void emitTermTooltip(final Appendable out, final KAFDocument document, final Term term)
            throws IOException {

        // Emit basic term-level information: ID, POS
        out.append("<strong>Term ").append(term.getId()).append("</strong>");
        if (term.getPos() != null && term.getMorphofeat() != null) {
            out.append(": pos ").append(term.getPos()).append('/').append(term.getMorphofeat());
        }

        // Emit detailed term-level information: lemma, dep tree link, sst, synset, bbn, sumo
        if (term.getLemma() != null) {
            out.append(", lemma '").append(term.getLemma().replace("\"", "&quot;")).append("'");
        }

        final Dep dep = document.getDepToTerm(term);
        if (dep != null) {
            out.append(", ").append(dep.getRfunc()).append(" of '")
                    .append(dep.getFrom().getForm().replace("\"", "&quot;")).append("' (")
                    .append(dep.getFrom().getId()).append(")");
        }
        final ExternalRef sstRef = NAFUtils.getRef(term, NAFUtils.RESOURCE_WN_SST, null);
        if (sstRef != null) {
            out.append(", sst ").append(sstRef.getReference());
        }
        final ExternalRef synsetRef = NAFUtils.getRef(term, NAFUtils.RESOURCE_WN_SYNSET, null);
        if (synsetRef != null) {
            out.append(", wn ").append(synsetRef.getReference());
        }
        final ExternalRef bbnRef = NAFUtils.getRef(term, NAFUtils.RESOURCE_BBN, null);
        if (bbnRef != null) {
            out.append(", bbn ").append(bbnRef.getReference());
        }
        final List<ExternalRef> sumoRefs = NAFUtils.getRefs(term, NAFUtils.RESOURCE_SUMO, null);
        final List<ExternalRef> yagoRefs = NAFUtils.getRefs(term, NAFUtils.RESOURCE_YAGO, null);
        if (!sumoRefs.isEmpty() || !yagoRefs.isEmpty()) {
            out.append(", sense");
            for (final ExternalRef sumoRef : sumoRefs) {
                out.append(" sumo:").append(sumoRef.getReference());
            }
            for (final ExternalRef yagoRef : yagoRefs) {
                out.append(" yago:").append(yagoRef.getReference());
            }
        }

        // Emit predicate info, if available
        final List<Predicate> predicates = document.getPredicatesByTerm(term);
        if (!predicates.isEmpty()) {
            final Predicate predicate = predicates.get(0);
            out.append("<br/><b>Predicate ").append(predicate.getId()).append("</b>: sense ");
            final boolean isNoun = term.getPos().toUpperCase().equals("N");
            for (final ExternalRef ref : predicate.getExternalRefs()) {
                final String resource = ref.getResource().toLowerCase();
                if ("propbank".equals(resource) && !isNoun || "nombank".equals(resource) && isNoun) {
                    out.append(ref.getReference());
                    break;
                }
            }
        }

        // Emit entity info, if available
        final List<Entity> entities = document.getEntitiesByTerm(term);
        if (!entities.isEmpty()) {
            final Entity entity = entities.get(0);
            out.append("<br/><b>Entity ").append(entity.getId()).append("</b>: type ")
                    .append(entity.getType());
            String separator = ", sense ";
            for (final ExternalRef ref : entity.getExternalRefs()) {
                out.append(separator);
                try {
                    out.append(format(new URIImpl(ref.getReference())));
                } catch (final Throwable ex) {
                    out.append(ref.getReference());
                }
                separator = " ";
            }
        }

        // Emit coref info, if available and enabled
        for (final Coref coref : document.getCorefsByTerm(term)) {
            if (coref.getSpans().size() > 1) {
                out.append("<br/><b>Coref ").append(coref.getId()).append("</b>: ");
                String separator = "";
                for (final Span<Term> span : coref.getSpans()) {
                    out.append(separator);
                    out.append(span.getTargets().get(0).getId());
                    out.append(" '").append(span.getStr()).append("'");
                    separator = ", ";
                }
            }
        }
    }

    public void renderProperties(final Appendable out, final Model model, final Resource node,
            final boolean emitID, final URI... excludedProperties) throws IOException {

        final Set<Resource> seen = Sets.newHashSet(node);
        renderPropertiesHelper(out, model, node, emitID, seen,
                ImmutableSet.copyOf(excludedProperties));
    }

    private void renderPropertiesHelper(final Appendable out, final Model model,
            final Resource node, final boolean emitID, final Set<Resource> seen,
            final Set<URI> excludedProperties) throws IOException {

        // Open properties table
        out.append("<table class=\"properties table table-condensed\">\n<tbody>\n");

        // Emit a raw for the node ID, if requested
        if (emitID) {
            out.append("<tr><td><a>ID</a>:</td><td>");
            renderObject(out, node, model);
            out.append("</td></tr>\n");
        }

        // Emit other properties
        for (final URI pred : this.valueComparator.sortedCopy(model.filter(node, null, null)
                .predicates())) {
            if (excludedProperties.contains(pred)) {
                continue;
            }
            out.append("<tr><td>");
            renderObject(out, pred, model);
            out.append(":</td><td>");
            final List<Resource> nested = Lists.newArrayList();
            String separator = "";
            for (final Value obj : this.valueComparator.sortedCopy(model.filter(node, pred, null)
                    .objects())) {
                if (obj instanceof Literal || model.filter((Resource) obj, null, null).isEmpty()) {
                    out.append(separator);
                    renderObject(out, obj, model);
                    separator = ", ";
                } else {
                    nested.add((Resource) obj);
                }
            }
            out.append("".equals(separator) ? "" : "<br/>");
            for (final Resource obj : nested) {
                out.append(separator);
                if (seen.add(obj)) {
                    renderPropertiesHelper(out, model, obj, true, seen, excludedProperties);
                } else {
                    renderObject(out, obj, model);
                }
            }
            out.append("</td></tr>\n");
        }

        // Close properties table
        out.append("</tbody>\n</table>\n");
    }

    public void renderTriplesTable(final Appendable out, final Model model) throws IOException {

        // TODO: confidence?

        out.append("<table class=\"table table-condensed datatable\">\n<thead>\n");
        out.append("<tr><th width='25%' class='col-ts'>");
        out.append(shorten(RDF.SUBJECT));
        out.append("</th><th width='25%' class='col-tp'>");
        out.append(shorten(RDF.PREDICATE));
        out.append("</th><th width='25%' class='col-to'>");
        out.append(shorten(RDF.OBJECT));
        out.append("</th><th width='25%' class='col-te'>");
        out.append(shorten(KS.EXPRESSED_BY));
        out.append("</th></tr>\n");
        out.append("</thead>\n<tbody>\n");

        for (final Statement statement : this.statementComparator.sortedCopy(model)) {
            if (statement.getContext() != null) {
                out.append("<tr><td>");
                renderObject(out, statement.getSubject(), model);
                out.append("</td><td>");
                renderObject(out, statement.getPredicate(), model);
                out.append("</td><td>");
                renderObject(out, statement.getObject(), model);
                out.append("</td><td>");
                String separator = "";
                for (final Value mentionID : model.filter(statement.getContext(), KS.EXPRESSED_BY,
                        null).objects()) {
                    final String extent = model.filter((Resource) mentionID, NIF.ANCHOR_OF, null)
                            .objectLiteral().stringValue();
                    out.append(separator);
                    renderObject(out, mentionID, model);
                    out.append(" '").append(escape(extent)).append("'");
                    separator = "<br/>";
                }
                out.append("</ol></td></tr>\n");
            }
        }

        out.append("</tbody>\n</table>\n");
    }

    public void renderMentionsTable(final Appendable out, final Model model) throws IOException {

        out.append("<table class=\"table table-condensed datatable\">\n<thead>\n");
        out.append("<tr><th width='12%' class='col-mi'>id</th><th width='18%' class='col-ma'>");
        out.append(shorten(NIF.ANCHOR_OF));
        out.append("</th><th width='11%' class='col-mt'>");
        out.append(shorten(RDF.TYPE));
        out.append("</th><th width='18%' class='col-mo'>mention attributes</th><th width='11%' class='col-md'>");
        out.append(shorten(GAF.DENOTED_BY)).append("<sup>-1</sup>");
        out.append("</th><th width='30%' class='col-me'>");
        out.append(shorten(KS.EXPRESSED_BY)).append("<sup>-1</sup>");
        out.append("</th></tr>\n</thead>\n<tbody>\n");

        for (final Resource mentionID : this.valueComparator.sortedCopy(ModelUtil
                .getMentions(model))) {
            out.append("<tr><td>");
            renderObject(out, mentionID, model);
            out.append("</td><td>");
            out.append(model.filter(mentionID, NIF.ANCHOR_OF, null).objectString());
            out.append("</td><td>");
            renderObject(out, model.filter(mentionID, RDF.TYPE, null).objects(), model);
            out.append("</td><td>");
            final Model mentionModel = new LinkedHashModel();
            for (final Statement statement : model.filter(mentionID, null, null)) {
                final URI pred = statement.getPredicate();
                if (!NIF.BEGIN_INDEX.equals(pred) && !NIF.END_INDEX.equals(pred)
                        && !NIF.ANCHOR_OF.equals(pred) && !RDF.TYPE.equals(pred)
                        && !KS.MENTION_OF.equals(pred)) {
                    mentionModel.add(statement);
                }
            }
            if (!mentionModel.isEmpty()) {
                renderProperties(out, mentionModel, mentionID, false);
            }
            out.append("</td><td>");
            renderObject(out, model.filter(null, GAF.DENOTED_BY, mentionID).subjects(), model);
            out.append("</td><td><ol>");
            for (final Resource factID : model.filter(null, KS.EXPRESSED_BY, mentionID).subjects()) {
                for (final Statement statement : model.filter(null, null, null, factID)) {
                    out.append("<li>");
                    renderObject(out, statement.getSubject(), model);
                    out.append(", ");
                    renderObject(out, statement.getPredicate(), model);
                    out.append(", ");
                    renderObject(out, statement.getObject(), model);
                    out.append("</li>");
                }
            }
            out.append("</ol></td></tr>\n");
        }

        out.append("</tbody>\n</table>\n");
    }

    public void renderObject(final Appendable out, final Object object, @Nullable final Model model)
            throws IOException {

        if (object instanceof URI) {
            final URI uri = (URI) object;
            out.append("<a>").append(shorten(uri)).append("</a>");

        } else if (object instanceof Literal) {
            final Literal literal = (Literal) object;
            out.append("<span");
            if (literal.getLanguage() != null) {
                out.append(" title=\"@").append(literal.getLanguage()).append("\"");
            } else if (literal.getDatatype() != null) {
                out.append(" title=\"").append(shorten(literal.getDatatype())).append("\"");
            }
            out.append(">").append(literal.stringValue()).append("</span>");

        } else if (object instanceof BNode) {
            final BNode bnode = (BNode) object;
            out.append("_:").append(bnode.getID());

        } else if (object instanceof Iterable<?>) {
            String separator = "";
            for (final Object element : (Iterable<?>) object) {
                out.append(separator);
                renderObject(out, element, model);
                separator = "<br/>";
            }

        } else if (object != null) {
            out.append(object.toString());
        }
    }

    private static String select(final Map<Object, String> map,
            final Iterable<? extends Value> keys, final String defaultColor) {
        String color = null;
        for (final Value key : keys) {
            if (key instanceof URI) {
                final String mappedColor = map.get(key);
                if (mappedColor != null) {
                    if (color == null) {
                        color = mappedColor;
                    } else {
                        break;
                    }
                }
            }
        }
        return color != null ? color : defaultColor;
    }

    private List<String> format(final Iterable<? extends Value> values) {
        final List<String> result = Lists.newArrayList();
        for (final Value value : values) {
            result.add(format(value));
        }
        return result;
    }

    private String format(final Value value) {
        if (value instanceof URI) {
            final URI uri = (URI) value;
            final String ns = uri.getNamespace();
            final String prefix = Namespaces.DEFAULT.prefixFor(ns);
            if (prefix == null) {
                return escape("<.." + ns.charAt(ns.length() - 1) + uri.getLocalName() + ">");
            } else {
                return prefix + ":" + escape(uri.getLocalName());
            }
        }
        return escape(Statements.formatValue(value, Namespaces.DEFAULT));
    }

    private String escape(final String string) {
        return HtmlEscapers.htmlEscaper().escape(string);
    }

    @Nullable
    private String shorten(@Nullable final URI uri) {
        if (uri == null) {
            return null;
        }
        final String prefix = Namespaces.DEFAULT.prefixFor(uri.getNamespace());
        if (prefix != null) {
            return prefix + ':' + uri.getLocalName();
        }
        return "&lt;../" + uri.getLocalName() + "&gt;";
        // final int index = uri.stringValue().lastIndexOf('/');
        // if (index >= 0) {
        // return "&lt;.." + uri.stringValue().substring(index) + "&gt;";
        // }
        // return "&lt;" + uri.stringValue() + "&gt;";
    }

    private String hash(final Value value) {
        final StringBuilder builder = new StringBuilder();
        if (value instanceof URI) {
            builder.append((char) 1);
            builder.append(value.stringValue());
        } else if (value instanceof BNode) {
            builder.append((char) 2);
            builder.append(((BNode) value).getID());
        } else if (value instanceof Literal) {
            final Literal literal = (Literal) value;
            builder.append((char) 3);
            builder.append(literal.getLabel());
            if (literal.getLanguage() != null) {
                builder.append((char) 4);
                builder.append(literal.getLanguage());
            } else if (literal.getDatatype() != null) {
                builder.append((char) 5);
                builder.append(literal.getDatatype().stringValue());
            }
        }
        return "N" + Hash.murmur3(builder.toString()).toString().replace('-', 'x');
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private Iterable<? extends URI> nodeTypes;

        @Nullable
        private Iterable<? extends String> nodeNamespaces;

        @Nullable
        private Iterable<? extends String> rankedNamespaces;

        @Nullable
        private URI denotedByProperty;

        @Nullable
        private Map<Object, String> colorMap;

        @Nullable
        private Map<Object, String> styleMap;

        @Nullable
        private Mustache template;

        private final Map<String, Object> templateParameters;

        Builder() {
            this.templateParameters = Maps.newHashMap();
        }

        public Builder withProperties(final Map<?, ?> properties, @Nullable final String prefix) {
            final String p = prefix == null ? "" : prefix.endsWith(".") ? prefix : prefix + ".";
            for (final Map.Entry<?, ?> entry : properties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null
                        && entry.getKey().toString().startsWith(p)) {
                    final String name = entry.getKey().toString().substring(p.length());
                    final String value = Strings.emptyToNull(entry.getValue().toString());
                    if ("template".equals(name)) {
                        withTemplate(value);
                    } else if (name.startsWith("template.")) {
                        withTemplateParameter(name.substring("template.".length()), value);
                    }
                }
            }
            return this;
        }

        public Builder withNodeTypes(@Nullable final Iterable<? extends URI> nodeTypes) {
            this.nodeTypes = nodeTypes;
            return this;
        }

        public Builder withNodeNamespaces(@Nullable final Iterable<? extends String> nodeNamespaces) {
            this.nodeNamespaces = nodeNamespaces;
            return this;
        }

        public Builder withRankedNamespaces(
                @Nullable final Iterable<? extends String> rankedNamespaces) {
            this.rankedNamespaces = rankedNamespaces;
            return this;
        }

        public Builder withDenotedByProperty(@Nullable final URI denotedByProperty) {
            this.denotedByProperty = denotedByProperty;
            return this;
        }

        public Builder withColorMap(@Nullable final Map<Object, String> colorMap) {
            this.colorMap = colorMap;
            return this;
        }

        public Builder withStyleMap(@Nullable final Map<Object, String> styleMap) {
            this.styleMap = styleMap;
            return this;
        }

        public Builder withTemplate(@Nullable final Object template) {
            this.template = template == null ? null : loadTemplate(template);
            return this;
        }

        public Builder withTemplateParameter(final String name, @Nullable final Object value) {
            this.templateParameters.put(name, value);
            return this;
        }

        public Renderer build() {
            return new Renderer(this);
        }

    }

    public static enum Algorithm {

        DOT,

        NEATO,

        FDP,

        SFDP,

        TWOPI,

        CIRCO

    }

    private static class ValueOrdering extends Ordering<Value> {

        private final List<String> rankedNamespaces;

        public ValueOrdering(@Nullable final Iterable<? extends String> rankedNamespaces) {
            this.rankedNamespaces = rankedNamespaces == null ? ImmutableList.of() : ImmutableList
                    .copyOf(rankedNamespaces);
        }

        @Override
        public int compare(final Value v1, final Value v2) {
            if (v1 instanceof URI) {
                if (v2 instanceof URI) {
                    final int rank1 = this.rankedNamespaces.indexOf(((URI) v1).getNamespace());
                    final int rank2 = this.rankedNamespaces.indexOf(((URI) v2).getNamespace());
                    if (rank1 >= 0 && (rank1 < rank2 || rank2 < 0)) {
                        return -1;
                    } else if (rank2 >= 0 && (rank2 < rank1 || rank1 < 0)) {
                        return 1;
                    }
                    final String string1 = Statements.formatValue(v1, Namespaces.DEFAULT);
                    final String string2 = Statements.formatValue(v2, Namespaces.DEFAULT);
                    return string1.compareTo(string2);
                } else {
                    return -1;
                }
            } else if (v1 instanceof BNode) {
                if (v2 instanceof BNode) {
                    return ((BNode) v1).getID().compareTo(((BNode) v2).getID());
                } else if (v2 instanceof URI) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (v1 instanceof Literal) {
                if (v2 instanceof Literal) {
                    return ((Literal) v1).getLabel().compareTo(((Literal) v2).getLabel());
                } else if (v2 instanceof Resource) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                if (v1 == v2) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }

    }

    private final class Renderable implements Callable<String> {

        static final int SENTENCE_TEXT = 0;

        static final int SENTENCE_PARSING = 1;

        static final int SENTENCE_GRAPH = 2;

        static final int METADATA = 3;

        static final int MENTIONS = 4;

        static final int TRIPLES = 5;

        static final int GRAPH = 6;

        static final int NAF = 7;

        private final KAFDocument document;

        private final Model model;

        private final int sentenceID;

        @Nullable
        private final long[] times;

        private final int type;

        private Renderable(final KAFDocument document, final Model model, final int sentenceID,
                @Nullable final long[] times, final int type) {
            this.document = document;
            this.model = model;
            this.sentenceID = sentenceID;
            this.times = times;
            this.type = type;
        }

        @Override
        public String call() throws Exception {
            final long ts = System.currentTimeMillis();
            try {
                if (this.type == NAF) {
                    return this.document.toString();
                } else {
                    final StringBuilder builder = new StringBuilder(128 * 1024);
                    if (this.type == SENTENCE_TEXT) {
                        renderText(builder, this.document,
                                this.document.getTermsBySent(this.sentenceID), this.model);
                    } else if (this.type == SENTENCE_PARSING) {
                        renderParsing(builder, this.document, this.model, this.sentenceID);
                    } else if (this.type == SENTENCE_GRAPH) {
                        int begin = Integer.MAX_VALUE;
                        int end = Integer.MIN_VALUE;
                        for (final Term term : this.document.getSentenceTerms(this.sentenceID)) {
                            begin = Math.min(begin, NAFUtils.getBegin(term));
                            end = Math.max(end, NAFUtils.getEnd(term));
                        }
                        final Model sentenceModel = ModelUtil.getSubModel(this.model,
                                ModelUtil.getMentions(this.model, begin, end));
                        renderGraph(builder, sentenceModel, Algorithm.NEATO);
                    } else if (this.type == METADATA) {
                        renderProperties(builder, this.model,
                                new URIImpl(this.document.getPublic().uri), true, KS.HAS_MENTION);
                    } else if (this.type == MENTIONS) {
                        renderMentionsTable(builder, this.model);
                    } else if (this.type == TRIPLES) {
                        renderTriplesTable(builder, this.model);
                    } else if (this.type == GRAPH) {
                        renderGraph(builder, this.model, Algorithm.NEATO);
                    } else {
                        throw new Error("Unexpected rendering type " + this.type);
                    }
                    return builder.toString();
                }
            } catch (final Throwable ex) {
                LOGGER.error("Renderable task failed", ex);
                throw ex;
            } finally {
                if (this.times != null) {
                    synchronized (this.times) {
                        this.times[this.type] += System.currentTimeMillis() - ts;
                    }
                }
            }
        }

    }

    private final class ParsingRenderer {

        private final Appendable out;

        private final KAFDocument document;

        private final Model model;

        private final int sentence;

        private final List<Term> terms;

        private final List<Dep> deps;

        private final Map<Term, Integer> indexes;

        ParsingRenderer(final Appendable out, final KAFDocument document, final Model model,
                final int sentence) {
            this.out = out;
            this.document = document;
            this.model = model;
            this.sentence = sentence;
            this.terms = document.getTermsBySent(sentence);
            this.deps = Lists.newArrayListWithCapacity(this.terms.size());
            this.indexes = Maps.newIdentityHashMap();
            for (int index = 0; index < this.terms.size(); ++index) {
                final Term term = this.terms.get(index);
                this.deps.add(document.getDepToTerm(term));
                this.indexes.put(term, index);
            }
        }

        void render(final boolean emitDependencies, final boolean emitMentions,
                final boolean emitSRL) throws IOException {

            this.out.append("<table class=\"txt\" cellspacing=\"0\" cellpadding=\"0\">\n");

            if (emitDependencies) {
                renderDependencies();
            }

            renderTerms(emitMentions);

            if (emitSRL) {
                renderSRL();
            }

            this.out.append("</table>\n");
        }

        private void renderDependencies() throws IOException {

            // every term is mapped to 4 consecutive horizontal cells
            // - line between cells 0 and 1 (left vertical) is used for outgoing leftward edges
            // - line between cells 1 and 2 (center vertical) is used for incoming edge
            // - line between cells 2 and 3 (right vertical) is used for outgoing rightward edges
            // three arrays are used to control the rendering of these lines; they are initialized
            // to consider the dependency roots, then they are progressively filled
            final boolean[] leftVerticalLines = new boolean[this.terms.size()];
            final boolean[] centerVerticalLines = new boolean[this.terms.size()];
            final boolean[] rightVerticalLines = new boolean[this.terms.size()];
            for (int i = 0; i < this.terms.size(); ++i) {
                if (this.deps.get(i) == null) {
                    centerVerticalLines[this.indexes.get(this.terms.get(i))] = true;
                }
            }

            // allocate the dependency arcs to table rows and render each of them
            final List<List<Term>> rows = computeDependencyRows();
            for (int j = 0; j < rows.size(); ++j) {
                final List<Term> row = rows.get(j);

                // open the table row
                this.out.append("<tr class=\"txt_dep\">\n");

                // label array: i-th element contains label of edge from/to element i (with other
                // endpoint j > i)
                final String[] labels = new String[this.terms.size()];

                // update arrays for labels and vertical lines
                for (final Term term : row) {
                    final int termIndex = this.indexes.get(term);
                    final Dep termDep = this.deps.get(termIndex);
                    final Term parent = termDep == null ? term : termDep.getFrom();
                    final int parentIndex = this.indexes.get(parent);
                    final String label = termDep == null ? "" : termDep.getRfunc().toLowerCase();
                    centerVerticalLines[termIndex] = true;
                    if (termIndex < parentIndex) { // term <-- parent (right to left)
                        leftVerticalLines[parentIndex] = true;
                        labels[termIndex] = label;
                    } else if (termIndex > parentIndex) { // parent --> term (left to right)
                        rightVerticalLines[parentIndex] = true;
                        labels[parentIndex] = label;
                    }
                }

                // generate the table row, by emitting TDs (spanning multiple cells) each
                // corresponding to a blank space or to an horizontal, labelled dep edge
                String label = null;
                boolean arrow = false;
                int start = 0;
                int end = 0;
                for (int i = 0; i < this.terms.size(); ++i) {
                    ++end;
                    if (leftVerticalLines[i]) {
                        renderDependencyCell(start, end, label, arrow);
                        start = end;
                        label = null;
                        arrow = false;
                    }
                    ++end;
                    if (centerVerticalLines[i]) {
                        renderDependencyCell(start, end, label, arrow);
                        start = end;
                        label = rightVerticalLines[i] ? null : labels[i];
                        arrow = j == rows.size() - 1;
                    }
                    ++end;
                    if (rightVerticalLines[i]) {
                        renderDependencyCell(start, end, label, arrow);
                        start = end;
                        label = labels[i];
                        arrow = false;
                    }
                    ++end;
                }
                renderDependencyCell(start, end, null, arrow); // emit remaining blank TD

                // close the table row
                this.out.append("</tr>\n");
            }

            // emit a final row to extend the vertical edges departing from terms
            this.out.append("<tr>\n");
            for (int i = 0; i < this.terms.size(); ++i) {
                final boolean left = leftVerticalLines[i];
                final boolean right = rightVerticalLines[i];
                this.out.append("<td class=\"txt_dep_co").append(left ? " rb" : "")
                        .append("\"></td>");
                this.out.append("<td class=\"txt_dep_ci").append(left ? " lb" : "")
                        .append("\"></td>");
                this.out.append("<td class=\"txt_dep_ci").append(right ? " rb" : "")
                        .append("\"></td>");
                this.out.append("<td class=\"txt_dep_co").append(right ? " lb" : "")
                        .append("\"></td>\n");
            }
            this.out.append("</tr>\n");
        }

        private void renderDependencyCell(final int from, final int to, final String label,
                final boolean arrow) throws IOException {

            // open table cell
            this.out.append("<td class=\"");

            // emit CSS classes for left, right and top borders
            String separator = "";
            if (from != 0) {
                this.out.append(separator).append("txt_lb");
                separator = " ";
            }
            if (to != 4 * this.terms.size()) {
                this.out.append(separator).append("txt_rb");
                separator = " ";
            }
            if (label != null) {
                this.out.append(separator).append("txt_tb");
            }
            this.out.append("\"");

            // emit colspan attribute to control the length of the arc
            if (to - from > 1) {
                this.out.append(" colspan=\"").append(Integer.toString(to - from)).append("\"");
            }

            // emit the cell content (i.e., the dependency labels, if any)
            this.out.append("><div><span>") //
                    .append(label != null ? label : "&nbsp;") //
                    .append("</span></div>");

            // emit the <div> displaying the downward arrow, if requested
            if (arrow) {
                this.out.append("<div class=\"txt_ab\"></div>");
            }

            // close table cell
            this.out.append("</td>\n");
        }

        private List<List<Term>> computeDependencyRows() {

            // allocate a table for the result
            final List<List<Term>> rows = Lists.newArrayList();

            // start with all the terms, pick up the ones of the first row and then drop them and
            // repeat for the second row and so on, until all terms (=edges) have been considered
            final Set<Term> remaining = Sets.newHashSet(this.terms);
            while (!remaining.isEmpty()) {
                final List<Term> candidates = ImmutableList.copyOf(remaining);
                final List<Term> row = Lists.newArrayList();
                rows.add(row);

                // consider each candidate term for inclusion in the row
                for (final Term t1 : candidates) {

                    // retrieve dep parent, start / end indexes of the dep edge t1
                    final int s1 = this.indexes.get(t1);
                    final Dep dep1 = this.deps.get(s1);
                    final Term p1 = dep1 == null ? t1 : dep1.getFrom();
                    final int e1 = this.indexes.get(p1);

                    // can emit t1 only if its edge does not contain (horizontally) the edge of
                    // another candidate (in which case we pick up the other candidate)
                    boolean canEmit = true;
                    for (final Term t2 : candidates) {
                        if (t2 != t1) { // don't compare t1 with itself

                            // Retrieve dep parent, start / end indexes of dep edge t2
                            final int s2 = this.indexes.get(t2);
                            final Dep dep2 = this.deps.get(s2);
                            final Term p2 = dep2 == null ? t2 : dep2.getFrom();
                            final int e2 = this.indexes.get(p2);

                            // Compare t1 and t2. If t1 would contain t2 (in the graph) drop it
                            if (Math.min(s1, e1) <= Math.min(s2, e2)
                                    && Math.max(s1, e1) >= Math.max(s2, e2)) {
                                canEmit = false;
                                break;
                            }
                        }
                    }

                    // emit t1 iff it satisfied all the tests. Do not consider it anymore
                    if (canEmit) {
                        row.add(t1);
                        remaining.remove(t1);
                    }
                }
            }

            // add an initial empty row with no dep edges (will only contain the vertical line to
            // the dep root)
            rows.add(ImmutableList.<Term>of());

            // reverse the row so that the first one is the first to be emitted in the table
            Collections.reverse(rows);
            return rows;
        }

        private void renderTerms(final boolean emitMentions) throws IOException {

            final Markable[] markables = emitMentions ? Markable.extract(this.terms, this.model,
                    Renderer.this.colorMap) : new Markable[this.terms.size()];

            // open the TR row
            this.out.append("<tr class=\"txt_terms\">\n");

            // emit the TD cells for each term, possibly adding entity / predicate highlighting
            for (int i = 0; i < this.terms.size(); ++i) {
                final Term term = this.terms.get(i);
                this.out.append("<td colspan=\"4\"><div class=\"");
                final Markable markable = markables[i];
                if (markable == null) {
                    this.out.append("txt_term_c\">");
                } else {
                    final boolean start = i == 0 || markable != markables[i - 1];
                    final boolean end = i == this.terms.size() - 1 || markable != markables[i + 1];
                    this.out.append(start ? end ? "txt_term_lcr" : "txt_term_lc"
                            : end ? "txt_term_cr" : "txt_term_c");
                    this.out.append("\" style=\"background-color: ").append(markable.color)
                            .append("\">");
                }
                this.out.append("<span class=\"txt_term_tip\" title=\"");
                emitTermTooltip(this.out, this.document, term);
                this.out.append("\">").append(term.getForm().replace(' ', '_')).append("</span>");
                this.out.append("</div></td>\n");
            }

            // close the TR row
            this.out.append("</tr>\n");
        }

        private void renderSRL() throws IOException {

            // retrieve all the predicate in the sentence
            // final List<Predicate> predicates =
            // this.document.getPredicatesBySent(this.sentence);

            // retrieve all the SRL proposition in the sentence
            final List<SRLElement> propositions = Lists.newArrayList();
            for (final Predicate predicate : this.document.getPredicatesBySent(this.sentence)) {
                propositions.add(new SRLElement(null, predicate, true));
            }

            // allocate propositions to 'proposition' rows, each one reporting one or more
            // predicates starting from the ones with smallest extent
            for (final List<SRLElement> propositionRow : computeSRLRows(propositions)) {

                // emit a blank TR to visually separate predicate rows
                this.out.append("<tr class=\"txt_empty\"><td").append(" colspan=\"")
                        .append(Integer.toString(4 * this.terms.size())).append("\"")
                        .append("></td></tr>\n");

                // extract all the markables (Predicate/Role) allocated to the row
                final List<SRLElement> markables = Lists.newArrayList();
                for (final SRLElement proposition : propositionRow) {
                    final Predicate predicate = (Predicate) proposition.element;
                    markables.add(new SRLElement(proposition, predicate, false));
                    for (final Role role : predicate.getRoles()) {
                        markables.add(new SRLElement(proposition, role, false));
                    }
                }

                // allocate the markables to concrete TR 'markable' rows, to account for markables
                // containing one each other; emit these rows one at a time
                for (final List<SRLElement> markableRow : computeSRLRows(markables)) {

                    // open the TR row
                    this.out.append("<tr class=\"txt_srl\">\n");

                    // determine which cells in the TR row must have a left/right vertical line;
                    // this is done w.r.t. the subset of predicates rendered in the TR row
                    final boolean[] leftBorders = new boolean[this.terms.size()];
                    final boolean[] rightBorders = new boolean[this.terms.size()];
                    for (final SRLElement markable : markableRow) {
                        final SRLElement proposition = markable.parent;
                        final int s = this.indexes.get(proposition.terms.get(0));
                        final int e = this.indexes.get(proposition.terms.get(proposition.terms
                                .size() - 1));
                        leftBorders[s] = true;
                        rightBorders[e] = true;
                        if (s > 0) {
                            rightBorders[s - 1] = true;
                        }
                        if (e < this.terms.size() - 1) {
                            leftBorders[e + 1] = true;
                        }
                    }

                    // associate each term=cell to the markable it possibly represent
                    final SRLElement[] cells = new SRLElement[this.terms.size()];
                    for (final SRLElement markable : markableRow) {
                        for (final Term term : markable.terms) {
                            cells[this.indexes.get(term)] = markable;
                        }
                    }

                    // emit the cells of the TR row, each one being blank or corresponding to a
                    // predicate or argument; this is done by scanning terms from left to right
                    int start = 0;
                    int end = start + 1;
                    while (start < this.terms.size()) {

                        // determine where to end the current TD cell
                        final SRLElement markable = cells[start];
                        while (end < this.terms.size() && cells[end] == markable
                                && !leftBorders[end]) {
                            ++end;
                        }

                        // open the TD cell, emitting CSS classes for left/right borders
                        final boolean lb = leftBorders[start]; // left border
                        final boolean rb = rightBorders[end - 1]; // right border
                        this.out.append("<td colspan=\"")
                                .append(Integer.toString(4 * (end - start)))
                                .append("\"")
                                .append(lb ? rb ? "class=\"txt_lb txt_rb\"" : "class=\"txt_lb\""
                                        : rb ? "class=\"txt_rb\"" : "") //
                                .append(">");

                        // emit the predicate/argument for the current cell, if any
                        if (markable != null) {
                            this.out.append("<div>");
                            final Object element = markable.element;
                            if (element instanceof Predicate) {
                                final String rolesetID = NAFUtils.getRoleset((Predicate) element);
                                if (rolesetID != null) {
                                    this.out.append(rolesetID);
                                }
                            } else {
                                this.out.append(((Role) element).getSemRole());
                            }
                            this.out.append("</div>");
                        }

                        // close the TD cell
                        this.out.append("</td>\n");

                        // update start/end indexes
                        start = end;
                        ++end;
                    }

                    // close the TR row
                    this.out.append("</tr>\n");
                }
            }
        }

        private List<List<SRLElement>> computeSRLRows(final Iterable<SRLElement> elements) {

            // allocate the resulting row list
            final List<List<SRLElement>> rows = Lists.newArrayList();

            // select a non-overlapping subset of supplied elements to form the first row, then
            // discard them and repeat to form next rows, until all elements have been allocated
            final Set<SRLElement> remaining = Sets.newHashSet(elements);
            while (!remaining.isEmpty()) {

                // allocate a new table row
                final List<SRLElement> row = Lists.newArrayList();
                rows.add(row);

                // rank the remaining elements in order of increasing (# terms) length
                final List<SRLElement> ranking = Ordering.natural().sortedCopy(remaining);

                // try to add as many remaining elements from the ranking, ensuring there are no
                // overlappings in the added ones; we use the ranking as an heuristic that should
                // lead to a 'good' selection (no optimality guarantee, whatever optimality means)
                for (final SRLElement candidate : ranking) {

                    // check for overlapping with already chosen elements in the current rows
                    boolean canEmit = true;
                    for (final SRLElement element : row) {
                        if (candidate.overlaps(element)) {
                            canEmit = false;
                            break;
                        }
                    }

                    // add the element upon success, and do not consider it anymore
                    if (canEmit) {
                        row.add(candidate);
                        remaining.remove(candidate);
                    }
                }
            }
            return rows;
        }

    }

    private static final class SRLElement implements Comparable<SRLElement> {

        final SRLElement parent;

        final Object element;

        final List<Term> terms;

        final int begin;

        final int end;

        SRLElement(final SRLElement parent, final Object element, final boolean useProposition) {
            this.parent = parent;
            this.element = element;
            if (useProposition) {
                final Predicate predicate = (Predicate) element;
                final Set<Term> termSet = Sets.newHashSet();
                termSet.addAll(predicate.getTerms());
                for (final Role role : predicate.getRoles()) {
                    termSet.addAll(role.getTerms());
                }
                this.terms = Ordering.from(Term.OFFSET_COMPARATOR).immutableSortedCopy(termSet);
            } else if (element instanceof Predicate) {
                this.terms = ((Predicate) element).getTerms();
            } else {
                this.terms = ((Role) element).getTerms();
            }
            this.begin = NAFUtils.getBegin(this.terms.get(0));
            this.end = NAFUtils.getEnd(this.terms.get(this.terms.size() - 1));
        }

        boolean overlaps(final SRLElement other) {
            return this.end > other.begin && this.begin < other.end;
        }

        @Override
        public int compareTo(final SRLElement other) {
            int result = 0;
            if (other != this) {
                result = this.terms.size() - other.terms.size();
                if (result == 0) {
                    result = System.identityHashCode(this.element)
                            - System.identityHashCode(other.element);
                }
            }
            return result;
        }

    }

    private static final class Markable {

        final List<Term> terms;

        final String color;

        Markable(final Iterable<Term> terms, final String color) {
            this.terms = ImmutableList.copyOf(terms);
            this.color = color;
        }

        static Markable[] extract(final List<Term> terms, final Model model,
                final Map<Object, String> colorMap) {

            final int[] offsets = new int[terms.size()];
            for (int i = 0; i < terms.size(); ++i) {
                offsets[i] = terms.get(i).getOffset();
            }

            final Markable[] markables = new Markable[offsets.length];
            for (final Statement stmt : model.filter(null, GAF.DENOTED_BY, null)) {
                final Resource instance = stmt.getSubject();
                final String color = select(colorMap, model.filter(instance, RDF.TYPE, null)
                        .objects(), null);
                if (stmt.getObject() instanceof URI && color != null) {
                    final URI mentionURI = (URI) stmt.getObject();
                    final String name = mentionURI.getLocalName();
                    if (name.indexOf(';') < 0) {
                        final int index = name.indexOf(',');
                        final int start = Integer.parseInt(name.substring(5, index));
                        final int end = Integer.parseInt(name.substring(index + 1));
                        final int s = Arrays.binarySearch(offsets, start);
                        if (s >= 0) {
                            int e = s;
                            while (e < offsets.length && offsets[e] < end) {
                                ++e;
                            }
                            final Markable markable = new Markable(ImmutableList.copyOf(terms
                                    .subList(s, e)), color);
                            for (int i = s; i < e; ++i) {
                                markables[i] = markable;
                            }
                        }
                    }
                }
            }

            return markables;
        }

    }

    static final class Runner implements Runnable {

        private final List<File> inputFiles;

        private final List<File> outputFiles;

        private final RDFGenerator generator;

        @Nullable
        private final Mustache template;

        private Runner(final List<File> inputFiles, final List<File> outputFiles,
                final RDFGenerator generator, @Nullable final String template) {
            this.inputFiles = inputFiles;
            this.outputFiles = outputFiles;
            this.generator = generator;
            this.template = template == null ? null : loadTemplate(template);
        }

        private static void addFiles(final Collection<File> inputFiles,
                final Collection<File> outputFiles, final File input, final File output,
                final String format, final boolean recursive) {
            if (input.isFile()) {
                inputFiles.add(input);
                outputFiles
                        .add(new File(output.getAbsolutePath() + "/" + input.getName() + format));
            } else {
                for (final File entry : input.listFiles()) {
                    final String name = entry.getName();
                    if (entry.isDirectory() && recursive || entry.isFile()
                            && (name.endsWith(".naf") || name.endsWith(".naf.xml"))) {
                        addFiles(inputFiles, outputFiles, entry, new File(output.getAbsolutePath()
                                + "/" + input.getName()), format, recursive);
                    }
                }
            }
        }

        static Runner create(final String name, final String... args) {

            final Options options = Options.parse(
                    "r,recursive|f,format!|t,template!|d,directory!|m,merge|n,normalize|+", args);

            final String template = options.getOptionArg("t", String.class);
            String format = options.getOptionArg("f", String.class, ".html.gz");
            format = format.startsWith(".") ? format : "." + format;
            final boolean merge = options.hasOption("m");
            final boolean normalize = options.hasOption("n");

            File outputDir = options.getOptionArg("d", File.class);
            if (outputDir == null) {
                outputDir = new File(System.getProperty("user.dir"));
            } else if (!outputDir.exists()) {
                throw new IllegalArgumentException("Directory '" + outputDir + "' does not exist");
            }

            final boolean recursive = options.hasOption("r");
            final List<File> inputFiles = Lists.newArrayList();
            final List<File> outputFiles = Lists.newArrayList();
            for (final File file : options.getPositionalArgs(File.class)) {
                if (!file.exists()) {
                    throw new IllegalArgumentException("File/directory '" + file
                            + "' does not exist");
                }
                addFiles(inputFiles, outputFiles, file, outputDir, format, recursive);
            }

            final RDFGenerator generator = RDFGenerator.builder()
                    .withProperties(Util.PROPERTIES, "eu.fbk.dkm.pikes.cmd.RDFGenerator")
                    .withMerging(merge).withNormalization(normalize).build();

            return new Runner(inputFiles, outputFiles, generator, template);
        }

        @Override
        public void run() {

            LOGGER.info("Rendering {} NAF files to HTML", this.inputFiles.size());

            final NAFFilter filter = NAFFilter.builder()
                    .withProperties(Util.PROPERTIES, "eu.fbk.dkm.pikes.cmd.NAFFilter").build();

            final Renderer renderer = Renderer.DEFAULT;

            final Tracker tracker = new Tracker(LOGGER, null, //
                    "Processed %d NAF files (%d NAF/s avg)", //
                    "Processed %d NAF files (%d NAF/s, %d NAF/s avg)");

            int succeeded = 0;
            tracker.start();
            for (int i = 0; i < this.inputFiles.size(); ++i) {
                final File inputFile = this.inputFiles.get(i);
                final File outputFile = this.outputFiles.get(i);
                LOGGER.debug("Processing {} ...", inputFile);
                MDC.put("context", inputFile.getName());
                try {
                    final KAFDocument document = KAFDocument.createFromFile(inputFile);
                    filter.filter(document);
                    final Model model = this.generator.generate(document, null);
                    Files.createParentDirs(outputFile);
                    try (Writer writer = IO.utf8Writer(IO.write(outputFile.getAbsolutePath()))) {
                        renderer.renderAll(writer, document, model, this.template, null);
                    }
                    ++succeeded;
                } catch (final Throwable ex) {
                    LOGGER.error("Processing failed for " + inputFile, ex);
                } finally {
                    MDC.remove("context");
                }
                tracker.increment();
            }
            tracker.end();

            LOGGER.info("Successfully rendered {}/{} files", succeeded, this.inputFiles.size());
        }

    }

}
