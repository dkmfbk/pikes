package eu.fbk.dkm.pikes.resources.conllAIDA;

import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
 * DEPRECATED!! Better use ConvertDocsFromAIDAGS
 */
public class ConvertDocsFromGS {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static String DEFAULT_URL = "http://pikes.fbk.eu/conll/";

    public static void main(String[] args) throws Exception {


        final CommandLine cmd = CommandLine
                .parser()
                .withName("ConvertDocsFromGS")
                .withHeader("Generates < YAGO entity, rdf:type , NER type> triples")
                .withOption("c", "conll", "CONLL folder", "FOLDER", CommandLine.Type.DIRECTORY, true, false, true)
                .withOption("o", "output", "Output file", "FOLDER", CommandLine.Type.DIRECTORY, true,
                        false, true)
                .withOption("u", "url-template", "URL template (with %d for the ID)", "URL",
                        CommandLine.Type.STRING, true, false, false)
                .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                .parse(args);

        File conllfolder = cmd.getOptionValue("conll", File.class);
        File outputfile = cmd.getOptionValue("output", File.class);

        List<String> conll_list = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(conllfolder.toString()+"/eng.train"))) {

            conll_list.addAll(stream
 //                   .filter(line -> !line.startsWith("-DOCSTART-"))
 //                   .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        //added as missing starting DOCSTART
        conll_list.add("-DOCSTART- -X- O O");

        try (Stream<String> stream = Files.lines(Paths.get(conllfolder.toString()+"/eng.testa"))) {


            conll_list.addAll(stream
 //                   .filter(line -> !line.startsWith("-DOCSTART-"))
 //                   .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        //added as missing starting DOCSTART
        conll_list.add("-DOCSTART- -X- O O");

        try (Stream<String> stream = Files.lines(Paths.get(conllfolder.toString()+"/eng.testb"))) {
            conll_list.addAll(stream
 //                   .filter(line -> !line.startsWith("-DOCSTART-"))
 //                   .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        Integer ID=1;
        conll_list.remove(0);
        conll_list.add("-DOCSTART-"); //to ease processing

        String text="";

        for (String line : conll_list
                ) {

            if (line.startsWith("-DOCSTART-")) {

                if (!text.isEmpty()) {
                    File outputFile = new File(outputfile.getAbsoluteFile().toString() + "/" + StringUtils.leftPad(ID.toString(),4,"0") + ".naf");

                    //File outputFile = new File(outputFileName);
                    outputFile.getParentFile().mkdirs();
                    KAFDocument document = new KAFDocument("en", "v3");

                    document.save(outputFile.getAbsolutePath());

                    document.setRawText(text);

                    KAFDocument.FileDesc fileDesc = document.createFileDesc();
                    fileDesc.title = ID.toString();

                    Date thisDate = new Date();

                    fileDesc.creationtime = sdf.format(thisDate);
                    String URL_str = ID.toString();
                    fileDesc.filename = URL_str;

                    String urlTemplate = DEFAULT_URL;
                    if (cmd.hasOption("url-template")) {
                        urlTemplate = cmd.getOptionValue("url-template", String.class);
                    }

                    KAFDocument.Public aPublic = document.createPublic();
                    //aPublic.uri = URL_str;
                    aPublic.uri = urlTemplate + ID.toString();
                    aPublic.publicId = ID.toString();

                    document.save(outputFile.getAbsolutePath());
                    text="";
                    ID++;
                }

            } else if (line.isEmpty()) text+="\n";
            else {
                String[] conll_item = line.split(" ");
                text+=conll_item[0]+" ";
            }

        }
    }

}