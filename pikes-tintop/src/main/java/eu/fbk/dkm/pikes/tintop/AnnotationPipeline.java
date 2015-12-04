package eu.fbk.dkm.pikes.tintop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.machinelinking.api.client.Topic;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import eu.fbk.dkm.pikes.resources.*;
import eu.fbk.dkm.pikes.resources.ontonotes.VerbNetStatisticsExtractor;
import eu.fbk.dkm.pikes.tintop.annotators.AnnotatorUtils;
import eu.fbk.dkm.pikes.tintop.annotators.PikesAnnotations;
import eu.fbk.dkm.pikes.tintop.annotators.raw.AnnotatedEntity;
import eu.fbk.dkm.pikes.tintop.annotators.raw.Semafor;
import eu.fbk.dkm.pikes.tintop.util.NER2SSTtagset;
import eu.fbk.dkm.pikes.tintop.util.NerEntity;
import eu.fbk.dkm.pikes.tintop.util.PipelineConfiguration;
import ixa.kaflib.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import se.lth.cs.srl.corpus.Word;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 12:48
 * To change this template use File | Settings | File Templates.
 */

public class AnnotationPipeline {

    static Logger logger = Logger.getLogger(AnnotationPipeline.class.getName());

//    static String[] stanfordSentimentLabels = new String[] {
//            "Very negative",
//            "Negative",
//            "Neutral",
//            "Positive",
//            "Very Positive"
//    };

    static public LinkedHashSet<String> stanfordAnnotators = new LinkedHashSet<>();

    static {
        stanfordAnnotators.add("tokenize");
        stanfordAnnotators.add("ssplit");
        stanfordAnnotators.add("dbps");
        stanfordAnnotators.add("anna_pos");
        stanfordAnnotators.add("simple_pos");
        stanfordAnnotators.add("lemma");
        stanfordAnnotators.add("ukb");
        stanfordAnnotators.add("ner");
        stanfordAnnotators.add("parse");
        stanfordAnnotators.add("dcoref");
        stanfordAnnotators.add("conll_parse");
        stanfordAnnotators.add("mst_parse");
        stanfordAnnotators.add("mate");
        stanfordAnnotators.add("semafor");
    }

    private String annotators;
    private StanfordCoreNLP stanfordPipeline;
    private PredicateMatrix PM;
    private VerbNetStatisticsExtractor statisticsExtractor = null;

    boolean enablePM = false;
    boolean enableNafFilter = false;
    boolean enableOntoNotesFilter = false;

    private boolean modelsLoaded = false;

    private Properties config = new Properties();
    private Properties sPr = new Properties();

    public AnnotationPipeline(String configFile) throws IOException {
        config = PipelineConfiguration.getInstance(configFile).getProperties();
        enablePM = config.getProperty("enable_predicate_matrix", "0").equals("1");
        enableNafFilter = config.getProperty("enable_naf_filter", "0").equals("1");
        enableOntoNotesFilter = config.getProperty("enable_on_filter", "0").equals("1");

        annotators = config.getProperty("stanford_annotators", "tokenize");
    }

    public Properties getConfig() {
        return config;
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
            head.label().setValue(head.label().toString() + ixa.kaflib.Tree.HEAD_MARK);
        }

