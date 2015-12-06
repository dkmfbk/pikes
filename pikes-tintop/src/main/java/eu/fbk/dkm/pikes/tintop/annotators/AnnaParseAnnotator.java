package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.models.AnnaParseModel;
import is2fbk.data.SentenceData09;
import is2fbk.parser.Parser;

import java.io.File;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class AnnaParseAnnotator implements Annotator {

	private Parser parser;

	public AnnaParseAnnotator(String annotatorName, Properties props) {
		File posModel = new File(props.getProperty(annotatorName + ".model"));
		parser = AnnaParseModel.getInstance(posModel).getParser();
	}

	@Override
	public void annotate(Annotation annotation) {
		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap stanfordSentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<String> forms = new ArrayList<>();
				List<String> poss = new ArrayList<>();
				List<String> lemmas = new ArrayList<>();

				forms.add("<root>");
				poss.add("<root>");
				lemmas.add("<root>");

				for (CoreLabel token : stanfordSentence.get(CoreAnnotations.TokensAnnotation.class)) {
					String form = token.get(CoreAnnotations.TextAnnotation.class);
					String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
					String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);

					form = AnnotatorUtils.codeToParenthesis(form);
					lemma = AnnotatorUtils.codeToParenthesis(lemma);
					pos = AnnotatorUtils.codeToParenthesis(pos);

					forms.add(form);
					poss.add(pos);
					lemmas.add(lemma);
				}

				SentenceData09 localSentenceData091 = new SentenceData09();
				localSentenceData091.init(forms.toArray(new String[forms.size()]));
				localSentenceData091.setPPos(poss.toArray(new String[poss.size()]));

				SentenceData09 localSentenceData092;
				synchronized (this) {
					localSentenceData092 = parser.apply(localSentenceData091);
				}

				List<CoreLabel> tokens = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);

				for (int i = 0; i < tokens.size(); i++) {
					CoreLabel token = tokens.get(i);
					token.set(CoreAnnotations.CoNLLDepTypeAnnotation.class, localSentenceData092.plabels[i]);
					token.set(CoreAnnotations.CoNLLDepParentIndexAnnotation.class, localSentenceData092.pheads[i] - 1);
				}
			}
		}
		else {
			throw new RuntimeException("unable to find words/tokens in: " + annotation);
		}

	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(PikesAnnotations.CONLLPARSE_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_SSPLIT_POS_LEMMA;
	}
}
