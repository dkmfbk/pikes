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

    private NormalizeText normText;
    private LexparsConfig lexpars;

    CoreLabelTokenFactory factory = new CoreLabelTokenFactory();

    public TokenPro(String conf_folder) {
        this.normText = new NormalizeText(conf_folder);
        try {
            this.lexpars = new LexparsConfig("ita", conf_folder);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ArrayList<CoreLabel>> analyze(String content) {

        String[] lines = content.split(System.getProperty("line.separator"));

        ArrayList<CoreLabel> thisSent = new ArrayList<>();
        ArrayList<ArrayList<CoreLabel>> out = new ArrayList<>();

        int position = -1;
        int tokenid = 0;
        ArrayList<Integer> tokenposition = new ArrayList<Integer>();
        LinkedList<String> tempTokens = new LinkedList<String>();

        ListIterator<String> wordlp;
        boolean findEndOfSentence = false;
        int sentid = 0;

        for (String line : lines) {

            tempTokens.clear();
            tokenposition.clear();
            if (disableTokenization) {
                tempTokens.add(line);
                //System.err.println(position + " " +line);
                tokenposition.add(position);
            } else {
                // do something with line.
                //start tokenizing depending on the spaces and other rules
                tempTokens = tokenize(line, position, tokenposition);
            }
            //System.err.println(position + "/"+(position+line.length()+1) + " " + line);
            position += line.length() + 1;
            ////////////
            //System.err.println("> "+line);

            /// write the tokenized line in the output file
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

                    //manage the brakes at the beginning of the sentence
                    if ("([{".contains(wTemp.substring(0, 1))) {
                        openParenthesis++;
                    }
                    if (wTemp.contains(")") || wTemp.contains("]") || wTemp.contains("}")) {
                        openParenthesis = openParenthesis - 1;
                        if (openParenthesis < 0) {
                            openParenthesis = 0;
                        }
                    }
                    if (!disableSentenceSplitting &&
                            endSentenceCase == 1 && openParenthesis == 0 &&
                            (openParenthesis > 0 || !normText.isInterpunzione(wTemp.substring(0, 1)))) {
                        //out.append("\n");
                        findEndOfSentence = true;
                    }
                    endSentenceCase = 0;

                    if (wTemp.length() == 0) {
                        out.add(thisSent);
                        tokenid = 0;
                        thisSent = new ArrayList<>();
                    } else {
                        tokenid++;
                        CoreLabel token = factory.makeToken(wTemp, wTemp, position, wTemp.length());
                        thisSent.add(token);
                    }

//                    if (fieldsout.contains("token")) {
//                        out.add(wTemp);
//                    }

//                    if (!disableTokenization) {
//                        out.add(thisSent);
//                        thisSent = new ArrayList<>();
//                    }
                    if (i < tokenposition.size() - 1) {
                        i++;
                        if (!disableSentenceSplitting && (wTemp.length() == 1 && lexpars.hasEndSentenceChar(wTemp))) {
                            //out.append("\n");
                            endSentenceCase = 1;
                        }
                    }
                } else {
                    // EOS end of sentence empty line
                    endSentenceCase = 2;
//                    thisSent.add("\n");
                    out.add(thisSent);
                    tokenid = 0;
                    thisSent = new ArrayList<>();
//                    System.out.println("3");
                    findEndOfSentence = true;
                }
            }

            //EOL end of line empty line
            if (endSentenceCase == 0 && lexpars.hasEndSentenceChar(String.valueOf("\r"))) {
//                thisSent.add("\n");
                out.add(thisSent);
                thisSent = new ArrayList<>();
                findEndOfSentence = true;
                //here you may find if this set as EOS char in the xml with char id = <char id="13" hexcode="0x0d" desc="CARRIAGE RETURN" />
                // EOS,EOL,emptyline,another EOL
            }
        }

        if (thisSent.size() > 0) {
            out.add(thisSent);
            thisSent = new ArrayList<>();
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
                //System.err.println("line "+charPosition + "/"+matcher.start()+" "+line);
                subline = line.substring(charPosition, matcher.start());
                stopword = matcher.group().trim();
                charPosition = matcher.end();
                //System.err.println("1>> "+charPosition+" [["+stopword+"]] "+ subline);
            } else {
                subline = line.substring(charPosition, line.length());
                stopword = "";
                charPosition = line.length();
                //System.err.println("2>> "+charPosition+" "+ subline);
            }
            subline = subline.trim();
            for (int chp = 0; chp < subline.length(); chp++) {
                ch = subline.charAt(chp);
                boolean splittedword = lexpars.generalSplittingRules(ch);

                if (!splittedword) {
                    if (tokenposition != null && wordTemp.length() == 0) {
                        tokenposition.add(position + chp + 1);
                    }
                    //if (wrapper.DEBUG)
                    //System.err.println("CHAR: " +ch+ " " + (int) ch + " U+" + String.format("%04x", line.codePointAt(chp)) + " " +lexpars.charSplitter(ch) + " " + lexpars.generalSplittingRules(ch));
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
                    //System.err.println(" {" +tokenNorm + "} " + wTemp);

                    if (tokenNorm.length() > 0) {
                        /// this is to check the abb.list before reconstructing the wordl
                        /*if(!disableSentenceSplitting &&
                                (lexpars.containsEndSentenceChar(wTemp)
                                        //        && lexpars.isAbbreviation(wTemp)
                                )) {
                            tokenNorm = tokenNorm.replaceAll("\\s+","");
                        } */

                        //System.err.println(tokenNorm + " => " + lexpars.containsEndSentenceChar(wTemp) + " " +lexpars.isAbbreviation(wTemp));
                        // reconstruct the list after the spacing!!!
                        //System.out.println("=={"+tokenNorm+"}");
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
                                        //!lexpars.isAbbreviation(teta[i]) &&
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
            //System.err.println(subline.trim()+"\n"+ matcher.group().trim());

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
