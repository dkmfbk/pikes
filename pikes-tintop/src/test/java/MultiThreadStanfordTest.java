import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.PikesAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by alessio on 01/12/15.
 */

public class MultiThreadStanfordTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadStanfordTest.class);
    private static String annotators = "tokenize, ssplit, pos, lemma, conll_parse, mate";
    //    private static String annotators = "tokenize, ssplit, pos, lemma, mst_parse, semafor";
    private static File nafFolder = new File("/Users/alessio/Documents/scripts/mateplus/models/naf/");
    private static Properties props = new Properties();
    static StanfordCoreNLP loadPipeline;

    static {
        props.setProperty("annotators", annotators);

        props.setProperty("customAnnotatorClass.conll_parse", "eu.fbk.dkm.pikes.tintop.annotators.AnnaParseAnnotator");
        props.setProperty("customAnnotatorClass.semafor", "eu.fbk.dkm.pikes.tintop.annotators.SemaforAnnotator");
        props.setProperty("customAnnotatorClass.dbps", "eu.fbk.dkm.pikes.tintop.annotators.DBpediaSpotlightAnnotator");
        props.setProperty("customAnnotatorClass.mate", "eu.fbk.dkm.pikes.tintop.annotators.MateSrlAnnotator");
        props.setProperty("customAnnotatorClass.mst_parse", "eu.fbk.dkm.pikes.tintop.annotators.MstParserAnnotator");

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
    }

    static void annotate(String text) throws Exception {
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation s = new Annotation(text);
        Annotation myDoc = new Annotation(s);
        pipeline.annotate(myDoc);

        for (CoreMap sentence : myDoc.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                System.out.println(token.get(PikesAnnotations.MateAnnotation.class));
            }
        }

    }

    public static void main(String[] args) throws Exception {

        loadPipeline = new StanfordCoreNLP(props);

        List<String> texts = new ArrayList<>();

//        for (File file : nafFolder.listFiles()) {
//            if (!file.isFile()) {
//                continue;
//            }
//
//            if (!file.getName().endsWith(".naf")) {
//                continue;
//            }
//
//            KAFDocument document = KAFDocument.createFromFile(file);
//            texts.add(document.getRawText());
//
//        }

        String onlyText = "G. W. Bush and Bono are very strong supporters of the fight of HIV in Africa. Their March 2002 meeting resulted in a 5 billion dollar aid.";
        texts.add(onlyText);

        texts.parallelStream().forEach((text) -> {
            try {
                MultiThreadStanfordTest.annotate(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
