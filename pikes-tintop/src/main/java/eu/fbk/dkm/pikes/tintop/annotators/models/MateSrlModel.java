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

public class MateSrlModel {

	private static MateSrlModel instance;
	private SemanticRoleLabeler labeler;
	static Logger logger = Logger.getLogger(MateSrlModel.class.getName());

	private MateSrlModel(File mateModel) {
		logger.info("Loading model for Mate Tools");

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

	public static MateSrlModel getInstance(File posModel) {
		if (instance == null) {
			instance = new MateSrlModel(posModel);
		}

		return instance;
	}

	public SemanticRoleLabeler getLabeler() {
		return labeler;
	}
}
