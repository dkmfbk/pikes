package eu.fbk.dkm.pikes.resources.conllAIDA;

import java.io.BufferedReader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.BitSet;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.rdfpro.util.IO;
import eu.fbk.utils.core.CommandLine;

public final class KeepOverlappingAnnotations {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeepOverlappingAnnotations.class);

    public static void main(final String... args) {
        try {
            // Parse command line
            final CommandLine cmd = CommandLine.parser().withName("keep-overlapping-annotations")
                    .withHeader("Filters an input CONLL/AIDA file, keeping only annotations "
                            + "overlapping with the ones in a supplied gold standard CONLL/AIDA file")
                    .withOption("i", "input", "the CONLL/AIDA FILE to filter", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("g", "gold",
                            "the gold standard CONLL/AIDA FILE to use as reference", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "the output filtered CONLL/AIDA FILE to generate",
                            "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("d", "dataset", "the dataset format, either conll03 or aida",
                            "FORMAT", CommandLine.Type.STRING, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            // Read options
            final Path goldPath = cmd.getOptionValue("g", Path.class);
            final Path inputPath = cmd.getOptionValue("i", Path.class);
            final Path outputPath = cmd.getOptionValue("o", Path.class);
            final boolean isAida = cmd.getOptionValue("d", String.class, "conll03")
                    .equalsIgnoreCase("aida");

            // Read gold standard (both CONLL03 and AIDA supported)
            int tokenIndex = 0;
            final BitSet goldMask = new BitSet();
            try (BufferedReader in = new BufferedReader(
                    IO.utf8Reader(IO.buffer(IO.read(goldPath.toString()))))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (isTokenLine(line)) {
                        if (getAnnotation(line, isAida) != null) {
                            goldMask.set(tokenIndex, true);
                        }
                        ++tokenIndex;
                    }
                }
            }
            LOGGER.info("Read gold standard {}: {} tokens, {} tokens annotated", goldPath,
                    tokenIndex, goldMask.cardinality());

            // Read input a first time to detect mentions (both CONLL03 and AIDA supported)
            final int[] mentionMask = new int[tokenIndex];
            int annotatedTokenCounter = 0;
            int mentionIndex = 0;
            tokenIndex = 0;
            String previousAnnotation = null;
            try (BufferedReader in = new BufferedReader(
                    IO.utf8Reader(IO.buffer(IO.read(inputPath.toString()))))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String annotation = null;
                    if (isTokenLine(line)) {
                        annotation = getAnnotation(line, isAida);
                        if (annotation != null) {
                            if (tokenIndex == 0 || mentionMask[tokenIndex - 1] == 0
                                    || isBeginToken(annotation, previousAnnotation, isAida)) {
                                ++mentionIndex; // start of new entity mention found
                            }
                            mentionMask[tokenIndex] = mentionIndex;
                            ++annotatedTokenCounter;
                        }
                        ++tokenIndex;
                    }
                    previousAnnotation = annotation;
                }
            }
            final int numInputTokens = tokenIndex;
            final int numInputMentions = mentionIndex;
            LOGGER.info("Read input {}: {} tokens, {} tokens annotated, {} mentions", inputPath,
                    numInputTokens, annotatedTokenCounter, numInputMentions);

            // Remove all mentions (in the mask) that do not overlap with a gold mention
            tokenIndex = 0;
            int removedMentions = 0;
            int removedTokens = 0;
            while (tokenIndex < mentionMask.length) {
                mentionIndex = mentionMask[tokenIndex];
                if (mentionIndex != 0) {
                    final int start = tokenIndex;
                    while (mentionMask[tokenIndex + 1] == mentionIndex) {
                        ++tokenIndex;
                    }
                    boolean overlaps = false;
                    for (int i = start; i <= tokenIndex; ++i) {
                        if (goldMask.get(i)) {
                            overlaps = true;
                            break;
                        }
                    }
                    if (!overlaps) {
                        for (int i = start; i <= tokenIndex; ++i) {
                            mentionMask[i] = 0;
                            ++removedTokens;
                        }
                        ++removedMentions;
                    }
                }
                ++tokenIndex;
            }
            LOGGER.info("Kept {} tokens annotated and {} mentions overlapping with gold standard",
                    annotatedTokenCounter - removedTokens, numInputMentions - removedMentions);

            // Emit output
            tokenIndex = 0;
            removedTokens = 0;
            try (BufferedReader in = new BufferedReader(
                    IO.utf8Reader(IO.buffer(IO.read(inputPath.toString()))))) {
                try (Writer out = IO.utf8Writer(IO.buffer(IO.write(outputPath.toString())))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String modifiedLine = line;
                        if (isTokenLine(line)) {
                            final String annotation = getAnnotation(line, isAida);
                            if (annotation != null && mentionMask[tokenIndex] == 0) {
                                modifiedLine = clearAnnotation(line, isAida);
                                ++removedTokens;
                            }
                            ++tokenIndex;
                        }
                        out.append(modifiedLine).append('\n');
                    }
                }
            }
            LOGGER.info("Emitted {}: {} tokens removed", outputPath, removedTokens);

        } catch (final Throwable ex) {
            // Handle failure
            CommandLine.fail(ex);
        }
    }

    private static boolean isTokenLine(final String line) {
        return !Strings.isNullOrEmpty(line) && !line.startsWith("-DOCSTART-");
    }

    private static boolean isBeginToken(final String annotation,
            @Nullable final String previousAnnotation, final boolean isAida) {
        return previousAnnotation == null || isAida && annotation.equals("B")
                || !isAida && (annotation.startsWith("B-")
                        || !annotation.substring(2).equals(previousAnnotation.substring(2)));
    }

    private static String getAnnotation(final String line, final boolean isAida) {
        if (isAida) {
            final int index = line.indexOf('\t');
            if (index > 0) {
                final int nextIndex = line.indexOf('\t', index + 1);
                if (nextIndex > index) {
                    final String annotation = line.substring(index, nextIndex).trim();
                    return annotation.isEmpty() ? null : annotation;
                }
            }
            return null;
        } else {
            final String tokens[] = line.split("\\s+");
            return tokens[3].equals("O") ? null : tokens[3];
        }
    }

    private static String clearAnnotation(final String line, final boolean isAida) {
        if (isAida) {
            final int index = line.indexOf('\t');
            return index < 0 ? line : line.substring(0, index);
        } else {
            final String tokens[] = line.split("\\s+");
            tokens[3] = "O";
            return String.join(" ", tokens);
        }
    }

}
