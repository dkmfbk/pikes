package eu.fbk.dkm.pikes.resources.signalmedia;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by alessio on 28/12/15.
 */

public class JsonToNaf {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToNaf.class);
    private static final String DEFAULT_PREFIX = "http://signalmedia/";

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./taol-extractor")
                    .withHeader("Convert file from SignalMedia JSON to NAF")
                    .withOption("i", "input", "Input file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("p", "prefix", String.format("Prefix (default %s)", DEFAULT_PREFIX), "PREFIX",
                            CommandLine.Type.STRING, true, false, false)
                    .withOption("t", "skip-title", "Do not insert title into text")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);
            String prefix = cmd.getOptionValue("prefix", String.class, DEFAULT_PREFIX);

            boolean skipTitle = cmd.hasOption("skip-title");

            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            InputStream fileStream = new FileInputStream(inputFile);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(decoder);

            String line;
            while ((line = reader.readLine()) != null) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> rootNode = mapper.readValue(line, Map.class);

                String id = (String) rootNode.get("id");
                String content = (String) rootNode.get("content");
                String title = (String) rootNode.get("title");
                String mediaType = (String) rootNode.get("media-type");
                String source = (String) rootNode.get("source");
                String published = (String) rootNode.get("published");

                if (!skipTitle) {
                    content = title + "\n\n" + content;
                }

                // Fix a stupid bug in the dataset
                content = content.replaceAll("]]>", "");

                String simpleID = id.replaceAll("[^0-9a-zA-Z]", "");
                String subFolder = simpleID.substring(0, 2);
                File subFolderFile = new File(outputFolder + File.separator + subFolder);
                subFolderFile.mkdirs();

                String url = prefix + id;
                String outputFile = outputFolder + File.separator + subFolder + File.separator + id + ".naf";

                KAFDocument document = new KAFDocument("en", "v3");

                KAFDocument.Public documentPublic = document.createPublic();
                documentPublic.uri = url;
                documentPublic.publicId = id;

                KAFDocument.FileDesc documentFileDesc = document.createFileDesc();
                documentFileDesc.filename = id + ".naf";
                documentFileDesc.title = title;
                documentFileDesc.creationtime = published;
                documentFileDesc.author = source;
                documentFileDesc.filetype = mediaType;

                document.setRawText(content);

                document.save(outputFile);
            }
            reader.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
