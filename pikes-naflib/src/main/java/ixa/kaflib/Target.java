package ixa.kaflib;

import java.io.Serializable;


public class Target implements Serializable {
    private Term term;
    private boolean head;

    Target(Term term, boolean head) {
	this.term = term;
	this.head = head;
    }

    public Term getTerm() {
	return this.term;
    }

    public boolean isHead() {
	return head;
    }

    public void setTerm(Term term) {
	this.term = term;
	this.head = false;
    }

    public void setTerm(Term term, boolean head) {
	this.term = term;
	this.head = head;
    }

    public void setHead(boolean head) {
	this.head = head;
    }
}
