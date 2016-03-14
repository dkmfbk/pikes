package eu.fbk.dkm.pikes.resources.ecb;

import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marcorospocher on 12/03/16.
 */
public class ECBparser {



    private static final Logger LOGGER = LoggerFactory.getLogger(ECBparser.class);
    private static final String DEFAULT_PREFIX = "http://dkm.fbk.eu/pikes/dataset/ecb/";

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./taol-extractor")
                    .withHeader("Convert file from ecb annotated txt to NAF")
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

            // uncomment to get the manual mention spans
            //Pattern MY_PATTERN = Pattern.compile("\\\">[^<]*</MENTION>");

            String tags;

            int i = 0;
            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                String url = prefix+file.getName();
                String id = "" + i;
                String title = "";

                String content = FileUtils.readFileToString(file, "utf-8");

                // uncomment to get the manual mention spans
                //Matcher m = MY_PATTERN.matcher(content);
                //while (m.find()) System.out.println(m.group(0).replace("\">","").replace("</MENTION>",""));

                content=content.replaceAll("\\<[^>]*>","");

                File outputFile = new File(
                        outputFolder.getAbsolutePath() + File.separator +
                                file.getAbsolutePath().substring(
                                        inputFolder.getAbsolutePath().length()).replace(".ecb",".naf"));
                Files.createParentDirs(outputFile);

                KAFDocument document = new KAFDocument("en", "v3");

                KAFDocument.Public documentPublic = document.createPublic();
                documentPublic.uri = url;
                documentPublic.publicId = id;

                KAFDocument.FileDesc documentFileDesc = document.createFileDesc();
                documentFileDesc.filename = file.getName();
                documentFileDesc.title = title;

                StringBuffer finalContent = new StringBuffer();

                document.setRawText(content);

                document.save(outputFile.getAbsolutePath());


            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }

}
