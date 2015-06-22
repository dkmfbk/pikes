package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.pipeline.Annotator;
import eu.fbk.dkm.pikes.tintop.annotators.raw.AnnotatedEntity;

/**
 * Created by alessio on 27/05/15.
 */

public class PikesAnnotations {

	private PikesAnnotations() {

	}

	public static final String PIKES_SIMPLEPOS = "simple_pos";
	public static final Annotator.Requirement SIMPLEPOS_REQUIREMENT = new Annotator.Requirement(PIKES_SIMPLEPOS);

	public static final String PIKES_WORDNET = "wordnet";
	public static final Annotator.Requirement WORDNET_REQUIREMENT = new Annotator.Requirement(PIKES_WORDNET);

	public static final String PIKES_CONLLPARSE = "conll_parse";
	public static final Annotator.Requirement CONLLPARSE_REQUIREMENT = new Annotator.Requirement(PIKES_CONLLPARSE);

	public static final String PIKES_SRL = "srl";
	public static final Annotator.Requirement SRL_REQUIREMENT = new Annotator.Requirement(PIKES_SRL);

	public static final String PIKES_DBPS = "dbps";
	public static final Annotator.Requirement DBPS_REQUIREMENT = new Annotator.Requirement(PIKES_DBPS);

	public static final String PIKES_LINKING = "linking";
	public static final Annotator.Requirement LINKING_REQUIREMENT = new Annotator.Requirement(PIKES_LINKING);

	public static class UKBAnnotation implements CoreAnnotation<String> {
		public Class<String> getType() {
			return String.class;
		}
	}

	public static class SimplePosAnnotation implements CoreAnnotation<String> {
		public Class<String> getType() {
			return String.class;
		}
	}

	public static class DBpediaSpotlightAnnotation implements CoreAnnotation<AnnotatedEntity> {
		public Class<AnnotatedEntity> getType() {
			return AnnotatedEntity.class;
		}
	}

//	public static class ConllParserAnnotation implements CoreAnnotation<DepPair> {
//		public Class<DepPair> getType() {
//			return DepPair.class;
//		}
//	}

}
