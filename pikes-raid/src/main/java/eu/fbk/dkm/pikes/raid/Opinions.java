package eu.fbk.dkm.pikes.raid;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Opinion.OpinionHolder;
import ixa.kaflib.Opinion.OpinionTarget;
import ixa.kaflib.Opinion.Polarity;
import ixa.kaflib.Span;
import ixa.kaflib.Term;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public final class Opinions {

    public static Set<Term> heads(final KAFDocument document, final Span<Term> span,
            final Component component) {

        if (span == null || span.isEmpty()) {
            return ImmutableSet.of();
        }
        return NAFUtils.extractHeads(document, null, span.getTargets(),
                NAFUtils.matchExtendedPos(document, component.getHeadPos()));
    }

    public static boolean deduplicate(final KAFDocument document, final Iterable<Opinion> opinions) {
        boolean modified = false;
        final List<Opinion> seen = Lists.newArrayList();
        outer: for (final Opinion o : opinions) {
            for (final Opinion s : seen) {
                if (Objects.equal(o.getLabel(), s.getLabel())
                        && Objects.equal(o.getPolarity(), s.getPolarity())
                        && sameSpan(o.getExpressionSpan(), s.getExpressionSpan())
                        && sameSpan(o.getHolderSpan(), s.getHolderSpan())
                        && sameSpan(o.getTargetSpan(), s.getTargetSpan())) {
                    document.removeAnnotation(o);
                    modified = true;
                    continue outer;
                }
            }
            seen.add(o);
        }
        return modified;
    }

    private static boolean sameSpan(final Span<Term> span1, final Span<Term> span2) {
        return span1 == null
                && span2 == null
                || span1 != null
                && span2 != null
                && ImmutableSet.copyOf(span1.getTargets()).equals(
                        ImmutableSet.copyOf(span2.getTargets()));
    }

    public static List<Opinion> merge(final KAFDocument document,
            final Iterable<Opinion> opinions, final Iterable<Component> components) {
        // TODO
        return null;
    }

    public static List<Opinion> split(final KAFDocument document,
            final Iterable<Opinion> opinions, final Iterable<Component> components) {
        // TODO
        return null;
    }

    @Deprecated
    public static List<Opinion> split(final KAFDocument document,
            final Iterable<Opinion> inOpinions, @Nullable final String outLabel) {

        final List<Span<Term>> emptySpanList = Lists.newArrayList();
        emptySpanList.add(null);

        final List<Opinion> outOpinions = Lists.newArrayList();
        for (final Opinion inOpinion : inOpinions) {

            final OpinionExpression inExp = inOpinion.getOpinionExpression();
            final OpinionHolder inHolder = inOpinion.getOpinionHolder();
            final OpinionTarget inTarget = inOpinion.getOpinionTarget();

            final Set<Term> expHeads = heads(document, inOpinion.getExpressionSpan(),
                    Component.EXPRESSION);

            if (!expHeads.isEmpty()) {
                final List<Span<Term>> expSpans = NAFUtils.splitSpan(document, inExp.getSpan(),
                        expHeads);

                final Set<Term> holderHeads = heads(document, inOpinion.getHolderSpan(),
                        Component.HOLDER);
                final List<Span<Term>> holderSpans = holderHeads.isEmpty() ? emptySpanList //
                        : NAFUtils.splitSpan(document, inHolder.getSpan(), holderHeads);

                final Set<Term> targetHeads = heads(document, inOpinion.getTargetSpan(),
                        Component.TARGET);
                final List<Span<Term>> targetSpans = targetHeads.isEmpty() ? emptySpanList
                        : NAFUtils.splitSpan(document, inTarget.getSpan(), targetHeads);

                for (final Span<Term> expSpan : expSpans) {
                    for (final Span<Term> holderSpan : holderSpans) {
                        for (final Span<Term> targetSpan : targetSpans) {
                            final Opinion outOpinion = document.newOpinion();
                            outOpinion.setLabel(outLabel);
                            final OpinionExpression outExp = outOpinion
                                    .createOpinionExpression(expSpan);
                            outExp.setPolarity(inExp.getPolarity());
                            outExp.setSentimentProductFeature(inExp.getSentimentProductFeature());
                            outExp.setSentimentSemanticType(inExp.getSentimentSemanticType());
                            outExp.setStrength(inExp.getStrength());
                            outExp.setSubjectivity(inExp.getSubjectivity());
                            if (holderSpan != null) {
                                outOpinion.createOpinionHolder(holderSpan).setType(
                                        inOpinion.getOpinionHolder().getType());
                            }
                            if (targetSpan != null) {
                                outOpinion.createOpinionTarget(targetSpan).setType(
                                        inOpinion.getOpinionTarget().getType());
                            }
                            outOpinions.add(outOpinion);
                        }
                    }
                }
            }
        }
        return outOpinions;
    }

    public static void retain(final Iterable<Opinion> opinions,
            @Nullable final Opinion refOpinion, final Iterable<Component> components) {

        for (final Opinion opinion : opinions) {
            final OpinionExpression expr = opinion.getOpinionExpression();
            if (!Iterables.contains(components, Component.POLARITY)) {
                if (refOpinion == null || refOpinion.getOpinionExpression() == null) {
                    expr.setPolarity(null);
                    expr.setStrength(null);
                    expr.setSentimentProductFeature(null);
                    expr.setSentimentSemanticType(null);
                    expr.setSubjectivity(null);
                } else {
                    final OpinionExpression refExpr = refOpinion.getOpinionExpression();
                    expr.setPolarity(refExpr.getPolarity());
                    expr.setStrength(refExpr.getStrength());
                    expr.setSentimentProductFeature(refExpr.getSentimentProductFeature());
                    expr.setSentimentSemanticType(refExpr.getSentimentSemanticType());
                    expr.setSubjectivity(refExpr.getSubjectivity());
                }
            }
            if (!Iterables.contains(components, Component.EXPRESSION)) {
                if (refOpinion == null || refOpinion.getOpinionExpression() == null) {
                    expr.setSpan(KAFDocument.newTermSpan());
                } else {
                    expr.setSpan(refOpinion.getOpinionExpression().getSpan());
                }
            }
            if (!Iterables.contains(components, Component.HOLDER)) {
                if (refOpinion == null || refOpinion.getOpinionHolder() == null) {
                    opinion.removeOpinionHolder();
                } else {
                    opinion.createOpinionHolder(refOpinion.getOpinionHolder().getSpan()).setType(
                            refOpinion.getOpinionHolder().getType());
                }
            }
            if (!Iterables.contains(components, Component.TARGET)) {
                if (refOpinion == null || refOpinion.getOpinionTarget() == null) {
                    opinion.removeOpinionTarget();
                } else {
                    opinion.createOpinionTarget(refOpinion.getOpinionTarget().getSpan()).setType(
                            refOpinion.getOpinionTarget().getType());
                }
            }
        }
    }

    public static List<Opinion> create(final KAFDocument document, final Span<Term> expression,
            final Iterable<Span<Term>> holders, final Iterable<Span<Term>> targets,
            final Polarity polarity) {

        final int numHolders = Iterables.size(holders);
        final int numTargets = Iterables.size(targets);

        final List<Opinion> opinions = Lists.newArrayList();
        for (final Span<Term> holder : holders) {
            for (final Span<Term> target : targets) {

                // Discard wrong holder / target combinations
                if (holder == null && numHolders > 1 || target == null && numTargets > 1) {
                    continue;
                }

                // Create the opinion and its expression
                final Opinion opinion = document.createOpinion();
                final OpinionExpression expr = opinion.createOpinionExpression(expression);

                // Assign the polarity, if available
                if (polarity == Polarity.NEUTRAL) {
                    expr.setPolarity("neutral");
                } else if (polarity == Polarity.NEGATIVE) {
                    expr.setPolarity("negative");
                } else if (polarity == Polarity.POSITIVE) {
                    expr.setPolarity("positive");
                }

                // Assign the holder, if available
                if (holder != null) {
                    opinion.createOpinionHolder(holder);
                }

                // Assign the target, if available
                if (target != null) {
                    opinion.createOpinionTarget(target);
                }

                // Store the opinion
                opinions.add(opinion);
            }
        }
        return opinions;
    }

}
