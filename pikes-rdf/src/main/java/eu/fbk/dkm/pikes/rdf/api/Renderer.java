package eu.fbk.dkm.pikes.rdf.api;

import java.util.Map;

import javax.annotation.Nullable;

public interface Renderer extends AutoCloseable {

    final Renderer NIL = new Renderer() {

        @Override
        public void render(final Document document, final Appendable out,
                final Map<String, String> options) {
            // do nothing
        }

    };

    void render(Document document, Appendable out, @Nullable Map<String, String> options);

    @Override
    default void close() {
        // do nothing
    }

}
