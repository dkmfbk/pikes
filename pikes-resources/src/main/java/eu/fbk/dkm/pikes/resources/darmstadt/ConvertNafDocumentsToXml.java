package eu.fbk.dkm.pikes.resources.darmstadt;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.google.common.collect.Sets;

import org.openrdf.model.impl.URIImpl;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;

import eu.fbk.dkm.pikes.naflib.Corpus;
import eu.fbk.dkm.utils.CommandLine;

/**
 * Created by alessio on 26/05/15.
 */

public class ConvertNafDocumentsToXml {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ConvertNafDocumentsToXml.class);

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("yamcha-extractor")
					.withHeader("Check ESWC dataset with Darmstadt")
					.withOption("i", "input-folder", "the folder of the NAF corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("o", "output-file", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withOption("l", "label", "opinion label", "STRING", CommandLine.Type.STRING, true, false, true)
					.withOption("n", "numeric", "use numeric values for IDs")
					.withOption(null, "list", "use list of file to sort", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File inputFolder = cmd.getOptionValue("input-folder", File.class);
			File outputFile = cmd.getOptionValue("output-file", File.class);
			Set<String> labels = Sets.newHashSet(cmd.getOptionValue("label", String.class, "").split(","));
			
			File list = cmd.getOptionValue("list", File.class);

			boolean useNumeric = cmd.hasOption("numeric");

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Sentences");
			doc.appendChild(rootElement);

			int id = 0;

			Iterable<KAFDocument> corpus = Corpus.create(false, inputFolder);

			if (list != null) {
				LOGGER.info("Load file list from {}", list.getAbsolutePath());
				ArrayList<KAFDocument> files = new ArrayList<>();
				List<String> fileList = Files.readAllLines(list.toPath());
				for (String fileName : fileList) {
					fileName = fileName.trim();
					if (fileName.length() == 0) {
						continue;
					}
					String documentFileName = inputFolder + File.separator + fileName;
					files.add(KAFDocument.createFromFile(new File(documentFileName)));
				}
				corpus = files;
			}

			int fileNum = 0;
			for (KAFDocument document : corpus) {
				fileNum++;
				LOGGER.info("File {}", document.getPublic().uri);
				Element sentenceElement = doc.createElement("sentence");

				if (useNumeric) {
					sentenceElement.setAttribute("id", "" + id++);
				}
				else {
					URIImpl uri = new URIImpl(document.getPublic().uri);
					sentenceElement.setAttribute("id", uri.getLocalName());
				}

				rootElement.appendChild(sentenceElement);
				Element textElement = doc.createElement("text");
				textElement.appendChild(doc.createTextNode(document.getRawText()));
				sentenceElement.appendChild(textElement);

				for (Opinion opinion : document.getOpinions()) {

				    boolean matches = false;
				    for (String l : labels) {
				        if (opinion.getLabel().contains(l)) {
				            matches = true;
				            break;
				        }
				    }
				    if (!matches) {
				        continue;
				    }
				    
					String expression = null;
					if (opinion.getOpinionExpression() == null) {
						continue;
					}

					HashMap<String, Integer> indexes = new HashMap<>();
					indexes.put("holder-start", -1);
					indexes.put("holder-end", -1);
					indexes.put("target-start", -1);
					indexes.put("target-end", -1);

					expression = opinion.getExpressionSpan().getStr();
					indexes.put("expression-start", opinion.getExpressionSpan().getTargets().get(0).getOffset());
					indexes.put("expression-end", opinion.getExpressionSpan().getTargets().get(opinion.getExpressionSpan().getTargets().size() - 1).getOffset() +
							opinion.getExpressionSpan().getTargets().get(opinion.getExpressionSpan().getTargets().size() - 1).getLength());

					String holder = null;
					if (opinion.getOpinionHolder() != null && !opinion.getOpinionHolder().getTerms().isEmpty()) {
						holder = opinion.getHolderSpan().getStr();
						indexes.put("holder-start", opinion.getHolderSpan().getTargets().get(0).getOffset());
						indexes.put("holder-end", opinion.getHolderSpan().getTargets().get(opinion.getHolderSpan().getTargets().size() - 1).getOffset() +
								opinion.getHolderSpan().getTargets().get(opinion.getHolderSpan().getTargets().size() - 1).getLength());
					}
					else {
						holder = "null";
					}

					String target = null;
					if (opinion.getOpinionTarget() != null && !opinion.getOpinionTarget().getTerms().isEmpty()) {
						target = opinion.getTargetSpan().getStr();
						indexes.put("target-start", opinion.getTargetSpan().getTargets().get(0).getOffset());
						indexes.put("target-end", opinion.getTargetSpan().getTargets().get(opinion.getTargetSpan().getTargets().size() - 1).getOffset() +
								opinion.getTargetSpan().getTargets().get(opinion.getTargetSpan().getTargets().size() - 1).getLength());
					}
					else {
						target = "null";
					}

					Element frameElement = doc.createElement("frame");

					Element holderElement = doc.createElement("holder");
					holderElement.setAttribute("value", holder);
					holderElement.setAttribute("start", Integer.toString(indexes.get("holder-start")));
					holderElement.setAttribute("end", Integer.toString(indexes.get("holder-end")));

					Element topicElement = doc.createElement("topic");
					topicElement.setAttribute("value", target);
					topicElement.setAttribute("start", Integer.toString(indexes.get("target-start")));
					topicElement.setAttribute("end", Integer.toString(indexes.get("target-end")));

					Element opinionElement = doc.createElement("opinion");
					opinionElement.setAttribute("value", expression);
					opinionElement.setAttribute("start", Integer.toString(indexes.get("expression-start")));
					opinionElement.setAttribute("end", Integer.toString(indexes.get("expression-end")));
					Element polarityElement = doc.createElement("polarity");
					polarityElement.appendChild(doc.createTextNode(opinion.getPolarity() != null ? normalizePolarity(opinion.getPolarity()) : "neutral"));
					opinionElement.appendChild(polarityElement);

					frameElement.appendChild(holderElement);
					frameElement.appendChild(topicElement);
					frameElement.appendChild(opinionElement);
					sentenceElement.appendChild(frameElement);
				}
			}

			LOGGER.info("Read {} files", fileNum);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			DOMSource source = new DOMSource(doc);

			StreamResult result = new StreamResult(outputFile);
//			StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
	
	private static String normalizePolarity( String polarity) {
        String p = polarity.toLowerCase();
        if (p.contains("pos")) {
            return "positive";
        } else if (p.contains("neg")) {
            return "negative";
        } else {
            return "neutral";
        }
    }
	
}
