package ixa.kaflib;

import java.io.Serializable;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Manages ID creation. Each ID is created taking into account the annotations of the same type created so far, in a document context. This class keeps a counter for each type of annotation (terms, chunks...).
 */
class IdManager implements Serializable {

	public GenericId wfs = new GenericId("w");
	public GenericId terms = new GenericId("t");
	public GenericId marks = new GenericId("m");
	public GenericId mws = new GenericId("t.mw");
	public GenericId chunks = new GenericId("c");
	public GenericId entities = new GenericId("e");
	public GenericId corefs = new GenericId("co");
	public GenericId timex3s = new GenericId("tmx");
	public GenericId linkedentities = new GenericId("le");
	public GenericId properties = new GenericId("p");
	public GenericId categories = new GenericId("cat");
	public GenericId opinions = new GenericId("o");
	public GenericId relations = new GenericId("r");
	public GenericId predicates = new GenericId("pr");
	public GenericId roles = new GenericId("rl");
	public GenericId terminals = new GenericId("ter");
	public GenericId nonterminals = new GenericId("nter");
	public GenericId edges = new GenericId("tre");
	public GenericId ssts = new GenericId("sst");
	public GenericId topics = new GenericId("top");
	public GenericId tlinks = new GenericId("tlink");
	public GenericId clinks = new GenericId("clink");

	private HashMap<String, Integer> componentCounter = new HashMap();
	boolean inconsistentIdComponent = false;
	private static final String COMPONENT_PREFIX = ".";

	String getNextComponentId(String termId) {
		String newId;
		int nextIndex;
		if (this.inconsistentIdComponent) {
			throw new IllegalStateException("Inconsistent component IDs. Can't create new component IDs.");
		}
		if (!componentCounter.containsKey(termId)) {
			nextIndex = 1;
		}
		else {
			nextIndex = componentCounter.get(termId) + 1;
		}
		newId = termId + COMPONENT_PREFIX + Integer.toString(nextIndex);
		componentCounter.put(termId, nextIndex);
		return newId;
	}


	void updateComponentCounter(String id, String termId) {
		int componentInd;
		Matcher matcher = Pattern.compile("^" + terms.getPrefix() + "_?\\d+\\" + COMPONENT_PREFIX + "(\\d+)$").matcher(id);
		if (!matcher.find()) {
			this.inconsistentIdComponent = true;
			return;
		}
		componentInd = Integer.valueOf(matcher.group(1));
		componentCounter.put(termId, componentInd);
	}

}
