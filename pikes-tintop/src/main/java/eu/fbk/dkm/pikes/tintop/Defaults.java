package eu.fbk.dkm.pikes.tintop;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Properties;

/**
 * Created by alessio on 15/01/16.
 */

public class Defaults {

    private static String[] booleanTrue = new String[] { "yes", "1", "y", "true" };
    private static String[] booleanFalse = new String[] { "no", "0", "n", "false" };

    public static Integer getInteger(@Nullable String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Boolean getBoolean(@Nullable String value, boolean defaultValue) {
        if (value != null) {
            for (String s : booleanTrue) {
                if (value.equalsIgnoreCase(s)) {
                    return true;
                }
            }
            for (String s : booleanFalse) {
                if (value.equalsIgnoreCase(s)) {
                    return false;
                }
            }
        }
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Double getDouble(@Nullable String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Properties classProperties() {
        Properties ret = new Properties();
        ret.setProperty("stanford.customAnnotatorClass.simple_pos",
                "eu.fbk.fcw.wnpos.SimplePosAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.ukb", "eu.fbk.fcw.ukb.UKBAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.conll_parse",
                "eu.fbk.fcw.mate.AnnaParseAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.semafor", "eu.fbk.fcw.semafor.SemaforAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.mate", "eu.fbk.fcw.mate.MateSrlAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.mst_fake",
                "eu.fbk.dkm.pikes.tintop.annotators.FakeMstParserAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.ner_custom",
                "eu.fbk.dkm.pikes.tintop.annotators.NERCustomAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.ner_confidence",
                "eu.fbk.fcw.ner.NERConfidenceAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.dbps", "eu.fbk.dkm.pikes.twm.LinkingAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.ml", "eu.fbk.dkm.pikes.twm.LinkingAnnotator");

        // Unused
        ret.setProperty("stanford.customAnnotatorClass.anna_pos",
                "eu.fbk.fcw.mate.AnnaPosAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.mst_server",
                "eu.fbk.fcw.mst.api.MstServerParserAnnotator");
        ret.setProperty("stanford.customAnnotatorClass.anna_fake",
                "eu.fbk.dkm.pikes.tintop.annotators.FakeAnnaParserAnnotator");
        return ret;
    }

    public static final String MODEL_FOLDER = "models" + File.separator;
    public static final String DEFAULT_URI = "http://untitled/";
    public static final int MAXLEN = 200;
    public static final int MAX_TEXT_LEN = 1000;
    public static final String ANNOTATORS = "tokenize, ssplit, dbps, pos, simple_pos, lemma, ukb, ner_custom, parse, conll_parse, mst_fake, mate, semafor, dcoref";

    public static final String PREDICATE_MATRIX = MODEL_FOLDER + "PredicateMatrix.txt";
    public static final String WN_DICT = "wordnet" + File.separator;

    public static final String ON_FREQUENCIES = MODEL_FOLDER + "on-frequencies.tsv";

    public static void setNotPresent(Properties config) {
        config.setProperty("stanford.annotators", config.getProperty("stanford.annotators", ANNOTATORS));
        config.setProperty("stanford.ner_custom.maxlength",
                config.getProperty("stanford.ner_custom.maxlength", Integer.toString(MAXLEN)));
        config.setProperty("stanford.parse.maxlen",
                config.getProperty("stanford.parse.maxlen", Integer.toString(MAXLEN)));
        config.setProperty("default_uri", config.getProperty("default_uri", DEFAULT_URI));
        config.setProperty("max_text_len", getInteger(config.getProperty("max_text_len"), MAX_TEXT_LEN).toString());
    }
}
