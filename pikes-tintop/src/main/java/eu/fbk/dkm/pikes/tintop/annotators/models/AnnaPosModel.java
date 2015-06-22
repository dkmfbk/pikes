package eu.fbk.dkm.pikes.tintop.annotators.models;

import is2.tag.Tagger;
import org.apache.log4j.Logger;
import se.lth.cs.srl.util.BohnetHelper;

import java.io.File;

/**
 * Created by alessio on 25/05/15.
 */

public class AnnaPosModel {

	private static AnnaPosModel instance;
	private Tagger tagger;
	static Logger logger = Logger.getLogger(AnnaPosModel.class.getName());

	private AnnaPosModel(File posModel) {
		logger.info("Loading model for Anna POS");
		tagger = BohnetHelper.getTagger(posModel);
	}

	public static AnnaPosModel getInstance(File posModel) {
		if (instance == null) {
			instance = new AnnaPosModel(posModel);
		}

		return instance;
	}

	public Tagger getTagger() {
		return tagger;
	}
}
