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
 */

public class FT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FT.class);
    private static String DEFAULT_URL = "http://document/%s";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static Pattern TITLE_PATTERN = Pattern.compile("FT [A-Za-z0-9- ]+ / (\\([^\\(\\)]*\\))?(.*)");

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("ft-extractor")
                    .withHeader("Extract FT documents from TREC dataset and save them in NAF format")
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
            Element dateElement = JOOX.$(element).find("DATE").get(0);
            Element headlineElement = JOOX.$(element).find("HEADLINE").get(0);
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

            Matcher matcher = TITLE_PATTERN.matcher(headline);
            if (matcher.find()) {
                headline = matcher.group(2).trim();
            }

            Calendar.Builder builder = new Calendar.Builder();
            try {
                builder.setDate(1900 + Integer.parseInt(date.substring(0, 2)), Integer.parseInt(date.substring(2, 4)),
                        Integer.parseInt(date.substring(4)));
            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage());
            }
            Calendar calendar = builder.build();

            text = headline + "\n\n" + text;

            KAFDocument document = new KAFDocument("en", "v3");
            document.setRawText(text);

            KAFDocument.FileDesc fileDesc = document.createFileDesc();
            fileDesc.title = headline;
            fileDesc.creationtime = sdf.format(calendar.getTime());
            KAFDocument.Public aPublic = document.createPublic();
            aPublic.uri = url;
            aPublic.publicId = docno;

            document.save(outputFile.getAbsolutePath());
        }
    }
}
