package eu.fbk.dkm.pikes.tintop.util;

import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 11:11
 * To change this template use File | Settings | File Templates.
 */

public class NER2SSTtagset {

	static Logger logger = Logger.getLogger(NER2SSTtagset.class.getName());

	public static final HashMap<String, String> tagset = new HashMap<>();
	static {
		tagset.put("PERSON", "PER");
		tagset.put("LOCATION", "LOC");
		tagset.put("ORGANIZATION", "ORG");
	}
}
