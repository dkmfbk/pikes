package eu.fbk.dkm.pikes.resources.trec;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import eu.fbk.utils.core.CommandLine;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;

/**
 * Created by alessio on 15/12/15.
 */

public class Qrels {

    private static final Logger LOGGER = LoggerFactory.getLogger(Qrels.class);
//    private static String folder = "/Users/alessio/Documents/scripts/pikesir/test/trec/queries/";
//    private static String outputFile = "/Users/alessio/Documents/scripts/pikesir/test/trec/queries.tsv";

    public static void main(String[] args) {

        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("trec-qrels-converter")
                    .withHeader("Convert TREC qrels into TSV format")
                    .withOption("i", "input", "Input folder", "FOLDER", CommandLine.Type.DIRECTORY_EXISTING, true,
                            false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            HashMultimap<String, String> qrels = HashMultimap.create();

            Iterator<File> fileIterator = FileUtils.iterateFiles(inputFolder, null, true);
            while (fileIterator.hasNext()) {
                File file = fileIterator.next();

                LOGGER.info(file.getName());

                List<String> lines = FileUtils.readLines(file, Charsets.UTF_8);
                for (String line : lines) {
                    line = line.trim();

                    String[] parts = line.split("\\s+");

                    String qID = "q" + parts[0];
                    String docID = parts[2];
                    String relevance = parts[3];

                    if (relevance.equals("0")) {
                        continue;
                    }

                    qrels.put(qID, docID);
                }
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            for (String key : qrels.keySet()) {
                writer.append(key).append("\t");
                StringBuffer stringBuffer = new StringBuffer();
                for (String value : qrels.get(key)) {
                    stringBuffer.append(";").append(value).append(":1");
                }
                writer.append(stringBuffer.toString().substring(1));
                writer.append("\n");
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
