package ixa.kaflib;

import java.io.Serializable;


public class CLink implements Serializable {

	private String id;

	private Predicate from;

	private Predicate to;

	private String relType;


	CLink(String id, Predicate from, Predicate to) {
		this.id = id;
		this.from = from;
		this.to = to;
	}

	public String getId() {
		return this.id;
	}

	public Predicate getFrom() {
		return this.from;
	}

	public void setFrom(Predicate from) {
		this.from = from;
	}

	public Predicate getTo() {
		return this.to;
	}

	public void setTo(Predicate to) {
		this.to = to;
	}

	public boolean hasRelType() {
		return this.relType != null;
	}

	public String getRelType() {
		return this.relType;
	}

	public void setRelType(String relType) {
		this.relType = relType;
	}
}