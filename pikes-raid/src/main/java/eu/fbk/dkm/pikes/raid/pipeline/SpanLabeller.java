package eu.fbk.dkm.pikes.raid.pipeline;

import com.google.common.collect.*;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.utils.Range;
import eu.fbk.dkm.utils.eval.ConfusionMatrix;
import eu.fbk.dkm.utils.eval.PrecisionRecall;
import eu.fbk.dkm.utils.eval.SetPrecisionRecall;
import eu.fbk.dkm.utils.svm.Classifier;
import eu.fbk.dkm.utils.svm.FeatureStats;
import eu.fbk.dkm.utils.svm.LabelledVector;
import eu.fbk.dkm.utils.svm.Vector;
import ixa.kaflib.*;
import ixa.kaflib.Predicate.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class SpanLabeller {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpanLabeller.class);

    private final Classifier classifier;

    private Predictor predictor;

    private SpanLabeller(final Classifier classifier) {
        this.classifier = classifier;
        this.predictor = new Predictor() {

            @Override
            public LabelledVector predict(final Vector vector) {
                return classifier.predict(false, vector);
            }

        };
    }

    public static SpanLabeller readFrom(final Path path) throws IOException {
        return new SpanLabeller(Classifier.readFrom(path));
    }

    public Span<Term> expand(final KAFDocument document, final Iterable<Term> heads,
            final Iterable<Term> excludedTerms, final boolean merge) {

        // Start expanding all the heads
        final Set<Term> headSet = ImmutableSet.copyOf(heads);
        final Set<Term> terms = Sets.newHashSet();
        for (final Term head : headSet) {
            final Iterable<Term> exclusion = Iterables.concat(excludedTerms,
                    Sets.difference(headSet, ImmutableSet.of(head)));
            terms.addAll(expand(document, head, exclusion).getTargets());
        }

        // Return null if no term was selected
        if (terms.isEmpty()) {
            return KAFDocument.newTermSpan();
        }

        // Merge separate ranges, if requested and possible
        if (merge) {
            int startIndex = Integer.MAX_VALUE;
            int endIndex = Integer.MIN_VALUE;
            for (final Term term : terms) {
                final int index = document.getTerms().indexOf(term);
                startIndex = Math.min(startIndex, index);
                endIndex = Math.max(endIndex, index);
            }
            for (int i = startIndex + 1; i < endIndex; ++i) {
                final Term term = document.getTerms().get(i);
                if (!terms.contains(term) && terms.contains(document.getTerms().get(i - 1))) {
                    final Dep dep = document.getDepToTerm(term);
                    if (dep != null) {
                        final String func = dep.getRfunc().toUpperCase();
                        if ((func.contains("COORD") || func.equals("P"))
                                && (terms.contains(document.getTerms().get(i + 1)) || i + 2 <= endIndex
                                        && terms.contains(document.getTerms().get(i + 2)))) {
                            terms.add(term);
                        }
                    }
                }
            }
        }

        // Create and return the resulting span, setting all the heads in it
        final Span<Term> result = KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR)
                .sortedCopy(terms));
        Iterables.addAll(result.getHeads(), headSet);
        return result;
    }

    public Span<Term> expand(final KAFDocument document, final Term head,
            final Iterable<Term> excludedTerms) {
        return expand(document, this.predictor, excludedTerms, getMinimalSpan(document, head));
    }

    public static List<Term> getTermsByDepAncestors(final KAFDocument document,
            final Iterable<Term> ancestors, final String allowedPos) {

        final List<Term> terms = Lists.newArrayList();
        final List<Term> queue = Lists.newLinkedList();
        Iterables.addAll(queue, ancestors);

        while (!queue.isEmpty()) {
            final Term term = queue.remove(0);
            for (final Dep dep : document.getDepsFromTerm(term)) {
                final Term to = dep.getTo();
                final char pos = Character.toUpperCase(to.getPos().charAt(0));
                if (allowedPos.indexOf(pos) >= 0) {
                    terms.add(to);
                } else {
                    queue.add(to);
                }
            }
        }

        Collections.sort(terms, Term.OFFSET_COMPARATOR);
        return terms;
    }

    private static Span<Term> expand(final KAFDocument document, final Predictor predictor,
            @Nullable final Iterable<Term> marked, final Span<Term> span) {

        // Build a set of marked term
        final Set<Term> markedSet = marked == null ? ImmutableSet.of() : ImmutableSet
                .copyOf(marked);

        // Select terms recursively
        final Set<Term> selection = Sets.newHashSet();
        expandRecursive(document, predictor, markedSet, span, span, 0, selection);

        // Create and return resulting span
        final Span<Term> result = KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR)
                .sortedCopy(selection), NAFUtils.extractHead(document, span));
        // System.out.println(span.getStr() + " -> " + result.getStr()); // TODO
        return result;
    }

    private static void expandRecursive(final KAFDocument document, final Predictor predictor,
            final Set<Term> marked, final Span<Term> span, final Span<Term> root, final int depth,
            final Set<Term> selection) {

        // Add the supplied span to the selection
        selection.addAll(span.getTargets());

        // Build a list of related terms comprising R, Q, V, G, D terms dominated by the head
        final List<Span<Term>> children = Lists.newArrayList();
        for (final Term head : span.getTargets()) {
            final List<Term> queue = Lists.newLinkedList();
            queue.add(head);
            while (!queue.isEmpty()) {
                final Term term = queue.remove(0);
                final List<Dep> deps = document.getDepsFromTerm(term);
                if (!deps.isEmpty()) {
                    for (final Dep dep : deps) {
                        final Term to = dep.getTo();
                        if (!span.getTargets().contains(to)) {
                            if ("PC".contains(to.getPos())) {
                                queue.add(to);
                            } else {
                                children.add(getMinimalSpan(document, to));
                            }
                        }
                    }
                } else if (!span.getTargets().contains(term)) {
                    children.add(getMinimalSpan(document, term));
                }
            }
        }

        // Sort child spans by absolute offset distance w.r.t. reference span
        final int offset = getSpanOffset(span);
        Collections.sort(children, new Comparator<Span<Term>>() {

            @Override
            public int compare(final Span<Term> span1, final Span<Term> span2) {
                final int distance1 = Math.abs(offset - getSpanOffset(span1));
                final int distance2 = Math.abs(offset - getSpanOffset(span2));
                return distance1 - distance2;
            }

        });

        // Select terms, relying on the supplied predictor
        for (final Span<Term> child : children) {
            final Vector features = features(document, marked, selection, root, span, child, depth);
            if (predictor.predict(features).getLabel() == 1) {
                for (final Term term : child.getTargets()) {
                    for (Dep dep = document.getDepToTerm(term); dep != null
                            && !selection.contains(dep.getFrom()); dep = document.getDepToTerm(dep
                            .getFrom())) {
                        selection.add(dep.getFrom());
                    }
                }
                expandRecursive(document, predictor, marked, child, root, depth + 1, selection);
            }
        }
    }

    // private static List<String> features(final String prefix, final Term term,
    // final String externalRefResource) {
    // final List<String> result = Lists.newArrayList();
    // for (final ExternalRef ref : NAFUtils.getRefs(term, externalRefResource, null)) {
    // result.add(prefix + ref.getReference());
    // }
    // return result;
    // }

    private static List<String> features(final String prefix, final Range parent, final Range child) {
        final int dist = parent.distance(child);
        final String direction = child.begin() > parent.begin() ? "after" : "before";
        final String distance = dist <= 1 ? "adjacent" : dist <= 3 ? "verynear"
                : dist <= 6 ? "near" : "far";
        return ImmutableList.of(prefix + distance, prefix + direction, prefix + direction + "."
                + distance);
    }

    private static Vector features(final KAFDocument document, final Set<Term> marked,
            final Set<Term> selection, final Span<Term> root, final Span<Term> parent,
            final Span<Term> child, final int depth) {

        // Extract main terms
        // final Term rootTerm = getMainTerm(document, root);
        final Term parentTerm = getMainTerm(document, parent);
        final Term childTerm = getMainTerm(document, child);
        final Term childHead = NAFUtils.extractHead(document, child);

        // Compute dependency data
        Dep dep = document.getDepToTerm(childHead);
        String path = dep.getRfunc();
        String pathex = dep.getRfunc() + "-" + dep.getTo().getMorphofeat().substring(0, 1);
        final Set<Term> connectives = Sets.newHashSet();
        while (!parent.getTargets().contains(dep.getFrom())) {
            connectives.add(dep.getFrom());
            dep = document.getDepToTerm(dep.getFrom());
            path = dep.getRfunc() + "." + path;
            pathex = dep.getRfunc() + "-" + dep.getTo().getLemma().toLowerCase() + "." + pathex;
        }

        // Allocate a builder for constructing the feature vector
        final Vector.Builder builder = Vector.builder();

        // Add document id (not used for training, only for proper CV splitting)
        builder.set("_cluster." + document.getPublic().uri);

        // Add term index (not used for training / classification)
        builder.set("_index", document.getTerms().indexOf(childTerm));
        builder.set("depth" + depth);

        // Add features related to relative span positions
        final Set<Term> descendants = document.getTermsByDepAncestors(Iterables.concat(
                child.getTargets(), connectives));
        final Range rootRange = Range.enclose(NAFUtils.termRangesFor(document, root.getTargets()));
        final Range parentRange = Range.enclose(NAFUtils.termRangesFor(document,
                parent.getTargets()));
        final Range childRange = Range
                .enclose(NAFUtils.termRangesFor(document, child.getTargets()));
        final Range descendantsRange = Range
                .enclose(NAFUtils.termRangesFor(document, descendants));
        builder.set(features("pos.descroot.", rootRange, descendantsRange));
        builder.set(features("pos.descparent.", parentRange, descendantsRange));
        builder.set(features("pos.childparent.", parentRange, childRange));

        final List<Range> parentRanges = NAFUtils.termRangesFor(document, parent.getTargets());
        final List<Range> selectionRanges = NAFUtils.termRangesFor(document, selection);
        final List<Range> descendantRanges = NAFUtils.termRangesFor(document, descendants);
        builder.set("span.enclosed", Range.enclose(selectionRanges).overlaps(descendantRanges));
        builder.set("span.connected",
                Range.enclose(selectionRanges).connectedWith(descendantRanges));
        builder.set("span.connected.parent",
                Range.enclose(parentRanges).connectedWith(descendantRanges));
        builder.set("span.marked", marked.contains(childTerm));
        builder.set("span.marked.descendant", !Sets.intersection(marked, descendants).isEmpty());
        builder.set("span.depth", depth);

        // Add root features
        // builder.set("parent.pos." + parentTerm.getPos());
        // if (parentTerm != rootTerm) {
        // builder.set("root.pos." + rootTerm.getMorphofeat().substring(0, 1));
        // builder.set("root.named", rootTerm.getMorphofeat().startsWith("NNP"));
        // }

        // Add parent features
        // builder.set("parent.pos." + parentTerm.getPos());
        builder.set("parent.pos." + parentTerm.getMorphofeat().substring(0, 1));
        builder.set("parent.named", parentTerm.getMorphofeat().startsWith("NNP"));
        builder.set("parent.lemma." + parentTerm.getLemma().toLowerCase());
        builder.set("parent.morph." + parentTerm.getMorphofeat());

        // builder.set("parent.entity", !document.getEntitiesByTerm(parentTerm).isEmpty());
        // builder.set("parent.timex",
        // !document.getTimeExsByWF(parentTerm.getWFs().get(0)).isEmpty());
        // builder.set("parent.predicate", !document.getPredicatesByTerm(parentTerm).isEmpty());
        // builder.set(features("parent.sst.", parentTerm, NAFUtils.RESOURCE_WN_SST));

        // Add child features
        // builder.set("child.pos." + childTerm.getPos());
        builder.set("child.pos." + childTerm.getMorphofeat().substring(0, 1));
        builder.set("child.named", childTerm.getMorphofeat().startsWith("NNP"));
        builder.set("child.lemma." + childTerm.getLemma().toLowerCase());
        builder.set("child.morph." + childTerm.getMorphofeat());
        // builder.set("child.entity", !document.getEntitiesByTerm(childTerm).isEmpty());
        // builder.set("child.timex",
        // !document.getTimeExsByWF(childTerm.getWFs().get(0)).isEmpty());
        // builder.set("child.predicate", !document.getPredicatesByTerm(childTerm).isEmpty());
        // builder.set(features("child.sst.", childTerm, NAFUtils.RESOURCE_WN_SST));

        // for (final Entity entity : document.getEntitiesByTerm(childTerm)) {
        // if (entity.getType() != null) {
        // builder.set("child.entity." + entity.getType().toLowerCase());
        // }
        // }
        // for (final Entity entity : document.getEntitiesBySent(childTerm.getSent())) {
        // if (entity.getType() != null
        // && entity.isNamed()
        // && !Sets.intersection(ImmutableSet.copyOf(entity.getTerms()), descendants)
        // .isEmpty()) {
        // builder.set("child.entity");
        // builder.set("child.entity." + entity.getType().toLowerCase());
        // }
        // }

        for (final Dep childDep : document.getDepsFromTerm(childHead)) {
            final Term to = childDep.getTo();
            // final boolean before = to.getOffset() < childHead.getOffset();
            // builder.set("depdown." + childDep.getRfunc() + "." + (before ? "before" :
            // "after"));
            builder.set("depdown." + childDep.getRfunc() + "."
                    + to.getMorphofeat().substring(0, 1));
        }
        // builder.set("dep.path." + path);
        // builder.set("dep.pospath." + path + "-" + childTerm.getPos());
        // builder.set("dep.pospath." + parentTerm.getPos() + "-" + path + "-" +
        // childTerm.getPos());
        // builder.set("deppos." + dep.getRfunc() + "." + childTerm.getMorphofeat());
        builder.set("dep." + pathex);

        builder.set("dep." + (depth == 0 ? "top." : "nested.") + dep.getRfunc());
        if (dep.getRfunc().equals("COORD")) {
            // System.out.println("*** " + parent.getStr() + " --> " + child.getStr());
        }
        // builder.set("dep.funcpos." + (childIndex > parentIndex ? "after." : "before.")
        // + dep.getRfunc() + "." + childTerm.getMorphofeat());
        if (!child.getTargets().contains(dep.getTo())) {
            // builder.set("dep.funcposconn." + dep.getRfunc() + "." + childTerm.getMorphofeat()
            // + "." + dep.getTo().getLemma().toLowerCase());
            // builder.set("dep.funcposconn." + (childIndex > parentIndex ? "after." : "before.")
            // + dep.getRfunc() + "." + childTerm.getMorphofeat() + "."
            // + dep.getTo().getLemma().toLowerCase());
        }

        // Emit SRL features
        for (final Predicate predicate : document.getPredicatesByTerm(parentTerm)) {
            for (final Role role : predicate.getRoles()) {
                if (role.getTerms().contains(childTerm)) {
                    builder.set("srl." + role.getSemRole());
                }
            }
        }

        // Finalize the construction and return the resulting feature vector
        return builder.build();
    }

    private static Span<Term> getMinimalSpan(final KAFDocument document, final Term term) {
        final Set<Term> terms = Sets.newHashSet(term);
        for (final Entity entity : document.getEntitiesByTerm(term)) {
            if (document.getTermsHead(Iterables.concat(terms, entity.getTerms())) != null) {
                terms.addAll(entity.getTerms());
            }
        }
        final List<Term> queue = Lists.newLinkedList(terms);
        while (!queue.isEmpty()) {
            final Term t = queue.remove(0);
            for (final Dep dep : document.getDepsByTerm(t)) {
                if (dep.getRfunc().equals("VC") || dep.getRfunc().equals("IM")) {
                    if (terms.add(dep.getFrom())) {
                        queue.add(dep.getFrom());
                    }
                    if (terms.add(dep.getTo())) {
                        queue.add(dep.getTo());
                    }
                }
            }
        }
        final Term head = document.getTermsHead(terms);
        return KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms),
                head);
    }

    private static Term getMainTerm(final KAFDocument document, final Span<Term> span) {
        @SuppressWarnings("deprecation")
        Term term = span.getHead();
        if (term == null) {
            term = document.getTermsHead(span.getTargets());
            span.setHead(term);
        }
        outer: while (true) {
            for (final Dep dep : document.getDepsFromTerm(term)) {
                if (dep.getRfunc().equals("VC") || dep.getRfunc().equals("IM")) {
                    term = dep.getTo();
                    continue outer;
                }
            }
            break;
        }
        return term;
    }

    private static int getSpanOffset(final Span<Term> span) {
        int offset = Integer.MAX_VALUE;
        for (final Term term : span.getTargets()) {
            offset = Math.min(term.getOffset(), offset);
        }
        return offset;
    }

    public void writeTo(final Path path) throws IOException {
        this.classifier.writeTo(path);
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof SpanLabeller)) {
            return false;
        }
        final SpanLabeller other = (SpanLabeller) object;
        return this.classifier.equals(other.classifier);
    }

    @Override
    public int hashCode() {
        return this.classifier.hashCode();
    }

    @Override
    public String toString() {
        return "HeadExpander (" + this.classifier.toString() + ")";
    }

    public static Trainer train() {
        return new Trainer();
    }

    public static final class Trainer {

        private final List<LabelledVector> trainingSet;

        private final SetPrecisionRecall.Evaluator evaluator;

        private Trainer() {
            this.trainingSet = Lists.newArrayList();
            this.evaluator = SetPrecisionRecall.evaluator();
        }

        public void add(final KAFDocument document, final Term head,
                final Iterable<Term> excluded, final Span<Term> span) {

            final Set<Term> spanTerms = ImmutableSet.copyOf(span.getTargets());
            final Set<Term> excludedTerms = ImmutableSet.copyOf(excluded);

            final Span<Term> outSpan = expand(document, new Predictor() {

                @Override
                public LabelledVector predict(final Vector vector) {
                    final Term term = document.getTerms().get((int) vector.getValue("_index"));
                    final boolean included = spanTerms.contains(term)
                            && !excludedTerms.contains(term);
                    final LabelledVector result = vector.label(included ? 1 : 0);
                    Trainer.this.trainingSet.add(result);
                    return result;
                }

            }, excluded, getMinimalSpan(document, head));

            this.evaluator.add(ImmutableList.of(spanTerms),
                    ImmutableList.of(ImmutableSet.copyOf(outSpan.getTargets())));
        }

        public SpanLabeller end(final int gridSize, final boolean analyze) throws IOException {

            // Emit feature stats if enabled
            if (analyze && LOGGER.isInfoEnabled()) {
                LOGGER.info("Feature analysis (top 30 features):\n{}", FeatureStats.toString(
                        FeatureStats.forVectors(2, this.trainingSet, null).values(), 30));
            }

            // Log the performance penalty caused by the candidate selection algorithm
            LOGGER.info("Maximum achievable performances on training set due to recursive "
                    + "algorithm: " + this.evaluator.getResult());

            // Perform training considering a grid of parameters of the size specified (min 1)
            // final List<Classifier.Parameters> grid =
            // Classifier.Parameters.forLinearLRLossL1Reg(2,
            // new float[] { 1f, 1f }, 1f, 1f).grid(Math.max(1, gridSize), 10.0f);
            final List<Classifier.Parameters> grid = Lists.newArrayList();
            for (final float weight : new float[] { 0.25f, 0.5f, 1.0f, 2.0f, 4.0f }) {
                // grid.addAll(Classifier.Parameters.forSVMPolyKernel(2, new float[] { 1f, weight
                // },
                // 1f, 1f, 0.0f, 3).grid(Math.max(1, gridSize), 10.0f));

                grid.addAll(Classifier.Parameters.forSVMLinearKernel(2,
                        new float[] { 1f, weight }, 1f).grid(Math.max(1, gridSize), 10.0f));
                // grid.addAll(Classifier.Parameters.forLinearLRLossL1Reg(2,
                // new float[] { 1f, weight }, 1f, 1f).grid(Math.max(1, gridSize), 10.0f));
            }
            final Classifier classifier = Classifier.train(grid, this.trainingSet,
                    ConfusionMatrix.labelComparator(PrecisionRecall.Measure.F1, 1, true), 100000);

            // THE SVM BELOW WAS ORIGINALLY USED
            // final Classifier.Parameters parameters = Classifier.Parameters.forSVMRBFKernel(2,
            // new float[] { 1f, 1f }, 1f, .1f);
            // final Classifier classifier = Classifier.train(parameters, this.trainingSet);

            // Log parameters of the best classifier
            LOGGER.info("Best classifier parameters: {}", classifier.getParameters());

            // Perform cross-validation and emit some performance statistics, if enabled
            if (analyze && LOGGER.isInfoEnabled()) {
                final List<LabelledVector> trainingPredictions = classifier.predict(false,
                        this.trainingSet);
                final ConfusionMatrix matrix = LabelledVector.evaluate(this.trainingSet,
                        trainingPredictions, 2);
                LOGGER.info("Performances on training set:\n{}", matrix);
                final ConfusionMatrix crossMatrix = Classifier.crossValidate(
                        classifier.getParameters(), this.trainingSet, 5, -1);
                LOGGER.info("5-fold cross-validation performances:\n{}", crossMatrix);
            }

            // Build and return the created span labeller
            return new SpanLabeller(classifier);
        }

    }

    private interface Predictor {

        LabelledVector predict(final Vector vector);

    }

}
