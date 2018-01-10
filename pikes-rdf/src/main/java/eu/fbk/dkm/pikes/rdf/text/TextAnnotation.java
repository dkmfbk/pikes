package eu.fbk.dkm.pikes.rdf.text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import eu.fbk.dkm.pikes.rdf.api.Annotation;
import eu.fbk.dkm.pikes.rdf.vocab.KEMT;
import eu.fbk.dkm.pikes.rdf.vocab.NIF;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Statements;

public final class TextAnnotation extends Annotation {

    private static final long serialVersionUID = 1L;

    public static final Annotation.Format FORMAT = Annotation.Format.register("TXT",
            ImmutableList.of("text/plain"), Charsets.UTF_8, ImmutableList.of("txt"),
            TextAnnotation.class, true);

    private final String text;

    public TextAnnotation(final String text) {
        this.text = Objects.requireNonNull(text);
    }

    public TextAnnotation(final Reader reader) throws IOException {
        this(CharStreams.toString(reader));
    }

    @Override
    public Model getSource() {

        // Obtain the default value factory
        final ValueFactory vf = Statements.VALUE_FACTORY;

        // Generate BNodes for document resource and document NIF context string
        final String id = Hash.murmur3(this.text).toString();
        final Resource document = vf.createBNode(id);
        final Resource documentCtx = vf.createBNode(id + "-ctx");

        // Emit RDF for document resource and NIF context
        final Model model = new LinkedHashModel();
        model.add(document, RDF.TYPE, KEMT.TEXT_RESOURCE);
        model.add(documentCtx, RDF.TYPE, NIF.CONTEXT);
        model.add(documentCtx, NIF.SOURCE_URL, document);
        model.add(documentCtx, NIF.IS_STRING, vf.createLiteral(this.text));
        return model;
    }

    @Override
    public Format getFormat() {
        return FORMAT;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public void write(final Writer out) throws IOException {
        out.write(this.text);
    }

}
