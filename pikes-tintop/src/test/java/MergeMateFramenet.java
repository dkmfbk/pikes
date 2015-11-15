import ch.qos.logback.classic.Level;
import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.io.Files;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.dkm.pikes.resources.util.corpus.Corpus;
import eu.fbk.dkm.pikes.resources.util.corpus.Sentence;
import eu.fbk.dkm.pikes.resources.util.corpus.Srl;
import eu.fbk.dkm.pikes.resources.util.corpus.Word;
import eu.fbk.dkm.pikes.resources.util.fnlu.*;
import eu.fbk.dkm.pikes.resources.util.onsenses.Inventory;
import eu.fbk.dkm.pikes.resources.util.onsenses.Sense;
import eu.fbk.dkm.pikes.resources.util.onsenses.Wn;
import eu.fbk.dkm.pikes.resources.util.propbank.*;
import eu.fbk.dkm.pikes.resources.util.semlink.vnfn.SemLinkRoot;
import eu.fbk.dkm.pikes.resources.util.semlink.vnfnroles.Role;
import eu.fbk.dkm.pikes.resources.util.semlink.vnfnroles.SemLinkRolesRoot;
import eu.fbk.dkm.pikes.resources.util.semlink.vnfnroles.Vncls;
import eu.fbk.dkm.pikes.resources.util.semlink.vnpb.Argmap;
import eu.fbk.dkm.pikes.resources.util.semlink.vnpb.PbvnTypemap;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.FrequencyHashSet;
import net.didion.jwnl.data.PointerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 12/11/15.
 */

// *todo: verificare parentesi
// *todo: aggiungere semlink
// *todo: separare estrazioni
// *todo: verificare +
// *todo: dividere nofb su altro file

public class MergeMateFramenet {

