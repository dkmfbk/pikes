package ixa.kaflib;

import java.io.Serializable;
import java.util.*;

public class Span<T> implements Serializable {
	@Override
	public String toString() {
		return "Span{" +
				"targets=" + targets +
				'}';
	}

	//private List<String> targets;
	private List<T> targets;
	private Set<T> heads;

	Span() {
		this.targets = new ArrayList<T>();
		this.heads = new HashSet<>();
	}

	Span(List<T> targets) {
		this.targets = targets;
		this.heads = new HashSet<>();
	}

	Span(List<T> targets, T head) {
		this.targets = targets;
		this.heads = new HashSet<>();
		heads.add(head);
	}

	public boolean isEmpty() {
		return (this.targets.size() <= 0);
	}

	public String getStr() {
		StringBuilder builder = new StringBuilder();
		for (Object term : targets) {
			if (builder.length() != 0) {
				builder.append(' ');
			}
			if (term instanceof Term) {
				builder.append(((Term) term).getStr());
			}
			else if (term instanceof WF) {
				builder.append(((WF) term).getForm());
			}
			else {
				builder.append(term.toString());
			}
		}
		return builder.toString();
	}

	public List<T> getTargets() {
		return this.targets;
	}

	public T getFirstTarget() {
		return this.targets.get(0);
	}

	public boolean hasHead() {
		return (this.heads.size() > 0);
	}

	@Deprecated
	public T getHead() {
		for (T term : this.heads) {
			return term;
		}

		return null;
	}

	public Set<T> getHeads() {
		return heads;
	}

	public boolean isHead(T target) {
		return (this.heads.contains(target));
	}

	public void setHead(T head) {
		this.heads.add(head);
	}

	public void deleteHead(T head) {
		this.heads.remove(head);
	}

	public void clearHeads() {
		this.heads = new HashSet<>();
	}

	public void addTarget(T target) {
		this.targets.add(target);
	}

	public void addTarget(T target, boolean isHead) {
		this.targets.add(target);
		if (isHead) {
			this.heads.add(target);
		}
	}

	public void addTargets(List<T> targets) {
		this.targets.addAll(targets);
	}

	public boolean hasTarget(T target) {
		for (T t : targets) {
			if (t == target) {
				return true;
			}
		}
		return false;
	}

	public int size() {
		return this.targets.size();
	}
	

	@Override
	public boolean equals(Object object) {
	    if (object == this) {
	        return true;
	    }
	    if (!(object instanceof Span<?>)) {
	        return false;
	    }
	    Span<?> other = (Span<?>) object;
	    return other.targets.equals(targets) && other.heads.equals(heads);
	}

    @Override
    public int hashCode() {
        return Objects.hash(targets, heads);
    }

}
