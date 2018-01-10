package eu.fbk.dkm.pikes.rdf.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import eu.fbk.utils.core.IO;

public interface Extractor extends AutoCloseable {

    final Extractor NIL = new Extractor() {

        @Override
        public void extract(final Document document, @Nullable final Map<String, String> options) {
            // do nothing
        }

    };

    void extract(final Document document, @Nullable final Map<String, String> options);

    // // TODO: keep method, remove default implementation
    // default void extract(final Document document, @Nullable final Map<String, String> options)
    // {
    // for (final Object annotation : document.getAnnotations()) {
    // if (annotation instanceof KAFDocument) {
    // final KAFDocument kaf = (KAFDocument) annotation;
    // final boolean[] sentenceIDs = new boolean[kaf.getNumSentences()];
    // Arrays.fill(sentenceIDs, true);
    // try {
    // extract(annotation, document.getModel(), sentenceIDs);
    // } catch (final Throwable ex) {
    // Throwables.throwIfUnchecked(ex);
    // throw new RuntimeException(ex);
    // }
    // }
    // }
    // }
    //
    // // TODO: drop this method
    // @Deprecated
    // default void extract(final Object annotation, final Model model, final boolean[]
    // sentenceIDs)
    // throws Exception {
    // extract(new Document(model, ImmutableList.of(annotation)), null);
    // }

    @Override
    default void close() {
        // do nothing
    }

    public static Extractor concat(@Nullable final Iterable<? extends Extractor> extractors) {
        if (extractors == null || Iterables.isEmpty(extractors)) {
            return NIL;
        } else if (Iterables.size(extractors) == 1) {
            return Objects.requireNonNull(Iterables.getOnlyElement(extractors));
        } else {
            final List<Extractor> delegates = ImmutableList.copyOf(extractors);
            return new Extractor() {

                @Override
                public void extract(final Document document,
                        @Nullable Map<String, String> options) {
                    options = options == null ? null : ImmutableMap.copyOf(options);
                    for (final Extractor delegate : delegates) {
                        delegate.extract(document, options);
                    }
                }

                @Override
                public void close() {
                    for (final Extractor delegate : delegates) {
                        IO.closeQuietly(delegate);
                    }
                }

            };
        }
    }

}
