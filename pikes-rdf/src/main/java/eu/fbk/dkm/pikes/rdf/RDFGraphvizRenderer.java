package eu.fbk.dkm.pikes.rdf;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.html.HtmlEscapers;
import eu.fbk.rdfpro.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RDFGraphvizRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDFGraphvizRenderer.class);

    private static final String NEWLINE = "&#10;";

    private final Set<String> nodeNamespaces;

    private final Set<Resource> nodeTypes;

    private final Set<IRI> ignoredProperties;

    private final Set<IRI> collapsedProperties;

    private final Map<? super Resource, String> colorMap;

    private final Map<? super Resource, String> styleMap;

    private final Namespaces namespaces;

    private final Ordering<? super Value> valueComparator;

    private final String graphvizCommand;

    private RDFGraphvizRenderer(final Builder builder) {
        this.nodeNamespaces = builder.nodeNamespaces == null ? ImmutableSet.of() //
                : ImmutableSet.copyOf(builder.nodeNamespaces);
        this.nodeTypes = builder.nodeTypes == null ? ImmutableSet.of() //
                : ImmutableSet.copyOf(builder.nodeTypes);
        this.ignoredProperties = builder.ignoredProperties == null ? ImmutableSet.of() //
                : ImmutableSet.copyOf(builder.ignoredProperties);
        this.collapsedProperties = builder.collapsedProperties == null ? ImmutableSet.of() //
                : ImmutableSet.copyOf(builder.collapsedProperties);
        this.colorMap = builder.colorMap == null ? null : ImmutableMap.copyOf(builder.colorMap);
        this.styleMap = builder.styleMap == null ? null : ImmutableMap.copyOf(builder.styleMap);
        this.namespaces = builder.namespaces == null ? Namespaces.DEFAULT : builder.namespaces;
        this.valueComparator = builder.valueComparator != null ? Ordering
                .from(builder.valueComparator) : Ordering.from(Statements.valueComparator());
        this.graphvizCommand = builder.graphvizCommand == null ? "neato" : builder.graphvizCommand;
    }

    public <T extends Appendable> T emitSVG(final T out, final QuadModel model) throws IOException {

        Process process = null;
        File dotFile = null;

        try {
            dotFile = File.createTempFile("graphviz-", ".dot");
            dotFile.deleteOnExit();

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    dotFile), Charsets.UTF_8))) {
                emitDot(writer, model);
            }

            process = new ProcessBuilder().command(this.graphvizCommand,
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
                            LOGGER.error("[" + RDFGraphvizRenderer.this.graphvizCommand + "] "
                                    + line);
                        }
                    } catch (final IOException ex) {
                        LOGGER.error(
                                "[" + RDFGraphvizRenderer.this.graphvizCommand + "] "
                                        + ex.getMessage(), ex);
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

        return out;
    }

    public void emitDot(final Appendable out, final QuadModel model) throws IOException {

        // Identify graph nodes using the supplied selector
        final Set<Resource> nodes = Sets.newHashSet();
        if (!this.nodeNamespaces.isEmpty()) {
            for (final Value value : Iterables.concat(model.subjects(), model.objects())) {
                if (value instanceof IRI
                        && this.nodeNamespaces.contains(((IRI) value).getNamespace())) {
                    nodes.add((IRI) value);
                }
            }
        }
        if (!this.nodeTypes.isEmpty()) {
            for (final Statement stmt : model.filter(null, RDF.TYPE, null)) {
                if (stmt.getObject() instanceof Resource
                        && this.nodeTypes.contains(stmt.getObject())) {
                    nodes.add(stmt.getSubject());
                }
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
        for (final Statement stmt : model) {

            // Skip the statement if its property should be ignored
            if (this.ignoredProperties.contains(stmt.getPredicate())) {
                continue;
            }

            // Ensure that subject and object are both nodes and the edge was not emitted before
            if (!(stmt.getObject() instanceof Resource)) {
                continue;
            }
            final Resource sourceNode = stmt.getSubject();
            final Resource targetNode = (Resource) stmt.getObject();
            if (!nodes.contains(sourceNode) || !nodes.contains(targetNode)
                    || !encounteredEdges.add(ImmutableList.of(sourceNode, targetNode))) {
                continue;
            }
            final String sourceId = hash(sourceNode);
            final String targetId = hash(targetNode);

            // Retrieve the predicates associated to the edge
            final List<IRI> properties = this.valueComparator.sortedCopy(model.filter(sourceNode,
                    null, targetNode).predicates());

            // Select edge style
            final List<IRI> keys = Lists.newArrayList(properties);
            for (final Value sourceType : model.filter(sourceNode, RDF.TYPE, null).objects()) {
                if (sourceType instanceof IRI) {
                    keys.add(Statements.VALUE_FACTORY.createIRI(sourceType.stringValue() + "-from"));
                }
            }
            for (final Value targetType : model.filter(targetNode, RDF.TYPE, null).objects()) {
                if (targetType instanceof IRI) {
                    keys.add(Statements.VALUE_FACTORY.createIRI(targetType.stringValue() + "-to"));
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

    private boolean emitDotTooltip(final Appendable out, final QuadModel model,
            final Resource node, final int indent, final Set<Resource> excludedNodes,
            final Set<Resource> expandedNodes) throws IOException {
        boolean notEmpty = false;
        for (final IRI pred : this.valueComparator.sortedCopy(model.filter(node, null, null)
                .predicates())) {
            if (this.ignoredProperties.contains(pred)) {
                continue;
            }
            expandedNodes.add(node);
            for (final Value object : this.valueComparator.sortedCopy(model.filter(node, pred,
                    null).objects())) {
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
                            && !this.collapsedProperties.contains(pred)) {
                        emitDotTooltip(out, model, (Resource) object, indent + 1, excludedNodes,
                                expandedNodes);
                    }
                }
            }
        }
        return notEmpty;
    }

    private List<String> format(final Iterable<? extends Value> values) {
        final List<String> result = Lists.newArrayList();
        for (final Value value : values) {
            result.add(format(value));
        }
        return result;
    }

    private String format(final Value value) {
        if (value instanceof IRI) {
            final IRI IRI = (IRI) value;
            final String ns = IRI.getNamespace();
            final String prefix = this.namespaces.prefixFor(ns);
            if (prefix == null) {
                return escape("<.." + ns.charAt(ns.length() - 1) + IRI.getLocalName() + ">");
            } else {
                return prefix + ":" + escape(IRI.getLocalName());
            }
        }
        return escape(Statements.formatValue(value, this.namespaces));
    }

    @Nullable
    private String shorten(@Nullable final IRI IRI) {
        if (IRI == null) {
            return null;
        }
        final String prefix = this.namespaces.prefixFor(IRI.getNamespace());
        if (prefix != null) {
            return prefix + ':' + IRI.getLocalName();
        }
        return "&lt;../" + IRI.getLocalName() + "&gt;";
    }

    private static String select(@Nullable final Map<? super Resource, String> map,
            final Iterable<? extends Value> keys, final String defaultColor) {
        String color = null;
        if (map != null) {
            for (final Value key : keys) {
                if (key instanceof Resource) {
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
        }
        return color != null ? color : defaultColor;
    }

    private static String escape(final String string) {
        return HtmlEscapers.htmlEscaper().escape(string);
    }

    private static String hash(final Value value) {
        final StringBuilder builder = new StringBuilder();
        if (value instanceof IRI) {
            builder.append((char) 1);
            builder.append(value.stringValue());
        } else if (value instanceof BNode) {
            builder.append((char) 2);
            builder.append(((BNode) value).getID());
        } else if (value instanceof Literal) {
            final Literal literal = (Literal) value;
            builder.append((char) 3);
            builder.append(literal.getLabel());
            if (literal.getLanguage().isPresent()) {
                builder.append((char) 4);
                builder.append(literal.getLanguage().get());
            } else if (!literal.getDatatype().equals(XMLSchema.STRING)) {
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
        private Iterable<String> nodeNamespaces;

        @Nullable
        private Iterable<? extends Resource> nodeTypes;

        @Nullable
        private Iterable<? extends IRI> ignoredProperties;

        @Nullable
        private Iterable<? extends IRI> collapsedProperties;

        @Nullable
        private Map<? super Resource, String> colorMap;

        @Nullable
        private Map<? super Resource, String> styleMap;

        @Nullable
        private Namespaces namespaces;

        @Nullable
        private Comparator<? super Value> valueComparator;

        @Nullable
        private String graphvizCommand;

        public Builder withNodeNamespaces(@Nullable final Iterable<String> nodeNamespaces) {
            this.nodeNamespaces = nodeNamespaces;
            return this;
        }

        public Builder withNodeTypes(@Nullable final Iterable<? extends IRI> nodeTypes) {
            this.nodeTypes = nodeTypes;
            return this;
        }

        public Builder withIgnoredProperties(
                @Nullable final Iterable<? extends IRI> ignoredProperties) {
            this.ignoredProperties = ignoredProperties;
            return this;
        }

        public Builder withCollapsedProperties(
                @Nullable final Iterable<? extends IRI> collapsedProperties) {
            this.collapsedProperties = collapsedProperties;
            return this;
        }

        public Builder withColorMap(@Nullable final Map<? super Resource, String> colorMap) {
            this.colorMap = colorMap;
            return this;
        }

        public Builder withStyleMap(@Nullable final Map<? super Resource, String> styleMap) {
            this.styleMap = styleMap;
            return this;
        }

        public Builder withNamespaces(@Nullable final Namespaces namespaces) {
            this.namespaces = namespaces;
            return this;
        }

        public Builder withValueComparator(
                @Nullable final Comparator<? super Value> valueComparator) {
            this.valueComparator = valueComparator;
            return this;
        }

        public Builder withGraphvizCommand(@Nullable final String graphvizCommand) {
            this.graphvizCommand = graphvizCommand;
            return this;
        }

        public RDFGraphvizRenderer build() {
            return new RDFGraphvizRenderer(this);
        }

    }

}
