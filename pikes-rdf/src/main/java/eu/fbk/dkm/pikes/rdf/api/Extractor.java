package eu.fbk.dkm.pikes.rdf.api;

import eu.fbk.rdfpro.util.QuadModel;

public interface Extractor {

    void extract(Annotation annotation, QuadModel model) throws Exception;

    public static Extractor concat(final Extractor... extractors) {
        return new Extractor() {

            @Override
            public void extract(final Annotation annotation, final QuadModel model)
                    throws Exception {
                for (final Extractor extractor : extractors) {
                    extractor.extract(annotation, model);
                }
            }

        };
    }

}
