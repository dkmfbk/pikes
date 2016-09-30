package eu.fbk.dkm.pikes.depparseannotation;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.pipeline.Annotator;

/**
 * Created by alessio on 22/09/16.
 */

public class DepparseAnnotations {

    public static final String PIKES_CONLLPARSE = "conll_parse";
    public static final Annotator.Requirement CONLLPARSE_REQUIREMENT = new Annotator.Requirement(PIKES_CONLLPARSE);

    public static class MstParserAnnotation implements CoreAnnotation<DepParseInfo> {

        @Override public Class<DepParseInfo> getType() {
            return DepParseInfo.class;
        }
    }

}
