package eu.fbk.dkm.pikes.resources.ecb;

import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 29/09/16.
 */

public class ECBPlusSplitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ECBPlusSplitter.class);
    private static final Pattern headerPattern = Pattern.compile("#begin document ([0-9]+)_.*");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./ecbplus-splitter")
                    .withHeader("Splits ECB+ results by folder")
                    .withOption("i", "input", "Input txt file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);

            Map<String, List<String>> res = new LinkedHashMap<>();

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line;
            String folder = null;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = headerPattern.matcher(line);
                if (matcher.find()) {
                    folder = matcher.group(1);
                }

                if (folder == null) {
                    continue;
                }
                res.putIfAbsent(folder, new ArrayList<>());
                res.get(folder).add(line);
            }

            reader.close();

            outputFolder.mkdirs();

            for (String key : res.keySet()) {
                String thisFileString = outputFolder.getAbsolutePath() + File.separator + key + ".txt";
                File thisFile = new File(thisFileString);

                BufferedWriter writer = new BufferedWriter(new FileWriter(thisFile));
                for (String thisLine : res.get(key)) {
                    writer.append(thisLine).append("\n");
                }

                writer.close();
            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }
}
