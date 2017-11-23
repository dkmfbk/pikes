package eu.fbk.dkm.pikes.rdf.api;

import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.QuadModel;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;

import java.util.ArrayList;
import java.util.List;

public interface Mapper {

    void map(QuadModel model) throws Exception;

    public static Mapper concat(final Mapper... mappers) {
        return new Mapper() {

            @Override
            public void map(final QuadModel model) throws Exception {
                for (final Mapper mapper : mappers) {
                    mapper.map(model);
                }
            }

        };
    }

    public static Mapper forProcessor(final RDFProcessor processor) {
        return new Mapper() {

            @Override
            public void map(final QuadModel model) throws Exception {
                final List<Statement> statements = new ArrayList<>();
                final RDFSource source = RDFSources.wrap(model);
                final RDFHandler handler = RDFHandlers.wrap(statements);
                processor.wrap(source).emit(handler, 1);
                model.addAll(statements);
            }

        };
    }

}
