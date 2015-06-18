package ixa.kaflib;

/**
 * Topic related to the text.
 */
public class SimpleTopic {

	/**
	 * Topic's properties
	 */
	private float probability;
	private String label;

	SimpleTopic() {
	}

	public SimpleTopic(float probability, String label) {
		this.probability = probability;
		this.label = label;
	}

	public float getProbability() {
		return probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
