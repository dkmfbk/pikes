package eu.fbk.dkm.pikes.tintop;

import cc.mallet.classify.Classifier;
import cc.mallet.types.Instance;
import cc.mallet.types.Labeling;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.machinelinking.api.client.*;
import com.machinelinking.api.client.Topic;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import eu.fbk.dkm.pikes.resources.*;
import eu.fbk.dkm.pikes.resources.ontonotes.VerbNetStatisticsExtractor;
import eu.fbk.dkm.pikes.tintop.annotators.AnnotatorUtils;
import eu.fbk.dkm.pikes.tintop.annotators.PikesAnnotations;
import eu.fbk.dkm.pikes.tintop.annotators.raw.AnnotatedEntity;
import eu.fbk.dkm.pikes.tintop.annotators.raw.DBpediaSpotlight;
import eu.fbk.dkm.pikes.tintop.annotators.raw.DBpediaSpotlightTag;
import eu.fbk.dkm.pikes.tintop.annotators.raw.Semafor;
import eu.fbk.dkm.pikes.tintop.old.CachedParsedText;
import eu.fbk.dkm.pikes.tintop.old.SST;
import eu.fbk.dkm.pikes.tintop.util.NER2SSTtagset;
import eu.fbk.dkm.pikes.tintop.util.NerEntity;
import eu.fbk.dkm.pikes.tintop.util.POStagset;
import eu.fbk.dkm.pikes.tintop.util.PipelineConfiguration;
import is2fbk.data.SentenceData09;
import is2fbk.parser.Options;
import is2fbk.parser.Parser;
import ixa.kaflib.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ejml.simple.SimpleMatrix;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.pipeline.Pipeline;

import javax.annotation.Nullable;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.zip.ZipFile;

//import eu.newsreader.eventcoreference.naf.EventCorefWordnetSimServer;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 12:48
 * To change this template use File | Settings | File Templates.
 */

public class AnnotationPipeline {

    static Logger logger = Logger.getLogger(AnnotationPipeline.class.getName());

    static String[] stanfordSentimentLabels = new String[] {
            "Very negative",
            "Negative",
            "Neutral",
            "Positive",
            "Very Positive"
    };

    static public LinkedHashSet<String> stanfordAnnotators = new LinkedHashSet<>();

    static {
        stanfordAnnotators.add("tokenize");
        stanfordAnnotators.add("ssplit");
        stanfordAnnotators.add("anna_pos");
        stanfordAnnotators.add("simple_pos");
        stanfordAnnotators.add("lemma");
        stanfordAnnotators.add("ukb");
        stanfordAnnotators.add("ner");
        stanfordAnnotators.add("parse");
        stanfordAnnotators.add("dcoref");
        stanfordAnnotators.add("sentiment");
        stanfordAnnotators.add("semafor");
    }

    private SemanticRoleLabeler mateSrl = null;
    private SemanticRoleLabeler mateSrlBe = null;
    private Parser mateParser = null;

    private String annotators;
    private StanfordCoreNLP stanfordPipeline;
    private APIClient ml;
    private DBpediaSpotlight dbp;
    private Classifier malletClassifier;
    private PredicateMatrix PM;
    private SST sst;
    private String maxSentLen;
    private VerbNetStatisticsExtractor statisticsExtractor = null;

    private Semafor semafor = null;
//	private EventCorefWordnetSimServer eventCorefWordnetSimServer;

    boolean enableDBPS = false;
    boolean enableML = false;
    boolean enableSST = false;
    boolean enableMateBe = false;
    boolean enablePM = false;
    boolean enableFactuality = false;
    boolean enableEventCoref = false;
    boolean enableNafFilter = false;
    boolean enableOntoNotesFilter = false;

    private boolean modelsLoaded = false;

    private Properties config = new Properties();
    private Properties stanfordProps = new Properties();

