package eu.fbk.dkm.pikes.resources.goodbadfor;

import eu.fbk.dkm.pikes.resources.mpqa.Record;
import eu.fbk.dkm.pikes.resources.mpqa.RecordSet;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Term;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by alessio on 24/03/15.
 */

public class CorpusAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorpusAnnotator.class);

	public static void main(final String[] args) throws IOException, XMLStreamException {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.goodbadfor-annotator")
					.withHeader("Annotated files with goodFor/badFor annotations")
					.withOption("i", "input-path", "the base path of the corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("t", "test", "test only on this file", "FILE", CommandLine.Type.STRING, true, false, false)
					.withOption("f", "force", "Force opinion")
					.withOption("s", "skip", "Skip writing files and show them")
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputPath = cmd.getOptionValue("i", File.class);

			File annotationsFolder = new File(inputPath.getAbsolutePath() + File.separator + "MPQA" + File.separator);
			File nafFolder = new File(inputPath.getAbsolutePath() + File.separator + "NAF-parsed" + File.separator);
//			File documentsFolder = new File(inputPath.getAbsolutePath() + File.separator + "GATE" + File.separator);

			boolean forceOpinion = false;
			if (cmd.hasOption("force")) {
				forceOpinion = true;
			}

			boolean skip = false;
			if (cmd.hasOption("skip")) {
				skip = true;
			}

			String testFile = cmd.getOptionValue("t", String.class);

			if (!annotationsFolder.exists()) {
				LOGGER.error("Folder {} does not exist", annotationsFolder.getAbsolutePath());
			}

			if (!nafFolder.exists()) {
				LOGGER.error("Folder {} does not exist", nafFolder.getAbsolutePath());
			}

//			if (!documentsFolder.exists()) {
//				LOGGER.error("Folder {} does not exist", documentsFolder.getAbsolutePath());
//			}

			Iterator<File> fileIterator;
			fileIterator = FileUtils.iterateFiles(nafFolder, new String[]{"naf"}, false);
			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				String fileBaseName = FilenameUtils.removeExtension(file.getName());

				if (testFile != null && !testFile.equals(fileBaseName)) {
					continue;
				}

				File mpqaFile = new File(annotationsFolder.getAbsolutePath() + File.separator + fileBaseName + ".eu.fbk.dkm.pikes.resources.mpqa");
//				File gateFile = new File(documentsFolder.getAbsolutePath() + File.separator + fileBaseName + ".xml");

//				String xmlText = CorpusLoader.getTextFromGateFile(gateFile);

				LOGGER.info(String.format("Loading file %s", mpqaFile));
				if (!mpqaFile.exists()) {
					LOGGER.warn("File {} does not exist", mpqaFile.getAbsolutePath());
					continue;
				}

				String text = "";
				LOGGER.info(String.format("Loading file %s", file));
				KAFDocument document = KAFDocument.createFromFile(file);
				text = document.getRawText();
				text = StringEscapeUtils.unescapeHtml(text);
				List<Term> terms = document.getTerms();

				// Check if there are already opinions
				List<Opinion> opinions = document.getOpinions();
				if (opinions.size() > 0 && !forceOpinion) {
					LOGGER.info("Opinions already present, skipping...");
					continue;
				}

				final RecordSet annotations = RecordSet.readFromFile(mpqaFile);

				HashMap<String, Record> index = new HashMap<>();

				for (Record record : annotations.getRecords()) {

					String span1 = record.getSpan().apply(text);
					String span2 = record.getValue("span");

					if (span1 == null || span2 == null) {
						continue;
					}

					span1 = StringEscapeUtils.unescapeHtml(span1);
					span2 = StringEscapeUtils.unescapeHtml(span2);

					String span1OnlyLetters = span1.replaceAll("[^0-9a-zA-Z]", "");
					String span2OnlyLetters = span2.replaceAll("[^0-9a-zA-Z]", "");

					if (!span1OnlyLetters.equals(span2OnlyLetters)) {
						LOGGER.trace(span1);
						LOGGER.trace(span2);
						LOGGER.warn("The span is different, skipping");
						continue;
					}

					String id = record.getValue("id");
					if (id == null) {
						LOGGER.warn("ID is null");
						continue;
					}

//					if (index.containsKey(id)) {
//						LOGGER.warn("ID {} already exist", id);
//						continue;
//					}

					index.put(id, record);
				}

				for (Record record : annotations.getRecords()) {
					String type = record.getName();
					if (type == null) {
						continue;
					}
					if (type.equals("gfbf") || type.equals("influencer")) {

						String label = "gold-" + type;
						String attribute = "polarity";
						if (type.equals("influencer")) {
							attribute = "effect";
						}

						LOGGER.debug(record.toString());

						try {
							Record agent = index.get(record.getValue("agent"));
							Record target = index.get(record.getValue("object"));

							List<Term> attitudeSpan = new ArrayList<>();
							List<Term> targetSpan = new ArrayList<>();
							List<Term> sourceSpan = new ArrayList<>();

							attitudeSpan.addAll(eu.fbk.dkm.pikes.resources.mpqa.CorpusAnnotator.getSpan(terms, record.getSpan()));

							Opinion opinion = document.newOpinion();
							opinion.setLabel(label);

							if (agent != null) {
								sourceSpan.addAll(eu.fbk.dkm.pikes.resources.mpqa.CorpusAnnotator.getSpan(terms, agent.getSpan()));
								if (sourceSpan.size() > 0) {
									Opinion.OpinionHolder opinionHolder = opinion.createOpinionHolder(KAFDocument.newTermSpan(sourceSpan));
									String attitude = agent.getValue("writerAttitude");
									if (attitude != null) {
										opinionHolder.setType(attitude);
									}
								}
							}

							if (target != null) {
								targetSpan.addAll(eu.fbk.dkm.pikes.resources.mpqa.CorpusAnnotator.getSpan(terms, target.getSpan()));
								if (targetSpan.size() > 0) {
									Opinion.OpinionTarget opinionTarget = opinion.createOpinionTarget(KAFDocument.newTermSpan(targetSpan));
									String attitude = target.getValue("writerAttitude");
									if (attitude != null) {
										opinionTarget.setType(attitude);
									}
								}
							}


							if (attitudeSpan.size() > 0) {
								opinion.createOpinionExpression(KAFDocument.newTermSpan(attitudeSpan));
								opinion.getOpinionExpression().setPolarity(record.getValue(attribute));
							}

						} catch (Exception e) {
							LOGGER.warn(e.getMessage());
							e.printStackTrace();
						}


					}
				}

				if (skip) {
					System.out.println(document);
				}
				else {
					document.save(file.getAbsolutePath());
				}
			}

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}


}