        for (Tree child : node.children()) {
            addHeads(child, node, headFinder);
        }

    }

    public void loadModels() throws Exception {

        if (modelsLoaded) {
            return;
        }

        logger.info("Loading Stanford CoreNLP");
        sPr = new Properties();
        sPr.setProperty("annotators", annotators);

        sPr.setProperty("customAnnotatorClass.anna_pos", "eu.fbk.dkm.pikes.tintop.annotators.AnnaPosAnnotator");
        sPr.setProperty("customAnnotatorClass.simple_pos", "eu.fbk.dkm.pikes.tintop.annotators.SimplePosAnnotator");
        sPr.setProperty("customAnnotatorClass.ukb", "eu.fbk.dkm.pikes.tintop.annotators.UKBAnnotator");
        sPr.setProperty("customAnnotatorClass.conll_parse", "eu.fbk.dkm.pikes.tintop.annotators.AnnaParseAnnotator");
        sPr.setProperty("customAnnotatorClass.semafor", "eu.fbk.dkm.pikes.tintop.annotators.SemaforAnnotator");
        sPr.setProperty("customAnnotatorClass.dbps", "eu.fbk.dkm.pikes.tintop.annotators.DBpediaSpotlightAnnotator");
        sPr.setProperty("customAnnotatorClass.mate", "eu.fbk.dkm.pikes.tintop.annotators.MateSrlAnnotator");
        sPr.setProperty("customAnnotatorClass.mst_parse", "eu.fbk.dkm.pikes.tintop.annotators.MstParserAnnotator");

        Properties stanfordFromConfig = AnnotatorUtils.stanfordConvertedProperties(config, "stanford");
        sPr.putAll(stanfordFromConfig);

        stanfordPipeline = new StanfordCoreNLP(sPr);

        // Predicate Matrix

        if (enablePM) {
            logger.info("Loading Predicate Matrix");
            PM = new PredicateMatrix(config.getProperty("predicate_matrix"));
        }

        // NAF filter

        if (enableNafFilter) {
            logger.info("Loading WordNet for NAF filter");
            WordNet.setPath(config.getProperty("naf_filter_wordnet_path"));
            WordNet.init();
        }

        // OntoNotes

        if (enableOntoNotesFilter) {
            logger.info("Loading OntoNotes");
            statisticsExtractor = new VerbNetStatisticsExtractor();
//			statisticsExtractor.loadDir(config.getProperty("on_folder"));
//			statisticsExtractor.loadFrequencies();
            statisticsExtractor.loadFrequencies(config.getProperty("on_frequencies"));
        }

        modelsLoaded = true;
    }

    private KAFDocument parseAll(KAFDocument NAFdocument, HashSet<String> annotators) throws Exception {

        String text = NAFdocument.getRawText();
        text = StringEscapeUtils.unescapeHtml(text);
        LinguisticProcessor linguisticProcessor;

        String maxTextLen = getConfig().getProperty("max_text_len", "1000000");
        int limit = Integer.parseInt(maxTextLen);
        if (text.length() > limit) {
            throw new Exception(String.format("Input too long (%d chars, limit is %d)", text.length(), limit));
        }

        loadModels();

        // Load pipeline
        Properties thisSessionProps = new Properties(sPr);
//        if (annotators != null) {
//            StringBuffer thisAnnotators = new StringBuffer();
//            for (String a : stanfordAnnotators) {
//                if (annotators.contains(a)) {
//                    thisAnnotators.append(a).append(",");
//                }
//            }
//
//            String annoString = thisAnnotators.toString();
//            logger.info(annoString);
//            thisSessionProps.setProperty("annotators", annoString);
//        } else {
//            annotators = new HashSet<>();
//        }
        StanfordCoreNLP thisPipeline = new StanfordCoreNLP(thisSessionProps);

        // Stanford
        logger.info("Annotating with Stanford CoreNLP");
        linguisticProcessor = new LinguisticProcessor("text", "Stanford CoreNLP");
        linguisticProcessor.setBeginTimestamp();
        Annotation document = new Annotation(text);
        document.set(CoreAnnotations.DocDateAnnotation.class, NAFdocument.getFileDesc().creationtime);
        thisPipeline.annotate(document);
        linguisticProcessor.setEndTimestamp();
        NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
        Map<Integer, CorefChain> coreferenceGraph = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        // Add tmx0
        try {
            Timex3 tmx0 = NAFdocument.newTimex3("tmx0", "DATE");
            tmx0.setValue(NAFdocument.getFileDesc().creationtime.substring(0, 10));
        } catch (Exception e) {
            logger.warn("Document creation time is not included in the NAF headers");
        }

        logger.info("Getting information");
        TreeMap<Integer, Integer> sentIndexes = new TreeMap<>();
        int totTokens = 0;
        ArrayList<Term> allTerms = new ArrayList<>();

        HashMap<Integer, Integer> tokenFromStart = new HashMap<>();
        HashMap<Integer, Integer> tokenFromEnd = new HashMap<>();

        ArrayList<WF> allTokens = new ArrayList<>();
        HashMap<Integer, HashSet<AnnotatedEntity>> keywords = new HashMap<>();

        if (document.has(PikesAnnotations.DBpediaSpotlightAnnotations.class)) {
            for (AnnotatedEntity e : document.get(PikesAnnotations.DBpediaSpotlightAnnotations.class)) {
                int start = e.getStartIndex();
                if (keywords.get(start) == null) {
                    keywords.put(start, new HashSet<AnnotatedEntity>());
                }
                keywords.get(start).add(e);
                logger.debug("Annotated entity (DS): " + e);
            }
        }

        // Main loop
        List<CoreMap> get = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (int sentIndex = 0; sentIndex < get.size(); sentIndex++) {
            CoreMap stanfordSentence = get.get(sentIndex);
            List<CoreLabel> tokens = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);

            ArrayList<Term> terms = new ArrayList<>();
            ArrayList<String> ners = new ArrayList<>();

            sentIndexes.put(sentIndex, totTokens);
            totTokens += tokens.size();

            HashMap<Integer, TreeSet<Integer>> children = new HashMap<>();

            String lastNER = "O";
            ArrayList<NerEntity> entities = new ArrayList<>();

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel stanfordToken = tokens.get(i);
                String form = stanfordToken.get(CoreAnnotations.TextAnnotation.class);
                String lemma = stanfordToken.get(CoreAnnotations.LemmaAnnotation.class);
                String pos = stanfordToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);

                form = AnnotatorUtils.codeToParenthesis(form);
                if (lemma != null) {
                    lemma = AnnotatorUtils.codeToParenthesis(lemma);
                }
                pos = AnnotatorUtils.codeToParenthesis(pos);

                children.put(i, new TreeSet<Integer>());

                // Tokens
                WF thisWF = NAFdocument.newWF(form, stanfordToken.beginPosition(), sentIndex + 1);
                thisWF.setPara(1); //todo: Always set paragraph 1

                Integer tokenID = totTokens - tokens.size() + i;

                tokenFromStart.put(stanfordToken.beginPosition(), tokenID);
                tokenFromEnd.put(stanfordToken.beginPosition() + thisWF.getLength(), tokenID);
                allTokens.add(tokenID, thisWF);

                // Term
                Span<WF> thisWFSpan = KAFDocument.newWFSpan();
                thisWFSpan.addTarget(thisWF);
                Term thisTerm = NAFdocument.newTerm("open", lemma, pos, thisWFSpan);
                thisTerm.setMorphofeat(pos);

                // WordNet sense
                String wnSense = stanfordToken.get(PikesAnnotations.UKBAnnotation.class);
                if (wnSense != null) {
                    thisTerm.setWordnetSense(stanfordToken.get(PikesAnnotations.UKBAnnotation.class));
                }

                // Simple POS
                String simplePos = stanfordToken.get(PikesAnnotations.SimplePosAnnotation.class);
                if (simplePos == null) {
                    simplePos = "O";
                }
                thisTerm.setPos(simplePos);

                terms.add(thisTerm);
                allTerms.add(thisTerm);

                String ne = stanfordToken.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                String normVal = stanfordToken.getString(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class);
                if (ne != null) {
                    if (ne.equals("O")) {
                        ners.add("0");
                    } else {

                        // Alternative string for SST
                        String alt = NER2SSTtagset.tagset.get(ne);
                        if (alt == null) {
                            alt = "MISC";
                        }

                        if (ne.equals(lastNER)) {
                            entities.get(entities.size() - 1).setEndToken(i);
                            ners.add("I-" + alt);
                        } else {
                            NerEntity newEntity = new NerEntity(ne, i, normVal);
                            entities.add(newEntity);
                            ners.add("B-" + alt);
                        }
                    }
                    lastNER = ne;
                } else {
                    ners.add("0");
                }

            }

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel stanfordToken = tokens.get(i);

                // Dependencies
                int head = stanfordToken.get(CoreAnnotations.CoNLLDepParentIndexAnnotation.class);
                head++;
                String depRel = stanfordToken.get(CoreAnnotations.CoNLLDepTypeAnnotation.class);
                if (head != 0) {
                    Term from = terms.get(head - 1);
                    Term to = terms.get(i);
                    NAFdocument.newDep(from, to, depRel);
                }

                List<Word> toRoot = Word.pathToRoot(stanfordToken.get(PikesAnnotations.MateTokenAnnotation.class));
                for (Word w : toRoot) {
                    int id = w.getIdx() - 1;
                    if (id < 0) {
                        continue;
                    }
                    children.get(id).add(i);
                }

            }

            // Opinion

