package eu.fbk.dkm.pikes.resources.vuaopinion;

import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by alessio on 09/04/15.
 */

public class CorpusPreprocessor {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusPreprocessor.class);

	static public KAFDocument text2naf(String text) {
		KAFDocument doc = new KAFDocument("en", "v3");
		doc.setRawText(text);

		String date = "";
		try {
			date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale("en")).format(new Date());
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		KAFDocument.Public p = doc.createPublic();
		p.uri = "http://www.example.com";
		p.publicId = "0";

		KAFDocument.FileDesc d = doc.createFileDesc();
		d.creationtime = date;
		d.author = "Unknown author";
		d.filename = "test.xml";
		d.title = "Unknown title";

		return doc;
	}

	public static void main(String[] args) {

		try {
			CommandLine cmd = null;
			cmd = CommandLine
					.parser()
					.withName("corpus-preprocessor")
					.withHeader(
							"Convert KAF to NAF")
					.withOption("i", "input-path", "the base EN path of the corpus", "DIR",
							CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputPath = cmd.getOptionValue("i", File.class);
			if (!inputPath.exists()) {
				throw new IOException(String.format("Folder %s does not exist", inputPath.getAbsolutePath()));
			}

			File kafPath = new File(inputPath.getAbsolutePath() + File.separator + "kaf");
			if (!kafPath.exists()) {
				throw new IOException(String.format("Folder %s does not exist", kafPath.getAbsolutePath()));
			}
			File nafPath = new File(inputPath.getAbsolutePath() + File.separator + "naf");
			if (nafPath.exists()) {
				throw new IOException(String.format("Folder %s exists", nafPath.getAbsolutePath()));
			}
			nafPath.mkdir();

			Iterator<File> fileIterator;
			fileIterator = FileUtils.iterateFiles(kafPath, new String[]{"kaf"}, false);

			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				String fileBaseName = FilenameUtils.removeExtension(file.getName());
				KAFDocument document = KAFDocument.createFromFile(file);

				StringBuffer buffer = new StringBuffer();
				List<WF> wFs = document.getWFs();
				for (WF wf : wFs) {
					buffer.append(wf.getForm());
					buffer.append(" ");
				}
				String text = buffer.toString().trim();

				KAFDocument doc = text2naf(text);
				File nafFile = new File(nafPath.getAbsolutePath() + File.separator + fileBaseName + ".naf");
				doc.save(nafFile.getAbsolutePath());
//				System.out.println(fileBaseName);
			}

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}
}
