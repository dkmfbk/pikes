package eu.fbk.dkm.pikes.rdf.api;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

public interface Service extends Annotator, Extractor, Distiller, Renderer, AutoCloseable {

    default void process(final Document document, @Nullable final Map<String, String> options) {
        process(document, null, options);
    }

    default void process(final Document document, @Nullable final Appendable out,
            @Nullable final Map<String, String> options) {
        Objects.requireNonNull(document);
        Objects.requireNonNull(out);
        process(document, options);
        if (out != null) {
            render(document, out, options);
        }
    }

    @Override
    default void close() {
        // do nothing
    }

}
