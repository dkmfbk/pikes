package eu.fbk.dkm.pikes.tintop.annotators;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.pipeline.SentenceAnnotator;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.RuntimeInterruptedException;

public class NERConfidenceAnnotator extends SentenceAnnotator {

    private static final Logger LOGGER = Logger.getLogger(NERConfidenceAnnotator.class);

    private static final int DEFAULT_MAX_LABELINGS = 100;

    private static final double DEFAULT_MIN_SPAN_CONFIDENCE = 0.5;

    private static final Set<String> DEFAULT_PASS_DOWN_PROPERTIES = CollectionUtils.asSet(
            "encoding", "inputEncoding", "outputEncoding", "maxAdditionalKnownLCWords", "map",
            "ner.combinationMode");

    private final AbstractSequenceClassifier<CoreLabel> ner;

    private final long maxTime;

    private final int nThreads;

    private final int maxSentenceLength;

    private final int maxLabelings;

    private final double minSpanConfidence;

    public NERConfidenceAnnotator(final String name, final Properties properties) {
        try {
            this.ner = CRFClassifier.<CoreLabel>getClassifier(DefaultPaths.DEFAULT_NER_CONLL_MODEL,
                    PropertiesUtils.extractSelectedProperties(properties,
                            DEFAULT_PASS_DOWN_PROPERTIES));
            this.maxTime = PropertiesUtils.getLong(properties, name + ".maxtime", -1);
            this.nThreads = PropertiesUtils.getInt(properties, name + ".nthreads",
                    PropertiesUtils.getInt(properties, "nthreads", 1));
            this.maxSentenceLength = PropertiesUtils.getInt(properties, name + ".maxlength",
                    Integer.MAX_VALUE);
            this.maxLabelings = PropertiesUtils.getInt(properties, name + ".maxLabelings",
                    DEFAULT_MAX_LABELINGS);
            this.minSpanConfidence = PropertiesUtils.getDouble(properties,
                    name + "minSpanConfidence", DEFAULT_MIN_SPAN_CONFIDENCE);
        } catch (final ClassNotFoundException | IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    protected int nThreads() {
        return this.nThreads;
    }

    @Override
    protected long maxTime() {
        return this.maxTime;
    }

    @Override
    public void annotate(final Annotation annotation) {
        super.annotate(annotation);
        // nsc.finalizeClassification(annotation); TODO
    }

    @Override
    public void doOneSentence(final Annotation annotation, final CoreMap sentence) {

        // Retrieve the tokens of the sentence
        final List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

        // If the sentence is too long, mark all the tokens with NER label O, probability 1.0
        if (this.maxSentenceLength > 0 && tokens.size() > this.maxSentenceLength) {
            doOneFailedSentence(annotation, sentence);
            return;
        }

        try {
            // Obtain top K labelings, each scored with a probability
            final Counter<List<CoreLabel>> labelings = this.ner.classifyKBest(tokens,
                    NamedEntityTagAnnotation.class, this.maxLabelings);

            // Map labelings to <span, scored tags> entities
            final Map<Span, Counter<String>> entities = extractEntities(labelings);

            // Resolve overlappings by dropping low-priority spans
            final int numEntitiesBefore = entities.size();
            filterEntities(entities, this.minSpanConfidence);

            // Annotate input tokens
            for (final Entry<Span, Counter<String>> entry : entities.entrySet()) {
                final Span span = entry.getKey();
                final Counter<String> counter = entry.getValue();
                final String tag = Counters.argmax(counter);
                for (int i = span.begin; i < span.end; ++i) {
                    final CoreLabel token = tokens.get(i);
                    token.set(ScoredNamedEntityTagsAnnotation.class, counter);
                    token.set(NamedEntityTagAnnotation.class, tag);
                    token.set(NormalizedNamedEntityTagAnnotation.class, tag);
                }
            }

            // Log outcome, if enabled
            if (LOGGER.isDebugEnabled()) {
                final StringBuilder builder = new StringBuilder();
                builder.append("NER annotation for \"").append(annotation).append("\":");
                builder.append("\n  ").append(labelings.size()).append("/")
                        .append(this.maxLabelings).append(" labelings, ")
                        .append(Counters.L1Norm(Counters.exp(labelings)))
                        .append(" confidence total; ");
                builder.append("\n  ").append(entities.size()).append("/")
                        .append(numEntitiesBefore)
                        .append(" non-overlapping entities with conf >= ")
                        .append(this.minSpanConfidence);
                for (final Entry<Span, Counter<String>> entry : entities.entrySet()) {
                    final Span span = entry.getKey();
                    final Counter<String> counter = entry.getValue();
                    builder.append("\n  ");
                    for (int i = span.begin; i < span.end; ++i) {
                        builder.append(tokens.get(i));
                    }
                    builder.append(" = ").append(counter);
                }
                LOGGER.debug(builder.toString());
            }

        } catch (final RuntimeInterruptedException ex) {
            // If interrupted, mark all tokens with NER label O, probability 1.0
            doOneFailedSentence(annotation, sentence);
            return;
        }
    }

    @Override
    public void doOneFailedSentence(final Annotation annotation, final CoreMap sentence) {
        final List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for (final CoreLabel token : tokens) {
            final String tag = this.ner.backgroundSymbol(); // should be "O"
            final Counter<String> counter = new ClassicCounter<String>();
            counter.setCount(tag, 1.0);
            token.set(ScoredNamedEntityTagsAnnotation.class, counter);
            token.set(NamedEntityTagAnnotation.class, tag);
            token.set(NormalizedNamedEntityTagAnnotation.class, tag);
        }
    }

    @Override
    public Set<Requirement> requires() {
        return TOKENIZE_SSPLIT_POS_LEMMA;
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(NER_REQUIREMENT);
    }

    public static class ScoredNamedEntityTagsAnnotation
            implements CoreAnnotation<Counter<String>> {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Class<Counter<String>> getType() {
            return (Class) Counter.class;
        }

    }

    private static Map<Span, Counter<String>> extractEntities(
            final Counter<List<CoreLabel>> labelingCounters) {

        final Map<Span, Counter<String>> entities = Maps.newHashMap();
        for (final List<CoreLabel> labeling : labelingCounters.keySet()) {
            final double expProb = labelingCounters.getCount(labeling);
            final double prob = Math.exp(expProb);
            int index = 0;
            while (index < labeling.size()) {
                final int beginIndex = index++;
                final String tag = labeling.get(beginIndex).get(NamedEntityTagAnnotation.class);
                if (tag != null && !"O".equalsIgnoreCase(tag)) {
                    while (index < labeling.size() && tag
                            .equals(labeling.get(index).get(NamedEntityTagAnnotation.class))) {
                        ++index;
                    }
                    final Span span = new Span(beginIndex, index);
                    Counter<String> entityCounter = entities.get(span);
                    if (entityCounter == null) {
                        entityCounter = new ClassicCounter<>();
                        entities.put(span, entityCounter);
                    }
                    entityCounter.incrementCount(tag, prob);
                }
            }
        }
        return entities;
    }

    private static void filterEntities(final Map<Span, Counter<String>> entities,
            final double minSpanConfidence) {

        // Do nothing if there are no entities
        if (entities.isEmpty()) {
            return;
        }

        // Compute <span, span confidence> pairs, dropping spans whose confidence is below the
        // threshold and identifying max token index;
        int maxIndex = 0;
        final Counter<Span> spans = new ClassicCounter<>();
        for (final Entry<Span, Counter<String>> entry : ImmutableList
                .copyOf(entities.entrySet())) {
            final Span span = entry.getKey();
            final Counter<String> tags = entry.getValue();
            final double confidence = Counters.L1Norm(tags);
            if (confidence >= minSpanConfidence) {
                spans.setCount(span, confidence);
                maxIndex = Math.max(maxIndex, span.end);
            } else {
                entities.remove(span);
            }
        }

        // Scan spans from the one with highest confidence, dropping it in case it overlaps with
        // tokens of previously scanned spans (this strategy resolves overlaps by always taking
        // the most probable span)
        final boolean[] taggedTokens = new boolean[maxIndex];
        outer: for (final Span span : Counters.toSortedList(spans)) {
            for (int index = span.begin; index < span.end; ++index) {
                if (taggedTokens[index]) {
                    entities.remove(span);
                    continue outer; // skip span
                }
            }
            for (int index = span.begin; index < span.end; ++index) {
                taggedTokens[index] = true;
            }
        }
    }

    private static final class Span {

        final short begin;

        final short end;

        Span(final int begin, final int end) {
            this.begin = (short) begin;
            this.end = (short) end;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof Span)) {
                return false;
            }
            final Span other = (Span) object;
            return this.begin == other.begin && this.end == other.end;
        }

        @Override
        public int hashCode() {
            return this.begin << 16 | this.end;
        }

        @Override
        public String toString() {
            return this.begin + "," + this.end;
        }

    }

}