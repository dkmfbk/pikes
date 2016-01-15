package eu.fbk.dkm.pikes.tintop;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Filters;
import eu.fbk.dkm.pikes.tintop.annotators.DepParseInfo;
import eu.fbk.dkm.pikes.tintop.annotators.PikesAnnotations;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.*;

/**
 * Created by alessio on 26/02/15.
 */

public class StanfordTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StanfordTest.class);

    public static void main(String[] args) {

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, dbps");
		props.setProperty("customAnnotatorClass.dbps","eu.fbk.dkm.pikes.tintop.annotators.DBpediaSpotlightAnnotator");

        System.out.println("Loading other annotators");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        String onlyText = "G. W. Bush and Bono are very strong supporters of the fight of HIV in Africa.";
        Annotation annotation = new Annotation(onlyText);
        pipeline.annotate(annotation);

        List<CoreMap> sents = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap thisSent : sents) {
        }

        System.exit(1);

        // read some text in the text variable
        String text = "This is the worst movie I've ever watched!";

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

//		System.out.println(document.keySet());
//		System.exit(1);

        PrintWriter out;
        out = new PrintWriter(System.out);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences != null && sentences.size() > 0) {
            ArrayCoreMap sentence = (ArrayCoreMap) sentences.get(0);

//			out.println("Sentence's keys: ");
//			out.println(sentence.keySet());

//			Tree tree2 = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            System.out.println("Sentiment class name:");
//			System.out.println(sentence.get(SentimentCoreAnnotations.ClassName.class));
//			System.out.println(RNNCoreAnnotations.getPredictedClass(tree2));

            ArrayList<CoreLabel> indexedTokens = new ArrayList<>();
            HashMap<Word, CoreLabel> indexedWords = new HashMap<>();

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                indexedTokens.add(token);
            }
            System.out.println(indexedTokens);

            int i = -1;
//			for (Tree t : tree2.getLeaves()) {
//				i++;
//
//				List<Word> words = t.yieldWords();
//				for (Word w : words) {
//					indexedWords.put(w, indexedTokens.get(i));
//				}
//			}
//
//			Iterator<Tree> treeIterator = tree2.iterator();
//			while (treeIterator.hasNext()) {
//				Tree tree = treeIterator.next();
//				System.out.println(((CoreLabel) tree.label()).get(RNNCoreAnnotations.PredictedClass.class));
//			}
        }

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
//		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
//
//		for (
//				CoreMap sentence
//				: sentences)
//
//		{
//
//			Tree opinion = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
//			printTree(opinion, 0);
//
//		}

    }
}
