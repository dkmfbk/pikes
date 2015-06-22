package eu.fbk.dkm.pikes.tintop.annotators.models;

import org.apache.log4j.Logger;
import org.fbk.dkm.nlp.pipeline.annotators.raw.UKB;

import java.io.IOException;
import java.util.Map;

/**
 * Created by alessio on 27/05/15.
 */

public class UKBModel {

	private static UKBModel instance;
	private UKB tagger;
	static Logger logger = Logger.getLogger(UKBModel.class.getName());

	private UKBModel(Map properties) throws IOException {
		tagger = new UKB(properties);
	}

	public static UKBModel getInstance(Map properties) throws IOException {
		if (instance == null) {
			logger.info("Loading model for UKB");
			instance = new UKBModel(properties);
		}

		return instance;
	}

	public UKB getTagger() {
		return tagger;
	}

}
