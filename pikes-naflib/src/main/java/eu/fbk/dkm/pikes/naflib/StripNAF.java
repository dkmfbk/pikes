package eu.fbk.dkm.pikes.naflib;

import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.rdfpro.util.IO;
import ixa.kaflib.KAFDocument;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;

/**
 * Created by marcorospocher on 19/07/16.
 */
public class StripNAF {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StripNAF.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    public enum removeLayer {
        deps, chunks, entities, properties, categories, coreferences, opinions, relations, srl, constituency, timeExpressions, linkedEntities, constituencyStrings;
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("stripNAF")
                    .withHeader("Strip NAF files of unnecessary layers")
                    .withOption("i", "input-folder", "the folder of the input NAF corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output-folder", "the folder of the input NAF corpus", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input-folder", File.class);
            File outputFolder = cmd.getOptionValue("output-folder", File.class);

            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                if (!file.getName().endsWith(".naf.gz")) {
                    continue;
                }

                //System.out.print("Processing: "+file.getAbsoluteFile().toString());
                File outputFile = new File(file.getAbsoluteFile().toString().replace(inputFolder.getAbsolutePath(),outputFolder.getAbsolutePath()));

                if (!outputFile.exists()) {

                    try (Reader reader = IO.utf8Reader(IO.buffer(IO.read(file.getAbsoluteFile().toString())))) {
                        try {

                            //System.out.print(" WORKING");

                            KAFDocument document = KAFDocument.createFromStream(reader);
                            reader.close();

                            //System.out.println("Processing: "+file.getAbsoluteFile().toString());

                            for (removeLayer layer : removeLayer.values()) {
                                document.removeLayer(KAFDocument.Layer.valueOf(layer.toString()));
                            }

                            Files.createParentDirs(outputFile);
                            try (Writer w = IO.utf8Writer(IO.buffer(IO.write(outputFile.getAbsolutePath())))) {
                                w.write(document.toString());
                                w.close();
                                //System.out.print(" SAVED");

                            }

                            System.out.println("");

                        } catch (Exception e) {

                        }

                    }
                } //else System.out.println(" SKIPPED");

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
