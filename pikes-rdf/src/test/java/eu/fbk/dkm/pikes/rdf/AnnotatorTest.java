package eu.fbk.dkm.pikes.rdf;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.XMLSchema;

import eu.fbk.dkm.pikes.rdf.api.Annotator;
import eu.fbk.dkm.pikes.rdf.api.Document;
import eu.fbk.dkm.pikes.rdf.naf.NAFAnnotation;

public class AnnotatorTest {

    @Test
    public void testHttpRestAnnotator() throws Exception {

        final Annotator annotator = Annotator.newHttpAnnotator(
                "https://knowledgestore2.fbk.eu/pikes-demo/api/text2naf",
                true,
                ImmutableMap.<String, String>builder().put("meta_author", "${dct:author}")
                        .put("meta_date", "${dct:created}").put("meta_uri", "${uri}")
                        .put("meta_title", "${dct:title}").put("outputformat", "output_naf")
                        .put("text", "${text}").build(), null, NAFAnnotation.FORMAT);

        final URI uri = new URIImpl("ex:doc");
        final Document document = new Document(uri, "Aldo likes FRED more than PIKES");
        document.getGraph().add(uri, DCTERMS.CREATED,
                new LiteralImpl("2014-01-01T00:00:00Z", XMLSchema.DATE));

        annotator.annotate(document);
        System.out.println(document.getAnnotations() + "\n");
        document.getAnnotations().get(0).write(System.out);
    }

}
