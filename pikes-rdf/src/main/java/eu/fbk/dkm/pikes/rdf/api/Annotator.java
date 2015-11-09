package eu.fbk.dkm.pikes.rdf.api;

import java.util.Map;

public interface Annotator {

    void annotate(Document document) throws Exception;

    public static Annotator concat(final Annotator... annotators) {
        return new Annotator() {

            private final Annotator[] delegates = annotators.clone();

            @Override
            public void annotate(final Document document) throws Exception {
                for (final Annotator annotator : this.delegates) {
                    annotator.annotate(document);
                }
            }

        };
    }

    static Annotator roundRobin(final Annotator... annotators) {
        // TODO
        return null;
    }

    static Annotator newHttpAnnotator(final String url, final boolean post,
            final Map<String, String> parameters, final Map<String, String> headers,
            final Annotation.Format format) {
        return new HttpAnnotator(url, post, parameters, headers, format);
    }

}
