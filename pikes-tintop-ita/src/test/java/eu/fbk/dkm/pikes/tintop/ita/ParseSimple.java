package eu.fbk.dkm.pikes.tintop.ita;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 30/05/16.
 */

public class ParseSimple {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseSimple.class);
    private static final Pattern nounPattern = Pattern.compile("([^\\(\\)]+)\\(([^\\(\\)]+)\\)");
    private static final HashMap<String, String> deMauroMorpho = new HashMap<>();

    static {
        deMauroMorpho.put("s", "n");
        deMauroMorpho.put("v", "v");
        deMauroMorpho.put("agg", "a");
        deMauroMorpho.put("avv", "r");
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./parse-dataset")
                    .withHeader("Parse the dataset and save it in a new file")
                    .withOption("m", "demauro-input-path", "De Mauro input file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("n", "easyn-input-path", "Easy nouns input file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("r", "easyv-input-path", "Easy verbs input file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output-path", "Converted file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final File deMauroPath = cmd.getOptionValue("m", File.class);
            final File verbsPath = cmd.getOptionValue("r", File.class);
            final File nounsPath = cmd.getOptionValue("n", File.class);
            final File outputPath = cmd.getOptionValue("o", File.class);

            List<String> lines;
            HashMultimap<String, String> easy1Words = HashMultimap.create();
            HashMultimap<String, String> easy2Words = HashMultimap.create();
            HashMultimap<String, String> easy3Words = HashMultimap.create();

            // Verbs

            lines = Files.readLines(verbsPath, Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("/");
                easy1Words.put("v", parts[0]);
                String w = parts[1];
                if (parts[1].startsWith("-")) {
                    int num = 3;
                    if (parts[0].endsWith("rsi")) {
                        num = 4;
                    }
                    w = parts[0].substring(0, parts[0].length() - num) + parts[1].substring(1);
                }
                easy1Words.put("v", w);
            }

            // Nouns

            lines = Files.readLines(nounsPath, Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("-")) {
                    continue;
                }

                String[] parts = line.split("/");
                for (String part : parts) {

                    part = part.replaceAll("^\\((.*)\\)", "$1");
                    Matcher matcher = nounPattern.matcher(part);
                    if (matcher.find()) {
                        easy1Words.put("n", matcher.group(1));
                        easy1Words.put("n", matcher.group(1) + matcher.group(2));
                    } else {
                        easy1Words.put("n", part);
                    }
                }
            }

            // De Mauro

            lines = Files.readLines(deMauroPath, Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("-")) {
                    continue;
                }

                String[] parts = line.split("\\t");

                String lemma = parts[0];
                String type = parts[3].replaceAll("\\s+.*", "");
                String morpho = parts[2].replaceAll("\\..*", "");
                String easyMorpho = deMauroMorpho.get(morpho);

                if (easyMorpho == null) {
                    continue;
                }

                switch (type) {
                case "FO":
                    easy2Words.put(easyMorpho, lemma);
                    break;
                case "AU":
                case "AD":
                    easy3Words.put(easyMorpho, lemma);
                    break;
                default:
                    LOGGER.error("Error on type {}", type);
                }
            }

            easy2Words.putAll(easy1Words);
            easy3Words.putAll(easy2Words);

            List<HashMultimap<String, String>> list = new ArrayList<>();
            list.add(easy1Words);
            list.add(easy2Words);
            list.add(easy3Words);

            // Save file

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

            JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.setIndent("  ");

            JsonObject jsonLevel = new JsonObject();

            int i = 0;
            for (HashMultimap mmap : list) {
                JsonObject jsonPos = new JsonObject();
                for (Object pos : mmap.keySet()) {

                    JsonArray array = new JsonArray();
                    for (Object lemma : mmap.get((String) pos)) {
                        array.add((String) lemma);
                    }

                    jsonPos.add((String) pos, array);
                }
                jsonLevel.add("level-" + ++i, jsonPos);
            }

            Gson builder = new GsonBuilder().create();
            builder.toJson(jsonLevel, jsonWriter);

            jsonWriter.close();
            writer.close();

            LOGGER.info("Easy 1 size: {}", easy1Words.size());
            LOGGER.info("Easy 2 size: {}", easy2Words.size());
            LOGGER.info("Easy 3 size: {}", easy3Words.size());

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
