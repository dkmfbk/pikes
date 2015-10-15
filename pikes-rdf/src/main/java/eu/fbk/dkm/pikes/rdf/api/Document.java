package eu.fbk.dkm.pikes.rdf.api;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import org.openrdf.model.URI;

import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.rdfpro.util.QuadModel;

public final class Document {

    private QuadModel graph;

    private List<Annotation> annotations;

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

    @Override
    public String toString() {
        try {
            return getURI().stringValue();
        } catch (final Throwable ex) {
            return "<invalid document>";
        }
    }

}
