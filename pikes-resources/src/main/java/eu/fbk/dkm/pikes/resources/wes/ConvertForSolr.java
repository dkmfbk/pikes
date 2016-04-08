package eu.fbk.dkm.pikes.resources.wes;

import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 06/12/15.
 */

public class ConvertForSolr {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertForSolr.class);
    private static Pattern wesFilePattern = Pattern.compile("wes2015\\.d[0-9]+\\.naf");

    public static void main(String[] args) {

        String nafFolder = "/Users/alessio/Documents/Resources/wes/new";
        String xmlFolder = "/Users/alessio/Documents/Resources/wes/xml-no-title";
        String[] extensions = new String[] { "naf" };

        File nafFolderFile = new File(nafFolder);
        File htmlFolderFile = new File(xmlFolder);

        try {
            Iterator<File> fileIterator = FileUtils.iterateFiles(nafFolderFile, extensions, true);

            if (!htmlFolderFile.exists()) {
                htmlFolderFile.mkdirs();
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            fileIterator.forEachRemaining((File f) -> {
                File outputXml = new File(xmlFolder + File.separator + f.getName() + ".xml");

                try {

                    Matcher m = wesFilePattern.matcher(f.getName());
                    if (m.matches()) {
                        KAFDocument document = KAFDocument.createFromFile(f);
                        String title = document.getFileDesc().title;
                        String text = document.getRawText().substring(title.length() + 1).trim();
                        String id = document.getPublic().publicId;

                        Document doc = docBuilder.newDocument();
                        Element moreRootElement = doc.createElement("add");
                        Element rootElement = doc.createElement("doc");
                        doc.appendChild(moreRootElement);
                        moreRootElement.appendChild(rootElement);

                        Element idEl = doc.createElement("field");
                        idEl.setAttribute("name", "id");
                        idEl.setTextContent(id);
                        rootElement.appendChild(idEl);

                        Element titleEl = doc.createElement("field");
                        titleEl.setAttribute("name", "title");
                        titleEl.setTextContent(title);
                        rootElement.appendChild(titleEl);

                        Element textEl = doc.createElement("field");
                        textEl.setAttribute("name", "text");
                        textEl.setTextContent(text);
                        rootElement.appendChild(textEl);

                        DOMSource source = new DOMSource(doc);
                        StreamResult result = new StreamResult(outputXml);
                        transformer.transform(source, result);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
