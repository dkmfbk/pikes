package eu.fbk.dkm.pikes.tintop;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * Created by alessio on 26/02/15.
 */

public class StanfordTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StanfordTest.class);

    private static void printOutput(Annotation annotation) {
        List<CoreMap> sents = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap thisSent : sents) {
            List<CoreLabel> tokens = thisSent.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                System.out.println(token);
                System.out.println(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
                System.out.println(token.get(CoreAnnotations.LemmaAnnotation.class));
                System.out.println();
            }
            System.out.println("---");
        }
    }

    public static void main(String[] args) {

        String ITAtext = "Non dire gatto se non ce l'hai nel sacco.";
        ITAtext = "Il vedere è una proprietà.";
        String ENGtext = "Washington D.C. is the capital of the United States. It was named after George Washington, the first president of the U.S.";

        Properties props;
        Annotation annotation;

        props = new Properties();
        props.setProperty("annotators", "ita_toksent, pos, ita_lemma");
        props.setProperty("ita_toksent.conf_folder", "/Users/alessio/Documents/scripts/textpro-ita/giovanni/conf");
        props.setProperty("pos.model", "/Users/alessio/Documents/Resources/italian7.tagger");
        props.setProperty("ita_lemma.fstan_command", "/Users/alessio/Documents/scripts/textpro/modules/MorphoPro/bin/fstan/x86_64/fstan");
        props.setProperty("ita_lemma.fstan_model", "/Users/alessio/Documents/scripts/textpro/modules/MorphoPro/models/italian-utf8.fsa");

        props.setProperty("customAnnotatorClass.ita_toksent", "eu.fbk.dkm.pikes.tintop.annotators.TP_TokenAnnotator");
        props.setProperty("customAnnotatorClass.ita_lemma", "eu.fbk.dkm.pikes.tintop.annotators.TP_LemmaAnnotator");
        StanfordCoreNLP ITApipeline = new StanfordCoreNLP(props);
        annotation = new Annotation(ITAtext);
        ITApipeline.annotate(annotation);
        printOutput(annotation);

//        props = new Properties();
//        props.setProperty("annotators", "tokenize, ssplit, pos");
//        StanfordCoreNLP ENGpipeline = new StanfordCoreNLP(props);
//        annotation = new Annotation(ENGtext);
//        ENGpipeline.annotate(annotation);
//        printOutput(annotation);


    }
}
