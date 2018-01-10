package eu.fbk.dkm.pikes.rdf.rules;

import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.pikes.rdf.api.Distiller;
import eu.fbk.dkm.pikes.rdf.api.Document;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.RuleEngine;
import eu.fbk.rdfpro.Ruleset;
import eu.fbk.rdfpro.util.QuadModel;

// FIXME: this implementation is highly inefficient

public class RuleDistiller implements Distiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleDistiller.class);

    public static final Ruleset DEFAULT_RULESET;

    public static final Model DEFAULT_MAPPINGS;

    static {
        DEFAULT_RULESET = Ruleset
                .fromRDF(RuleDistiller.class.getResource("RuleDistiller.rules.ttl").toString());
        DEFAULT_MAPPINGS = new LinkedHashModel();
        RDFSources
                .read(false, true, null, null, null, true,
                        RuleDistiller.class.getResource("RuleDistiller.mapping.ttl").toString())
                .emit(RDFHandlers.wrap(DEFAULT_MAPPINGS), 1);
    }

    private final RuleEngine engine;

    private final Model mappings;

    public RuleDistiller(@Nullable final Ruleset ruleset, @Nullable final Model mappings) {
        this.engine = RuleEngine.create(ruleset != null ? ruleset : DEFAULT_RULESET);
        this.mappings = mappings != null ? mappings : DEFAULT_MAPPINGS;
    }

    @Override
    public void distill(final Document document, final Map<String, String> options) {

        final Model documentModel = document.getModel();

        final QuadModel model = QuadModel.create();
        model.addAll(this.mappings);
        model.addAll(documentModel);

        this.engine.eval(model);

        final int numKEM = documentModel.size();
        final int numMapping = this.mappings.size();
        int numDistilled = 0;

        for (final Statement stmt : model) {
            if (!this.mappings.contains(stmt) && !documentModel.contains(stmt)) {
                documentModel.add(stmt);
                ++numDistilled;
            }
        }

        LOGGER.info("{} triples distilled out of {} KEM triples and {} mapping triples",
                numDistilled, numKEM, numMapping);
    }

}
