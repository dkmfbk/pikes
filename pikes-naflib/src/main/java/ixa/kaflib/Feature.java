package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/** Class for representing features. There are two types of features: properties and categories. */
public class Feature implements Relational, Serializable {

    /* Feature's ID (required) */
    private String id;

    /* Lemma (required) */
    private String lemma;

    private List<Span<Term>> references;

    private List<ExternalRef> externalReferences;

    Feature(String id, String lemma, List<Span<Term>> references) {
	if (references.size() < 1) {
	    throw new IllegalStateException("Features must contain at least one reference span");
	}
	if (references.get(0).size() < 1) {
	    throw new IllegalStateException("Features' reference's spans must contain at least one target");
	}
	this.id = id;
	this.lemma = lemma;
	this.references = references;
	this.externalReferences = new ArrayList<ExternalRef>();
    }

    Feature(Feature feature, HashMap<String, Term> terms) {
	this.id = feature.id;
	this.lemma = feature.lemma;
	/* Copy references */
	String id = feature.getId();
	this.references = new ArrayList<Span<Term>>();
	for (Span<Term> span : feature.getSpans()) {
	    /* Copy span */
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
		this.references.add(new Span<Term>(copiedTargets, copiedHead));
	    }
	    else {
		this.references.add(new Span<Term>(copiedTargets));
	    }
	}
	/* Copy external references */
	this.externalReferences = new ArrayList<ExternalRef>();
	for (ExternalRef externalRef : feature.getExternalRefs()) {
	    this.externalReferences.add(new ExternalRef(externalRef));
	}
    }

    public boolean isAProperty() {
	return this.id.matches("p.*");
    }

    public boolean isACategory() {
	return this.id.matches("c.*");
    }

    public String getId() {
	return this.id;
    }

    void setId(String id) {
	this.id = id;
    }

    public String getLemma() {
	return this.lemma;
    }

    public void setLemma(String lemma) {
	this.lemma = lemma;
    }

    /** Returns the term targets of the first span. When targets of other spans are needed getReferences() method should be used. */ 
    public List<Term> getTerms() {
	return this.references.get(0).getTargets();
    }

    /** Adds a term to the first span. */
    public void addTerm(Term term) {
	this.references.get(0).addTarget(term);
    }

    /** Adds a term to the first span. */
    public void addTerm(Term term, boolean isHead) {
	this.references.get(0).addTarget(term, isHead);
    }

    public List<Span<Term>> getSpans() {
	return this.references;
    }

    public void addSpan(Span<Term> span) {
	references.add(span);
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

    public String getSpanStr(Span<Term> span) {
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
	return getSpanStr(this.getSpans().get(0));
    }


    /** Deprecated */
    public List<List<Term>> getReferences() {
	List<List<Term>> list = new ArrayList<List<Term>>();
	for (Span<Term> span : this.references) {
	    list.add(span.getTargets());
	}
	return list;
    }

    /** Deprecated */
    public void addReference(List<Term> span) {
	this.references.add(KAFDocument.<Term>list2Span(span));
    }
}
