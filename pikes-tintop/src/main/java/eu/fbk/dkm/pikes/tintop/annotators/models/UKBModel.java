package eu.fbk.dkm.pikes.tintop.annotators.models;

import eu.fbk.dkm.pikes.tintop.annotators.raw.UKB;
import eu.fbk.dkm.pikes.tintop.annotators.raw.UKB_MT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Created by alessio on 27/05/15.
 */

public class UKBModel {

	private static UKBModel instance;
	private UKB_MT tagger;
	private static final Logger LOGGER = LoggerFactory.getLogger(UKBModel.class);

	private UKBModel(Properties properties) throws IOException {
		tagger = new UKB_MT(properties);
	}

	public static UKBModel getInstance(Properties properties) throws IOException {
		if (instance == null) {
			LOGGER.info("Loading model for UKB");
			instance = new UKBModel(properties);
		}

		return instance;
	}

	public UKB_MT getTagger() {
		return tagger;
	}

}
