package eu.fbk.dkm.pikes.rdf.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import eu.fbk.utils.core.IO;

public interface Annotator extends AutoCloseable {

    final Annotator NIL = new Annotator() {

        @Override
        public void annotate(final Document document,
                @Nullable final Map<String, String> options) {
            // do nothing
        }

    };

    void annotate(Document document, @Nullable Map<String, String> options);

    @Override
    default void close() {
        // do nothing
    }

    public static Annotator concat(@Nullable final Iterable<? extends Annotator> annotators) {
        if (annotators == null || Iterables.isEmpty(annotators)) {
            return NIL;
        } else if (Iterables.size(annotators) == 1) {
            return Objects.requireNonNull(Iterables.getOnlyElement(annotators));
        } else {
            final List<Annotator> delegates = ImmutableList.copyOf(annotators);
            return new Annotator() {

                @Override
                public void annotate(final Document document,
                        @Nullable Map<String, String> options) {
                    options = options == null ? null : ImmutableMap.copyOf(options);
                    for (final Annotator delegate : delegates) {
                        delegate.annotate(document, options);
                    }
                }

                @Override
                public void close() {
                    for (final Annotator delegate : delegates) {
                        IO.closeQuietly(delegate);
                    }
                }

            };
        }
    }

}
