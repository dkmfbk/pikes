package eu.fbk.dkm.pikes.rdf.naf;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.pikes.rdf.api.Annotation;
import eu.fbk.dkm.pikes.rdf.util.ModelUtil;
import eu.fbk.dkm.pikes.rdf.vocab.KEMT;
import eu.fbk.dkm.pikes.rdf.vocab.NIF;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Statements;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.KAFDocument.FileDesc;
import ixa.kaflib.KAFDocument.Public;

public final class NAFAnnotation extends Annotation {

    private static final long serialVersionUID = 1L;

    public static final Annotation.Format FORMAT = Annotation.Format.register("NAF",
            ImmutableList.of("application/naf+xml"), Charsets.UTF_8, ImmutableList.of("naf"),
            NAFAnnotation.class, true);

    private static final Logger LOGGER = LoggerFactory.getLogger(NAFAnnotation.class);

    private static final DateFormat[] DATE_FORMATS = new DateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") };

    private KAFDocument naf;

    public NAFAnnotation(final KAFDocument naf) {
        this.naf = Objects.requireNonNull(naf);
    }

    public NAFAnnotation(final Reader reader) throws IOException {
        this(KAFDocument.createFromStream(reader));
    }

    @Override
    public Model getSource() {

        // Obtain the default value factory
        final ValueFactory vf = Statements.VALUE_FACTORY;

        // Extract header and raw text from NAF document
        final Public pub = this.naf.getPublic();
        final FileDesc desc = this.naf.getFileDesc();
        final String text = this.naf.getRawText();

        // Generate IRI/BNodes for document resource and document NIF context string
        final Resource document, documentCtx;
        if (pub != null && pub.uri != null) {
            final String iri = ModelUtil.cleanIRI(pub.uri);
            document = vf.createIRI(iri);
            documentCtx = vf.createBNode(iri + "#ctx");
        } else {
            final String id = Hash.murmur3(text).toString();
            document = vf.createBNode(id);
            documentCtx = vf.createBNode(id + "-ctx");
        }

        // Emit always available triples for document resource and NIF context
        final Model model = new LinkedHashModel();
        model.add(document, RDF.TYPE, KEMT.TEXT_RESOURCE);
        model.add(documentCtx, RDF.TYPE, NIF.CONTEXT);
        model.add(documentCtx, NIF.SOURCE_URL, document);

        // Emit triple dcterms:identifier based on the "Public" header
        if (pub != null) {
            model.add(document, DCTERMS.IDENTIFIER, vf.createLiteral(pub.publicId));
        }

        // Emit triples dcterms:creator, dcterms:title, dcterms:created for the "FileDesc" header
        if (desc != null) {
            if (desc.author != null) {
                model.add(document, DCTERMS.CREATOR, vf.createLiteral(desc.author));
            }
            if (desc.title != null) {
                model.add(document, DCTERMS.TITLE, vf.createLiteral(desc.title));
            }
            if (desc.creationtime != null) {
                Date date = null;
                for (final DateFormat format : DATE_FORMATS) {
                    try {
                        date = format.parse(desc.creationtime);
                        break;
                    } catch (final Throwable ex) {
                        // ignore
                    }
                }
                if (date == null) {
                    LOGGER.info("Cannot parse date " + desc.creationtime);
                    model.add(document, DCTERMS.CREATED, vf.createLiteral(desc.creationtime));
                } else {
                    model.add(document, DCTERMS.CREATED, vf.createLiteral(date));
                }
            }
        }

        // Emit triple nif:isString with the raw text, if available
        if (text != null) {
            model.add(documentCtx, NIF.IS_STRING, vf.createLiteral(text));
        }

        // Return the RDF model built
        return model;
    }

    @Override
    public Format getFormat() {
        return FORMAT;
    }

    public KAFDocument getNAF() {
        return this.naf;
    }

    public void setNAF(final KAFDocument naf) {
        this.naf = Objects.requireNonNull(naf);
    }

    @Override
    public void write(final Writer out) throws IOException {
        out.write(this.naf.toString());
    }

    @Override
    public NAFAnnotation clone() {
        try {
            final String nafString = this.naf.toString();
            final NAFAnnotation annotation = (NAFAnnotation) super.clone();
            annotation.naf = KAFDocument.createFromStream(new StringReader(nafString));
            return annotation;
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
