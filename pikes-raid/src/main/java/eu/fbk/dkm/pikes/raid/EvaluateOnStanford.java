package eu.fbk.dkm.pikes.raid;

import com.google.common.collect.Iterables;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.PrecisionRecallStats;
import eu.fbk.dkm.utils.ValueComparator;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Opinion.OpinionTarget;
import ixa.kaflib.Term;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by alessio on 02/04/15.
 */

public class EvaluateOnStanford {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EvaluateOnStanford.class);
	private static final String STANFORD_LABEL = "stanford-sentiment";

	// todo: move to a utility class
	private static final String DEFAULT_NAF_PARSED_DIR = "NAF-parsed";
	public static List<String> DEFAULT_NAF_EXTENSIONS = new ArrayList<>();

	static {
		DEFAULT_NAF_EXTENSIONS.add("xml");
		DEFAULT_NAF_EXTENSIONS.add("naf");
	}

	public static Map sortByValue(Map unsortedMap, boolean desc) {
		Map sortedMap = new TreeMap(new ValueComparator(unsortedMap, desc));
		sortedMap.putAll(unsortedMap);
		return sortedMap;
	}

	private static void addOpinionToMap(Map<Opinion, Integer> map, Opinion opinion) {
		map.put(opinion, opinion.getOpinionExpression().getTerms().size());
	}

	public static void main(String[] args) {
		CommandLine cmd = null;
		try {
			cmd = CommandLine
					.parser()
					.withName("evaluate")
					.withHeader("Calculate p/r on a dataset")
					.withOption("i", "input-path", "the base path of the corpus", "DIR",
							CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("p", "parsed-dir",
							String.format("folder with the parsed NAFS, default [basedir]/%s", DEFAULT_NAF_PARSED_DIR),
							"DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
					.withOption("e", "extensions", String.format("Input extensions (default %s)", DEFAULT_NAF_EXTENSIONS), "EXTS", CommandLine.Type.STRING, true, true, false)
					.withOption("t", "threshold", "Threshold for neutral", "NUM", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			File mainFolder = cmd.getOptionValue("i", File.class);
			File input = new File(mainFolder.getAbsolutePath() + File.separator + DEFAULT_NAF_PARSED_DIR);
			if (cmd.hasOption("p")) {
				input = cmd.getOptionValue("p", File.class);
			}

			Integer threshold = cmd.getOptionValue("t", Integer.class);

			List<String> extensions = null;
			if (cmd.hasOption("e")) {
				extensions = cmd.getOptionValues("e", String.class);
			}
			if (extensions == null) {
				extensions = DEFAULT_NAF_EXTENSIONS;
			}

			try {
				if (!input.exists()) {
					throw new IOException(String.format("Folder %s does not exist", input.getAbsolutePath()));
				}

				LOGGER.info("Loading file list");
				Iterator<File> fileIterator = FileUtils.iterateFiles(input, extensions.toArray(new String[extensions.size()]), true);

				PrecisionRecallStats precisionRecallStats = new PrecisionRecallStats();

				int goldOpinionCount = 0;

				int numFiles = 0;

				while (fileIterator.hasNext()) {
					File file = fileIterator.next();
					LOGGER.info(String.format("Loading file %s", file));

					KAFDocument document = KAFDocument.createFromFile(file);
					++numFiles;

					OpinionSet mpqaOpinions = new OpinionSet();
					OpinionSet stanfordOpinions = new OpinionSet(true);

					for (Opinion opinion : document.getOpinions()) {
						if (opinion.getLabel() == null || opinion.getLabel().toLowerCase().contains("gold")) {
							if (isValidOpinion(opinion)) {
								mpqaOpinions.add(opinion);
								++goldOpinionCount;
							}
						}
						if (opinion.getLabel() != null && opinion.getLabel().toLowerCase().contains("stanford")) {
							stanfordOpinions.add(opinion);
						}
					}

//					System.out.println(stanfordOpinions.size());
//					System.out.println(mpqaOpinions);

					entryLoop:
					for (OpinionSet.OpinionEntry entry : mpqaOpinions) {
						Opinion opinion = entry.getOpinion();
						HashSet<Term> terms = new HashSet<>(opinion.getOpinionExpression().getTerms());
						LOGGER.debug("Finding {}", opinion.getOpinionExpression().getSpan().getStr());
						for (OpinionSet.OpinionEntry checkEntry : stanfordOpinions) {
							Opinion checkOpinion = checkEntry.getOpinion();
							LOGGER.trace("Checking {}", checkOpinion.getOpinionExpression().getSpan().getStr());
							HashSet<Term> checkTerms = new HashSet<>(checkOpinion.getOpinionExpression().getTerms());
							int sizeBefore = checkTerms.size();
							checkTerms.retainAll(terms);
							if (checkTerms.size() == sizeBefore) {
								LOGGER.debug("Found! {} === {}", opinion.getOpinionExpression().getSpan().getStr(), checkOpinion.getOpinionExpression().getSpan().getStr());

								String stanfordPolarity;
								String goldPolarity = normalizePolarity(opinion.getOpinionExpression().getPolarity());

								String stanfordPolarities = checkOpinion.getOpinionExpression().getStrength();
								if (stanfordPolarities != null && stanfordPolarities.length() > 0) {
									String[] parts = stanfordPolarities.split("\\|");
									Double neg = Double.parseDouble(parts[0].replace(',', '.')) + Double.parseDouble(parts[1].replace(',', '.'));
									Double neu = Double.parseDouble(parts[2].replace(',', '.'));
									Double pos = Double.parseDouble(parts[3].replace(',', '.')) + Double.parseDouble(parts[4].replace(',', '.'));
									if (threshold == null || 100 * neu > threshold) {
										if (neg > neu && neg > pos) {
											stanfordPolarity = "negative";
										}
										else if (pos > neu && pos > neg) {
											stanfordPolarity = "positive";
										}
										else {
											stanfordPolarity = "neutral";
										}
									}
									else {
										if (pos > neg) {
											stanfordPolarity = "positive";
										}
										else if (pos < neg) {
											stanfordPolarity = "negative";
										}
										else {
											stanfordPolarity = "neutral";
										}
									}
								}
								else {
									stanfordPolarity = checkOpinion.getOpinionExpression().getPolarity().toLowerCase();
								}

								if (stanfordPolarity.equals("neutral")) {
									precisionRecallStats.incrementFN();
								}
								else {
									if (stanfordPolarity.contains(goldPolarity)) {
										precisionRecallStats.incrementTP();
									}
									else {
										precisionRecallStats.incrementFP();
									}
								}
								LOGGER.debug("Comparing -{}- and -{}-", opinion.getOpinionExpression().getPolarity(), checkOpinion.getOpinionExpression().getPolarity());
								continue entryLoop;
							}
						}
						LOGGER.debug("Not found");
					}
				}

				LOGGER.info("Precision: {}", precisionRecallStats.getPrecision());
				LOGGER.info("Recall: {}", precisionRecallStats.getRecall());
				LOGGER.info("F1: {}", precisionRecallStats.getFMeasure());
				LOGGER.info("(computed on {} gold opinions and {} files)", goldOpinionCount, numFiles);

			} catch (Exception e) {
				LOGGER.error(e.getMessage());
				e.printStackTrace();
			}
		} catch (Exception e) {
			CommandLine.fail(e);
		}
	}

	private static String normalizePolarity(String polarity) {
		String p = polarity.toLowerCase();
		if (p.contains("pos")) {
			return "positive";
		}
		else if (p.contains("neg")) {
			return "negative";
		}
		else {
			return "neutral";
		}
	}


	private static boolean isValidOpinion(final Opinion opinion) {
		final OpinionTarget target = opinion.getOpinionTarget();
		final OpinionExpression exp = opinion.getOpinionExpression();
		if (exp != null && target != null && exp.getPolarity() != null && exp.getSpan() != null
				&& exp.getSpan().size() > 0 && target.getSpan() != null
				&& target.getSpan().size() > 0) {
			final int id = opinion.getOpinionTarget().getSpan().getTargets().get(0).getSent();
			for (final Term term : Iterables.concat(exp.getTerms(), target.getTerms())) {
				if (term.getSent() != id) {
					return false;
				}
			}
			if (normalizePolarity(exp.getPolarity()).equals("neutral")) {
				return false;
			}
			return true;
		}
		return false;
	}

}
