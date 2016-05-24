package eu.fbk.dkm.pikes.tintop.ita.token;

/**
 * User: Mohammad Qwaider and Christian Girardi
 * Date: 20-mar-2013
 * Time: 12.19.36
 */

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;

public class TokenPro {

    static private boolean disableTokenization = false;
    static private boolean disableSentenceSplitting = false;

    private LexparsConfig lexpars;

    CoreLabelTokenFactory factory = new CoreLabelTokenFactory();

    public TokenPro(String conf_folder) {
        try {
            this.lexpars = new LexparsConfig("ita", conf_folder);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ArrayList<CoreLabel>> analyze(String content) {

        String[] lines = content.split(System.getProperty("line.separator"));

        ArrayList<CoreLabel> thisSent = new ArrayList<>();
        ArrayList<ArrayList<CoreLabel>> out = new ArrayList<>();

        int position = -1;
        ArrayList<Integer> tokenposition = new ArrayList<Integer>();
        LinkedList<String> tempTokens = new LinkedList<String>();

        ListIterator<String> wordlp;

        for (String line : lines) {

            tempTokens.clear();
            tokenposition.clear();
            if (disableTokenization) {
                tempTokens.add(line);
                tokenposition.add(position);
            } else {
                tempTokens = tokenize(line, position, tokenposition);
            }
            position += line.length() + 1;

            wordlp = tempTokens.listIterator();
            int i = 0;
            int endSentenceCase = 0;
            int openParenthesis = 0;

            while (wordlp.hasNext()) {
                String wTemp = wordlp.next();
                if (!wTemp.equals("\n")) {
                    if (disableTokenization && wTemp.length() == 0) {
                        i++;
                        continue;
                    }

                    if ("([{".contains(wTemp.substring(0, 1))) {
                        openParenthesis++;
                    }
                    if (wTemp.contains(")") || wTemp.contains("]") || wTemp.contains("}")) {
                        openParenthesis = openParenthesis - 1;
                        if (openParenthesis < 0) {
                            openParenthesis = 0;
                        }
                    }
                    endSentenceCase = 0;

                    if (wTemp.length() == 0) {
                        out.add(thisSent);
                        thisSent = new ArrayList<>();
                    } else {
                        CoreLabel token = factory.makeToken(wTemp, wTemp, position, wTemp.length());
                        token.setIndex(thisSent.size() + 1);
                        token.setSentIndex(out.size());
                        thisSent.add(token);
                    }

                    if (i < tokenposition.size() - 1) {
                        i++;
                        if (!disableSentenceSplitting && (wTemp.length() == 1 && lexpars.hasEndSentenceChar(wTemp))) {
                            endSentenceCase = 1;
                        }
                    }
                } else {
                    endSentenceCase = 2;
                    out.add(thisSent);
                    thisSent = new ArrayList<>();
                }
            }

            if (endSentenceCase == 0 && lexpars.hasEndSentenceChar(String.valueOf("\r"))) {
                out.add(thisSent);
                thisSent = new ArrayList<>();
            }
        }

        if (thisSent.size() > 0) {
            out.add(thisSent);
//            thisSent = new ArrayList<>();
        }

        return out;

    }

    public LinkedList<String> tokenize(String line, int position, List<Integer> tokenposition) {
        char ch;
        boolean splittedchar = false;
        line = " " + line;
        StringBuffer wordTemp = new StringBuffer();
        LinkedList<String> tempTokens = new LinkedList<String>();
        Matcher matcher = lexpars.getAbbreviationMatcher(line);

        if (line.trim().length() == 0) {
            return tempTokens;
        }

        int charPosition = 0;
        String subline = "";
        String stopword = "";
        while (true) {
            if (matcher != null && matcher.find()) {
                subline = line.substring(charPosition, matcher.start());
                stopword = matcher.group().trim();
                charPosition = matcher.end();
            } else {
                subline = line.substring(charPosition, line.length());
                stopword = "";
                charPosition = line.length();
            }
            subline = subline.trim();
            for (int chp = 0; chp < subline.length(); chp++) {
                ch = subline.charAt(chp);
                boolean splittedword = lexpars.generalSplittingRules(ch);

                if (!splittedword) {
                    if (tokenposition != null && wordTemp.length() == 0) {
                        tokenposition.add(position + chp + 1);
                    }
                    if (!splittedchar) {
                        splittedchar = lexpars.charSplitter(ch);
                    }

                    if (lexpars.charSplitter(ch)) {
                        wordTemp.append(" ").append(String.valueOf(ch)).append(" ");
                    } else {
                        wordTemp.append(String.valueOf(ch));
                    }
                }

                if (splittedword || chp == subline.length() - 1) {
                    String wTemp = wordTemp.toString().replaceAll("\\s+", " ").trim();
                    String tokenNorm = lexpars.checkSplitRules(wTemp);

                    if (tokenNorm.length() > 0) {
                        String[] teta = tokenNorm.split(" ");
                        if (teta.length > 1) {
                            for (int i = 0; i < teta.length; i++) {
                                if (teta[i].length() > 0) {
                                    tempTokens.addLast(teta[i]);
                                    if (tokenposition != null && i < teta.length - 1) {
                                        tokenposition.add(tokenposition.get(tokenposition.size() - 1) + teta[i].trim()
                                                .length());
                                    }
                                }
                                if (i == teta.length - 1 &&
                                        !disableSentenceSplitting &&
                                        lexpars.hasEndSentenceChar(
                                                String.valueOf(teta[i].charAt(teta[i].length() - 1)))) {
                                    tempTokens.addLast("\n");
                                }
                            }
                        } else {
                            tempTokens.addLast(tokenNorm);
                        }

                    }
                    wordTemp.setLength(0);
                    splittedchar = false;
                }
            }

            if (stopword.length() > 0) {
                tempTokens.addLast(stopword);
                if (tokenposition != null) {
                    tokenposition.add(position);
                }
            }

            if (charPosition >= line.length()) {
                break;
            }
        }

        return tempTokens;
    }

    public static void main(String[] args) throws JAXBException, IOException {
        TokenPro tokenizer = new TokenPro("/Users/alessio/Documents/scripts/textpro-ita/giovanni/conf");
        ArrayList<ArrayList<CoreLabel>> tokens = tokenizer.analyze("Non dire gatto se non ce l'hai nel sacco. :D");
        System.out.println(tokens);
    }

}
