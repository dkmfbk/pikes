package eu.fbk.dkm.pikes.tintop.ita.simpserver;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import eu.fbk.dkm.pikes.tintop.annotators.Defaults;
import eu.fbk.dkm.utils.CommandLine;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */

public class SimpServer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SimpServer.class);

    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final Integer DEFAULT_PORT = 8011;

    public SimpServer(String host, Integer port, String configFile) {
        logger.info("starting " + host + "\t" + port + " (" + new Date() + ")...");

        final HttpServer httpServer = new HttpServer();
        NetworkListener nl = new NetworkListener("pikes-ita", host, port);
        httpServer.addListener(nl);

//        Properties props2 = new Properties();
//        props2.setProperty("annotators", "tokenize, ssplit, pos, ita_morpho, ita_lemma");
//        props2.setProperty("customAnnotatorClass.ita_lemma", "eu.fbk.dh.digimorph.annotator.DigiLemmaAnnotator");
//        props2.setProperty("customAnnotatorClass.ita_morpho", "eu.fbk.dh.digimorph.annotator.DigiMorphAnnotator");
//        props2.setProperty("pos.model", "/Users/alessio/Documents/Resources/ita-models/italian5.tagger");
//        props2.setProperty("tokenize.language", "Spanish");
//        props2.setProperty("ita_morpho.model", "/Users/alessio/Documents/Resources/ita-models/italian.db");
//        StanfordCoreNLP pipeline2 = new StanfordCoreNLP(props2);

        Properties props = new Properties();

//        props.setProperty("annotators", "tokenize, ssplit, ml, pos, ita_morpho, ita_lemma, ner, depparse");
////        props.setProperty("annotators", "tokenize, ssplit, ml, pos, ita_morpho, ita_lemma");
//        props.setProperty("customAnnotatorClass.ita_lemma", "eu.fbk.dh.digimorph.annotator.DigiLemmaAnnotator");
//        props.setProperty("customAnnotatorClass.ita_morpho", "eu.fbk.dh.digimorph.annotator.DigiMorphAnnotator");
//        props.setProperty("customAnnotatorClass.ml", "eu.fbk.dkm.pikes.tintop.annotators.LinkingAnnotator");
//
//        props.setProperty("tokenize.language", "Spanish");
//        props.setProperty("ssplit.newlineIsSentenceBreak", "always");
//
//        props.setProperty("ita_toksent.conf_folder", "/Users/alessio/Documents/Resources/ita-models/conf");
//
//        props.setProperty("pos.model", "/Users/alessio/Documents/Resources/ita-models/italian5.tagger");
//        props.setProperty("ner.model",
//                "/Users/alessio/Documents/Resources/ita-models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser");
//        props.setProperty("depparse.model", "/Users/alessio/Documents/Resources/ita-models/parser-model-1.txt.gz");
//        props.setProperty("ner.useSUTime", "0");
//
//        props.setProperty("ita_morpho.model", "/Users/alessio/Documents/Resources/ita-models/italian.db");
//
//        props.setProperty("ml.annotator", "ml-annotate");
//        props.setProperty("ml.address", "http://ml.apnetwork.it/annotate");
//        props.setProperty("ml.min_confidence", "0.2");

        if (configFile != null) {
            try {
                FileInputStream stream = new FileInputStream(configFile);
                props.load(stream);
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        String glossarioFileName = "/Users/alessio/Documents/SIMPATICO/glossario-parsed-edited.json";
//        String easyWordsFileName = "/Users/alessio/Documents/SIMPATICO/easy-output.json";
        String glossarioFileName = props.getProperty("glossario");
        String easyWordsFileName = props.getProperty("easyWords");

        Boolean parseGlossario = Defaults.getBoolean(props.getProperty("glossario.parse", "true"), true);

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Gson gson = new Gson();

        // Load simple words

        EasyLanguage easyLanguage = new EasyLanguage();
        if (easyWordsFileName != null) {
            logger.info("Loading easy lemmas");
            try {
                JsonReader reader = new JsonReader(new FileReader(easyWordsFileName));
                easyLanguage = gson.fromJson(reader, EasyLanguage.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        HashMap<Integer, HashMultimap<String, String>> easyWords = new HashMap<>();

        easyWords.put(1, HashMultimap.create());
        easyWords.get(1).putAll("S", Arrays.asList(easyLanguage.level1.n));
//        easyWords.get(1).putAll("A", Arrays.asList(easyLanguage.level1.a));
//        easyWords.get(1).putAll("B", Arrays.asList(easyLanguage.level1.r));
        easyWords.get(1).putAll("V", Arrays.asList(easyLanguage.level1.v));
        easyWords.put(2, HashMultimap.create());
        easyWords.get(2).putAll("S", Arrays.asList(easyLanguage.level2.n));
        easyWords.get(2).putAll("A", Arrays.asList(easyLanguage.level2.a));
        easyWords.get(2).putAll("B", Arrays.asList(easyLanguage.level2.r));
        easyWords.get(2).putAll("V", Arrays.asList(easyLanguage.level2.v));
        easyWords.put(3, HashMultimap.create());
        easyWords.get(3).putAll("S", Arrays.asList(easyLanguage.level3.n));
        easyWords.get(3).putAll("A", Arrays.asList(easyLanguage.level3.a));
        easyWords.get(3).putAll("B", Arrays.asList(easyLanguage.level3.r));
        easyWords.get(3).putAll("V", Arrays.asList(easyLanguage.level3.v));

        // Loading glossario

//        RadixTree<GlossarioEntry> glossario = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
        HashMap<String, GlossarioEntry> glossario = new HashMap<>();
        if (glossarioFileName != null) {
            logger.info("Loading glossario");
            try {
                JsonReader reader = new JsonReader(new FileReader(glossarioFileName));
                GlossarioEntry[] entries = gson.fromJson(reader, GlossarioEntry[].class);
                for (GlossarioEntry entry : entries) {
                    for (String form : entry.getForms()) {

                        if (parseGlossario) {
                            Annotation annotation = new Annotation(form);
                            pipeline.annotate(annotation);
                            StringBuffer stringBuffer = new StringBuffer();
                            List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
                            for (CoreLabel token : tokens) {
                                stringBuffer.append(token.get(CoreAnnotations.LemmaAnnotation.class)).append(" ");
                            }

                            String pos = entry.getPos();
                            String annotatedPos = tokens.get(0).get(CoreAnnotations.PartOfSpeechAnnotation.class);
                            if (pos == null || annotatedPos.substring(0, 1).equals("S")) {
                                glossario.put(stringBuffer.toString().trim(), entry);
                            }
                        }

                        glossario.put(form, entry);
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

//        for (KeyValuePair<String> pair : glossario.getKeyValuePairsForKeysStartingWith("bene")) {
//            System.out.println(pair);
//        }

        // Post stuff

        httpServer.getServerConfiguration().addHttpHandler(new SimpHandler(pipeline, glossario, easyWords), "/simp");

        httpServer.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "webdemo-ita-old/"), "/");
        httpServer.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "webdemo/"), "/lib/");

        // Fix
        // see: http://stackoverflow.com/questions/35123194/jersey-2-render-swagger-static-content-correctly-without-trailing-slash
//        httpServer.getServerConfiguration().addHttpHandler(
//                new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "webdemo/static/"), "/static/");

        try {
            httpServer.start();
            Thread.currentThread().join();
        } catch (Exception e) {
            logger.error("error running " + host + ":" + port);
        }
    }

    public static void main(String[] args) {

        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./tintop-server-ita")
                    .withHeader("Run the Tintop Server for Italian simplification")
                    .withOption("c", "config", "Configuration file", "FILE", CommandLine.Type.FILE_EXISTING, true,
                            false, false)
                    .withOption("p", "port", String.format("Host port (default %d)", DEFAULT_PORT), "NUM",
                            CommandLine.Type.INTEGER, true, false, false)
                    .withOption("h", "host", String.format("Host address (default %s)", DEFAULT_HOST), "NUM",
                            CommandLine.Type.STRING, true, false, false)
//                    .withOption(null, "properties", "Additional properties", "PROPS", CommandLine.Type.STRING, true,
//                            true, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            String host = cmd.getOptionValue("host", String.class, DEFAULT_HOST);
            Integer port = cmd.getOptionValue("port", Integer.class, DEFAULT_PORT);
            String configFile = cmd.getOptionValue("config", String.class);
//            File configFile = cmd.getOptionValue("config", File.class);
//            List<String> addProperties = cmd.getOptionValues("properties", String.class);

//            Properties additionalProps = new Properties();
//            for (String property : addProperties) {
//                try {
//                    additionalProps.load(new StringReader(property));
//                } catch (Exception e) {
//                    logger.warn(e.getMessage());
//                }
//            }

            SimpServer pipelineServer = new SimpServer(host, port, configFile);

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }
}
