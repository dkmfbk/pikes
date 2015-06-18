package ixa.kaflib;

/**
 * Linked Entity in the text.
 */
public class SSTspan {

	/**
	 * LinkedEntity's ID (required)
	 */
	private String sstID;

	/**
	 * LinedEntity's properties
	 */
	private String type;
	private String label;

	/**
	 * Mentions to the same entity (at least one required)
	 */
	private Span<Term> mentions;

	SSTspan(String linkedEntityId) {
		this.sstID= linkedEntityId;
		this.mentions = new Span<Term>();
	}

	SSTspan(String sstSpan, Span<Term> mentions) {
		if (mentions.size() < 1) {
			throw new IllegalStateException("SSTspan must contain at least one reference span");
		}
		this.sstID = sstSpan;
		this.mentions = mentions;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getId() {
		return sstID;
	}

	void setId(String id) {
		this.sstID = id;
	}

	public String getSpanStr() {
		String str = "";
		for (Term term : mentions.getTargets()) {
			if (!str.isEmpty()) {
				str += " ";
			}
			str += term.getForm();
		}
		return str;
	}


	/**
	 * Returns the term targets of the first span. When targets of other spans are needed getReferences() method should be used.
	 */
	public Span<Term> getTerms() {
		return mentions;
//		if (this.mentions.size() > 0) {
//			return this.mentions.get(0).getTargets();
//		}
//		else {
//			return null;
//		}
	}

}
