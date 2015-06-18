package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/** Chunks are noun, verb or prepositional phrases, spanning terms. */
public class Chunk implements Serializable {

    /** Chunk's ID (required) */
    private String cid;

    /** Type of the phrase (optional) */
    private String phrase;

    /** Declension case (optional) */
    private String chunkcase;

    /** Chunk's target terms (at least one required) */
    private Span<Term> span;

    Chunk(String cid, Span<Term> span) {
	if (span.size() < 1) {
	    throw new IllegalStateException("Chunks must contain at least one term target");
	}
	this.cid = cid;
	this.span = span;
    }

    Chunk(Chunk chunk, HashMap<String, Term> terms) {
	this.cid = chunk.cid;
	this.phrase = chunk.phrase;
	this.chunkcase = chunk.chunkcase;
	/* Copy span */
	String id = chunk.getId();
	Span<Term> span = chunk.span;
	List<Term> targets = span.getTargets();
	List<Term> copiedTargets = new ArrayList<Term>();
	for (Term term : targets) {
	    Term copiedTerm = terms.get(term.getId());
	    if (copiedTerm == null) {
		throw new IllegalStateException("Term not found when copying " + id);
	    }
	    copiedTargets.add(copiedTerm);
	}
	if (span.hasHead()) {
	    Term copiedHead = terms.get(span.getHead().getId());
	    this.span = new Span<Term>(copiedTargets, copiedHead);
	}
	else {
	    this.span = new Span<Term>(copiedTargets);
	}
    }

    public String getId() {
	return cid;
    }

    void setId(String id) {
	this.cid = id;
    }

    public boolean hasHead() {
	return (span.getHead() != null);
    }

    public Term getHead() {
	return span.getHead();
    }

    public boolean hasPhrase() {
	return phrase != null;
    }

    public String getPhrase() {
	return phrase;
    }

    public void setPhrase(String phrase) {
	this.phrase = phrase;
    }

    public boolean hasCase() {
	return chunkcase != null;
    }

    public String getCase() {
	return chunkcase;
    }

    public void setCase(String chunkcase) {
	this.chunkcase = chunkcase;
    }

    public List<Term> getTerms() {
	return this.span.getTargets();
    }

    public void addTerm(Term term) {
	this.span.addTarget(term);
    }

    public void addTerm(Term term, boolean isHead) {
	this.span.addTarget(term, isHead);
    }

    public Span<Term> getSpan() {
	return this.span;
    }

    public void setSpan(Span<Term> span) {
	this.span = span;
    }

    public String getStr() {
	String str = "";
	for (Term term : this.span.getTargets()) {
	    if (!str.isEmpty()) {
		str += " ";
	    }
	    str += term.getStr();
	}
	return str;
    }

    /** Deprecated */
    public void setHead(Term term) {
        this.span.setHead(term);
    }
		
}
