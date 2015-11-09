package eu.fbk.dkm.pikes.rdf.api;

public interface Extractor {

    void extract(Document document) throws Exception;

    public static Extractor concat(final Extractor... extractors) {
        return new Extractor() {

            private final Extractor[] delegates = extractors.clone();

            @Override
            public void extract(final Document document) throws Exception {
                for (final Extractor extractor : this.delegates) {
                    extractor.extract(document);
                }
            }

        };
    }

}
