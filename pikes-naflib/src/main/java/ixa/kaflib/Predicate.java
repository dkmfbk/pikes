package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Predicate extends IReferable implements Serializable {

    public static class Role implements Serializable {

        private String rid;
        private String semRole;
        private Span<Term> span;
        private List<ExternalRef> externalReferences;
        private List<String> flags;

        Role(String id, String semRole, Span span) {
            this.rid = id;
            this.semRole = semRole;
            this.span = span;
            this.externalReferences = new ArrayList<ExternalRef>();
            this.flags = new ArrayList<String>();
        }

        @Override
        public String toString() {
            return "Role{" +
                    semRole + " -> " +
                    span.getStr() +
                    '}';
        }

        public String getId() {
            return this.rid;
        }

        public void setId(String id) {
            this.rid = id;
        }

        public String getSemRole() {
            return this.semRole;
        }

        public void setSemRole(String semRole) {
            this.semRole = semRole;
        }

        public Span<Term> getSpan() {
            return this.span;
        }

        public void setSpan(Span<Term> span) {
            this.span = span;
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

        public List<String> getFlags() {
            return this.flags;
        }

        public void addFlag(String flag) {
            if (!this.flags.contains(flag)) {
                this.flags.add(flag);
            }
        }

        public void removeFlag(String flag) {
            this.flags.remove(flag);
        }

    }

    private String id;
    private String uri;
    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean hasSource() {
        return this.source != null;
    }

    private double confidence;
    private Span<Term> span;
    private List<Role> roles;
    private List<ExternalRef> externalReferences;
    private List<String> flags;

    Predicate(String id, Span<Term> span) {
        this.id = id;
        this.span = span;
        this.roles = new ArrayList<Role>();
        this.confidence = -1.0f;
        this.externalReferences = new ArrayList<ExternalRef>();
        this.flags = new ArrayList<String>();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean hasUri() {
        return (this.uri != null);
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean hasConfidence() {
        return confidence != -1.0f;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Span<Term> getSpan() {
        return this.span;
    }

    public void setSpan(Span<Term> span) {
        this.span = span;
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

    public String getStr() {
        String str = "";
        if (!this.span.isEmpty()) {
            Term target = this.span.getFirstTarget();
            str += target.getId() + " " + target.getStr() + " ";
        }
        str += ":";
        for (Role role : this.roles) {
            if (!role.span.isEmpty()) {
                Term roleTarget = role.getSpan().getFirstTarget();
                str += " " + role.getSemRole() + "[" + roleTarget.getId() + " " + roleTarget.getStr() + "]";
            }
        }
        return str;
    }

    public String getSpanStr() {
        String str = "";
        for (Term term : this.span.getTargets()) {
            if (!str.isEmpty()) {
                str += " ";
            }
            str += term.getStr();
        }
        return str;
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

    public List<Role> getRoles() {
        return this.roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public List<String> getFlags() {
        return this.flags;
    }

    public void addFlag(String flag) {
        if (!this.flags.contains(flag)) {
            this.flags.add(flag);
        }
    }

    public void removeFlag(String flag) {
        this.flags.remove(flag);
    }

}
