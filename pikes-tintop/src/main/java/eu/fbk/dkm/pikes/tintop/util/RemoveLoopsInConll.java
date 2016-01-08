package eu.fbk.dkm.pikes.tintop.util;

import eu.fbk.dkm.pikes.resources.util.corpus.Corpus;
import eu.fbk.dkm.pikes.resources.util.corpus.Sentence;
import eu.fbk.dkm.pikes.resources.util.corpus.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.actors.threadpool.AtomicInteger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by alessio on 28/12/15.
 */

public class RemoveLoopsInConll {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveLoopsInConll.class);

    public static boolean sentenceIsLoopFree(Sentence sentence) {
        java.util.List<Word> words = sentence.getWords();
        for (int i = 0; i < words.size(); i++) {
            List<Integer> ancestors = sentence.getAncestors(i);
            int size = ancestors.size();
            if (size > sentence.getWords().size()) {
                return false;
            }
        }

        return true;
    }

    public static void removeLoops(String inputFile, String outputFile) throws IOException {
        Corpus conll2009 = Corpus.readDocumentFromFile(inputFile, "conll2009");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

        AtomicInteger removedSentences = new AtomicInteger(0);
        AtomicInteger totalSentences = new AtomicInteger(0);

        conll2009.getSentences().parallelStream().forEach((Sentence sentence) -> {
            totalSentences.incrementAndGet();
            boolean loopFree = sentenceIsLoopFree(sentence);

            if (loopFree) {
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

        LOGGER.info("Removed {} sentences out of {}", removedSentences, totalSentences);

        writer.close();
    }

    public static void main(String[] args) {
        String inputFile = args[0];
        String outputFile = args[1];

        try {
            removeLoops(inputFile, outputFile);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
