package eu.fbk.dkm.pikes.twm;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.List;

/**
 * Created by alessio on 22/09/16.
 */

public class TWMAnnotations {

    public static final String PIKES_DBPS = "dbps";
    public static final Annotator.Requirement DBPS_REQUIREMENT = new Annotator.Requirement(PIKES_DBPS);

    public static final String PIKES_LINKING = "linking";
    public static final Annotator.Requirement LINKING_REQUIREMENT = new Annotator.Requirement(PIKES_LINKING);

    public static class DBpediaSpotlightAnnotation implements CoreAnnotation<LinkingTag> {

        @Override public Class<LinkingTag> getType() {
            return LinkingTag.class;
        }
    }

    public static class LinkingAnnotations implements CoreAnnotation<LinkingList<LinkingTag>> {

        @Override public Class<LinkingList<LinkingTag>> getType() {
            return ErasureUtils.<Class<LinkingList<LinkingTag>>>uncheckedCast(List.class);
        }
    }

}
