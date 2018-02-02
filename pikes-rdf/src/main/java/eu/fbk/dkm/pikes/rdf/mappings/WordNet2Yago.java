package eu.fbk.dkm.pikes.rdf.mappings;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.fbk.dkm.pikes.rdf.naf.NAFExtractorUD;
import eu.fbk.dkm.pikes.rdf.vocab.*;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.dkm.pikes.resources.YagoTaxonomy;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;

import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.core.CommandLine;
import net.didion.jwnl.data.Synset;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.model.Model;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WordNet2Yago {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordNet2Yago.class);

    private static final IRI mappingProperty = SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/lemon/ontolex#", "isConceptOf");

    public static final String WN_NAMESPACE = "http://wordnet-rdf.princeton.edu/wn30/";

    private static String DEFAULT_PATH_INPUT = "/Users/marcorospocher/CodeRepository/pikes/wordnet";

    public static void main(final String... args) throws IOException {


        final CommandLine cmd = CommandLine
                .parser()
                .withName("WordNet2Yago")
                .withHeader("Generates mappings from WN to DBYAGO")
                .withOption("i", "input",
                        String.format("input folder"), "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
                .withOption("o", "output",
                        String.format("output file"), "FILE",
                        CommandLine.Type.FILE, true, false, false)
                .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);


        String WN_path = cmd.getOptionValue("input", String.class);
        LOGGER.info("Loading WordNet...");
        WordNet.setPath(WN_path);
        WordNet.init();
        LOGGER.info("Done Loading WordNet.");

        File outputFile  = cmd.getOptionValue("output", File.class);

        final ValueFactory vf=Statements.VALUE_FACTORY;
        final Model model =  new LinkedHashModel();;

        Iterator<Synset> nounIterator = WordNet.getNounSynsets();
        generateMappings(nounIterator, model, vf,WordNet.POS_NOUN);

        Iterator<Synset> adjIterator = WordNet.getADJSynsets();
        generateMappings(adjIterator, model, vf,WordNet.POS_ADJECTIVE);

        Iterator<Synset> advIterator = WordNet.getADVSynsets();
        generateMappings(advIterator, model, vf,WordNet.POS_ADVERB);

        Iterator<Synset> verbIterator = WordNet.getVerbSynsets();
        generateMappings(verbIterator, model, vf,WordNet.POS_VERB);

        OutputStream outputstream = IO.buffer(IO.write(outputFile.getAbsolutePath()));
        writeGraph(model,outputstream,outputFile.getAbsolutePath().toString());

    }

    private static void generateMappings(Iterator<Synset> nounIterator, Model model, ValueFactory vf, String POS) {
//        int i=0;
//        while(nounIterator.hasNext()&&i<10) {
        while(nounIterator.hasNext()) {
            Synset syn = nounIterator.next();
//            System.out.println(syn.toString());
            String synsetID = WordNet.getSynsetID(syn.getOffset(),POS);
//            System.out.println(synsetID);
            IRI yago = YagoTaxonomy.getDBpediaYagoIRI(synsetID);
            if (yago!=null) {
//                System.out.println(yago.toString());
                model.add(vf.createStatement(vf.createIRI(WN_NAMESPACE,synsetID), mappingProperty, yago));
                LOGGER.debug("Added Yago mapping wn:" + synsetID + " -> dbyago:"
                        + yago.getLocalName());
            }
        }
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
            namespaces.add(new SimpleNamespace("dbyago", YagoTaxonomy.NAMESPACE));
            namespaces.add(new SimpleNamespace("ontolex", "http://www.w3.org/ns/lemon/ontolex#"));
            RDFSources.wrap(stmts, namespaces).emit(writer, 1);
        } catch (final Throwable ex) {
            throw new IOException(ex);
        }
    }

}
