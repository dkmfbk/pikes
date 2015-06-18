package ixa.kaflib;

import java.io.Serializable;
import java.util.List;

public class Terminal extends TreeNode implements Serializable {

    /** The term referenced by this terminal */
    private Span<Term> span;

    Terminal(String id, Span<Term> span) {
	super(id, false, true);
	this.span = span;
    }

    /** Returns the Span object */
    public Span<Term> getSpan() {
	return this.span;
    }

    private String getStrValue() {
	String str = "";
	for (Term term : span.getTargets()) {
	    if (!str.isEmpty()) {
		str += " ";
	    }
	    str += term.getStr();
	}
	return str;
    }

    public String getStr() {
	String strValue = this.getStrValue();
	if (strValue.startsWith("-") || strValue.endsWith("-")) {
	    return strValue.replace("-", "- ");
   	}
   	else if (strValue.contains("--")) { 
	    return strValue.replace("--", "-");
   	}
   	else {
	    return strValue;
   	}
    }

    public void addChild(TreeNode tn) throws Exception {
	throw new Exception("It is not possible to add child nodes to Terminal nodes.");
    }

    public List<TreeNode> getChildren() {
	return null;
    }

}
