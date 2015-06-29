package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.tintop.util.PikesProperties;

/**
 * Created by alessio on 27/05/15.
 */

public class Default {

	static PikesProperties properties = new PikesProperties();

	// s.anna_pos.model
	// anna_parser.model
	// mate_srl.model
	// mate_srl.model_be

	// mate_srl.enabled

	static {
		// Stanford
		properties.put("s.tokenize.whitespace", "0");
		properties.put("s.ssplit.eolonly", "false");
		properties.put("s.ssplit.newlineIsSentenceBreak", "always");
		properties.put("s.parse.maxlen", "100");
		properties.put("s.annotators", "tokenize");
		properties.put("s.dcoref.maxdist", "-1");

		properties.put("dbps.enabled", "1");
		properties.put("dbps.address", "http://spotlight.sztaki.hu:2222/rest/annotate");
		properties.put("dbps.timeout", "2000");
		properties.put("dbps.min_confidence", "0");
		properties.put("dbps.nbest", "1");


		// Custom annotators
		properties.put("s.customAnnotatorClass.anna_pos", "org.fbk.dkm.nlp.pipeline.annotators.AnnaPosAnnotator");
		properties.put("s.customAnnotatorClass.tt_pos", "org.fbk.dkm.nlp.pipeline.annotators.TreeTaggerPosAnnotator");

	}
}
