package eu.fbk.dkm.pikes.tintop.annotators.models;

import org.apache.log4j.Logger;
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
	static Logger logger = Logger.getLogger(MateSrlBeModel.class.getName());

	private MateSrlBeModel(File mateModel) {
		logger.info("Loading model for Mate Tools (verb to be)");

		try {
			ZipFile zipFile;
			zipFile = new ZipFile(mateModel);
			labeler = Pipeline.fromZipFile(zipFile);
			zipFile.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
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
