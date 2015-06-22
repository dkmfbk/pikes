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

public class AnnaPosAnnotator implements Annotator {

	private Tagger tagger;

	public AnnaPosAnnotator(String annotatorName, Properties props) {
		File posModel = new File(props.getProperty(annotatorName + ".model"));
		tagger = AnnaPosModel.getInstance(posModel).getTagger();
	}

	@Override
	public void annotate(Annotation annotation) {
		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

				String[] annaTokens = new String[tokens.size() + 1];
				annaTokens[0] = "<ROOT>";

				for (int i = 0, sz = tokens.size(); i < sz; i++) {
					CoreLabel thisToken = tokens.get(i);
					annaTokens[i + 1] = thisToken.originalText();
				}

				SentenceData09 instance = new SentenceData09();
				instance.init(annaTokens);
				tagger.apply(instance);

				for (int i = 0, sz = tokens.size(); i < sz; i++) {
					CoreLabel thisToken = tokens.get(i);
					String pos = AnnotatorUtils.parenthesisToCode(instance.ppos[i + 1]);
					thisToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, pos);
				}
			}
		}
		else {
			throw new RuntimeException("unable to find words/tokens in: " + annotation);
		}

	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(POS_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_AND_SSPLIT;
	}
}
