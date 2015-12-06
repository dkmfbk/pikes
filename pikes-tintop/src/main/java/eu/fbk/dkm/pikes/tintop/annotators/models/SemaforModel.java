package eu.fbk.dkm.pikes.tintop.annotators.models;

import edu.cmu.cs.lti.ark.fn.Semafor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alessio on 25/05/15.
 */

public class SemaforModel {

    private static SemaforModel instance;
    private Semafor parser;
    private static final Logger LOGGER = LoggerFactory.getLogger(SemaforModel.class);

    private SemaforModel(String modelDir) {
        LOGGER.info("Loading model for Semafor");
        try {
            parser = Semafor.getSemaforInstance(modelDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SemaforModel getInstance(String modelDir) {
        if (instance == null) {
            instance = new SemaforModel(modelDir);
        }

        return instance;
    }

    public Semafor getParser() {
        return parser;
    }
}
