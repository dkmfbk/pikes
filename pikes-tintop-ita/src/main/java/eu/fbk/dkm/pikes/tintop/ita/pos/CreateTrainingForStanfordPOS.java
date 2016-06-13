package eu.fbk.dkm.pikes.tintop.ita.pos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 03/05/16.
 */

public class CreateTrainingForStanfordPOS {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTrainingForStanfordPOS.class);

    public static void main(String[] args) {
//        String input = args[0];
//        String output = args[1];

        String input = "/Users/alessio/Documents/Resources/universal-dependencies-1.2/UD_Italian/it-ud-test.conllu";
        String output = "/Users/alessio/Documents/Resources/universal-dependencies-1.2/UD_Italian/it-ud-test.conllu.stanford";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));

            List<String> lines = Files.readAllLines((new File(input)).toPath());
            StringBuffer lineBuffer = new StringBuffer();

            String multiToken = null;
            StringBuffer multiPos = new StringBuffer();
            Pattern fromPattern = Pattern.compile("^([0-9]+)");
            Pattern endPattern = Pattern.compile("([0-9]+)$");
            Integer from = null;
            Integer end = null;

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("#")) {
                    continue;
                }

                if (line.length() == 0) {
                    writer.append(lineBuffer.toString().trim());
                    writer.append("\n");
//                    System.out.println(lineBuffer.toString().trim());
                    lineBuffer = new StringBuffer();
                    continue;
//                    System.exit(1);
                }
//                    if (tokens.size() > 0) {
//
//                        System.out.println(tokens);
//                        System.out.println(poss);
//
//                        tokens = new HashMap<>();
//                        poss = new HashMap<>();
//
////                        StringBuffer buffer = new StringBuffer();
////                        buffer.append(token);
////                        buffer.append("_");
////                        buffer.append(pos);
////                        buffer.append(" ");
////                        lineBuffer.append(buffer.toString());
//                    }
//
//                    continue;

                String[] parts = line.split("\\s+");

                String id = parts[0];
                String token = parts[1];
                String pos = parts[4];
                Integer numericId = null;

                if (id.contains("-")) {
                    multiToken = token;
                    multiPos = new StringBuffer();
                    Matcher matcher;

                    matcher = fromPattern.matcher(id);
                    if (matcher.find()) {
                        from = Integer.parseInt(matcher.group(1));
                    }
                    matcher = endPattern.matcher(id);
                    if (matcher.find()) {
                        end = Integer.parseInt(matcher.group(1));
                    }

                    continue;
                }

                numericId = Integer.parseInt(id);
                if (end != null && from != null) {
                    if (numericId <= end || numericId >= from) {
                        if (multiPos.length() > 0) {
                            multiPos.append("+");
                        }
                        multiPos.append(pos);
                    }

                    if (numericId.equals(end)) {
                        StringBuilder buffer = new StringBuilder();
                        buffer.append(multiToken);
                        buffer.append("_");
                        buffer.append(multiPos.toString());
                        buffer.append(" ");
                        lineBuffer.append(buffer.toString());

                        multiPos = new StringBuffer();
                        multiToken = null;
                        end = null;
                        from = null;
                    }

                    continue;
                }

                if (token.equals("_")) {
                    LOGGER.error("Error in token {}", token);
                    continue;
                }

                StringBuffer buffer = new StringBuffer();
                buffer.append(token);
                buffer.append("_");
                buffer.append(pos);
                buffer.append(" ");
                lineBuffer.append(buffer.toString());

            }

            writer.append(lineBuffer.toString().trim());
            writer.append("\n");

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