    public AnnotationPipeline(String configFile) throws IOException {
        config = PipelineConfiguration.getInstance(configFile).getProperties();
        enableDBPS = config.getProperty("enable_dbps", "0").equals("1");
        enableML = config.getProperty("enable_ml", "0").equals("1");
        enableSST = config.getProperty("enable_sst", "0").equals("1");
        enableMateBe = config.getProperty("enable_mate_be", "0").equals("1");
        enableFactuality = config.getProperty("enable_mallet", "0").equals("1");
        enablePM = config.getProperty("enable_predicate_matrix", "0").equals("1");
        enableEventCoref = config.getProperty("enable_event_coreference", "0").equals("1");
        enableNafFilter = config.getProperty("enable_naf_filter", "0").equals("1");
        enableOntoNotesFilter = config.getProperty("enable_on_filter", "0").equals("1");

        annotators = config.getProperty("stanford_annotators", "tokenize");
        maxSentLen = config.getProperty("stanford_maxsentlen", "200");
    }

    public Properties getConfig() {
        return config;
    }

    public static Sentence createSentenceFromAnna33(SentenceData09 sentence) {
        return createSentenceFromAnna33(sentence, null);
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

    public void loadModels() throws Exception {

        if (modelsLoaded) {
            return;
        }

        // Stanford (always enabled)

        logger.info("Loading Stanford CoreNLP");
        stanfordProps = new Properties();
        stanfordProps.setProperty("tokenize.whitespace",
                config.getProperty("stanford_token_whitespace", "0").equals("1") ? "true" : "false");
        stanfordProps.setProperty("ssplit.eolonly",
                config.getProperty("stanford_eolonly", "0").equals("1") ? "true" : "false");
        stanfordProps.setProperty("ssplit.newlineIsSentenceBreak",
                config.getProperty("stanford_break_on_newline", "always"));
        stanfordProps.setProperty("parse.maxlen", maxSentLen);
        stanfordProps.setProperty("annotators", annotators);
        stanfordProps.setProperty("anna_pos.model", config.getProperty("mate_model_pos", ""));
        stanfordProps.setProperty("dcoref.maxdist", "5");

        stanfordProps
                .setProperty("customAnnotatorClass.anna_pos", "eu.fbk.dkm.pikes.tintop.annotators.AnnaPosAnnotator");
        stanfordProps.setProperty("customAnnotatorClass.simple_pos",
                "eu.fbk.dkm.pikes.tintop.annotators.SimplePosAnnotator");
        stanfordProps.setProperty("customAnnotatorClass.ukb", "eu.fbk.dkm.pikes.tintop.annotators.UKBAnnotator");
        stanfordProps
                .setProperty("customAnnotatorClass.semafor", "eu.fbk.dkm.pikes.tintop.annotators.SemaforAnnotator");

        stanfordProps.setProperty("ukb.folder", "ukb/");
        stanfordProps.setProperty("ukb.model", "models/wnet30_wnet30g_rels.bin");
        stanfordProps.setProperty("ukb.dict", "models/wnet30_dict.txt");

//		stanfordProps.setProperty("dcoref.postprocessing", "true");
//		stanfordProps.setProperty("tokenize.options", "normalizeParentheses=false,normalizeOtherBrackets=false");
        stanfordPipeline = new StanfordCoreNLP(stanfordProps);

        // Mate (always enabled)

        logger.info("Loading Anna Parser");
        String[] arrayOfString = { "-model", config.getProperty("mate_model_parser") };
        Options localOptions = new Options(arrayOfString);
        mateParser = new Parser(localOptions);

        logger.info("Loading Mate Srl");
        ZipFile zipFile;
        zipFile = new ZipFile(config.getProperty("mate_model_srl"));
        mateSrl = Pipeline.fromZipFile(zipFile);
        zipFile.close();
        Language.setLanguage(Language.L.valueOf("eng"));

        if (enableMateBe) {
            logger.info("Loading mate model for be.*");
            zipFile = new ZipFile(config.getProperty("mate_model_be"));
            mateSrlBe = Pipeline.fromZipFile(zipFile);
            zipFile.close();
        }

        // ML

        if (enableML) {
            logger.info("Loading Machine Linking");
            ml = new APIClient(config.getProperty("ml_app_id"), config.getProperty("ml_app_key"));
        }

        // DBP spotlight

        if (enableDBPS) {
            logger.info("Loading DBpedia Spotlight");
            dbp = new DBpediaSpotlight();
        }

        // Mallet

        if (enableFactuality) {
            logger.info("Loading Mallet");
            String clFile = config.getProperty("mallet_model");
            try {
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(clFile)));
                malletClassifier = (Classifier) ois.readObject();
                ois.close();
            } catch (Exception e) {
                logger.error("Unable to load classifier: " + clFile);
            }

            malletClassifier.getInstancePipe().getDataAlphabet().stopGrowth();
            malletClassifier.getInstancePipe().getTargetAlphabet().stopGrowth();
        }

