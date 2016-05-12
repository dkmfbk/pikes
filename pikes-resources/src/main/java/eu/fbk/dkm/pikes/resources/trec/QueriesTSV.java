package eu.fbk.dkm.pikes.resources.trec;

import com.google.common.base.Charsets;
import eu.fbk.dkm.utils.CommandLine;
import org.apache.commons.io.FileUtils;
import org.joox.JOOX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

/**
 * Created by alessio on 15/12/15.
 */

public class QueriesTSV {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueriesTSV.class);
//    private static String folder = "/Users/alessio/Documents/scripts/pikesir/test/trec/queries/";
//    private static String outputFile = "/Users/alessio/Documents/scripts/pikesir/test/trec/queries.tsv";

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("trec-queriesTSV-converter")
                    .withHeader("Convert TREC queries into TSV format")
                    .withOption("i", "input", "Input folder", "FOLDER", CommandLine.Type.DIRECTORY_EXISTING, true,
                            false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            Iterator<File> fileIterator = FileUtils.iterateFiles(inputFolder, null, true);
            while (fileIterator.hasNext()) {
                File file = fileIterator.next();

                LOGGER.info(file.getName());

                String content = FileUtils.readFileToString(file, Charsets.UTF_8);

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
                    String title = titleElement.getTextContent().trim().replaceAll("\\s+", " ");
                    String desc = descElement.getTextContent().trim().substring(12).trim().replaceAll("\\s+", " ");

                    writer.append(number).append("\t");
                    writer.append(title).append("\t");
                    writer.append(desc).append("\n");
                }
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
