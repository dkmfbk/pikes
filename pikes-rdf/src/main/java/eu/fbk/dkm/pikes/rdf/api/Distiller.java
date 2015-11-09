package eu.fbk.dkm.pikes.rdf.api;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;

import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.Ruleset;
import eu.fbk.rdfpro.util.Statements;

public interface Distiller {

    void distill(Document document) throws Exception;

    public static Distiller concat(final Distiller... distillers) {
        return new Distiller() {

            private final Distiller[] delegates = distillers.clone();

            @Override
            public void distill(final Document document) throws Exception {
                for (final Distiller delegate : this.delegates) {
                    delegate.distill(document);
                }
            }

        };
    }

    public static Distiller wrap(final RDFProcessor processor, final boolean replace) {
        return new Distiller() {

            @Override
            public void distill(final Document document) throws Exception {
                final List<Statement> statements = new ArrayList<>();
                final RDFSource source = RDFSources.wrap(document.getGraph());
                final RDFHandler handler = RDFHandlers.wrap(statements);
                processor.wrap(source).emit(handler, 1);
                if (replace) {
                    document.getGraph().clear();
                }
                document.getGraph().addAll(statements);
            }

        };
    }

    public static Distiller newRuleDistiller(final Ruleset... rulesets) {
        // TODO
        return new Distiller() {

            @Override
            public void distill(final Document document) throws Exception {
                final URI instanceURI = Statements.VALUE_FACTORY.createURI("ex:fred");
                document.getGraph().add(instanceURI, RDF.TYPE, KS.INSTANCE);
            }

        };
    }
}
