package eu.fbk.dkm.pikes.rdf.api;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;

import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.QuadModel;

public interface Distiller {

    void distill(QuadModel model) throws Exception;

    public static Distiller concat(final Distiller... mappers) {
        return new Distiller() {

            @Override
            public void distill(final QuadModel model) throws Exception {
                for (final Distiller mapper : mappers) {
                    mapper.distill(model);
                }
            }

        };
    }

    public static Distiller wrap(final RDFProcessor processor) {
        return new Distiller() {

            @Override
            public void distill(final QuadModel model) throws Exception {
                final List<Statement> statements = new ArrayList<>();
                final RDFSource source = RDFSources.wrap(model);
                final RDFHandler handler = RDFHandlers.wrap(statements);
                processor.wrap(source).emit(handler, 1);
                model.addAll(statements);
            }

        };
    }

}
