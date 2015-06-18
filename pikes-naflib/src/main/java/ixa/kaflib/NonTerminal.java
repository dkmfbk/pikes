package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class NonTerminal extends TreeNode implements Serializable {

    /** Label */
    private String label;

    /** Nodes' children */
    private List<TreeNode> children;


    NonTerminal(String id, String label) {
	super(id, false, false);
	this.label = label;
	this.children = new ArrayList<TreeNode>();
    }

    public String getLabel() {
	return this.label;
    }

    public void setLabel(String label) {
	this.label = label;
    }

    public void addChild(TreeNode tn) throws Exception {
	this.children.add(tn);
    }

    public List<TreeNode> getChildren() {
	return this.children;
    }

}
