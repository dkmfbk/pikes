package eu.fbk.dkm.pikes.tintop.annotators;

import javax.annotation.Nullable;
import java.io.File;

/**
 * Created by alessio on 15/01/16.
 */

public class Defaults {

    private static String[] booleanTrue = new String[] {"yes", "1", "y"};
    private static String[] booleanFalse = new String[] {"no", "0", "n"};

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

    public static final String MODEL_FOLDER = "models" + File.separator;
    public static final int MAXLEN = 200;

    public static final int UKB_MAX_NUM_OF_RESTARTS = 50;
    public static final int UKB_MAX_INSTANCES = 10;
    public static final String UKB_FOLDER = "ukb" + File.separator;
    public static final String UKB_MODEL = MODEL_FOLDER + "wnet30_wnet30g_rels.bin";
    public static final String UKB_DICT = MODEL_FOLDER + "wnet30_dict.txt";

    public static final String SEMAFOR_MODEL_DIR = MODEL_FOLDER + "semafor" + File.separator;

    public static final String ANNA_POS_MODEL = MODEL_FOLDER + "anna_pos.model";

    public static final String ANNA_PARSE_MODEL = MODEL_FOLDER + "anna_parse.model";

    public static final String MATE_MODEL = MODEL_FOLDER + "mate.model";
    public static final String MATE_MODEL_BE = MODEL_FOLDER + "mate_be.model";

    public static final String DBPS_ANNOTATOR = "dbpedia-candidates";
    public static final String DBPS_ADDRESS = "http://spotlight.sztaki.hu:2222/rest";
    public static final double DBPSC_MIN_CONFIDENCE = 0.01;
    public static final double DBPSC_FIRST_CONFIDENCE = 0.5;
    public static final double DBPS_MIN_CONFIDENCE = 0.33;
    public static final double ML_CONFIDENCE = 0.5;
}
