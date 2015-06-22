package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.models.MateSrlBeModel;
import eu.fbk.dkm.pikes.tintop.annotators.models.MateSrlModel;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Sentence;

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * Created by alessio on 06/05/15.
 */

public class MateSrlAnnotator implements Annotator {

	private SemanticRoleLabeler labeler;
	private SemanticRoleLabeler labelerBe = null;

	public MateSrlAnnotator(String annotatorName, Properties props) {
		File model;

		model = new File(props.getProperty(annotatorName + ".model"));
		labeler = MateSrlModel.getInstance(model).getLabeler();

		String modelBe = props.getProperty(annotatorName + ".model_be");
		if (modelBe != null) {
			model = new File(modelBe);
			labelerBe = MateSrlBeModel.getInstance(model).getLabeler();
		}
	}

	public static Sentence createMateSentence(CoreMap stanfordSentence) {
		Sentence ret;

		int size = stanfordSentence.size();

		String[] forms = new String[size + 1];
		String[] poss = new String[size + 1];
		String[] lemmas = new String[size + 1];
		String[] feats = new String[size + 1];
		String[] labels = new String[size];
		int[] parents = new int[size];

		forms[0] = "<root>";
		poss[0] = "<root>";
		lemmas[0] = "<root>";
		feats[0] = "<root>";

		java.util.List<CoreLabel> get = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);
		for (int i = 0; i < get.size(); i++) {
			CoreLabel token = get.get(i);
			forms[i + 1] = token.get(CoreAnnotations.TextAnnotation.class);
			poss[i + 1] = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
			lemmas[i + 1] = token.get(CoreAnnotations.LemmaAnnotation.class);
			feats[i + 1] = null;

			labels[0] = token.get(CoreAnnotations.CoNLLDepTypeAnnotation.class);
			parents[0] = token.get(CoreAnnotations.CoNLLDepParentIndexAnnotation.class) + 1;
		}

		ret = new Sentence(forms, lemmas, poss, feats);

		ret.setHeadsAndDeprels(parents, labels);

		return ret;
	}

	@Override
	public void annotate(Annotation annotation) {
		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap stanfordSentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				Sentence mateSentence = createMateSentence(stanfordSentence);
				labeler.parseSentence(mateSentence);

				System.out.println(mateSentence.getPredicates());

//				List<String> forms = new ArrayList<>();
//				List<String> poss = new ArrayList<>();
//				List<String> lemmas = new ArrayList<>();
//
//				forms.add("<root>");
//				poss.add("<root>");
//				lemmas.add("<root>");
//
//				for (CoreLabel token : stanfordSentence.get(CoreAnnotations.TokensAnnotation.class)) {
//					String form = token.get(CoreAnnotations.TextAnnotation.class);
//					String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//					String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
//
//					form = AnnotatorUtils.codeToParenthesis(form);
//					lemma = AnnotatorUtils.codeToParenthesis(lemma);
//					pos = AnnotatorUtils.codeToParenthesis(pos);
//
//					forms.add(form);
//					poss.add(pos);
//					lemmas.add(lemma);
//				}
//
//				SentenceData09 localSentenceData091 = new SentenceData09();
//				localSentenceData091.init(forms.toArray(new String[forms.size()]));
//				localSentenceData091.setPPos(poss.toArray(new String[poss.size()]));
//
//				SentenceData09 localSentenceData092;
//				synchronized (this) {
//					localSentenceData092 = parser.apply(localSentenceData091);
//				}
//
//				List<CoreLabel> tokens = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);
//
//				for (int i = 0; i < tokens.size(); i++) {
//					CoreLabel token = tokens.get(i);
//					token.set(CoreAnnotations.CoNLLDepTypeAnnotation.class, localSentenceData092.plabels[i]);
//					token.set(CoreAnnotations.CoNLLDepParentIndexAnnotation.class, localSentenceData092.pheads[i] - 1);
//				}
			}
		}
		else {
			throw new RuntimeException("unable to find words/tokens in: " + annotation);
		}

	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(PikesAnnotations.SRL_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return Collections.singleton(PikesAnnotations.CONLLPARSE_REQUIREMENT);
	}
}
