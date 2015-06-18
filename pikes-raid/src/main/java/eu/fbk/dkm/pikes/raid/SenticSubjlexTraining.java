package eu.fbk.dkm.pikes.raid;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import eu.fbk.dkm.pikes.resources.SenticNet;
import eu.fbk.dkm.pikes.resources.SubjectivityLexicon;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.dkm.pikes.resources.mpqa.CorpusAnnotator;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.svm.Classifier;
import eu.fbk.dkm.utils.svm.LabelledVector;
import eu.fbk.dkm.utils.svm.Vector;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Term;
import org.apache.commons.io.FileUtils;
import org.fbk.cit.hlt.core.analysis.stemmer.Stemmer;
import org.fbk.cit.hlt.core.analysis.stemmer.StemmerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Created by alessio on 17/04/15.
 */

public class SenticSubjlexTraining {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SenticSubjlexTraining.class);
	private static final Integer MAX_DOCS = 200;

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("yamcha-extractor")
					.withHeader("Extract YAMCHA training set")
					.withOption("i", "input-folder", "the folder of the corpus", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
					.withOption("w", "wordnet-path", "WordNet dict folder", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
//					.withOption("o", "output-folder", "output folder", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
					.withOption("e", "extensions", String.format("Input extensions (default %s)", CorpusAnnotator.DEFAULT_NAF_EXTENSIONS), "EXTS", CommandLine.Type.STRING, true, true, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File mainFolder = cmd.getOptionValue("i", File.class);
			File wnFolder = cmd.getOptionValue("w", File.class);
//			File outputFolder = cmd.getOptionValue("o", File.class);

			List<String> extensions = null;
			if (cmd.hasOption("e")) {
				extensions = cmd.getOptionValues("e", String.class);
			}
			if (extensions == null) {
				extensions = CorpusAnnotator.DEFAULT_NAF_EXTENSIONS;
			}

			Stemmer stemmer = StemmerFactory.getInstance(Locale.US);

//			if (!outputFolder.exists()) {
//				boolean createdOutputFolder = outputFolder.mkdirs();
//				if (!createdOutputFolder) {
//					LOGGER.error("Unable to create {}", outputFolder.getAbsolutePath());
//					System.exit(1);
//				}
//			}
//
//			File trainDataFile = new File(outputFolder.getAbsolutePath() + File.separator + "data.train");
//			BufferedWriter writer = new BufferedWriter(new FileWriter(trainDataFile));

			SenticNet senticNet = SenticNet.getInstance();
			SubjectivityLexicon subjectivityLexicon = SubjectivityLexicon.getInstance();

			WordNet.setPath(wnFolder.getAbsolutePath());
			WordNet.init();

			int numFiles = 0;

			LOGGER.info("Loading file list");

			if (!mainFolder.exists()) {
				LOGGER.error("Folder {} does not exist", mainFolder.getAbsolutePath());
			}

			Iterator<File> fileIterator;
//			Classifier.Parameters parameters = Classifier.Parameters.forSVMLinearKernel(2, new float[]{1, 2.5f}, 1.0f);
			Classifier.Parameters parameters = Classifier.Parameters.forLinearL1LossL2Reg(2, new float[]{1, 2}, 1.0f, 1.0f);
			List<LabelledVector> trainingSet = new ArrayList<>();

//			HashMap<String, KAFDocument> documents = new HashMap<>();

			fileIterator = FileUtils.iterateFiles(mainFolder, extensions.toArray(new String[extensions.size()]), true);
			List<File> files = Lists.newArrayList(fileIterator);
			Collections.shuffle(files);
			for (File file : files) {
				numFiles++;
				if (MAX_DOCS != null && numFiles > MAX_DOCS) {
					break;
				}

				LOGGER.info(String.format("Loading file %s", file));
				KAFDocument document = KAFDocument.createFromFile(file);
//				documents.put(file.getAbsolutePath(), document);

				Multimap<Term, SenticNet.Lexeme> senticnetMM = senticNet.match(document, document.getTerms());
				Multimap<Term, SubjectivityLexicon.Lexeme> subjectivityMM = subjectivityLexicon.match(document, document.getTerms());

				HashSet<Term> opinionTerms = new HashSet<>();

				for (Opinion opinion : document.getOpinions()) {
					if (opinion.getOpinionExpression() == null) {
						continue;
					}

					if (opinion.getLabel() == null) {
						continue;
					}

					if (!opinion.getLabel().contains("gold")) {
						continue;
					}

					if (opinion.getOpinionExpression().getSpan() == null) {
						continue;
					}

					for (Term term : opinion.getOpinionExpression().getSpan().getTargets()) {
						opinionTerms.add(term);
					}
				}

				for (Term term : document.getTerms()) {
					final LabelledVector.Builder builder = Vector.builder();

					boolean inDataset = false;

					if (!senticnetMM.get(term).isEmpty()) {
						builder.set("SENTIC", true);
						inDataset = true;
					}
					if (!subjectivityMM.get(term).isEmpty()) {
						builder.set("SUBJLEX", true);
						inDataset = true;
					}

					if (!inDataset) {
						continue;
					}

					builder.set("LEMMA." + term.getLemma(), true);
					builder.set("MORPHO." + term.getMorphofeat(), true);
					builder.set("POS." + term.getPos(), true);

					int label = 0;
					if (opinionTerms.contains(term)) {
						label = 1;
					}
					LabelledVector vector = builder.build().label(label, null);
					trainingSet.add(vector);
				}
			}

//			FrequencyHashSet<String> wordnets = new FrequencyHashSet<>();
//			for (String fileName : documents.keySet()) {
//				KAFDocument document = documents.get(fileName);
//				List<Term> terms = document.getTerms();
//				for (Term term : terms) {
//					if (!heads.get(document).contains(term)) {
//						continue;
//					}
//
//					List<ExternalRef> externalRefs = term.getExternalRefs();
//					for (ExternalRef externalRef : externalRefs) {
//						String resource = externalRef.getResource();
//						if (resource.equals("wn30-ukb")) {
//							String wn = externalRef.getReference();
//							wordnets.addAll(WordNet.getHyponyms(wn));
//							wordnets.addAll(WordNet.getHypernyms(wn));
//							wordnets.addAll(WordNet.getGenericSet(wn, net.didion.jwnl.data.PointerType.SIMILAR_TO));
//						}
//					}
//				}
//			}
//
//			SortedSet<Map.Entry<String, Integer>> wnSorted = wordnets.getSorted();
//			for (Map.Entry<String, Integer> entry : wnSorted) {
//				System.out.println(WordNet.getLemmas(entry.getKey()));
//				System.out.println(entry);
//				System.out.println();
//			}

//			LOGGER.info("Feature analysis:\n{}", FeatureStats.toString(FeatureStats.forVectors(2, trainingSet, null).values()));

			List<Classifier.Parameters> grid = parameters.grid(25, 10);
//			Classifier classifier = Classifier.train(grid, trainingSet, ConfusionMatrix.labelComparator(PrecisionRecall.Measure.F1, 1, true));
//			ConfusionMatrix crossValidate = Classifier.crossValidate(classifier.getParameters(), trainingSet, 3);

//			ConfusionMatrix crossValidate = Classifier.crossValidate(parameters, trainingSet, 3);
//			LOGGER.info("\n" + crossValidate.toString());

//				SVM mySVM = SVM.train(parameters, trainingSet);
//				List<LabelledVector> output = mySVM.predict(false, trainingSet);
//				ConfusionMatrix confusionMatrix = LabelledVector.evaluate(trainingSet, output, 2);
//				System.out.println(confusionMatrix);

//				mySVM.writeTo();

//			writer.close();
		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
