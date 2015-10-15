package eu.fbk.dkm.pikes.rdf.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
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

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;

import eu.fbk.dkm.pikes.naflib.NafRenderUtils;
import eu.fbk.dkm.pikes.naflib.NafRenderUtils.Markable;
import eu.fbk.dkm.pikes.rdf.util.ModelUtil;
import eu.fbk.dkm.pikes.rdf.util.RDFGraphvizRenderer;
import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.dkm.utils.vocab.NWR;
import eu.fbk.dkm.utils.vocab.OWLTIME;
import eu.fbk.dkm.utils.vocab.SUMO;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Namespaces;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

final class TemplateRenderer implements Renderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

    private static final Set<URI> DEFAULT_NODE_TYPES = ImmutableSet.of(KS.INSTANCE, KS.ATTRIBUTE);

    private static final Set<String> DEFAULT_NODE_NAMESPACES = ImmutableSet
            .of("http://dbpedia.org/resource/");

    private static final Map<Object, String> DEFAULT_COLOR_MAP = ImmutableMap
            .<Object, String>builder() //
            .put("node", "#F0F0F0") //
            .put(NWR.PERSON, "#FFC8C8") //
            .put(NWR.ORGANIZATION, "#FFFF84") //
            .put(NWR.LOCATION, "#A9C5EB") //
            .put(KS.ATTRIBUTE, "#EEBBEE") //
            .put(SUMO.PROCESS, "#CFE990") //
            .put(SUMO.RELATION, "#FFFFFF") //
            .put(OWLTIME.INTERVAL, "#B4D1B6") //
            .put(OWLTIME.DATE_TIME_INTERVAL, "#B4D1B6") //
            .put(OWLTIME.PROPER_INTERVAL, "#B4D1B6") //
            .put(NWR.MISC, "#D1BAA2") //
            .build();

    private static final Map<Object, String> DEFAULT_STYLE_MAP = ImmutableMap.of();

    private static final Mustache DEFAULT_TEMPLATE = loadTemplate("Renderer.html");

    private static final List<String> DEFAULT_RANKED_NAMESPACES = ImmutableList.of(
            "http://www.newsreader-project.eu/ontologies/propbank/",
            "http://www.newsreader-project.eu/ontologies/nombank/");

    private final Ordering<Value> valueComparator;

    private final Ordering<Statement> statementComparator;

    private final Map<Object, String> colorMap;

    private final Mustache template;

    private final Map<String, ?> templateParameters;

    private final RDFGraphvizRenderer graphvizRenderer;

    private TemplateRenderer(final Builder builder) {
        this.colorMap = builder.colorMap == null ? DEFAULT_COLOR_MAP : ImmutableMap
                .copyOf(builder.colorMap);
        this.valueComparator = Ordering.from(Statements.valueComparator(Iterables.toArray(
                builder.rankedNamespaces == null ? DEFAULT_RANKED_NAMESPACES
                        : builder.rankedNamespaces, String.class)));
        this.statementComparator = Ordering.from(Statements.statementComparator("spoc",
                this.valueComparator));
        this.graphvizRenderer = RDFGraphvizRenderer
                .builder()
                .withNodeNamespaces(
                        builder.nodeNamespaces == null ? DEFAULT_NODE_NAMESPACES
                                : builder.nodeNamespaces)
                .withNodeTypes(builder.nodeTypes == null ? DEFAULT_NODE_TYPES : builder.nodeTypes)
                .withValueComparator(this.valueComparator) //
                .withColorMap(this.colorMap) //
                .withStyleMap(MoreObjects.firstNonNull(builder.styleMap, DEFAULT_STYLE_MAP)) //
                .withGraphvizCommand("neato").build();
        this.template = MoreObjects.firstNonNull(builder.template, DEFAULT_TEMPLATE);
        this.templateParameters = builder.templateParameters;
    }

    @Override
    public void render(final QuadModel model, @Nullable final Annotation annotation,
            final Appendable out) throws IOException {

        final long ts = System.currentTimeMillis();

        final URI uri = (URI) model.filter(null, RDF.TYPE, KS.TEXT).subjects().iterator().next();

        final List<Map<String, Object>> sentencesModel = Lists.newArrayList();
        // for (int i = 1; i <= doc.getNumSentences(); ++i) {
        // final int sentenceID = i;
        // final Map<String, Object> sm = Maps.newHashMap();
        // sm.put("id", i);
        // sm.put("markup", (Callable<String>) () -> {
        // return renderText(new StringBuilder(), doc, doc.getTermsBySent(sentenceID), model)
        // .toString();
        // });
        // sm.put("parsing", (Callable<String>) () -> {
        // return renderParsing(new StringBuilder(), doc, model, sentenceID).toString();
        // });
        // sm.put("graph",
        // (Callable<String>) () -> {
        // int begin = Integer.MAX_VALUE;
        // int end = Integer.MIN_VALUE;
        // for (final Term term : doc.getSentenceTerms(sentenceID)) {
        // begin = Math.min(begin, NAFUtils.getBegin(term));
        // end = Math.max(end, NAFUtils.getEnd(term));
        // }
        // final QuadModel sentenceModel = ModelUtil.getSubModel(model,
        // ModelUtil.getMentions(model, begin, end));
        // return renderGraph(new StringBuilder(), sentenceModel).toString();
        //
        // });
        // sentencesModel.add(sm);
        // }

        final Map<String, Object> documentModel = Maps.newHashMap();
        documentModel.put("title", uri.stringValue());
        documentModel.put("sentences", sentencesModel);
        documentModel.put("metadata", (Callable<String>) () -> {
            return renderProperties(new StringBuilder(), model, //
                    new URIImpl(uri.stringValue()), true).toString();
        });
        documentModel.put("mentions", (Callable<String>) () -> {
            return renderMentionsTable(new StringBuilder(), model).toString();
        });
        documentModel.put("triples", (Callable<String>) () -> {
            return renderTriplesTable(new StringBuilder(), model).toString();
        });
        documentModel.put("graph", (Callable<String>) () -> {
            return renderGraph(new StringBuilder(), model).toString();
        });
        if (annotation != null) {
            documentModel.put("naf", (Callable<String>) () -> {
                return annotation.toString();
            });
        }

        documentModel.putAll(this.templateParameters);
        if (this.templateParameters != null) {
            documentModel.putAll(this.templateParameters);
        }

        final Mustache actualTemplate = this.template != null ? loadTemplate(this.template)
                : this.template;
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
            LOGGER.debug("Done in {} ms", System.currentTimeMillis() - ts);
        }
    }

    private <T extends Appendable> T renderGraph(final T out, final QuadModel model)
            throws IOException {
        return this.graphvizRenderer.emitSVG(out, model);
    }

    private <T extends Appendable> T renderText(final T out, final KAFDocument document,
            final Iterable<Term> terms, final QuadModel model) throws IOException {
        final List<Term> termList = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
        NafRenderUtils.renderText(out, document, terms,
                extractMarkables(termList, model, this.colorMap));
        return out;
    }

    private <T extends Appendable> T renderParsing(final T out, final KAFDocument document,
            @Nullable final QuadModel model, final int sentence) throws IOException {
        NafRenderUtils.renderParsing(out, document, sentence, true, true,
                extractMarkables(document.getTermsBySent(sentence), model, this.colorMap));
        return out;
    }

    private <T extends Appendable> T renderProperties(final T out, final QuadModel model,
            final Resource node, final boolean emitID, final URI... excludedProperties)
            throws IOException {

        final Set<Resource> seen = Sets.newHashSet(node);
        renderPropertiesHelper(out, model, node, emitID, seen,
                ImmutableSet.copyOf(excludedProperties));
        return out;
    }

    private <T extends Appendable> T renderPropertiesHelper(final T out, final QuadModel model,
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
        return out;
    }

    private <T extends Appendable> T renderTriplesTable(final T out, final QuadModel model)
            throws IOException {

        out.append("<table class=\"table table-condensed datatable\">\n<thead>\n");
        out.append("<tr><th width='25%' class='col-ts'>");
        out.append(shorten(RDF.SUBJECT));
        out.append("</th><th width='25%' class='col-tp'>");
        out.append(shorten(RDF.PREDICATE));
        out.append("</th><th width='25%' class='col-to'>");
        out.append(shorten(RDF.OBJECT));
        out.append("</th><th width='25%' class='col-te'>");
        out.append(shorten(KS.EXPRESSES)).append("<sup>-1</sup>");
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
                for (final Resource mentionID : model.filter(statement.getContext(), KS.EXPRESSES,
                        null).subjects()) {
                    final String extent = model.filter(mentionID, NIF.ANCHOR_OF, null)
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
        return out;
    }

    private <T extends Appendable> T renderMentionsTable(final T out, final QuadModel model)
            throws IOException {

        out.append("<table class=\"table table-condensed datatable\">\n<thead>\n");
        out.append("<tr><th width='12%' class='col-mi'>id</th><th width='18%' class='col-ma'>");
        out.append(shorten(NIF.ANCHOR_OF));
        out.append("</th><th width='11%' class='col-mt'>");
        out.append(shorten(RDF.TYPE));
        out.append("</th><th width='18%' class='col-mo'>mention attributes</th><th width='11%' class='col-md'>");
        out.append(shorten(KS.DENOTES)).append("/").append(shorten(KS.IMPLIES));
        out.append("</th><th width='30%' class='col-me'>");
        out.append(shorten(KS.EXPRESSES));
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
            final QuadModel mentionModel = QuadModel.create();
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
            renderObject(out, Iterables.concat(
                    model.filter(mentionID, KS.DENOTES, null).objects(),
                    model.filter(mentionID, KS.IMPLIES, null).objects()), model);
            out.append("</td><td><ol>");
            for (final Value factID : model.filter(mentionID, KS.EXPRESSES, null).objects()) {
                for (final Statement statement : model.filter(null, null, null, (Resource) factID)) {
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
        return out;
    }

    private <T extends Appendable> T renderObject(final T out, final Object object,
            @Nullable final QuadModel model) throws IOException {

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
        return out;
    }

    private static List<Markable> extractMarkables(final List<Term> terms, final QuadModel model,
            final Map<Object, String> colorMap) {

        final int[] offsets = new int[terms.size()];
        for (int i = 0; i < terms.size(); ++i) {
            offsets[i] = terms.get(i).getOffset();
        }

        final List<Markable> markables = Lists.newArrayList();
        for (final Statement stmt : model.filter(null, KS.DENOTES, null)) {
            final Resource instance = (Resource) stmt.getObject();
            final String color = select(colorMap,
                    model.filter(instance, RDF.TYPE, null).objects(), null);
            if (stmt.getSubject() instanceof URI && color != null) {
                final URI mentionURI = (URI) stmt.getSubject();
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

    private static String escape(final String string) {
        return HtmlEscapers.htmlEscaper().escape(string);
    }

    @Nullable
    private static String shorten(@Nullable final URI uri) {
        if (uri == null) {
            return null;
        }
        final String prefix = Namespaces.DEFAULT.prefixFor(uri.getNamespace());
        if (prefix != null) {
            return prefix + ':' + uri.getLocalName();
        }
        return "&lt;../" + uri.getLocalName() + "&gt;";
    }

    private static Mustache loadTemplate(final Object spec) {
        // Accepts Mustache, URL, File, String (url / filename / template)
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private Iterable<? extends URI> nodeTypes;

        @Nullable
        private Iterable<String> nodeNamespaces;

        @Nullable
        private Iterable<String> rankedNamespaces;

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

        public Builder withNodeNamespaces(@Nullable final Iterable<String> nodeNamespaces) {
            this.nodeNamespaces = nodeNamespaces;
            return this;
        }

        public Builder withRankedNamespaces(@Nullable final Iterable<String> rankedNamespaces) {
            this.rankedNamespaces = rankedNamespaces;
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
            return new TemplateRenderer(this);
        }

    }

}
