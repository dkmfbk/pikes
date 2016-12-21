package eu.fbk.dkm.pikes.resources.ecb;

import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.IO;
import ixa.kaflib.KAFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 21/09/16.
 */

public class ConvertECBPlus {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertECBPlus.class);
    private static Pattern folderPattern = Pattern.compile("^([0-9]+)");

    public static void main(String[] args) {
        final CommandLine cmd = CommandLine
                .parser()
                .withName("convert-ecb-plus")
                .withHeader("Convert ECB+ files to NAF")
                .withOption("i", "input-path", "the base path of the corpus", "DIR",
                        CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("o", "output-path", "output NAF folder", "DIR",
                        CommandLine.Type.DIRECTORY, true, false, true)
                .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

        final File inputPath = cmd.getOptionValue("i", File.class);
        final File outputPath = cmd.getOptionValue("o", File.class);

        boolean opMkDirs = outputPath.mkdirs();
        if (!opMkDirs) {
            LOGGER.error("Unable to create folder {}", outputPath.getAbsolutePath());
        }

        File[] files = inputPath.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }

            File[] thisFolderFiles = file.listFiles();
            for (File nafFile : thisFolderFiles) {
                if (!nafFile.isFile()) {
                    continue;
                }
                if (!nafFile.getAbsolutePath().endsWith(".xml")) {
                    continue;
                }

                String relativeFilePath = nafFile.getAbsolutePath().substring(inputPath.getAbsolutePath().length());
                if (relativeFilePath.startsWith(File.separator)) {
                    relativeFilePath = relativeFilePath.substring(1);
                }

                try {
                    KAFDocument document = new KAFDocument("en", "FBK");

                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    XPathFactory xPathfactory = XPathFactory.newInstance();
                    XPath xpath = xPathfactory.newXPath();

                    XPathExpression expr;
                    NodeList nl;

                    Document doc = dBuilder.parse(IO.read(nafFile.getAbsolutePath()));
                    doc.getDocumentElement().normalize();

                    // Normalization rules
                    expr = xpath.compile("/Document/token");
                    nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

                    StringBuffer buffer = new StringBuffer();
                    StringBuffer text = new StringBuffer();
                    int lastSent = 0;
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node item = nl.item(i);
                        Element element = (Element) item;

                        int sentence = Integer.parseInt(element.getAttribute("sentence"));
                        if (relativeFilePath.contains("ecbplus") && sentence == 0) {
                            continue;
                        }
                        if (sentence != lastSent) {
                            if (buffer.length() > 0) {
                                text.append(buffer.toString().trim()).append("\n");
                            }
                            buffer = new StringBuffer();
                            lastSent = sentence;
                        }

                        buffer.append(element.getTextContent()).append(" ");
                    }
                    if (buffer.length() > 0) {
                        text.append(buffer.toString().trim()).append("\n");
                    }

                    document.setRawText(text.toString().trim());
                    KAFDocument.Public aPublic = document.createPublic();
                    aPublic.uri = "http://ecbplus/" + relativeFilePath;
                    aPublic.publicId = relativeFilePath;
                    KAFDocument.FileDesc fileDesc = document.createFileDesc();
                    fileDesc.title = "";

                    Matcher matcher = folderPattern.matcher(relativeFilePath);
                    if (matcher.find()) {
                        String folderID = matcher.group(1);
                        File newFolder = new File(outputPath + File.separator + folderID);
                        newFolder.mkdirs();
                    }

                    File outputFile = new File(outputPath + File.separator + relativeFilePath + ".naf");
                    document.save(outputFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }
}
