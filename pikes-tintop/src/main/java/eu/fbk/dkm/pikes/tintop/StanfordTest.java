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
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import eu.fbk.dkm.pikes.tintop.annotators.DepParseInfo;
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

	public static void printTree(Tree tree, int depth) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(StringUtils.repeat("-", depth));
		stringBuffer.append(" ");
		stringBuffer.append(tree.label());
		stringBuffer.append(" ");
		stringBuffer.append(tree.getLeaves());

		if (tree.isLeaf()) {
			stringBuffer.append(" [leaf]");
		}

		if (((CoreLabel) tree.label()).containsKey(RNNCoreAnnotations.PredictedClass.class)) {
			stringBuffer.append(" ");
			stringBuffer.append("Predicted Class: " + RNNCoreAnnotations.getPredictedClass(tree));
		}
		stringBuffer.append("|");
		stringBuffer.append(RNNCoreAnnotations.getNodeVector(tree));
		stringBuffer.append("|");
		stringBuffer.append(RNNCoreAnnotations.getPredictions(tree));

		System.out.println(stringBuffer.toString());
		for (Tree t : tree.children()) {
			printTree(t, depth + 1);
		}
	}

	public static void removeAts(Tree node) {
		node.label().setValue(node.label().value().replace("@", ""));
		for (Tree child : node.children()) {
			removeAts(child);
		}
	}

	public static void addHeads(Tree node) {
		addHeads(node, null, null);
	}

	public static void addHeads(Tree node, Tree parent, HeadFinder headFinder) {
		if (node == null || node.isLeaf()) {
			return;
		}

		if (headFinder == null) {
			headFinder = new CollinsHeadFinder();
		}

		Tree head = headFinder.determineHead(node, parent);
		if (!head.isLeaf()) {
			head.label().setValue(head.label().toString() + "=H");
		}

		for (Tree child : node.children()) {
			addHeads(child, node, headFinder);
		}

	}

	private static String space(int width) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < width; i++) {
			b.append(' ');
		}
		return b.toString();
	}

	// helper for toString()
	private static void recToString(IndexedWord curr, StringBuilder sb, int offset, Set<IndexedWord> used,
			SemanticGraph dependencies, HashMap<Integer, String> depLabels, HashMap<Integer, Integer> depParents) {
		used.add(curr);
		List<SemanticGraphEdge> edges = dependencies.outgoingEdgeList(curr);
		Collections.sort(edges);
		for (SemanticGraphEdge edge : edges) {
			IndexedWord target = edge.getTarget();
			depParents.put(target.index(), edge.getSource().index());
			depLabels.put(target.index(), edge.getRelation().toString());
			sb.append(space(2 * offset)).append("-> ").append(target).append(" (").append(edge.getRelation()).append(")\n");
			if (!used.contains(target)) { // recurse
				recToString(target, sb, offset + 1, used, dependencies, depLabels, depParents);
			}
		}
	}


	public static void main(String[] args) {

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, parse");
//		props.setProperty("annotators", "tokenize, ssplit, dbps");
//		props.setProperty("tokenize.whitespace", "true");
//		props.setProperty("ssplit.eolonly", "true");
//		props.setProperty("ssplit.newlineIsSentenceBreak", "true");

//		props.setProperty("customAnnotatorClass.anna_pos","org.fbk.dkm.nlp.pipeline.annotators.AnnaPosAnnotator");
//		props.setProperty("customAnnotatorClass.tt_pos","org.fbk.dkm.nlp.pipeline.annotators.TreeTaggerPosAnnotator");
//		props.setProperty("customAnnotatorClass.ukb","org.fbk.dkm.nlp.pipeline.annotators.UKBAnnotator");

//		props.setProperty("customAnnotatorClass.simple_pos", "org.fbk.dkm.nlp.pipeline.annotators.SimplePosAnnotator");
//		props.setProperty("customAnnotatorClass.conll_parse", "org.fbk.dkm.nlp.pipeline.annotators.AnnaParseAnnotator");
//		props.setProperty("customAnnotatorClass.dbps", "org.fbk.dkm.nlp.pipeline.annotators.DBpediaSpotlightAnnotator");
//		props.setProperty("customAnnotatorClass.conll_srl", "org.fbk.dkm.nlp.pipeline.annotators.MateSrlAnnotator");
//
//		props.setProperty("conll_parse.model", "/Users/alessio/Documents/tintop/retrain-anna-20140819.model");
//
//		props.setProperty("conll_srl.model", "/Users/alessio/Documents/tintop/retrain-srl-20140818.model");
//
//		props.setProperty("dbps.address", "https://knowledgestore2.fbk.eu/dbps/rest/annotate");
//		props.setProperty("dbps.use_proxy", "0");
//		props.setProperty("dbps.proxy_url", "proxy.fbk.eu");
//		props.setProperty("dbps.proxy_port", "3128");
//		props.setProperty("dbps.min_confidence", "0.33");
//		props.setProperty("dbps.timeout", "2000");

//		try {
//			ZipFile zipFile;
//			zipFile = new ZipFile(props.getProperty("conll_srl.model"));
//			SemanticRoleLabeler mateSrl = Pipeline.fromZipFile(zipFile);
//			zipFile.close();
//			Language.setLanguage(Language.L.valueOf("eng"));
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage());
//		}
//
//		System.exit(1);

