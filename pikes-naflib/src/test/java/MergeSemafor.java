import ixa.kaflib.*;
import org.openrdf.query.algebra.Str;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by alessio on 06/11/15.
 */

public class MergeSemafor {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MergeSemafor.class);

    public static void main(String[] args) {
        String nafFolder = "/Users/alessio/Documents/semafor-sentences/naf";
        String semFolder = "/Users/alessio/Documents/semafor-sentences/semafor";
        String outFolder = "/Users/alessio/Documents/semafor-sentences/out";

        try {

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            File nafFolderFile = new File(nafFolder);
            if (!nafFolderFile.exists()) {
                throw new IOException();
            }
            if (!nafFolderFile.isDirectory()) {
                throw new IOException();
            }

            File[] listOfFiles = nafFolderFile.listFiles();

            for (int i = 0; i < listOfFiles.length; i++) {
                File file = listOfFiles[i];
                if (file.isFile()) {

                    System.out.println("File " + file.getName());
                    KAFDocument document = KAFDocument.createFromFile(file);

                    File semaforFile = new File(semFolder + File.separator + file.getName().replaceAll("naf$", "xml"));
                    if (!semaforFile.exists()) {
                        LOGGER.error("Semafor file {} does not exist", semaforFile.getAbsolutePath());
                        continue;
                    }

                    Document doc = dBuilder.parse(semaforFile);
                    doc.getDocumentElement().normalize();

                    NodeList nList;
                    nList = doc.getElementsByTagName("sentences");
                    int numSent = nList.getLength();

                    if (numSent != 1) {
                        LOGGER.error("Wrong number of sentences: {}", numSent);
                        continue;
                    }

                    nList = doc.getElementsByTagName("annotationSet");
                    for (int temp = 0; temp < nList.getLength(); temp++) {
                        Node nNode = nList.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;

                            String frameName = eElement.getAttribute("frameName");
                            HashMap<String, List<Term>> roles = new HashMap<>();

                            NodeList labelList = eElement.getElementsByTagName("label");
                            for (int j = 0; j < labelList.getLength(); j++) {
                                Node labelNode = labelList.item(j);
                                if (labelNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element labelElement = (Element) labelNode;

                                    String name = labelElement.getAttribute("name");
                                    String span = labelElement.getAttribute("span");
                                    int start = Integer.parseInt(labelElement.getAttribute("start"));
                                    int end = Integer.parseInt(labelElement.getAttribute("end"));

                                    String[] tokens = span.split("\\s+");
                                    if (tokens.length == 0) {
                                        LOGGER.error("Invalid tokens");
                                        continue;
                                    }

                                    List<Term> terms = document.getTerms();

                                    String firstToken = tokens[0];
                                    String lastToken = tokens[tokens.length - 1];
                                    Integer foundFirst = findToken(terms, firstToken, start);
                                    Integer foundLast = findToken(terms, lastToken, end - lastToken.length() + 1);

                                    if (foundFirst == null || foundLast == null) {
                                        LOGGER.error("Found is null");
                                        continue;
                                    }

                                    List<Term> okTerms = new ArrayList<>();
                                    for (Term term : terms) {
                                        if (term.getWFs().size() != 1) {
                                            LOGGER.error("Wrong number of WF");
                                            continue;
                                        }

                                        for (WF wf : term.getWFs()) {
                                            if (wf.getOffset() >= foundFirst && wf.getOffset() <= foundLast) {
                                                okTerms.add(term);
                                            }
                                        }
                                    }

                                    roles.put(name, okTerms);
                                }
                            }

                            if (!roles.containsKey("Target")) {
                                LOGGER.error("No Target");
                                continue;
                            }

                            Span<Term> target = KAFDocument.newTermSpan(roles.get("Target"));

                            Predicate predicate = document.newPredicate(target);
                            predicate.addExternalRef(document.createExternalRef("FrameNet", frameName));
                            predicate.setId("f_" + predicate.getId());

                            for (String key : roles.keySet()) {
                                if (key.equals("Target")) {
                                    continue;
                                }

                                Span<Term> span = KAFDocument.newTermSpan(roles.get(key));
                                Predicate.Role role = document.newRole(predicate, key, span);
                                role.addExternalRef(document.createExternalRef("FrameNet", frameName + "@" + key));
                                predicate.addRole(role);
                            }
                        }
                    }

                    String outFileName = outFolder + File.separator + file.getName();
                    document.save(outFileName);
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static Integer findToken(List<Term> terms, String token, int start) {
        HashMap<Integer, Term> okTerms = new HashMap<>();
        for (Term term : terms) {
            for (WF wf : term.getWFs()) {
                if (wf.getForm().trim().toLowerCase().equals(token.toLowerCase())) {
                    okTerms.put(wf.getOffset(), term);
                }
            }
        }

        Integer found = null;
        for (int k = 0; k < 5; k++) {
            if (okTerms.containsKey(start - k)) {
                found = start - k;
                break;
            }
        }

        return found;
    }

}
