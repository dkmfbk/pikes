package eu.fbk.dkm.pikes.tintop.ita;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            System.out.println("Dep parse (c): " + thisSent
                    .get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class));
            System.out.println(
                    "Dep parse (b): " + thisSent.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class));
            System.out.println("Dep parse (cc): " + thisSent
                    .get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class));
            System.out.println("Tree: " + thisSent.get(TreeCoreAnnotations.TreeAnnotation.class));

            List<CoreLabel> tokens = thisSent.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                System.out.println("Token: " + token);
                System.out.println("Index: " + token.index());
                System.out.println("Sent index: " + token.sentIndex());
                System.out.println("Begin: " + token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class));
                System.out.println("End: " + token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
                System.out.println("POS: " + token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
                System.out.println("Lemma: " + token.get(CoreAnnotations.LemmaAnnotation.class));
                System.out.println("NER: " + token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                System.out.println();
            }

            System.out.println("---");
            System.out.println();
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String[] args) throws IOException {

//        int start = 4516;
//        int end = 4517;
        int start = 10;
        int end = 50;

        List<String> lines = Files.readAllLines(new File("/Users/alessio/Desktop/testi.txt").toPath());
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < lines.size(); i++) {
            if (i < start) {
                continue;
            }
            if (i >= end) {
                break;
            }
            String line = lines.get(i);
            buffer.append(line.trim()).append("\n");
            if (end - start < 10) {
                System.out.println(line);
            }
        }

        String ITAtext = buffer.toString();
        ITAtext = "Assange risponderà ai giudici svedesi ma via web e senza uscire dall'ambasciata.";
        Properties props;
        Annotation annotation;

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, ita_morpho, ita_lemma, ner, depparse");
        props.setProperty("tokenize.language", "Spanish");

        props.setProperty("ita_toksent.conf_folder", "/Users/alessio/Documents/Resources/ita-models/conf");

        props.setProperty("pos.model", "/Users/alessio/Documents/Resources/ita-models/italian5.tagger");
//        props.setProperty("ita_lemma.fstan_command",
//                "/Users/alessio/Documents/Resources/ita-models/MorphoPro/bin/fstan/x86_64/fstan");
//        props.setProperty("ita_lemma.fstan_model",
//                "/Users/alessio/Documents/Resources/ita-models/MorphoPro/models/italian-utf8.fsa");
        props.setProperty("ner.model",
                "/Users/alessio/Documents/Resources/ita-models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser");
        props.setProperty("depparse.model", "/Users/alessio/Documents/Resources/ita-models/parser-model-1.txt.gz");
        props.setProperty("ner.useSUTime", "0");

        props.setProperty("customAnnotatorClass.ita_lemma", "eu.fbk.dh.digimorph.annotator.DigiLemmaAnnotator");
        props.setProperty("customAnnotatorClass.ita_morpho", "eu.fbk.dh.digimorph.annotator.DigiMorphAnnotator");
        props.setProperty("ita_morpho.model", "/Users/alessio/Documents/Resources/ita-models/italian.db");

        StanfordCoreNLP ITApipeline = new StanfordCoreNLP(props);
        annotation = new Annotation(ITAtext);
        ITApipeline.annotate(annotation);
        System.out.println(ITApipeline.timingInformation());

        int tokenSize = annotation.get(CoreAnnotations.TokensAnnotation.class).size();
        int sentenceSize = annotation.get(CoreAnnotations.SentencesAnnotation.class).size();

        LOGGER.info("### STATISTICS:");
        LOGGER.info("Number of sentences: {}", sentenceSize);
        LOGGER.info("Number of tokens: {}", tokenSize);

        int chars = 0;
        int wordSize = 0;
        for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
            if (token.get(CoreAnnotations.PartOfSpeechAnnotation.class).startsWith("F")) {
                continue;
            }
            chars += token.endPosition() - token.beginPosition();
            wordSize++;
        }

        LOGGER.info("Number of words: {}", wordSize);

        LOGGER.info("Document length (with spaces): {}", ITAtext.length());
        LOGGER.info("Document length (without spaces): {}", ITAtext.replaceAll("\\s+", "").length());
        LOGGER.info("Document length (letters only): {}", chars);

        double gulpease = 89 + (300 * sentenceSize - 10 * chars) / (wordSize * 1.0);
        LOGGER.info("Gulpease: {}", gulpease);

        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            int ss = sentence.get(CoreAnnotations.TokensAnnotation.class).get(0)
                    .get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            int se = sentence.get(CoreAnnotations.TokensAnnotation.class)
                    .get(sentence.get(CoreAnnotations.TokensAnnotation.class).size() - 1)
                    .get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
            LOGGER.info(String.format("Sentence %d, start %d end %d",
                    sentence.get(CoreAnnotations.SentenceIndexAnnotation.class), ss, se));
        }

//        System.out.println("Length: " + ITAtext.length());
        if (ITAtext.length() < 100) {
            printOutput(annotation);
        }

//        props = new Properties();
//        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
//        StanfordCoreNLP ENGpipeline = new StanfordCoreNLP(props);
//        annotation = new Annotation(ENGtext);
//        ENGpipeline.annotate(annotation);
//        printOutput(annotation);

    }
}
