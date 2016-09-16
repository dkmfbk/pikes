package eu.fbk.dkm.pikes.raid.sbrs;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableList;
import eu.fbk.dkm.pikes.naflib.Corpus;
import eu.fbk.dkm.pikes.raid.Component;
import eu.fbk.dkm.pikes.raid.Extractor;
import eu.fbk.dkm.pikes.raid.Trainer;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.utils.svm.Util;
import ixa.kaflib.KAFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Created by alessio on 20/08/15.
 */

public class CreateTraining {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTraining.class);

	public static void main(String[] args) throws IOException {

		String folder = "/Users/alessio/Documents/Resources/johansson-moschitti/NAF-parsed";
		String wordnetPath = "/Users/alessio/Documents/Resources/wn-3.0-dict/dict";

		// ---

		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("eu.fbk")).setLevel(Level.DEBUG);

		File folderFile = new File(folder);
		WordNet.setPath(wordnetPath);

		LOGGER.info("Starting parsing");

		final Component[] components = Component.forLetters("ht").toArray(new Component[0]);
		Set<String> labels = new HashSet<>();
		labels.add("gold-mpqa-subjective");

		final List<Path> inputPaths = new ArrayList<>();
		inputPaths.add(folderFile.toPath());

		final Properties properties = Util.parseProperties("joint=true holder.unique=true target.unique=true");
//		final Trainer<? extends Extractor> trainer = Trainer.create(properties, components);
		final Trainer<? extends Extractor> trainer = new SBRSTrainer(properties, components);

		final List<Path> files = Util.fileMatch(inputPaths, ImmutableList.of(".naf", ".naf.gz", ".naf.bz2", ".naf.xz", ".xml", ".xml.gz", ".xml.bz2", ".xml.xz"), false, false);
		Iterable<KAFDocument> documents = files != null ? Corpus.create(false, files)
				: ImmutableList.of(NAFUtils.readDocument(null));

		StreamSupport.stream(documents.spliterator(), false).forEach(
				(final KAFDocument document) -> {
					trainer.add(document, labels);
				});
//		final Extractor extractor = trainer.train();

	}

}
