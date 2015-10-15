package eu.fbk.dkm.pikes.rdf;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.XMLSchema;

import eu.fbk.dkm.pikes.rdf.api.Annotation;
import eu.fbk.dkm.pikes.rdf.api.Annotator;
import eu.fbk.dkm.pikes.rdf.naf.NAFAnnotation;
import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.rdfpro.util.QuadModel;

public class AnnotatorTest {

    @Test
    public void testHttpRestAnnotator() throws Exception {

        final Annotator annotator = Annotator.newHttpRestAnnotator(
                "https://knowledgestore2.fbk.eu/pikes-demo/api/text2naf",
                true,
                ImmutableMap.<String, String>builder().put("meta_author", "${dct:author}")
                        .put("meta_date", "${dct:created}").put("meta_uri", "${uri}")
                        .put("meta_title", "${dct:title}").put("outputformat", "output_naf")
                        .put("text", "${text}").build(), null, NAFAnnotation.FORMAT);

        final QuadModel model = QuadModel.create();
        final URI docURI = new URIImpl("ex:doc");
        final URI ctxURI = new URIImpl("ex:ctx");
        model.add(docURI, NIF.SOURCE_URL, ctxURI);
        model.add(docURI, DCTERMS.CREATED, new LiteralImpl("2014-01-01", XMLSchema.DATE));
        model.add(ctxURI, NIF.IS_STRING, new LiteralImpl("Aldo likes FRED more than PIKES"));

        final Annotation annotation = annotator.annotate(model);
        System.out.println(annotation + "\n");
        annotation.write(System.out);
    }

}
