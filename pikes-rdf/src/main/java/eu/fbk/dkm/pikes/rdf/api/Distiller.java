package eu.fbk.dkm.pikes.rdf.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandler;

import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.Ruleset;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.core.IO;

public interface Distiller extends AutoCloseable {

    final Distiller NIL = new Distiller() {

        @Override
        public void distill(final Document document, @Nullable final Map<String, String> options) {
            // do nothing
        }

    };

    void distill(Document document, @Nullable Map<String, String> options);

    @Override
    default void close() {
        // do nothing
    }

    public static Distiller concat(@Nullable final Iterable<? extends Distiller> distillers) {
        if (distillers == null || Iterables.isEmpty(distillers)) {
            return NIL;
        } else if (Iterables.size(distillers) == 1) {
            return Objects.requireNonNull(Iterables.getOnlyElement(distillers));
        } else {
            final List<Distiller> delegates = ImmutableList.copyOf(distillers);
            return new Distiller() {

                @Override
                public void distill(final Document document,
                        @Nullable Map<String, String> options) {
                    options = options == null ? null : ImmutableMap.copyOf(options);
                    for (final Distiller delegate : delegates) {
                        delegate.distill(document, options);
                    }
                }

                @Override
                public void close() {
                    for (final Distiller delegate : delegates) {
                        IO.closeQuietly(delegate);
                    }
                }

            };
        }
    }

    public static Distiller forProcessor(final RDFProcessor processor) {
        return new Distiller() {

            @Override
            public void distill(final Document document,
                    @Nullable final Map<String, String> options) {
                // TODO options
                final List<Statement> statements = new ArrayList<>();
                final RDFSource source = RDFSources.wrap(document.getModel());
                final RDFHandler handler = RDFHandlers.wrap(statements);
                processor.wrap(source).emit(handler, 1);
                document.getModel().addAll(statements);
            }

        };
    }

    public static Distiller newRuleDistiller(final Ruleset... rulesets) {
        // TODO
        return new Distiller() {

            @Override
            public void distill(final Document document,
                    @Nullable final Map<String, String> options) {
                final IRI instanceURI = Statements.VALUE_FACTORY.createIRI("ex:fred");
                document.getModel().add(instanceURI, RDF.TYPE, KS.INSTANCE);
            }

        };
    }

}