        // Predicate Matrix

        if (enablePM) {
            logger.info("Loading Predicate Matrix");
            PM = new PredicateMatrix(config.getProperty("predicate_matrix"));
        }

        // SST

        if (enableSST) {
            logger.info("Loading SST");
            sst = new SST(config.getProperty("sst_folder"));
        }

        // OntoNotes

        if (enableOntoNotesFilter) {
            logger.info("Loading OntoNotes");
            statisticsExtractor = new VerbNetStatisticsExtractor();
//			statisticsExtractor.loadDir(config.getProperty("on_folder"));
//			statisticsExtractor.loadFrequencies();
            statisticsExtractor.loadFrequencies(config.getProperty("on_frequencies"));
        }

        if (getConfig().getProperty("semafor.port") != null && getConfig().getProperty("semafor.host") != null) {
            semafor = new Semafor(getConfig().getProperty("semafor.host"),
                    Integer.parseInt(getConfig().getProperty("semafor.port")));
        }

        // Event coreference

//		if (enableEventCoref) {
//			logger.info("Loading event coreference module");
//			eventCorefWordnetSimServer = new EventCorefWordnetSimServer();
//			eventCorefWordnetSimServer.setSimthreshold(Double.parseDouble(config.getProperty("event_coref_simthreshold")));
//			eventCorefWordnetSimServer.setRelations(config.getProperty("event_coref_relations"));
//			eventCorefWordnetSimServer.setMethod(config.getProperty("event_coref_method"));
//			eventCorefWordnetSimServer.loadWordNet(config.getProperty("event_coref_model"));
//		}

        // NAF filter

        if (enableNafFilter) {
            logger.info("Loading WordNet for NAF filter");
            WordNet.setPath(config.getProperty("naf_filter_wordnet_path"));
            WordNet.init();
        }

