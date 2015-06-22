package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import org.annolab.tt4j.TokenHandler;
import org.annolab.tt4j.TreeTaggerWrapper;

import java.io.IOException;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class TreeTaggerPosAnnotator implements Annotator {

	private TreeTaggerWrapper tt;
	protected ArrayList<String> poss;

	public TreeTaggerPosAnnotator(String annotatorName, Properties props) throws IOException {
		System.setProperty("treetagger.home", props.getProperty(annotatorName + ".home"));
		tt = new TreeTaggerWrapper<>();
		tt.setModel(props.getProperty(annotatorName + ".model"));
		tt.setHandler(new TokenHandler<String>() {
			public void token(String token, String pos, String lemma) {

				// Compatibility
				pos = pos.replaceAll("V[BDHV]", "VB");
				pos = pos.replace("IN/that", "IN");

				poss.add(pos);
			}
		});
	}

	@Override
	public void annotate(Annotation annotation) {
		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

				synchronized (this) {
					try {
						poss = new ArrayList<>();
						ArrayList<String> stringTokens = new ArrayList<>();
						for (int i = 0, sz = tokens.size(); i < sz; i++) {
							stringTokens.add(tokens.get(i).originalText());
						}
						tt.process(stringTokens);
						for (int i = 0, sz = tokens.size(); i < sz; i++) {
							CoreLabel thisToken = tokens.get(i);
							String pos = AnnotatorUtils.parenthesisToCode(poss.get(i));
							thisToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, pos);
						}

					} catch (Exception e) {
						e.printStackTrace();
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
		return Collections.singleton(POS_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_AND_SSPLIT;
	}
}
