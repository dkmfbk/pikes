package ixa.kaflib;

import org.eclipse.rdf4j.query.algebra.Str;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Linked Entity in the text.
 */
public class LinkedEntity {

	/**
	 * LinkedEntity's ID (required)
	 */
	private String linkedEntityId;

	/**
	 * LinedEntity's properties
	 */
	private String resource;
	private String reference;
	private double confidence;
	private Boolean spotted;

	private HashMap<String, HashSet<String>> types = new HashMap<>();

	/**
	 * Mentions to the same entity (at least one required)
	 */
	private Span<WF> mentions;

	LinkedEntity(String linkedEntityId) {
		this.linkedEntityId = linkedEntityId;
		this.mentions = new Span<WF>();
	}

	LinkedEntity(String linkedEntityId, Span<WF> mentions) {
		if (mentions.size() < 1) {
			throw new IllegalStateException("LinkedEntity must contain at least one reference span");
		}
		this.linkedEntityId = linkedEntityId;
		this.mentions = mentions;
	}

	LinkedEntity(LinkedEntity linkedEntity, HashMap<String, WF> WFs) {
		this.linkedEntityId = linkedEntity.linkedEntityId;
		this.resource = linkedEntity.resource;
		this.reference = linkedEntity.reference;
		this.confidence = linkedEntity.confidence;

		String id = linkedEntity.getId();
		this.mentions = linkedEntity.getWFs();
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public String getId() {
		return linkedEntityId;
	}

	void setId(String id) {
		this.linkedEntityId = id;
	}

	public String getSpanStr() {
		String str = "";
		for (WF wf : mentions.getTargets()) {
			if (!str.isEmpty()) {
				str += " ";
			}
			str += wf.getForm();
		}
		return str;
	}

	public void addType(String category, String type) {
		if (types.get(category) == null) {
			types.put(category, new HashSet<>());
		}
		types.get(category).add(type);
	}

	public HashMap<String, HashSet<String>> getTypes() {
		return types;
	}

	public void setTypes(HashMap<String, HashSet<String>> types) {
		this.types = types;
	}

	public Boolean isSpotted() {
		return spotted;
	}

	public void setSpotted(Boolean spotted) {
		this.spotted = spotted;
	}

	/**
	 * Returns the term targets of the first span. When targets of other spans are needed getReferences() method should be used.
	 */
	public Span<WF> getWFs() {
		return mentions;
//		if (this.mentions.size() > 0) {
//			return this.mentions.get(0).getTargets();
//		}
//		else {
//			return null;
//		}
	}

}
