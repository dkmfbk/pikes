package eu.fbk.dkm.pikes.resources.goodbadfor;

import eu.fbk.dkm.pikes.resources.mpqa.Record;
import eu.fbk.dkm.pikes.resources.mpqa.RecordSet;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Created by alessio on 24/03/15.
 */

public class CorpusLoader {

	//	static Logger logger = Logger.getLogger(CorpusLoader.class.getName());
	private static final Logger LOGGER = LoggerFactory.getLogger(CorpusLoader.class);
	private static final int MIN_STR_LEN = 0;
	private static final boolean ENABLE_EXTREME_GUESS = true;
	public static final String DEFAULT_NAMESPACE = "http://eu.fbk.dkm.pikes.resources.mpqa.cs.pitt.edu/corpora/gfbf_corpus/";

	private static int textAfterGuessingOverlap(String text, Record record, int expectedLength, String span1) {
		return textAfterGuessingOverlap(text, record, expectedLength, span1, false);
	}

	private static int textAfterGuessingOverlap(String text, Record record, int expectedLength, String span1, boolean trim) {
		int maxTo = Math.max(expectedLength * 3, 15);
		for (int i = 0; i < maxTo; i++) {
			int start = record.getSpan().begin - i;
			String span = text.substring(start, start + expectedLength);

			if (trim) {
				span1 = span1.replaceAll("[^0-9a-zA-Z]", "");
				span = span.replaceAll("[^0-9a-zA-Z]", "");
			}

			LOGGER.trace("Span1: {}", span);
			LOGGER.trace("Span2: {}", span1);

			if (span1.equals(span) && span.length() > MIN_STR_LEN) {
				LOGGER.trace("Adding {}", i);
				return i;
			}
		}

		return -1;
	}

