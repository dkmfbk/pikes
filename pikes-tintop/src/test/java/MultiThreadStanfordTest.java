import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import ixa.kaflib.KAFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by alessio on 01/12/15.
 */

public class MultiThreadStanfordTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadStanfordTest.class);
    //    private static String annotators = "tokenize, ssplit, pos, lemma, ner, conll_parse, mate, parse, dcoref";
    //    private static String annotators = "tokenize, ssplit, pos, lemma, mst_parse, semafor";
    private static String annotators = "tokenize, ssplit, pos, lemma, ner_custom, parse, dcoref";
//    private static String annotators = "tokenize";
    private static File nafFolder = new File("/Users/alessio/Desktop/elastic/naf-stronzi/");
    private static Properties props = new Properties();
    static StanfordCoreNLP loadPipeline;

    static {
        props.setProperty("annotators", annotators);

//        props.setProperty("customAnnotatorClass.conll_parse", "eu.fbk.dkm.pikes.tintop.annotators.AnnaParseAnnotator");
//        props.setProperty("customAnnotatorClass.semafor", "eu.fbk.dkm.pikes.tintop.annotators.SemaforAnnotator");
//        props.setProperty("customAnnotatorClass.dbps", "eu.fbk.dkm.pikes.tintop.annotators.DBpediaSpotlightAnnotator");
//        props.setProperty("customAnnotatorClass.mate", "eu.fbk.dkm.pikes.tintop.annotators.MateSrlAnnotator");
//        props.setProperty("customAnnotatorClass.mst_parse", "eu.fbk.dkm.pikes.tintop.annotators.MstParserAnnotator");
        props.setProperty("customAnnotatorClass.ner_custom", "eu.fbk.dkm.pikes.tintop.annotators.NERCustomAnnotator");

        props.setProperty("conll_parse.model",
                "/Users/alessio/Documents/scripts/mateplus/models/retrain-anna-20140819.model");
        props.setProperty("mate.model", "/Users/alessio/Documents/scripts/mateplus/models/retrain-srl-20140818.model");
        props.setProperty("mate.model_be", "/Users/alessio/Documents/scripts/mateplus/models/only-be-2.model");
        props.setProperty("semafor.model_dir",
                "/Users/alessio/Documents/scripts/semafor-semantic-parser/add-on/models");

        props.setProperty("dbps.address", "http://dkm-server-1:7001/rest/annotate");
        props.setProperty("dbps.min_confidence", "0.33");
        props.setProperty("dbps.timeout", "2000");

        props.setProperty("mst_parse.server", "dkm-server-1.fbk.eu");
        props.setProperty("mst_parse.port", "7201");

        props.setProperty("parse.maxlen", "150");
        props.setProperty("ner_custom.maxlength", "200");
//        props.setProperty("ner.maxtime", "1000");

    }

    static void annotate(String text) throws Exception {
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation s = new Annotation(text);
        Annotation myDoc = new Annotation(s);
        pipeline.annotate(myDoc);

        for (CoreMap sentence : myDoc.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println(sentence.get(CoreAnnotations.TokensAnnotation.class).size());
//            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
//                System.out.println(token.get(PikesAnnotations.MateAnnotation.class));
//            }
        }

    }

    public static void main(String[] args) throws Exception {

        loadPipeline = new StanfordCoreNLP(props);

        List<String> texts = new ArrayList<>();

        for (File file : nafFolder.listFiles()) {
            if (!file.isFile()) {
                continue;
            }

            if (!file.getName().endsWith(".naf")) {
                continue;
            }

            LOGGER.info("Adding {}", file.getName());
            KAFDocument document = KAFDocument.createFromFile(file);
            texts.add(document.getRawText());

        }

//        String onlyText = "G. W. Bush and Bono are very strong supporters of the fight of HIV in Africa. Their March 2002 meeting resulted in a 5 billion dollar aid.";
//        texts.add(onlyText);

        final AtomicInteger i = new AtomicInteger(0);
        texts.parallelStream().forEach((text) -> {
            int tmp = i.incrementAndGet();

            try {
                LOGGER.info("File {}", tmp);
                MultiThreadStanfordTest.annotate(text);
            } catch (Exception e) {
                LOGGER.error("Error on file {}", tmp);
                e.printStackTrace();
            }
        });
    }
}
