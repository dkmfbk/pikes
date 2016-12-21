package eu.fbk.dkm.pikes.resources.trec;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.joox.JOOX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marcorospocher on 10/05/16.
 */
public class Queries {


    private static final Logger LOGGER = LoggerFactory.getLogger(Queries.class);
    private static String DEFAULT_URL = "http://trec/query/";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
    private static Pattern datePattern = Pattern.compile("^([a-zA-Z]+\\s+[0-9]+,\\s+[0-9]+)");

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("queries-extractor")
                    .withHeader("Extract Queries documents from TREC dataset and save them in NAF format")
                    .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE, true,
                            false, true)
                    .withOption("o", "output", "Output folder", "FOLDER", CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("u", "url-template", "URL template (with %d for the ID)", "URL",
                            CommandLine.Type.STRING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            File inputfile = cmd.getOptionValue("input", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);

            String urlTemplate = DEFAULT_URL;
            if (cmd.hasOption("url-template")) {
                urlTemplate = cmd.getOptionValue("url-template", String.class);
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            LOGGER.info(inputfile.getName());

            String content = FileUtils.readFileToString(inputfile, Charsets.UTF_8);

            StringBuffer newContent = new StringBuffer();
            newContent.append("<root>\n");
            newContent.append(content
                    .replaceAll("<title>", "</num>\n<title>")
                    .replaceAll("<desc>", "</title>\n<desc>")
                    .replaceAll("<narr>", "</desc>\n<narr>")
                    .replaceAll("</top>", "</narr>\n</top>")
                    .replaceAll("R&D", "R&amp;D")
            );
            newContent.append("</root>\n");

            Document doc = dBuilder.parse(new ByteArrayInputStream(newContent.toString().getBytes(Charsets.UTF_8)));
            for (Element element : JOOX.$(doc).find("top")) {
                Element numElement = JOOX.$(element).find("num").get(0);
                Element titleElement = JOOX.$(element).find("title").get(0);
                Element descElement = JOOX.$(element).find("desc").get(0);

                String number = "q" + numElement.getTextContent().trim().substring(7).trim();
                //String title = titleElement.getTextContent().trim().replaceAll("\\s+", " ");
                String title = titleElement.getTextContent().trim().replaceAll("\\s+", " ");
                String desc = descElement.getTextContent().trim().substring(12).trim().replaceAll("\\s+", " ");

                saveFile(outputFolder.getAbsolutePath() + "/keyword/" + number + ".naf", title, number, urlTemplate);
                saveFile(outputFolder.getAbsolutePath() + "/desc/" + number + ".naf", desc, number, urlTemplate);
                saveFile(outputFolder.getAbsolutePath() + "/keyword_desc/" + number + ".naf", title+"\n\n"+desc, number, urlTemplate);

            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }


    private static void saveFile(String outputFilename, String raw, String id, String url_template)
            throws IOException, SAXException, ParserConfigurationException {

        File file = new File(outputFilename);
        file.getParentFile().mkdirs();

        File outputFile = new File(outputFilename);

        KAFDocument document = new KAFDocument("en", "v3");
        document.setRawText(raw);

        KAFDocument.FileDesc fileDesc = document.createFileDesc();
        fileDesc.title = id;

        KAFDocument.Public aPublic = document.createPublic();
        aPublic.uri = url_template+id;
        aPublic.publicId = id;

        document.save(outputFile.getAbsolutePath());

    }


}
