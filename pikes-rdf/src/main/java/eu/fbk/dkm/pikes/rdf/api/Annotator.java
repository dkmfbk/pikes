package eu.fbk.dkm.pikes.rdf.api;

import java.util.Map;

import eu.fbk.rdfpro.util.QuadModel;

public interface Annotator {

    Annotation annotate(QuadModel model) throws Exception;

    static Annotator newHttpRestAnnotator(final String url, final boolean post,
            final Map<String, String> parameters, final Map<String, String> headers,
            final Annotation.Format format) {
        return new HttpRestAnnotator(url, post, parameters, headers, format);
    }

    static Annotator newRoundRobinAnnotator(final Annotator... annotators) {
        // TODO
        return null;
    }

}
