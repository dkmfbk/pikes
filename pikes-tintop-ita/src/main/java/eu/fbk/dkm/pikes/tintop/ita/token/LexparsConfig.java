/**
 *
 */
package eu.fbk.dkm.pikes.tintop.ita.token;

import eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.CharSplitter.GeneralRules.Char;
import eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.CharSplitter.LanguageRest;
import eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.KnownWordsList.LanguageRes;
import eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.KnownWordsList.LanguageRes.Expression;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: Mohammad Qwaider
 * Date: 20-mar-2013
 * Time: 12.19.36
 */

/*
 * language should be set up
 */
public class LexparsConfig {

    Tokenizer myTokenizer;
    BitSet charids = new BitSet();
    BitSet gensplitrules = new BitSet();
    Pattern knownWordsListPatterns = null;
    ArrayList<Expression> knownWordsList = new ArrayList<Expression>();

    Pattern endOfSentencePatterns = null;
    Pattern abbreviationPatterns = null;

    public LexparsConfig(String language, String conf_folder) throws IOException, JAXBException {
        this.readConfigFile(conf_folder);
        // collect the splitting chars
        charids = myTokenizer.getSplittingRules().getCharSplitter().getGeneralRules().getCharIds();
        Iterator<LanguageRest> langRest = myTokenizer.getSplittingRules().getCharSplitter().getLanguageRest()
                .iterator();
        while (langRest.hasNext()) {
            LanguageRest langRestmp = langRest.next();
            String languages[] = { langRestmp.getId() };
            if (checkLang(language, languages)) {
                Iterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.CharSplitter.LanguageRest.Char> langchl = langRestmp
                        .getChar().iterator();
                while (langchl.hasNext()) {
                    eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.CharSplitter.LanguageRest.Char langchtmp = langchl
                            .next();
                    charids.set(langchtmp.getId());

                }
            }
        }

        // collect the splitting rules
        Iterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.GeneralSplittingRules.Char> chrl = myTokenizer
                .getSplittingRules().getGeneralSplittingRules().getChar().iterator();
        while (chrl.hasNext()) {
            eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.GeneralSplittingRules.Char chrtmp = chrl.next();
            gensplitrules.set(chrtmp.getId());
        }

        StringBuffer patterns = new StringBuffer();
        Iterator<LanguageRes> langRes = myTokenizer.getSplittingRules().getKnownWordsList().getLanguageRes().iterator();
        LanguageRes langRestmp;
        Iterator<Expression> iter;
        while (langRes.hasNext()) {
            langRestmp = langRes.next();
            String languages[] = { langRestmp.getId() };
            if (checkLang(language, languages)) {
                iter = langRestmp.getExpression().iterator();
                while (iter.hasNext()) {
                    Expression expr = iter.next();
                    patterns.append("|").append(expr.getFind());
                    knownWordsList.add(expr);
                }
            }
        }
        if (patterns.length() > 0) {
            //System.err.println(abbrs.toString());
            knownWordsListPatterns = Pattern.compile(patterns.toString().substring(1));
        }
        //System.err.println("splitrules.size() " + splitrules.size());

        //collect the end of sentence patterns
        patterns.setLength(0);
        ListIterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.EndSentenceChars.LanguageRes> ewcl = myTokenizer
                .getEndSentenceChars().getLanguageRes().listIterator();
        while (ewcl.hasNext()) {
            eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.EndSentenceChars.LanguageRes ewcltmp = ewcl
                    .next();
            String languages[] = { ewcltmp.getId() };
            if (checkLang(language, languages)) {
                ListIterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.EndSentenceChars.LanguageRes.Expression> eexp = ewcltmp
                        .getExpression().listIterator();
                while (eexp.hasNext()) {
                    patterns.append("|").append(eexp.next().getFind());
                }
                ListIterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.EndSentenceChars.LanguageRes.Char> charl = ewcltmp
                        .getChar().listIterator();
                while (charl.hasNext()) {
                    patterns.append("|").append("\\").append(String.valueOf((char) charl.next().getId()));
                }
            }
        }
        if (patterns.length() > 0) {
            endOfSentencePatterns = Pattern.compile(patterns.toString().substring(1));
        }

        //abbreviations
        ListIterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.AbbreviationList.LanguageRes> langl =
                myTokenizer.getAbbreviationList().getLanguageRes().listIterator();
        while (langl.hasNext()) {
            eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.AbbreviationList.LanguageRes langtmp = langl.next();
            String languages[] = { langtmp.getId() };
            if (checkLang(language, languages)) {
                StringBuffer abbrs = new StringBuffer();
                ListIterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.AbbreviationList.LanguageRes.Expression> expl = langtmp
                        .getExpression().listIterator();
                while (expl.hasNext()) {
                    eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.AbbreviationList.LanguageRes.Expression exptmp = expl
                            .next();
                    abbrs.append("|").append(exptmp.getFind().replaceAll("\\.", "\\\\."));
                }

                ListIterator<String> abbl = langtmp.getAbbreviation().listIterator();
                while (abbl.hasNext()) {
                    //String abbtmp = structAbbWithcharSplitterRule(abbl.next(), language);
                    //abbrs.append("|").append(abbtmp.replaceAll("\\.","\\\\.").trim());
                    String abb = abbl.next().replaceAll("\\.", "\\\\.");
                    abbrs.append("|\\s+").append(abb);
                    //if (abb.substring(0,1).toUpperCase().equals(abb.substring(0,1)))
                    //    abbrs.append("|^").append(abb);
                    // System.out.println(word.trim()+"="+abbtmp+"=>"+word.equalsIgnoreCase(abbtmp));
                }

                if (abbrs.length() > 0) {
                    //System.err.println("ABBR: " +abbrs.toString().substring(1));
                    abbreviationPatterns = Pattern.compile(abbrs.toString().substring(1));
                }
            }

        }

    }

