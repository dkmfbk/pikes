package eu.fbk.dkm.pikes.raid.pipeline;

import com.google.common.collect.*;
import eu.fbk.dkm.pikes.raid.Component;
import eu.fbk.dkm.pikes.raid.Opinions;
import eu.fbk.dkm.pikes.raid.Trainer;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class PipelineTrainer extends Trainer<PipelineExtractor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineTrainer.class);

    @Nullable
    private final LinkLabeller.Trainer holderLinkTrainer;

    @Nullable
    private final LinkLabeller.Trainer targetLinkTrainer;

    @Nullable
    private final SpanLabeller.Trainer holderSpanTrainer;

    @Nullable
    private final SpanLabeller.Trainer targetSpanTrainer;

    private final int linkGridSize;

    private final int spanGridSize;

    private final boolean analyze;

    private final boolean jointSpan;

    private final boolean holderUnique;

    private final boolean targetUnique;

    private final NAFFilter filter;

    public PipelineTrainer(final Properties properties, final Component... components) {
        super(components);
        final boolean hasHolder = components().contains(Component.HOLDER);
        final boolean hasTarget = components().contains(Component.TARGET);
        this.holderUnique = Boolean.parseBoolean(properties.getProperty("holder.unique", "false"));
        this.targetUnique = Boolean.parseBoolean(properties.getProperty("target.unique", "false"));
        this.linkGridSize = Integer.parseInt(properties.getProperty("gridsize.link", "25"));
        this.spanGridSize = Integer.parseInt(properties.getProperty("gridsize.span", "25"));
        this.analyze = Boolean.parseBoolean(properties.getProperty("analyze", "true"));
        this.jointSpan = Boolean.parseBoolean(properties.getProperty("joint", "true"));
        this.holderLinkTrainer = hasHolder ? LinkLabeller.train("NN", "PRP", "JJP", "DTP", "WP")
                : null;
        this.targetLinkTrainer = hasTarget ? LinkLabeller.train("NN", "PRP", "JJP", "DTP", "WP",
                "VB") : null;
        if (this.jointSpan) {
            final SpanLabeller.Trainer t = hasHolder || hasTarget ? SpanLabeller.train() : null;
            this.holderSpanTrainer = t;
            this.targetSpanTrainer = t;
        } else {
            this.holderSpanTrainer = hasHolder ? SpanLabeller.train() : null;
            this.targetSpanTrainer = hasTarget ? SpanLabeller.train() : null;
        }
        this.filter = NAFFilter.builder(false).withTermSenseCompletion(true)
                .withEntityAddition(true).withEntityRemoveOverlaps(true)
                .withEntitySpanFixing(true).withSRLPredicateAddition(true)
                .withSRLRemoveWrongRefs(true).withSRLSelfArgFixing(true).build();
    }

    @Override
    protected void doFilter(final KAFDocument document) {
        this.filter.accept(document);
    }

    @Override
    protected synchronized void doAdd(final KAFDocument document, final int sentence,
            final Opinion[] opinions) {
        addExpressions(document, sentence, opinions);
        addArguments(document, sentence, opinions);
    }

    @Override
    protected synchronized PipelineExtractor doTrain() throws IOException {

        // TODO: Alessio

        // Train link labellers, if enabled
        LinkLabeller holderLinkLabeller = null;
        LinkLabeller targetLinkLabeller = null;
        if (components().contains(Component.HOLDER)) {
            LOGGER.info("====== Training holder link labeller ======");
            holderLinkLabeller = this.holderLinkTrainer.end(this.linkGridSize, this.analyze);
        }
        if (components().contains(Component.TARGET)) {
            LOGGER.info("====== Training target link labeller ======");
            targetLinkLabeller = this.targetLinkTrainer.end(this.linkGridSize, this.analyze);
        }

        // Train span labellers, if enabled
        SpanLabeller holderSpanLabeller = null;
        SpanLabeller targetSpanLabeller = null;
        if (this.jointSpan) {
            if (this.holderLinkTrainer != null) {
                LOGGER.info("====== Training joint holder/target span labeller ======");
                holderSpanLabeller = this.holderSpanTrainer.end(this.spanGridSize, this.analyze);
                targetSpanLabeller = holderSpanLabeller;
            }
        } else {
            if (components().contains(Component.HOLDER)) {
                LOGGER.info("====== Training holder span labeller ======");
                holderSpanLabeller = this.holderSpanTrainer.end(this.spanGridSize, this.analyze);
            }
            if (components().contains(Component.TARGET)) {
                LOGGER.info("====== Training target span labeller ======");
                targetSpanLabeller = this.targetSpanTrainer.end(this.spanGridSize, this.analyze);
            }
        }

        // Build and return the resulting opinion extractor
        return new PipelineExtractor(holderLinkLabeller, targetLinkLabeller, holderSpanLabeller,
                targetSpanLabeller, this.holderUnique, this.targetUnique);
    }

    private void addExpressions(final KAFDocument document, final int sentence,
            final Opinion[] opinions) {
        // TODO: Alessio
    }

    private void addArguments(final KAFDocument document, final int sentence,
            final Opinion[] opinions) {

        // Index holder and target spans by expression head, keeping track of all exp. heads
        final Set<Term> expressionHeads = Sets.newHashSet();
        final Multimap<Term, Span<Term>> holderSpans = HashMultimap.create();
        final Multimap<Term, Span<Term>> targetSpans = HashMultimap.create();
        for (final Opinion opinion : opinions) {
            final Set<Term> heads = Opinions.heads(document,
                    NAFUtils.normalizeSpan(document, opinion.getExpressionSpan()),
                    Component.EXPRESSION);
            if (!heads.isEmpty()) {
                final Term head = Ordering.from(Term.OFFSET_COMPARATOR).max(heads);
                expressionHeads.add(head);
                final Span<Term> holderSpan = opinion.getHolderSpan();
                final Span<Term> targetSpan = opinion.getTargetSpan();
                if (holderSpan != null) {
                    holderSpans.putAll(head, NAFUtils.splitSpan(document, holderSpan, //
                            Opinions.heads(document, holderSpan, Component.HOLDER)));
                }
                if (targetSpan != null) {
                    targetSpans.putAll(head, NAFUtils.splitSpan(document, targetSpan, //
                            Opinions.heads(document, targetSpan, Component.TARGET)));
                }
            }
        }

        // Add training samples for holder and target extraction, separately (if enabled)
        for (final Term expressionHead : expressionHeads) {
            if (components().contains(Component.HOLDER)) {
                addArguments(document, sentence, expressionHead, holderSpans.get(expressionHead),
                        this.holderLinkTrainer, this.holderSpanTrainer);
            }
            if (components().contains(Component.TARGET)) {
                addArguments(document, sentence, expressionHead, targetSpans.get(expressionHead),
                        this.targetLinkTrainer, this.targetSpanTrainer);
            }
        }
    }

    private void addArguments(final KAFDocument document, final int sentence,
            final Term expressionHead, final Iterable<Span<Term>> argSpans,
            final LinkLabeller.Trainer linkTrainer, final SpanLabeller.Trainer spanTrainer) {

        // Extract heads and spans of the arguments (only where defined)
        final List<Term> heads = Lists.newArrayList();
        final List<Span<Term>> spans = Lists.newArrayList();
        for (final Span<Term> span : argSpans) {
            final Term head = NAFUtils.extractHead(document, span);
            if (head != null) {
                heads.add(head);
                spans.add(span);
            }
        }

        // Add a sample for node labelling
        linkTrainer.add(document, expressionHead, heads);

        // Add samples for span labelling (one for each argument)
        for (int i = 0; i < heads.size(); ++i) {
            final List<Term> excludedTerms = Lists.newArrayList(heads);
            excludedTerms.remove(heads.get(i));
            spanTrainer.add(document, heads.get(i), excludedTerms, spans.get(i));
        }
    }

}
