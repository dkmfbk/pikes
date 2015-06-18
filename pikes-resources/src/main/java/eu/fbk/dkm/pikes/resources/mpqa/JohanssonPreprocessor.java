package eu.fbk.dkm.pikes.resources.mpqa;

import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import org.openrdf.model.impl.URIImpl;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by alessio on 15/05/15.
 */

public class JohanssonPreprocessor {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JohanssonPreprocessor.class);
	public static final String DEFAULT_NAMESPACE = "http://eu.fbk.dkm.pikes.resources.mpqa.cs.pitt.edu/corpora/mpqa_corpus/";

	private static class Span {
		private int start, end, id;
		private String value;

		public Span(int start, int end, int id, String value) {
			this.start = start;
			this.end = end;
			this.id = id;
			this.value = value;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public int getId() {
			return id;
		}

		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			return "Span{" +
					"start=" + start +
					", end=" + end +
					", id=" + id +
					", value='" + value + '\'' +
					'}';
		}
	}

	public static void main(final String[] args) throws IOException, XMLStreamException {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("corpus-preprocessor")
					.withHeader(
							"Produces NAF files starting from the MPQA v.2 corpus preprocessed by Johansson/Moschitti.")
					.withOption("i", "input-path", "the base path of the Johansson MPQA corpus", "DIR",
							CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("o", "output",
							"the output path where to save produced files",
							"DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("n", "namespace",
							String.format("the namespace for generating document URIs, default %s", DEFAULT_NAMESPACE),
							"NS", CommandLine.Type.STRING, true, false, false)
					.withOption("doc", "doc", "Check only one document", "URL", CommandLine.Type.STRING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputPath = cmd.getOptionValue("i", File.class);

			final File outputPath = cmd.getOptionValue("o", File.class);
			if (!outputPath.exists()) {
				outputPath.mkdirs();
			}

			String namespace = DEFAULT_NAMESPACE;
			if (cmd.hasOption("n")) {
				namespace = cmd.getOptionValue("n", String.class);
			}

			String checkOneDoc = cmd.getOptionValue("doc", String.class);

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			for (File f : Files.fileTreeTraverser().preOrderTraversal(inputPath)) {

				// Only consider tokens
				if (!f.getAbsolutePath().endsWith("tokens.xml")) {
					continue;
				}

				final String name = f.getName().replace('/', '_');
				final String documentURI = namespace + name;

				if (checkOneDoc != null && !checkOneDoc.equals(documentURI)) {
					continue;
				}

				Document doc = dBuilder.parse(f);

				NodeList nList = doc.getElementsByTagName("annotation");

				HashMap<String, LinkedHashMap<Integer, Span>> spans = new HashMap<>();

				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					if (nNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}

					Element eElement = (Element) nNode;

					String provides = eElement.getAttribute("provides");
					if (spans.get(provides) == null) {
						spans.put(provides, new LinkedHashMap<>());
					}

					NodeList eS = eElement.getElementsByTagName("e");
					for (int spanID = 0; spanID < eS.getLength(); spanID++) {
						Node span = eS.item(spanID);
						if (span.getNodeType() != Node.ELEMENT_NODE) {
							continue;
						}

						Element eSpan = (Element) span;

						Integer id = Integer.parseInt(eSpan.getAttribute("id"));
						Integer start = Integer.parseInt(eSpan.getAttribute("start").replaceAll("#", ""));
						Integer end = Integer.parseInt(eSpan.getAttribute("end").replaceAll("#", ""));
						String value = eSpan.getTextContent();

						Span s = new Span(start, end, id, value);
						spans.get(provides).put(s.id, s);
					}
				}

				StringBuffer buffer = new StringBuffer();

				Integer lastToken = 0;
				for (Span span : spans.get("SENTENCES").values()) {
					if (span.start != lastToken + 1) {
						LOGGER.warn("Missing sentence [{}/{}]", f.getName(), span.start);
						for (int i = lastToken + 1; i < span.start; i++) {
							String token = spans.get("TOKENS").get(i).getValue();
							token = token.replace(' ', '_');
							token = token.replace('<', '_');
							token = token.replace('>', '_');
							buffer.append(token).append(" ");
						}
						buffer.append("\n");
					}
					for (int i = span.start; i <= span.end; i++) {
						String token = spans.get("TOKENS").get(i).getValue();
						token = token.replace(' ', '_');
						token = token.replace('<', '_');
						token = token.replace('>', '_');
						buffer.append(token).append(" ");
					}
					lastToken = span.end;
					buffer.append("\n");
				}

				String text = buffer.toString();

				File nafFile = new File(outputPath.getAbsolutePath() + File.separator + name);

				final KAFDocument document = new KAFDocument("en", "v3");

				document.setRawText(text);

				document.createPublic();
				document.getPublic().publicId = new URIImpl(documentURI).getLocalName();
				document.getPublic().uri = documentURI;

				document.createFileDesc();
//				document.getFileDesc().author = source + " / " + description;
//				document.getFileDesc().creationtime = createTime;
//				document.getFileDesc().filename = mediaFile;
//				document.getFileDesc().filetype = mediaType;
//				document.getFileDesc().title = title + " (" + topic + " / " + country + ")";

				document.save(nafFile.getAbsolutePath());
//				System.out.println(text);
			}


		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}

}
