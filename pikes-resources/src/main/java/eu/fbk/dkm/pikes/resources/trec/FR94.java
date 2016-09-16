package eu.fbk.dkm.pikes.resources.trec;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.joox.JOOX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 27/11/15.
 *
 * Warning: some files are too big for the JDK
 * - FR941202.2
 *
 * Solution: pass -DentityExpansionLimit=0 to the Java command
 */

public class FR94 {

    private static final Logger LOGGER = LoggerFactory.getLogger(FR94.class);
    private static String DEFAULT_URL = "http://document/%s";

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("fr94-extractor")
                    .withHeader("Extract FR94 documents from TREC dataset and save them in NAF format")
                    .withOption("i", "input", "Input folder", "FOLDER", CommandLine.Type.DIRECTORY_EXISTING, true,
                            false, true)
                    .withOption("o", "output", "Output folder", "FOLDER", CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("u", "url-template", "URL template (with %d for the ID)", "URL",
                            CommandLine.Type.STRING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            File inputDir = cmd.getOptionValue("input", File.class);

            String urlTemplate = DEFAULT_URL;
            if (cmd.hasOption("url-template")) {
                urlTemplate = cmd.getOptionValue("url-template", String.class);
            }

            File outputDir = cmd.getOptionValue("output", File.class);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputDir)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                String outputTemplate = outputDir.getAbsolutePath() + File.separator + file.getName();
                File newFolder = new File(outputTemplate);
                newFolder.mkdirs();

                outputTemplate += File.separator + "NAF";
                saveFile(file, outputTemplate, urlTemplate);
            }
        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }

    private static void saveFile(File inputFile, String outputFilePattern, String urlTemplate)
            throws IOException, SAXException, ParserConfigurationException {

        LOGGER.info("Input file: {}", inputFile);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE tutorials [\n");
        stringBuffer.append("<!ENTITY hyph \"-\">\n");
        stringBuffer.append("<!ENTITY blank \" \">\n");
        stringBuffer.append("<!ENTITY sect \" \">\n");
        stringBuffer.append("<!ENTITY para \" \">\n");
        stringBuffer.append("<!ENTITY cir \" \">\n");
        stringBuffer.append("<!ENTITY rsquo \" \">\n");
        stringBuffer.append("<!ENTITY mu \" \">\n");
        stringBuffer.append("<!ENTITY times \" \">\n");
        stringBuffer.append("<!ENTITY bull \" \">\n");
        stringBuffer.append("<!ENTITY ge \">=\">\n");
        stringBuffer.append("<!ENTITY reg \" \">\n");
        stringBuffer.append("<!ENTITY cent \" \">\n");
        stringBuffer.append("<!ENTITY amp \" \">\n");
        stringBuffer.append("<!ENTITY gt \">\">\n");
        stringBuffer.append("<!ENTITY lt \"<\">\n");
        stringBuffer.append("<!ENTITY acirc \"a\">\n");
        stringBuffer.append("<!ENTITY ncirc \"n\">\n");
        stringBuffer.append("<!ENTITY atilde \"a\">\n");
        stringBuffer.append("<!ENTITY ntilde \"n\">\n");
        stringBuffer.append("<!ENTITY otilde \"o\">\n");
        stringBuffer.append("<!ENTITY utilde \"u\">\n");
        stringBuffer.append("<!ENTITY aacute \"a\">\n");
        stringBuffer.append("<!ENTITY cacute \"c\">\n");
        stringBuffer.append("<!ENTITY eacute \"e\">\n");
        stringBuffer.append("<!ENTITY Eacute \"E\">\n");
        stringBuffer.append("<!ENTITY Gacute \"G\">\n");
        stringBuffer.append("<!ENTITY iacute \"i\">\n");
        stringBuffer.append("<!ENTITY lacute \"l\">\n");
        stringBuffer.append("<!ENTITY nacute \"n\">\n");
        stringBuffer.append("<!ENTITY oacute \"o\">\n");
        stringBuffer.append("<!ENTITY pacute \"p\">\n");
        stringBuffer.append("<!ENTITY racute \"r\">\n");
        stringBuffer.append("<!ENTITY sacute \"s\">\n");
        stringBuffer.append("<!ENTITY uacute \"u\">\n");
        stringBuffer.append("<!ENTITY ocirc \"o\">\n");
        stringBuffer.append("<!ENTITY auml \"a\">\n");
        stringBuffer.append("<!ENTITY euml \"e\">\n");
        stringBuffer.append("<!ENTITY Euml \"E\">\n");
        stringBuffer.append("<!ENTITY iuml \"i\">\n");
        stringBuffer.append("<!ENTITY Iuml \"I\">\n");
        stringBuffer.append("<!ENTITY Kuml \"K\">\n");
        stringBuffer.append("<!ENTITY Ouml \"O\">\n");
        stringBuffer.append("<!ENTITY ouml \"o\">\n");
        stringBuffer.append("<!ENTITY uuml \"u\">\n");
        stringBuffer.append("<!ENTITY Ccedil \"C\">\n");
        stringBuffer.append("<!ENTITY ccedil \"c\">\n");
        stringBuffer.append("<!ENTITY agrave \"a\">\n");
        stringBuffer.append("<!ENTITY Agrave \"A\">\n");
        stringBuffer.append("<!ENTITY egrave \"e\">\n");
        stringBuffer.append("<!ENTITY Egrave \"E\">\n");
        stringBuffer.append("<!ENTITY igrave \"i\">\n");
        stringBuffer.append("<!ENTITY Ograve \"O\">\n");
        stringBuffer.append("<!ENTITY ograve \"o\">\n");
        stringBuffer.append("<!ENTITY ugrave \"u\">\n");
        stringBuffer.append("]>\n");
        stringBuffer.append("<ROOT>\n");
        stringBuffer.append(Files.toString(inputFile, Charsets.UTF_8));
        stringBuffer.append("\n</ROOT>\n");

        InputStream is = new ByteArrayInputStream(stringBuffer.toString().getBytes());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);

        doc.getDocumentElement().normalize();

        int i = 0;
        for (Element element : JOOX.$(doc).find("DOC")) {
            Element docnoElement = JOOX.$(element).find("DOCNO").get(0);
            Element textElement = JOOX.$(element).find("TEXT").get(0);

            // Incrementing also in case of errors
            i++;
            File outputFile = new File(outputFilePattern + "-" + i + ".naf");

            if (textElement == null) {
                LOGGER.error("TEXT is null");
                continue;
            }

            String text = textElement.getTextContent().trim();

            String docno = "";
            if (docnoElement != null) {
                docno = docnoElement.getTextContent().trim();
            }

            if (docno.equals("")) {
                LOGGER.error("DOCNO is empty");
            }

            String url = String.format(urlTemplate, docno);

            text = text.replaceAll("([^\\n])\\n([^\\n])", "$1 $2");
            text = text.replaceAll("\\n+([a-z])", " $1");

            KAFDocument document = new KAFDocument("en", "v3");
            document.setRawText(text);

            KAFDocument.FileDesc fileDesc = document.createFileDesc();
            fileDesc.title = docno;
            KAFDocument.Public aPublic = document.createPublic();
            aPublic.uri = url;
            aPublic.publicId = docno;

            document.save(outputFile.getAbsolutePath());
        }
    }
}
