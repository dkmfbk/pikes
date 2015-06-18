package eu.fbk.dkm.pikes.resources.boxer;

import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import org.openrdf.model.impl.URIImpl;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by alessio on 05/05/15.
 */

public class CorpusSplitter {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusSplitter.class);
	public static final Integer sentencesPerCluster = 50;
	private static final String NAMESPACE = "http://www.newsreader-project.eu/eu.fbk.dkm.pikes.resources.boxer/";

	private static void createDocument(ArrayList<String> list, File folder, Integer index) {

		StringBuffer buffer = new StringBuffer();
		for (String line:list) {
			buffer.append(line);
			buffer.append("\n");
		}

		String text = buffer.toString();
		String nafFileName = index + ".naf";
		File nafFile = new File(folder.getAbsolutePath() + File.separator + nafFileName);
		String documentURI = NAMESPACE + nafFileName;

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

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.darmstadt-loader")
					.withHeader("Load Boxer corpus and split it")
					.withOption("i", "input-file", "corpus file", "DIR", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output-folder", "output folder", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
//					.withOption("f", "force", "Force opinion")
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputFile = cmd.getOptionValue("i", File.class);
			final File outputFolder = cmd.getOptionValue("o", File.class);

			if (!outputFolder.exists()) {
				outputFolder.mkdirs();
			}

			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			ArrayList<String> list = new ArrayList<>();
			String line;

			int index = 0;

			while ((line = reader.readLine()) != null) {
				index++;
				line = line.trim();
				list.add(line);
				if (list.size() >= sentencesPerCluster) {
					createDocument(list, outputFolder, index);
					list = new ArrayList<>();
				}
			}
			if (list.size() > 0) {
				createDocument(list, outputFolder, index);
			}
			reader.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}

}
