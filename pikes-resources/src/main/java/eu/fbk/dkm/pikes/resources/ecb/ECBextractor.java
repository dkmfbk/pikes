package eu.fbk.dkm.pikes.resources.ecb;

import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.Coref;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.regex.Pattern;

/**
 * Created by marcorospocher on 12/03/16.
 */
public class ECBextractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ECBextractor.class);
    private static final Pattern tokenPattern = Pattern.compile("/([0-9]+)/([0-9])\\.ecb#char=([0-9]+)");
//    private static final Boolean removeAloneClusters = false;
//    private static final Pattern chainPattern = Pattern.compile("CHAIN=\"([0-9]+)\"");

    public static void main(String[] args) {
        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./ecb-extractor")
                    .withHeader("Extracts URI of events in the gold standard")
                    .withOption("n", "input-naf", "Input NAF folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE",
                            CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputNaf = cmd.getOptionValue("input-naf", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputNaf)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                String path = file.getParentFile().toString();
                Integer folder = Integer.parseInt(path.substring(path.lastIndexOf("/")).substring(1));
                Integer fileNum = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));

                LOGGER.debug(file.getAbsolutePath());
                KAFDocument document = KAFDocument.createFromFile(file);
                String uri = document.getPublic().uri;

                for (Coref coref : document.getCorefs()) {
                    if (coref.getType() == null) {
                        continue;
                    }
                    if (!coref.getType().equals("event-gold")) {
                        continue;
                    }

                    Integer cluster = Integer.parseInt(coref.getCluster());
                    String idCluster = folder + "_" + cluster;

                    for (Span<Term> termSpan : coref.getSpans()) {
                        Term term = termSpan.getTargets().get(0);

                        String thisURI =
                                uri + "#char=" + term.getOffset() + "," + (term.getOffset() + term.getLength());
                        writer.append(thisURI).append("\n");
                    }
                }
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }

}
