package eu.fbk.dkm.pikes.tintop.ita;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import eu.fbk.dkm.pikes.tintop.ita.simpserver.GlossarioEntry;
import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by alessio on 30/05/16.
 */

public class ParseGlossario {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseGlossario.class);

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./convert-glossario")
                    .withHeader("Convert che raw file for glossario")
                    .withOption("i", "input-path", "Original file with glossario", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output-path", "Converted file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final File inputPath = cmd.getOptionValue("i", File.class);
            final File outputPath = cmd.getOptionValue("o", File.class);

            String lastDef = null;

            HashMap<String, String> definitions = new HashMap<>();

            List<String> lines = Files.readLines(inputPath, Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() <= 1) {
                    continue;
                }

                boolean isDef = true;
                if (line.length() > 60) {
                    isDef = false;
                }
                if (line.matches(".*[\\.;\\]]$")) {
                    isDef = false;
                }
                if (line.matches("^[0-9].*")) {
                    isDef = false;
                }

                if (isDef) {
                    lastDef = line;
                }

                if (lastDef != null) {
                    definitions.putIfAbsent(lastDef, "");
                    if (!isDef) {
                        char space = ' ';
                        if (line.matches("^[0-9].*")) {
                            space = '\n';
                        }
                        String value = definitions.get(lastDef) + space + line;
                        value = value.trim();
                        definitions.put(lastDef, value);
                    }
                }

//                System.out.println((isDef ? "[DEF]" : "[TXT]") + " --- " + line);

            }

            List<GlossarioEntry> entries = new ArrayList<>();

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

            JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.setIndent("  ");

            Gson gson = new Gson();

            for (String key : definitions.keySet()) {
                String value = definitions.get(key);
                if (value.length() == 0) {
                    LOGGER.error("The key {} has no value", key);
                }

                GlossarioEntry glossarioEntry = new GlossarioEntry(key, value);
                entries.add(glossarioEntry);
            }

            gson.toJson(entries, writer);
//            jsonWriter.close();
            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
