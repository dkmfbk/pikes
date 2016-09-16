package eu.fbk.dkm.pikes.resources.darmstadt;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.dkm.pikes.naflib.Corpus;
import ixa.kaflib.KAFDocument;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Created by alessio on 25/05/15.
 */

public class IdentifyDocuments {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IdentifyDocuments.class);

	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	public static int computeLevenshteinDistance(String str1, String str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++) {
			distance[i][0] = i;
		}
		for (int j = 1; j <= str2.length(); j++) {
			distance[0][j] = j;
		}

		for (int i = 1; i <= str1.length(); i++) {
			for (int j = 1; j <= str2.length(); j++) {
				distance[i][j] = minimum(
						distance[i - 1][j] + 1,
						distance[i][j - 1] + 1,
						distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
			}
		}

		return distance[str1.length()][str2.length()];
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("yamcha-extractor")
					.withHeader("Check ESWC dataset with Darmstadt")
					.withOption("i", "input-folder", "the folder of the NAF corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("d", "dataset-file", "the XML file provided from the task organizers", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output-file", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File inputFolder = cmd.getOptionValue("input-folder", File.class);
			File datasetFile = cmd.getOptionValue("dataset-file", File.class);
			File outputFile = cmd.getOptionValue("output-file", File.class);

			HashMap<String, String> textToFile = new HashMap<>();

			Corpus corpus = Corpus.create(false, inputFolder);
			for (Path file : corpus.files()) {

//				if (!file.toFile().getAbsolutePath().contains("webs-review-66EE-776CCC4-39995BC2-prod6")) {
//					continue;
//				}

				KAFDocument document = KAFDocument.createFromFile(file.toFile());
				String text = document.getRawText();
				text = text.replaceAll("[^a-zA-Z]", "");
				textToFile.put(text, file.toFile().getName());
			}

			StringBuffer buffer = new StringBuffer();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(datasetFile);
			NodeList nList = doc.getElementsByTagName("text");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				String text = nNode.getTextContent();
//				if (!text.contains("http://www.epinions.com/webs-review-66EE-776CCC4-39995BC2-prod6")) {
//					continue;
//				}
				text = text.replaceAll("[^a-zA-Z]", "");
				if (textToFile.keySet().contains(text)) {
					buffer.append(textToFile.get(text)).append("\n");
				}
				else {

					int found = 0;
					String fileFound = null;

					for (String key : textToFile.keySet()) {
						int distance = computeLevenshteinDistance(key, text);
						double ratio = (distance * 1.0) / (key.length() * 1.0);
						if (ratio < 0.02) {
							found++;
							fileFound = key;
						}
					}

					if (found == 1) {
						buffer.append(textToFile.get(fileFound)).append("\n");
					}
					else {
						System.out.println("---");
						System.out.println(nNode.getTextContent());
						System.out.println("NOT FOUND!");
					}
				}
			}

			Files.write(buffer.toString(), outputFile, Charsets.UTF_8);

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