//            boolean includeNeutral = config.getProperty("stanford_include_neutral", "0").equals("1");
//
//            Tree sentimentTree = stanfordSentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
//            if (sentimentTree != null) {
//                HashMap<edu.stanford.nlp.ling.Word, Term> indexedWords = new HashMap<>();
//                int wordIndex = -1;
//                for (Tree t : sentimentTree.getLeaves()) {
//                    wordIndex++;
//                    List<edu.stanford.nlp.ling.Word> words = t.yieldWords();
//                    for (edu.stanford.nlp.ling.Word w : words) {
//                        indexedWords.put(w, terms.get(wordIndex));
//                    }
//                }
//
//                for (Tree tree : sentimentTree) {
//
//                    Integer predictedClass;
//                    try {
//                        predictedClass = RNNCoreAnnotations.getPredictedClass(tree);
//                    } catch (Exception e) {
//                        continue;
//                    }
//
//                    if (predictedClass == null) {
//                        continue;
//                    }
//
//                    if (!includeNeutral && predictedClass == 2) {
//                        continue;
//                    }
//
//                    Span<Term> treeSpan = KAFDocument.newTermSpan();
//                    for (edu.stanford.nlp.ling.Word word : tree.yieldWords()) {
//                        treeSpan.addTarget(indexedWords.get(word));
//                    }
//
//                    Opinion opinion = NAFdocument.createOpinion();
//                    opinion.setLabel("stanford-sentiment");
//                    Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(treeSpan);
//                    opinionExpression.setPolarity(stanfordSentimentLabels[predictedClass]);
//
//                    NumberFormat nf = NumberFormat.getNumberInstance();
//                    nf.setMaximumFractionDigits(2);
//
//                    SimpleMatrix predictions = RNNCoreAnnotations.getPredictions(tree);
//                    StringBuffer stringBuffer = new StringBuffer();
//                    stringBuffer.append(nf.format(predictions.get(0)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(1)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(2)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(3)));
//                    stringBuffer.append("|");
//                    stringBuffer.append(nf.format(predictions.get(4)));
//                    opinionExpression.setStrength(stringBuffer.toString());
//                }
//            }

            // Entities

            for (NerEntity entity : entities) {

                int startIndex = terms.get(entity.getStartToken()).getWFs().get(0).getOffset();
                int endIndex = terms.get(entity.getEndToken()).getWFs()
                        .get(terms.get(entity.getEndToken()).getWFs().size() - 1).getOffset() +
                        terms.get(entity.getEndToken()).getWFs()
                                .get(terms.get(entity.getEndToken()).getWFs().size() - 1).getLength();

                logger.debug("Stanford NER entity: " + entity + "");
                logger.debug(String.format("Stanford NER entity: %s (from %d to %d)", entity.getLabel(), startIndex,
                        endIndex));

                Span<Term> thisTermSpan = KAFDocument.newTermSpan();
                Span<WF> thisWFSpan = KAFDocument.newWFSpan();

                for (int i = entity.getStartToken(); i <= entity.getEndToken(); i++) {
                    thisTermSpan.addTarget(terms.get(i));
                    thisWFSpan.addTargets(terms.get(i).getWFs());
                }

                List<Span<Term>> thisTermList = new LinkedList<>();
                List<Span<WF>> thisWFList = new LinkedList<>();

                thisTermList.add(thisTermSpan);
                thisWFList.add(thisWFSpan);

                Entity thisEntity;
                Timex3 thisTimex;

                switch (entity.getLabel()) {
                case "PERSON":
                case "LOCATION":
                case "ORGANIZATION":
                case "MISC":
                case "MONEY":
                case "PERCENT":
                    thisEntity = NAFdocument.newEntity(thisTermList);
                    thisEntity.setType(entity.getLabel());

                    AnnotatedEntity e = null;

                    // Normalized value
                    if (entity.getNormalizedValue() != null && entity.getNormalizedValue().length() > 0) {
                        thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));
                    }

                    HashSet<AnnotatedEntity> possibleEntities = keywords.get(startIndex);
                    if (possibleEntities != null) {
                        for (AnnotatedEntity loopEntity : possibleEntities) {
                            if (loopEntity.getEndIndex() != endIndex) {
                                continue;
                            }
                            if (e == null || e.getRel() < loopEntity.getRel()) {
                                e = loopEntity;
                            }
                        }
                    }

                    if (e != null) {
                        ExternalRef ext = NAFdocument.newExternalRef(e.getSource(), e.getLink());
                        ext.setConfidence(e.getRel());
                        thisEntity.addExternalRef(ext);
                    }

                    break;

                case "NUMBER":
                    thisEntity = NAFdocument.newEntity(thisTermList);
                    thisEntity.setType("CARDINAL");
                    thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));
                    break;

                case "ORDINAL":
                    thisEntity = NAFdocument.newEntity(thisTermList);
                    thisEntity.setType("ORDINAL");
                    thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));
                    break;

                case "DATE":
                case "TIME":
                    thisTimex = NAFdocument.newTimex3(thisWFSpan, entity.getLabel());
                    thisTimex.setValue(entity.getNormalizedValue());
                    break;

                case "DURATION":
                    thisTimex = NAFdocument.newTimex3(thisWFSpan, entity.getLabel());
                    thisTimex.setValue(entity.getNormalizedValue());
                    break;

                default:
                    logger.debug(entity.getLabel());
                }
            }

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel stanfordToken = tokens.get(i);

                se.lth.cs.srl.corpus.Predicate predicate = stanfordToken.get(PikesAnnotations.MateAnnotation.class);
                if (predicate != null) {
                    Span<Term> thisTermSpan = KAFDocument.newTermSpan();
                    Term thisTerm = terms.get(predicate.getIdx() - 1);
                    String tmpSense = predicate.getSense();

                    thisTermSpan.addTarget(thisTerm);

                    Predicate newPred = NAFdocument.newPredicate(thisTermSpan);
                    newPred.setSource("mate");

                    ExternalRef e;
                    // If it's a verb -> PropBank, if it's a noun -> NomBank
                    if (thisTerm.getPos().equals("V")) {
                        e = NAFdocument.newExternalRef("PropBank", tmpSense);
                        e.setSource("mate");
                    } else {
                        e = NAFdocument.newExternalRef("NomBank", tmpSense);
                        e.setSource("mate");
                    }
                    newPred.addExternalRef(e);

                    String sense = null;
                    if (thisTerm.getPos().equals("V")) {
                        sense = tmpSense;
                    } else {
                        // check NomBank
                        NomBank.Roleset roleset = NomBank.getRoleset(tmpSense);
                        try {
                            sense = roleset.getPBId();
                        } catch (Exception ex) {
                            logger.debug(ex.getMessage());
                        }
                    }

                    ArrayList<String> vnClasses = new ArrayList<>();
                    ArrayList<String> fnFrames = new ArrayList<>();

                    if (enablePM) {
                        if (sense != null && sense.length() > 0) {

                            HashSet<String> vnToAdd = new HashSet<>();
                            String vnFinal = null;

                            // VerbNet
                            vnClasses = PM.getVNClasses(sense);
                            if (!vnClasses.isEmpty()) {
                                if (vnClasses.size() == 1 || !enableOntoNotesFilter) {
                                    for (String vnClass1 : vnClasses) {
                                        vnToAdd.add(vnClass1);
                                        vnFinal = vnClass1;
                                    }
                                } else {
                                    Integer value = 0;

                                    for (String vnClass : vnClasses) {
                                        Integer thisValue = statisticsExtractor.getVnTotals().get(vnClass);
                                        thisValue = thisValue == null ? 0 : thisValue;
                                        if (thisValue >= value) {
                                            vnFinal = vnClass;
                                            value = thisValue;
                                        }
                                    }

                                    // Reset the list of classes
                                    vnClasses = new ArrayList<>();

                                    if (vnFinal != null) {
                                        vnToAdd.add(vnFinal);
                                        vnClasses.add(vnFinal);
                                    }
                                }
                            }
                            ArrayList<String> vnSubClasses = PM.getVNSubClasses(sense);
                            if (!vnSubClasses.isEmpty()) {
                                for (String vnSubClass1 : vnSubClasses) {
                                    for (String vnClass : vnClasses) {
                                        if (!vnSubClass1.startsWith(vnClass)) {
                                            continue;
                                        }

                                        vnToAdd.add(vnSubClass1);

                                        // Remove upper class
                                        if (vnFinal != null) {
                                            if (vnSubClass1.startsWith(vnFinal)) {
                                                vnToAdd.remove(vnFinal);
                                            }
                                        }
                                    }
                                }
                            }

                            for (String vnClass1 : vnToAdd) {
                                ExternalRef vnClass = NAFdocument.newExternalRef("VerbNet", vnClass1);
                                newPred.addExternalRef(vnClass);
                            }

                            // FrameNet
                            fnFrames = PM.getFNFrames(sense);

							if (enableOntoNotesFilter) {
								HashSet<String> possibleFrames = new HashSet<>();
								for (String vnClass : vnClasses) {
									possibleFrames.addAll(PM.getVNClassesToFN(vnClass));
								}

//								System.out.println("vnClasses: " + vnClasses);
//								System.out.println("fnFrames (before): " + fnFrames);
								fnFrames.retainAll(possibleFrames);
//								System.out.println("fnFrames (after): " + fnFrames);
//								System.out.println("Possible frames: " + possibleFrames);
							}

                            if (!fnFrames.isEmpty()) {
                                if (fnFrames.size() == 1 || !enableOntoNotesFilter) {
                                    for (String fnFrame1 : fnFrames) {
                                        ExternalRef fnFrame = NAFdocument.newExternalRef("FrameNet", fnFrame1);
                                        newPred.addExternalRef(fnFrame);
                                    }
                                } else {
                                    Integer value = 0;
                                    String fnFinal = null;

                                    for (String fnFrame : fnFrames) {
                                        Integer thisValue = statisticsExtractor.getFnTotals()
                                                .get(fnFrame.toLowerCase());
                                        thisValue = thisValue == null ? 0 : thisValue;
                                        if (thisValue >= value) {
                                            fnFinal = fnFrame;
                                            value = thisValue;
                                        }
                                    }

                                    // Reset the list of frames
                                    fnFrames = new ArrayList<>();

                                    if (fnFinal != null) {
                                        ExternalRef fnFrame = NAFdocument.newExternalRef("FrameNet", fnFinal);
                                        newPred.addExternalRef(fnFrame);
                                        fnFrames.add(fnFinal);
                                    }
                                }
                            }

                            // PropBank
                            ArrayList<String> pbPredicates = PM.getPBPredicates(sense);
                            if (!pbPredicates.isEmpty()) {
                                for (String pbPredicate1 : pbPredicates) {
                                    ExternalRef pbPredicate = NAFdocument.newExternalRef("PropBank", pbPredicate1);
                                    newPred.addExternalRef(pbPredicate);
                                }
                            }

                            // ESO
                            ArrayList<String> esoClasses = PM.getESOClasses(sense);
                            if (!esoClasses.isEmpty()) {
                                for (String esoClass1 : esoClasses) {
                                    ExternalRef esoClass = NAFdocument.newExternalRef("ESO", esoClass1);
                                    newPred.addExternalRef(esoClass);
                                }
                            }

                            // EventType
                            ArrayList<String> eventTypes = PM.getEventTypes(sense);
                            if (!eventTypes.isEmpty()) {
                                for (String eventType1 : eventTypes) {
                                    ExternalRef eventType = NAFdocument.newExternalRef("EventType", eventType1);
                                    newPred.addExternalRef(eventType);
                                }
                            }

                            // WordNet
                            ArrayList<String> wnSenses = PM.getWNSenses(sense);
                            if (!wnSenses.isEmpty()) {
                                for (String wnSense1 : wnSenses) {
                                    ExternalRef wnSense = NAFdocument.newExternalRef("WordNet", wnSense1);
                                    newPred.addExternalRef(wnSense);
                                }
                            }

                        }
                    }

                    for (Word w : predicate.getArgMap().keySet()) {
                        Span<Term> thisTermSpanForRole = KAFDocument.newTermSpan();
                        for (int k : children.get(w.getIdx() - 1)) {
                            thisTermSpanForRole.addTarget(terms.get(k));
                        }
                        thisTermSpanForRole.setHead(terms.get(w.getIdx() - 1));

                        String argument = predicate.getArgMap().get(w);
                        Predicate.Role newRole = NAFdocument.newRole(newPred, argument, thisTermSpanForRole);

                        if (enablePM) {

                            // VerbNet
                            ArrayList<String> vnThematicRoles = PM.getVNThematicRoles(sense + ":" + argument);
                            if (!vnThematicRoles.isEmpty()) {
                                for (String vnThematicRole1 : vnThematicRoles) {
                                    if (!enableOntoNotesFilter) {
                                        ExternalRef vnThematicRole = NAFdocument
                                                .newExternalRef("VerbNet", vnThematicRole1);
                                        newRole.addExternalRef(vnThematicRole);
                                    } else {
                                        String[] parts = vnThematicRole1.split("@");
                                        if (vnClasses.contains(parts[0])) {
                                            ExternalRef vnThematicRole = NAFdocument
                                                    .newExternalRef("VerbNet", vnThematicRole1);
                                            newRole.addExternalRef(vnThematicRole);
                                        }
                                    }
                                }
                            }

                            // FrameNet
                            ArrayList<String> fnFrameElements = PM.getFNFrameElements(sense + ":" + argument);
                            if (!fnFrameElements.isEmpty()) {
                                for (String fnFrameElement1 : fnFrameElements) {
                                    if (!enableOntoNotesFilter) {
                                        ExternalRef fnFrameElement = NAFdocument
                                                .newExternalRef("FrameNet", fnFrameElement1);
                                        newRole.addExternalRef(fnFrameElement);
                                    } else {
                                        String[] parts = fnFrameElement1.split("@");
                                        if (fnFrames.contains(parts[0])) {
                                            ExternalRef fnFrameElement = NAFdocument
                                                    .newExternalRef("FrameNet", fnFrameElement1);
                                            newRole.addExternalRef(fnFrameElement);
                                        }
                                    }
                                }
                            }

                            // PropBank
                            ArrayList<String> pbArguments = PM.getPBArguments(sense + ":" + argument);
                            if (!pbArguments.isEmpty()) {
                                for (String pbArgument1 : pbArguments) {
                                    ExternalRef pbArgument = NAFdocument.newExternalRef("PropBank", pbArgument1);
                                    newRole.addExternalRef(pbArgument);
                                }
                            }

                            // ESO
                            ArrayList<String> esoRoles = PM.getESORoles(sense + ":" + argument);
                            if (!esoRoles.isEmpty()) {
                                for (String esoRole1 : esoRoles) {
                                    ExternalRef esoRole = NAFdocument.newExternalRef("ESO", esoRole1);
                                    newRole.addExternalRef(esoRole);
                                }
                            }
                        }

                        newPred.addRole(newRole);
                    }

                }
            }

            SemaforParseResult semaforParseResult = stanfordSentence.get(PikesAnnotations.SemaforAnnotation.class);
            ObjectMapper mapper = new ObjectMapper();
            Semafor.SemaforResponse semaforResponse = mapper
                    .readValue(semaforParseResult.toJson(), Semafor.SemaforResponse.class);
            for (Semafor.SemaforFrame semaforFrame : semaforResponse.getFrames()) {
                Semafor.SemaforAnnotation semaforTarget = semaforFrame.getTarget();
                if (semaforTarget == null) {
                    continue;
                }
                String frameName = semaforTarget.getName();

                if (semaforTarget.getSpans().size() == 0) {
                    continue;
                }
                if (semaforFrame.getAnnotationSets().size() == 0) {
                    continue;
                }

                Semafor.SemaforSpan semaforSpan = semaforTarget.getSpans().get(0);
                Semafor.SemaforSet semaforAnnotation = semaforFrame.getAnnotationSets().get(0);

                Span<Term> termSpan = KAFDocument.newTermSpan();
                for (int i = semaforSpan.getStart(); i < semaforSpan.getEnd(); i++) {
                    termSpan.addTarget(terms.get(i));
                }

                if (termSpan.size() == 0) {
                    continue;
                }

                Predicate predicate = NAFdocument.newPredicate(termSpan);
                predicate.setSource("semafor");
                predicate.setConfidence(semaforAnnotation.getScore());
                predicate.addExternalRef(NAFdocument.createExternalRef("FrameNet", frameName));
                predicate.setId("f_" + predicate.getId());

                for (Semafor.SemaforAnnotation frameAnnotation : semaforAnnotation.getFrameElements()) {
                    Semafor.SemaforSpan roleSpan = frameAnnotation.getSpans().get(0);
                    String roleName = frameAnnotation.getName();

                    Span<Term> roleTermSpan = KAFDocument.newTermSpan();
                    for (int i = roleSpan.getStart(); i < roleSpan.getEnd(); i++) {
                        roleTermSpan.addTarget(terms.get(i));
                    }

                    if (roleTermSpan.size() == 0) {
                        continue;
                    }

                    Predicate.Role role = NAFdocument.newRole(predicate, "", roleTermSpan);
                    final Term head = NAFUtils.extractHead(NAFdocument, role.getSpan());
                    if (head != null) {
                        final Span<Term> newSpan = KAFDocument
                                .newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(
                                        NAFdocument.getTermsByDepAncestors(ImmutableList.of(head))));
                        role.setSpan(newSpan);
                    }
                    role.addExternalRef(
                            NAFdocument.createExternalRef("FrameNet", frameName + "@" + roleName));
                    predicate.addRole(role);
                }

            }

            // Constituency
            Tree tree = stanfordSentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            if (tree != null) {
                NAFdocument.addConstituencyString(tree.toString(), sentIndex + 1);
                try {
                    logger.debug("Tree: " + tree.toString());
                    addHeads(tree);
                    NAFdocument.addConstituencyFromParentheses(tree.toString(), sentIndex + 1);
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }
            }

        } // end sentences loop

        // Entities
        for (Integer startIndex : keywords.keySet()) {
            for (AnnotatedEntity e : keywords.get(startIndex)) {
                Integer startToken = tokenFromStart.get(e.getStartIndex());
                Integer endToken = tokenFromEnd.get(e.getEndIndex());
                Span<WF> span = KAFDocument.newWFSpan();
                if (startToken != null && endToken != null) {
                    for (int j = startToken; j <= endToken; j++) {
                        span.addTarget(allTokens.get(j));
                    }

                    try {
                        LinkedEntity linkedEntity = NAFdocument.newLinkedEntity(span);
                        linkedEntity.setConfidence(e.getRel());
                        linkedEntity.setReference(e.getLink());
                        linkedEntity.setResource(e.getSource());
                        for (Topic topic : e.getTopics()) {
                            linkedEntity.addTopic(new SimpleTopic(topic.getProbability(), topic.getLabel()));
                        }
                    } catch (Exception err) {
                        logger.error("Error on adding linkedEntity: " + err.getMessage());
//						err.printStackTrace();
                    }
                }
            }
        }

        // Coref

        // Loop through clusters
        if (coreferenceGraph != null) {
            for (Object c : coreferenceGraph.keySet()) {

                CorefChain chain = coreferenceGraph.get(c);
                Map<IntPair, Set<CorefChain.CorefMention>> mentionMap = chain.getMentionMap();

                // Skip coreference if its size is 1
                if (mentionMap.size() < 2) {
                    continue;
                }

                List<Span<Term>> mentions = new ArrayList<>();

                // Loop through sentences
                for (IntPair p : mentionMap.keySet()) {

                    // Loop through mentions
                    for (CorefChain.CorefMention m : mentionMap.get(p)) {

                        int sentenceStartTokenIndex = sentIndexes.get(m.sentNum - 1);
                        int start = sentenceStartTokenIndex + m.startIndex - 1;

                        Span<Term> thisTermSpan = KAFDocument.newTermSpan();
                        for (int i = start; i < start + m.endIndex - m.startIndex; i++) {
                            thisTermSpan.addTarget(allTerms.get(i));
                        }
                        if (!thisTermSpan.isEmpty()) {
                            mentions.add(thisTermSpan);
                        }

//					logger.info(m.animacy);
//					logger.info(m.gender);
//					logger.info(m.mentionSpan);
//					logger.info(m.mentionType);
//					logger.info(m.number);
                    }
                }

                if (mentions.size() > 0) {
                    NAFdocument.newCoref(mentions);
                }
            }
        }

        // NAF filter
        if (enableNafFilter) {
            logger.info("Applying NAF filter");
            linguisticProcessor = new LinguisticProcessor("naf-filter", "NAF filter");
            linguisticProcessor.setBeginTimestamp();
            try {
                NAFFilter.builder(false)
                        .withTermSenseCompletion(true).withSRLRoleLinking(false, false)
                        .withOpinionLinking(false, false).build()
                        .filter(NAFdocument);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            linguisticProcessor.setEndTimestamp();
            NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
        }

        logger.info("Parsing finished");
        return NAFdocument;
    }

    public KAFDocument parseFromNAF(KAFDocument NAFdocument, HashSet<String> annotators) throws Exception {

        NAFdocument = parseAll(NAFdocument, annotators);

        return NAFdocument;
    }

    public KAFDocument parseFromString(String textInNafFormat) throws Exception {
        return parseFromString(textInNafFormat, null);
    }

    public KAFDocument parseFromString(String textInNafFormat, HashSet<String> annotators) throws Exception {
        logger.debug("Parsing of NAF");

        InputStream is = new ByteArrayInputStream(textInNafFormat.getBytes());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        KAFDocument NAFdocument = KAFDocument.createFromStream(br);

        try {
            logger.info("Document: " + NAFdocument.getFileDesc().filename);
            logger.info("Title: " + NAFdocument.getFileDesc().title);
            logger.debug("Text: " + NAFdocument.getRawText());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        NAFdocument = parseAll(NAFdocument, annotators);

        return NAFdocument;
    }

}
