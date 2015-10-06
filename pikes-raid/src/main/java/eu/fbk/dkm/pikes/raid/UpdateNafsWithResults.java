package eu.fbk.dkm.pikes.raid;

import eu.fbk.dkm.pikes.naflib.Corpus;
import eu.fbk.dkm.pikes.raid.mdfsa.APIManager;
import eu.fbk.dkm.pikes.resources.mpqa.CorpusAnnotator;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static eu.fbk.dkm.pikes.raid.CreateTrainingForExpression.readList;

/**
 * Created by alessio on 17/04/15.
 */

public class UpdateNafsWithResults {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UpdateNafsWithResults.class);
	private static final Integer MAX_DOCS = 10;
	private static final String DEFAULT_LABEL = "gold";

	private static Long DEFAULT_SEED = 2l;
	private static String DEFAULT_CLASSIFICATION_LABEL = "_CLASS";
	private static Float DEFAULT_SPLIT = 0.75f;

	private static Float DEFAULT_NEG_POL = -0.2f;
	private static Float DEFAULT_POS_POL = 0.2f;

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("yamcha-extractor")
					.withHeader("Extract YAMCHA training set")
					.withOption("i", "input-folder", "the folder of the corpus", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
					.withOption("o", "output-folder", "output folder", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
					.withOption("r", "results-file", "CRF++ results file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("l", "label", "label to use", "LABEL", CommandLine.Type.STRING, true, false, true)
					.withOption("e", "extensions", String.format("Input extensions (default %s)", CorpusAnnotator.DEFAULT_NAF_EXTENSIONS), "EXTS", CommandLine.Type.STRING, true, true, false)
					.withOption(null, "sentiment-model", "MDFSA model", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption(null, "sentiment-properties", "MDFSA properties file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption(null, "sentiment-neg-limit", String.format("MDFSA negative limit (default %f)", DEFAULT_NEG_POL), "NUM", CommandLine.Type.FLOAT, true, false, false)
					.withOption(null, "sentiment-pos-limit", String.format("MDFSA positive limit (default %f)", DEFAULT_POS_POL), "NUM", CommandLine.Type.FLOAT, true, false, false)
					.withOption(null, "seed", "Seed", "NUM", CommandLine.Type.FLOAT, true, false, false)
					.withOption(null, "split", "Split part (training)", "NUM", CommandLine.Type.POSITIVE_FLOAT, true, false, false)
					.withOption(null, "train-list", "Training set file list", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption(null, "test-list", "Test set file list", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption(null, "fake", "Fake mode")
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File mainFolder = cmd.getOptionValue("input-folder", File.class);
			File outputFolder = cmd.getOptionValue("output-folder", File.class);

			File resultsFile = cmd.getOptionValue("results-file", File.class);
			String label = cmd.getOptionValue("label", String.class);
			Float split = cmd.getOptionValue("split", Float.class, DEFAULT_SPLIT);
			Long seed = cmd.getOptionValue("seed", Long.class, DEFAULT_SEED);

			List<String> extensions = null;
			if (cmd.hasOption("extensions")) {
				extensions = cmd.getOptionValues("extensions", String.class);
			}
			if (extensions == null) {
				extensions = CorpusAnnotator.DEFAULT_NAF_EXTENSIONS;
			}

			File trainList = cmd.getOptionValue("train-list", File.class);
			File testList = cmd.getOptionValue("test-list", File.class);

			File sentimentModel = cmd.getOptionValue("sentiment-model", File.class);
			File sentimentProperties = cmd.getOptionValue("sentiment-properties", File.class);

			Float negLimit = cmd.getOptionValue("sentiment-neg-limit", Float.class, DEFAULT_NEG_POL);
			Float posLimit = cmd.getOptionValue("sentiment-pos-limit", Float.class, DEFAULT_POS_POL);

			boolean fakeMode = cmd.hasOption("fake");

			if ((trainList != null && testList == null) || (testList != null && trainList == null)) {
				throw new CommandLine.Exception("Train list and test list must be both declared or both missing");
			}

			// ---

			if (!outputFolder.exists()) {
				boolean mkdirs = outputFolder.mkdirs();
				if (!mkdirs) {
					throw new Exception(String.format("Unable to create folder %s", outputFolder.getAbsolutePath()));
				}
			}

			APIManager am = null;
			if (sentimentModel != null && sentimentProperties != null) {
				LOGGER.info("Loading sentiment models");

				Properties prp = new Properties();
				InputStream iS = new FileInputStream(sentimentProperties);
				prp.load(iS);

				am = new APIManager(prp);
				am.loadModel(sentimentModel.getAbsolutePath());
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

			BufferedReader reader = new BufferedReader(new FileReader(resultsFile));
			HashMap<Integer, Integer> startIndex = new HashMap<>();
			HashMap<Integer, Integer> endIndex = new HashMap<>();

			int exprID = -1;
			int j = -1;

			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\\s+");
				if (parts.length <= 1) {
					continue;
				}

				String res = parts[parts.length - 1];
				j++;

				if (res.startsWith("B")) {
					exprID++;
					endIndex.put(exprID, j);
					startIndex.put(j, exprID);
				}
				if (res.startsWith("I")) {
					if (endIndex.get(exprID) == j - 1) {
						endIndex.put(exprID, j);
					}
				}
			}
			LOGGER.info("Total tokens in the test: {}", j + 1);
			reader.close();

			j = -1;
			for (Path file : corpuses[1].files()) {

				String baseFileName = file.toFile().getName();
				String outputFile = outputFolder.getAbsolutePath() + File.separator + baseFileName;

				LOGGER.debug(baseFileName);

				KAFDocument document = corpuses[1].get(file);
				List<Term> terms = document.getTerms();
				for (int i = 0; i < terms.size(); i++) {
					j++;

					if (startIndex.keySet().contains(j)) {
						int length = endIndex.get(startIndex.get(j)) - j + 1;
						Span<Term> termSpan = KAFDocument.newTermSpan();
						for (int k = 0; k < length; k++) {
							Term term;
							try {
								term = terms.get(i + k);
							} catch (Exception e) {
								LOGGER.warn("Error in token {} ({}) in file {}", i + k, j + 1, baseFileName);
								continue;
							}
							termSpan.addTarget(term);
						}

						Opinion opinion = document.newOpinion();
						opinion.setLabel(label);
						Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(termSpan);

						// Sentiment
						if (am != null) {

//							System.out.println(baseFileName);
//
//							HashSet<Term> containingTerms = new HashSet<>();
//							Term firstTerm = termSpan.getFirstTarget();
//							containingTerms.add(firstTerm);
//							System.out.println(document.getTermsByDepAncestors(Collections.singletonList(firstTerm)));
//							containingTerms.addAll(document.getTermsByDepAncestors(Collections.singletonList(firstTerm)));
//
//							System.out.println(containingTerms);
//							System.out.println();

//							List<Dep> deps = document.getDepsFromTerm(firstTerm);
//							for (Dep dep : deps) {
//								containingTerms.add(dep.)
//							}


//							while (!containingTerms.containsAll(termSpan.getTargets())) {
//
//							}

//							for (Term term : termSpan.getTargets()) {
//								final Dep dep = document.getDepToTerm(term);
//								System.out.println(term);
//								System.out.println(dep);
//
//								// Sopra
//								// System.out.println(dep.getFrom());
//
//								System.out.println();
//							}


//							Span<Term> normalizedSpan = NAFUtils.normalizeSpan(document, termSpan);
//							Term head = normalizedSpan.getHead();
//
//							List<Dep> deps = document.getDepsBySent(head.getSent());
//							String[] depList = new String[deps.size()];
//							for (int z = 0; z < deps.size(); z++) {
//								Dep dep = deps.get(z);
//								depList[z] = dep.toString();
//							}
//
//							Integer headID = Integer.parseInt(head.getId().replaceAll("[^0-9]", ""));

//							System.out.println(deps);
//							System.out.println(termSpan.getStr());
//							System.out.println(termSpan.size());
//							System.out.println(termSpan.getHead());
//							System.out.println(normalizedSpan.getStr());
//							System.out.println(normalizedSpan.size());
//							System.out.println(normalizedSpan.getHead());

//							Set<Term> allTerms = document.getTermsByDepAncestors(Collections.singletonList(head));

//							ArrayList<Integer> idsToRemove = new ArrayList<>();
//							for (Term term : allTerms) {
//								Set<Term> children = document.getTermsByDepAncestors(Collections.singletonList(term));
//								children.retainAll(normalizedSpan.getTargets());
//								if (children.size() == 0) {
//									Integer idToRemove = Integer.parseInt(term.getId().replaceAll("[^0-9]", ""));
//									idsToRemove.add(idToRemove);
//								}
//							}
//
//							System.out.println(Arrays.toString(depList));
//							System.out.println(headID);
//							System.out.println(idsToRemove);
//							double computedPolarity = am.evaluateSentence(depList, headID, idsToRemove);
//							System.out.println(computedPolarity);
//							System.out.println();

							double computedPolarity = am.evaluateSentence(termSpan.getStr());

							String polarity = "Neutral";
							if (computedPolarity != -2.0) {
								if (computedPolarity < negLimit) {
									polarity = "Negative";
								}
								if (computedPolarity > posLimit) {
									polarity = "Positive";
								}
							}

							opinionExpression.setPolarity(polarity);
//							System.out.println(termSpan.getStr());
//							System.out.println();
						}
					}
				}

				if (!fakeMode) {
					document.save(outputFile);
				}
			}
			LOGGER.info("Total tokens in the NAFs: {}", j + 1);

//
//
//
//			File templateFile = new File(outputFolder.getAbsolutePath() + File.separator + "template.crf");
//
//			BufferedWriter trainWriter = new BufferedWriter(new FileWriter(trainDataFile));
//			BufferedWriter testWriter = new BufferedWriter(new FileWriter(testDataFile));
//
//			LOGGER.info("Loading resources");
//			senticNet = SenticNet.getInstance();
//			subjectivityLexicon = SubjectivityLexicon.getInstance();
//			stemmer = StemmerFactory.getInstance(Locale.US);
//			intensities = Intensities.getInstance();
//
//			if (wnFolder != null) {
//				WordNet.setPath(wnFolder.getAbsolutePath());
//				WordNet.init();
//			}
//
//			LOGGER.info("Parsing corpus");
//			Corpus myCorpus = Corpus.create(false, mainFolder);
//			Corpus[] corpuses = myCorpus.split(seed, split, 1.0f - split);
//
//			ArrayList<String> columns = new ArrayList<>();
//
//			// Populate columns
//			for (Path file : corpuses[0].files()) {
//				KAFDocument document = corpuses[0].get(file);
//				ArrayList<ArrayList<LinkedHashMap<String, String>>> sentences = extractFeats(document, labels);
//				if (columns.size() == 0 && sentences.size() > 0 && sentences.get(0).size() > 0) {
//					for (String key : sentences.get(0).get(0).keySet()) {
//						if (!key.equals(DEFAULT_CLASSIFICATION_LABEL)) {
//							columns.add(key);
//						}
//					}
//					break;
//				}
//			}
//
//			// Train
//			for (Path file : corpuses[0].files()) {
//				KAFDocument document = corpuses[0].get(file);
//				writeFeats(document, trainWriter, space, labels, skipEmptyTrain);
//			}
//
//			// Test
//			for (Path file : corpuses[1].files()) {
//				KAFDocument document = corpuses[1].get(file);
//				writeFeats(document, testWriter, space, labels, skipEmptyTest);
//			}
//
//			trainWriter.close();
//			testWriter.close();
//
//			BufferedWriter templateWriter = new BufferedWriter(new FileWriter(templateFile));
//			StringBuffer buffer = new StringBuffer();
//
//			int featNo = 0;
//			for (int i = 0; i < columns.size(); i++) {
//				String colName = columns.get(i);
//
//				if (colName.equals(DEFAULT_CLASSIFICATION_LABEL)) {
//					continue;
//				}
//
//				buffer.append("#").append(colName).append("\n");
//
//				for (int offset = -slotSize; offset <= slotSize; offset++) {
//					buffer.append("U").append(++featNo).append(":")
//							.append("%x[").append(offset).append(",").append(i).append("]")
//							.append("\n");
//				}
//
//				if (DOUBLE_FEATURES.contains(colName)) {
//					for (int offset = -slotSize; offset <= slotSize - 1; offset++) {
//						buffer.append("U").append(++featNo).append(":")
//								.append("%x[").append(offset).append(",").append(i).append("]")
//								.append("/")
//								.append("%x[").append(offset + 1).append(",").append(i).append("]")
//								.append("\n");
//					}
//				}
//
//				if (TRIPLE_FEATURES.contains(colName)) {
//					for (int offset = -slotSize; offset <= slotSize - 2; offset++) {
//						buffer.append("U").append(++featNo).append(":")
//								.append("%x[").append(offset).append(",").append(i).append("]")
//								.append("/")
//								.append("%x[").append(offset + 1).append(",").append(i).append("]")
//								.append("/")
//								.append("%x[").append(offset + 2).append(",").append(i).append("]")
//								.append("\n");
//					}
//				}
//
//				buffer.append("\n");
//			}
//
//			buffer.append("#BIGRAMS\n");
//			buffer.append("B").append("\n");
//
//			templateWriter.write(buffer.toString());
//			templateWriter.close();
//
//			LOGGER.debug(columns.toString());

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}

}
