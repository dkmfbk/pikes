package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.depparseannotation.DepParseInfo;
import eu.fbk.dkm.pikes.depparseannotation.DepparseAnnotations;
import eu.fbk.dkm.pikes.tintop.annotators.raw.MstServerParser;

import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class MstServerParserAnnotator implements Annotator {

	MstServerParser parser;
	int maxLen = -1;

	public MstServerParserAnnotator(String annotatorName, Properties props) {
		String server = props.getProperty(annotatorName + ".server");
		Integer port = Integer.parseInt(props.getProperty(annotatorName + ".port"));
		if (props.containsKey(annotatorName + ".maxlen")) {
			maxLen = Integer.parseInt(props.getProperty(annotatorName + ".maxlen"));
		}
		parser = new MstServerParser(server, port);
	}

	@Override
	public void annotate(Annotation annotation) {
		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
				if (maxLen > 0 && tokens.size() > maxLen) {
					continue;
				}

				ArrayList<String> forms = new ArrayList<>();
				ArrayList<String> poss = new ArrayList<>();
				for (CoreLabel stanfordToken : tokens) {
					String form = stanfordToken.get(CoreAnnotations.TextAnnotation.class);
					String pos = stanfordToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);
					forms.add(form);
					poss.add(pos);

				}
				try {
					DepParseInfo depParseInfo = parser.tag(forms, poss);
					sentence.set(DepparseAnnotations.MstParserAnnotation.class, depParseInfo);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		else {
			throw new RuntimeException("unable to find words/tokens in: " + annotation);
		}
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(PikesAnnotations.MSTPARSE_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_SSPLIT_POS;
	}
}
