package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** An entity is a term (or a multiword) that clearly identifies one item. The optional Entity layer is used to reference terms that are entities. */
public class Entity implements Relational, Serializable {

    private static final long serialVersionUID = 1L;

    /** Entity's ID (required) */
    private String eid;

    /** Type of the entity (optional). Currently, 8 values are possible: 
     * - Person
     * - Organization
     * - Location
     * - Date
     * - Time
     * - Money
     * - Percent
     * - Misc
     */ 
    private String type;
    
    /** Whether the entity is a 'named entity' (i.e., proper noun). */
    private boolean named;

    /** Reference to different occurrences of the same entity in the document (at least one required) */
    private List<Span<Term>> references;

    /** External references (optional) */
    private List<ExternalRef> externalReferences;

    Entity(String eid, List<Span<Term>> references) {
	if (references.size() < 1) {
	    throw new IllegalStateException("Entities must contain at least one reference span");
	}
	if (references.get(0).size() < 1) {
	    throw new IllegalStateException("Entities' reference's spans must contain at least one target");
	}
	this.eid = eid;
	this.references = references;
	this.externalReferences = new ArrayList<ExternalRef>();
	this.named = true;
    }

    Entity(Entity entity, HashMap<String, Term> terms) {
	this.eid = entity.eid;
	this.type = entity.type;
	/* Copy references */
	String id = entity.getId();
	this.references = new ArrayList<Span<Term>>();
	for (Span<Term> span : entity.getSpans()) {
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
	for (ExternalRef externalRef : entity.getExternalRefs()) {
	    this.externalReferences.add(new ExternalRef(externalRef));
	}
	this.named = true;
    }

    public String getId() {
	return eid;
    }

    void setId(String id) {
	this.eid = id;
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

    public boolean isNamed() {
    return named;
    }

    public void setNamed(boolean named) {
    this.named = named;
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
	this.references.add(span);
    }

    public ExternalRef getExternalRef(String resource) {
    for (ExternalRef ref : externalReferences) {
        if (ref.getResource().equalsIgnoreCase(resource)) {
            return ref;
        }
    }
    return null;
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
