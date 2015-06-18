package ixa.kaflib;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Class for representing opinions.
 */
public class Opinion implements Serializable {

    private static final long serialVersionUID = -6971847529645535297L;

    public enum Polarity {

        NEUTRAL, POSITIVE, NEGATIVE;

        public static Polarity forOpinion(final Opinion opinion) {
            return forLabel(opinion == null || opinion.getOpinionExpression() == null ? null
                    : opinion.getOpinionExpression().getPolarity());
        }

        public static Polarity forExpression(final OpinionExpression expression) {
            return forLabel(expression == null ? null : expression.getPolarity());
        }

        public static Polarity forLabel(@Nullable final String string) {
            if (string != null) {
                final String s = string.toLowerCase();
                if (s.contains("pos")) {
                    return POSITIVE;
                } else if (s.contains("neg")) {
                    return NEGATIVE;
                }
            }
            return NEUTRAL;
        }

    }
    
    public static class OpinionHolder implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = -956906026133317235L;
        private String type;
        private Span<Term> span;
        private final List<ExternalRef> externalReferences;

        OpinionHolder(final Span<Term> span) {
            this.span = span;
            this.externalReferences = new ArrayList<ExternalRef>();
        }

        OpinionHolder(final OpinionHolder oh, final HashMap<String, Term> terms) {
            /* Copy span */
            final Span<Term> span = oh.span;
            final List<Term> targets = span.getTargets();
            final List<Term> copiedTargets = new ArrayList<Term>();
            for (final Term term : targets) {
                final Term copiedTerm = terms.get(term.getId());
                if (copiedTerm == null) {
                    throw new IllegalStateException("Term not found when copying opinion_holder");
                }
                copiedTargets.add(copiedTerm);
            }
            if (span.hasHead()) {
                final Term copiedHead = terms.get(span.getHead().getId());
                this.span = new Span<Term>(copiedTargets, copiedHead);
            } else {
                this.span = new Span<Term>(copiedTargets);
            }
            this.externalReferences = new ArrayList<ExternalRef>();
            for (final ExternalRef externalRef : oh.getExternalRefs()) {
                this.externalReferences.add(new ExternalRef(externalRef));
            }
        }

        public boolean hasType() {
            return this.type != null;
        }

