/**
 *
 */
package eu.fbk.dkm.pikes.resources.reader;

import java.util.ArrayList;
import java.util.HashMap;


public class DataElementNode extends DataNode {
	public String name;
	public HashMap<String, String> attributes = new HashMap(0);
	public ArrayList<DataNode> children = new ArrayList(0);

	DataElementNode(String name) {
		this.name = name;
	}

	public String getText() {
		if (!name.equals("__ROOT__")) {
			throw new IllegalArgumentException("may only call on root node");
		}
		//if(children.size() != 1)
		//throw new IllegalArgumentException("must contain single text node: node = " + this);
		StringBuilder out = new StringBuilder();

		for (DataNode n : children) {
			if (!(n instanceof DataTextNode)) {
				throw new IllegalArgumentException("must contain only text nodes");
			}
			out.append(((DataTextNode) n).text);
		}
		return out.toString();
	}

	public String getTopAttribute(String key) {
		if (!name.equals("__ROOT__")) {
			throw new IllegalArgumentException("may only call on root node");
		}
		if (children.size() != 1) {
			throw new IllegalArgumentException("must contain single element node: node = " + this);
		}
		DataNode n = children.get(0);

		if (!(n instanceof DataElementNode)) {
			throw new IllegalArgumentException("must contain single element node");
		}
		return ((DataElementNode) n).attributes.get(key);
	}

	public String getTopName(String key) {
		if (!name.equals("__ROOT__")) {
			throw new IllegalArgumentException("may only call on root node");
		}
		if (children.size() != 1) {
			throw new IllegalArgumentException("must contain single element node: node = " + this);
		}
		DataNode n = children.get(0);

		if (!(n instanceof DataElementNode)) {
			throw new IllegalArgumentException("must contain single element node");
		}
		return ((DataElementNode) n).name;
	}

	@Override
	public String toString() {
		return "DataElementNode{" +
				"name='" + name + '\'' +
				", attributes=" + attributes +
				", children=" + children +
				'}';
	}
}