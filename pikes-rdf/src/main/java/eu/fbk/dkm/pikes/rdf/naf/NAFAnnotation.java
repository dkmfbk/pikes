package eu.fbk.dkm.pikes.rdf.naf;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import org.openrdf.model.URI;

import ixa.kaflib.KAFDocument;

import eu.fbk.dkm.pikes.rdf.api.Annotation;
import eu.fbk.dkm.pikes.rdf.util.ModelUtil;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Statements;

public final class NAFAnnotation extends Annotation {

    public static final Annotation.Format FORMAT = Annotation.Format.register("NAF",
            ImmutableList.of("application/naf+xml"), Charsets.UTF_8, ImmutableList.of("naf"),
            NAFAnnotation.class, true);

    private final URI uri;

    private final KAFDocument document;

    public NAFAnnotation(final KAFDocument document) {

        URI uri;
        if (document.getPublic().uri != null) {
            uri = Statements.VALUE_FACTORY.createURI(ModelUtil.cleanIRI(document.getPublic().uri));
        } else {
            uri = Statements.VALUE_FACTORY.createURI("md5", Hash.murmur3(document.getRawText())
                    .toString());
        }

        this.uri = uri;
        this.document = document;
    }

    public NAFAnnotation(final Reader reader) throws IOException {
        this(KAFDocument.createFromStream(reader));
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public String getText() {
        return this.document.getRawText();
    }

    @Override
    public Format getFormat() {
        return FORMAT;
    }

    public KAFDocument getDocument() {
        return this.document;
    }

    @Override
    public void write(final Writer out) throws IOException {
        out.write(this.document.toString());
    }

}
