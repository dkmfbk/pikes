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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by alessio on 27/11/15.
 *
 * Warning: inconsistencies
 * - FB496073 (lines 5212, 5366)
 * - FB496111 (line 21480)
 * - FB496246 (line 9252)
 */

public class FBIS {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBIS.class);
    private static String DEFAULT_URL = "http://document/%s";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static DateFormat format = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("fbis-extractor")
                    .withHeader("Extract FBIS documents from TREC dataset and save them in NAF format")
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
        stringBuffer.append("<!ENTITY amp \" \">\n");
        stringBuffer.append("<!ENTITY gt \" \">\n");
        stringBuffer.append("<!ENTITY lt \" \">\n");
        stringBuffer.append("<!ENTITY AElig \"A\">\n");
        stringBuffer.append("<!ENTITY ap \" \">\n");
        stringBuffer.append("<!ENTITY deg \" \">\n");
        stringBuffer.append("<!ENTITY egrave \"e\">\n");
        stringBuffer.append("<!ENTITY eacute \"e\">\n");
        stringBuffer.append("<!ENTITY oacute \"o\">\n");
        stringBuffer.append("<!ENTITY ubreve \"u\">\n");
        stringBuffer.append("<!ENTITY Ubreve \"U\">\n");
        stringBuffer.append("<!ENTITY egs \" \">\n");
        stringBuffer.append("<!ENTITY els \" \">\n");
        stringBuffer.append("<!ENTITY percnt \" \">\n");
        stringBuffer.append("<!ENTITY pound \"£\">\n");
        stringBuffer.append("<!ENTITY yen \"¥\">\n");
        stringBuffer.append("<!ENTITY agr \"\">\n");
        stringBuffer.append("<!ENTITY bgr \"\">\n");
        stringBuffer.append("<!ENTITY dgr \"\">\n");
        stringBuffer.append("<!ENTITY egr \"\">\n");
        stringBuffer.append("<!ENTITY ggr \"\">\n");
        stringBuffer.append("<!ENTITY Ggr \"\">\n");
        stringBuffer.append("<!ENTITY kgr \"\">\n");
        stringBuffer.append("<!ENTITY lgr \"\">\n");
        stringBuffer.append("<!ENTITY mgr \"\">\n");
        stringBuffer.append("<!ENTITY pgr \"\">\n");
        stringBuffer.append("<!ENTITY rgr \"\">\n");
        stringBuffer.append("<!ENTITY sgr \"\">\n");
        stringBuffer.append("<!ENTITY tgr \"\">\n");
        stringBuffer.append("<!ENTITY xgr \"\">\n");
        stringBuffer.append("<!ENTITY zgr \"\">\n");
        stringBuffer.append("<!ENTITY eegr \"\">\n");
        stringBuffer.append("<!ENTITY khgr \"\">\n");
        stringBuffer.append("<!ENTITY phgr \"\">\n");
        stringBuffer.append("<!ENTITY thgr \"\">\n");
        stringBuffer.append("<!ENTITY ohm \"\">\n");
        stringBuffer.append("<!ENTITY Bgr \"\">\n");
        stringBuffer.append("<!ENTITY Ngr \"\">\n");
        stringBuffer.append("<!ENTITY EEgr \"\">\n");
        stringBuffer.append("<!ENTITY OHgr \"\">\n");
        stringBuffer.append("<!ENTITY PSgr \"\">\n");
        stringBuffer.append("<!ENTITY Omacr \"\">\n");
        stringBuffer.append("]>\n");
        stringBuffer.append("<ROOT>\n");
        stringBuffer.append(Files.toString(inputFile, Charsets.UTF_8)
                .replaceAll("<F P=[0-9]+>", "<F>")
                .replaceAll("<FIG ID=[^>]+>", "<FIG>")
                .replaceAll("</?3>", "")
        );
        stringBuffer.append("\n</ROOT>\n");

        InputStream is = new ByteArrayInputStream(stringBuffer.toString().getBytes());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);

        doc.getDocumentElement().normalize();

        int i = 0;
        for (Element element : JOOX.$(doc).find("DOC")) {
            Element docnoElement = JOOX.$(element).find("DOCNO").get(0);
            Element dateElement = JOOX.$(element).find("DATE1").get(0);
            Element headlineElement = JOOX.$(element).find("TI").get(0);
            Element textElement = JOOX.$(element).find("TEXT").get(0);

            // Incrementing also in case of errors
            i++;
            File outputFile = new File(outputFilePattern + "-" + i + ".naf");

            if (textElement == null) {
                LOGGER.error("TEXT is null");
                continue;
            }

            String text = JOOX.$(element).find("TEXT").content();
            if (text.length() == 0) {
                LOGGER.error("TEXT is empty");
                continue;
            }

            String docno = "";
            if (docnoElement != null) {
                docno = docnoElement.getTextContent().trim();
            }

            String date = "";
            if (dateElement != null) {
                date = dateElement.getTextContent().trim();
            }

            String headline = "";
            if (headlineElement != null) {
                headline = headlineElement.getTextContent().trim();
            }

            if (docno.equals("")) {
                LOGGER.error("DOCNO is empty");
            }

            String url = String.format(urlTemplate, docno);

            headline = headline.replace('\n', ' ');
            headline = headline.replaceAll("\\s+", " ");
            text = text.replace('\n', ' ');
            text = text.replaceAll("\\s+", " ");

            Date thisDate = null;
            try {
                thisDate = format.parse(date);
            } catch (Exception e) {
                // ignored
            }

            text = headline + "\n\n" + text;

            KAFDocument document = new KAFDocument("en", "v3");
            document.setRawText(text);

            KAFDocument.FileDesc fileDesc = document.createFileDesc();
            fileDesc.title = headline;
            if (thisDate != null) {
                fileDesc.creationtime = sdf.format(thisDate);
            }
            KAFDocument.Public aPublic = document.createPublic();
            aPublic.uri = url;
            aPublic.publicId = docno;

            document.save(outputFile.getAbsolutePath());
        }
    }
}