	public static String getTextFromGateFile(File file) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(file);
		NodeList nList = doc.getElementsByTagName("TextWithNodes");
		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = nList.item(temp);
			return nNode.getTextContent();
		}

		return null;
	}

	public static void main(final String[] args) throws IOException, XMLStreamException {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.goodbadfor-loader")
					.withHeader("Load goodFor/badFor library")
					.withOption("i", "input-path", "the base path of the corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("n", "namespace", String.format("the namespace for generating document URIs, default %s", DEFAULT_NAMESPACE), "NS", CommandLine.Type.STRING, true, false, false)
					.withOption("t", "test", "test only on this file", "FILE", CommandLine.Type.STRING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputPath = cmd.getOptionValue("i", File.class);

			File documentsFolder = new File(inputPath.getAbsolutePath() + File.separator + "GATE" + File.separator);
			File annotationsFolder = new File(inputPath.getAbsolutePath() + File.separator + "MPQA" + File.separator);
			File nafFolder = new File(inputPath.getAbsolutePath() + File.separator + "NAF" + File.separator);

			String namespace = DEFAULT_NAMESPACE;
			if (cmd.hasOption("n")) {
				namespace = cmd.getOptionValue("n", String.class);
			}

			String testFile = cmd.getOptionValue("t", String.class);

			if (!documentsFolder.exists()) {
				LOGGER.error("Folder {} does not exist", documentsFolder.getAbsolutePath());
			}
			if (!annotationsFolder.exists()) {
				LOGGER.error("Folder {} does not exist", annotationsFolder.getAbsolutePath());
			}

			if (nafFolder.exists()) {
				LOGGER.error("Folder {} exists", nafFolder.getAbsolutePath());
			}
			nafFolder.mkdir();

			int skippedRows = 0;
			int totalRows = 0;
			int docsNo = 0;

			Iterator<File> fileIterator;
			fileIterator = FileUtils.iterateFiles(documentsFolder, new String[]{"xml"}, false);

			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				String fileBaseName = FilenameUtils.removeExtension(file.getName());

				if (testFile != null && !testFile.equals(fileBaseName)) {
					continue;
				}
				String nafFileName = fileBaseName + ".naf";
				File nafFile = new File(nafFolder + File.separator + nafFileName);
				File mpqaFile = new File(annotationsFolder.getAbsolutePath() + File.separator + fileBaseName + ".eu.fbk.dkm.pikes.resources.mpqa");

				LOGGER.info(String.format("Loading file %s", mpqaFile));
				if (!mpqaFile.exists()) {
					LOGGER.warn("File {} does not exist", mpqaFile.getAbsolutePath());
					continue;
				}

				String text = getTextFromGateFile(file);
				if (text == null) {
					LOGGER.warn("text is null");
					continue;
				}
				String documentURI = namespace + nafFileName;

				docsNo++;

				LOGGER.trace("Original text length: {}", text.length());
				int originalTextLength = text.length();

				final RecordSet annotations = RecordSet.readFromFile(mpqaFile);
				totalRows += annotations.getRecords().size();
				TreeMap<Integer, Record> records = new TreeMap<>();
				for (Record record : annotations.getRecords()) {
					records.put(record.getSpan().begin, record);
				}

				for (Record record : records.values()) {
					String span1 = record.getValue("span");
					String span2 = record.getSpan().apply(text, false);

					if (span1 == null || span2 == null) {
						continue;
					}

					span1 = StringEscapeUtils.unescapeHtml(span1);

					if (!span1.trim().equals(span2.trim())) {
						int expectedLength = record.getSpan().end - record.getSpan().begin;

						String span1OnlyLetters = span1.replaceAll("[^0-9a-zA-Z]", "");
						String span2OnlyLetters = span2.replaceAll("[^0-9a-zA-Z]", "");

						if (expectedLength != span1.length() && (!span1OnlyLetters.equals(span2OnlyLetters) || span1OnlyLetters.length() < MIN_STR_LEN)) {
							LOGGER.debug("Span: {}", span1);
							LOGGER.debug("Length: {}/{}", span1.length(), expectedLength);
							LOGGER.debug("Text: {}", span2);

							if (ENABLE_EXTREME_GUESS) {
								int offset = textAfterGuessingOverlap(text, record, expectedLength, span1, true);
								text = new StringBuilder(text).insert(record.getSpan().begin - offset, StringUtils.repeat(" ", offset)).toString();
								LOGGER.debug("Guessed offset: {}", offset);
								continue;
							}

							// Skip
							skippedRows++;
							continue;
						}
//						System.out.println("DIFF");
//						System.out.println(record.getSpan());
//						System.out.println(expectedLength);
//						System.out.println(span1);
//						System.out.println(span1.length());
//						System.out.println(span2);
//						System.out.println(span2.length());
//						System.out.println();

						if (span1OnlyLetters.equals(span2OnlyLetters)) {
							LOGGER.trace("Identical unless blanks - {}", span1OnlyLetters);
							continue;
						}

						// Guessing overlap
						int offset = textAfterGuessingOverlap(text, record, expectedLength, span1);
						if (offset != -1) {
							text = new StringBuilder(text).insert(record.getSpan().begin - offset, StringUtils.repeat(" ", offset)).toString();
						}
						else {
							skippedRows++;
							LOGGER.warn("Span not found: {}", record.toString());
						}
					}
				}

				LOGGER.trace("Final text length: {}", text.length());
				int diff = text.length() - originalTextLength;
				if (diff != 0) {
					LOGGER.debug("Difference in length: {}", diff);
				}

				text = text.replaceAll("\\s", "&nbsp;");

				final KAFDocument document = new KAFDocument("en", "v3");
				document.setRawText(text);
				document.createPublic();
				document.getPublic().publicId = Statements.VALUE_FACTORY.createIRI(documentURI).getLocalName();
				document.getPublic().uri = documentURI;
				document.createFileDesc();
				document.getFileDesc().filename = nafFileName;
				document.getFileDesc().title = "-";
				document.save(nafFile.getAbsolutePath());
			}

			LOGGER.info("=== Statistics ===");
			LOGGER.info("Total documents: {}", docsNo);
			LOGGER.info("Total rows: {}", totalRows);
			LOGGER.info("Skipped rows: {}", skippedRows);

//			fileIterator = FileUtils.iterateFiles(annotationsFolder, new String[]{"eu.fbk.dkm.pikes.resources.mpqa"}, false);
//			while (fileIterator.hasNext()) {
//				File file = fileIterator.next();
//				String fileBaseName = FilenameUtils.removeExtension(file.getName());
//				final RecordSet annotations = RecordSet.readFromFile(file);
//				for (Record record : annotations.getRecords()) {
//					System.out.println(fileBaseName);
//					System.out.println(record.getSpan());
//					System.out.println(record.getSpan().apply(texts.get(fileBaseName), false));
//					System.out.println();
//				}
//			}

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}


}
