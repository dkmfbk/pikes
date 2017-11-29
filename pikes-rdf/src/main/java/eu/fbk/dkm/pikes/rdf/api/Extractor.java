package eu.fbk.dkm.pikes.rdf.api;


import org.eclipse.rdf4j.model.Model;

public interface Extractor {

    void extract(Object document, Model model, boolean[] sentenceIDs) throws Exception;

    public static Extractor concat(final Extractor... extractors) {
        return new Extractor() {

            @Override
            public void extract(final Object document, final Model model, final boolean[] sentenceIDs) throws Exception {
                for (final Extractor extractor : extractors) {
                    extractor.extract(document, model, sentenceIDs);
                }
            }

        };
    }

}
