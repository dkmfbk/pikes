package eu.fbk.dkm.pikes.resources.treccani;

import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.lang.StringEscapeUtils;
import org.joox.JOOX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Created by alessio on 17/12/15.
 */

public class TAOL {

    private static final Logger LOGGER = LoggerFactory.getLogger(TAOL.class);
    private static final String DEFAULT_PREFIX = "opencms://system/modules/com.atosorigin.treccani.bancadati.xml";

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
                i++;

                for (Element element : JOOX.$(doc).get()) {
                    if (!element.getTagName().equals("ARTICOLI")) {
                        continue;
                    }

                    for (Element articolo : JOOX.$(element).find("ARTICOLO")) {
                        String language = articolo.getAttribute("language");
                        String thisPrefix = prefix + "/taol/" + language + "/";

                        String url = thisPrefix + i;
                        String id = "" + i;
                        String title = "";

                        Element cidaElement = JOOX.$(articolo).find("CIDA").get(0);
                        if (cidaElement != null) {
                            String cida = cidaElement.getTextContent().trim().replaceAll("\\s+", "");
                            url = thisPrefix + cida;
                            id = cida;
                        }

                        Element ctitElement = JOOX.$(articolo).find("CTIT").get(0);
                        if (ctitElement != null) {
                            title = ctitElement.getTextContent().trim().replaceAll("\\s+", "");
                        }

                        Element contentElement = JOOX.$(articolo).find("content").get(0);
                        if (contentElement != null) {
                            File outputFile = new File(
                                    outputFolder.getAbsolutePath() + File.separator + language + File.separator +
                                            file.getAbsolutePath().substring(
                                                    inputFolder.getAbsolutePath().length()));
                            Files.createParentDirs(outputFile);

                            KAFDocument document = new KAFDocument(language, "v3");

                            KAFDocument.Public documentPublic = document.createPublic();
                            documentPublic.uri = url;
                            documentPublic.publicId = id;

                            KAFDocument.FileDesc documentFileDesc = document.createFileDesc();
                            documentFileDesc.filename = file.getName();
                            documentFileDesc.title = title;

                            String content = contentElement.getTextContent();
                            content = content.replaceAll("<br />", "\n");
                            content = content.replaceAll(" +", " ");
                            content = content.replaceAll("<[^>]+>", "");

                            StringBuffer finalContent = new StringBuffer();

                            String[] lines = content.split(System.getProperty("line.separator"));
                            for (String line : lines) {
                                line = line.trim();
                                if (!line.matches(".*[.?!]+$") && line.length() != 0) {
                                    line = line + ".";
                                }
                                if (line.startsWith("H1.") || line.startsWith("H2.")) {
                                    line = line.substring(3).trim();
                                }

                                finalContent.append(line).append("\n");
                            }

                            String text = StringEscapeUtils.unescapeHtml(finalContent.toString());

                            document.setRawText(text);

                            document.save(outputFile.getAbsolutePath());
                        }
                    }
                }
            }

//            String serverUrl = cmd.getOptionValue("server", String.class);
//            File inputFile = cmd.getOptionValue("input", File.class);
//
//            URL url = new URL(serverUrl);
//            TintopServer server = new TintopServer(url);
//            TintopClient client = new TintopClient(server);
//
//            String whole = FileUtils.readFileToString(inputFile);
//            System.out.println(client.call(whole));
        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }
}
