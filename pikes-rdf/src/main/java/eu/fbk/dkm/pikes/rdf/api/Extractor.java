package eu.fbk.dkm.pikes.rdf.api;

import eu.fbk.rdfpro.rules.model.QuadModel;

public interface Extractor {

    void extract(Object document, QuadModel model) throws Exception;

    public static Extractor concat(final Extractor... extractors) {
        return new Extractor() {

            @Override
            public void extract(final Object document, final QuadModel model) throws Exception {
                for (final Extractor extractor : extractors) {
                    extractor.extract(document, model);
                }
            }

        };
    }

}
