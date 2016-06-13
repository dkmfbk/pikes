package eu.fbk.dkm.pikes.tintop.ita.token;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi and Mohammed Qwaider
 * Date: 14-feb-2013
 * Time: 14.59.43
 */
public class NormalizeText {

    private static final String PUNCTUATION_REGEX = "\\p{Punct}+";
    private static final String EMAIL_REGEX = "^[\\w|\\-|\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    private static final String URL_REGEX =
            "^https?\\:\\/\\/[A-Za-z0-9\\.-]+((\\:\\d+)?\\/\\S*)?.+";
    private static Pattern[] pattern = {};
    private Hashtable<String, String> charCategory = new Hashtable<String, String>();
    private Hashtable<String, String> charNormalize = new Hashtable<String, String>();
    private Hashtable<String, String> charSplitter = new Hashtable<String, String>();

    //class of characters use to write the normal words
    private List typesOfWords = new ArrayList();

    public NormalizeText(String conf_folder) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {

                String action = null;
                //String category2 = null;
                String hexcode = null;

                public void startElement(String uri, String localName, String qName,
                        Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase("CHARS")) {
                        action = attributes.getValue("action");
                    }

                    if (qName.equalsIgnoreCase("CHAR")) {
                        if (attributes.getIndex("hexcode") >= 0) {
                            hexcode = attributes.getValue("hexcode");
                            if (hexcode != null) {
                                hexcode = hexcode.toLowerCase();
                                if (attributes.getIndex("category") >= 0) {
                                    charCategory.put(hexcode, attributes.getValue("category"));
                                }

                                String remove = "";
                                if (attributes.getIndex("removeit") >= 0 &&
                                        attributes.getValue("removeit").equalsIgnoreCase("yes")) {
                                    charNormalize.put(hexcode, "");
                                    remove = "remove";
                                }

                                if (action != null && action.equals("splittoken")) {
                                    charSplitter.put(hexcode, remove);
                                }
                            }

                        }
                    }

                }

