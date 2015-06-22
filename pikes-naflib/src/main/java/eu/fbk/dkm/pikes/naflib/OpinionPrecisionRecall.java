package eu.fbk.dkm.pikes.naflib;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import eu.fbk.dkm.utils.Util;
import eu.fbk.dkm.utils.eval.ConfusionMatrix;
import eu.fbk.dkm.utils.eval.PrecisionRecall;
import eu.fbk.dkm.utils.eval.SetPrecisionRecall;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Opinion.Polarity;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;


public final class OpinionPrecisionRecall implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpinionPrecisionRecall.class);

    private static final Set<Object> NULL_TERMS = ImmutableSet.of(new Object());

    private static final Set<Object> IMPLICIT = ImmutableSet.of(new Object());

    private static final Set<Object> WRITER = ImmutableSet.of(new Object());

    private static final long serialVersionUID = 1L;

    private final ConfusionMatrix polarityCM;

    private final SetPrecisionRecall[] polaritySPRsByValue;

    private final SetPrecisionRecall polaritySPR;

    private final SetPrecisionRecall expressionSPR;

    private final SetPrecisionRecall holderSPR;

    private final SetPrecisionRecall targetSPR;

    public OpinionPrecisionRecall(final ConfusionMatrix polarityCM,
                                  final SetPrecisionRecall[] polaritySPRsByValue, final SetPrecisionRecall polaritySPR,
                                  final SetPrecisionRecall expressionSPR, final SetPrecisionRecall holderSPR,
                                  final SetPrecisionRecall targetSPR) {

        Preconditions.checkNotNull(polaritySPRsByValue);
        Preconditions.checkArgument(polaritySPRsByValue.length == Polarity.values().length);

        this.polarityCM = Preconditions.checkNotNull(polarityCM);
        this.polaritySPRsByValue = polaritySPRsByValue.clone();
        this.polaritySPR = Preconditions.checkNotNull(polaritySPR);
        this.expressionSPR = Preconditions.checkNotNull(expressionSPR);
        this.holderSPR = Preconditions.checkNotNull(holderSPR);
        this.targetSPR = Preconditions.checkNotNull(targetSPR);
    }

    public ConfusionMatrix getPolarityCM() {
        return this.polarityCM;
    }

    public SetPrecisionRecall getPolaritySPR() {
        return this.polaritySPR;
    }

    public SetPrecisionRecall getPolaritySPR(final Polarity polarity) {
        return Preconditions.checkNotNull(this.polaritySPRsByValue[polarity.ordinal()]);
    }

    public SetPrecisionRecall getExpressionSPR() {
        return this.expressionSPR;
    }

    public SetPrecisionRecall getHolderSPR() {
        return this.holderSPR;
    }

    public SetPrecisionRecall getTargetSPR() {
        return this.targetSPR;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof OpinionPrecisionRecall)) {
            return false;
        }
        final OpinionPrecisionRecall other = (OpinionPrecisionRecall) object;
        return this.polarityCM.equals(other.polarityCM)
                && this.polaritySPRsByValue.equals(other.polaritySPRsByValue)
                && this.polaritySPR.equals(other.polaritySPR)
                && this.expressionSPR.equals(other.expressionSPR)
                && this.holderSPR.equals(other.holderSPR)
                && this.targetSPR.equals(other.targetSPR);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.polarityCM, this.polaritySPRsByValue, this.polaritySPR,
                this.expressionSPR, this.holderSPR, this.targetSPR);
    }

    public String toString(final boolean includePolarityCM) {
        final StringBuilder builder = new StringBuilder();
        builder.append("                             aligned              intersection"
                + "                   overlap                     exact                 counts");
        builder.append("\n                 p     r    f1     a       p     r    f1     a       "
                + "p     r    f1     a       p     r    f1     a       tp     fp     fn");
        builder.append("\n---------------------------------------------------------------------"
                + "--------------------------------------------------------------------");
        toStringHelper(builder, "expression", this.expressionSPR);
        toStringHelper(builder, "holder", this.holderSPR);
        toStringHelper(builder, "target", this.targetSPR);
        toStringHelper(builder, "polarity", this.polaritySPR);
        toStringHelper(builder, "- neutral", this.polaritySPRsByValue[Polarity.NEUTRAL.ordinal()]);
        toStringHelper(builder, "- positive",
                this.polaritySPRsByValue[Polarity.POSITIVE.ordinal()]);
        toStringHelper(builder, "- negative",
                this.polaritySPRsByValue[Polarity.NEGATIVE.ordinal()]);
        if (includePolarityCM) {
            builder.append("\n\n");
            builder.append(this.polarityCM.toString("neu", "pos", "neg"));
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    private void toStringHelper(final StringBuilder builder, final String label,
            final SetPrecisionRecall spr) {
        builder.append(String.format("\n%-10s", label));
        for (final PrecisionRecall pr : new PrecisionRecall[] { spr.getAlignedPR(),
                spr.getIntersectionPR(), spr.getOverlapPR(), spr.getExactPR() }) {
            builder.append(String.format("   %5.3f %5.3f %5.3f %5.3f", pr.getPrecision(),
                    pr.getRecall(), pr.getF1(), pr.getAccuracy()));
        }
        builder.append(String.format("   %6d %6d %6d", (int) spr.getExactPR().getTP(), (int) spr
                .getExactPR().getFP(), (int) spr.getExactPR().getFN()));
    }

    private static Set<Object> getHolder(@Nullable final Opinion opinion) {
        if (opinion != null) {
            final Span<Term> holder = opinion.getHolderSpan();
            if (holder != null && !holder.isEmpty()) {
                return ImmutableSet.copyOf(holder.getTargets());
            }
            final OpinionExpression expr = opinion.getOpinionExpression();
            if (expr != null && "implicit".equalsIgnoreCase(expr.getSentimentProductFeature())) {
                return IMPLICIT;
            }
        }
        return WRITER; // default
    }

    private static Set<Object> getTarget(@Nullable final Opinion opinion) {
        if (opinion != null) {
            final Span<Term> target = opinion.getTargetSpan();
            if (target != null && !target.isEmpty()) {
                return ImmutableSet.copyOf(target.getTargets());
            }
        }
        return NULL_TERMS;
    }

    private static Set<Object> getExpression(@Nullable final Opinion opinion) {
        if (opinion != null) {
            final Span<Term> expr = opinion.getExpressionSpan();
            if (expr != null && !expr.isEmpty()) {
                return ImmutableSet.copyOf(expr.getTargets());
            }
        }
        return NULL_TERMS;
    }

    private static Polarity getPolarity(@Nullable final Opinion opinion) {
        return opinion == null || opinion.getOpinionExpression() == null ? Polarity.NEUTRAL
                : Polarity.forExpression(opinion.getOpinionExpression());
    }

    private static Map<Set<Object>, Polarity> getPolarityMap(final Iterable<Opinion> opinions,
            @Nullable final Polarity expectedPolarity) {
        final Map<Set<Object>, Polarity> map = Maps.newHashMap();
        for (final Opinion opinion : opinions) {
            final Set<Object> expression = getExpression(opinion);
            final Polarity polarity = getPolarity(opinion);
            if (expectedPolarity != null && !expectedPolarity.equals(polarity)) {
                continue;
            }
            final Polarity oldPolarity = map.put(expression, polarity);
            if (oldPolarity != null && !Objects.equals(oldPolarity, polarity)) {
                LOGGER.warn("Different polarities " + oldPolarity + ", " + polarity
                        + " for expression of opinion " + opinion.getId());
            }
        }
        return map;
    }

    private static Multimap<Set<Object>, Opinion> indexOpinionsByExpression(
            final Iterable<Opinion> opinions) {
        final Multimap<Set<Object>, Opinion> map = HashMultimap.create();
        for (final Opinion opinion : opinions) {
            final Set<Object> expression = getExpression(opinion);
            map.put(expression, opinion);
        }
        return map;
    }

    private static Multimap<Integer, Opinion> indexOpinionsBySentence(
            final Iterable<Opinion> opinions) {
        final ListMultimap<Integer, Opinion> map = ArrayListMultimap.create();
        for (final Opinion opinion : opinions) {
            final OpinionExpression oe = opinion.getOpinionExpression();
            final Integer sent = oe == null || oe.getSpan() == null
                    || oe.getSpan().getTargets().isEmpty() ? null : oe.getSpan().getTargets()
                    .get(0).getSent();
            map.put(sent, opinion);
        }
        return map;
    }

    public static BiFunction<Opinion, Opinion, List<Double>> matcher() {
        return (final Opinion g, final Opinion t) -> {
            final Set<Object> ge = getExpression(g);
            final Set<Object> te = getExpression(t);
            final Set<Object> gh = getHolder(g);
            final Set<Object> th = getHolder(t);
            final Set<Object> gt = getTarget(g);
            final Set<Object> tt = getTarget(t);
            if (Sets.intersection(ge, te).isEmpty()) {
                return null;
            }
            final List<Double> scores = Lists.newArrayListWithCapacity(6);
            scores.add(Util.coverage(ge, te));
            scores.add(Util.coverage(te, ge));
            scores.add(Util.coverage(gt, tt));
            scores.add(Util.coverage(tt, gt));
            scores.add(Util.coverage(gh, th));
            scores.add(Util.coverage(th, gh));
            return scores;
        };
    }

    public static Evaluator evaluator() {
        return new Evaluator();
    }

    public static final class Evaluator {

        private final ConfusionMatrix.Evaluator polarityCMEvaluator;

        private final SetPrecisionRecall.Evaluator[] polarityByValueEvaluators;

        private final SetPrecisionRecall.Evaluator polarityEvaluator;

        private final SetPrecisionRecall.Evaluator expressionEvaluator;

        private final SetPrecisionRecall.Evaluator holderEvaluator;

        private final SetPrecisionRecall.Evaluator targetEvaluator;

        @Nullable
        private OpinionPrecisionRecall score;

        private Evaluator() {
            final int numPolarities = Polarity.values().length;
            this.polarityCMEvaluator = ConfusionMatrix.evaluator(numPolarities);
            this.polarityByValueEvaluators = new SetPrecisionRecall.Evaluator[numPolarities];
            this.polarityEvaluator = SetPrecisionRecall.evaluator();
            this.expressionEvaluator = SetPrecisionRecall.evaluator();
            this.holderEvaluator = SetPrecisionRecall.evaluator();
            this.targetEvaluator = SetPrecisionRecall.evaluator();
            this.score = null;
            for (final Polarity polarity : Polarity.values()) {
                this.polarityByValueEvaluators[polarity.ordinal()] = SetPrecisionRecall
                        .evaluator();
            }
        }

        @SuppressWarnings("unchecked")
        public synchronized Evaluator add(final Iterable<Opinion> goldOpinions,
                final Iterable<Opinion> testOpinions) {

            // Extract expressions and associated polarities and opinions
            final Map<Set<Object>, Polarity> goldMap = getPolarityMap(goldOpinions, null);
            final Map<Set<Object>, Polarity> testMap = getPolarityMap(testOpinions, null);
            final Multimap<Set<Object>, Opinion> goldMultimap = indexOpinionsByExpression(goldOpinions);
            final Multimap<Set<Object>, Opinion> testMultimap = indexOpinionsByExpression(testOpinions);

            // Update expression SPR
            this.expressionEvaluator.add(goldMap.keySet(), testMap.keySet());

            // Update polarity SPRs
            this.polarityEvaluator.add(goldMap, testMap);
            for (final Polarity polarity : Polarity.values()) {
                this.polarityByValueEvaluators[polarity.ordinal()].add(
                        getPolarityMap(goldOpinions, polarity),
                        getPolarityMap(testOpinions, polarity));
            }

            // Update polarity confusion matrix
            for (final Set<Object>[] pair : Util.align(Set.class, goldMap.keySet(),
                    testMap.keySet(), false, true, true, SetPrecisionRecall.matcher())) {
                final Set<Object> g = pair[0];
                final Set<Object> t = pair[1];
                if (t != null && g != null) {
                    final Polarity gp = goldMap.get(g);
                    final Polarity tp = testMap.get(t);
                    this.polarityCMEvaluator.add(gp.ordinal(), tp.ordinal(), 1);
                }
            }

            // Update holder and target SPRs
            for (final Set<Object>[] pair : Util.align(Set.class, goldMap.keySet(),
                    testMap.keySet(), false, true, true, SetPrecisionRecall.matcher())) {
                if (pair[0] != null && pair[1] != null) {
                    final Set<Object> g = pair[0];
                    final Set<Object> t = pair[1];
                    final Set<Set<Object>> goldHolders = Sets.newHashSet();
                    final Set<Set<Object>> goldTargets = Sets.newHashSet();
                    final Set<Set<Object>> testHolders = Sets.newHashSet();
                    final Set<Set<Object>> testTargets = Sets.newHashSet();
                    for (final Opinion opinion : goldMultimap.get(g)) {
                        goldHolders.add(getHolder(opinion));
                        goldTargets.add(getTarget(opinion));
                    }
                    for (final Opinion opinion : testMultimap.get(t)) {
                        testHolders.add(getHolder(opinion));
                        testTargets.add(getTarget(opinion));
                    }
                    this.holderEvaluator.add(goldHolders, testHolders);
                    this.targetEvaluator.add(goldTargets, testTargets);
                }
            }

            return this;
        }

        public Evaluator add(final KAFDocument document, final String goldLabel,
                final String testLabel) {

            final Multimap<Integer, Opinion> goldMap = indexOpinionsBySentence(document
                    .getOpinions(goldLabel));
            final Multimap<Integer, Opinion> testMap = indexOpinionsBySentence(document
                    .getOpinions(testLabel));

            for (int i = 0; i < document.getNumSentences(); ++i) {
                final Collection<Opinion> goldOpinions = goldMap.get(i);
                final Collection<Opinion> testOpinions = testMap.get(i);
                if (!goldOpinions.isEmpty() || !testOpinions.isEmpty()) {
                    add(goldOpinions, testOpinions);
                }
            }

            return this;
        }

        public Evaluator add(final Iterable<KAFDocument> documents, final String goldLabel,
                final String testLabel) {
            StreamSupport.stream(documents.spliterator(), true).forEach(document -> {
                Preconditions.checkNotNull(document);
                add(document, goldLabel, testLabel);
            });
            return this;
        }

        public synchronized Evaluator add(final OpinionPrecisionRecall opr) {
            this.score = null;
            this.polarityCMEvaluator.add(opr.getPolarityCM());
            for (int i = 0; i < this.polarityByValueEvaluators.length; ++i) {
                this.polarityByValueEvaluators[i].add(opr.polaritySPRsByValue[i]);
            }
            this.polarityEvaluator.add(opr.getPolaritySPR());
            this.expressionEvaluator.add(opr.getExpressionSPR());
            this.holderEvaluator.add(opr.getHolderSPR());
            this.targetEvaluator.add(opr.getTargetSPR());
            return this;
        }

        public synchronized Evaluator add(final Evaluator evaluator) {
            synchronized (evaluator) {
                this.score = null;
                this.polarityCMEvaluator.add(evaluator.polarityCMEvaluator);
                for (int i = 0; i < this.polarityByValueEvaluators.length; ++i) {
                    this.polarityByValueEvaluators[i].add(evaluator.polarityByValueEvaluators[i]);
                }
                this.polarityEvaluator.add(evaluator.polarityEvaluator);
                this.expressionEvaluator.add(evaluator.expressionEvaluator);
                this.holderEvaluator.add(evaluator.holderEvaluator);
                this.targetEvaluator.add(evaluator.targetEvaluator);
            }
            return this;
        }

        public synchronized OpinionPrecisionRecall getResult() {
            if (this.score == null) {
                final int polarities = this.polarityByValueEvaluators.length;
                final ConfusionMatrix polarityCM = this.polarityCMEvaluator.getResult();
                final SetPrecisionRecall[] polaritySPRsByValue = new SetPrecisionRecall[polarities];
                for (int i = 0; i < polarities; ++i) {
                    polaritySPRsByValue[i] = this.polarityByValueEvaluators[i].getResult();
                }
                final SetPrecisionRecall polaritySPR = this.polarityEvaluator.getResult();
                final SetPrecisionRecall expressionSPR = this.expressionEvaluator.getResult();
                final SetPrecisionRecall holderSPR = this.holderEvaluator.getResult();
                final SetPrecisionRecall targetSPR = this.targetEvaluator.getResult();
                this.score = new OpinionPrecisionRecall(polarityCM, polaritySPRsByValue,
                        polaritySPR, expressionSPR, holderSPR, targetSPR);
            }
            return this.score;
        }

    }

}
