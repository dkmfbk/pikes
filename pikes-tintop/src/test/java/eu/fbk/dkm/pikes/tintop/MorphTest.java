package eu.fbk.dkm.pikes.tintop;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by alessio on 08/06/16.
 */

public class MorphTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MorphTest.class);

    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, ita_morpho, ita_lemma");
        props.setProperty("customAnnotatorClass.ita_lemma", "eu.fbk.dh.digimorph.annotator.DigiLemmaAnnotator");
        props.setProperty("customAnnotatorClass.ita_morpho", "eu.fbk.dh.digimorph.annotator.DigiMorphAnnotator");

        props.setProperty("tokenize.language", "Spanish");
        props.setProperty("ssplit.newlineIsSentenceBreak", "always");

        props.setProperty("pos.model", "/Users/alessio/Documents/Resources/ita-models/italian5.tagger");

        props.setProperty("ita_morpho.model", "/Users/alessio/Documents/Resources/ita-models/italian.db");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        LOGGER.info("Annotating");

        for (int i = 0; i < 50; i++) {
            Annotation annotation = new Annotation("Questa è una prova.");
            pipeline.annotate(annotation);
        }

        LOGGER.info("Finishing");

        Thread.sleep(60000);
    }
}
