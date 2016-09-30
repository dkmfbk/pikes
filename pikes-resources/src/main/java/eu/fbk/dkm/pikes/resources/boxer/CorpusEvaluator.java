package eu.fbk.dkm.pikes.resources.boxer;

import eu.fbk.dkm.pikes.resources.mpqa.CorpusAnnotator;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.eval.PrecisionRecall;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Predicate;
import ixa.kaflib.Term;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by alessio on 05/05/15.
 */

public class CorpusEvaluator {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusEvaluator.class);

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.darmstadt-loader")
					.withHeader("Load Boxer corpus and split it")
					.withOption("i", "input-folder", "input folder", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("a", "annotation", "annotation file", "DIR", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputFolder = cmd.getOptionValue("i", File.class);
			final File annotationFile = cmd.getOptionValue("a", File.class);

			List<String> extensions = null;
			extensions = CorpusAnnotator.DEFAULT_NAF_EXTENSIONS;

			ArrayList<String> lines = new ArrayList<>();

			BufferedReader reader = new BufferedReader(new FileReader(annotationFile));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				lines.add(line);
			}
			reader.close();

			PrecisionRecall.Evaluator evaluator = PrecisionRecall.evaluator();

			Iterator<File> fileIterator = FileUtils.iterateFiles(inputFolder, extensions.toArray(new String[extensions.size()]), true);
			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				LOGGER.info("Loading file {}", file.getAbsolutePath());
				KAFDocument document = KAFDocument.createFromFile(file);

				HashMap<Term, String> fnTerms = new HashMap<>();
				for (Predicate predicate : document.getPredicates()) {
					HashSet<String> frameNets = new HashSet<>();
					for (ExternalRef externalRef : predicate.getExternalRefs()) {
						if (!externalRef.getResource().equals("eu.fbk.dkm.pikes.resources.FrameNet")) {
							continue;
						}
						frameNets.add(externalRef.getReference());
					}

					if (frameNets.size() != 1) {
						continue;
					}
					String fn = null;
					for (String fn1 : frameNets) {
						fn = fn1;
					}
					if (fn == null) {
						continue;
					}

					for (Term predicateTerm : predicate.getTerms()) {
						fnTerms.put(predicateTerm, fn);
					}
				}

				int last = Integer.parseInt(FilenameUtils.getBaseName(file.getAbsolutePath()));
				int start = last - (last - 1) % CorpusSplitter.sentencesPerCluster;
				for (int i = start; i < last; i++) {
					int j = i - 1;
					int sent = j - start + 1;

					String[] parts = lines.get(j).split(":");
					String lemma = parts[0];
					List<Term> terms = document.getSentenceTerms(sent + 1);
					Term mainTerm = null;

					LOGGER.debug(" " + sent + " " + j);
					LOGGER.debug(Arrays.toString(parts));
					LOGGER.debug(lemma);
					LOGGER.debug(terms.toString());

					for (Term term : terms) {
						if (term.getLemma().equals(lemma)) {
							mainTerm = term;
						}
					}

					if (mainTerm == null) {
//						LOGGER.info(" " + sent + " " + j);
//						LOGGER.info(Arrays.toString(parts));
//						LOGGER.info(lemma);
//						LOGGER.info(terms.toString());
						evaluator.addFN(1);
						continue;
					}

					if (!mainTerm.getPos().equals("V")) {
						continue;
					}

					if (fnTerms.get(mainTerm) == null) {
//						LOGGER.info(" " + sent + " " + j);
//						LOGGER.info(Arrays.toString(parts));
//						LOGGER.info(lemma);
//						LOGGER.info(terms.toString());
						evaluator.addFN(1);
						continue;
					}

//					evaluator.addTP(1);

					if (fnTerms.get(mainTerm).equals(parts[1])) {
						evaluator.addTP(1);
						continue;
					}

					evaluator.addFN(1);
					evaluator.addFP(1);
				}
			}

			PrecisionRecall precisionRecall = evaluator.getResult();
			System.out.println(precisionRecall.toString());
		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}
}
