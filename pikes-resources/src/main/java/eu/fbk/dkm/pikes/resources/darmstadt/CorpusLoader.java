package eu.fbk.dkm.pikes.resources.darmstadt;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.openrdf.model.impl.URIImpl;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by alessio on 10/04/15.
 */

public class CorpusLoader {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusLoader.class);
	public static final String DEFAULT_NAMESPACE = "https://www.ukp.tu-eu.fbk.dkm.pikes.resources.darmstadt.de/eu.fbk.dkm.pikes.resources.darmstadt-service-review-corpus/";
	public static final String[] MMAX_PATTERN = new String[]{"basedata", "markables"};
	public static final String[] MMAX_SUFFIXES = new String[]{"_words", "_OpinionExpression_level"};

	private static void getFilesRecursive(File pFile, HashSet<String> folders) {
		for (File file : pFile.listFiles()) {
			if (file.isDirectory()) {
				folders.add(file.getAbsolutePath());
				getFilesRecursive(file, folders);
			}
		}
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.darmstadt-loader")
					.withHeader("Load eu.fbk.dkm.pikes.resources.darmstadt-service-review-corpus")
					.withOption("i", "input-folder", "the folder of the corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("n", "namespace", String.format("the namespace for generating document URIs, default %s", DEFAULT_NAMESPACE), "NS", CommandLine.Type.STRING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputFile = cmd.getOptionValue("i", File.class);

			String namespace = DEFAULT_NAMESPACE;
			if (cmd.hasOption("n")) {
				namespace = cmd.getOptionValue("n", String.class);
			}

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setValidating(false);
			dbFactory.setNamespaceAware(true);
			dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
			dbFactory.setFeature("http://xml.org/sax/features/validation", false);
			dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

			HashSet<String> folders = new HashSet<>();
			getFilesRecursive(inputFile, folders);

			HashSet<String> okFolders = new HashSet<>();
			okLoop:
			for (String folder : folders) {
				for (String pattern : MMAX_PATTERN) {
					StringBuffer newFolder = new StringBuffer();
					newFolder.append(folder);
					newFolder.append(File.separator);
					newFolder.append(pattern);

					if (!folders.contains(newFolder.toString())) {
						continue okLoop;
					}
				}

				okFolders.add(folder);
			}

			for (String folder : okFolders) {
				LOGGER.info("Entering folder {}", folder);

				String baseDataDir = folder + File.separator + MMAX_PATTERN[0];
				File nafDir = new File(folder + File.separator + "naf");

				if (nafDir.exists()) {
					LOGGER.warn("{} dir exists", nafDir.getAbsolutePath());
				}
				else {
					nafDir.mkdir();
				}

				Iterator<File> fileIterator;
				fileIterator = FileUtils.iterateFiles(new File(baseDataDir), new String[]{"xml"}, false);
				while (fileIterator.hasNext()) {
					File file = fileIterator.next();
					StringBuffer stringBuffer = new StringBuffer();
					String fileBaseName = FilenameUtils.removeExtension(file.getName());
					fileBaseName = fileBaseName.replaceAll(MMAX_SUFFIXES[0], "");

					String fileContent = Files.toString(file, Charsets.UTF_8);

					// Fix
					fileContent = fileContent.replaceAll("&", "&amp;");

					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(new ByteArrayInputStream(fileContent.getBytes(Charsets.UTF_8)));
					NodeList nList = doc.getElementsByTagName("word");
					for (int temp = 0; temp < nList.getLength(); temp++) {
						Element nNode = (Element) nList.item(temp);
						stringBuffer.append(nNode.getTextContent().replaceAll("\\s+", ""));
						stringBuffer.append(" ");
					}

					String nafFileName = fileBaseName + ".naf";
					File nafFile = new File(nafDir.getAbsolutePath() + File.separator + nafFileName);
					String text = stringBuffer.toString().trim();
					String documentURI = namespace + nafFileName;

					final KAFDocument document = new KAFDocument("en", "v3");
					document.setRawText(text);
					document.createPublic();
					document.getPublic().publicId = new URIImpl(documentURI).getLocalName();
					document.getPublic().uri = documentURI;
					document.createFileDesc();
					document.getFileDesc().filename = nafFileName;
					document.getFileDesc().title = "-";
					document.save(nafFile.getAbsolutePath());
				}
			}

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
