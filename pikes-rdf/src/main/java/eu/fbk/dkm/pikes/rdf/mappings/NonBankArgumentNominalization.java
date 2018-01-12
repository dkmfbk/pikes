package eu.fbk.dkm.pikes.rdf.mappings;

import com.google.common.base.Charsets;
import com.google.common.collect.*;
import eu.fbk.dkm.pikes.resources.NAFUtilsUD;
import eu.fbk.dkm.pikes.resources.Sumo;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.core.CommandLine;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NonBankArgumentNominalization {

    private static final IRI argumentNominalization = SimpleValueFactory.getInstance().createIRI("http://pikes.fbk.eu/mappings#", "ArgumentNominalization");

    private static final Logger LOGGER = LoggerFactory.getLogger(NonBankArgumentNominalization.class);

    public static void main(final String... args) throws IOException {


        final CommandLine cmd = CommandLine
                .parser()
                .withName("NonBankArgumentNominalization")
                .withHeader("Generates the argument nominalization files from the NomBank TSV file (obtained running eu.fbk.dkm.pikes.resources.NomBank)")
                .withOption("i", "input",
                        String.format("input TSV file"), "FILE",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("o", "output",
                        String.format("output file"), "FILE",
                        CommandLine.Type.FILE, true, false, true)
                .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

        String nombankTsvFile = cmd.getOptionValue("input", String.class);
        File outputFile  = cmd.getOptionValue("output", File.class);

        final ValueFactory vf= Statements.VALUE_FACTORY;
        final Model model =  new LinkedHashModel();

        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(nombankTsvFile), Charsets.UTF_8))) {

            String line;
            int counter =0;
            while ((line = br.readLine()) != null) {
                LOGGER.debug("Processing line " + line);
                final String[] tokens = line.split("\t");
                final String id = tokens[0];
                final List<Integer> mandatoryArgs = Lists.newArrayList();
                if (tokens.length > 24 && !tokens[24].equals("")) {
                    for (final String arg : Ordering.natural().sortedCopy(
                            Arrays.asList(tokens[24].split("\\s+")))) {
                        mandatoryArgs.add(Integer.parseInt(arg));
                    }
                }
                if (!mandatoryArgs.isEmpty()) {
                    //argument nominalization
                    counter++;
                    IRI IRI = NAFUtilsUD.createPreMOnSemanticClassIRIfor(NAFUtilsUD.RESOURCE_NOMBANK,id);
                    model.add(IRI, RDF.TYPE, argumentNominalization);
                    LOGGER.debug("Added argument nominalization for predicate " + IRI.getLocalName());

                }
            }
            System.out.println("Created "+ counter +" argument nominalization triples");
        }

        OutputStream outputstream = IO.buffer(IO.write(outputFile.getAbsolutePath()));
        writeGraph(model,outputstream,outputFile.getAbsolutePath().toString());

    }


    private static void writeGraph(final Model graph, final OutputStream stream,
                                   final String fileName) throws IOException {

        final RDFFormat rdfFormat = Rio.getWriterFormatForFileName(fileName).get();
        if (rdfFormat == null) {
            throw new IOException("Unsupported RDF format for " + fileName);
        }

        try {
            final RDFWriter writer = Rio.createWriter(rdfFormat, stream);
            final List<Statement> stmts = Lists.newArrayList(graph);
            Collections.sort(stmts, Statements.statementComparator("spoc", //
                    Statements.valueComparator(RDF.NAMESPACE)));
            final Set<Namespace> namespaces = Sets.newLinkedHashSet(graph.getNamespaces());
            namespaces.add(new SimpleNamespace("wn30", "http://wordnet-rdf.princeton.edu/wn30/"));
            namespaces.add(new SimpleNamespace("sumo", Sumo.SUMO_NAMESPACE));
            namespaces.add(new SimpleNamespace("ontolex", "http://www.w3.org/ns/lemon/ontolex#"));
            RDFSources.wrap(stmts, namespaces).emit(writer, 1);
        } catch (final Throwable ex) {
            throw new IOException(ex);
        }
    }

}
