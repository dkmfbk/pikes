package eu.fbk.dkm.pikes.tintop.annotators.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.pipeline.Pipeline;

import java.io.File;
import java.util.zip.ZipFile;

/**
 * Created by alessio on 25/05/15.
 */

public class MateSrlBeModel {

	private static MateSrlBeModel instance;
	private SemanticRoleLabeler labeler;
	private static final Logger LOGGER = LoggerFactory.getLogger(MateSrlBeModel.class);

	private MateSrlBeModel(File mateModel) {
		LOGGER.info("Loading model for Mate Tools (verb to be)");

		try {
			ZipFile zipFile;
			zipFile = new ZipFile(mateModel);
			labeler = Pipeline.fromZipFile(zipFile);
			zipFile.close();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		Language.setLanguage(Language.L.valueOf("eng"));
	}

	public static MateSrlBeModel getInstance(File posModel) {
		if (instance == null) {
			instance = new MateSrlBeModel(posModel);
		}

		return instance;
	}

	public SemanticRoleLabeler getLabeler() {
		return labeler;
	}
}
