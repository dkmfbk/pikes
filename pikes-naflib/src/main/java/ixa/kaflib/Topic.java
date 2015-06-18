package ixa.kaflib;

/**
 * Topic related to the text.
 */
public class Topic extends SimpleTopic {

	/**
	 * Topic's ID (required)
	 */
	private String topicID;

	Topic(String topicID) {
		this.topicID = topicID;
	}

	public String getId() {
		return topicID;
	}

	void setId(String id) {
		this.topicID = id;
	}

}
