package eu.fbk.dkm.pikes.tintop.util;

import ch.qos.logback.classic.Level;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Filters;
import eu.fbk.fcw.utils.corpus.Corpus;
import eu.fbk.fcw.utils.corpus.Sentence;
import eu.fbk.fcw.utils.corpus.Word;
import eu.fbk.dkm.pikes.depparseannotation.DepParseInfo;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by alessio on 26/02/15.
 */

public class ReparseConllStanford {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReparseConllStanford.class);

    public static void main(String[] args) {

        try {
            final eu.fbk.utils.core.CommandLine cmd = eu.fbk.utils.core.CommandLine
                    .parser()
                    .withName("./reparse-conll")
                    .withHeader(
                            "Parse a document in CoNLL format with Stanford Parser, then save it in CoNLL format again")
                    .withOption("i", "input", "Input file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE",
                            CommandLine.Type.FILE, true, false, true)
                    .withOption("k", "keep-loops", "Keep loops (by default they will be removed)")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("edu.stanford")).setLevel(Level.ERROR);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            boolean keepLoops = cmd.hasOption("keep-loops");

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            Properties stanfordProps = new Properties();
            stanfordProps.setProperty("annotators", "tokenize, ssplit, pos, parse");
            stanfordProps.setProperty("tokenize.whitespace", "true");
            stanfordProps.setProperty("ssplit.eolonly", "true");
            stanfordProps.setProperty("parse.keepPunct", "true");

            Corpus conll2009 = Corpus.readDocumentFromFile(inputFile, "conll2009");
            AtomicInteger removedSentences = new AtomicInteger(0);
            AtomicInteger totalSentences = new AtomicInteger(0);

            conll2009.getSentences().parallelStream().forEach((Sentence sentence) -> {
                totalSentences.incrementAndGet();
                StanfordCoreNLP pipeline = new StanfordCoreNLP(stanfordProps);

                StringBuilder stanfordSentenceBuilder = new StringBuilder();

                for (Word word : sentence) {
                    stanfordSentenceBuilder.append(" ").append(word.getForm().replaceAll("\\s+", "_"));
                }

                String stanfordSentence = stanfordSentenceBuilder.toString().trim();

                Annotation annotation = new Annotation(stanfordSentence);
                pipeline.annotate(annotation);

                CoreMap coreMap = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0);

                Tree tree = coreMap.get(TreeCoreAnnotations.TreeAnnotation.class);
                GrammaticalStructure grammaticalStructure = new EnglishGrammaticalStructure(tree,
                        Filters.acceptFilter(), new CollinsHeadFinder());
                SemanticGraph dependencies = SemanticGraphFactory.makeFromTree(grammaticalStructure);
//                SemanticGraph dependencies = SemanticGraphFactory
//                        .makeFromTree(grammaticalStructure, SemanticGraphFactory.Mode.BASIC,
//                                GrammaticalStructure.Extras.NONE, true, null);
                DepParseInfo info = new DepParseInfo(dependencies);

                for (Integer id : info.getDepParents().keySet()) {
                    sentence.getWords().get(id - 1).setDepParent(info.getDepParents().get(id));
                }
                for (Integer id : info.getDepLabels().keySet()) {
                    sentence.getWords().get(id - 1).setDepLabel(info.getDepLabels().get(id));
                }

                boolean writeIt = true;
                if (!keepLoops) {
                    writeIt = RemoveLoopsInConll.sentenceIsLoopFree(sentence);
                }

                if (writeIt) {
                    synchronized (writer) {
                        try {
                            writer.append(sentence.toConllString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    removedSentences.incrementAndGet();
                }
            });

            LOGGER.info("Total sentences: {}", totalSentences);
            LOGGER.info("Removed sentences: {}", removedSentences);

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
