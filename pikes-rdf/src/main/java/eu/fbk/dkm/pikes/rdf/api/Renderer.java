package eu.fbk.dkm.pikes.rdf.api;


import eu.fbk.rdfpro.util.QuadModel;

public interface Renderer {

    void render(Object document, QuadModel model, Appendable out) throws Exception;

}
