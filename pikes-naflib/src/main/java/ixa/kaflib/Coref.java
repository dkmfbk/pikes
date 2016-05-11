package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** The coreference layer creates clusters of term spans (which we call mentions) which share the same referent. For instance, “London” and “the capital city of England” are two mentions referring to the same entity. It is said that those mentions corefer. */
public class Coref implements Serializable {

    /** Coreference's ID (required) */
    private String coid;

    /** (optional) */
    private String type;

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	private String cluster;

    /** Mentions to the same entity (at least one required) */
    private List<Span<Term>> mentions;

    /** External references (optional) */
    private List<ExternalRef> externalReferences;


    Coref(String coid, List<Span<Term>> mentions) {
	if (mentions.size() < 1) {
	    throw new IllegalStateException("Coreferences must contain at least one reference span");
	}
	if (mentions.get(0).size() < 1) {
	    throw new IllegalStateException("Coreferences' reference's spans must contain at least one target");
	}
	this.coid = coid;
	this.mentions = mentions;
	this.externalReferences = new ArrayList<ExternalRef>();
    }

    Coref(Coref coref, HashMap<String, Term> terms) {
	this.coid = coref.coid;
	/* Copy references */
	String id = coref.getId();
	this.mentions = new ArrayList<Span<Term>>();
	for (Span<Term> span : coref.getSpans()) {
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
		this.mentions.add(new Span<Term>(copiedTargets, copiedHead));
	    }
	    else {
		this.mentions.add(new Span<Term>(copiedTargets));
	    }
	}
    }

    public String getId() {
	return coid;
    }

    void setId(String id) {
	this.coid = id;
    }

    public boolean hasType() {
	return this.type != null;
    }

    public boolean hasCluster() {
	return this.cluster != null;
    }

    public String getType() {
	return this.type;
    }

    public void setType(String type) {
	this.type = type;
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

    /** Returns the term targets of the first span. When targets of other spans are needed getReferences() method should be used. */ 
    public List<Term> getTerms() {
	return this.mentions.get(0).getTargets();
    }

    /** Adds a term to the first span. */
    public void addTerm(Term term) {
	this.mentions.get(0).addTarget(term);
    }

    /** Adds a term to the first span. */
    public void addTerm(Term term, boolean isHead) {
	this.mentions.get(0).addTarget(term, isHead);
    }

    public List<Span<Term>> getSpans() {
	return this.mentions;
    }

    public void addSpan(Span<Term> span) {
	this.mentions.add(span);
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

    /** Deprecated */
    public List<List<Target>> getReferences() {
	List<List<Target>> list = new ArrayList<List<Target>>();
	for (Span<Term> span : this.mentions) {
	    list.add(KAFDocument.span2TargetList(span));
	}
	return list;
    }

    /** Deprecated */
    public void addReference(List<Target> span) {
	this.mentions.add(KAFDocument.targetList2Span(span));
    }
}