                public void endElement(String uri, String localName,
                        String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("CHARS")) {
                        action = null;
                    } else {
                        hexcode = null;
                    }
                }

                public void characters(char ch[], int start, int length) throws SAXException {
                    if (hexcode != null) {
                        charNormalize.put(hexcode, new String(ch, start, length));
                    }
                }
            };

            //load normalization config file
            File overwrittenFile = new File(conf_folder + File.separator + "normalization.xml");
            if (overwrittenFile.exists() && overwrittenFile.isFile()) {
                saxParser.parse(overwrittenFile, handler);
            } else {
                URL url = getClass().getResource("/conf/normalization.xml");

                if (url != null) {
                    saxParser.parse(url.openStream(), handler);
                } else {
                    System.err.println("Error: normalization.xml file not found!");
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        typesOfWords.add(Character.CURRENCY_SYMBOL);
        typesOfWords.add(Character.DECIMAL_DIGIT_NUMBER);
        typesOfWords.add(Character.FORMAT);
        typesOfWords.add(Character.LETTER_NUMBER);
        typesOfWords.add(Character.LOWERCASE_LETTER);
        typesOfWords.add(Character.UPPERCASE_LETTER);
        typesOfWords.add(Character.MODIFIER_SYMBOL);
        typesOfWords.add(Character.NON_SPACING_MARK);
        typesOfWords.add(Character.OTHER_LETTER);
        typesOfWords.add(Character.OTHER_NUMBER);
        typesOfWords.add(Character.OTHER_SYMBOL);
        typesOfWords.add(Character.PRIVATE_USE);
        typesOfWords.add(Character.SURROGATE);
        typesOfWords.add(Character.TITLECASE_LETTER);
        typesOfWords.add(Character.UNASSIGNED);

        //initialize the Pattern object
        pattern = new Pattern[3];
        pattern[0] = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
        pattern[1] = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);
        pattern[2] = Pattern.compile(PUNCTUATION_REGEX, Pattern.CASE_INSENSITIVE);

    }

    public boolean isAccent(int ch) {
        String chstr = charCategory.get(String.format("%04x", ch).toLowerCase());

        if (chstr != null && chstr.equals("accent")) {
            return true;
        }
        return false;
    }

    public boolean isApostrophe(int ch) {
        String chstr = charCategory.get(String.format("%04x", ch).toLowerCase());

        if (chstr != null && chstr.equals("apostrophe")) {
            return true;
        }
        return false;
    }

    public boolean isQuote(int ch) {
        String chstr = charCategory.get(String.format("%04x", ch).toLowerCase());

        if (chstr != null && chstr.equals("quote")) {
            return true;
        }
        return false;
    }

    public boolean isInterpunzione(String str) {
        //if (str.matches(PUNCTUATION_REGEX)) {
        if (Pattern.matches("\\p{Punct}", str) || isQuote(str.codePointAt(0))) {
            // if (charNormalize.containsKey(String.format("%04x", ch).toLowerCase())) {
            return true;
        }
        return false;
    }

    public boolean isCurrency(int ch) {
        String chstr = charCategory.get(String.format("%04x", ch).toLowerCase());

        if (chstr != null && chstr.equals("currency")) {
            return true;
        }
        return false;
    }

    public String normalize(int ch) {
        String chstr = String.format("%04x", ch);
        if (charNormalize.containsKey(chstr)) {
            return charNormalize.get(chstr);
        }
        return hexToString(chstr);
    }

    /*
ABB_
CUR

CAP
PUN
UPP
DIG
LET
LOW

JLD
JLE
OTH

    OrthoFeature
    TokenType

     */
    public String getOrthoType(String str) {
        boolean isDigit = false;
        boolean isAlphaLow = false;
        boolean isAlphaUp = false;
        boolean isCurrency = false;

        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                isDigit = true;
            } else if (Character.isUpperCase(str.charAt(i))) {
                isAlphaUp = true;
            } else if (Character.isLowerCase(str.charAt(i))) {
                isAlphaLow = true;
            } else if (isCurrency(str.codePointAt(i))) {
                isCurrency = true;
            }
        }

        //if (str.matches(EMAIL_REGEX))
        //    return "EMA";
        //else  if (str.matches(URL_REGEX))
        //    return "URL";
        //else
        //if (isCurrency)
        //    return "CUR";
        if (isAlphaUp || isAlphaLow) {
            //if (str.contains("."))
            //    return "ABB";
            //else
            if (isAlphaUp && !isAlphaLow) {
                return "CAP";
            } else if (!isAlphaUp && isAlphaLow) {
                return "LOW";
            } else {
                return "UPP";
            }

        } else if (str.matches(PUNCTUATION_REGEX)) {
            return "PUN";
        } else if (isDigit) {
            return "DIG";
        }

        return "OTH";
    }

    public String normalize(String str) {
        String nrml = Normalizer.normalize(str, Normalizer.Form.NFD);
        String strnorm = "";
        int countUpperChars = 0;
        for (int i = 0; i < nrml.length(); i++) {
            if (isCurrency(nrml.codePointAt(i))) {
                strnorm += "_";
            } else {
                //conto quanti caratteri sono maiuscoli
                if (Character.isLetter(nrml.codePointAt(i)) && Character.isUpperCase(nrml.codePointAt(i))) {
                    countUpperChars++;
                }
                strnorm += normalize(nrml.codePointAt(i));
            }
            //System.err.println(nrml.charAt(i) + " - \\u" + String.format("%04x", nrml.codePointAt(i)));
        }

        if (countUpperChars > 0) {
            //mantengo il case del primo carattere, il resto faccio il lowcase
            //if (nrml.length() == countUpperChars) {
            //    return strnorm.toLowerCase();
            //} else
            if (nrml.length() > 1) {
                return Character.toUpperCase(strnorm.charAt(0)) + strnorm.substring(1).toLowerCase();
            }
            return strnorm.toUpperCase();
        }
        return strnorm.toLowerCase();
    }

    /**
     * encodes a string of 4-digit hex numbers to unicode
     * <br />
     *
     * @param hex string of 4-digit hex numbers
     * @return normal java string
     */
    public static final String hexToString(String hex) {
        if (hex == null || hex.length() > 4) {
            return "";
        }
        try {
            return String.valueOf((char) (Integer.parseInt(hex, 16)));
        } catch (NumberFormatException NF_Ex) { /* dont care*/ }

        return "";
    }

    public static void main(String args[]) throws IOException {
        NormalizeText normalizer = new NormalizeText(args[0]);
        InputStreamReader in = new InputStreamReader(new FileInputStream(args[0]), "UTF8");
        BufferedReader bin = new BufferedReader(in);
        String line;
        while ((line = bin.readLine()) != null) {
            System.out.println(normalizer.normalize(line));
        }
        in.close();
        bin.close();
    }

}
