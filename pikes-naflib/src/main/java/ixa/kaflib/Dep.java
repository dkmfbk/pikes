package ixa.kaflib;

import java.io.Serializable;
import java.util.*;


/** Dependencies represent dependency relations among terms. */
public class Dep implements Serializable {

    /** Source term of the dependency (required) */
    private Term from;

    /** Target term of the dependency (required) */
    private Term to;

    /** Relational function of the dependency (required) */
    private String rfunc;

    /** Declension case (optional) */
    private String depcase;

    Dep(Term from, Term to, String rfunc) {
	this.from = from;
	this.to = to;
	this.rfunc = rfunc;
    }

    Dep(Dep dep, HashMap<String, Term> terms) {
	this.from = terms.get(dep.from.getId());
	if (this.from == null) {
	    throw new IllegalStateException("Couldn't find the term when loading dep (" + dep.getFrom().getId()+", "+dep.getTo().getId()+")");
	}
	this.to = terms.get(dep.to.getId());
	if (this.to == null) {
	    throw new IllegalStateException("Couldn't find the term when loading dep (" + dep.getFrom().getId()+", "+dep.getTo().getId()+")");
	}
	this.rfunc = dep.rfunc;
	this.depcase = dep.depcase;
    }

    public Term getFrom() {
	return this.from;
    }

    public void setFrom(Term term) {
	this.from = term;
    }

    public Term getTo() {
	return to;
    }

    public void setTo(Term term) {
	this.to = term;
    }

    public String getRfunc() {
	return rfunc;
    }

     public void setRfunc(String rfunc) {
	 this.rfunc = rfunc;
    }

    public boolean hasCase() {
	return depcase != null;
    }

    public String getCase() {
	return depcase;
    }

    public void setCase(String depcase) {
	this.depcase = depcase;
    }

    public String getStr() {
        String idFrom = this.getFrom().getId().replaceAll("[^0-9]", "");
        String idTo = this.getTo().getId().replaceAll("[^0-9]", "");
        return String.format("%s(%s-%s, %s-%s)", rfunc, this.getFrom().getStr(), idFrom, this.getTo().getStr(), idTo);
    }

    @Override
    public String toString() {
        return getStr();
    }

    public static final class Path {

        private final List<Dep> deps;

        private final List<Term> terms;

        private String label;

        private Path(final List<Dep> deps, final List<Term> terms) {
            this.deps = deps;
            this.terms = terms;
        }

        public static Path create(final Term from, final Term to, final Iterable<Dep> deps) {

            Term term = from;
            List<Dep> depList = new ArrayList<>();
            final List<Term> termList = new ArrayList<>();
            termList.add(term);
            for (final Dep dep : deps) {
                depList.add(dep);
                term = dep.getTo() == term ? dep.getFrom() : dep.getTo();
                if (!termList.add(term)) {
                    throw new IllegalArgumentException("Path contains loop");
                }
            }
            if (!term.equals(to)) {
                throw new IllegalArgumentException("Invalid path");
            }

            return new Path(depList, termList);
        }

        public static Path create(final Term from, final Term to, final KAFDocument document) {

            // Handle empty path
            if (from == to) {
                return create(from, to, Collections.<Dep>emptyList());
            }

            // Determine path from dep root to TO node. End if the FROM node is found here
            final List<Dep> toPath = new ArrayList<Dep>();
            for (Dep dep = document.getDepToTerm(to); dep != null; dep = document.getDepToTerm(dep
                    .getFrom())) {
                toPath.add(dep);
                if (dep.getFrom() == from) {
                    Collections.reverse(toPath);
                    return create(from, to, toPath);
                }
            }

            // Determine path from dep root to FROM node. End if node in toPath is found here
            final List<Dep> fromPath = new ArrayList<Dep>();
            for (Dep dep = document.getDepToTerm(from); dep != null; dep = document
                    .getDepToTerm(dep.getFrom())) {
                fromPath.add(dep);
                if (dep.getFrom() == to) {
                    return create(from, to, fromPath);
                }
                for (int i = 0; i < toPath.size(); ++i) {
                    if (dep.getFrom() == toPath.get(i).getFrom()) {
                        for (int j = i; j >= 0; --j) {
                            fromPath.add(toPath.get(j));
                        }
                        return create(from, to, fromPath);
                    }
                }
            }

            // No connection
            return null;
        }

        public Term getFrom() {
            return this.terms.get(0);
        }

        public Term getTo() {
            return this.terms.get(this.terms.size() - 1);
        }

        public List<Dep> getDeps() {
            return this.deps;
        }

        public List<Term> getTerms() {
            return this.terms;
        }

        public String getLabel() {
            if (this.label == null) {
                final StringBuilder builder = new StringBuilder();
                Term term = this.terms.get(0);
                for (final Dep dep : this.deps) {
                    builder.append(dep.getRfunc().toLowerCase());
                    if (term.equals(dep.getFrom())) {
                        builder.append('D');
                        term = dep.getTo();
                    } else {
                        builder.append('U');
                        term = dep.getFrom();
                    }
                }
                this.label = builder.toString();
            }
            return this.label;
        }

        public int length() {
            return this.deps.size();
        }

        public Path concat(final Path path) {
            if (!path.getFrom().equals(getTo())) {
                throw new IllegalArgumentException();
            }
            if (this.deps.isEmpty()) {
                return path;
            } else if (path.deps.isEmpty()) {
                return this;
            } else {
                List<Dep> deps = new ArrayList<Dep>();
                deps.addAll(this.deps);
                deps.addAll(path.deps);
                return create(this.terms.get(0), path.terms.get(this.terms.size() - 1), deps);
            }
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof Path)) {
                return false;
            }
            final Path other = (Path) object;
            return this.deps.equals(other.deps);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.deps);
        }

        @Override
        public String toString() {
            return getLabel();
        }

    }
    
}
