package eu.fbk.dkm.pikes;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import eu.fbk.dkm.pikes.rdf.naf.NAFExtractor;
import eu.fbk.dkm.pikes.rdf.vocab.*;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NewRDFGeneratorTest {


    final static String DEFAULT_PATH_INPUT = "/Users/marcorospocher/Downloads/pikes-kem-ud/input-naf";
    final static String DEFAULT_PATH_OUTPUT = "/Users/marcorospocher/Downloads/pikes-kem-ud/output-rdf";

    public static void main(final String... args) {


        final CommandLine cmd = CommandLine
                .parser()
                .withName("stripNAF")
                .withHeader("Strip NAF files of unnecessary layers")
                .withOption("i", "input",
                        String.format("input folder (default %s)", DEFAULT_PATH_INPUT), "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
                .withOption("o", "output",
                        String.format("output folder (default %s)", DEFAULT_PATH_OUTPUT), "FOLDER",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
                .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);


        // Input/output
        File inputFolder = new File(DEFAULT_PATH_INPUT);
        if (cmd.hasOption("input")) {
            inputFolder = cmd.getOptionValue("input", File.class);
        }

        File outputFolder = new File(DEFAULT_PATH_OUTPUT);
        if (cmd.hasOption("output")) {
            inputFolder = cmd.getOptionValue("output", File.class);
        }


        for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
            if (!file.isFile()) {
                continue;
            }
            if (file.getName().startsWith(".")) {
                continue;
            }

            if ((!file.getName().endsWith(".naf.gz")) && (!file.getName().endsWith(".naf"))) {
                continue;
            }

            //System.out.print("Processing: "+file.getAbsoluteFile().toString());
            String outputFileName=file.getAbsoluteFile().toString().replace(inputFolder.getAbsolutePath(), outputFolder.getAbsolutePath())+".trig";
            File outputFile = new File(outputFileName);


            if (!outputFile.exists()) {

                try (Reader reader = IO.utf8Reader(IO.buffer(IO.read(file.getAbsoluteFile().toString())))) {
                    try {

                   //System.out.print(" WORKING");
//                        Reading NAF
                        final KAFDocument document = KAFDocument.createFromStream(reader);
                        reader.close();

                        final Model model = new LinkedHashModel();

                        NAFExtractor extractor= NAFExtractor.builder().build();

                        extractor.generate(document,model,null);

                        OutputStream outputstream = IO.buffer(IO.write(outputFile.getAbsolutePath()));

                        writeGraph(model, outputstream, outputFileName);


                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            namespaces.add(KS.NS);
            namespaces.add(NIF.NS);
            namespaces.add(DCTERMS.NS);
            namespaces.add(OWLTIME.NS);
            namespaces.add(XMLSchema.NS);
            namespaces.add(OWL.NS); // not strictly necessary
            namespaces.add(RDF.NS); // not strictly necessary
            namespaces.add(RDFS.NS);
            namespaces.add(KEM.NS);
            namespaces.add(KEMT.NS);
            namespaces.add(ITSRDF.NS);
            namespaces.add(new SimpleNamespace("dbpedia", "http://dbpedia.org/resource/"));
            namespaces.add(new SimpleNamespace("wn30", "http://wordnet-rdf.princeton.edu/wn30/"));
            namespaces.add(new SimpleNamespace("sst", "http://pikes.fbk.eu/wn/sst/"));
            namespaces.add(new SimpleNamespace("bbn", "http://pikes.fbk.eu/bbn/"));
            namespaces.add(new SimpleNamespace("pm", "http://premon.fbk.eu/resource/"));
            namespaces.add(new SimpleNamespace("ili", "http://sli.uvigo.gal/rdf_galnet/"));
            namespaces.add(new SimpleNamespace("ner", "http://pikes.fbk.eu/ner/"));
            namespaces.add(new SimpleNamespace("olia-penn-pos","http://purl.org/olia/penn.owl#"));
            namespaces.add(new SimpleNamespace("olia-ud-pos","http://fginter.github.io/docs/u/pos/all.html#"));

            //add missing namespace http://premon.fbk.eu/resource/,  http://pikes.fbk.eu/ner/ http://lexvo.org/id/iso639-3/
            RDFSources.wrap(stmts, namespaces).emit(writer, 1);
        } catch (final Throwable ex) {
            throw new IOException(ex);
        }
    }

}




