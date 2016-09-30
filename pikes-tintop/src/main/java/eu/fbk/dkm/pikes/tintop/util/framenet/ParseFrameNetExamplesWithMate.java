package eu.fbk.dkm.pikes.tintop.util.framenet;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.AnnotatorUtils;
import eu.fbk.utils.core.CommandLine;
import is2fbk.data.SentenceData09;
import is2fbk.parser.Options;
import is2fbk.parser.Parser;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.pipeline.Pipeline;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * Created by alessio on 11/11/15.
 */

public class ParseFrameNetExamplesWithMate {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ParseFrameNetExamplesWithMate.class);

    public static Sentence createSentenceFromAnna33(SentenceData09 sentence, @Nullable List<String> lemmas) {
        ArrayList<String> forms = new ArrayList<>(Arrays.asList(sentence.forms));
        ArrayList<String> pos = new ArrayList<>(Arrays.asList(sentence.ppos));
        ArrayList<String> feats = new ArrayList<>(Arrays.asList(sentence.pfeats));
        forms.add(0, "<root>");
        pos.add(0, "<root>");
        feats.add(0, "<root>");

        if (lemmas == null) {
            if (sentence.lemmas != null) {
                lemmas = new ArrayList<>(Arrays.asList(sentence.lemmas));
            } else {
                lemmas = new ArrayList<>(Arrays.asList(sentence.plemmas));
            }
            lemmas.add(0, "<root>");
        }

        Sentence s;
        s = new Sentence(
                forms.toArray(new String[forms.size()]),
                lemmas.toArray(new String[lemmas.size()]),
                pos.toArray(new String[pos.size()]),
                feats.toArray(new String[feats.size()])
        );
        s.setHeadsAndDeprels(sentence.pheads, sentence.plabels);
        return s;
    }

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("parse-fn-with-mate")
                    .withHeader("Parse FrameNet example sentences with Stanford CoreNLP and Mate SRL parser")
                    .withOption("f", "fn-folder", "FrameNet LU folder", "FOLDER", CommandLine.Type.DIRECTORY_EXISTING,
                            true, false, true)
                    .withOption("a", "anna-model", "Anna model", "FILE", CommandLine.Type.FILE_EXISTING, true, false,
                            true)
                    .withOption("m", "mate-model", "Mate SRL model", "FILE", CommandLine.Type.FILE_EXISTING, true,
                            false, true)
                    .withOption("o", "output", "Output folder", "FOLDER", CommandLine.Type.DIRECTORY,
                            true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

//            String fnFolder = "/Users/alessio/Desktop/lu";
//            String mateModelParser = "/Users/alessio/Desktop/srl-20130917/models/retrain-anna-20140819.model";
//            String mateModelSrl = "/Users/alessio/Desktop/srl-20130917/models/retrain-srl-20140818.model";
            String fnFolder = cmd.getOptionValue("fn-folder", String.class);
            String mateModelParser = cmd.getOptionValue("anna-model", String.class);
            String mateModelSrl = cmd.getOptionValue("mate-model", String.class);

            File outputDir = cmd.getOptionValue("output", File.class);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            if (!outputDir.isDirectory()) {
                LOGGER.error("{} is not a directory", outputDir.getAbsolutePath());
                System.exit(1);
            }

            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
            props.setProperty("tokenize.whitespace", "true");
            props.setProperty("ssplit.eolonly", "true");

            LOGGER.info("Loading Stanford CoreNLP");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

            LOGGER.info("Loading Anna Parser");
            String[] arrayOfString = { "-model", mateModelParser };
            Options localOptions = new Options(arrayOfString);
            Parser mateParser = new Parser(localOptions);

            LOGGER.info("Loading Mate Srl");
            ZipFile zipFile;
            zipFile = new ZipFile(mateModelSrl);
            SemanticRoleLabeler mateSrl = Pipeline.fromZipFile(zipFile);
            zipFile.close();
            Language.setLanguage(Language.L.valueOf("eng"));

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            File nafFolderFile = new File(fnFolder);
            if (!nafFolderFile.exists()) {
                throw new IOException();
            }
            if (!nafFolderFile.isDirectory()) {
                throw new IOException();
            }

            HashSet<String> allSentences = new HashSet<>();
            int count = 0;

            long begin = System.currentTimeMillis();

            File[] listOfFiles = nafFolderFile.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                File file = listOfFiles[i];
                if (file.isFile()) {

                    if (!file.getName().endsWith(".xml")) {
                        continue;
                    }

                    LOGGER.debug("### File: {}", file.getName());

                    StringBuilder stringBuilder = new StringBuilder();

                    Document doc = dBuilder.parse(file);
                    doc.getDocumentElement().normalize();

                    NodeList nList;
                    nList = doc.getElementsByTagName("sentence");
                    for (int temp = 0; temp < nList.getLength(); temp++) {
                        Node nNode = nList.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            allSentences.add(eElement.getAttribute("ID"));
                            count++;

                            NodeList labelList = eElement.getElementsByTagName("text");
                            for (int j = 0; j < labelList.getLength(); j++) {
                                Node labelNode = labelList.item(j);
                                if (labelNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element labelElement = (Element) labelNode;
                                    String text = labelElement.getTextContent();

                                    LOGGER.debug("Text: {}", text);

                                    try {

                                        Annotation s = new Annotation(text);
                                        pipeline.annotate(s);

                                        List<CoreMap> sents = s.get(CoreAnnotations.SentencesAnnotation.class);
                                        for (CoreMap thisSent : sents) {
                                            List<String> forms = new ArrayList<>();
                                            List<String> poss = new ArrayList<>();
                                            List<String> lemmas = new ArrayList<>();

                                            forms.add("<root>");
                                            poss.add("<root>");
                                            lemmas.add("<root>");

                                            List<CoreLabel> tokens = thisSent
                                                    .get(CoreAnnotations.TokensAnnotation.class);
                                            for (CoreLabel token : tokens) {
                                                String form = token.get(CoreAnnotations.TextAnnotation.class);
                                                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                                                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);

                                                form = AnnotatorUtils.codeToParenthesis(form);
                                                lemma = AnnotatorUtils.codeToParenthesis(lemma);
                                                pos = AnnotatorUtils.codeToParenthesis(pos);

                                                forms.add(form);
                                                poss.add(pos);
                                                lemmas.add(lemma);
                                            }

                                            SentenceData09 localSentenceData091 = new SentenceData09();
                                            localSentenceData091.init(forms.toArray(new String[forms.size()]));
                                            localSentenceData091.setPPos(poss.toArray(new String[poss.size()]));

                                            // Anna
                                            SentenceData09 localSentenceData092 = mateParser
                                                    .apply(localSentenceData091);
                                            Sentence mateSentence = createSentenceFromAnna33(localSentenceData092,
                                                    lemmas);

                                            // Mate
                                            mateSrl.parseSentence(mateSentence);

                                            stringBuilder.append(mateSentence.toString());
                                            stringBuilder.append("\n\n");
                                        }
                                    } catch (Throwable t) {
                                        LOGGER.error("Error in file {}", file.getName());
                                        t.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    File outputFile = new File(
                            outputDir.getAbsolutePath() + File.separator + file.getName() + ".conll");
                    Files.write(stringBuilder.toString(), outputFile, Charsets.UTF_8);
                }
            }

            long end = System.currentTimeMillis();

            LOGGER.info("{} sentence(s) in {} millisecond(s)", count, (end - begin));

        } catch (Exception ex) {
            CommandLine.fail(ex);
        }
    }

}
