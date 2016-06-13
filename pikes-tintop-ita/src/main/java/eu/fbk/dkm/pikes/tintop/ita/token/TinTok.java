package eu.fbk.dkm.pikes.tintop.ita.token;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessio on 26/05/16.
 */

public class TinTok {

    private static final Logger LOGGER = LoggerFactory.getLogger(TinTok.class);
    static CoreLabelTokenFactory factory = new CoreLabelTokenFactory();

    class CharacterTable {

        public final static char QUOTATION_MARK = '"';

        public final static char APOSTROPHE = '\'';

        public final static char LEFT_PARENTHESIS = '(';

        public final static char RIGHT_PARENTHESIS = ')';

        public final static char LOW_LINE = '_';

        public final static char SPACE = ' ';

        public final static char NUMBER_SIGN = '#';

        public final static char SOLIDUS = '/';

        public final static char VERTICAL_LINE = '|';

        public final static char FULL_STOP = '.';

        //public static final char SPACE = 0x20;

        public static final char HORIZONTAL_TABULATION = 0x0009;

        public static final char LINE_FEED = 0x000a;

        public static final char FORM_FEED = 0x000c;

        public static final char CARRIADGE_RETURN = 0x000d;

        public static final char PLUS_SIGN = '+';
    }

    static String readFile(File path) throws IOException {
        return readFile(path, Charset.defaultCharset());
    }

    static String readFile(File path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(path.toPath());
        return new String(encoded, encoding);
    }

    static boolean isSeparatorChar(char ch) {
        if (ch == CharacterTable.SPACE) {
            return true;
        } else if (ch == CharacterTable.CARRIADGE_RETURN) {
            return true;
        } else if (ch == CharacterTable.LINE_FEED) {
            return true;
        } else if (ch == CharacterTable.HORIZONTAL_TABULATION) {
            return true;
        } else if (ch == CharacterTable.FORM_FEED) {
            return true;
        }

        return false;
    }

    static public List<CoreLabel> tokenList(String text) {
        if (text.length() == 0) {
            return new ArrayList<>();
        }
        List<CoreLabel> list = new ArrayList<CoreLabel>();
        char currentChar = text.charAt(0);
        char previousChar = currentChar;
        int start = 0;
        boolean isCurrentCharLetterOrDigit;
        boolean isPreviousCharLetterOrDigit;

        if (!Character.isLetterOrDigit(currentChar)) {
            if (!isSeparatorChar(currentChar)) {
                list.add(factory.makeToken(new String(new char[] { currentChar }), 0, 1));
            }
        }

        //logger.debug("0\t" + (int) previousChar + "\t<" + previousChar + ">");
        for (int i = 1; i < text.length(); i++) {
            currentChar = text.charAt(i);
            isCurrentCharLetterOrDigit = Character.isLetterOrDigit(currentChar);
            isPreviousCharLetterOrDigit = Character.isLetterOrDigit(previousChar);
            //logger.debug(i + (int) currentChar + "\t<" + currentChar + ">");
            if (isCurrentCharLetterOrDigit) {
                if (!isPreviousCharLetterOrDigit) {
                    start = i;
                }
            } else {
                if (isPreviousCharLetterOrDigit) {
                    // word o number

                    // Check if it is an abbreviation
                    list.add(factory.makeToken(text.substring(start, i), start, i - start));

                    if (!isSeparatorChar(currentChar)) {
                        // otherPageCounter
                        list.add(factory.makeToken(new String(new char[] { currentChar }), i, 1));
                    }
                } else {
                    //otherPageCounter
                    if (!isSeparatorChar(currentChar)) {
                        list.add(factory.makeToken(new String(new char[] { currentChar }), i, 1));
                    }
                }
            }
            previousChar = currentChar;
        }
        if (Character.isLetterOrDigit(previousChar)) {
            list.add(factory.makeToken(text.substring(start, text.length()), start, text.length() - start));
        }

        return list;
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("command")
                    .withHeader("Description of the command")
                    .withOption("i", "input-path", "the base path of the corpus", "DIR",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final File inputPath = cmd.getOptionValue("i", File.class);

            String text = readFile(inputPath);
            List<CoreLabel> tokens = tokenList(text);

            for (CoreLabel token : tokens) {
                System.out.println(token);
            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
