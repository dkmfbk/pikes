package eu.fbk.dkm.pikes.raid;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.fbk.dkm.pikes.naflib.Corpus;
import eu.fbk.dkm.pikes.resources.*;
import eu.fbk.dkm.utils.ArrayUtils;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.*;
import org.fbk.cit.hlt.core.analysis.stemmer.Stemmer;
import org.fbk.cit.hlt.core.analysis.stemmer.StemmerFactory;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by alessio on 17/04/15.
 */

public class CreateTrainingForExpression {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CreateTrainingForExpression.class);
	private static final String DEFAULT_LABEL = "gold";

	private static SenticNet senticNet;
	private static SubjectivityLexicon subjectivityLexicon;
	private static Stemmer stemmer;
	private static Intensities intensities;

	public enum Features {
		STANFORD,
		SENTICNET,
		SUBJLEXICON,
		INTENSITY,
		WORDNET,
		SENTIWORDNET,
		MOSCHITTI,
		SST,
		ENTITIES,
		STEM,
		POS,
		DEP,
		SRL
	}

	private static boolean MAJORITY = false;

	private static boolean FEATS_STANFORD = false;
	private static boolean FEATS_SENTICNET = MAJORITY;
	private static boolean FEATS_SUBJLEXICON = MAJORITY;
	private static boolean FEATS_INTENSITY = MAJORITY;
	private static boolean FEATS_WORDNET = MAJORITY;
	private static boolean FEATS_SENTIWORDNET = false;

	private static boolean FEATS_MOSCHITTI = true;

	private static boolean FEATS_SST = true;
	private static boolean FEATS_ENTITIES = true;
	private static boolean FEATS_STEM = true;
	private static boolean FEATS_POS = true;
	private static boolean FEATS_DEP = MAJORITY;
	private static boolean FEATS_SRL = MAJORITY;

	private static Long DEFAULT_SEED = 2l;
	private static String DEFAULT_CLASSIFICATION_LABEL = "_CLASS";
	private static Integer DEFAULT_SLOT_SIZE = 1;
	private static Float DEFAULT_SPLIT = 0.75f;

	private static String DEFAULT_NONE = "-";
	private static String DEFAULT_YES = "Y";

	private static HashSet<String> DOUBLE_FEATURES = new HashSet<>();

	static {
		DOUBLE_FEATURES.add("LEMMA");
		DOUBLE_FEATURES.add("P");
		DOUBLE_FEATURES.add("E");
		DOUBLE_FEATURES.add("SST");
	}

	private static HashSet<String> TRIPLE_FEATURES = new HashSet<>();

	static {
		TRIPLE_FEATURES.add("LEMMA");
		TRIPLE_FEATURES.add("P");
	}

	public enum Type {
		MALLET, MALLET_WINDOW, YAMCHA, CRFSUITE, WAPITI
	}

	public enum OutputType {
		SINGLE, COMPLETE
	}

	static Type DEFAULT_TYPE = Type.CRFSUITE;

	public static ArrayList<ArrayList<LinkedHashMap<String, String>>> extractFeats(KAFDocument document, String[] labels, Set<String> hypernyms, boolean skipEmpty) {
		HashSet<Term> opinionTerms = new HashSet<>();
		HashMap<Term, String> stanfordTerms = new HashMap<>();
		HashMap<Term, String> entityTerms = new HashMap<>();

		// Preprocessing srl
		HashMultimap<Term, String> srlFeatures = HashMultimap.create();
		String featName;
		if (FEATS_SRL) {
			for (Predicate predicate : document.getPredicates()) {
				for (Term term : predicate.getTerms()) {
					srlFeatures.put(term, "isPredicate");
					for (ExternalRef externalRef : predicate.getExternalRefs()) {
						if (externalRef.getReference().length() == 0) {
							continue;
						}
						featName = "isPredicate." + externalRef.getResource() + "." + externalRef.getReference();
						srlFeatures.put(term, featName);
					}
					for (Predicate.Role role : predicate.getRoles()) {
						for (ExternalRef externalRef : role.getExternalRefs()) {
							if (externalRef.getReference().length() == 0) {
								continue;
							}
//							featName = "hasRole." + externalRef.getResource() + "." + externalRef.getReference();
//							srlFeatures.put(term, featName);
							featName = "hasRole." + externalRef.getReference();
							srlFeatures.put(term, featName);

							for (Term roleTerm : role.getTerms()) {
								featName = "isRole";
								srlFeatures.put(roleTerm, featName);
//								featName = "isRole." + externalRef.getResource() + "." + externalRef.getReference();
//								srlFeatures.put(roleTerm, featName);

								for (ExternalRef roleExternalRef : predicate.getExternalRefs()) {
									if (roleExternalRef.getReference().length() == 0) {
										continue;
									}
									featName = "isRoleFor." + roleExternalRef.getReference();
									srlFeatures.put(term, featName);
//									featName = "isRoleFor." + roleExternalRef.getResource() + "." + roleExternalRef.getReference();
//									srlFeatures.put(term, featName);
								}
							}

						}
					}

				}

			}
		}

		// Preprocessing entities
		for (Entity entity : document.getEntities()) {
			for (Term term : entity.getTerms()) {
				entityTerms.put(term, entity.getType());
			}
		}

		// Preprocessing opinions
		for (Opinion opinion : document.getOpinions()) {
			if (opinion.getOpinionExpression() == null) {
				continue;
			}

			if (opinion.getLabel() == null) {
				continue;
			}

			if (opinion.getOpinionExpression().getSpan() == null) {
				continue;
			}

			boolean hasLabel = false;
			for (String label : labels) {
				if (opinion.getLabel().contains(label)) {
					hasLabel = true;
					break;
				}
			}

			if (!hasLabel) {
				if (opinion.getLabel().equals("stanford-sentiment")) {
					if (opinion.getOpinionExpression().getSpan().size() == 1) {
						String pol = opinion.getOpinionExpression().getPolarity();
						if (pol.equals("Neutral")) {
							pol = "M";
						}
						stanfordTerms.put(opinion.getOpinionExpression().getSpan().getFirstTarget(), pol);
					}
				}
				continue;
			}

			// for VUA dataset
			if (opinion.getOpinionExpression().getPolarity() != null) {
				if (opinion.getOpinionExpression().getPolarity().equals("NON-OPINIONATED")) {
					continue;
				}
			}

//			boolean first = true;
			for (Term term : opinion.getOpinionExpression().getSpan().getTargets()) {
//				if (first) {
//					firstTerms.add(term);
//					first = false;
//				}
				opinionTerms.add(term);
			}
		}

		Multimap<Term, SenticNet.Lexeme> senticnetMM = senticNet.match(document, document.getTerms());
		Multimap<Term, SubjectivityLexicon.Lexeme> subjectivityMM = subjectivityLexicon.match(document, document.getTerms());
		Multimap<Term, Intensities.Lexeme> intensitiesMM = intensities.match(document, document.getTerms());

//		StringBuffer buffer = new StringBuffer();
		ArrayList<ArrayList<LinkedHashMap<String, String>>> ret = new ArrayList<>();

		for (int i = 0; i < document.getNumSentences(); i++) {

			ArrayList<LinkedHashMap<String, String>> sentence = new ArrayList<>();
			int sent = i + 1;
			String last = "O";
			for (Term term : document.getSentenceTerms(sent)) {
				LinkedHashMap<String, String> feats = new LinkedHashMap<>();

				feats.put("TERM", term.getForm());
				feats.put("LEMMA", term.getLemma());
				if (FEATS_STEM) {
					feats.put("STEM", stemmer.stem(term.getLemma()));
				}
				if (FEATS_POS) {
					feats.put("P", term.getPos());
				}
				feats.put("M", term.getMorphofeat());

				if (FEATS_DEP) {
					Dep to = document.getDepToTerm(term);
					feats.put("DEP.R", DEFAULT_NONE);
					feats.put("DEP.L", DEFAULT_NONE);
					feats.put("DEP.M", DEFAULT_NONE);
					feats.put("DEP.P", DEFAULT_NONE);
					if (to != null) {
						feats.put("DEP.R", to.getRfunc());
						feats.put("DEP.L", to.getRfunc() + "." + to.getFrom().getLemma());
						feats.put("DEP.M", to.getRfunc() + "." + to.getFrom().getMorphofeat());
						feats.put("DEP.P", to.getRfunc() + "." + to.getFrom().getPos());
					}
				}

				if (FEATS_SRL) {
					for (String s : srlFeatures.get(term)) {
						feats.put("SRL." + s, DEFAULT_YES);
					}
				}

				if (FEATS_ENTITIES) {
					String entity = entityTerms.get(term);
					if (entity == null) {
						entity = DEFAULT_NONE;
					}
					if (!skipEmpty || !entity.equals(DEFAULT_NONE)) {
						feats.put("E", entity);
					}
				}

				if (FEATS_SST) {
					String SST = DEFAULT_NONE;
					for (ExternalRef externalRef : term.getExternalRefs()) {
						if (externalRef.getResource().equals("wn30-sst")) {
							SST = externalRef.getReference();
							break;
						}
					}
					if (!skipEmpty || !SST.equals(DEFAULT_NONE)) {
						feats.put("SST", SST);
					}
				}

				if (FEATS_SENTICNET) {
					Collection<SenticNet.Lexeme> snLexemes = senticnetMM.get(term);
					String isInSenticNet = DEFAULT_NONE;
					String bigAptitude = DEFAULT_NONE;
					String bigAttention = DEFAULT_NONE;
					String bigPleasentness = DEFAULT_NONE;
					String bigPolarity = DEFAULT_NONE;
					String bigSensitivity = DEFAULT_NONE;
					if (snLexemes.size() > 0) {
						isInSenticNet = DEFAULT_YES;
						for (SenticNet.Lexeme lexeme : snLexemes) {
							bigAptitude = lexeme.getAptitude() > 0.5 ? DEFAULT_YES : DEFAULT_NONE;
							bigAttention = lexeme.getAttention() > 0.5 ? DEFAULT_YES : DEFAULT_NONE;
							bigPleasentness = lexeme.getPleasentness() > 0.5 ? DEFAULT_YES : DEFAULT_NONE;
							bigPolarity = lexeme.getPolarity() > 0.5 ? DEFAULT_YES : DEFAULT_NONE;
							bigSensitivity = lexeme.getSensitivity() > 0.5 ? DEFAULT_YES : DEFAULT_NONE;
							break;
						}
					}
					if (!skipEmpty || !isInSenticNet.equals(DEFAULT_NONE)) {
						feats.put("SNi", isInSenticNet);
					}
					if (!skipEmpty || !bigAptitude.equals(DEFAULT_NONE)) {
						feats.put("SNa", bigAptitude);
					}
					if (!skipEmpty || !bigAttention.equals(DEFAULT_NONE)) {
						feats.put("SNt", bigAttention);
					}
					if (!skipEmpty || !bigPleasentness.equals(DEFAULT_NONE)) {
						feats.put("SNl", bigPleasentness);
					}
					if (!skipEmpty || !bigPolarity.equals(DEFAULT_NONE)) {
						feats.put("SNp", bigPolarity);
					}
					if (!skipEmpty || !bigSensitivity.equals(DEFAULT_NONE)) {
						feats.put("SNs", bigSensitivity);
					}
				}

				Collection<SubjectivityLexicon.Lexeme> slLexemes = null;
				if (FEATS_MOSCHITTI || FEATS_SUBJLEXICON) {
					slLexemes = subjectivityMM.get(term);
				}

				if (FEATS_MOSCHITTI) {
					String subjLexM = DEFAULT_NONE;
					if (slLexemes.size() > 0) {
						String level = "weak";
						String pol = "neu";
						for (SubjectivityLexicon.Lexeme lexeme : slLexemes) {
							if (lexeme.isStrong()) {
								level = "str";
							}
							pol = lexeme.getPolarity().toString().substring(0, 3).toLowerCase();
							break;
						}
						subjLexM = level + "/" + pol;
					}
					if (!skipEmpty || !subjLexM.equals(DEFAULT_NONE)) {
						feats.put("MOSCHITTI", subjLexM);
					}
				}

				if (FEATS_SUBJLEXICON) {
					String isInSubjLex = DEFAULT_NONE;
					String subjLexM = DEFAULT_NONE;
					String isInSubjLexStrong = DEFAULT_NONE;
					if (slLexemes.size() > 0) {
						isInSubjLex = DEFAULT_YES;
						for (SubjectivityLexicon.Lexeme lexeme : slLexemes) {
							if (lexeme.isStrong()) {
								isInSubjLexStrong = DEFAULT_YES;
							}
							subjLexM = lexeme.getPolarity().toString() + "." + isInSubjLexStrong;
							break;
						}
					}
					if (!skipEmpty || !isInSubjLex.equals(DEFAULT_NONE)) {
						feats.put("SLi", isInSubjLex);
					}
					if (!skipEmpty || !isInSubjLexStrong.equals(DEFAULT_NONE)) {
						feats.put("SLs", isInSubjLexStrong);
					}
					if (!skipEmpty || !subjLexM.equals(DEFAULT_NONE)) {
						feats.put("SLm", subjLexM);
					}
				}

				if (FEATS_INTENSITY) {
					for (Intensities.Type type : Intensities.Type.values()) {
						String typeStr = DEFAULT_NONE;
						if (intensitiesMM.get(term).size() > 0) {
							for (Intensities.Lexeme lexeme : intensitiesMM.get(term)) {
								if (lexeme.getType().equals(type)) {
//								System.out.println(lexeme);
									typeStr = DEFAULT_YES;
								}
							}
						}
						char first = type.toString().charAt(0);
						if (!skipEmpty || !typeStr.equals(DEFAULT_NONE)) {
							feats.put("IN" + first, typeStr);
						}
					}
				}

				if (FEATS_STANFORD) {
					String stanfordLabel = "M";
					if (stanfordTerms.containsKey(term)) {
						stanfordLabel = stanfordTerms.get(term);
					}
					String[] split = stanfordLabel.split("(?<=[\\S])[\\S]*\\s*");
					stanfordLabel = ArrayUtils.implode("", split);
					feats.put("STF", stanfordLabel);
				}

				String wnSense = getWnFromTerm(term);

				if (FEATS_WORDNET) {
					Set<String> termHypernyms = new HashSet<>();
					if (wnSense != null) {
						termHypernyms = WordNet.getHypernyms(wnSense, true);
					}
					if (hypernyms.size() > 0) {
						for (String hypernym : hypernyms) {
							if (termHypernyms.contains(hypernym)) {
								feats.put("WN." + hypernym, DEFAULT_YES);
							}
							else {
								if (!skipEmpty) {
									feats.put("WN." + hypernym, DEFAULT_NONE);
								}
							}
						}
					}
					else {
						for (String hypernym : termHypernyms) {
							feats.put("WN." + hypernym, DEFAULT_YES);
						}
					}
				}

				if (FEATS_SENTIWORDNET) {
					if (!skipEmpty) {
						feats.put("SWN+", DEFAULT_NONE);
						feats.put("SWN-", DEFAULT_NONE);
					}
					if (wnSense != null) {
						PosNegPair swnPair = SentiWordNet.searchValue(wnSense);
						int posTimes = (int) Math.round(swnPair.getPosScore() / .125);
						int negTimes = (int) Math.round(swnPair.getNegScore() / .125);
						if (posTimes > 0) {
							feats.put("SWN+", Integer.toString(posTimes));
						}
						if (negTimes > 0) {
							feats.put("SWN-", Integer.toString(negTimes));
						}
					}
				}

				if (opinionTerms.contains(term)) {
					if (last.equals("O")) {
						last = "B-t";
					}
					else {
						last = "I-t";
					}
				}
				else {
					last = "O";
				}
				feats.put(DEFAULT_CLASSIFICATION_LABEL, last);

				sentence.add(feats);
			}
			ret.add(sentence);
		}

		return ret;
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("yamcha-extractor")
					.withHeader("Extract YAMCHA training set")
					.withOption("i", "input-folder", "the folder of the corpus", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
					.withOption("w", "wordnet-path", "WordNet dict folder", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
					.withOption("s", "sentiwordnet-path", "SentiWordNet file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("o", "output-folder", "output folder", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
					.withOption("l", "label", "label(s), in comma separated format", "LABEL", CommandLine.Type.STRING, true, false, false)
					.withOption("t", "type", String.format("Output type, default %s", DEFAULT_TYPE), "TYPE", CommandLine.Type.STRING, true, false, false)
//					.withOption("e", "extensions", String.format("Input extensions (default %s)", CorpusAnnotator.DEFAULT_NAF_EXTENSIONS), "EXTS", CommandLine.Type.STRING, true, true, false)
					.withOption(null, "seed", "Seed", "NUM", CommandLine.Type.FLOAT, true, false, false)
					.withOption(null, "slot", String.format("Slot size, default %d", DEFAULT_SLOT_SIZE), "NUM", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
					.withOption(null, "split", "Split part (training)", "NUM", CommandLine.Type.POSITIVE_FLOAT, true, false, false)
					.withOption(null, "skip-empty-train", "Skip empty sentences in training")
					.withOption(null, "skip-empty-test", "Skip empty sentences in test")
					.withOption(null, "train-list", "Trining set file list", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption(null, "test-list", "Test set file list", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File mainFolder = cmd.getOptionValue("i", File.class);
			File outputFolder = cmd.getOptionValue("o", File.class);

			File wnFolder = cmd.getOptionValue("w", File.class);
			File swnFolder = cmd.getOptionValue("s", File.class);

			String label = cmd.getOptionValue("l", String.class, DEFAULT_LABEL);
			String[] labels = label.split(",");

			boolean skipEmptyTrain = cmd.hasOption("skip-empty-train");
			boolean skipEmptyTest = cmd.hasOption("skip-empty-test");

			Type type = DEFAULT_TYPE;

			String typeString = cmd.getOptionValue("type", String.class);
			if (typeString != null) {
				try {
					type = Type.valueOf(typeString.toUpperCase());
				} catch (Exception e) {
					throw new CommandLine.Exception(e.getMessage(), e);
				}
			}

			if (type.equals(Type.YAMCHA)) {
				FEATS_SRL = false;
				FEATS_WORDNET = false;
				FEATS_SENTIWORDNET = false;
			}

			Integer slotSize = cmd.getOptionValue("slot", Integer.class, DEFAULT_SLOT_SIZE);
			Float split = cmd.getOptionValue("split", Float.class, DEFAULT_SPLIT);

//			char space = mallet ? ' ' : '\t';

			Long seed = cmd.getOptionValue("seed", Long.class, DEFAULT_SEED);

//			List<String> extensions = null;
//			if (cmd.hasOption("e")) {
//				extensions = cmd.getOptionValues("e", String.class);
//			}
//			if (extensions == null) {
//				extensions = CorpusAnnotator.DEFAULT_NAF_EXTENSIONS;
//			}

			File trainList = cmd.getOptionValue("train-list", File.class);
			File testList = cmd.getOptionValue("test-list", File.class);

			if ((trainList != null && testList == null) || (testList != null && trainList == null)) {
				throw new Exception("Train list and test list must be both declared or both missing");
			}

			// ---

			if (!outputFolder.exists()) {
				boolean createdOutputFolder = outputFolder.mkdirs();
				if (!createdOutputFolder) {
					LOGGER.error("Unable to create {}", outputFolder.getAbsolutePath());
					System.exit(1);
				}
			}

			LOGGER.info("Loading resources");
			senticNet = SenticNet.getInstance();
			subjectivityLexicon = SubjectivityLexicon.getInstance();
			stemmer = StemmerFactory.getInstance(Locale.US);
			intensities = Intensities.getInstance();

			if (wnFolder != null) {
				WordNet.setPath(wnFolder.getAbsolutePath());
				WordNet.init();
			}

			if (swnFolder != null) {
				SentiWordNet.setPath(swnFolder);
				SentiWordNet.init();
			}

			LOGGER.info("Parsing corpus");
			Corpus[] corpuses = new Corpus[2];
			if (trainList != null) {
				List<File> trainFiles = readList(trainList, mainFolder, "naf");
				List<File> testFiles = readList(testList, mainFolder, "naf");
				corpuses[0] = Corpus.create(false, trainFiles);
				corpuses[1] = Corpus.create(false, testFiles);
			}
			else {
				Corpus myCorpus = Corpus.create(false, mainFolder);
				corpuses = myCorpus.split(seed, split, 1.0f - split);
			}

			// WordNet
			Set<String> allHypernyms = new TreeSet<>();

			// Populate columns
			ArrayList<String> columns = new ArrayList<>();
			if (type.equals(Type.YAMCHA)) {
				if (wnFolder != null) {
					LOGGER.info("Collecting WordNet information");
					for (int i = 0; i < 2; i++) {
						for (Path file : corpuses[i].files()) {
							KAFDocument document = corpuses[i].get(file);
							for (Term term : document.getTerms()) {
								String wnSense = getWnFromTerm(term);
								if (wnSense != null && wnSense.length() > 0) {
									Set<String> hypernyms = WordNet.getHypernyms(wnSense, true);
									allHypernyms.addAll(hypernyms);
								}
							}
						}
					}
					LOGGER.info("Loaded {} hypernyms", allHypernyms.size());
				}
				for (Path file : corpuses[0].files()) {
					KAFDocument document = corpuses[0].get(file);
					ArrayList<ArrayList<LinkedHashMap<String, String>>> sentences = extractFeats(document, labels, allHypernyms, false);
					if (columns.size() == 0 && sentences.size() > 0 && sentences.get(0).size() > 0) {
						for (String key : sentences.get(0).get(0).keySet()) {
							if (!key.equals(DEFAULT_CLASSIFICATION_LABEL)) {
								columns.add(key);
							}
						}
						break;
					}
				}
			}

			// Train
			LOGGER.info("Loading training data");
			File trainDataFile = new File(outputFolder.getAbsolutePath() + File.separator + "data.train");
			BufferedWriter trainWriter = new BufferedWriter(new FileWriter(trainDataFile));
			for (Path file : corpuses[0].files()) {
				KAFDocument document = corpuses[0].get(file);
				writeFeats(document, trainWriter, labels, skipEmptyTrain, allHypernyms, type, slotSize);
			}
			trainWriter.close();

			// Test
			LOGGER.info("Loading test data");
			File testDataFile = new File(outputFolder.getAbsolutePath() + File.separator + "data.test");
			BufferedWriter testWriter = new BufferedWriter(new FileWriter(testDataFile));
			for (Path file : corpuses[1].files()) {
				KAFDocument document = corpuses[1].get(file);
				writeFeats(document, testWriter, labels, skipEmptyTest, allHypernyms, type, slotSize);
			}
			testWriter.close();

			if (type.equals(Type.YAMCHA)) {
				File templateFile = new File(outputFolder.getAbsolutePath() + File.separator + "template.crf");
				BufferedWriter templateWriter = new BufferedWriter(new FileWriter(templateFile));
				StringBuffer buffer = new StringBuffer();

				int featNo = 0;
				for (int i = 0; i < columns.size(); i++) {
					String colName = columns.get(i);

					if (colName.equals(DEFAULT_CLASSIFICATION_LABEL)) {
						continue;
					}

					buffer.append("#").append(colName).append("\n");

					if (!colName.startsWith("WN")) {
						for (int offset = -slotSize; offset <= slotSize; offset++) {
							buffer.append("U").append(++featNo).append(":")
									.append("%x[").append(offset).append(",").append(i).append("]")
									.append("\n");
						}
					}
					else {
						buffer.append("U").append(++featNo).append(":")
								.append("%x[").append("0").append(",").append(i).append("]")
								.append("\n");
					}

					if (DOUBLE_FEATURES.contains(colName)) {
						for (int offset = -slotSize; offset <= slotSize - 1; offset++) {
							buffer.append("U").append(++featNo).append(":")
									.append("%x[").append(offset).append(",").append(i).append("]")
									.append("/")
									.append("%x[").append(offset + 1).append(",").append(i).append("]")
									.append("\n");
						}
					}

					if (TRIPLE_FEATURES.contains(colName)) {
						for (int offset = -slotSize; offset <= slotSize - 2; offset++) {
							buffer.append("U").append(++featNo).append(":")
									.append("%x[").append(offset).append(",").append(i).append("]")
									.append("/")
									.append("%x[").append(offset + 1).append(",").append(i).append("]")
									.append("/")
									.append("%x[").append(offset + 2).append(",").append(i).append("]")
									.append("\n");
						}
					}

					buffer.append("\n");
				}

				buffer.append("#BIGRAMS\n");
				buffer.append("B").append("\n");

				templateWriter.write(buffer.toString());
				templateWriter.close();
			}

			LOGGER.debug(columns.toString());

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}

	private static String getWnFromTerm(Term term) {
		String wnSense = term.getWordnetSense();
		if (wnSense == null || wnSense.length() == 0) {
			for (ExternalRef externalRef : term.getExternalRefs()) {
				if (externalRef.getResource().equals("wn30-ukb")) {
					wnSense = externalRef.getReference();
					if (wnSense != null && wnSense.length() > 0) {
						break;
					}
				}
			}
		}

		return wnSense;
	}

	public static List<File> readList(File fileList, File baseFolder, @Nullable String replaceExtension) throws IOException {

		List<File> ret = new ArrayList<>();

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileList));

			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}

				String fileName = baseFolder.getAbsolutePath() + File.separator + line;

				if (replaceExtension != null) {
					fileName = fileName.replaceAll("\\.[^\\.]+$", "." + replaceExtension);
				}

				File file = new File(fileName);
				if (!file.exists()) {
					LOGGER.warn("File {} does not exist", fileName);
					continue;
				}

				ret.add(file);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return ret;
	}

	private static void writeFeats(KAFDocument document, BufferedWriter writer, String[] labels, boolean skipEmptySentences, Set<String> hypernyms, Type type, int slotSize) throws IOException {

		char space = '\t';
		OutputType outputType = OutputType.SINGLE;
		boolean classBefore = false;
		boolean skipEmptyFeatures = true;
		String featurePrefix = "";

		switch (type) {
			case MALLET:
				space = ' ';
				break;
			case MALLET_WINDOW:
				space = ' ';
				outputType = OutputType.COMPLETE;
				break;
			case CRFSUITE:
				outputType = OutputType.COMPLETE;
				classBefore = true;
				break;
			case YAMCHA:
				skipEmptyFeatures = false;
				break;
			case WAPITI:
				outputType = OutputType.COMPLETE;
				featurePrefix = "u:";
				break;
		}

		ArrayList<ArrayList<LinkedHashMap<String, String>>> sentences = extractFeats(document, labels, hypernyms, false);

		String string1 = "";
		String string2 = "";
		if (classBefore) {
			string1 = Character.toString(space);
		}
		else {
			string2 = Character.toString(space);
		}

		string1 += featurePrefix;

		StringBuffer bigBuffer = new StringBuffer();

		for (ArrayList<LinkedHashMap<String, String>> sentence : sentences) {

			boolean isAnnotated = false;
			StringBuffer buffer = new StringBuffer();

			for (int i = 0; i < sentence.size(); i++) {
				LinkedHashMap<String, String> token = sentence.get(i);
				String classification = token.get(DEFAULT_CLASSIFICATION_LABEL);

				if (classBefore) {
					buffer.append(classification);
				}

				switch (outputType) {
					case SINGLE:
						// Features
						for (String key : token.keySet()) {
							if (key.equals(DEFAULT_CLASSIFICATION_LABEL)) {
								continue;
							}

							String value = token.get(key);

							if (key.startsWith("WN")) {
								buffer.append(string1).append(key).append(string2);
							}
							else {
								if (!skipEmptyFeatures || !value.equals(DEFAULT_NONE)) {
									buffer.append(string1).append(key).append(".").append(value).append(string2);
								}
							}
						}

						break;
					case COMPLETE:

						// Sentence features
						if (i == 0) {
							buffer.append(string1).append("BOS").append(string2);
						}

						// Other features
						for (String key : token.keySet()) {
							if (key.equals(DEFAULT_CLASSIFICATION_LABEL)) {
								continue;
							}

//							String value = token.get(key);

							if (key.startsWith("WN")) {
								buffer.append(string1).append(key).append(string2);
							}
							else {
								for (int offset = -slotSize; offset <= slotSize; offset++) {
									LinkedHashMap<String, String> thisToken;
									try {
										thisToken = sentence.get(i + offset);
									} catch (IndexOutOfBoundsException e) {
										continue;
									}

									String thisValue = thisToken.get(key);
									if (thisValue == null) {
										continue;
									}
									if (!skipEmptyFeatures || !thisValue.equals(DEFAULT_NONE)) {
										buffer.append(string1)
												.append("[").append(offset).append("]")
												.append(key).append(".").append(thisValue)
												.append(string2);
									}
								}
							}
						}

						// Sentence features
						if (i == sentence.size() - 1) {
							buffer.append(string1).append("EOS").append(string2);
						}

						break;
				}

				if (!classBefore) {
					buffer.append(classification);
				}

				buffer.append("\n");
			}
			buffer.append("\n");
			if (!skipEmptySentences || isAnnotated) {
				bigBuffer.append(buffer.toString());
			}
		}

		writer.write(bigBuffer.toString());
	}
}
