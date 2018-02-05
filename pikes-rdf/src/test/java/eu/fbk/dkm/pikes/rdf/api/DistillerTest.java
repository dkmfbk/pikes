package eu.fbk.dkm.pikes.rdf.api;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.pikes.rdf.rules.RuleDistiller;
import eu.fbk.dkm.pikes.rdf.util.ModelUtil;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.Ruleset;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;

public class DistillerTest {

    public static void main(final String... args) {
        try {
            final CommandLine cmd = CommandLine.parser().withName("distiller-test")
                    .withOption("o", "output", "specifies the output file to generate", "PATH",
                            Type.FILE, true, false, true)
                    .withOption("k", "kem", "specifies the KEM file to process", "PATH",
                            Type.FILE_EXISTING, true, false, true)
                    .withOption("m", "mappings", "specifies the mapping file to use", "PATH",
                            Type.FILE_EXISTING, true, false, false)
                    .withOption("r", "rules", "specifies the rules file to use", "PATH",
                            Type.FILE_EXISTING, true, false, false)
                    .withOption("d", "delta", "emits only delta statements").withHeader("")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final Path pathKem = cmd.getOptionValue("k", Path.class);
            final Path pathMappings = cmd.getOptionValue("m", Path.class);
            final Path pathRules = cmd.getOptionValue("r", Path.class);
            final Path pathOutput = cmd.getOptionValue("o", Path.class);
            final boolean delta = cmd.hasOption("d");

            Ruleset ruleset = null;
            if (pathRules != null) {
                ruleset = Ruleset.fromRDF(pathRules.toString());
            }

            Model mappings = null;
            if (pathMappings != null) {
                mappings = new LinkedHashModel();
                RDFSources.read(false, true, null, null, null, true, pathMappings.toString())
                        .emit(RDFHandlers.wrap(mappings), 1);
            }

            final Document document = new Document();
            RDFSources.read(false, true, null, null, null, true, pathKem.toString())
                    .emit(RDFHandlers.wrap(document.getModel()), 1);

            Distiller distiller = null;
            final Writer stream = null;
            final RDFHandler writer = null;

            try {
                distiller = new RuleDistiller(ruleset, mappings);

                if (!delta) {
                    distiller.distill(document, ImmutableMap.of());
                } else {
                    final Set<Statement> originalModel = Sets.newHashSet(document.getModel());
                    distiller.distill(document, ImmutableMap.of());
                    document.getModel().removeAll(originalModel);
                }

                ModelUtil.write(document.getModel(), pathOutput.toString());

                // final List<Statement> stmts = Lists.newArrayList(document.getModel());
                // Collections.sort(stmts, Statements.statementComparator("spoc", //
                // Statements.valueComparator(RDF.NAMESPACE)));
                //
                // final Set<Namespace> namespaces = Sets
                // .newLinkedHashSet(document.getModel().getNamespaces());
                // namespaces.add(KS.NS);
                // namespaces.add(NIF.NS);
                // namespaces.add(DCTERMS.NS);
                // namespaces.add(OWLTIME.NS);
                // namespaces.add(XMLSchema.NS);
                // namespaces.add(OWL.NS); // not strictly necessary
                // namespaces.add(RDF.NS); // not strictly necessary
                // namespaces.add(RDFS.NS);
                // namespaces.add(KEM.NS);
                // namespaces.add(KEMT.NS);
                // namespaces.add(ITSRDF.NS);
                // namespaces.add(SUMO.NS);
                // namespaces.add(new SimpleNamespace("frframe", "http://framebase.org/frame/"));
                // namespaces.add(new SimpleNamespace("frfe", "http://framebase.org/fe/"));
                // namespaces.add(new SimpleNamespace("dbpedia", "http://dbpedia.org/resource/"));
                // namespaces.add(
                // new SimpleNamespace("wn30", "http://wordnet-rdf.princeton.edu/wn30/"));
                // namespaces.add(new SimpleNamespace("sst", "http://pikes.fbk.eu/wn/sst/"));
                // namespaces.add(new SimpleNamespace("bbn", "http://pikes.fbk.eu/bbn/"));
                // namespaces.add(new SimpleNamespace("pm", "http://premon.fbk.eu/resource/"));
                // namespaces.add(new SimpleNamespace("ili", "http://sli.uvigo.gal/rdf_galnet/"));
                // namespaces.add(new SimpleNamespace("ner", "http://pikes.fbk.eu/ner/"));
                // namespaces.add(
                // new SimpleNamespace("olia-penn-pos", "http://purl.org/olia/penn.owl#"));
                // namespaces.add(new SimpleNamespace("olia-ud-pos",
                // "http://fginter.github.io/docs/u/pos/all.html#"));
                //
                // if (pathOutput.toString().endsWith(".ttl")) {
                // stream = Files.newBufferedWriter(pathOutput, Charsets.UTF_8);
                // writer = PrettyTurtle.INSTANCE.getWriter(stream);
                // } else {
                // stream = null;
                // writer = RDFHandlers.write(null, 1000, pathOutput.toString());
                // }
                //
                // RDFSources.wrap(stmts, namespaces).emit(writer, 1);

            } finally {
                IO.closeQuietly(writer);
                IO.closeQuietly(stream);
                IO.closeQuietly(distiller);
            }

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

}
