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

            System.out.println();
            System.out.println(thisSent.get(CoreAnnotations.SentenceIndexAnnotation.class));
            System.out.println();

            List<CoreLabel> tokens = thisSent.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                System.out.println(token);
                System.out.println(token.index());
                System.out.println(token.sentIndex());
                System.out.println(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
                System.out.println(token.get(CoreAnnotations.LemmaAnnotation.class));
                System.out.println(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                System.out.println();
            }

            System.out.println("---");
            System.out.println();
        }
    }

    public static void main(String[] args) {

        String ITAtext = "Esponente del Partito democratico, è stato eletto nel novembre 2008 presidente degli Stati Uniti d'America e rieletto nel novembre 2012. Nel 2009 gli è stato conferito il premio Nobel per la pace \"per il suo straordinario impegno per rafforzare la diplomazia internazionale e la cooperazione tra i popoli\".";
        String ENGtext = "Washington D.C. is the capital of the United States. It was named after George Washington, the first president of the U.S.";

        Properties props;
        Annotation annotation;

        props = new Properties();
        props.setProperty("annotators", "ita_toksent, pos, ita_lemma, ner");

        props.setProperty("ita_toksent.conf_folder", "/Users/alessio/Documents/Resources/ita-models/conf");
        props.setProperty("pos.model", "/Users/alessio/Documents/Resources/ita-models/italian5.tagger");
        props.setProperty("ita_lemma.fstan_command", "/Users/alessio/Documents/Resources/ita-models/MorphoPro/bin/fstan/x86_64/fstan");
        props.setProperty("ita_lemma.fstan_model", "/Users/alessio/Documents/Resources/ita-models/MorphoPro/models/italian-utf8.fsa");
        props.setProperty("ner.model", "/Users/alessio/Documents/Resources/ita-models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser");

        props.setProperty("customAnnotatorClass.ita_toksent", "eu.fbk.dkm.pikes.tintop.annotators.ITA_TokenAnnotator");
        props.setProperty("customAnnotatorClass.ita_lemma", "eu.fbk.dkm.pikes.tintop.annotators.ITA_LemmaAnnotator");
        StanfordCoreNLP ITApipeline = new StanfordCoreNLP(props);
        annotation = new Annotation(ITAtext);
        ITApipeline.annotate(annotation);
        printOutput(annotation);

//        System.exit(1);

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        StanfordCoreNLP ENGpipeline = new StanfordCoreNLP(props);
        annotation = new Annotation(ENGtext);
        ENGpipeline.annotate(annotation);
        printOutput(annotation);


    }
}
