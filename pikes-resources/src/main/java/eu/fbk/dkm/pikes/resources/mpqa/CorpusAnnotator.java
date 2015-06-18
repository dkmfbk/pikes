package eu.fbk.dkm.pikes.resources.mpqa;

import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Term;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 20/03/15.
 */

public class CorpusAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorpusAnnotator.class);
	public static final String DEFAULT_NAF_PARSED_DIR = "NAF-parsed";

	static Pattern keyValuePatt = Pattern.compile("^([^=]+)=(.*)$");
	static Pattern spanPatt = Pattern.compile("^([^,]*),([^,]*)$");
	public static List<String> DEFAULT_NAF_EXTENSIONS = new ArrayList<>();

	public static String GOLD_LABEL = "gold-eu.fbk.dkm.pikes.resources.mpqa";

	static {
		DEFAULT_NAF_EXTENSIONS.add("xml");
		DEFAULT_NAF_EXTENSIONS.add("naf");
	}

	public static List<Term> getSpan(List<Term> terms, Span interval) {
		if (interval == null) {
			return new ArrayList<Term>();
		}

		int start = interval.begin;
		int end = interval.end - 1;

		LOGGER.debug("Start: " + start + " - End: " + end);
		return getSpan(terms, start, end);
	}

	public static List<Term> getSpan(List<Term> terms, String interval) {
		if (interval == null) {
			return new ArrayList<Term>();
		}

		Matcher matcher = spanPatt.matcher(interval);
		if (!matcher.matches()) {
			return new ArrayList<Term>();
		}

		int start = Integer.parseInt(matcher.group(1));
		int end = Integer.parseInt(matcher.group(2)) - 1;

		LOGGER.debug("Start: " + start + " - End: " + end);
		return getSpan(terms, start, end);
	}

	public static List<Term> getSpan(List<Term> terms, int start, int end) {
		List<Term> ret = new ArrayList<>();

		for (Term t : terms) {
			int tStart = t.getOffset();
			int tEnd = t.getOffset() + t.getLength();
			if ((tEnd >= start && tEnd <= end) || (tStart >= start && tStart <= end)) {
				ret.add(t);
//				System.out.println(t);
//				System.out.println(t.getOffset());
//				System.out.println(t.getLength());
			}
		}

		return ret;
	}

	public static void main(String[] args) {
		CommandLine cmd = null;
		try {
			cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.mpqa-annotator")
					.withHeader("Annotated files with MPQA annotations")
					.withOption("i", "input-path", "the base path of the MPQA corpus", "DIR",
							CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("o", "output",
							String.format("the output path where to load and save produced files, default [basedir]/%s", DEFAULT_NAF_PARSED_DIR),
							"DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
					.withOption("a", "annotation",
							String.format("the annotation file, default [basedir]/%s", CorpusPreprocessor.DEFAULT_ANNOTATION_TSV),
							"FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("e", "extensions", String.format("Input extensions (default %s)", DEFAULT_NAF_EXTENSIONS), "EXTS", CommandLine.Type.STRING, true, true, false)
					.withOption("t", "test", "test only on this file", "FILE", CommandLine.Type.STRING, true, false, false)
					.withOption("f", "force", "Force opinion")
					.withOption("F", "fake", "Fake mode, do not write to files")
					.withOption("s", "exclude-source-local-null", "Exclude opinion if source is null")
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);
		} catch (Throwable ex) {
			CommandLine.fail(ex);
			System.exit(1);
		}

		boolean forceOpinion = false;
		if (cmd.hasOption("force")) {
			forceOpinion = true;
		}

		boolean fake = false;
		if (cmd.hasOption("fake")) {
			fake = true;
		}

		boolean includeNullSources = true;
		if (cmd.hasOption("s")) {
			includeNullSources = false;
		}

		File mainFolder = cmd.getOptionValue("i", File.class);
		String testFile = cmd.getOptionValue("t", String.class);

		File input = new File(mainFolder.getAbsolutePath() + File.separator + DEFAULT_NAF_PARSED_DIR);
		if (cmd.hasOption("o")) {
			input = cmd.getOptionValue("o", File.class);
		}

		File mpqa = new File(mainFolder.getAbsolutePath() + File.separator + CorpusPreprocessor.DEFAULT_ANNOTATION_TSV);
		if (cmd.hasOption("a")) {
			mpqa = cmd.getOptionValue("a", File.class);
		}

		List<String> extensions = null;
		if (cmd.hasOption("e")) {
			extensions = cmd.getOptionValues("e", String.class);
		}
		if (extensions == null) {
			extensions = DEFAULT_NAF_EXTENSIONS;
		}

		HashMap<String, HashSet<HashMap<String, String>>> opinionsByDocument = new HashMap<>();

		try {
			if (!input.exists()) {
				throw new IOException(String.format("Folder %s does not exist", input.getAbsolutePath()));
			}
			if (!mpqa.exists()) {
				throw new IOException(String.format("File %s does not exist", input.getAbsolutePath()));
			}

			LOGGER.info("Loading TSV file");
			BufferedReader reader = new BufferedReader(new FileReader(mpqa));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				if (parts.length < 5) {
					continue;
				}

				HashMap<String, String> properties = new HashMap<>();
				for (String s : parts) {
					Matcher matcher = keyValuePatt.matcher(s);
					if (matcher.matches()) {
						properties.put(matcher.group(1), matcher.group(2));
					}
				}

				String document = properties.get("document");
				LOGGER.trace(document);
				if (document == null) {
					continue;
				}

				if (!opinionsByDocument.containsKey(document)) {
					opinionsByDocument.put(document, new HashSet<HashMap<String, String>>());
				}
				opinionsByDocument.get(document).add(properties);
			}
			reader.close();

			LOGGER.info("Loading file list");
			Iterator<File> fileIterator = FileUtils.iterateFiles(input, extensions.toArray(new String[extensions.size()]), true);

			while (fileIterator.hasNext()) {
				File file = fileIterator.next();

				String fileBaseName = FilenameUtils.removeExtension(file.getName());
				if (testFile != null && !testFile.equals(fileBaseName)) {
					continue;
				}

				LOGGER.info(String.format("Loading file %s", file));
				KAFDocument document = KAFDocument.createFromFile(file);

				// Check if there are already opinions
				List<Opinion> opinions = document.getOpinions();
				boolean hasGoldOpinions = false;
				for (Opinion opinion : opinions) {
					if (opinion.getLabel().equals(GOLD_LABEL)) {
						hasGoldOpinions = true;
						break;
					}
				}

				if (hasGoldOpinions && !forceOpinion) {
					LOGGER.info("Gold opinions already present, skipping...");
				}
				else {
					List<Term> terms = document.getTerms();

					String documentID = document.getPublic().uri;
					HashSet<HashMap<String, String>> map = opinionsByDocument.get(documentID);
					if (map == null) {
						continue;
					}

					for (HashMap<String, String> properties : map) {

						// Source
						List<Term> sourceSpan = new ArrayList<>();
						String sourceLocal = properties.get("source-local");
						if (sourceLocal == null && !includeNullSources) {
							LOGGER.trace("source-local is null");
							continue;
						}
						if (sourceLocal != null) {
							String[] parts = sourceLocal.split("\\|");
							for (String part : parts) {
								sourceSpan.addAll(getSpan(terms, part));
							}
						}

						// Target
						List<Term> targetSpan = new ArrayList<>();
						targetSpan.addAll(getSpan(terms, properties.get("target")));

						// Attitude
						List<Term> attitudeSpan = new ArrayList<>();
						attitudeSpan.addAll(getSpan(terms, properties.get("expression")));

						Opinion opinion = document.newOpinion();
						opinion.setLabel(GOLD_LABEL + "-" + properties.get("type"));
						LOGGER.debug("Adding opinion {}", properties.get("sentence"));

						if (sourceSpan.size() > 0) {
							opinion.createOpinionHolder(KAFDocument.newTermSpan(sourceSpan));
						}
						if (targetSpan.size() > 0) {
							opinion.createOpinionTarget(KAFDocument.newTermSpan(targetSpan));
						}
						if (attitudeSpan.size() > 0) {
							opinion.createOpinionExpression(KAFDocument.newTermSpan(attitudeSpan));
							opinion.getOpinionExpression().setPolarity(properties.get("sentiment"));
							opinion.getOpinionExpression().setStrength(properties.get("intensity"));
						}
					}
				}

				NAFFilter.builder(false).withSRLRoleLinking(true, true)
						.withOpinionLinking(true, true).build().filter(document);

				if (!fake) {
					document.save(file.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();
		}

	}
}
