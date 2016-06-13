package eu.fbk.dkm.pikes.tintop.ita.token_old;

import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 14/01/15
 * Time: 12.33
 */
public class RemoveGarbage {

    int thresholdLength = 33;
    Hashtable<Integer, Double> alnumRatioThresholds = new Hashtable<Integer, Double>();

    public RemoveGarbage() {
        // 1 => 0,    # single chars can be non-alphanumeric
        alnumRatioThresholds.put(1, 0.0);
        // 2 => 0,    # so can doublets
        alnumRatioThresholds.put(2, 0.0);
        // 3 => 0.32, # at least one of three should be alnum
        alnumRatioThresholds.put(3, 0.32);
        // 4 => 0.24, # at least one of four should be alnum
        alnumRatioThresholds.put(4, 0.24);
        // 5 => 0.39, # at least two of five should be alnum
        alnumRatioThresholds.put(5, 0.39);
    }

    public String applyRule(String line) {
        StringBuilder newline = new StringBuilder();
        String[] words = line.split("\\s+");

        for (String word : words) {
            // Rule 1: "If a string is longer than thresholdLength characters, it is garbage"
            if (word.length() > thresholdLength) {
                word = "<GARBAGE1>" + word + "</GARBAGE1>";
                System.err.println("#1 for " + word);
                continue;
            } else {
                /* Rule 2: "If a string's ratio of alphanumeric characters to total
                    # characters is less than 50%, the string is garbage"
                    #
                    # FIXME: fails 1.1). --- perhaps an applicability threshold on length?
                    # Or perhaps there should be a sliding scale with length, e.g. a 5-char
                    # string is allowed 40% alnum, etc.?

                    #
                    # Idiom: $str =~ tr/x// yields the count of 'x' in $str, without changing
                    # $str.
                     */
                Double word_length = (double) word.length();
                if (word_length > 0) {
                    String tword = word;
                    Double num_alphanumerics = word_length - tword.replaceAll("[a-zA-Z0-9]", "").length();
                    double thresholdRate = 0.49;
                    if (alnumRatioThresholds.containsKey(word_length)) {
                        thresholdRate = alnumRatioThresholds.get(word_length);
                    }

                    Double ratio = new Double(num_alphanumerics / word_length);
                    if (ratio < thresholdRate) {
                        System.err.println("#2 for " + word + ": " + num_alphanumerics + "; total: " + word_length +
                                "; ratio: " + ratio);

                        word = "<GARBAGE2>" + word + "</GARBAGE2>";
                        continue;
                    }

                    /*Rule 3: "If a string has 3 identical alfa characters or joined consonants in a row, it is
                      garbage"
                    */
                    int consonants = 0;
                    int vocals = 0;
                    int other = 0;
                    int nodouble = 0;
                    String prevchar = "";

                    //System.err.println("@3 " + word);
                    final int len = word.length();
                    for (int i = 0; i < len; i++) {
                        String chars = String.valueOf(word.charAt(i)).toLowerCase();
                        if (chars.equals(prevchar)
                                && prevchar.replaceAll("[r|t|o|p|s|d|f|g|l|z|c|v|b|n|m]", "").length() > 0) {
                            nodouble = 1;
                        }
                        if (chars.replaceAll("[q|w|r|t|y|p|s|d|f|g|h|k|l|z|x|c|v|b|n|m]", "").length() == 0) {
                            vocals = 0;
                            other = 0;
                            consonants++;
                        } else if (chars.replaceAll("[a|e|i|o|u|à|è|ì|ò|ù]", "").length() == 0) {
                            other = 0;
                            consonants = 0;
                            vocals++;
                        } else {
                            vocals = 0;
                            consonants = 0;
                            if (chars.replaceAll("[0-9]", "").length() == 0) {
                                other = 0;
                            } else {
                                other++;
                            }
                        }
                        //System.err.println(word+ " -> " + chars + "("+consonants+","+vocals+","+other+")");

                        if (nodouble > 0 || other > 1 || vocals > 3 || consonants > 3) {
                            //System.err.println("#3 for "+word);

                            word = "<GARBAGE3>" + word + "</GARBAGE3>";
                            break;
                        }
                    }

                }
            }
            newline.append(word).append(" ");
        }
        return newline.toString();
    }
}
