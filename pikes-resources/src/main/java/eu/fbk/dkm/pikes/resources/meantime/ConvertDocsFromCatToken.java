package eu.fbk.dkm.pikes.resources.meantime;

import eu.fbk.dkm.pikes.naflib.StripNAF;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by marcorospocher on 12/05/16.
 */
public class ConvertDocsFromCatToken {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static String DEFAULT_URL = "http://pikes.fbk.eu/conll/";

    public static void main(String[] args) throws Exception {


        final CommandLine cmd = CommandLine
                .parser()
                .withName("ConvertDocsFromCatToken")
                .withHeader("ConvertDocsFromCatToken")
                .withOption("i", "input-folder", "the folder of the input NAF corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                .withOption("o", "output-folder", "the folder of the input NAF corpus", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
                .withOption("s", "sentences", "limit to 5 sentences")
//                .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true,
//                        false, true)
                .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

        File inputFolder = cmd.getOptionValue("input-folder", File.class);
        File outputFolder = cmd.getOptionValue("output-folder", File.class);
        boolean sentence = cmd.hasOption("s");

        for (final File file : com.google.common.io.Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
            if (!file.isFile()) {
                continue;
            }
            if (file.getName().startsWith(".")) {
                continue;
            }

            if (!file.getName().endsWith(".xml")) {
                continue;
            }



            File outputFile = new File(file.getAbsoluteFile().toString().replace(inputFolder.getAbsolutePath(),outputFolder.getAbsolutePath()).replace(".xml",".naf"));

            if (!outputFile.exists()) {


                try (Reader reader = IO.utf8Reader(IO.buffer(IO.read(file.getAbsoluteFile().toString())))) {
                    try {

                        //System.out.print(" WORKING");


//                        List<String> content = FileUtils.readLines(file, "utf-8");
//                        String header = content.get(0);
//                        List<String> token = content.stream().filter(line -> line.startsWith("<token")).collect(Collectors.toList());
//                        System.out.println("CIAO");

                        String rawText = "";

                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                        dbf.setValidating(false);
                        dbf.setIgnoringComments(false);
                        dbf.setIgnoringElementContentWhitespace(true);
                        dbf.setNamespaceAware(true);
                        // dbf.setCoalescing(true);
                        // dbf.setExpandEntityReferences(true);

                        DocumentBuilder db = null;
                        db = dbf.newDocumentBuilder();
                        db.setEntityResolver(new DOMUtils.NullResolver());

                        // db.setErrorHandler( new MyErrorHandler());
                        InputSource ips = new InputSource(reader);
                        //return db.parse(ips);

                        Document catDoc = db.parse(ips);

                        Integer prevSentenceNum = 0;
                        NodeList tokens = catDoc.getElementsByTagName("token");
                        for(int k=0;k<tokens.getLength();k++){
                            Node token = ((Node)tokens.item(k));
                            String tk = token.getTextContent();
//                            System.out.println(token.getNodeName()+" : "+token.getTextContent());
                            Integer sentenceNum = Integer.parseInt(token.getAttributes().getNamedItem("sentence").getTextContent());
//                            System.out.println(tk+"   "+sentenceNum);
                            if (sentence)
                                if (sentenceNum > 5)
                                    break;
                            if (sentenceNum != prevSentenceNum) {
                                rawText = rawText + "\n";
                                prevSentenceNum = sentenceNum;
                            }
                            rawText=rawText+" "+tk;
                        }
                        System.out.println(rawText);


                        if (!rawText.isEmpty()) {

                            outputFile.getParentFile().mkdirs();
                            KAFDocument document = new KAFDocument("en", "v3");

                            document.save(outputFile.getAbsolutePath());

                            document.setRawText(rawText);




                            KAFDocument.FileDesc fileDesc = document.createFileDesc();
                            fileDesc.title = catDoc.getDocumentElement().getAttribute("doc_name");

                            Date thisDate = new Date();

                            fileDesc.creationtime = sdf.format(thisDate);
                            String URL_str = catDoc.getDocumentElement().getAttribute("url");
                            fileDesc.filename = catDoc.getDocumentElement().getAttribute("doc_name");

                            String urlTemplate = DEFAULT_URL;
                            if (cmd.hasOption("url-template")) {
                                urlTemplate = cmd.getOptionValue("url-template", String.class);
                            }

                            KAFDocument.Public aPublic = document.createPublic();
                            //aPublic.uri = URL_str;
                            aPublic.uri = URL_str;
                            aPublic.publicId = catDoc.getDocumentElement().getAttribute("doc_id");

                            document.save(outputFile.getAbsolutePath());
                        }



                    } catch (Exception e) {

                    }

                }
            } //else System.out.println(" SKIPPED");



//
//
//
//
//                if (!text.isEmpty()) {
//                    File outputFile = new File(outputfile.getAbsoluteFile().toString() + "/" + StringUtils.leftPad(ID.toString(),4,"0") + ".naf");
//
//                    //File outputFile = new File(outputFileName);
//                    outputFile.getParentFile().mkdirs();
//                    KAFDocument document = new KAFDocument("en", "v3");
//
//                    document.save(outputFile.getAbsolutePath());
//
//                    document.setRawText(text);
//
//                    KAFDocument.FileDesc fileDesc = document.createFileDesc();
//                    fileDesc.title = ID.toString();
//
//                    Date thisDate = new Date();
//
//                    fileDesc.creationtime = sdf.format(thisDate);
//                    String URL_str = ID.toString();
//                    fileDesc.filename = URL_str;
//
//                    String urlTemplate = DEFAULT_URL;
//                    if (cmd.hasOption("url-template")) {
//                        urlTemplate = cmd.getOptionValue("url-template", String.class);
//                    }
//
//                    KAFDocument.Public aPublic = document.createPublic();
//                    //aPublic.uri = URL_str;
//                    aPublic.uri = urlTemplate + ID.toString();
//                    aPublic.publicId = ID.toString();
//
//                    document.save(outputFile.getAbsolutePath());
//                    text="";
//                    ID++;
//                }
//
//            } else if (line.isEmpty()) text+="\n";
//            else {
//                String[] conll_item = line.split(" ");
//                text+=conll_item[0]+" ";
//            }

        }
    }

}