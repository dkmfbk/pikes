package ixa.kaflib;

import java.io.Serializable;
import java.util.*;

/**  */
public class Tree implements Serializable { //?

    public static final String HEAD_MARK = "=H";

    /** Tree's root node */
    private TreeNode root;
	private Integer sentence = null;

    Tree(TreeNode root) {
	this.root = root;
    }

	Tree(TreeNode root, Integer sentence) {
		this.root = root;
		this.sentence = sentence;
	}

	public TreeNode getRoot() {
	return this.root;
    }

    public void setRoot(TreeNode root) {
	this.root = root;
    }


    /***********************************************************/
    /* Code for converting OpenNLP's parentheses output to NAF */
    /***********************************************************/

	static void parenthesesToKaf(String parOut, KAFDocument kaf) throws Exception {
		parenthesesToKaf(parOut, kaf, null);
	}

	public Integer getSentence() {
		return sentence;
	}

	static void parenthesesToKaf(String parOut, KAFDocument kaf, Integer sentence) throws Exception {
	String[] tokens = Tree.tokenize(parOut);
	Tree.check(tokens);
        HashMap<Integer, Integer> parMatching = Tree.matchParentheses(tokens);
	HashMap<Integer, Term> termMatching = Tree.matchTerms(tokens, kaf.getSentenceTerms(sentence));
	// behin-behineko irtenbidea errorea ekiditeko: hutsa itzuli
	if (termMatching.size() == 0) {
	    return;
	}
	List<Tree> trees = new ArrayList<Tree>();
	int current = 0;
	while (current < tokens.length) {
	    int end = parMatching.get(current);
	    NonTerminal root = Tree.createNonTerminal(tokens, current+1, end-1, parMatching, termMatching, kaf);
	    kaf.newConstituent(root, sentence);
	    current = end + 1;
	}
    }

    private static String[] tokenize(String parOut) {
	List<String> tokens = new ArrayList<String>();
	int current = 0;
	int length = parOut.length();
	String token = new String("");
	while (current < length) {
	    char nextChar = parOut.charAt(current++);
	    if (nextChar == '(') {
		if (!token.isEmpty()) {
		    tokens.add(token);
		}
		tokens.add(new String("("));
		token = new String("");
	    }
	    else if (nextChar == ')') {
		if (!token.isEmpty()) {
		    tokens.add(token);
		}
		tokens.add(new String(")"));
		token = new String("");
	    }
	    else if ((nextChar == ' ') || (nextChar == '\n')) {
		if (!token.isEmpty()) {
		    tokens.add(token);
		    token = new String();
		}
	    }
	    else {
		token += nextChar;
	    }
	}
	return tokens.toArray(new String[tokens.size()]);
    }

    private static HashMap<Integer, Integer>  matchParentheses(String[] tokens) {
	HashMap<Integer, Integer> indexes = new HashMap<Integer, Integer>();
	Stack<Integer> stack = new Stack<Integer>();
	int ind = 0;
	for (String token : tokens) {
	    if (token.equals("(")) {
		stack.push(ind);
	    }
	    else if (token.equals(")")) {
		indexes.put(stack.pop(), ind);
	    }
	    ind++;
	}
	return indexes;
    }

    private static HashMap<Integer, Term> matchTerms(String[] tokens, List<Term> terms) throws Exception {
	HashMap<Integer, Term> mapping = new HashMap<Integer, Term>();
	int nextTerm = 0;
	for (int i=1; i<tokens.length; i++) {
	    if ((!tokens[i].equals("(")) && (!tokens[i].equals(")"))) {
		if ((!tokens[i-1].equals("(")) && (!tokens[i-1].equals(")"))) {
		    String termForm = terms.get(nextTerm).getForm();
		    String previousTermForm = "";
		    if (nextTerm != 0) {
			previousTermForm = terms.get(nextTerm-1).getForm();
		    }

		    if (termForm.equals("(")) {
			termForm = new String("-LRB-");
		    }
		    else if (termForm.equals(")")) {
			termForm = new String("-RRB-");
		    }
		    else if (termForm.equals("{")) { 
			termForm = new String("-LCB-");
		    }
		    else if (termForm.equals("}")) { 
			termForm = new String("-RCB-");
		    }
		    else if (termForm.equals("[")) {
			    termForm = new String("-LSB-");
		    }
		    else if (termForm.equals("]")) {
			    termForm = new String("-RSB-");
		    }

		    
		    if (termForm.equals(tokens[i]) || termForm.contains(tokens[i])) {
			mapping.put(i, terms.get(nextTerm));			
			nextTerm++;
		    }
		    else if ((nextTerm > 0) && previousTermForm.contains(tokens[i])) {
			// The token is part of a multitoken
			mapping.put(i, terms.get(nextTerm-1));
			// Don't update nextTerm
		    }
		    else {
			boolean matched = false;
			nextTerm++;
			while (!matched && (nextTerm != terms.size())) {
			    if (terms.get(nextTerm).getForm().equals(tokens[i])) {
				mapping.put(i, terms.get(nextTerm));
				matched = true;
			    }
			    nextTerm++;
			}
			if (!matched) {
			    //throw new Exception("Can't perform parentheses=>NAF at constituency (tok_id: " + terms.get(nextTerm).getId()  + ", [" + termForm + "] != [" + tokens[i] + "])");
				throw new Exception("Can't perform parentheses=>NAF at constituency: form \"" + tokens[i] + "\" not found in the KAF document.");
			}
		    }
		}
	    }
	}
	return mapping;
    }