    boolean checkLang(String currentLanguage, String[] languages) {
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equalsIgnoreCase(currentLanguage)) {
                return true;
            }
        }
        return false;
    }

    public boolean generalSplittingRules(int character) {
        return gensplitrules.get(character);
    }

    public boolean charSplitter(int character) {
        return charids.get(character);
    }

    String checkSplitRules(String str) {
        if (knownWordsListPatterns != null) {
            Matcher matcher = knownWordsListPatterns.matcher(str);
            if (matcher.find()) {
                for (Expression expr : knownWordsList) {
                    try {
                        //System.err.println("=> (" + str + ") " +expr.getFind());
                        Pattern pattern = Pattern.compile(expr.getFind());

                        if (!expr.getFind().contains(" ") && expr.getReplaceTo() == null) {
                            String strnorm = str.replaceAll("\\s+", "");
                            Matcher matcher2 = pattern.matcher(strnorm);
                            String res = "";
                            int pos = 0;
                            while (matcher2.find()) {

                                String item = matcher2.group();
                                res += strnorm.substring(pos, strnorm.indexOf(item)) + " " + item;
                                pos = strnorm.indexOf(item) + item.length();

                            }
                            if (res.length() > 0) {
                                res += " " + strnorm.substring(pos);
                                //Syst  em.err.println("+ " +res);
                                return res.trim();
                            }

                            //str = str.replaceAll("\\s+","").replaceAll(expr.getFind(), expr.getReplaceTo());
                        } else {
                            Matcher matcher2 = pattern.matcher(str);
                            String strnew = "";
                            while (matcher2.find()) {
                                strnew = str.replaceFirst(expr.getFind(), expr.getReplaceTo());
                                //System.err.println("-> (" + str + ") " +expr.getFind());
                                if (strnew.equals(str)) {
                                    break;
                                }
                                str = strnew;
                                matcher2 = pattern.matcher(str);
                            }

                        }
                    } catch (Exception e) {
                        System.err.println("\nERROR! The expression \"" + expr.getFind()
                                + "\" in the conf/tokenization.xml file is not valid.");
                        System.exit(0);
                    }
                }
            }
        }

        return str;

    }

    public String structAbbWithcharSplitterRule(String word, String lang) {
        Iterator<Char> chrl = myTokenizer.getSplittingRules().getCharSplitter().getGeneralRules().getChar().iterator();
        while (chrl.hasNext()) {
            Char chrtmp = chrl.next();
            char a = (char) chrtmp.getId();
            String tmp = String.valueOf(a);
            //System.err.println(word+"="+tmp+"=");

            word = word.replaceAll("\\" + tmp, " \\" + tmp + " ");

        }

        Iterator<LanguageRest> langResl = myTokenizer.getSplittingRules().getCharSplitter().getLanguageRest()
                .iterator();
        while (langResl.hasNext()) {
            LanguageRest langRestmp = langResl.next();
            String languages[] = { langRestmp.getId() };
            if (checkLang(lang, languages)) {
                Iterator<eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.CharSplitter.LanguageRest.Char> langchl = langRestmp
                        .getChar().iterator();
                while (langchl.hasNext()) {
                    eu.fbk.dkm.pikes.tintop.ita.token.Tokenizer.SplittingRules.CharSplitter.LanguageRest.Char langchtmp = langchl
                            .next();
                    char a = (char) langchtmp.getId();
                    String tmp = String.valueOf(a);
                    //System.err.println(word+"="+tmp+"=");

                    word = word.replaceAll("\\" + tmp, " \\" + tmp + " ");

                    // System.err.println(word+"="+tmp);
                }
            }
        }

        return word;
    }

    public Matcher getAbbreviationMatcher(String word) {
        if (abbreviationPatterns != null) {
            return abbreviationPatterns.matcher(word);
        }

        return null;
    }

    public boolean hasEndSentenceChar(String word) {
        Matcher matcher = endOfSentencePatterns.matcher(String.valueOf(word.charAt(word.length() - 1)));
        if (matcher.find() && matcher.start() == 0) {
            return true;
        }
        return false;
    }

    public boolean containsEndSentenceChar(String word) {
        Matcher matcher = endOfSentencePatterns.matcher(word.trim());
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    void readConfigFile(String conf_folder) throws JAXBException, IOException {
        JAXBContext jc = JAXBContext.newInstance("eu.fbk.dkm.pikes.tintop.ita.token");
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        URL url = getClass().getResource("/conf/tokenization.xml");
        File overwrittenFile = new File(conf_folder + File.separator + "tokenization.xml");

        if (overwrittenFile.exists() && overwrittenFile.isFile()) {
            myTokenizer = (Tokenizer) unmarshaller.unmarshal(
                    new InputStreamReader(new FileInputStream(overwrittenFile),
                            "UTF-8"));
        } else if (url != null) {
            myTokenizer = (Tokenizer) unmarshaller.unmarshal(new InputStreamReader(url.openStream(), "UTF-8"));
        } else {
            System.err.println("Error: tokenization.xml file not found!");
        }
    }

}
