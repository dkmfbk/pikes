package eu.fbk.dkm.pikes.tintop.annotators;

import eu.fbk.dkm.pikes.tintop.util.POStagset;

import java.util.Map;
import java.util.Properties;

/**
 * Created by alessio on 18/05/15.
 */

public class AnnotatorUtils {

    private static final String[] SUBST_CHARS = { "(", ")", "[", "]", "{", "}" };
    private static final String[] REPLACE_SUBSTS = { "-LRB-", "-RRB-", "-LSB-", "-RSB-", "-LCB-", "-RCB-" };

    public static String parenthesisToCode(String input) {
        if (input != null) {
            for (int i = 0; i < SUBST_CHARS.length; i++) {
                if (input.equals(SUBST_CHARS[i])) {
                    return REPLACE_SUBSTS[i];
                }
            }
        }
        return input;
    }

    public static String codeToParenthesis(String input) {
        if (input != null) {
            for (int i = 0; i < REPLACE_SUBSTS.length; i++) {
                if (input.toUpperCase().equals(REPLACE_SUBSTS[i])) {
                    return SUBST_CHARS[i];
                }
            }
        }
        return input;
    }

    public static String getSimplePos(String pos) {
        String simplePos = POStagset.tagset.get(pos.toUpperCase());
        if (simplePos == null) {
            simplePos = "O";
        }
        return simplePos;
    }
}