    private static NonTerminal createNonTerminal(String[] tokens, int start, int end, HashMap<Integer, Integer> parenthesesMap, HashMap<Integer, Term> termMap, KAFDocument kaf) {
	String tag = tokens[start];
	boolean isHead = isHead(tag);
	if (isHead) {
	    tag = removeHeadMark(tag);
	}
	NonTerminal nt = kaf.newNonTerminal(tag);
	if (isHead) {
	    nt.setHead(true);
	}
	if (end - start == 1) {
	    Terminal t = Tree.createTerminal(tokens[end], termMap.get(end), kaf);
	    try {
		nt.addChild(t);
	    } catch(Exception e) {}
	}
	else {
	    int current = start + 1;
	    while (current <= end) {
		int subParEnd = parenthesesMap.get(current);
		NonTerminal nnt = Tree.createNonTerminal(tokens, current+1, subParEnd-1, parenthesesMap, termMap, kaf);
		try {
		    nt.addChild(nnt);
		} catch(Exception e) {}
		current = subParEnd + 1;
	    }
	}
	return nt;
    }

    private static Terminal createTerminal(String token, Term term, KAFDocument kaf) {
	Span<Term> span = kaf.newTermSpan();
	span.addTarget(term);
	return kaf.newTerminal(span);
    }

    private static void check(String[] tokens) throws Exception {
	int opened = 0;
	for (int i=0; i<tokens.length; i++) {
	    if (tokens[i].equals("(")) {
		if ((i>0) && (tokens[i-1].equals("("))) {
		    throw Tree.getException(tokens, i);
		}
		else if (i == tokens.length-1) {
		    throw Tree.getException(tokens, i);
		}
		opened++;
	    }
	    else if (tokens[i].equals(")")) {
		if ((i<3) || tokens[i-1].equals("(")) {
		    throw Tree.getException(tokens, i);
		}
		opened--;
	    }
	    else { // string token
		if ((i==0) || (i == tokens.length-1)) {
		    throw Tree.getException(tokens, i);
		}
		else if (isAWord(tokens[i-1]) && isAWord(tokens[i+1])) {
		    throw Tree.getException(tokens, i);
		}
		else if (tokens[i-1].equals(")")) {
		    throw Tree.getException(tokens, i);
		}
		else if (tokens[i-1].equals("(") && tokens[i+1].equals(")")) {
		    throw Tree.getException(tokens, i);
		}
	    }
	}
	if (opened != 0) {
	    throw Tree.getException(tokens, tokens.length-1);
	}
    }

    private static boolean isAWord(String token) {
	return (!token.equals("(")) && (!token.equals(")"));
    }

    private static Exception getException(String[] tokens, int ind) {
	String str = new String("Parentheses format not valid: \"... ");
	for (int i=(ind<5 ? 0 : ind-5); i<(ind>tokens.length-6 ? tokens.length-1 : ind+5); i++) {
	    if (i == ind) {
		str += "->";
	    }
	    str += tokens[i];
	    if (i == ind) {
		str += "<-";
	    }
	    str += " ";
	}
	return new Exception(str + " ...\"");
    }

    private static boolean isHead(String tag) {
	return tag.endsWith(HEAD_MARK);
    }

    private static String removeHeadMark(String tag) {
	if (!isHead(tag)) {
	    return tag;
	}
	return tag.substring(0, tag.length() - HEAD_MARK.length());
    }
}
