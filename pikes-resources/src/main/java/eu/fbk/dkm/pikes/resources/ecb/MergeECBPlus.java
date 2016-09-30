package eu.fbk.dkm.pikes.resources.ecb;

import com.google.common.collect.HashMultimap;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.IO;
import ixa.kaflib.Coref;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by alessio on 28/09/16.
 */

public class MergeECBPlus {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeECBPlus.class);

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./ecbplus-merger")
                    .withHeader("Add mentions to NAF folder for ECB resource")
                    .withOption("i", "input-xml", "Input XML folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("n", "input-naf", "Input NAF folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input-xml", File.class);
            File nafFolder = cmd.getOptionValue("input-naf", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);

            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            File[] files = inputFolder.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }

                File[] thisFolderFiles = file.listFiles();
                for (File ecbFile : thisFolderFiles) {
                    if (!ecbFile.isFile()) {
                        continue;
                    }
                    if (!ecbFile.getAbsolutePath().endsWith(".xml")) {
                        continue;
                    }

                    String relativeFilePath = ecbFile.getAbsolutePath()
                            .substring(inputFolder.getAbsolutePath().length());
                    if (relativeFilePath.startsWith(File.separator)) {
                        relativeFilePath = relativeFilePath.substring(1);
                    }
                    String naf = nafFolder.getAbsolutePath() + File.separator + relativeFilePath + ".naf.gz";
                    File nafFile = new File(naf);

                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    XPathFactory xPathfactory = XPathFactory.newInstance();
                    XPath xpath = xPathfactory.newXPath();

                    XPathExpression expr;
                    NodeList nl;

                    Document doc = dBuilder.parse(IO.read(ecbFile.getAbsolutePath()));
                    doc.getDocumentElement().normalize();

                    Map<Integer, Integer> offsets = new HashMap<>();
//                    Map<Integer, Integer> anchors = new HashMap<>();
                    HashMultimap<String, Integer> clusterOffsets = HashMultimap.create();

                    // Normalization rules
                    expr = xpath.compile("/Document/token");
                    nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

                    StringBuffer buffer = new StringBuffer();
                    StringBuffer text = new StringBuffer();
                    int lastSent = 0;
                    int offset = 0;
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node item = nl.item(i);
                        Element element = (Element) item;
                        String token = element.getTextContent();

                        int t_id = Integer.parseInt(element.getAttribute("t_id"));
                        offsets.put(t_id, offset);

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

                        buffer.append(token).append(" ");
                        offset = text.length() + buffer.length();
                    }
                    if (buffer.length() > 0) {
                        text.append(buffer.toString().trim()).append("\n");
                    }

                    expr = xpath.compile("/Document/Markables/ACTION_OCCURRENCE");
                    nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node item = nl.item(i);
                        Element element = (Element) item;

                        String clusterID = element.getAttribute("m_id");

                        NodeList elements = element.getElementsByTagName("token_anchor");
                        for (int j = 0; j < elements.getLength(); j++) {
                            Node item2 = elements.item(j);
                            Element element2 = (Element) item2;

                            int t_id = Integer.parseInt(element2.getAttribute("t_id"));
                            clusterOffsets.put(clusterID, offsets.get(t_id));
                            break;
                        }

                    }

                    FileInputStream bais = new FileInputStream(nafFile);
                    GZIPInputStream gzis = new GZIPInputStream(bais);
                    InputStreamReader reader = new InputStreamReader(gzis);
                    BufferedReader in = new BufferedReader(reader);

                    KAFDocument nafDocument = KAFDocument.createFromStream(in);

                    Map<Integer, Term> termsHashMap = new HashMap<>();
                    for (Term term : nafDocument.getTerms()) {
                        termsHashMap.put(term.getOffset(), term);
                    }

                    for (String clusterId : clusterOffsets.keySet()) {
                        Set<Integer> terms = clusterOffsets.get(clusterId);
                        List<Span<Term>> termsList = new ArrayList<>();
                        for (Integer termOffset : terms) {
                            Term term = termsHashMap.get(termOffset);
                            if (term == null) {
                                LOGGER.error("Term is null!");
                                continue;
                            }
                            Span<Term> termSpan = KAFDocument.newTermSpan();
                            termSpan.addTarget(term);
                            termsList.add(termSpan);
                        }

                        if (termsList.size() == 0) {
                            continue;
                        }

                        Coref coref = nafDocument.newCoref(termsList);
                        coref.setCluster(clusterId);
                        coref.setType("event-gold");
                    }

                    String outFileName = outputFolder.getAbsolutePath() + File.separator + relativeFilePath + ".naf";
                    File outputFile = new File(outFileName);
                    Files.createParentDirs(outputFile);
                    nafDocument.save(outputFile);
                }
            }
        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
