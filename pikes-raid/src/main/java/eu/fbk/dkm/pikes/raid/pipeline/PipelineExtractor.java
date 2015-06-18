package eu.fbk.dkm.pikes.raid.pipeline;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import eu.fbk.dkm.pikes.raid.Component;
import eu.fbk.dkm.pikes.raid.Extractor;
import eu.fbk.dkm.pikes.raid.Opinions;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import ixa.kaflib.*;
import ixa.kaflib.Opinion.Polarity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PipelineExtractor extends Extractor {

    @Nullable
    private final LinkLabeller holderLinkLabeller;

    @Nullable
    private final LinkLabeller targetLinkLabeller;

    @Nullable
    private final SpanLabeller holderSpanLabeller;

    @Nullable
    private final SpanLabeller targetSpanLabeller;

    private final boolean holderUnique;

    private final boolean targetUnique;

    private final NAFFilter filter;

    protected PipelineExtractor(final Properties properties, final Path path) throws IOException {
        this(Files.exists(path.resolve("holder-link")) ? LinkLabeller.readFrom(path
                .resolve("holder-link")) : null,
                Files.exists(path.resolve("target-link")) ? LinkLabeller.readFrom(path
                        .resolve("target-link")) : null,
                Files.exists(path.resolve("holder-span")) ? SpanLabeller.readFrom(path
                        .resolve("holder-span")) : null,
                Files.exists(path.resolve("target-span")) ? SpanLabeller.readFrom(path
                        .resolve("target-span")) : null, //
                Boolean.parseBoolean(properties.getProperty("holder.unique", "false")), //
                Boolean.parseBoolean(properties.getProperty("target.unique", "false")));
    }

    protected PipelineExtractor(@Nullable final LinkLabeller holderLinkLabeller,
            @Nullable final LinkLabeller targetLinkLabeller,
            @Nullable final SpanLabeller holderSpanLabeller,
            @Nullable final SpanLabeller targetSpanLabeller, final boolean holderUnique,
            final boolean targetUnique) {

        Preconditions.checkArgument(holderLinkLabeller == null == (holderSpanLabeller == null));
        Preconditions.checkArgument(targetLinkLabeller == null == (targetSpanLabeller == null));

        this.holderLinkLabeller = holderLinkLabeller;
        this.targetLinkLabeller = targetLinkLabeller;
        this.holderSpanLabeller = holderSpanLabeller;
        this.targetSpanLabeller = targetSpanLabeller;
        this.holderUnique = holderUnique;
        this.targetUnique = targetUnique;

        this.filter = NAFFilter.builder(false).withTermSenseCompletion(true)
                .withEntityAddition(true).withEntityRemoveOverlaps(true)
                .withEntitySpanFixing(true).withSRLPredicateAddition(true)
                .withSRLRemoveWrongRefs(true).withSRLSelfArgFixing(true).build();
    }

    @Override
    protected void doFilter(final KAFDocument document) {
        this.filter.accept(document);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Iterable<Opinion> doExtract(final KAFDocument document, final int sentence,
            final EnumSet<Component> components) {

        // Extract expressions and, for each of them, their holders and targets
        final List<Opinion> opinions = Lists.newArrayList();
        for (final Span<Term> expressionSpan : findExpressions(document, sentence)) {

            // Identify the expression head
            final Term expressionHead = Ordering.from(Term.OFFSET_COMPARATOR).min(
                    Opinions.heads(document, NAFUtils.normalizeSpan(document, expressionSpan),
                            Component.EXPRESSION));

            // Find the polarity, if enabled
            Polarity polarity = null;
            if (components.contains(Component.POLARITY)) {
                polarity = findPolarity(expressionSpan);
            }

            // Find holders, if enabled
            final List<Span<Term>> holderSpans = Lists.newArrayList((Span<Term>) null);
            if (components.contains(Component.HOLDER) && this.holderLinkLabeller != null) {
                Iterables.addAll(holderSpans, findArguments(document, sentence, expressionHead, //
                        this.holderLinkLabeller, this.holderSpanLabeller, this.holderUnique));
            }

            // Find targets, if enabled
            final List<Span<Term>> targetSpans = Lists.newArrayList((Span<Term>) null);
            if (components.contains(Component.TARGET) && this.targetLinkLabeller != null) {
                findArguments(document, sentence, expressionHead, this.targetLinkLabeller,
                        this.targetSpanLabeller, this.targetUnique);
            }

            // Emit opinions
            opinions.addAll(Opinions.create(document, expressionSpan, holderSpans, targetSpans,
                    polarity));
        }
        return opinions;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    protected Iterable<Opinion> doRefine(final KAFDocument document, final int sentence,
            final EnumSet<Component> components, final Opinion opinion) {

        // Find the polarity
        Polarity polarity = Polarity.forOpinion(opinion);
        if (components.contains(Component.POLARITY)) {
            polarity = findPolarity(opinion.getExpressionSpan());
        }

        // Retrieve the expression head
        final List<Span<Term>> holderSpans = Lists.newArrayList((Span<Term>) null);
        final List<Span<Term>> targetSpans = Lists.newArrayList((Span<Term>) null);
        final Set<Term> expressionHeads = Opinions.heads(document,
                NAFUtils.normalizeSpan(document, opinion.getExpressionSpan()),
                Component.EXPRESSION);

        // Retrieve holders and targets only if head is defined
        if (!expressionHeads.isEmpty()) {

            // Take one head
            final Term expressionHead = Ordering.from(Term.OFFSET_COMPARATOR).max(expressionHeads);

            // Find holders
            if (components.contains(Component.HOLDER) && this.holderLinkLabeller != null) {
                Iterables.addAll(holderSpans, findArguments(document, sentence, expressionHead, //
                        this.holderLinkLabeller, this.holderSpanLabeller, this.holderUnique));
            } else if (opinion.getHolderSpan() != null) {
                holderSpans.add(opinion.getHolderSpan());
            }

            // Find targets
            if (components.contains(Component.TARGET) && this.targetLinkLabeller != null) {
                Iterables.addAll(targetSpans, findArguments(document, sentence, expressionHead, //
                        this.targetLinkLabeller, this.targetSpanLabeller, this.targetUnique));
            } else if (opinion.getTargetSpan() != null) {
                targetSpans.add(opinion.getTargetSpan());
            }
        }

        // Emit opinions
        return Opinions.create(document, opinion.getExpressionSpan(), holderSpans, targetSpans,
                polarity);
    }

    @Override
    protected void doWrite(final Properties properties, final Path path) throws IOException {

        // TODO: Alessio

        if (this.holderLinkLabeller != null) {
            this.holderLinkLabeller.writeTo(path.resolve("holder-link"));
            this.holderSpanLabeller.writeTo(path.resolve("holder-span"));
        }
        if (this.targetLinkLabeller != null) {
            this.targetLinkLabeller.writeTo(path.resolve("target-link"));
            this.targetSpanLabeller.writeTo(path.resolve("target-span"));
        }

        properties.setProperty("holder.unique", Boolean.toString(this.holderUnique));
        properties.setProperty("target.unique", Boolean.toString(this.targetUnique));
    }

    private Iterable<Span<Term>> findExpressions(final KAFDocument document, final int sentence) {
        // TODO: Alessio
        return ImmutableList.of();
    }

    private List<Span<Term>> findArguments(final KAFDocument document, final int sentence,
            final Term expressionHead, final LinkLabeller linkLabeller,
            final SpanLabeller spanLabeller, boolean unique) {

        final Map<Term, Float> map = linkLabeller.label(document, expressionHead);

        final Set<Term> blockedTerms = document.getTermsByDepDescendants(ImmutableSet
                .of(expressionHead));
        final Map<Term, Set<Term>> clusters = Maps.newHashMap();
        for (final Term term : document.getTermsBySent(expressionHead.getSent())) {
            clusters.put(term, ImmutableSet.of(term));
        }
        for (final Dep dep : document.getDepsBySent(expressionHead.getSent())) {
            if ("COORD".equals(dep.getRfunc()) || "CONJ".equals(dep.getRfunc())) {
                if (blockedTerms.contains(dep.getFrom()) || blockedTerms.contains(dep.getTo())) {
                    continue;
                }
                final Set<Term> fromCluster = clusters.get(dep.getFrom());
                final Set<Term> toCluster = clusters.get(dep.getTo());
                final Set<Term> mergedCluster = ImmutableSet.copyOf(Sets.union(fromCluster,
                        toCluster));
                for (final Term term : mergedCluster) {
                    clusters.put(term, mergedCluster);
                }
            }
        }
        Float bestScore = Float.MIN_VALUE;
        Set<Term> bestCluster = null;
        while (!clusters.isEmpty()) {
            final Set<Term> cluster = clusters.values().iterator().next();
            clusters.keySet().removeAll(cluster);
            float score = 0;
            int count = 0;
            for (final Term term : cluster) {
                final Float s = map.get(term);
                if (s != null) {
                    ++count;
                    score = Math.max(score, s);
                }
            }
            if (count > 0) {

                // // TODO
                // float score2 = -1000.0f;
                // for (Term term : cluster) {
                // int len = Dep.Path.create(term, expressionHead, document).length();
                // score2 = Math.max(score2, -len);
                // }
                // score += score2;

                for (final Term term : cluster) {
                    if ("CO".indexOf(term.getPos().charAt(0)) < 0) {
                        map.put(term, score);
                    }
                }
                if (bestCluster == null || score >= bestScore) {
                    bestScore = score;
                    bestCluster = cluster;
                }
            }
        }
        if (unique && bestCluster != null) {
            map.keySet().retainAll(bestCluster);
        }

        final List<Span<Term>> argSpans = Lists.newArrayList();
        for (final Term argHead : map.keySet()) {
            final List<Term> excludedTerms = Lists.newArrayList(map.keySet());
            excludedTerms.remove(argHead);
            argSpans.add(spanLabeller.expand(document, argHead, excludedTerms));
        }

        // If a unique span is required, we add missing terms between found spans so to get a span
        // of consecutive terms
        final List<Span<Term>> spans = NAFUtils.mergeSpans(document, argSpans, unique);
        if (spans.size() <= 1) {
            return spans;
        }
        final Set<Term> terms = Sets.newHashSet();
        for (final Span<Term> span : spans) {
            terms.addAll(span.getTargets());
        }
        return ImmutableList.of(KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR)
                .sortedCopy(terms)));
    }

    private Polarity findPolarity(final Span<Term> span) {
        // TODO: Mauro
        return null;
    }

}
