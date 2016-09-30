package eu.fbk.dkm.pikes.resources;

import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class Txt2Naf {

    private static final Logger LOGGER = LoggerFactory.getLogger(Txt2Naf.class);
//    private static final String DEFAULT_PREFIX = "http://dkm.fbk.eu/pikes/dataset/ecb";

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./taol-extractor")
                    .withHeader("Convert file from txt to NAF")
                    .withOption("i", "input", "Input folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);

            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                String content = FileUtils.readFileToString(file, "utf-8");

                File outputFile = new File(
                        outputFolder.getAbsolutePath() + File.separator +
                                file.getAbsolutePath().substring(
                                        inputFolder.getAbsolutePath().length()).replace(".txt",".naf"));
                Files.createParentDirs(outputFile);

                KAFDocument document = new KAFDocument("en", "v3");

                KAFDocument.Public documentPublic = document.createPublic();
                documentPublic.uri = "file://" + file.getAbsolutePath();
                documentPublic.publicId = file.getName();

                KAFDocument.FileDesc documentFileDesc = document.createFileDesc();
                documentFileDesc.filename = file.getName();
                documentFileDesc.title = file.getName();
                document.setRawText(content);
                document.save(outputFile.getAbsolutePath());
            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }

}
