package eu.fbk.dkm.pikes.rdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
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
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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

import eu.fbk.utils.svm.Util;
import eu.fbk.dkm.pikes.rdf.vocab.*;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;

import eu.fbk.dkm.pikes.naflib.NafRenderUtils;
import eu.fbk.dkm.pikes.naflib.NafRenderUtils.Markable;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.Namespaces;
import eu.fbk.rdfpro.util.Options;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.rdfpro.util.Tracker;

public class Renderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

    public static final Set<IRI> DEFAULT_NODE_TYPES = ImmutableSet.of(KS_OLD.ENTITY, KS_OLD.ATTRIBUTE);

    public static final Set<String> DEFAULT_NODE_NAMESPACES = ImmutableSet.of();

    public static final Map<Object, String> DEFAULT_COLOR_MAP = ImmutableMap
            .<Object, String>builder() //
            .put("node", "#F0F0F0") //
            .put(NWR.PERSON, "#FFC8C8") //
            .put(NWR.ORGANIZATION, "#FFFF84") //
            .put(NWR.LOCATION, "#A9C5EB") //
            .put(KS_OLD.ATTRIBUTE, "#EEBBEE") //
            // .put(KS_OLD.MONEY, "#EEBBEE") //
            // .put(KS_OLD.FACILITY, "#FFC65B") //
            // .put(KS_OLD.PRODUCT, "#FFC65B") //
            // .put(KS_OLD.WORK_OF_ART, "#FFC65B") //
            .put(SUMO.PROCESS, "#CFE990") //
            .put(SUMO.RELATION, "#FFFFFF") //
            .put(OWLTIME.INTERVAL, "#B4D1B6") //
            .put(OWLTIME.DATE_TIME_INTERVAL, "#B4D1B6") //
            .put(OWLTIME.PROPER_INTERVAL, "#B4D1B6") //
            .put(NWR.MISC, "#D1BAA2") //
            // .put(KS_OLD.LAW, "#D1BAA2") //
            .build();

    public static final Map<Object, String> DEFAULT_STYLE_MAP = ImmutableMap.of();

    public static final Mustache DEFAULT_TEMPLATE = loadTemplate("Renderer.html");

    public static final List<String> DEFAULT_RANKED_NAMESPACES = ImmutableList.of(
            "http://framebase.org/ns/", //
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

    private final Set<IRI> nodeTypes;

    private final Set<String> nodeNamespaces;

    private final Ordering<Value> valueComparator;

    private final Ordering<Statement> statementComparator;

    private final IRI denotedByProperty;

    private final Map<Object, String> colorMap;

    private final Map<Object, String> styleMap;

    private Mustache template;

    private Map<String, ?> templateParameters;

    private Renderer(final Builder builder) {
        this.nodeTypes = builder.nodeTypes == null ? DEFAULT_NODE_TYPES //
                : ImmutableSet.copyOf(builder.nodeTypes);
        this.nodeNamespaces = builder.nodeNamespaces == null ? DEFAULT_NODE_NAMESPACES
                : ImmutableSet.copyOf(builder.nodeNamespaces);
        this.valueComparator = Ordering.from(Statements.valueComparator(Iterables.toArray(
                builder.rankedNamespaces == null ? DEFAULT_RANKED_NAMESPACES
                        : builder.rankedNamespaces, String.class)));
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
        this.denotedByProperty = MoreObjects.firstNonNull(builder.denotedByProperty,
                GAF.DENOTED_BY);
        this.colorMap = builder.colorMap == null ? DEFAULT_COLOR_MAP : ImmutableMap
                .copyOf(builder.colorMap);
        this.styleMap = builder.styleMap == null ? DEFAULT_STYLE_MAP : ImmutableMap
                .copyOf(builder.styleMap);
        this.template = MoreObjects.firstNonNull(builder.template, DEFAULT_TEMPLATE);
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

    public void renderGraph(final Appendable out, final QuadModel model, final Algorithm algorithm)
            throws IOException {
        RDFGraphvizRenderer.builder().withNodeNamespaces(this.nodeNamespaces)
                .withNodeTypes(this.nodeTypes).withValueComparator(this.valueComparator)
                .withCollapsedProperties(ImmutableSet.of(this.denotedByProperty))
                .withColorMap(this.colorMap).withStyleMap(this.styleMap)
                .withGraphvizCommand(algorithm.name().toLowerCase()).build().emitSVG(out, model);
    }

    public void renderText(final Appendable out, final KAFDocument document,
            final Iterable<Term> terms, final Model model) throws IOException {
        final List<Term> termList = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
        NafRenderUtils.renderText(out, document, terms,
                extractMarkables(termList, model, this.colorMap));
    }

    public void renderParsing(final Appendable out, final KAFDocument document,
            @Nullable final Model model, final int sentence) throws IOException {
        NafRenderUtils.renderParsing(out, document, sentence, true, true,
                extractMarkables(document.getTermsBySent(sentence), model, this.colorMap));
    }

    public void renderProperties(final Appendable out, final Model model, final Resource node,
            final boolean emitID, final IRI... excludedProperties) throws IOException {

        final Set<Resource> seen = Sets.newHashSet(node);
        renderPropertiesHelper(out, model, node, emitID, seen,
                ImmutableSet.copyOf(excludedProperties));
    }

    private void renderPropertiesHelper(final Appendable out, final Model model,
            final Resource node, final boolean emitID, final Set<Resource> seen,
            final Set<IRI> excludedProperties) throws IOException {

        // Open properties table
        out.append("<table class=\"properties table table-condensed\">\n<tbody>\n");

        // Emit a raw for the node ID, if requested
        if (emitID) {
            out.append("<tr><td><a>ID</a>:</td><td>");
            renderObject(out, node, model);
            out.append("</td></tr>\n");
        }

        // Emit other properties
        for (final IRI pred : this.valueComparator.sortedCopy(model.filter(node, null, null)
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

        out.append("<table class=\"table table-condensed datatable\">\n<thead>\n");
        out.append("<tr><th width='25%' class='col-ts'>");
        out.append(shorten(RDF.SUBJECT));
        out.append("</th><th width='25%' class='col-tp'>");
        out.append(shorten(RDF.PREDICATE));
        out.append("</th><th width='25%' class='col-to'>");
        out.append(shorten(RDF.OBJECT));
        out.append("</th><th width='25%' class='col-te'>");
        out.append(shorten(KS_OLD.EXPRESSED_BY));
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
                for (final Value mentionID : model.filter(statement.getContext(), KS_OLD.EXPRESSED_BY,
                        null).objects()) {
                    final String extent = Models.objectString(model.filter((Resource) mentionID, NIF.ANCHOR_OF, null)).get();
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
        out.append(shorten(KS_OLD.EXPRESSED_BY)).append("<sup>-1</sup>");
        out.append("</th></tr>\n</thead>\n<tbody>\n");

        for (final Resource mentionID : this.valueComparator.sortedCopy(ModelUtil
                .getMentions(QuadModel.wrap(model)))) {
            out.append("<tr><td>");
            renderObject(out, mentionID, model);
            out.append("</td><td>");
            out.append(Models.objectString(model.filter(mentionID, NIF.ANCHOR_OF, null)).get());
            out.append("</td><td>");
            renderObject(out, model.filter(mentionID, RDF.TYPE, null).objects(), model);
            out.append("</td><td>");
            final Model mentionModel = new LinkedHashModel();
            for (final Statement statement : model.filter(mentionID, null, null)) {
                final IRI pred = statement.getPredicate();
                if (!NIF.BEGIN_INDEX.equals(pred) && !NIF.END_INDEX.equals(pred)
                        && !NIF.ANCHOR_OF.equals(pred) && !RDF.TYPE.equals(pred)
                        && !KS_OLD.MENTION_OF.equals(pred)) {
                    mentionModel.add(statement);
                }
            }
            if (!mentionModel.isEmpty()) {
                renderProperties(out, mentionModel, mentionID, false);
            }
            out.append("</td><td>");
            renderObject(out, model.filter(null, GAF.DENOTED_BY, mentionID).subjects(), model);
            out.append("</td><td><ol>");
            for (final Resource factID : model.filter(null, KS_OLD.EXPRESSED_BY, mentionID).subjects()) {
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

        if (object instanceof IRI) {
            final IRI uri = (IRI) object;
            out.append("<a>").append(shorten(uri)).append("</a>");

        } else if (object instanceof Literal) {
            final Literal literal = (Literal) object;
            out.append("<span");
            if (literal.getLanguage().isPresent()) {
                out.append(" title=\"@").append(literal.getLanguage().get()).append("\"");
            } else if (!literal.getDatatype().equals(XMLSchema.STRING)) {
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

    private static List<Markable> extractMarkables(final List<Term> terms, final Model model,
            final Map<Object, String> colorMap) {

        final int[] offsets = new int[terms.size()];
        for (int i = 0; i < terms.size(); ++i) {
            offsets[i] = terms.get(i).getOffset();
        }

        final List<Markable> markables = Lists.newArrayList();
        for (final Statement stmt : model.filter(null, GAF.DENOTED_BY, null)) {
            final Resource instance = stmt.getSubject();
            final String color = select(colorMap,
                    model.filter(instance, RDF.TYPE, null).objects(), null);
            if (stmt.getObject() instanceof IRI && color != null) {
                final IRI mentionIRI = (IRI) stmt.getObject();
                final String name = mentionIRI.getLocalName();
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
                        markables.add(new Markable(ImmutableList.copyOf(terms.subList(s, e)),
                                color));
                    }
                }
            }
        }

        return markables;
    }

    private static String select(final Map<Object, String> map,
            final Iterable<? extends Value> keys, final String defaultColor) {
        String color = null;
        for (final Value key : keys) {
            if (key instanceof IRI) {
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

    private static String escape(final String string) {
        return HtmlEscapers.htmlEscaper().escape(string);
    }

    @Nullable
    private static String shorten(@Nullable final IRI uri) {
        if (uri == null) {
            return null;
        }
        final String prefix = Namespaces.DEFAULT.prefixFor(uri.getNamespace());
        if (prefix != null) {
            return prefix + ':' + uri.getLocalName();
        }
        return "&lt;../" + uri.getLocalName() + "&gt;";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private Iterable<? extends IRI> nodeTypes;

        @Nullable
        private Iterable<? extends String> nodeNamespaces;

        @Nullable
        private Iterable<? extends String> rankedNamespaces;

        @Nullable
        private IRI denotedByProperty;

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

        public Builder withNodeTypes(@Nullable final Iterable<? extends IRI> nodeTypes) {
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

        public Builder withDenotedByProperty(@Nullable final IRI denotedByProperty) {
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
                        final QuadModel sentenceModel = ModelUtil.getSubModel(
                                QuadModel.wrap(this.model),
                                ModelUtil.getMentions(QuadModel.wrap(this.model), begin, end));
                        renderGraph(builder, sentenceModel, Algorithm.NEATO);
                    } else if (this.type == METADATA) {
                        renderProperties(builder, this.model,
                                Statements.VALUE_FACTORY.createIRI(this.document.getPublic().uri), true, KS_OLD.HAS_MENTION);
                    } else if (this.type == MENTIONS) {
                        renderMentionsTable(builder, this.model);
                    } else if (this.type == TRIPLES) {
                        renderTriplesTable(builder, this.model);
                    } else if (this.type == GRAPH) {
                        renderGraph(builder, QuadModel.wrap(this.model), Algorithm.NEATO);
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
