package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Mark implements Serializable {

    private String sid;

    private String type;

    private String lemma;

    /** Part of speech (optional). Possible values are:
     * - common noun (N)
     * - proper noun (R)
     * - adjective (G)
     * - verb (V)
     * - preposition (P)
     * - adverb (A)
     * - conjunction (C)
     * - determiner (D)
     * - other (O)
     **/
    private String pos;

    private String morphofeat;

    private String markcase;

    private Span<Term> span;

    private List<ExternalRef> externalReferences;


    Mark(String id, Span<Term> span) {
	/*
	if (span.size() < 1) {
	    throw new IllegalStateException("A Mark must have at least one WF");
	}
	*/
	this.sid = id;
	this.span = span;
	this.externalReferences = new ArrayList<ExternalRef>();
    }

    public String getId() {
	return sid;
    }

    void setId(String id) {
	this.sid = id;
    }

    public boolean hasType() {
	return type != null;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public boolean hasLemma() {
	return lemma != null;
    }

    public String getLemma() {
	return lemma;
    }

    public void setLemma(String lemma) {
	this.lemma = lemma;
    }

    public boolean hasPos() {
	return pos != null;
    }

    public String getPos() {
	return pos;
    }

    public void setPos(String pos) {
	this.pos = pos;
    }

    public boolean hasMorphofeat() {
	return morphofeat != null;
    }

    public String getMorphofeat() {
	return morphofeat;
    }

    public void setMorphofeat(String morphofeat) {
	this.morphofeat = morphofeat;
    }

    public boolean hasCase() {
	return markcase != null;
    }

    public String getCase() {
	return markcase;
    }

    public void setCase(String termcase) {
	this.markcase = markcase;
    }

    public String getStr() {
	String str = "";
	for (Term term : span.getTargets()) {
	    if (!str.isEmpty()) {
		str += " ";
	    }
	    str += term.getStr();
	}
	return str;
    }

    public Span<Term> getSpan() {
	return this.span;
    }

    public void setSpan(Span<Term> span) {
	this.span = span;
    }

    public List<ExternalRef> getExternalRefs() {
	return externalReferences;
    }

    public void addExternalRef(ExternalRef externalRef) {
	externalReferences.add(externalRef);
    }

    public void addExternalRefs(List<ExternalRef> externalRefs) {
	externalReferences.addAll(externalRefs);
    }
}
