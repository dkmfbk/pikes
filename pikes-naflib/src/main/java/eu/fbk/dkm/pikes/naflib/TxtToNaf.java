package eu.fbk.dkm.pikes.naflib;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by alessio on 17/12/15.
 */

public class TxtToNaf {

    private static final Logger LOGGER = LoggerFactory.getLogger(TxtToNaf.class);
    private static final String DEFAULT_PREFIX = "http://unknown/";
    private static STRATEGY DEFAULT_STRATEGY = STRATEGY.FILENAME;

    private enum STRATEGY {FILENAME, FIRSTLINE}

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./nafizer")
                    .withHeader("Convert list of TXT files to NAF")
                    .withOption("i", "input", "Input folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("p", "prefix", String.format("Prefix (default %s)", DEFAULT_PREFIX), "PREFIX",
                            CommandLine.Type.STRING, true, false, false)
                    .withOption("t", "title-strategy", String.format("Title strategy (default: %s)", DEFAULT_STRATEGY),
                            "strategy",
                            CommandLine.Type.STRING, true, false, false)
                    .withOption(null, "trim", "Trim text")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);
            String prefix = cmd.getOptionValue("prefix", String.class, DEFAULT_PREFIX);

            boolean trimText = cmd.hasOption("trim");

            STRATEGY strategy;

            try {
                strategy = STRATEGY.valueOf(cmd.getOptionValue("title-strategy", String.class));
            } catch (Exception e) {
                strategy = STRATEGY.FILENAME;
            }

            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            int i = 0;
            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }
                if (!file.getName().endsWith(".txt")) {
                    continue;
                }

                String fileContent = Files.toString(file, Charsets.UTF_8);
                if (trimText) {
                    fileContent = fileContent.trim();
                }

                if (fileContent == null || fileContent.length() == 0) {
                    continue;
                }

                i++;

                File outputFile = new File(
                        outputFolder.getAbsolutePath() + File.separator +
                                file.getAbsolutePath().substring(inputFolder.getAbsolutePath().length()) + ".naf");
                Files.createParentDirs(outputFile);

                String title = null;
                switch (strategy) {
                case FILENAME:
                    title = file.getName();
                    break;
                case FIRSTLINE:
                    String[] parts = fileContent.split("\n");
                    title = parts[0].trim();
                    break;
                }

                KAFDocument document = new KAFDocument("en", "v3");

                KAFDocument.Public documentPublic = document.createPublic();
                documentPublic.uri = prefix + i;
                documentPublic.publicId = "" + i;

                KAFDocument.FileDesc documentFileDesc = document.createFileDesc();
                documentFileDesc.filename = file.getName();
                documentFileDesc.title = title;

                document.setRawText(fileContent);
                LOGGER.info(outputFile.getAbsolutePath());
                document.save(outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
