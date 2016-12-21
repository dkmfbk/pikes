package eu.fbk.dkm.pikes.resources.tempeval;

import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.joox.JOOX;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Created by alessio on 05/02/16.
 */

public class TMLtoNAF {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMLtoNAF.class);
    private static final String DEFAULT_PREFIX = "http://tempeval3/";

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./taol-extractor")
                    .withHeader("Convert file from Treccani XML to NAF")
                    .withOption("i", "input", "Input folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("p", "prefix", String.format("Prefix (default %s)", DEFAULT_PREFIX), "PREFIX",
                            CommandLine.Type.STRING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);
            String prefix = cmd.getOptionValue("prefix", String.class, DEFAULT_PREFIX);

            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            int i = 0;
            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                String docID = null;
                Match docidElements = JOOX.$(doc).find("DOCID");
                for (Element docidElement : docidElements) {
                    docID = docidElement.getTextContent().trim();
                }

                if (docID == null) {
                    LOGGER.error("DOCID is null");
                    continue;
                }

                String url = prefix + docID;

                String thisTimex = null;
                Match docTimeElements = JOOX.$(doc).find("DCT").find("TIMEX3");

                for (Element docTimeElement : docTimeElements) {
                    String function = docTimeElement.getAttribute("functionInDocument");
                    if (function == null) {
                        continue;
                    }
                    if (!function.equals("CREATION_TIME")) {
                        continue;
                    }

                    thisTimex = docTimeElement.getAttribute("value");
                }

                if (thisTimex == null) {
                    LOGGER.error("TIMEX3 is null");
                    continue;
                }

                String text = null;
                Match textElements = JOOX.$(doc).find("TEXT");

                for (Element textElement : textElements) {
                    text = textElement.getTextContent();
                }

                if (text == null) {
                    LOGGER.error("TEXT is null");
                    continue;
                }

                String fileName = outputFolder.getAbsolutePath() + File.separator + file.getAbsolutePath()
                        .substring(inputFolder.getAbsolutePath().length());
                if (!fileName.endsWith("naf")) {
                    fileName += ".naf";
                }
                File outputFile = new File(fileName);
                Files.createParentDirs(outputFile);

                KAFDocument document = new KAFDocument("en", "v3");

                KAFDocument.Public documentPublic = document.createPublic();
                documentPublic.uri = url;
                documentPublic.publicId = docID;

                KAFDocument.FileDesc documentFileDesc = document.createFileDesc();
                documentFileDesc.filename = file.getName();
                documentFileDesc.title = docID;

                document.setRawText(text);

                document.save(outputFile);
            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
