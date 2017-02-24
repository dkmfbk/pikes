package eu.fbk.dkm.pikes.resources.conllAIDA;

import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

/**
 * Created by marcorospocher on 12/05/16.
 */
public class ConvertDocs {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static String DEFAULT_URL = "http://pikes.fbk.eu/conll/";

    public static void main(String[] args) throws Exception {


        final CommandLine cmd = CommandLine
                .parser()
                .withName("triplificator")
                .withHeader("Generates < YAGO entity, rdf:type , NER type> triples")
                .withOption("i", "conll", "conll file", "FILE", CommandLine.Type.FILE, true,
                        false, true)
                .withOption("o", "output", "Output file", "FOLDER", CommandLine.Type.DIRECTORY, true,
                        false, true)
                .withOption("u", "url-template", "URL template (with %d for the ID)", "URL",
                        CommandLine.Type.STRING, true, false, false)
                .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                .parse(args);

        File conllfolder = cmd.getOptionValue("conll", File.class);
        File outputfile = cmd.getOptionValue("output", File.class);




        try (Stream<String> stream = Files.lines(Paths.get(conllfolder.toString()))) {


            stream.forEach(line -> {

                JSONParser parser = new JSONParser();

                try {
                    Object obj = parser.parse(line);
                    JSONObject jsonObject = (JSONObject) obj;
                    String ID = (String) jsonObject.get("docId");
                    String text = (String) jsonObject.get("text");
                    //System.out.println(text);

                    File outputFile = new File(outputfile.getAbsoluteFile().toString()+"/"+ID+".naf");

                    //File outputFile = new File(outputFileName);
                    outputFile.getParentFile().mkdirs();

                    KAFDocument document = new KAFDocument("en", "v3");

                    //document.setRawText(postProcess(cleaned_text));
                    document.setRawText(text);

                    KAFDocument.FileDesc fileDesc = document.createFileDesc();
                    fileDesc.title = ID;

                    Date thisDate = new Date();

                    fileDesc.creationtime = sdf.format(thisDate);
                    String URL_str = ID;
                    fileDesc.filename = URL_str;

                    String urlTemplate = DEFAULT_URL;
                    if (cmd.hasOption("url-template")) {
                        urlTemplate = cmd.getOptionValue("url-template", String.class);
                    }

                    KAFDocument.Public aPublic = document.createPublic();
                    //aPublic.uri = URL_str;
                    aPublic.uri = urlTemplate + ID;
                    aPublic.publicId = ID;

                    document.save(outputFile.getAbsolutePath());




                } catch (org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
                }


            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}