        modelsLoaded = true;
    }

    private KAFDocument loadNafDocument(String input) throws Exception {
        KAFDocument NAFdocument = KAFDocument.createFromFile(new File(input));
        return NAFdocument;
    }

    private KAFDocument getNAF(CachedParsedText cache, KAFDocument NAFdocument, HashSet<String> annotators) {

        if (annotators == null) {
            annotators = new HashSet<>();
        }

        // Add tmx0
        try {
            Timex3 tmx0 = NAFdocument.newTimex3("tmx0", "DATE");
            tmx0.setValue(NAFdocument.getFileDesc().creationtime.substring(0, 10));
        } catch (Exception e) {
            logger.warn("Document creation time is not included in the NAF headers");
        }

        List<CoreMap> stanfordSentences = cache.getStanford();
        List<Sentence> mateSentences = cache.getMate();
        List<Sentence> mateSentencesBe = cache.getMateBe();
        Map<Integer, CorefChain> coreferenceGraph = cache.getCoreference();
        AnnotationResponse annotation = cache.getMl();
        List<DBpediaSpotlightTag> dbpediaAnnotation = cache.getDbpTags();

//		for (LinguisticProcessor l : cache.getLps()) {
//			String layer = l.getLayer();
//			NAFdocument.addLinguisticProcessor(layer, l);
//		}

        HashMap<Integer, HashSet<AnnotatedEntity>> keywords = new HashMap<>();

        // ML
        if (annotation != null) {
            for (Topic t : annotation.getTopics()) {
                NAFdocument.newTopic(t.getLabel(), t.getProbability());
            }
            for (Keyword k : annotation.getKeywords()) {
                for (NGram nGram : k.getNGrams()) {
                    AnnotatedEntity e = new AnnotatedEntity(k);
                    int start = nGram.getStart();
                    e.setStartIndex(start);
                    e.setEndIndex(nGram.getEnd());
                    if (keywords.get(start) == null) {
                        keywords.put(start, new HashSet<AnnotatedEntity>());
                    }
                    e.setTopics(Arrays.asList(k.getTopics()));
                    keywords.get(start).add(e);
                    logger.debug("Annotated entity (ML): " + e);
                }
            }
        }
        // DBP spotlight
        if (dbpediaAnnotation != null) {
            for (DBpediaSpotlightTag k : dbpediaAnnotation) {
                AnnotatedEntity e = new AnnotatedEntity(k);
                int start = e.getStartIndex();
                if (keywords.get(start) == null) {
                    keywords.put(start, new HashSet<AnnotatedEntity>());
                }
                keywords.get(start).add(e);
                logger.debug("Annotated entity (DS): " + e);
            }
        }

        // Main loop
        logger.info("Getting information");
        TreeMap<Integer, Integer> sentIndexes = new TreeMap<>();
        int totTokens = 0;
        ArrayList<Term> allTerms = new ArrayList<>();
        ArrayList<Instance> instances = new ArrayList<>();

        HashMap<Integer, Integer> tokenFromStart = new HashMap<>();
        HashMap<Integer, Integer> tokenFromEnd = new HashMap<>();

        ArrayList<WF> allTokens = new ArrayList<>();

        ArrayList<String> sentSSTs = new ArrayList<>();

        for (int sentIndex = 0; sentIndex < stanfordSentences.size(); sentIndex++) {

            CoreMap sentenceCoreMap = stanfordSentences.get(sentIndex);
            ArrayCoreMap stanfordSentence = (ArrayCoreMap) sentenceCoreMap;
            Sentence mateSentence = null;
            Sentence mateSentenceBe = null;

            try {
                mateSentence = mateSentences.get(sentIndex);
                mateSentenceBe = mateSentencesBe.get(sentIndex);
            } catch (Exception e) {
                // continue...
            }

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

                String wnSense = stanfordToken.get(PikesAnnotations.UKBAnnotation.class);
                if (wnSense != null) {
                    thisTerm.setWordnetSense(stanfordToken.get(PikesAnnotations.UKBAnnotation.class));
                }

                String simplePos = POStagset.tagset.get(pos);
                if (simplePos == null) {
                    simplePos = "O";
                }
                thisTerm.setPos(simplePos);

                terms.add(thisTerm);
                allTerms.add(thisTerm);

                // NER
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

            // Opinion

            boolean includeNeutral = config.getProperty("stanford_include_neutral", "0").equals("1");

            Tree sentimentTree = stanfordSentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
            if (sentimentTree != null) {
                HashMap<edu.stanford.nlp.ling.Word, Term> indexedWords = new HashMap<>();
                int wordIndex = -1;
                for (Tree t : sentimentTree.getLeaves()) {
                    wordIndex++;
                    List<edu.stanford.nlp.ling.Word> words = t.yieldWords();
                    for (edu.stanford.nlp.ling.Word w : words) {
                        indexedWords.put(w, terms.get(wordIndex));
                    }
                }

                for (Tree tree : sentimentTree) {

                    Integer predictedClass;
                    try {
                        predictedClass = RNNCoreAnnotations.getPredictedClass(tree);
                    } catch (Exception e) {
                        continue;
                    }

                    if (predictedClass == null) {
                        continue;
                    }

                    if (!includeNeutral && predictedClass == 2) {
                        continue;
                    }

                    Span<Term> treeSpan = KAFDocument.newTermSpan();
                    for (edu.stanford.nlp.ling.Word word : tree.yieldWords()) {
                        treeSpan.addTarget(indexedWords.get(word));
                    }

                    Opinion opinion = NAFdocument.createOpinion();
                    opinion.setLabel("stanford-sentiment");
                    Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(treeSpan);
                    opinionExpression.setPolarity(stanfordSentimentLabels[predictedClass]);

                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setMaximumFractionDigits(2);

                    SimpleMatrix predictions = RNNCoreAnnotations.getPredictions(tree);
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append(nf.format(predictions.get(0)));
                    stringBuffer.append("|");
                    stringBuffer.append(nf.format(predictions.get(1)));
                    stringBuffer.append("|");
                    stringBuffer.append(nf.format(predictions.get(2)));
                    stringBuffer.append("|");
                    stringBuffer.append(nf.format(predictions.get(3)));
                    stringBuffer.append("|");
                    stringBuffer.append(nf.format(predictions.get(4)));
                    opinionExpression.setStrength(stringBuffer.toString());
                }
            }

            // SST

            String thisSent = SST.getSentenceString(sentIndex, terms, ners);
            sentSSTs.add(thisSent);

            // Factuality

            for (int i = 0; i < terms.size(); i++) {
                if (terms.get(i).getPos().equals("V")) {

                    ArrayList<String> words = new ArrayList<>();
                    int start = Math.max(0, i - 4);
                    int end = Math.min(i + 3, terms.size() - 1);
                    for (int j = start; j < end; j++) {
                        words.add(terms.get(j).getForm());
                    }

                    String data = StringUtils.join(words, " ");
                    String key = "BOGUS";
                    Object o = terms.get(i);

                    Instance instance = new Instance(data, key, o, null);
                    instances.add(instance);

                }
            }

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
//						thisEntity = NAFdocument.newEntity(thisTermList);
//						thisEntity.setType(entity.getLabel());
//						thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));

                    thisTimex = NAFdocument.newTimex3(thisWFSpan, entity.getLabel());
                    thisTimex.setValue(entity.getNormalizedValue());
                    break;

                case "DURATION":
//						thisEntity = NAFdocument.newEntity(thisTermList);
//						thisEntity.setType(entity.getLabel());
//						thisEntity.addExternalRef(NAFdocument.createExternalRef("value", entity.getNormalizedValue()));

                    thisTimex = NAFdocument.newTimex3(thisWFSpan, entity.getLabel());
                    thisTimex.setValue(entity.getNormalizedValue());
                    break;

                default:
                    logger.debug(entity.getLabel());
                }
            }

            if (mateSentence != null) {

                // Parse
                for (int i = 0; i < tokens.size(); i++) {

                    Word mateToken = null;
                    mateToken = mateSentence.get(i + 1);

                    int head = mateToken.getHeadId();
                    if (head != 0) {
                        Term from = terms.get(head - 1);
                        Term to = terms.get(i);
                        NAFdocument.newDep(from, to, mateToken.getDeprel());
                    }

                    List<Word> toRoot = Word.pathToRoot(mateToken);
                    for (Word w : toRoot) {
                        int id = w.getIdx() - 1;
                        if (id < 0) {
                            continue;
                        }
                        children.get(id).add(i);
                    }
                }

                List<se.lth.cs.srl.corpus.Predicate> predicates = mateSentence.getPredicates();

                // Add predicates from be.*
                if (enableMateBe) {
                    for (se.lth.cs.srl.corpus.Predicate p : mateSentenceBe.getPredicates()) {
                        String sense = p.getSense();
                        if (!sense.startsWith("be.")) {
                            continue;
                        }
                        predicates.add(p);
                    }
//					predicates.addAll(mateSentenceBe.getPredicates());
                }

                for (se.lth.cs.srl.corpus.Predicate p : predicates) {
                    Span<Term> thisTermSpan = KAFDocument.newTermSpan();
                    Term thisTerm = terms.get(p.getIdx() - 1);
                    String tmpSense = p.getSense();

                    // Check if the lemma is "be" and the sense is not "be"
                    String lemma = thisTerm.getLemma();
                    if (lemma.equals("be") && !tmpSense.startsWith("be.")) {
                        continue;
                    }

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
                            logger.error(ex.getMessage());
                        }
                    }

                    ArrayList<String> vnClasses = new ArrayList<>();
                    ArrayList<String> fnFrames = new ArrayList<>();

                    if (enablePM && annotators.contains("cross_srl")) {
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

//							if (enableOntoNotesFilter) {
//								HashSet<String> possibleFrames = new HashSet<>();
//								for (String vnClass : vnClasses) {
//									possibleFrames.addAll(PM.getVNClassesToFN(vnClass));
//								}
//
//								System.out.println("vnClasses: " + vnClasses);
//								System.out.println("fnFrames (before): " + fnFrames);
//								fnFrames.retainAll(possibleFrames);
//								System.out.println("fnFrames (after): " + fnFrames);
//								System.out.println("Possible frames: " + possibleFrames);
//							}

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

                    for (Word w : p.getArgMap().keySet()) {
                        Span<Term> thisTermSpanForRole = KAFDocument.newTermSpan();
                        for (int k : children.get(w.getIdx() - 1)) {
                            thisTermSpanForRole.addTarget(terms.get(k));
                        }
                        thisTermSpanForRole.setHead(terms.get(w.getIdx() - 1));

                        String argument = p.getArgMap().get(w);
                        Predicate.Role newRole = NAFdocument.newRole(newPred, argument, thisTermSpanForRole);

                        if (enablePM && annotators.contains("cross_srl")) {

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

            // Semafor
            if (semafor != null) {

                StringBuffer conll = new StringBuffer();

                for (int i = 0; i < tokens.size(); i++) {

                    Word mateToken = mateSentence.get(i + 1);
                    CoreLabel stanfordToken = tokens.get(i);

                    String form = stanfordToken.get(CoreAnnotations.TextAnnotation.class);
                    String lemma = stanfordToken.get(CoreAnnotations.LemmaAnnotation.class);
                    String pos = stanfordToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);

//                    form = AnnotatorUtils.codeToParenthesis(form);
//                    if (lemma != null) {
//                        lemma = AnnotatorUtils.codeToParenthesis(lemma);
//                    }
//                    pos = AnnotatorUtils.codeToParenthesis(pos);

                    int head = mateToken.getHeadId();
                    String parseLabel = mateToken.getDeprel();

                    StringBuffer row = new StringBuffer();
                    row.append(i + 1);
                    row.append("\t");
                    row.append(form);
                    row.append("\t");
                    row.append("_"); // why not lemma?
                    row.append("\t");
                    row.append(pos);
                    row.append("\t");
                    row.append(pos);
                    row.append("\t");
                    row.append("_");
                    row.append("\t");
                    row.append(head);
                    row.append("\t");
                    row.append(parseLabel);
                    row.append("\t");
                    row.append("_");
                    row.append("\t");
                    row.append("_");

                    conll.append(row.toString()).append("\n");
                }

                try {
                    Semafor.SemaforResponse semaforResponse = semafor.tag(conll.toString());
//                    System.out.println(semaforResponse);

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
                                final Span<Term> newSpan = KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(
                                        NAFdocument.getTermsByDepAncestors(ImmutableList.of(head))));
                                role.setSpan(newSpan);
                            }
                            role.addExternalRef(NAFdocument.createExternalRef("FrameNet", frameName + "@" + roleName));
                            predicate.addRole(role);
                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }

            // Constituency
            Tree tree = stanfordSentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            if (tree != null) {
//				tree.label().setValue("TOP");
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

        // Factuality

        if (enableFactuality) {
            logger.info("Factuality");

            LinguisticProcessor linguisticProcessor = new LinguisticProcessor("factuality", "VUA Factuality");
            linguisticProcessor.setBeginTimestamp();

            Iterator<Instance> iterator = malletClassifier.getInstancePipe().newIteratorFrom(instances.iterator());
            while (iterator.hasNext()) {
                Instance instance = iterator.next();

                Labeling labeling = malletClassifier.classify(instance).getLabeling();

                Term t = (Term) instance.getName();
                Factuality factuality = NAFdocument.newFactuality(t);

                for (int location = 0; location < labeling.numLocations(); location++) {
                    factuality.addFactualityPart(labeling.labelAtLocation(location).toString(),
                            labeling.valueAtLocation(location));
                }
            }

            linguisticProcessor.setEndTimestamp();
            NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
//			NAFdocument.addLinguisticProcessor("factuality", "vua-factuality");
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

        // SST

        if (enableSST) {
            logger.info("Supersense tagger");
            LinguisticProcessor linguisticProcessor = new LinguisticProcessor("term", "SuperSense Tagger");
            linguisticProcessor.setBeginTimestamp();
            try {
                ArrayList<String> out = sst.runFromStrings(sentSSTs);
                SST.updateTerms(out, allTerms, NAFdocument);
//				NAFdocument.addLinguisticProcessor("sst", "Supersense Tagger");
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            linguisticProcessor.setEndTimestamp();
            NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
        }

        // Event coreference
//		if (enableEventCoref && annotators.contains("event_coref")) {
//			logger.info("Event coreference");
//			LinguisticProcessor linguisticProcessor = new LinguisticProcessor("ev-coref", "VUA Event Coreference");
//			linguisticProcessor.setBeginTimestamp();
//			eventCorefWordnetSimServer.process(NAFdocument);
//			linguisticProcessor.setEndTimestamp();
//			NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
//		}

        // NAF filter
        if (enableNafFilter && (annotators.contains("naf_filter") || annotators.size() == 0)) {
            logger.info("Applying NAF filter");
            LinguisticProcessor linguisticProcessor = new LinguisticProcessor("naf-filter", "NAF filter");
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

    public void saveParseToDisk(CachedParsedText cache, String saveFile) throws IOException {
        byte[] bytes = SerializationUtils.serialize(cache);
        FileUtils.writeByteArrayToFile(new File(saveFile), bytes);
    }

    public KAFDocument parseFromNAF(KAFDocument NAFdocument, HashSet<String> annotators) throws Exception {

        CachedParsedText cache = parse(NAFdocument, annotators);
        NAFdocument = getNAF(cache, NAFdocument, annotators);

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

        CachedParsedText cache = parse(NAFdocument, annotators);
        NAFdocument = getNAF(cache, NAFdocument, annotators);

        return NAFdocument;
    }

    public KAFDocument parseFromDisk(String nafDocument, String loadFile, HashSet<String> annotators) throws Exception {
        byte[] bytes = FileUtils.readFileToByteArray(new File(loadFile));
        CachedParsedText cache = SerializationUtils.deserialize(bytes);
        KAFDocument NAFdocument = loadNafDocument(nafDocument);
        NAFdocument = getNAF(cache, NAFdocument, annotators);
        return NAFdocument;
    }

    private CachedParsedText parse(KAFDocument NAFdocument) throws Exception {
        return parse(NAFdocument, null);
    }

    private CachedParsedText parse(KAFDocument NAFdocument, HashSet<String> annotators) throws Exception {
        String text = NAFdocument.getRawText();
        text = StringEscapeUtils.unescapeHtml(text);
        loadModels();

        Properties thisSessionProps = new Properties(stanfordProps);
        if (annotators != null) {
            StringBuffer thisAnnotators = new StringBuffer();
            for (String a : stanfordAnnotators) {
                if (annotators.contains(a)) {
                    thisAnnotators.append(a).append(",");
                }
            }

            String annoString = thisAnnotators.toString();
            logger.info(annoString);
            thisSessionProps.setProperty("annotators", annoString);
        } else {
            annotators = new HashSet<>();
        }
        StanfordCoreNLP thisPipeline = new StanfordCoreNLP(thisSessionProps);

        CachedParsedText cache = new CachedParsedText();

        // Stanford
        logger.info("Annotating with Stanford CoreNLP");
        Annotation document;
        synchronized (this) {

            LinguisticProcessor linguisticProcessor = new LinguisticProcessor("text", "Stanford CoreNLP");
            linguisticProcessor.setBeginTimestamp();

            document = new Annotation(text);
            // Set relative date for Timex
            try {
                document.set(CoreAnnotations.DocDateAnnotation.class, NAFdocument.getFileDesc().creationtime);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            thisPipeline.annotate(document);

            linguisticProcessor.setEndTimestamp();
            NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
        }

        List<CoreMap> stanfordSentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        Map<Integer, CorefChain> coreferenceGraph = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        cache.setCoreference(coreferenceGraph);
        cache.setStanford(stanfordSentences);

        // Mate
        if (annotators.contains("srl") || annotators.size() == 0) {
            logger.info("Mate tools");
            LinguisticProcessor linguisticProcessorAnna = new LinguisticProcessor("deps", "Anna 3.61");
            LinguisticProcessor linguisticProcessorMate = new LinguisticProcessor("srl", "Mate Tools");
            linguisticProcessorAnna.setBeginTimestamp();
            linguisticProcessorMate.setBeginTimestamp();
            LinguisticProcessor linguisticProcessorMateBe = new LinguisticProcessor("srl", "Mate Tools (be.xx model)");
            linguisticProcessorMateBe.setBeginTimestamp();

            cache.setMate(new ArrayList<Sentence>());
            cache.setMateBe(new ArrayList<Sentence>());
            for (CoreMap sentenceCoreMap : stanfordSentences) {

                ArrayCoreMap stanfordSentence = (ArrayCoreMap) sentenceCoreMap;

                List<String> forms = new ArrayList<>();
                List<String> poss = new ArrayList<>();
                List<String> lemmas = new ArrayList<>();

                forms.add("<root>");
                poss.add("<root>");
                lemmas.add("<root>");

                for (CoreLabel token : stanfordSentence.get(CoreAnnotations.TokensAnnotation.class)) {
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

                if (localSentenceData091.length() > Integer.parseInt(maxSentLen)) {
                    logger.info("Sentence is too long, skipping...");
                    cache.getMate().add(null);
                    cache.getMateBe().add(null);
                    continue;
                }

                Sentence mateSentence, mateSentenceBe;
                SentenceData09 localSentenceData092;

                // Anna
                synchronized (this) {
                    localSentenceData092 = mateParser.apply(localSentenceData091);
                }

                mateSentence = createSentenceFromAnna33(localSentenceData092, lemmas);
                mateSentenceBe = createSentenceFromAnna33(localSentenceData092, lemmas);

                // Mate
                synchronized (this) {
                    mateSrl.parseSentence(mateSentence);

                    if (enableMateBe) {
                        mateSrlBe.parseSentence(mateSentenceBe);
                    }
                }

                cache.getMate().add(mateSentence);
                cache.getMateBe().add(mateSentenceBe);
            }
            linguisticProcessorMateBe.setEndTimestamp();
            linguisticProcessorAnna.setEndTimestamp();
            linguisticProcessorMate.setEndTimestamp();
            NAFdocument.addLinguisticProcessor(linguisticProcessorMate.getLayer(), linguisticProcessorMate);
            if (enableMateBe) {
                NAFdocument.addLinguisticProcessor(linguisticProcessorMateBe.getLayer(), linguisticProcessorMateBe);
            }
            NAFdocument.addLinguisticProcessor(linguisticProcessorAnna.getLayer(), linguisticProcessorAnna);
        }

        // ML
        if (enableML) {
            logger.info("Machine Linking");
            try {
                Map<String, Object> mlOptions = new HashMap<>();
                mlOptions.put("min_weight", (float) Double.parseDouble(config.getProperty("ml_min_weight")));
                LinguisticProcessor linguisticProcessor = new LinguisticProcessor("linked-entities", "Machine Linking");
                linguisticProcessor.setBeginTimestamp();
                AnnotationResponse annotation = ml.annotate(text, mlOptions);
                linguisticProcessor.setEndTimestamp();
                NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);
                cache.setMl(annotation);
//				cache.addLinguisticProcessor(new LinguisticProcessor("linkedEntities", "machine-linking"));
            } catch (Exception e) {
                logger.error("Error on Machine Linking: " + e.getMessage());
                e.printStackTrace();
                cache.setMl(null);
            }
        }

        // DBP spotlight
        if (enableDBPS && (annotators.contains("linking") || annotators.size() == 0)) {
            logger.info("DBpedia Spotlight");
            List<DBpediaSpotlightTag> tags;
            try {
                LinguisticProcessor linguisticProcessor = new LinguisticProcessor("linked-entities",
                        "DBpedia Spotlight");
                linguisticProcessor.setBeginTimestamp();
                tags = dbp.tag(text);
                linguisticProcessor.setEndTimestamp();
                NAFdocument.addLinguisticProcessor(linguisticProcessor.getLayer(), linguisticProcessor);

                cache.setDbpTags(tags);
//				cache.addLinguisticProcessor(new LinguisticProcessor("linkedEntities", "dbpedia-spotlight"));
            } catch (Exception e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }

        return cache;
    }

    public KAFDocument parseFromText(String nafDocument, HashSet<String> annotators) throws Exception {
        KAFDocument NAFdocument = loadNafDocument(nafDocument);
        CachedParsedText cache = parse(NAFdocument);

        NAFdocument = getNAF(cache, NAFdocument, annotators);
        return NAFdocument;
    }

    public void saveFromText(String nafDocument, String saveFile) throws Exception {
        KAFDocument NAFdocument = loadNafDocument(nafDocument);
        CachedParsedText cache = parse(NAFdocument);

        saveParseToDisk(cache, saveFile);
    }

}
