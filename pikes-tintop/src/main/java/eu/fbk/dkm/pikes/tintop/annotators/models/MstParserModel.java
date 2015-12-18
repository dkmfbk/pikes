package eu.fbk.dkm.pikes.tintop.annotators.models;

import mst.DependencyParser;
import mst.DependencyPipe;
import mst.DependencyPipe2O;
import mst.ParserOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.lth.cs.srl.SemanticRoleLabeler;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by alessio on 25/05/15.
 */

public class MstParserModel {

    private static MstParserModel instance;
    private DependencyParser parser;
    private static final Logger LOGGER = LoggerFactory.getLogger(MstParserModel.class);

    private MstParserModel(File modelFile) {
        LOGGER.info("Loading model for MST Stacked Parser");

        try {
            ArrayList<String> argsList = new ArrayList<String>();
            argsList.add("test");
            argsList.add("separate-lab");
            argsList.add("model-name:" + modelFile.getAbsolutePath());
            argsList.add("decode-type:proj");
            argsList.add("order:2");
            argsList.add("format:CONLL");
            String[] argsArray = new String[argsList.size()];
            argsList.toArray(argsArray);
            ParserOptions options = new ParserOptions(argsArray);
            DependencyPipe pipe = options.secondOrder ?
                    new DependencyPipe2O(options) : new DependencyPipe(options);
            parser = new DependencyParser(pipe, options);
            parser.loadModel(options.modelName);
            pipe.closeAlphabets();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static MstParserModel getInstance(File posModel) {
        if (instance == null) {
            instance = new MstParserModel(posModel);
        }

        return instance;
    }

    public DependencyParser getLabeler() {
        return parser;
    }
}
