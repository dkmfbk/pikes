package ixa.kaflib;

import java.io.Serializable;
import java.util.List;

public abstract class TreeNode implements Serializable {

    /** The ID of the node */
    private String id;

    /** The id of the edge between this node and its parent. */
    private String edgeId;

    /** Wether the edge between this node and its parent is the "head" or not. */
    private boolean head;

    private boolean isTerminal;


    public TreeNode(String id, boolean head, boolean isTerminal) {
	this.id = id;
	this.head = head;
	this.isTerminal = isTerminal;
    }

    public String getId() {
	return this.id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public boolean hasEdgeId() {
	return this.edgeId != null;
    }

    public String getEdgeId() {
	return this.edgeId;
    }

    public void setEdgeId(String edgeId) {
	this.edgeId = edgeId;
    }

    public boolean getHead() {
	return this.head;
    }

    public void setHead(boolean head) {
	this.head = head;
    }

    public boolean isTerminal() {
	return isTerminal;
    }

    public abstract void addChild(TreeNode tn) throws Exception;

    public abstract List<TreeNode> getChildren();

}
