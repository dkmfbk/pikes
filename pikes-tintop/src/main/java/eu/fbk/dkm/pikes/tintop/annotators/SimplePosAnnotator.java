package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import is2.tag.Tagger;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by alessio on 06/05/15.
 */

public class SimplePosAnnotator implements Annotator {

	private Tagger tagger;

	public SimplePosAnnotator(String annotatorName, Properties props) {
	}

	@Override
	public void annotate(Annotation annotation) {
		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

				for (CoreLabel token : tokens) {
					String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
					if (pos != null) {
						token.set(PikesAnnotations.SimplePosAnnotation.class, AnnotatorUtils.getSimplePos(pos));
					}
				}


			}
		}
		else {
			throw new RuntimeException("unable to find words/tokens in: " + annotation);
		}

	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(PikesAnnotations.SIMPLEPOS_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_SSPLIT_POS;
	}
}
