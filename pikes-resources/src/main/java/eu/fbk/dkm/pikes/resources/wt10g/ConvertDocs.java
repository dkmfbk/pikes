package eu.fbk.dkm.pikes.resources.wt10g;

import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.lang.time.DateUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unbescape.html.HtmlEscape;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by marcorospocher on 12/05/16.
 */
public class ConvertDocs {


    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertDocs.class);
    private static String DEFAULT_URL = "http://pikes.fbk.eu/ke4ir/wt10g/docs/";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    //private static SimpleDateFormat sdf2 = new SimpleDateFormat("E, dd-MM-yy HH:mm:ss z");
    //private static DateFormat format = new SimpleDateFormat("E, d m yyyy", Locale.ENGLISH);
    //private static Pattern datePattern1 = Pattern.compile("^([a-zA-Z]+,\\s+[0-9]+[\\s,\\-][a-zA-Z]+[\\s,\\-][0-9]+)");
    private static String[] parsePatterns = {
            "E, dd-MMM-yy HH:mm:ss z",  //Wednesday, 01-Jan-97 15:20:23 GMT
            "E, dd MMM yy HH:mm:ss z", //Fri, 17 Jan 97 02:15:05 GMT
            "E, dd MMM yyyy HH:mm:ss z", //Wed, 01 Jan 1997 15:21:07 GM
            "E MMM dd HH:mm:ss yyyy",    //Wed Mar  6 13:36:27 1996
            "E, dd-MMM-yy HH:mm:ss yyyy",    //Friday, 21-Feb-97 15:29:07 1997
            ", dd MMM yyyy HH:mm:ss z",    //, 17 Feb 1997 14:15:0 GMT
            "MMM dd, yyyy",    //January 23, 1997
            "E, dd-MMM-yy z", //Friday, 07-Feb-97 ? GMT
            "E, dd MMM yy z", //Mon, 10 Feb 1997 ? GMT
            //OK Fri, 13 Jan 1997 22:13:59 GMT
            //OK Mon,10 Feb 97 14:00:41 +0000
            "E, dd-MMM-yy HH z" //Sunday, 12-Jan-97 00 GMT
//            DATE PROBLEM!!! :Sunday, 12-Jan-97 00 GMT
            //Tuesday, 21-Jan-97 00 GMT

    };

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("wt10g-mysql-extractor")
                    .withHeader("Extract documents from wt10g mysql dump and save them in NAF format")
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

            //String content = FileUtils.readFileToString(inputfile, Charsets.UTF_8);


            String document="";
                try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile), Charset.forName("Windows-1252")))) {
                for(String line; (line = br.readLine()) != null; ) {
                    if (line.contains("INSERT INTO trecdocument VALUES (")) {
                        //start of document
                        document=line.replace("INSERT INTO trecdocument VALUES (","");
                        if (document.trim().endsWith("');")) {
                            //single line sql
                            document=document.replace(");","");
                            processWT10GDocument(document, outputFolder, urlTemplate);
                            document="";
                        } else document=document+"\n";
                    } else if (line.trim().startsWith("');")) {
                        //end of document
                        line=line.replace(");","");
                        document=document+line;
                        processWT10GDocument(document, outputFolder, urlTemplate);
                        document="";
                    } else {
                        //middle of nowhere
                        if (!line.trim().isEmpty()) document=document+line+"\n";
                    }
                }
                // line is not visible here.
            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }


    private static void processWT10GDocument(String raw, File outputFolder, String urlTemplate) {



        String[] parts = raw.split("', '");

        String ID_str = parts[0].replaceFirst("'","").trim();
        System.out.println("STARTING PROCESSING OF DOCUMENT :"+ID_str);
        saveSingleSQL(raw,ID_str,outputFolder);

        String URL_str = parts[1].trim();;
        String DATE_str = parts[2].trim().replace("?","").replace(",",", ");
        //String DATE_str = "Sunday, 12-Jan-97 00 GMT".replace("?","").replace(",",", ");
        String TEXT_str = parts[3].substring(0,parts[3].lastIndexOf("'"));

        if (!TEXT_str.trim().isEmpty()) {

            Date thisDate = null;

            try {
                thisDate = DateUtils.parseDate(DATE_str, parsePatterns);

            } catch (ParseException e) {
                //
                try {
                    thisDate = DateUtils.parseDate("Wednesday, 01-Jan-97 00:00:00 GMT", parsePatterns);
                    if (!DATE_str.contains("NULL") && !DATE_str.isEmpty())
                        System.out.println("MALFORMED DATE PROBLEM in " + ID_str + "!!! :" + DATE_str);
                } catch (ParseException e1) {
                    // never enter here
                    thisDate = new Date();
                }


            }

            String[] localName_parts = ID_str.split("-");
            String localName = localName_parts[0] + File.separator + localName_parts[1] + File.separator + ID_str;
            String outputFileName = outputFolder.getAbsolutePath() + File.separator + "naf-in" + File.separator + localName + ".naf";


            //start cleaning content

            String text_to_clean = TEXT_str.replace("''", "'");
//        try {
//            //cleaned_text = parseToPlainText(ID_str,Jsoup.clean(TEXT_str.replace("''","'"), Whitelist.relaxed()));
//
//            cleaned_text = parseToPlainText(ID_str,TEXT_str.replace("''","'"));
//            if (cleaned_text.contains("</")||cleaned_text.contains("/>"))
//                System.out.println("POSSIBLE HTML TAGS: "+ID_str);
//
//            //save file if only parsing OK
//            File outputFile = new File(outputFileName);
//            outputFile.getParentFile().mkdirs();
//
//            KAFDocument document = new KAFDocument("en", "v3");
//            document.setRawText(cleaned_text.replace("]]","")); // added .replace("]]","") for cdata issue in kaflib
//
//            KAFDocument.FileDesc fileDesc = document.createFileDesc();
//            fileDesc.title = ID_str;
//
//            fileDesc.creationtime = sdf.format(thisDate);
//            fileDesc.filename = URL_str;
//
//            KAFDocument.Public aPublic = document.createPublic();
//            //aPublic.uri = URL_str;
//            aPublic.uri = urlTemplate+ID_str;
//            aPublic.publicId = ID_str;
//
//
//            document.save(outputFile.getAbsolutePath());
//
//
//
//        } catch (TikaException e) {
//            System.out.println("TIKA PROBLEMS PARSING - LEVEL1: "+ID_str);
//            //e.printStackTrace();
//            //
//            //System.out.println(TEXT_str);
//            String clean = Jsoup.clean(TEXT_str.replace("''","'"), Whitelist.relaxed());
//            //System.out.println(clean);
//            try {
//                cleaned_text=parseToPlainText(ID_str,clean);
////                int i=0;
////                while(cleaned_text.contains()&&i++<10) {
////                    System.out.println("Iteration :"+i);
////                    cleaned_text=parseToPlainText(cleaned_text);
////                }
//
//
//                //save file if only parsing OK
//                File outputFile = new File(outputFileName);
//                outputFile.getParentFile().mkdirs();
//
//                KAFDocument document = new KAFDocument("en", "v3");
//                document.setRawText(cleaned_text);
//
//                KAFDocument.FileDesc fileDesc = document.createFileDesc();
//                fileDesc.title = ID_str;
//
//                fileDesc.creationtime = sdf.format(thisDate);
//                fileDesc.filename = URL_str;
//
//                KAFDocument.Public aPublic = document.createPublic();
//                //aPublic.uri = URL_str;
//                aPublic.uri = urlTemplate+ID_str;
//                aPublic.publicId = ID_str;
//
//
//                document.save(outputFile.getAbsolutePath());
//
//                saveSingleSQL(raw,ID_str,outputFolder);
//
//            } catch (IOException e1) {
//                System.out.println("TIKA PROBLEMS PARSING - LEVEL2: "+ID_str);
//                e1.printStackTrace();
//                //saveSingleSQL(raw,ID_str,outputFolder);
//            } catch (SAXException e1) {
//                System.out.println("SAX PROBLEMS PARSING - LEVEL2: "+ID_str);
//                e1.printStackTrace();
//                //saveSingleSQL(raw,ID_str,outputFolder);
//            } catch (TikaException e1) {
//                System.out.println("IO PROBLEMS PARSING - LEVEL2: "+ID_str);
//                e1.printStackTrace();
//                //saveSingleSQL(raw,ID_str,outputFolder);
//            }
//
//
//        } catch (SAXException e) {
//            System.out.println("SAX PROBLEMS PARSING: "+ID_str);
//            e.printStackTrace();
//            //saveSingleSQL(raw,ID_str,outputFolder);
//        } catch (IOException e) {
//            System.out.println("IO PROBLEMS PARSING: "+ID_str);
//            e.printStackTrace();
//            //saveSingleSQL(raw,ID_str,outputFolder);
//        } catch (Throwable e) {
//            System.out.println("OTHERS PARSING: "+ID_str);
//            e.printStackTrace();
//            //saveSingleSQL(raw,ID_str,outputFolder);
//        }
//
//        //System.out.println("TEXT_str output: "+cleaned_text);

            HtmlCleaner cleaner = new HtmlCleaner();

// take default cleaner properties
            CleanerProperties props = cleaner.getProperties();

// customize cleaner's behaviour with property setters
            props.setAddNewlineToHeadAndBody(true);

            final SimpleHtmlSerializer htmlSerializer =
                    new SimpleHtmlSerializer(props);

// Clean HTML taken from simple string, file, URL, input stream,
// input source or reader. Result is root node of created
// tree-like structure. Single cleaner instance may be safely used
// multiple times.
            String cleaned_text = "";
            try {
                TagNode node = cleaner.clean(text_to_clean);
                //cleaned_text = HtmlEscape.unescapeHtml(node.getText().toString());
                cleaned_text=cleaner.clean(htmlSerializer.getAsString(node)).getText().toString();

//                try {
//                    cleaned_text = parseToPlainText(ID_str,htmlSerializer.getAsString(node));
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                } catch (SAXException e1) {
//                    e1.printStackTrace();
//                } catch (TikaException e1) {
//                    e1.printStackTrace();
//                }

            } catch (IllegalArgumentException e) {
                //e.printStackTrace();
                System.out.println("POSSIBLE ILLEGAL CHARACTERS : " + ID_str);
                text_to_clean = HtmlEscape.unescapeHtml(text_to_clean);
                TagNode node = cleaner.clean(text_to_clean);
                //cleaned_text = HtmlEscape.unescapeHtml(node.getText().toString());
                try {
                    cleaned_text = parseToPlainText(ID_str,htmlSerializer.getAsString(node));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (SAXException e1) {
                    e1.printStackTrace();
                } catch (TikaException e1) {
                    e1.printStackTrace();
                }
            } catch (ClassCastException e) {
                //e.printStackTrace();
                System.out.println("POSSIBLE CAST ISSUE - JSOUPED: " + ID_str);
                text_to_clean = Jsoup.clean(text_to_clean, Whitelist.simpleText());
                TagNode node = cleaner.clean(text_to_clean);
                //cleaned_text = HtmlEscape.unescapeHtml(node.getText().toString());
                try {
                    cleaned_text = parseToPlainText(ID_str,htmlSerializer.getAsString(node));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (SAXException e1) {
                    e1.printStackTrace();
                } catch (TikaException e1) {
                    e1.printStackTrace();
                }
            } catch (Throwable e) {
//            System.out.println("OTHERS PARSING: "+ID_str);
//            e.printStackTrace();
//            //saveSingleSQL(raw,ID_str,outputFolder);
                System.out.println("OTHERS PARSING ERRORS: " + ID_str);
                //WTX057-B29-245
                text_to_clean = Jsoup.clean(text_to_clean, Whitelist.simpleText());
                TagNode node = cleaner.clean(text_to_clean);
                //cleaned_text = HtmlEscape.unescapeHtml(node.getText().toString());
                try {
                    cleaned_text = parseToPlainText(ID_str,htmlSerializer.getAsString(node));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (SAXException e1) {
                    e1.printStackTrace();
                } catch (TikaException e1) {
                    e1.printStackTrace();
                }
                //e.printStackTrace();
            }


            if (cleaned_text.equals(text_to_clean)) System.out.println("POSSIBLE PROBLEM IN CLEANING HTML: " + ID_str);
            if (cleaned_text.contains("</") || cleaned_text.contains("/>"))
                System.out.println("POSSIBLE HTML TAGS IN: " + ID_str);

            //System.out.println("File :"+ID_str);
            //System.out.println("File :"+cleaned_text);

            File outputFile = new File(outputFileName);
            outputFile.getParentFile().mkdirs();

            KAFDocument document = new KAFDocument("en", "v3");

            while (cleaned_text.contains("]]>"))
                cleaned_text = cleaned_text.replace("]]>", "");

            //document.setRawText(postProcess(cleaned_text));
            document.setRawText(cleaned_text);

            KAFDocument.FileDesc fileDesc = document.createFileDesc();
            fileDesc.title = ID_str;

            fileDesc.creationtime = sdf.format(thisDate);
            fileDesc.filename = URL_str;

            KAFDocument.Public aPublic = document.createPublic();
            //aPublic.uri = URL_str;
            aPublic.uri = urlTemplate + ID_str;
            aPublic.publicId = ID_str;

            document.save(outputFile.getAbsolutePath());



        } else System.out.println("EMPTY FILE DISCARDED:" + ID_str);
        System.out.println("PROCESSING OF DOCUMENT CONCLUDED:" + ID_str);
    }


    private static String parseToPlainText(String ID_str, String TEXT_str) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler(-1);

        HtmlParser parser = new HtmlParser();
        //AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext pcontext = new ParseContext();
        try (InputStream stream = new ByteArrayInputStream( TEXT_str.getBytes(  ) )) {
            parser.parse(stream, handler, metadata,pcontext);
            //System.out.println(metadata.get("title"));
            String title = metadata.get("title");
            if (title==null) title ="";
            String content=handler.toString();
            //if (content.equalsIgnoreCase(TEXT_str)) System.out.println("SAME FILE AFTER PARSED!!!: "+ID_str);
            return title+"\n"+content;
        }
    }

    private static void saveSingleSQL(String raw, String ID_str, File outputFolder ){

        String[] localName_parts =  ID_str.split("-");
        String localName = localName_parts[0]+File.separator+localName_parts[1]+File.separator+ID_str;

        BufferedWriter writer = null;
        try {

            File errorFile = new File(outputFolder.getAbsolutePath()+File.separator+"sql"+File.separator+localName+".sql");
            errorFile.getParentFile().mkdirs();

            writer = new BufferedWriter(new FileWriter(errorFile));
            writer.write("INSERT INTO trecdocument VALUES ("+raw+");");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }

    }

    private static String postProcess(String data) {
        //do something

        String[] arr_data = data.split("\\n",-1);
        String final_data="";

        for(int i=0;i<arr_data.length;i++){
            if (!arr_data[i].isEmpty()) final_data=final_data+arr_data[i]+" ";
            else final_data=final_data+arr_data[i]+"\n";
        }
        return final_data;
    }

}