//		props.setProperty("ukb.folder","ukb/");
//		props.setProperty("ukb.model","models/wnet30_wnet30g_rels.bin");
//		props.setProperty("ukb.dict","models/wnet30_dict.txt");

//		props.setProperty("anna_pos.model", "/Users/alessio/Desktop/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model");
//		props.setProperty("tt_pos.home", "/Users/alessio/Desktop/treetagger");
//		props.setProperty("tt_pos.model", "/Users/alessio/Desktop/treetagger/lib/english-utf8.par");

//		System.out.println("Load first annotator");
//		StanfordCoreNLP pipeline_pre = new StanfordCoreNLP(props);
//
//		// ---

		System.out.println("Loading other annotators");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);


		String onlyText = "G. W. Bush and Bono are very strong supporters of the fight of HIV in Africa. Their March 2002 meeting resulted in a 5 billion dollar aid.";
		Annotation s = new Annotation(onlyText);

		Annotation myDoc = new Annotation(s);
		pipeline.annotate(myDoc);

		List<CoreMap> sents = myDoc.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap thisSent : sents) {

			SemanticGraph dependencies = thisSent.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			DepParseInfo info = new DepParseInfo(dependencies);

			System.out.println(info.getDepParents().size());
			System.out.println(info.getDepLabels().size());

			System.out.println(dependencies);

//			System.out.println(dependencies.getFirstRoot().index());
//			System.out.println(dependencies.getChildren(dependencies.getFirstRoot()));
//			System.out.println(dependencies);

//			ArrayCoreMap sentenceCoreMap = (ArrayCoreMap) thisSent;
//			List<CoreLabel> tokens = sentenceCoreMap.get(CoreAnnotations.TokensAnnotation.class);
//			for (CoreLabel token : tokens) {
//				System.out.println(token);
//				System.out.println(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
//				System.out.println(token.get(CoreAnnotations.LemmaAnnotation.class));
//				System.out.println(token.get(PikesAnnotations.SimplePosAnnotation.class));
//				System.out.println(token.get(CoreAnnotations.CoNLLDepTypeAnnotation.class));
//				System.out.println(token.get(CoreAnnotations.CoNLLDepParentIndexAnnotation.class));
//				System.out.println(token.get(PikesAnnotations.DBpediaSpotlightAnnotation.class));
//				System.out.println();
//			}
		}

//			System.out.println(s);

//			pipeline.annotate(doc);
//
//			CoreLabel clToken = new CoreLabel();
//			clToken.setValue("They");
//			clToken.setWord("This");
//			clToken.setOriginalText(stringToken);
//			clToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, "VBG");
//
//			List<CoreMap> sents = doc.get(CoreAnnotations.SentencesAnnotation.class);
//			for (CoreMap s : sents) {
//				ArrayCoreMap sentence = (ArrayCoreMap) s;
//				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
//				for (CoreLabel token : tokens) {
//					System.out.println(token);
//					System.out.println(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
//				}
//			}

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
