package eu.fbk.dkm.pikes.resources.goodbadfor;

import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.fbk.cit.hlt.thewikimachine.util.FrequencyHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Created by alessio on 01/04/15.
 */

public class CorpusAnalyzer {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorpusAnalyzer.class);

	public static String spanToLemmas(Span<Term> span) {
		StringBuffer stringBuffer = new StringBuffer();
		for (Term term : span.getTargets()) {
			stringBuffer.append(term.getLemma().toLowerCase());
			stringBuffer.append(" ");
		}
		return stringBuffer.toString().trim();
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.goodbadfor-analyzer")
					.withHeader("Analyze the corpus and makes statistics")
					.withOption("i", "input-path", "the base path of the corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("t", "test", "test only on this file", "FILE", CommandLine.Type.STRING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputPath = cmd.getOptionValue("i", File.class);

			File nafFolder = new File(inputPath.getAbsolutePath() + File.separator + "NAF-parsed" + File.separator);
			String testFile = cmd.getOptionValue("t", String.class);

			if (!nafFolder.exists()) {
				LOGGER.error("Folder {} does not exist", nafFolder.getAbsolutePath());
			}

			Iterator<File> fileIterator;
			fileIterator = FileUtils.iterateFiles(nafFolder, new String[]{"naf"}, false);

			FrequencyHashSet influenceRet = new FrequencyHashSet();
			FrequencyHashSet influenceRev = new FrequencyHashSet();

			FrequencyHashSet goodFor = new FrequencyHashSet();
			FrequencyHashSet badFor = new FrequencyHashSet();

			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				String fileBaseName = FilenameUtils.removeExtension(file.getName());

				if (testFile != null && !testFile.equals(fileBaseName)) {
					continue;
				}

				LOGGER.debug(String.format("Loading file %s", file));
				try {
					KAFDocument document = KAFDocument.createFromFile(file);
					List<Opinion> opinionList = document.getOpinions();
					for (Opinion opinion : opinionList) {
						if (opinion.getLabel().equals("gold-influencer")) {
							Opinion.OpinionExpression expression = opinion.getOpinionExpression();
							if (expression.getPolarity().equals("reverse")) {
								influenceRev.add(spanToLemmas(expression.getSpan()));
							}
							if (expression.getPolarity().equals("retain")) {
								influenceRet.add(spanToLemmas(expression.getSpan()));
							}
						}

						if (opinion.getLabel().equals("gold-gfbf")) {
							Opinion.OpinionExpression expression = opinion.getOpinionExpression();
							if (expression.getPolarity().equals("goodfor")) {
								goodFor.add(spanToLemmas(expression.getSpan()));
							}
							if (expression.getPolarity().equals("badfor")) {
								badFor.add(spanToLemmas(expression.getSpan()));
							}
						}
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage());
				}
			}

			System.out.println(influenceRet.getSorted());
			System.out.println(influenceRev.getSorted());
			System.out.println(goodFor.getSorted());
			System.out.println(badFor.getSorted());
		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}
}
