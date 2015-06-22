package eu.fbk.dkm.pikes.tintop.annotators.models;

import is2fbk.parser.Options;
import is2fbk.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by alessio on 25/05/15.
 */

public class AnnaParseModel {

	private static AnnaParseModel instance;
	private Parser parser;
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnaParseModel.class);

	private AnnaParseModel(File posModel) {
		LOGGER.info("Loading model for Anna Parser");
		String[] arrayOfString = {"-model", posModel.getAbsolutePath()};
		Options localOptions = new Options(arrayOfString);
		parser = new Parser(localOptions);
	}

	public static AnnaParseModel getInstance(File posModel) {
		if (instance == null) {
			instance = new AnnaParseModel(posModel);
		}

		return instance;
	}

	public Parser getParser() {
		return parser;
	}
}