    // Google docs: https://docs.google.com/document/d/1Uexv8352v0eI1Ij1I5j3U9cOHFNKPHlbqCSFTJcFTz4/edit#

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeMateFramenet.class);
    private static HashMap<String, String> lemmaToTransform = new HashMap();
    static final Pattern ONTONOTES_FILENAME_PATTERN = Pattern.compile("(.*)-([a-z]+)\\.xml");
    static final Pattern FRAMEBASE_PATTERN = Pattern
            .compile("^[^\\s]+\\s+[^\\s]+\\s+([^\\s]+)\\s+-\\s+(.+)\\s+-\\s+([a-z])#([0-9]+)$");
    static final Pattern LU_PATTERN = Pattern.compile("^(.*)\\.([a-z]+)$");
    static final Pattern PB_PATTERN = Pattern.compile("^verb-((.*)\\.[0-9]+)$");

    public enum OutputMapping {
        PBauto, NBauto, NBresource, PBtrivial
    }

    static {
        lemmaToTransform.put("cry+down(e)", "cry+down");
    }

    /**
     * Format the lemma for compatibility between datasets.
     * In particular, spaces and underscores are replaced by '+'
     *
     * @param lemmaFromPredicate the input lemma
     * @return the converted lemma
     */
    protected static String getLemmaFromPredicateName(String lemmaFromPredicate) {
        String lemma = lemmaFromPredicate.replace('_', '+')
                .replace(' ', '+');
        if (lemmaToTransform.keySet().contains(lemma)) {
            lemma = lemmaToTransform.get(lemma);
        }
        return lemma;
    }

    /**
     * Intersect collections of strings, ignoring empty sets
     *
     * @param collections input collection(s)
     * @return the resulting collection (intersection)
     */
    private static Collection<String> getIntersection(Collection<String>... collections) {
        return getIntersection(true, collections);
    }

    /**
     * Intersect collections of strings
     *
     * @param ignoreEmptySets select whether ignoring empty sets for the intersection
     * @param collections     input collection(s)
     * @return the resulting collection (intersection)
     */
    private static Collection<String> getIntersection(boolean ignoreEmptySets, Collection<String>... collections) {
        Collection<String> ret = null;
        for (Collection<String> collection : collections) {
            if (ignoreEmptySets && (collection == null || collection.size() == 0)) {
                continue;
            }
            if (ret == null) {
                ret = new HashSet<>();
                ret.addAll(collection);
            } else {
                ret.retainAll(collection);
            }
        }

        if (ret == null) {
            ret = new HashSet<>();
        }
        return ret;
    }

    private static ArrayList<Matcher> getPropBankPredicates(Roleset roleset) {

        ArrayList<Matcher> ret = new ArrayList<>();

        String source = roleset.getSource();
        if (source != null && source.length() > 0) {

            String[] parts = source.split("\\s+");
            for (String part : parts) {
                if (part.trim().length() == 0) {
                    continue;
                }

                Matcher matcher = PB_PATTERN.matcher(source);
                if (!matcher.find()) {
                    continue;
                }

                ret.add(matcher);
            }
        }

        return ret;
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./merger")
                    .withHeader("Transform linguistic resources into RDF")
                    .withOption("p", "propbank", "PropBank folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("w", "wordnet", "WordNet folder", "FOLDER", CommandLine.Type.DIRECTORY_EXISTING, true,
                            false, true)
                    .withOption("o", "ontonotes", "Ontonotes senses folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("l", "lu", "FrameNet LU folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption(null, "lu-parsed", "FrameNet LU folder (parsed, in CoNLL format)", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("f", "framebase", "FrameBase FrameNet-WordNet map", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("s", "semlink", "SemLink folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("n", "nombank", "NomBank folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption(null, "ignore-lemma", "ignore lemma information")
                    .withOption(null, "save-files", "serialize big files")
                    .withOption("O", "output", "Output file prefix", "PREFIX",
                            CommandLine.Type.STRING, true, false, true)
                    .withOption(null, "enable-sl4p",
                            "Enable extraction of frames using SemLink when framnet argument of roleset is empty")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            ((ch.qos.logback.classic.Logger) LOGGER).setLevel(Level.INFO);

            File pbFolder = cmd.getOptionValue("propbank", File.class);
            File nbFolder = cmd.getOptionValue("nombank", File.class);
            File wordnetFolder = cmd.getOptionValue("wordnet", File.class);
            File ontonotesFolder = cmd.getOptionValue("ontonotes", File.class);
            File framebaseFile = cmd.getOptionValue("framebase", File.class);
            File luFolder = cmd.getOptionValue("lu", File.class);
            File luParsedFolder = cmd.getOptionValue("lu-parsed", File.class);
            File semlinkFolder = cmd.getOptionValue("semlink", File.class);

            String outputPattern = cmd.getOptionValue("output", String.class);

            boolean enableSemLinkForPredicates = cmd.hasOption("enable-sl4p");
            boolean saveFiles = cmd.hasOption("save-files");

            boolean ignoreLemmaInFrameBaseMappings = cmd.hasOption("ignore-lemma");

            // Start

            Integer max = null;

            WordNet.setPath(wordnetFolder.getAbsolutePath());
            WordNet.init();

            JAXBContext fnContext = JAXBContext.newInstance(Frameset.class);
            Unmarshaller fnUnmarshaller = fnContext.createUnmarshaller();

            JAXBContext onContext = JAXBContext.newInstance(Inventory.class);
            Unmarshaller onUnmarshaller = onContext.createUnmarshaller();

            JAXBContext luContext = JAXBContext.newInstance(LexUnit.class);
            Unmarshaller luUnmarshaller = luContext.createUnmarshaller();

            JAXBContext semlinkContext = JAXBContext.newInstance(SemLinkRoot.class);
            Unmarshaller semlinkUnmarshaller = semlinkContext.createUnmarshaller();

            JAXBContext semlinkRolesContext = JAXBContext.newInstance(SemLinkRolesRoot.class);
            Unmarshaller semlinkRolesUnmarshaller = semlinkRolesContext.createUnmarshaller();

            JAXBContext semlinkPbContext = JAXBContext.newInstance(PbvnTypemap.class);
            Unmarshaller semlinkPbUnmarshaller = semlinkPbContext.createUnmarshaller();

            // SemLink

            LOGGER.info("Loading SemLink");
            File semlinkFile;

            semlinkFile = new File(semlinkFolder.getAbsolutePath() + File.separator + "vn-pb" + File.separator
                    + "vnpbMappings");
            PbvnTypemap semLinkPb = (PbvnTypemap) semlinkPbUnmarshaller.unmarshal(semlinkFile);

            HashMultimap<String, String> verbnetToPropbank = HashMultimap.create();
            HashMultimap<String, String> propbankToVerbnet = HashMultimap.create();

            for (eu.fbk.dkm.pikes.resources.util.semlink.vnpb.Predicate predicate : semLinkPb.getPredicate()) {
                String lemma = predicate.getLemma();
                Argmap argmap = predicate.getArgmap();
                if (argmap == null) {
                    continue;
                }

                String pbFrame = argmap.getPbRoleset().toLowerCase();
                String vnClass = argmap.getVnClass().toLowerCase();

                verbnetToPropbank.put(vnClass, pbFrame);
                propbankToVerbnet.put(pbFrame, vnClass);

                for (eu.fbk.dkm.pikes.resources.util.semlink.vnpb.Role role : argmap.getRole()) {
                    String pbArg = pbFrame + "@" + role.getPbArg().toLowerCase();
                    String vnTheta = vnClass + "@" + role.getVnTheta().toLowerCase();

                    verbnetToPropbank.put(vnTheta, pbArg);
                    propbankToVerbnet.put(pbArg, vnTheta);
                }

            }

            semlinkFile = new File(semlinkFolder.getAbsolutePath() + File.separator + "vn-fn" + File.separator
                    + "VN-FNRoleMapping.txt");
            SemLinkRolesRoot semLinkRoles = (SemLinkRolesRoot) semlinkRolesUnmarshaller.unmarshal(semlinkFile);

            HashMultimap<String, String> verbnetToFramenet = HashMultimap.create();
            HashMultimap<String, String> framenetToVerbnet = HashMultimap.create();

            for (Vncls vncls : semLinkRoles.getVncls()) {
                String frame = vncls.getFnframe().toLowerCase();
                String vnClass = vncls.getClazz().toLowerCase();

                verbnetToFramenet.put(vnClass, frame);
                framenetToVerbnet.put(frame, vnClass);

                if (vncls.getRoles() == null) {
                    continue;
                }

                for (Role role : vncls.getRoles().getRole()) {
                    String fnRole = frame + "@" + role.getFnrole().toLowerCase();
                    String vnRole = vnClass + "@" + role.getVnrole().toLowerCase();

                    verbnetToFramenet.put(vnRole, fnRole);
                    framenetToVerbnet.put(fnRole, vnRole);
                }
            }

            semlinkFile = new File(
                    semlinkFolder.getAbsolutePath() + File.separator + "vn-fn" + File.separator + "VNC-FNF.s");
            SemLinkRoot semLink = (SemLinkRoot) semlinkUnmarshaller.unmarshal(semlinkFile);

            for (eu.fbk.dkm.pikes.resources.util.semlink.vnfn.Vncls vncls : semLink.getVncls()) {
                String vnClass = vncls.getClazz().toLowerCase();
                String frame = vncls.getFnframe().toLowerCase();

                verbnetToFramenet.put(vnClass, frame);
                framenetToVerbnet.put(frame, vnClass);
            }

            int nbSource = 0;

            LOGGER.info("Loading NomBank files");
            HashMultimap<String, Roleset> nbFrames = HashMultimap.create();
            HashSet<Roleset> nbUnlinked = new HashSet<>();
            for (File file : Files.fileTreeTraverser().preOrderTraversal(nbFolder)) {

                if (!file.isFile()) {
                    continue;
                }

                if (!file.getName().endsWith(".xml")) {
                    continue;
                }

                LOGGER.debug(file.getName());

                Frameset frameset = (Frameset) fnUnmarshaller.unmarshal(file);
                List<Object> noteOrPredicate = frameset.getNoteOrPredicate();
                for (Object predicate : noteOrPredicate) {
                    if (predicate instanceof Predicate) {
                        String lemma = ((Predicate) predicate).getLemma();
                        List<Object> noteOrRoleset = ((Predicate) predicate).getNoteOrRoleset();
                        for (Object roleset : noteOrRoleset) {
                            if (roleset instanceof Roleset) {

                                // Warning: this is really BAD!
                                ((Roleset) roleset).setName(lemma);

                                ArrayList<Matcher> predicates = getPropBankPredicates((Roleset) roleset);
                                for (Matcher matcher : predicates) {
                                    String pb = matcher.group(1);
                                    nbFrames.put(pb, (Roleset) roleset);
                                    nbSource++;
                                }

                                if (predicates.size() == 0) {
                                    nbUnlinked.add((Roleset) roleset);
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.info("Loaded {} rolesets with source", nbSource);
            LOGGER.info("Loaded {} frames without source", nbUnlinked.size());

            LOGGER.info("Loading LU files");
            int i = 0;
            HashMap<String, HashMultimap<String, String>> lus = new HashMap<>();
            HashSet<String> existingFrames = new HashSet<>();
            List<Sentence> exampleSentences = new ArrayList<>();

            File existingFramesFile = new File(outputPattern + "-lu-existingFrames.ser");
            File lusFile = new File(outputPattern + "-lu-lus.ser");
            File exampleSentencesFile = new File(outputPattern + "-lu-exampleSentences.ser");
            if (existingFramesFile.exists() && lusFile.exists() && exampleSentencesFile.exists()) {
                LOGGER.info("Loading data from files");
                existingFrames = (HashSet<String>) loadObjectFromFile(existingFramesFile);
                lus = (HashMap<String, HashMultimap<String, String>>) loadObjectFromFile(lusFile);
                exampleSentences = (List<Sentence>) loadObjectFromFile(exampleSentencesFile);
            } else {
                for (File file : Files.fileTreeTraverser().preOrderTraversal(luFolder)) {
                    if (!file.isFile()) {
                        continue;
                    }

                    if (!file.getName().endsWith(".xml")) {
                        continue;
                    }

                    LOGGER.debug(file.getName());
                    i++;
                    if (max != null && i > max) {
                        break;
                    }

                    LexUnit lexUnit = (LexUnit) luUnmarshaller.unmarshal(file);
                    String lemma = "";
                    POSType posType = lexUnit.getPOS();
                    for (LexemeType lexeme : lexUnit.getLexeme()) {
                        lemma = lemma + " " + lexeme.getName();
                    }
                    lemma = lemma.trim();

                    if (lemma.length() == 0 || posType == null) {
                        LOGGER.error("Lemma or POS null ({}/{})", lemma, posType);
                        continue;
                    }
                    String pos = posType.toString().toLowerCase();
                    String frame = lexUnit.getFrame().toLowerCase();

//                if (!lemma.equals("muslim")) {
//                    continue;
//                }

                    // Get examples from parsed file
                    Corpus corpus = null;
                    File parsedFile = new File(luParsedFolder + File.separator + file.getName() + ".conll");
                    if (parsedFile.exists()) {
                        corpus = Corpus.readDocumentFromFile(parsedFile.getAbsolutePath(), "mate");
                    }

                    // Merge examples
                    int exampleNo = 0;
                    if (corpus != null) {
                        for (SubCorpusType subCorpus : lexUnit.getSubCorpus()) {
                            for (SentenceType sentence : subCorpus.getSentence()) {
                                String text = sentence.getText();
                                if (text != null && text.length() > 0) {

                                    Sentence conllSentence = corpus.getSentences().get(exampleNo++);

                                    // This is an example
                                    List<Integer> target = new ArrayList<>();
                                    HashMultimap<String, List<Integer>> roles = HashMultimap.create();

                                    for (AnnotationSetType annotationSet : sentence.getAnnotationSet()) {
                                        for (LayerType layer : annotationSet.getLayer()) {
                                            String name = layer.getName();
                                            if (name.equals("Target")) {
                                                for (LabelType label : layer.getLabel()) {
                                                    target = getSpan(text, label);

                                                    // Target should be unique...
                                                    break;
                                                }
                                            }
                                            if (name.equals("FE")) {
                                                for (LabelType label : layer.getLabel()) {
                                                    List<Integer> span = getSpan(text, label);
                                                    if (span == null) {
                                                        continue;
                                                    }
                                                    roles.put(label.getName(), span);
                                                }

                                            }
                                        }
                                    }

                                    if (target == null || target.size() == 0) {
                                        LOGGER.error("Target not found");
                                        continue;
                                    }

                                    try {
                                        Integer targetHead = conllSentence.searchHead(target);
                                        Srl srl = new Srl(conllSentence.getWords().get(targetHead), frame, "framenet");
                                        for (String roleLabel : roles.keySet()) {
                                            Set<List<Integer>> spans = roles.get(roleLabel);
                                            for (List<Integer> span : spans) {
                                                Integer roleHead = conllSentence.searchHead(span);
                                                eu.fbk.dkm.pikes.resources.util.corpus.Role role = new eu.fbk.dkm.pikes.resources.util.corpus.Role(
                                                        conllSentence.getWords().get(roleHead), roleLabel);
                                                srl.addRole(role);
                                            }
                                        }
                                        conllSentence.addSrl(srl);
                                    } catch (Exception e) {

                                        LOGGER.error("Error in aligning tokens");

//                                    System.out.println(conllSentence);
//                                    System.out.println(file.getName());
//                                    System.out.println(lemma);
//                                    System.out.println(text);
//                                    System.out.println(frame);
//                                    System.out.println(target);
//                                    System.out.println(roles);
//                                    System.out.println();
                                    }

                                    exampleSentences.add(conllSentence);
                                }
                            }
                        }
                    }

                    existingFrames.add(frame);
//                Matcher matcher = LU_PATTERN.matcher(lemma);
//                if (!matcher.matches()) {
//                    LOGGER.error("{} does not match", lemma);
//                    continue;
//                }

//                lemma = matcher.group(1);
//                lemma = getLemmaFromPredicateName(lemma);
//                String pos = matcher.group(2);

                    if (lus.get(pos) == null) {
                        lus.put(pos, HashMultimap.create());
                    }

                    lus.get(pos).put(lemma, frame);
                }
            }

            LOGGER.info("Load FrameBase file");
            HashMultimap<String, String> fbFramenetToWordNet = HashMultimap.create();

            List<String> lines = Files.readLines(framebaseFile, Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                Matcher matcher = FRAMEBASE_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }

                String frame = matcher.group(1).toLowerCase();
                String lemma = matcher.group(2);
                lemma = getLemmaFromPredicateName(lemma);
                String wnSynset = WordNet.getSynsetID(Long.parseLong(matcher.group(4)), matcher.group(3));

                String key = getFrameBaseKey(frame, lemma, ignoreLemmaInFrameBaseMappings);

                fbFramenetToWordNet.put(key, wnSynset);
            }

//            for (String key : fbFramenetToWordNet.keySet()) {
//                System.out.println(key + " -> " + fbFramenetToWordNet.get(key));
//            }

            LOGGER.info("Reading PropBank files");
            List<RolesetInfo> rolesets = new ArrayList<>();

            // Warning: this collects only verbs!
            for (File file : Files.fileTreeTraverser().preOrderTraversal(pbFolder)) {

                if (!file.isFile()) {
                    continue;
                }

                if (!file.getName().endsWith(".xml")) {
                    continue;
                }

                //todo: check ontonotes or not
                String type;
                String baseLemma;
                Matcher matcher = ONTONOTES_FILENAME_PATTERN.matcher(file.getName());
                if (matcher.matches()) {
                    type = matcher.group(2);
                    baseLemma = matcher.group(1);
                } else {
                    throw new Exception(
                            "File " + file.getName() + " does not appear to be a good OntoNotes frame file");
                }

                if (!type.equals("v")) {
                    continue;
                }

                LOGGER.debug(file.getName());

                HashMap<String, HashMap<String, Set>> senses = getSenses(file.getName(), ontonotesFolder, baseLemma,
                        type, onUnmarshaller);

                Frameset frameset = (Frameset) fnUnmarshaller.unmarshal(file);
                List<Object> noteOrPredicate = frameset.getNoteOrPredicate();

                for (Object predicate : noteOrPredicate) {
                    if (predicate instanceof Predicate) {

                        String lemma = getLemmaFromPredicateName(((Predicate) predicate).getLemma());

                        List<String> synsets = WordNet.getSynsetsForLemma(lemma.replace('+', ' '), type);

                        Set<String> luFrames = lus.get(type).get(lemma);
                        luFrames.retainAll(existingFrames);

                        List<Object> noteOrRoleset = ((Predicate) predicate).getNoteOrRoleset();
                        for (Object roleset : noteOrRoleset) {
                            if (roleset instanceof Roleset) {
                                String rolesetID = ((Roleset) roleset).getId();

                                RolesetInfo rolesetInfo = new RolesetInfo(file, rolesetID, baseLemma, lemma, type,
                                        senses, luFrames, (Roleset) roleset, synsets);
                                rolesets.add(rolesetInfo);
                            }
                        }
                    }
                }
            }

            // Looping rolesets

            int trivialCount = 0;
            int nonTrivialCount = 0;
            int nbCount = 0;
            int emptyRelatedCount = 0;
            int nbGreaterCount = 0;
            int nbZeroCount = 0;
            int unlinkedCount = 0;
            int roleMappingCount = 0;
            int noFrameBaseCount = 0;
            int semlinkCounter = 0;

            HashMap<OutputMapping, HashMap<String, String>> outputMappingsForPredicates = new HashMap<>();
            HashMap<OutputMapping, HashMap<String, String>> outputMappingsForPredicatesAdd = new HashMap<>();
            HashMap<OutputMapping, HashMap<String, String>> outputMappingsForRoles = new HashMap<>();
            for (OutputMapping outputMapping : OutputMapping.values()) {
                outputMappingsForPredicates.put(outputMapping, new HashMap<>());
                outputMappingsForPredicatesAdd.put(outputMapping, new HashMap<>());
                outputMappingsForRoles.put(outputMapping, new HashMap<>());
            }

            File frameFile = new File(outputPattern + "-frames.ser");
            File rolesFile = new File(outputPattern + "-roles.ser");
            File addFile = new File(outputPattern + "-add.ser");

            if (frameFile.exists() && rolesFile.exists() && addFile.exists()) {
                LOGGER.info("Loading mappings from files");
                outputMappingsForPredicates = (HashMap<OutputMapping, HashMap<String, String>>) loadObjectFromFile(
                        frameFile);
                outputMappingsForRoles = (HashMap<OutputMapping, HashMap<String, String>>) loadObjectFromFile(
                        rolesFile);
                outputMappingsForPredicatesAdd = (HashMap<OutputMapping, HashMap<String, String>>) loadObjectFromFile(
                        addFile);
            } else {
                for (RolesetInfo rolesetInfo : rolesets) {

                    Roleset roleset = rolesetInfo.getRoleset();
                    String rolesetID = rolesetInfo.getLabel();
                    HashMap<String, HashMap<String, Set>> senses = rolesetInfo.getSenses();
                    List<String> synsets = rolesetInfo.getSynsets();
                    String lemma = rolesetInfo.getLemma();
                    String baseLemma = rolesetInfo.getBaseLemma();
                    Set<String> luFrames = rolesetInfo.getLuFrames();
                    String type = rolesetInfo.getType();

                    String frameNet = roleset.getFramnet();

                    if (frameNet != null) {
                        frameNet = frameNet.toLowerCase();
                    }

                    LOGGER.debug(rolesetID);

                    ArrayList<String> fnFrames = new ArrayList<>();
                    if (frameNet != null) {
                        String[] fns = frameNet.split("\\s+");
                        for (String fn : fns) {
                            if (fn.length() == 0) {
                                continue;
                            }
                            fnFrames.add(fn);
                        }
                    }
                    fnFrames.retainAll(existingFrames);

                    if (enableSemLinkForPredicates && fnFrames.size() == 0) {
                        String vnClasses = roleset.getVncls();
                        if (vnClasses != null) {
                            vnClasses = vnClasses.trim();
                            String[] parts = vnClasses.split("\\s+");
                            for (String part : parts) {
                                Set<String> frames = verbnetToFramenet.get(part);
                                if (frames != null) {
                                    fnFrames = new ArrayList<>(frames);
                                }
                            }
                        }
                    }

                    Collection<String> wnFromSenses = new HashSet<>();
                    Collection<String> fnFromSenses = new HashSet<>();
                    if (senses.get(rolesetID) != null) {
                        wnFromSenses = senses.get(rolesetID).get("wn");
                        fnFromSenses = senses.get(rolesetID).get("fn");
                    }
                    fnFromSenses.retainAll(existingFrames);

//                                    System.out.println(synsets);
//                                    System.out.println(wnFromSenses);

                    Collection<String> wnCandidates = getIntersection(synsets, wnFromSenses);

                    boolean useBaseLemma = false;
                    String lemmaToUse = lemma;

                    if (!lemma.equals(baseLemma)) {
                        if (synsets.size() + wnFromSenses.size() == 0) {
                            useBaseLemma = true;
                        }
                        for (String wnCandidate : wnCandidates) {
                            Set<String> lemmas = WordNet.getLemmas(wnCandidate);
                            if (lemmas.contains(baseLemma)) {
                                useBaseLemma = true;
                            }
                        }

                        if (useBaseLemma && luFrames.size() != 0) {
                            LOGGER.debug("Base lemma should be used, but lexical unit found ({})",
                                    rolesetID);
                            useBaseLemma = false;
                        }
                    }

                    Set<String> luFramesToUse = new HashSet<>(luFrames);

                    if (useBaseLemma) {
                        LOGGER.debug("Using base lemma");
                        lemmaToUse = baseLemma;
                        luFramesToUse = lus.get(type).get(baseLemma);

                        List<String> newSynsets = WordNet
                                .getSynsetsForLemma(baseLemma.replace('+', ' '), type);
                        wnCandidates = getIntersection(wnCandidates, newSynsets);
                    }

                    Collection<String> fnCandidates = getIntersection(fnFrames, luFramesToUse,
                            fnFromSenses);

                    Collection<String> fnCandidatesOnlySemLink = getIntersection(fnFrames,
                            fnFromSenses);
                    if (fnCandidatesOnlySemLink.size() == 1) {
                        semlinkCounter++;
                    }

                    Collection<String> okFrames = getCandidateFrames(wnCandidates, fnCandidates,
                            lemmaToUse,
                            type, fbFramenetToWordNet, ignoreLemmaInFrameBaseMappings);

//                                    if (rolesetID.equals("add.04")) {
//                                        System.out.println(synsets);
//                                        System.out.println(wnFromSenses);
//
//                                        System.out.println(fnFrames);
//                                        System.out.println(luFramesToUse);
//                                        System.out.println(fnFromSenses);
//
//                                        System.out.println(wnCandidates);
//                                        System.out.println(fnCandidates);
//
//                                        System.out.println(lemmaToUse);
//                                    }

                    if (fnCandidatesOnlySemLink.size() == 1 && okFrames.size() == 0) {
                        for (String fnCandidate : fnCandidates) {
                            outputMappingsForPredicatesAdd.get(OutputMapping.PBauto)
                                    .put(rolesetID, fnCandidate);
                            noFrameBaseCount++;
                        }
                    }

                    // If Fp’ contains a singleton frame f, then we align p to f.
                    // Otherwise we avoid any alignment.
                    if (okFrames.size() == 1) {
                        for (String okFrame : okFrames) {
                            if (fnFrames.size() == 1 && fnFrames.contains(okFrame)) {
                                trivialCount++;
                                outputMappingsForPredicates.get(OutputMapping.PBtrivial)
                                        .put(rolesetID, okFrame);
                                continue;
                            }
                            nonTrivialCount++;

                            outputMappingsForPredicates.get(OutputMapping.PBauto)
                                    .put(rolesetID, okFrame);
                        }
                    }

                    // NomBank
                    Set<Roleset> fRolesets = nbFrames.get(rolesetID);
                    for (Roleset nbRoleset : fRolesets) {

                        // See bad choice above
                        String nbLemma = nbRoleset.getName();

                        List<String> nbSynsets = WordNet
                                .getSynsetsForLemma(nbLemma.replace('+', ' '), "n");

                        Set<String> relatedSynsets = new HashSet<>();
                        for (String wnCandidate : wnCandidates) {
                            relatedSynsets
                                    .addAll(WordNet.getGenericSet(wnCandidate, PointerType.DERIVED,
                                            PointerType.NOMINALIZATION, PointerType.PARTICIPLE_OF,
                                            PointerType.PERTAINYM));
                        }

                        if (relatedSynsets.size() == 0) {
                            emptyRelatedCount++;
                        }

                        Set<String> luNbFrames = lus.get("n").get(nbLemma);
                        Collection<String> fnNbCandidates = getIntersection(fnFrames, luFrames,
                                fnFromSenses, luNbFrames);

                        Collection<String> nbCandidates = getIntersection(nbSynsets, relatedSynsets);
                        Collection<String> okNbFrames = getCandidateFrames(nbCandidates, fnNbCandidates,
                                nbLemma, "n", fbFramenetToWordNet, ignoreLemmaInFrameBaseMappings);

                        // If Fp’ contains a singleton frame f, then we align p to f.
                        // Otherwise we avoid any alignment.
                        if (okNbFrames.size() == 1) {
                            for (String okFrame : okNbFrames) {
                                nbCount++;
                                outputMappingsForPredicates.get(OutputMapping.NBauto)
                                        .put(nbRoleset.getId(), okFrame);
                            }
                        }
                        if (okNbFrames.size() > 1) {
                            nbGreaterCount++;
                        }
                        if (okNbFrames.size() == 0) {
                            nbZeroCount++;
                        }
                    }
                }

                // Looping for roles
                for (RolesetInfo rolesetInfo : rolesets) {
                    Roleset roleset = rolesetInfo.getRoleset();
                    String rolesetID = rolesetInfo.getLabel();

                    for (Object roles : roleset.getNoteOrRolesOrExample()) {
                        if (!(roles instanceof Roles)) {
                            continue;
                        }

                        for (Object role : ((Roles) roles).getNoteOrRole()) {
                            if (!(role instanceof eu.fbk.dkm.pikes.resources.util.propbank.Role)) {
                                continue;
                            }

                            String roleStr = rolesetID + "@"
                                    + ((eu.fbk.dkm.pikes.resources.util.propbank.Role) role).getN();

                            HashSet<String> tempMappingsForRole = new HashSet<>();

                            for (Vnrole vnrole : ((eu.fbk.dkm.pikes.resources.util.propbank.Role) role)
                                    .getVnrole()) {
                                String vnClassRole = vnrole.getVncls().toLowerCase();
                                String vnThetaRole =
                                        vnClassRole + "@" + vnrole.getVntheta().toLowerCase();

                                Set<String> fnFrames = verbnetToFramenet
                                        .get(vnThetaRole);
                                tempMappingsForRole.addAll(fnFrames);
                            }

                            if (tempMappingsForRole.size() == 1) {
                                for (String frameRole : tempMappingsForRole) {

                                    // Check for inconsistencies
                                    String frameName = frameRole.replaceAll("@.*", "");
                                    String goodCandidate = outputMappingsForPredicates.get(OutputMapping.PBauto)
                                            .get(rolesetID);
                                    if (goodCandidate == null || !goodCandidate.equals(frameName)) {
                                        continue;
                                    }

                                    outputMappingsForRoles.get(OutputMapping.PBauto).put(roleStr, frameRole);
                                    roleMappingCount++;
                                }
                            }
                        }
                    }
                }

                // Unlinked NomBank
                for (Roleset nbRoleset : nbUnlinked) {

                    // See bad choice above
                    String nbLemma = nbRoleset.getName();
                    List<String> nbSynsets = WordNet.getSynsetsForLemma(nbLemma.replace('+', ' '), "n");

                    if (nbSynsets.size() == 1) {
                        Set<String> frames = lus.get("n").get(nbLemma);
                        if (frames != null && frames.size() == 1) {
                            for (String frame : frames) {
                                outputMappingsForPredicates.get(OutputMapping.NBresource).put(nbRoleset.getId(), frame);
                            }

                            unlinkedCount++;
                        }
                    } else {
                        //todo: check senses
                    }
                }

                LOGGER.info("*** STATISTICS ***");

                LOGGER.info("PropBank trivial: {}", trivialCount);
                LOGGER.info("PropBank non-trivial: {}", nonTrivialCount);
                LOGGER.info("PropBank non-FrameBase: {}", noFrameBaseCount);

                LOGGER.info("NomBank (linked): {}", nbCount);
                LOGGER.info("NomBank (unlinked): {}", unlinkedCount);
                LOGGER.info("NomBank (total): {}", unlinkedCount + nbCount);

                LOGGER.info("PropBank (only with SemLink): {}", semlinkCounter);

                LOGGER.info("PropBank roles: {}", roleMappingCount);

                LOGGER.info("No WordNet relations: {}", emptyRelatedCount);
                LOGGER.info("More than one frame: {}", nbGreaterCount);
                LOGGER.info("Zero frames: {}", nbZeroCount);
            }

            // Parsing examples (exampleSentences)

            LOGGER.info("Parsing examples");

            HashMap<String, FrequencyHashSet<String>> rolesCountByType = new HashMap<>();
            FrequencyHashSet<String> rolesCount = new FrequencyHashSet<>();

            for (Sentence sentence : exampleSentences) {
                HashMap<Word, HashMap<String, Srl>> srlIndex = new HashMap<>();

                for (Srl srl : sentence.getSrls()) {
                    Word target = srl.getTarget().get(0);

                    // Only verbs and nouns
                    if (!target.getPos().toLowerCase().startsWith("v") && !target.getPos().toLowerCase()
                            .startsWith("n")) {
                        continue;
                    }

                    if (!srlIndex.containsKey(target)) {
                        srlIndex.put(target, new HashMap<>());
                    }
                    srlIndex.get(target).put(srl.getSource(), srl);
                }

                for (Word word : srlIndex.keySet()) {
                    if (srlIndex.get(word).size() > 1) {

                        Srl srlFrameNet = srlIndex.get(word).get("framenet");
                        Srl srlMate = srlIndex.get(word).get("mate");

                        String framenet = srlFrameNet.getLabel();
                        String mate = srlMate.getLabel();

                        boolean isVerb = true;
                        if (word.getPos().toLowerCase().startsWith("n")) {
                            isVerb = false;
                        }

                        boolean mappingExists = false;
                        String frameGuess;

                        if (isVerb) {
                            frameGuess = outputMappingsForPredicates.get(OutputMapping.PBauto).get(mate);
                            if (frameGuess != null && frameGuess.equals(framenet)) {
                                mappingExists = true;
                            }
                            frameGuess = outputMappingsForPredicates.get(OutputMapping.PBtrivial).get(mate);
                            if (frameGuess != null && frameGuess.equals(framenet)) {
                                mappingExists = true;
                            }
                        } else {
                            frameGuess = outputMappingsForPredicates.get(OutputMapping.NBauto).get(mate);
                            if (frameGuess != null && frameGuess.equals(framenet)) {
                                mappingExists = true;
                            }
                            frameGuess = outputMappingsForPredicates.get(OutputMapping.NBresource).get(mate);
                            if (frameGuess != null && frameGuess.equals(framenet)) {
                                mappingExists = true;
                            }
                        }

                        if (mappingExists) {

                            HashMap<Word, String> roleWordsMate = new HashMap<>();
                            HashMap<Word, String> roleWordsFrameNet = new HashMap<>();

                            // Mate
                            for (eu.fbk.dkm.pikes.resources.util.corpus.Role role : srlMate.getRoles()) {
                                Word roleHead = role.getSpan().get(0);
                                String roleLabel = role.getLabel();
                                roleLabel = roleLabel.replaceAll("R-", "");

                                // Consider only core roles
                                if (roleLabel.startsWith("AM-")) {
                                    continue;
                                }

                                roleWordsMate.put(roleHead, roleLabel);
                            }

                            // FrameNet
                            for (eu.fbk.dkm.pikes.resources.util.corpus.Role role : srlFrameNet.getRoles()) {
                                Word roleHead = role.getSpan().get(0);
                                String roleLabel = role.getLabel();
                                roleWordsFrameNet.put(roleHead, roleLabel);
                            }

                            for (Word key : roleWordsMate.keySet()) {
                                String prefix = isVerb ? "v-" : "n-";
                                String mateCompressed =
                                        prefix + mate + "@" + roleWordsMate.get(key).replaceAll("[aA]", "");
                                rolesCount.add(mateCompressed);

                                if (!rolesCountByType.containsKey(mateCompressed)) {
                                    rolesCountByType.put(mateCompressed, new FrequencyHashSet<>());
                                }

                                String fnRole = roleWordsFrameNet.get(key);
                                if (fnRole != null) {
                                    fnRole = fnRole.toLowerCase();
                                    String fnCompressed = framenet + "@" + fnRole;
                                    rolesCountByType.get(mateCompressed).add(fnCompressed);
                                } else {
                                    rolesCountByType.get(mateCompressed).add("[none]");
                                }
                            }

//                            for (eu.fbk.dkm.pikes.resources.util.corpus.Role role : srlFrameNet.getRoles()) {
//                                Word roleHead = role.getSpan().get(0);
//                                if (roleWords.containsKey(roleHead)) {
//                                    String thisMateRole = mate + "@" + roleWords.get(roleHead);
//                                    String thisFrameNetRole = framenet + "@" + role.getLabel();
//
//                                    if (!pbToFn.containsKey(thisMateRole)) {
//                                        pbToFn.put(thisMateRole, new FrequencyHashSet<>());
//                                    }
//                                    pbToFn.get(thisMateRole).add(thisFrameNetRole);
//                                }
//                            }
                        } else {
                            // These *can* be good mappings
                        }

//                        if (!fnToPb.containsKey(framenet)) {
//                            fnToPb.put(framenet, new FrequencyHashSet<>());
//                        }
//                        if (!pbToFn.containsKey(mate)) {
//                            pbToFn.put(mate, new FrequencyHashSet<>());
//                        }
//
//                        fnToPb.get(framenet).add(mate);
//                        pbToFn.get(mate).add(framenet);
                    }
                }
            }

            // Evaluate and save
            double okThreshold = 0.5;
            int okMinFreq = 5;
            HashMap<OutputMapping, HashMap<String, String>> outputMappingsForRolesFromExamples = new HashMap<>();
            for (OutputMapping outputMapping : OutputMapping.values()) {
                outputMappingsForRolesFromExamples.put(outputMapping, new HashMap<>());
            }

            for (double threshold = 0.5; threshold < 1; threshold += 0.1) {
                for (int minFreq = 2; minFreq <= 10; minFreq++) {
                    int trivialMappingsCount = 0;
                    int correctMappingsCount = 0;
                    int wrongMappingsCount = 0;

                    HashMap<OutputMapping, HashMap<String, String>> outputMappingsForRolesFromExamplesTemp = new HashMap<>();
                    for (OutputMapping outputMapping : OutputMapping.values()) {
                        outputMappingsForRolesFromExamplesTemp.put(outputMapping, new HashMap<>());
                    }

                    for (String key : rolesCount.keySet()) {

                        String candidate = rolesCountByType.get(key).mostFrequent();
                        int freq = rolesCountByType.get(key).get(candidate);
                        double ratio = 0.0;
                        if (candidate != null && !candidate.equals("[none]")) {
                            ratio = (double) freq / (double) rolesCount.get(key);
                        } else {
                            candidate = null;
                        }
                        if (ratio > threshold && freq >= minFreq) {
                            String mate = key.substring(2);
                            OutputMapping mapping = key.startsWith("v") ? OutputMapping.PBauto : OutputMapping.NBauto;
                            outputMappingsForRolesFromExamplesTemp.get(mapping).put(mate, candidate);

                            // Save one version
                            if (Math.abs(okThreshold - threshold) < 0.01 && minFreq == okMinFreq) {
                                outputMappingsForRolesFromExamples.get(mapping).put(mate, candidate);
                            }

                            switch (mapping) {
                            case PBauto:
                                String fnRole = outputMappingsForRoles.get(OutputMapping.PBauto).get(mate);
                                if (fnRole != null) {
                                    trivialMappingsCount++;
                                    if (fnRole.equals(candidate)) {
                                        correctMappingsCount++;
                                    } else {
                                        wrongMappingsCount++;
//                                        LOGGER.error("Wrong mapping: {} -> {}", mate, candidate);
                                    }
                                }
                                break;
                            case NBauto:
                                break;
                            }
                        }
                    }

//                    LOGGER.info("Trivial role mappings: {}", trivialMappingsCount);
//                    LOGGER.info("Correct role mappings: {}", correctMappingsCount);
//                    LOGGER.info("Wrong role mappings: {}", wrongMappingsCount);

                    double precision = (double) correctMappingsCount / (double) trivialMappingsCount;
                    System.out.println(String.format(
                                    "%5f %5d %5d %5d %5d %5d %5d %5f",
                                    threshold,
                                    minFreq,
                                    outputMappingsForRolesFromExamplesTemp.get(OutputMapping.PBauto).size(),
                                    outputMappingsForRolesFromExamplesTemp.get(OutputMapping.NBauto).size(),
                                    trivialMappingsCount,
                                    correctMappingsCount,
                                    wrongMappingsCount,
                                    precision
                            )
                    );
                }
            }

            // Write files

            BufferedWriter writer;
            File outputFile;

            outputFile = new File(outputPattern + "-frames.tsv");
            LOGGER.info("Writing output file {}", outputFile.getName());
            writer = new BufferedWriter(new FileWriter(outputFile));
            for (OutputMapping outputMapping : outputMappingsForPredicates.keySet()) {
                for (String key : outputMappingsForPredicates.get(outputMapping).keySet()) {
                    String value = outputMappingsForPredicates.get(outputMapping).get(key);

                    writer.append(outputMapping.toString()).append('\t');
                    writer.append(key).append('\t');
                    writer.append(value).append('\n');
                }
            }
            writer.close();
            outputFile = new File(outputPattern + "-frames.ser");
            saveObjectToFile(outputMappingsForPredicates, outputFile);

            outputFile = new File(outputPattern + "-roles.tsv");
            LOGGER.info("Writing output file {}", outputFile.getName());
            writer = new BufferedWriter(new FileWriter(outputFile));
            for (OutputMapping outputMapping : outputMappingsForRoles.keySet()) {
                for (String key : outputMappingsForRoles.get(outputMapping).keySet()) {
                    String value = outputMappingsForRoles.get(outputMapping).get(key);

                    writer.append(outputMapping.toString()).append('\t');
                    writer.append(key).append('\t');
                    writer.append(value).append('\n');
                }
            }
            writer.close();
            outputFile = new File(outputPattern + "-roles.ser");
            saveObjectToFile(outputMappingsForRoles, outputFile);

            if (outputMappingsForRolesFromExamples != null) {
                outputFile = new File(outputPattern + "-roles-examples.tsv");
                LOGGER.info("Writing output file {}", outputFile.getName());
                writer = new BufferedWriter(new FileWriter(outputFile));
                for (OutputMapping outputMapping : outputMappingsForRolesFromExamples.keySet()) {
                    for (String key : outputMappingsForRolesFromExamples.get(outputMapping).keySet()) {
                        String value = outputMappingsForRolesFromExamples.get(outputMapping).get(key);

                        writer.append(outputMapping.toString()).append('\t');
                        writer.append(key).append('\t');
                        writer.append(value).append('\n');
                    }
                }
                writer.close();
            }

            outputFile = new File(outputPattern + "-add.tsv");
            LOGGER.info("Writing output file {}", outputFile.getName());
            writer = new BufferedWriter(new FileWriter(outputFile));
            for (OutputMapping outputMapping : outputMappingsForPredicatesAdd.keySet()) {
                for (String key : outputMappingsForPredicatesAdd.get(outputMapping).keySet()) {
                    String value = outputMappingsForPredicatesAdd.get(outputMapping).get(key);

                    writer.append(outputMapping.toString()).append('\t');
                    writer.append(key).append('\t');
                    writer.append(value).append('\n');
                }
            }
            writer.close();
            outputFile = new File(outputPattern + "-add.ser");
            saveObjectToFile(outputMappingsForPredicatesAdd, outputFile);

            if (saveFiles) {
                outputFile = new File(outputPattern + "-lu-existingFrames.ser");
                if (!outputFile.exists()) {
                    LOGGER.info("Writing object file {}", outputFile.getName());
                    saveObjectToFile(existingFrames, outputFile);
                }

                outputFile = new File(outputPattern + "-lu-lus.ser");
                if (!outputFile.exists()) {
                    LOGGER.info("Writing object file {}", outputFile.getName());
                    saveObjectToFile(lus, outputFile);
                }

                outputFile = new File(outputPattern + "-lu-exampleSentences.ser");
                if (!outputFile.exists()) {
                    LOGGER.info("Writing object file {}", outputFile.getName());
                    saveObjectToFile(exampleSentences, outputFile);
                }
            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }

    private static Object loadObjectFromFile(File inputFile) throws IOException {
        ObjectInputStream objectinputstream = null;
        FileInputStream streamIn = null;
        try {
            streamIn = new FileInputStream(inputFile);
            objectinputstream = new ObjectInputStream(streamIn);
            return objectinputstream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (objectinputstream != null) {
                objectinputstream.close();
            }
        }
        return null;
    }

    private static void saveObjectToFile(Object o, File outputFile) throws IOException {
        ObjectOutputStream oos = null;
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(outputFile);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(o);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                oos.close();
            }
        }
    }

    private static List<Integer> getSpan(String text, LabelType label) {
        List<Integer> ret = new ArrayList<>();

        Integer start = label.getStart();
        if (start == null) {
            return null;
        }

        Integer end = label.getEnd();
        String before = text.substring(0, start);
        before = before.replaceAll("\\s+", " ");
        int target = before.replaceAll("[^\\s]", "").length();
        String inside = text.substring(start, end);
        inside = inside.replaceAll("\\s+", " ");
        int length = inside.replaceAll("[^\\s]", "").length() + 1;

        for (int i = 0; i < length; i++) {
            ret.add(target + i);
        }

        return ret;
    }

    private static HashMap<String, HashMap<String, Set>> getSenses(String name, File ontonotesFolder, String fnLemma,
            String type, Unmarshaller onUnmarshaller)
            throws JAXBException {

        HashMap<String, HashMap<String, Set>> senses = new HashMap<>();

        //todo: add type (for PB 1.7, for example)
        File onSense = new File(ontonotesFolder.getAbsolutePath() + File.separator + name);
        if (onSense.exists()) {

            Inventory inventory = (Inventory) onUnmarshaller.unmarshal(onSense);
            for (Sense sense : inventory.getSense()) {

                if (sense.getMappings() == null) {
                    continue;
                }

                Set<String> onWn = new HashSet<>();
                Set<String> onFn = new HashSet<>();
                Set<String> onPb = new HashSet<>();

                // PropBank
                if (sense.getMappings().getPb() != null) {
                    String[] pbs = sense.getMappings().getPb().split(",");
                    for (String pb : pbs) {
                        pb = pb.trim();
                        if (pb.length() == 0) {
                            continue;
                        }
                        onPb.add(pb);
                    }
                }

                // FrameNet
                if (sense.getMappings().getFn() != null) {
                    String[] fns = sense.getMappings().getFn().split(",");
                    for (String fn : fns) {
                        fn = fn.trim().toLowerCase();
                        if (fn.length() == 0) {
                            continue;
                        }
                        onFn.add(fn);
                    }
                }

                // WordNet
                try {
                    for (Wn wn : sense.getMappings().getWn()) {
                        String lemma = wn.getLemma();
                        if (lemma == null || lemma.length() == 0) {
                            lemma = fnLemma;
                        }
                        String value = wn.getvalue();
                        String[] ids = value.split(",");
                        for (String id : ids) {
                            id = id.trim();
                            if (id.length() == 0) {
                                continue;
                            }
                            String synsetID = WordNet.getSynsetID(lemma + "-" + id + type);
                            onWn.add(synsetID);
                        }
                    }
                } catch (Exception e) {
                    // ignored
                }

                for (String pb : onPb) {
                    if (!senses.containsKey(pb)) {
                        senses.put(pb, new HashMap<>());
                    }
                    if (!senses.get(pb).containsKey("wn")) {
                        senses.get(pb).put("wn", new HashSet<>());
                    }
                    if (!senses.get(pb).containsKey("fn")) {
                        senses.get(pb).put("fn", new HashSet<>());
                    }
                    senses.get(pb).get("wn").addAll(onWn);
                    senses.get(pb).get("fn").addAll(onFn);
                }
            }
        }

        return senses;
    }

    private static Collection<String> getCandidateFrames(Collection<String> wnCandidates,
            Collection<String> fnCandidates,
            String lemma, String type, HashMultimap<String, String> fbFramenetToWordNet,
            boolean ignoreLemmaInFrameBaseMappings) {

        Collection<String> okFrames = new HashSet<>();
        for (String fnCandidate : fnCandidates) {
            String key = getFrameBaseKey(fnCandidate, lemma, type, ignoreLemmaInFrameBaseMappings);
            Collection<String> wnCandidatesForThisFrame = new HashSet<>(fbFramenetToWordNet.get(key));
            wnCandidatesForThisFrame.retainAll(wnCandidates);
            if (wnCandidatesForThisFrame.size() > 0) {
                okFrames.add(fnCandidate);
            }
        }

        return okFrames;
    }

    private static String getFrameBaseKey(String frame, String lemma, String type,
            boolean ignoreLemmaInFrameBaseMappings) {
        return getFrameBaseKey(frame, lemma + "." + type, ignoreLemmaInFrameBaseMappings);
    }

    private static String getFrameBaseKey(String frame, String lemma, boolean ignoreLemmaInFrameBaseMappings) {
        if (ignoreLemmaInFrameBaseMappings) {
            return frame;
        }
        return frame + "-" + lemma;
    }
}
