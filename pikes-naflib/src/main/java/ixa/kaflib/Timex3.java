package ixa.kaflib;

import java.io.Serializable;


/** The coreference layer creates clusters of term spans (which we call mentions) which share the same referent. For instance, “London” and “the capital city of England” are two mentions referring to the same entity. It is said that those mentions corefer. */
public class Timex3 implements TLinkReferable, Serializable {

    /** Timex3's ID (required) */
    private String timex3id;

    /** Timex3's type (required)*/
    private String type;

    private Timex3 beginPoint;

    private Timex3 endPoint;

    private String quant;

    private String freq;

    /** Timex3's functionInDocument */
    private String functionInDocument;

    private Boolean temporalFunction;

    /** Timex3's value */
    private String value;

    private String valueFromFunction;

    private String mod;

    private String anchorTimeId;

    private String comment;

    private Span<WF> span;

    /** Mentions to the same entity (at least one required) */
    //private List<Span<WF>> mentions;

    Timex3(String timex3id, String type){
	this.timex3id = timex3id;
	this.type = type;
	//this.mentions = new ArrayList<Span<WF>>();
    }

    /*
    Timex3(String timex3id, List<Span<WF>> mentions) {
	if (mentions.size() < 1) {
	    throw new IllegalStateException("Timex3 must contain at least one reference span");
	}
	if (mentions.get(0).size() < 1) {
	   throw new IllegalStateException("Timex3' reference's spans must contain at least one target");
	}
	this.timex3id = timex3id;
	this.mentions = mentions;
    }
    */

    /*
    Timex3(Timex3 timex3, HashMap<String, WF> WFs) {
	this.timex3id = timex3.timex3id;
	this.type = timex3.type;

	String id = timex3.getId();
	this.mentions = new ArrayList<Span<WF>>();
	for (Span<WF> span : timex3.getSpans()) {

	    List<WF> targets = span.getTargets();
	    List<WF> copiedTargets = new ArrayList<WF>();
	    for (WF wf : targets) {
		WF copiedWF = WFs.get(wf.getId());
		if (copiedWF == null) {
		    throw new IllegalStateException("Term not found when copying " + id);
		}
		copiedTargets.add(copiedWF);
	    }
	    if (span.hasHead()) {
		WF copiedHead = WFs.get(span.getHead().getId());
		this.mentions.add(new Span<WF>(copiedTargets, copiedHead));
	    }
	    else {
		this.mentions.add(new Span<WF>(copiedTargets));
	    }
	}
    }
    */

    public String getId() {
	return timex3id;
    }

    void setId(String id) {
	this.timex3id = id;
    }

    public String getType() {
	return type;
    }

    public void setType(String type){
	this.type = type;
    }

    public boolean hasBeginPoint() {
	return this.beginPoint != null;
    }

    public Timex3 getBeginPoint() {
	return this.beginPoint;
    }

    public void setBeginPoint(Timex3 beginPoint) {
	this.beginPoint = beginPoint;
    }

    public boolean hasEndPoint() {
	return this.endPoint != null;
    }

    public Timex3 getEndPoint() {
	return this.endPoint;
    }

    public void setEndPoint(Timex3 endPoint) {
	this.endPoint = endPoint;
    }

    public boolean hasFreq() {
	return this.freq != null;
    }

    public String getFreq() {
	return this.freq;
    }

    public void setFreq(String freq) {
	this.freq = freq;
    }

    public boolean hasQuant() {
	return this.quant != null;
    }

    public String getQuant() {
	return this.quant;
    }

    public void setQuant(String quant) {
	this.quant = quant;
    }

    public boolean hasFunctionInDocument() {
	return this.functionInDocument != null;
    }

    public String getFunctionInDocument() {
	return this.functionInDocument;
    }

    public void setFunctionInDocument(String functionInDocument) {
	this.functionInDocument = functionInDocument;
    }

    public boolean hasTemporalFunction() {
	return this.temporalFunction != null;
    }

    public Boolean getTemporalFunction() {
	return this.temporalFunction;
    }

    public void setTemporalFunction(Boolean temporalFunction) {
	this.temporalFunction = temporalFunction;
    }

    public boolean hasValue() {
	return this.value != null;
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value){
	this.value = value;
    }

    public boolean hasValueFromFunction() {
	return this.valueFromFunction != null;
    }

    public String getValueFromFunction() {
	return this.valueFromFunction;
    }

    public void setValueFromFunction(String valueFromFunction) {
	this.valueFromFunction = valueFromFunction;
    }

    public boolean hasMod() {
	return this.mod != null;
    }

    public String getMod() {
	return this.mod;
    }

    public void setMod(String mod) {
	this.mod = mod;
    }

    public boolean hasAnchorTimeId() {
	return this.anchorTimeId != null;
    }

    public String getAnchorTimeId() {
	return this.anchorTimeId;
    }

    public void setAnchorTimeId(String anchorTimeId) {
	this.anchorTimeId = anchorTimeId;
    }

    public boolean hasComment() {
	return this.comment != null;
    }

    public String getComment() {
	return this.comment;
    }

    public void setComment(String comment) {
	this.comment = comment;
    }

    public boolean hasSpan() {
	return this.span != null;
    }

    public Span<WF> getSpan() {
	return this.span;
    }

    public void setSpan(Span<WF> span) {
	this.span = span;
    }

    public String getSpanStr(Span<WF> span) {
	String str = "";
	for (WF wf : span.getTargets()) {
	    if (!str.isEmpty()) {
		str += " ";
	    }
	    str += wf.getForm();
	}
	return str;
    }

    /** Returns the term targets of the first span. When targets of other spans are needed getReferences() method should be used. */
    /*
    public List<WF> getWFs() {
	if (this.mentions.size()>0){
	    return this.mentions.get(0).getTargets();
	}
	else{
	    return null;
	}
    }
    */

    /** Adds a term to the first span. */
    /*
    public void addWF(WF wf) {
	this.mentions.get(0).addTarget(wf);
    }
    */

    /** Adds a term to the first span. */
    /*
    public void addWF(WF wf, boolean isHead) {
	this.mentions.get(0).addTarget(wf, isHead);
    }

    public List<Span<WF>> getSpans() {
	return this.mentions;
    }

    public void addSpan(Span<WF> span) {
	this.mentions.add(span);
    }

    public String getSpanStr(Span<WF> span) {
	String str = "";
	for (WF wf : span.getTargets()) {
	    if (!str.isEmpty()) {
		str += " ";
	    }
	    str += wf.getForm();
	}
	return str;
    }
    */

}
