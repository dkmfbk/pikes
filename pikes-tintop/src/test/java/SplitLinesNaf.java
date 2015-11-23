import com.google.common.base.Charsets;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by alessio on 20/11/15.
 */

public class SplitLinesNaf {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitLinesNaf.class);
    private static String DEFAULT_TITLE = "Document %d";
    private static String DEFAULT_URL = "http://document/%d";

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("split-lines-naf")
                    .withHeader("Split TXT file in lines")
                    .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER", CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("u", "url-template", "URL template (with %d for the ID)", "URL",
                            CommandLine.Type.STRING, true, false, false)
                    .withOption("t", "title-template", "Title template (with %d for the ID)", "URL",
                            CommandLine.Type.STRING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputDir = cmd.getOptionValue("output", File.class);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            if (!outputDir.isDirectory()) {
                LOGGER.error("{} is not a directory", outputDir.getAbsolutePath());
                System.exit(1);
            }
            if (!inputFile.exists()) {
                LOGGER.error("{} does not exist", inputFile.getAbsolutePath());
                System.exit(1);
            }

            String urlTemplate = DEFAULT_URL;
            String titleTemplate = DEFAULT_TITLE;
            if (cmd.hasOption("url-template")) {
                urlTemplate = cmd.getOptionValue("url-template", String.class);
            }
            if (cmd.hasOption("title-template")) {
                titleTemplate = cmd.getOptionValue("title-template", String.class);
            }

            int i = 0;
            List<String> lines = Files.readAllLines(inputFile.toPath(), Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                i++;

                String title = String.format(titleTemplate, i);
                String url = String.format(urlTemplate, i);

                KAFDocument document = new KAFDocument("en", "v3");
                document.setRawText(line);

                KAFDocument.FileDesc fileDesc = document.createFileDesc();
                fileDesc.title = title;
                KAFDocument.Public aPublic = document.createPublic();
                aPublic.publicId = Integer.toString(i);
                aPublic.uri = url;

                document.save(outputDir.getAbsolutePath() + File.separator + Integer.toString(i) + ".naf");
            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
