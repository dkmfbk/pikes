/**
 * 
 */
package eu.fbk.dkm.pikes.resources.reader;

public class DataTextNode extends DataNode {
	public String text;
	DataTextNode(String text) {
		this.text = text;
	}

	public String toString() {
		return "[TEXT: " + text + "]";
	}

}