        public String getType() {
            return this.type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public List<Term> getTerms() {
            return this.span.getTargets();
        }

        public void addTerm(final Term term) {
            this.span.addTarget(term);
        }

        public void addTerm(final Term term, final boolean isHead) {
            this.span.addTarget(term, isHead);
        }

        public Span<Term> getSpan() {
            return this.span;
        }

        public void setSpan(final Span<Term> span) {
            this.span = span;
        }

        public ExternalRef getExternalRef(final String resource) {
            for (final ExternalRef ref : this.externalReferences) {
                if (ref.getResource().equalsIgnoreCase(resource)) {
                    return ref;
                }
            }
            return null;
        }

        public List<ExternalRef> getExternalRefs() {
            return this.externalReferences;
        }

        public void addExternalRef(final ExternalRef externalRef) {
            this.externalReferences.add(externalRef);
        }

        public void addExternalRefs(final List<ExternalRef> externalRefs) {
            this.externalReferences.addAll(externalRefs);
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof OpinionTarget)) {
                return false;
            }
            final OpinionHolder other = (OpinionHolder) object;
            return Objects.equals(this.span, other.span) && Objects.equals(this.type, other.type)
                    && Objects.equals(this.externalReferences, other.externalReferences);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.span, this.type, this.externalReferences);
        }

        @Override
        public String toString() {
            return "Holder: " + this.span;
        }

    }

    public static class OpinionTarget implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = -9128844215615857214L;
        private Span<Term> span;
        private String type;
        private final List<ExternalRef> externalReferences;

        OpinionTarget(final Span<Term> span) {
            this.span = span;
            this.externalReferences = new ArrayList<ExternalRef>();
        }

        OpinionTarget(final OpinionTarget ot, final HashMap<String, Term> terms) {
            /* Copy span */
            final Span<Term> span = ot.span;
            final List<Term> targets = span.getTargets();
            final List<Term> copiedTargets = new ArrayList<Term>();
            for (final Term term : targets) {
                final Term copiedTerm = terms.get(term.getId());
                if (copiedTerm == null) {
                    throw new IllegalStateException("Term not found when copying opinion_target");
                }
                copiedTargets.add(copiedTerm);
            }
            if (span.hasHead()) {
                final Term copiedHead = terms.get(span.getHead().getId());
                this.span = new Span<Term>(copiedTargets, copiedHead);
            } else {
                this.span = new Span<Term>(copiedTargets);
            }
            this.externalReferences = new ArrayList<ExternalRef>();
            for (final ExternalRef externalRef : ot.getExternalRefs()) {
                this.externalReferences.add(new ExternalRef(externalRef));
            }
        }

        public boolean hasType() {
            return this.type != null;
        }

        public String getType() {
            return this.type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public List<Term> getTerms() {
            return this.span.getTargets();
        }

        public void addTerm(final Term term) {
            this.span.addTarget(term);
        }

        public void addTerm(final Term term, final boolean isHead) {
            this.span.addTarget(term, isHead);
        }

        public Span<Term> getSpan() {
            return this.span;
        }

        public void setSpan(final Span<Term> span) {
            this.span = span;
        }

        public ExternalRef getExternalRef(final String resource) {
            for (final ExternalRef ref : this.externalReferences) {
                if (ref.getResource().equalsIgnoreCase(resource)) {
                    return ref;
                }
            }
            return null;
        }

        public List<ExternalRef> getExternalRefs() {
            return this.externalReferences;
        }

        public void addExternalRef(final ExternalRef externalRef) {
            this.externalReferences.add(externalRef);
        }

        public void addExternalRefs(final List<ExternalRef> externalRefs) {
            this.externalReferences.addAll(externalRefs);
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof OpinionTarget)) {
                return false;
            }
            final OpinionTarget other = (OpinionTarget) object;
            return Objects.equals(this.span, other.span) && Objects.equals(this.type, other.type)
                    && Objects.equals(this.externalReferences, other.externalReferences);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.span, this.type, this.externalReferences);
        }

        @Override
        public String toString() {
            return "Target: " + this.span;
        }

    }

    public static class OpinionExpression implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 3404051215983026456L;

        /* Polarity (optional) */
        private String polarity;

        /* Strength (optional) */
        private String strength;

        /* Subjectivity (optional) */
        private String subjectivity;

        /* Sentiment semantic type (optional) */
        private String sentimentSemanticType;

        /* Sentiment product feature (optional) */
        private String sentimentProductFeature;

        private Span<Term> span;

        private final List<ExternalRef> externalReferences;

        OpinionExpression(final Span<Term> span) {
            this.span = span;
            this.externalReferences = new ArrayList<ExternalRef>();
        }

        OpinionExpression(final OpinionExpression oe, final HashMap<String, Term> terms) {
            this.polarity = oe.polarity;
            this.strength = oe.strength;
            this.subjectivity = oe.subjectivity;
            this.sentimentSemanticType = oe.sentimentSemanticType;
            this.sentimentProductFeature = oe.sentimentProductFeature;
            /* Copy span */
            final Span<Term> span = oe.span;
            final List<Term> targets = span.getTargets();
            final List<Term> copiedTargets = new ArrayList<Term>();
            for (final Term term : targets) {
                final Term copiedTerm = terms.get(term.getId());
                if (copiedTerm == null) {
                    throw new IllegalStateException(
                            "Term not found when copying opinion_expression");
                }
                copiedTargets.add(copiedTerm);
            }
            if (span.hasHead()) {
                final Term copiedHead = terms.get(span.getHead().getId());
                this.span = new Span<Term>(copiedTargets, copiedHead);
            } else {
                this.span = new Span<Term>(copiedTargets);
            }
            this.externalReferences = new ArrayList<ExternalRef>();
            for (final ExternalRef externalRef : oe.getExternalRefs()) {
                this.externalReferences.add(new ExternalRef(externalRef));
            }
        }

        public boolean hasPolarity() {
            return this.polarity != null;
        }

        public String getPolarity() {
            return this.polarity;
        }

        public void setPolarity(final String polarity) {
            this.polarity = polarity;
        }

        public boolean hasStrength() {
            return this.strength != null;
        }

        public String getStrength() {
            return this.strength;
        }

        public void setStrength(final String strength) {
            this.strength = strength;
        }

        public boolean hasSubjectivity() {
            return this.subjectivity != null;
        }

        public String getSubjectivity() {
            return this.subjectivity;
        }

        public void setSubjectivity(final String subjectivity) {
            this.subjectivity = subjectivity;
        }

        public boolean hasSentimentSemanticType() {
            return this.sentimentSemanticType != null;
        }

        public String getSentimentSemanticType() {
            return this.sentimentSemanticType;
        }

        public void setSentimentSemanticType(final String sentimentSemanticType) {
            this.sentimentSemanticType = sentimentSemanticType;
        }

        public boolean hasSentimentProductFeature() {
            return this.sentimentProductFeature != null;
        }

        public String getSentimentProductFeature() {
            return this.sentimentProductFeature;
        }

        public void setSentimentProductFeature(final String sentimentProductFeature) {
            this.sentimentProductFeature = sentimentProductFeature;
        }

        public List<Term> getTerms() {
            return this.span.getTargets();
        }

        public void addTerm(final Term term) {
            this.span.addTarget(term);
        }

        public void addTerm(final Term term, final boolean isHead) {
            this.span.addTarget(term, isHead);
        }

        public Span<Term> getSpan() {
            return this.span;
        }

        public void setSpan(final Span<Term> span) {
            this.span = span;
        }

        public ExternalRef getExternalRef(final String resource) {
            for (final ExternalRef ref : this.externalReferences) {
                if (ref.getResource().equalsIgnoreCase(resource)) {
                    return ref;
                }
            }
            return null;
        }

        public List<ExternalRef> getExternalRefs() {
            return this.externalReferences;
        }

        public void addExternalRef(final ExternalRef externalRef) {
            this.externalReferences.add(externalRef);
        }

        public void addExternalRefs(final List<ExternalRef> externalRefs) {
            this.externalReferences.addAll(externalRefs);
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof OpinionExpression)) {
                return false;
            }
            final OpinionExpression other = (OpinionExpression) object;
            return Objects.equals(this.polarity, other.polarity)
                    && Objects.equals(this.strength, other.strength)
                    && Objects.equals(this.subjectivity, other.subjectivity)
                    && Objects.equals(this.sentimentSemanticType, other.sentimentSemanticType)
                    && Objects.equals(this.sentimentProductFeature, other.sentimentProductFeature)
                    && Objects.equals(this.span, other.span)
                    && Objects.equals(this.externalReferences, other.externalReferences);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.polarity, this.strength, this.subjectivity,
                    this.sentimentSemanticType, this.sentimentProductFeature, this.span,
                    this.externalReferences);
        }

        @Override
        public String toString() {
            return "Expression " + this.polarity + ": " + this.span;
        }

    }

    private String id;
    private OpinionHolder opinionHolder;
    private OpinionTarget opinionTarget;
    private OpinionExpression opinionExpression;
    private String label;
    private final List<ExternalRef> externalReferences;

    public String getLabel() {
        return this.label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    Opinion(final String id) {
        this.id = id;
        this.externalReferences = new ArrayList<ExternalRef>();
    }

    Opinion(final Opinion opinion, final HashMap<String, Term> terms) {
        this.id = opinion.id;
        if (opinion.opinionHolder != null) {
            this.opinionHolder = new OpinionHolder(opinion.opinionHolder, terms);
        }
        if (opinion.opinionTarget != null) {
            this.opinionTarget = new OpinionTarget(opinion.opinionTarget, terms);
        }
        if (opinion.opinionExpression != null) {
            this.opinionExpression = new OpinionExpression(opinion.opinionExpression, terms);
        }
        this.externalReferences = new ArrayList<ExternalRef>();
        for (final ExternalRef externalRef : opinion.getExternalRefs()) {
            this.externalReferences.add(new ExternalRef(externalRef));
        }
    }

    public String getId() {
        return this.id;
    }

    void setId(final String id) {
        this.id = id;
    }

    public String getPolarity() {
        return this.opinionExpression == null ? null : this.opinionExpression.getPolarity();
    }

    public void setPolarity(final String polarity) {
        if (this.opinionExpression != null) {
            this.opinionExpression.setPolarity(polarity);
        } else if (polarity != null) {
            this.opinionExpression = new Opinion.OpinionExpression(KAFDocument.newTermSpan());
            this.opinionExpression.setPolarity(polarity);
        }
    }

    public Span<Term> getExpressionSpan() {
        return this.opinionExpression == null ? null : this.opinionExpression.getSpan();
    }

    public void setExpressionSpan(final Span<Term> expressionSpan) {
        if (this.opinionExpression != null) {
            this.opinionExpression.setSpan(expressionSpan != null ? expressionSpan //
                    : KAFDocument.newTermSpan());
        } else if (expressionSpan != null && !expressionSpan.isEmpty()) {
            this.opinionExpression = new Opinion.OpinionExpression(expressionSpan);
        }
    }

    public Span<Term> getHolderSpan() {
        return this.opinionHolder == null ? null : this.opinionHolder.getSpan();
    }

    public void setHolderSpan(final Span<Term> holderSpan) {
        if (holderSpan == null || holderSpan.isEmpty()) {
            this.opinionHolder = null;
        } else if (this.opinionHolder == null) {
            this.opinionHolder = new Opinion.OpinionHolder(holderSpan);
        } else {
            this.opinionHolder.setSpan(holderSpan);
        }
    }

    public Span<Term> getTargetSpan() {
        return this.opinionTarget == null ? null : this.opinionTarget.getSpan();
    }

    public void setTargetSpan(final Span<Term> targetSpan) {
        if (targetSpan == null || targetSpan.isEmpty()) {
            this.opinionTarget = null;
        } else if (this.opinionTarget == null) {
            this.opinionTarget = new Opinion.OpinionTarget(targetSpan);
        } else {
            this.opinionTarget.setSpan(targetSpan);
        }
    }

    public OpinionHolder getOpinionHolder() {
        return this.opinionHolder;
    }

    public OpinionTarget getOpinionTarget() {
        return this.opinionTarget;
    }

    public OpinionExpression getOpinionExpression() {
        return this.opinionExpression;
    }

    public OpinionHolder createOpinionHolder(final Span<Term> span) {
        this.opinionHolder = new Opinion.OpinionHolder(span);
        return this.opinionHolder;
    }

    public OpinionTarget createOpinionTarget(final Span<Term> span) {
        this.opinionTarget = new Opinion.OpinionTarget(span);
        return this.opinionTarget;
    }

    public OpinionExpression createOpinionExpression(final Span<Term> span) {
        this.opinionExpression = new Opinion.OpinionExpression(span);
        return this.opinionExpression;
    }

    public OpinionHolder removeOpinionHolder() {
        final OpinionHolder result = this.opinionHolder;
        this.opinionHolder = null;
        return result;
    }

    public OpinionTarget removeOpinionTarget() {
        final OpinionTarget result = this.opinionTarget;
        this.opinionTarget = null;
        return result;
    }

    public OpinionExpression removeOpinionExpression() {
        final OpinionExpression result = this.opinionExpression;
        this.opinionExpression = null;
        return result;
    }

    public String getSpanStr(final Span<Term> span) {
        String str = "";
        for (final Term term : span.getTargets()) {
            if (!str.isEmpty()) {
                str += " ";
            }
            str += term.getStr();
        }
        return str;
    }

    public String getStr() {
        return getSpanStr(getOpinionExpression().getSpan());
    }

    public ExternalRef getExternalRef(final String resource) {
        for (final ExternalRef ref : this.externalReferences) {
            if (ref.getResource().equalsIgnoreCase(resource)) {
                return ref;
            }
        }
        return null;
    }

    public List<ExternalRef> getExternalRefs() {
        return this.externalReferences;
    }

    public void addExternalRef(final ExternalRef externalRef) {
        this.externalReferences.add(externalRef);
    }

    public void addExternalRefs(final List<ExternalRef> externalRefs) {
        this.externalReferences.addAll(externalRefs);
    }

}
