package eu.fbk.dkm.pikes.rdf.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;

import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

public final class Document implements Cloneable {

    private QuadModel graph;

    private List<Annotation> annotations;

    public Document() {
        this.graph = QuadModel.create();
        this.annotations = new ArrayList<>();
    }

    public Document(final QuadModel graph, final Annotation... annotations) {
        this.graph = graph;
        this.annotations = Lists.newArrayList(annotations);
    }

    public Document(final URI uri, final String text) {
        this();
        initGraph(this.graph, uri, text);
    }

    public Document(final String location) throws IOException {
        this();
        final RDFFormat rdfFormat = Rio.getParserFormatForFileName(location);
        if (rdfFormat != null) {
            try {
                final List<Namespace> namespaces = Lists.newArrayList();
                RDFSources.read(false, true, null, null, location).emit(
                        RDFHandlers.wrap(this.graph, namespaces), 1);
                for (final Namespace namespace : namespaces) {
                    this.graph.setNamespace(namespace);
                }
            } catch (final RDFHandlerException ex) {
                throw new IOException("Could not read RDF from " + location, ex);
            }
        } else {
            final Annotation.Format format = Annotation.Format.forFileName(location);
            if (format == null) {
                throw new IllegalArgumentException("Unsupported file format: " + location);
            }
            try (InputStream in = IO.buffer(IO.read(location))) {
                final Annotation annotation = Annotation.read(in, format);
                this.annotations.add(annotation);
                initGraph(this.graph, annotation.getURI(), annotation.getText());
            }
        }
    }

    public URI getURI() {
        try {
            return Objects.requireNonNull(this.graph.filter(null, NIF.SOURCE_URL, null)
                    .objectURI());
        } catch (final Throwable ex) {
            throw new IllegalStateException(
                    "Invalid document graph. No unique document URI defined.", ex);
        }
    }

    public String getText() {
        try {
            return this.graph.filter(null, NIF.IS_STRING, null).objectLiteral().stringValue();
        } catch (final Throwable ex) {
            throw new IllegalStateException("Invalid document graph. No unique text defined.", ex);
        }
    }

    public QuadModel getGraph() {
        return this.graph;
    }

    public void setGraph(@Nullable final QuadModel graph) {
        this.graph = graph != null ? graph : QuadModel.create();
    }

    public List<Annotation> getAnnotations() {
        return this.annotations;
    }

    public void setAnnotations(final Iterable<Annotation> annotations) {
        this.annotations = annotations == null ? Lists.newArrayList() : Lists
                .newArrayList(annotations);
    }

    public boolean hasAnnotations() {
        return !this.annotations.isEmpty();
    }

    public boolean hasMentions() {
        return this.graph.contains(null, KS.MENTION_OF, null);
    }

    public boolean hasInstances() {
        return this.graph.contains(null, KS.DENOTES, null)
                || this.graph.contains(null, KS.IMPLIES, null);
    }

    public void clear() {
        final QuadModel graph = QuadModel.create();
        try {
            initGraph(graph, getURI(), getText());
        } catch (final Throwable ex) {
            // ignore - will start from empty graph
        }
        this.graph.clear();
        this.annotations.clear();
        this.graph.addAll(graph);
    }

    @Override
    public Document clone() {
        try {
            final Document document = (Document) super.clone();
            document.graph = QuadModel.create(document.graph);
            document.annotations = Lists.newArrayList(document.annotations);
            return document;
        } catch (final CloneNotSupportedException ex) {
            throw new Error(ex);
        }
    }

    @Override
    public String toString() {
        try {
            return getURI().stringValue();
        } catch (final Throwable ex) {
            return "<invalid document>";
        }
    }

    private static void initGraph(final QuadModel graph, final URI uri, final String text) {
        final URI ctxURI = Statements.VALUE_FACTORY.createURI(uri.stringValue() + "#ctx");
        graph.add(uri, RDF.TYPE, KS.RESOURCE);
        graph.add(ctxURI, NIF.SOURCE_URL, uri);
        graph.add(ctxURI, RDF.TYPE, NIF.CONTEXT);
        graph.add(ctxURI, NIF.IS_STRING, Statements.VALUE_FACTORY.createLiteral(text));
    }

}
