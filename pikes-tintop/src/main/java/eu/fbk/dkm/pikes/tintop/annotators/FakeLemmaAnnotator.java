package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.models.AnnaPosModel;
import is2.data.SentenceData09;
import is2.tag.Tagger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by alessio on 06/05/15.
 */

public class FakeLemmaAnnotator implements Annotator {

	public FakeLemmaAnnotator(String annotatorName, Properties props) {
	}

	@Override
	public void annotate(Annotation annotation) {

		int tk = 0;

		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

				for (int i = 0, sz = tokens.size(); i < sz; i++) {
					CoreLabel thisToken = tokens.get(i);
					thisToken.set(CoreAnnotations.LemmaAnnotation.class, thisToken.get(CoreAnnotations.TextAnnotation.class));
				}
			}
		}
		else {
			throw new RuntimeException("unable to find words/tokens in: " + annotation);
		}

	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(LEMMA_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_AND_SSPLIT;
	}
}
