package eu.fbk.dkm.pikes.rdf;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.fbk.rdfpro.util.IO;

public class SemaforExtractor {

    public static void main(final String... args) throws Throwable {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();

        for (final String arg : args) {
            final File dir = new File(arg);
            for (final File file : dir.listFiles()) {
                if (file.getName().endsWith(".out")) {
                    System.out.println("Procesing " + arg);
                    try (InputStream stream = IO.read(file.getAbsolutePath())) {
                        final Document document = builder.parse(stream);
                        process(document);
                        final DOMSource source = new DOMSource(document);
                        final StreamResult result = new StreamResult(new File(file
                                .getAbsolutePath().replace(".out", ".xml")));
                        transformer.transform(source, result);
                    }
                }
            }
        }
    }

    private static void process(final Document document) {
        final String text = document.getElementsByTagName("text").item(0).getTextContent();
        System.out.println(text);
        final NodeList list = document.getElementsByTagName("label");
        for (int i = 0; i < list.getLength(); ++i) {
            final Element element = (Element) list.item(i);
            final int start = Integer.parseInt(element.getAttribute("start"));
            final int end = Integer.parseInt(element.getAttribute("end"));
            final String span = text.substring(start, end + 1);
            element.setAttribute("span", span);
        }
    }

}
