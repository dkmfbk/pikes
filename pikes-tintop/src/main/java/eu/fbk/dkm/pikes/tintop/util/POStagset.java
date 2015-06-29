package eu.fbk.dkm.pikes.tintop.util;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 11:11
 * To change this template use File | Settings | File Templates.
 */

public class POStagset {

	/*
	  N	common noun
	  R	proper noun
	  G	adjective
	  V	verb
	  P	preposition
	  A	adverb
	  C	conjunction
	  D	determiner
	  O	other

	 */
	public static final HashMap<String, String> tagset = new HashMap<>();
	static {
//		HashMap<String, String> tagset = new HashMap<>();
		tagset.put("CC", "C");
		tagset.put("CD", "O");
		tagset.put("DT", "D");
		tagset.put("EX", "O");
		tagset.put("FW", "O");
		tagset.put("IN", "P");
		tagset.put("JJ", "G");
		tagset.put("JJR", "G");
		tagset.put("JJS", "G");
		tagset.put("LS", "O");
		tagset.put("MD", "O");
		tagset.put("NN", "N");
		tagset.put("NNS", "N");
		tagset.put("NNP", "R");
		tagset.put("NNPS", "R");
		tagset.put("PDT", "D");
		tagset.put("POS", "O");
		tagset.put("PRP", "Q");
		tagset.put("PRP$", "Q");
		tagset.put("RB", "A");
		tagset.put("RBR", "A");
		tagset.put("RBS", "A");
		tagset.put("RP", "O");
		tagset.put("SYM", "O");
		tagset.put("TO", "P");
		tagset.put("UH", "O");
		tagset.put("VB", "V");
		tagset.put("VBD", "V");
		tagset.put("VBG", "V");
		tagset.put("VBN", "V");
		tagset.put("VBP", "V");
		tagset.put("VBZ", "V");
		tagset.put("WDT", "O");
		tagset.put("WP", "Q");
		tagset.put("WP$", "Q");
		tagset.put("WRB", "O");
	}
}
