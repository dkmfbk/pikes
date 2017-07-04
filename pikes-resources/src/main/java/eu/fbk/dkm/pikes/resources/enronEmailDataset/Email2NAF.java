package eu.fbk.dkm.pikes.resources.enronEmailDataset;

import com.google.common.io.Files;
import eu.fbk.dkm.pikes.resources.ecb.ConvertECBPlus;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.IO;
import ixa.kaflib.KAFDocument;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marcorospocher on 24/04/2017.
 */
public class Email2NAF {
    private static final Logger LOGGER = LoggerFactory.getLogger(Email2NAF.class);
    private static Pattern folderPattern = Pattern.compile("^([0-9]+)");
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
            "E, dd-MMM-yy HH z", //Sunday, 12-Jan-97 00 GMT
//            DATE PROBLEM!!! :Sunday, 12-Jan-97 00 GMT
            //Tuesday, 21-Jan-97 00 GMT
            "E MMM dd HH:mm:ss z yy",//Tue May 08 13:11:00 CEST 2001
            "E, dd MMM yyyy HH:mm:ss Z (z)" //Wed, 24 Jan 2001 05:59:00 -0800 (PST)

    };
    private static String xml11pattern = "[^"
            + "\u0001-\uD7FF"
            + "\uE000-\uFFFD"
            + "\ud800\udc00-\udbff\udfff"
            + "]+";
    private static String xml10pattern = "[^"
            + "\u0009\r\n"
            + "\u0020-\uD7FF"
            + "\uE000-\uFFFD"
            + "\ud800\udc00-\udbff\udfff"
            + "]";

    public static void main(String[] args) {
        final CommandLine cmd = CommandLine
                .parser()
                .withName("convert-enron")
                .withHeader("Convert Enron Email Dataset files to NAF")
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


        for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputPath)) {
            if (!file.isFile()) {
                continue;
            }
            if (file.getName().startsWith(".")) {
                continue;
            }



                String relativeFilePath = file.getAbsolutePath().substring(inputPath.getAbsolutePath().length());
                if (relativeFilePath.startsWith(File.separator)) {
                    relativeFilePath = relativeFilePath.substring(1);
                }

                try {
                    System.out.println();
                    System.out.print("Starting processing of file: "+file.getAbsolutePath());

                    KAFDocument document = new KAFDocument("en", "FBK");

//IO.read(nafFile.getAbsolutePath())


//                    ContentHandler contentHandler = new CustomContentHandler();
//
//                    MimeConfig mime4jParserConfig = new MimeConfig();
//                    BodyDescriptorBuilder bodyDescriptorBuilder = new DefaultBodyDescriptorBuilder();
//                    MimeStreamParser mime4jParser = new MimeStreamParser(mime4jParserConfig, DecodeMonitor.SILENT,bodyDescriptorBuilder);
//                    mime4jParser.setContentDecoding(true);
//                    mime4jParser.setContentHandler(contentHandler);
//                    mime4jParser.parse(IO.read(file.getAbsolutePath()));
//
//                    Email email = ((CustomContentHandler) contentHandler).getEmail();
//
//                    Attachment plainText = email.getPlainTextEmailBody();
//                    //String to = email.getToEmailHeaderValue();
//                    //String cc = email.getCCEmailHeaderValue();
//                    String from = email.getFromEmailHeaderValue();
//                    String date = email.getHeader().getField("Date").getBody();
//                    String messageID = email.getHeader().getField("Message-ID").getBody();




                    Session s = Session.getDefaultInstance(new Properties());
                    String raw = readFile(file.getAbsolutePath());
                    InputStream is = new ByteArrayInputStream(raw.getBytes());


//                            IO.read(file.getAbsolutePath());
                    MimeMessage message = new MimeMessage(s, is);

                    message.getAllHeaderLines();
                    String from = "";
                    for (Enumeration<Header> e = message.getAllHeaders(); e.hasMoreElements();) {
                        Header h = e.nextElement();
                        if (h.getName().contains("X-From")) {
                            //System.out.println(h.getValue());
                            from = h.getValue().replaceAll(xml10pattern,"");;
                        }
                    }


                    MimeMessageParser parser = new MimeMessageParser(message);


//                    String subject = parser.getSubject();
                    String message_id = parser.getMimeMessage().getMessageID();
                    String date = parser.getMimeMessage().getSentDate().toString();


                    //System.out.println(parser.getMimeMessage().getContentType());

//                    Object temp = parser.getMimeMessage().
//

                    //String content = new String(bytes, StandardCharsets.US_ASCII);
                    String content = (String) parser.getMimeMessage().getContent();
                    //byte[] temp = content.getBytes(StandardCharsets.UTF_8);
                    //content = new String(temp);


                    content = content.replaceAll(xml10pattern,"");




//                    int n = plainText.getIs().available();
//                    byte[] bytes = new byte[n];
//                    plainText.getIs().read(bytes, 0, n);
//                    String s = new String(bytes, StandardCharsets.US_ASCII);


                    Date thisDate = null;
                    try {
                        thisDate = DateUtils.parseDate(date, parsePatterns);

                    } catch (ParseException e) {
                        System.out.println("DATE PROBLEM!!!");
                    }

                    document.setRawText(content);

                    KAFDocument.Public aPublic = document.createPublic();
                    aPublic.uri = "http://pikes.fbk.eu/enron/" + relativeFilePath;
                    aPublic.publicId = relativeFilePath;
                    KAFDocument.FileDesc fileDesc = document.createFileDesc();

                    fileDesc.title = message_id;
                    fileDesc.creationtime = sdf.format(thisDate);
                    fileDesc.author = from;

                    File outputFile = new File(outputPath + File.separator + relativeFilePath + ".naf");
                    outputFile.getParentFile().mkdirs();
                    document.save(outputFile);
                    System.out.print("  DONE");
                } catch (Exception e) {
                    e.printStackTrace();
                }


        }
    }

    public static String readFile(final String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder string_builder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        try {
            while ((line = reader.readLine()) != null) {
                string_builder.append(line);
                string_builder.append(ls);
            }

            return string_builder.toString();
        } finally {
            reader.close();
        }
    }
}
