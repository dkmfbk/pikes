package eu.fbk.dkm.pikes.resources.ontonotes;

import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.FrequencyHashSet;
import org.apache.commons.io.Charsets;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by alessio on 20/08/15.
 */

public class VerbNetStatisticsExtractor {

	File ontonotesDir = null, senseDir = null;
	FrequencyHashSet<String> vnTotals = new FrequencyHashSet<>();
	FrequencyHashSet<String> fnTotals = new FrequencyHashSet<>();

	public VerbNetStatisticsExtractor() {

	}

	public static void main(String[] args) {

		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("VerbNetStatisticsExtractor")
					.withHeader("Extracts statistics from OntoNotes on frequency of VerbNet/FrameNet")
					.withOption("n", "ontonotes", "OntoNotes folder", "FOLDER", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk.nafview")).parse(args);

			final File dir = cmd.getOptionValue("n", File.class);
			final File output = cmd.getOptionValue("o", File.class);

			VerbNetStatisticsExtractor statisticsExtractor = new VerbNetStatisticsExtractor();
			statisticsExtractor.loadDir(dir.getAbsolutePath());
			try {
				statisticsExtractor.loadFrequencies();
			} catch (Exception e) {
				e.printStackTrace();
			}

			BufferedWriter writer = new BufferedWriter(new FileWriter(output));
			for (String key : statisticsExtractor.getVnTotals().keySet()) {
				writer.append("VN").append("\t").append(key).append("\t").append(statisticsExtractor.getVnTotals().get(key).toString()).append("\n");
			}
			for (String key : statisticsExtractor.getFnTotals().keySet()) {
				writer.append("FN").append("\t").append(key).append("\t").append(statisticsExtractor.getFnTotals().get(key).toString()).append("\n");
			}
			writer.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}

	public void loadDir(String onDir) {
		ontonotesDir = new File(onDir + "/data/files/data/english/annotations/");
		senseDir = new File(onDir + "/data/files/data/english/metadata/sense-inventories/");
	}

	public FrequencyHashSet<String> getVnTotals() {
		return vnTotals;
	}

	public FrequencyHashSet<String> getFnTotals() {
		return fnTotals;
	}

	public void loadFrequencies(String fileName) throws IOException {
		vnTotals = new FrequencyHashSet<>();
		fnTotals = new FrequencyHashSet<>();

		List<String> lines = Files.readLines(new File(fileName), Charset.defaultCharset());
		for (String line : lines) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			if (line.startsWith("#")) {
				continue;
			}

			String[] parts = line.split("\\s+");
			if (parts.length < 3) {
				continue;
			}

			if (parts[0].equals("FN")) {
				fnTotals.add(parts[1], Integer.parseInt(parts[2]));
			}
			if (parts[0].equals("VN")) {
				vnTotals.add(parts[1], Integer.parseInt(parts[2]));
			}
		}

	}

	public void loadFrequencies() throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {

		if (ontonotesDir == null || senseDir == null) {
			return;
		}

		HashMap<String, HashSet<String>> vnMappings = new HashMap<>();
		HashMap<String, HashSet<String>> fnMappings = new HashMap<>();
		vnTotals = new FrequencyHashSet<>();
		fnTotals = new FrequencyHashSet<>();

		DefaultHandler handler = new DefaultHandler() {

			String senseID = null;
			String lemma = null;
			boolean inVn = false;
			boolean inFn = false;

			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				if (qName.equals("sense")) {
					senseID = attributes.getValue("n");
				}
				if (qName.equals("inventory")) {
					lemma = attributes.getValue("lemma");
				}
				if (qName.equals("vn")) {
					inVn = true;
				}
				if (qName.equals("fn")) {
					inFn = true;
				}
			}

			public void endElement(String uri, String localName, String qName) throws SAXException {
				if (qName.equals("vn")) {
					inVn = false;
				}
				if (qName.equals("fn")) {
					inFn = false;
				}
			}

			public void characters(char ch[], int start, int length) throws SAXException {
				if (inVn || inFn) {
					String value = new String(ch, start, length);

					if (value.trim().length() > 0 &&
							!value.equals("NM") &&
							!value.equals("NP")) {

						String key = lemma + "-" + senseID;

						String[] parts = value.split("[,\\s]+");
						for (String part : parts) {
							part = part.trim().toLowerCase();

							if (inVn) {
								if (!vnMappings.containsKey(key)) {
									vnMappings.put(key, new HashSet<>());
								}
								vnMappings.get(key).add(part);
							}
							if (inFn) {
								if (!fnMappings.containsKey(key)) {
									fnMappings.put(key, new HashSet<>());
								}
								fnMappings.get(key).add(part);
							}
						}
					}
				}
			}

		};

		SAXParserFactory spf = SAXParserFactory.newInstance();

		spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		spf.setFeature("http://xml.org/sax/features/validation", false);

		for (File f : Files.fileTreeTraverser().preOrderTraversal(senseDir)) {
			if (f.isDirectory()) {
				continue;
			}
			if (!f.getAbsolutePath().endsWith(".xml")) {
				continue;
			}

			String xml = Files.toString(f, Charset.defaultCharset());

			SAXParser saxParser = spf.newSAXParser();

			InputSource is = new InputSource(new StringReader(xml));

			try {
				saxParser.parse(is, handler);
			} catch (Exception e) {
				System.err.println("Error in file " + f);
			}
		}

		for (File f : Files.fileTreeTraverser().preOrderTraversal(ontonotesDir)) {

			if (f.isDirectory()) {
				continue;
			}
			if (!f.getAbsolutePath().endsWith(".sense")) {
				continue;
			}

			List<String> lines = Files.readLines(f, Charsets.UTF_8);
			for (String line : lines) {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}

				String[] parts = line.split("\\s+");
				String lemma = parts[3];
				String sense = parts[parts.length - 1];

				String key = lemma + "-" + sense;
				if (vnMappings.get(key) != null) {
					for (String vn : vnMappings.get(key)) {
						vnTotals.add(vn);
					}
				}
				if (fnMappings.get(key) != null) {
					for (String fn : fnMappings.get(key)) {
						fnTotals.add(fn);
					}
				}
			}
		}
	}

}
