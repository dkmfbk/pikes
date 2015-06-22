package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.raw.AnnotatedEntity;
import eu.fbk.dkm.pikes.tintop.annotators.raw.DBpediaSpotlight;
import eu.fbk.dkm.pikes.tintop.annotators.raw.DBpediaSpotlightTag;

import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class DBpediaSpotlightAnnotator implements Annotator {

	DBpediaSpotlight tagger;

	public DBpediaSpotlightAnnotator(String annotatorName, Properties props) {
		Properties newProps = AnnotatorUtils.stanfordConvertedProperties(props, annotatorName);
		System.out.println(newProps);
		tagger = new DBpediaSpotlight(newProps);
	}

	@Override
	public void annotate(Annotation annotation) {
		String text = annotation.get(CoreAnnotations.TextAnnotation.class);
		if (text == null) {
			throw new RuntimeException("Text is null");
		}

		List<DBpediaSpotlightTag> tags = new ArrayList<>();
		try {
			tags = tagger.tag(text);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		List<AnnotatedEntity> entities = new ArrayList<>();
		HashMap<Integer, AnnotatedEntity> index = new HashMap<>();
		for (DBpediaSpotlightTag tag : tags) {
			AnnotatedEntity entity = new AnnotatedEntity(tag);
			for (int i = entity.getStartIndex(); i < entity.getEndIndex(); i++) {
				index.put(i, entity);
			}
		}

		if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

				for (CoreLabel token : tokens) {
					AnnotatedEntity startEntity = index.get(token.beginPosition());
					if (startEntity == null) {
						continue;
					}

					AnnotatedEntity endEntity = index.get(token.endPosition() - 1);
					if (endEntity == null) {
						continue;
					}

					if (startEntity.equals(endEntity)) {
						token.set(PikesAnnotations.DBpediaSpotlightAnnotation.class, startEntity);
					}
				}

//
//				String[] annaTokens = new String[tokens.size() + 1];
//				annaTokens[0] = "<ROOT>";
//
//				for (int i = 0, sz = tokens.size(); i < sz; i++) {
//					CoreLabel thisToken = tokens.get(i);
//					annaTokens[i + 1] = thisToken.originalText();
//				}
//
//				SentenceData09 instance = new SentenceData09();
//				instance.init(annaTokens);
//				tagger.apply(instance);
//
//				for (int i = 0, sz = tokens.size(); i < sz; i++) {
//					CoreLabel thisToken = tokens.get(i);
//					String pos = AnnotatorUtils.parenthesisToCode(instance.ppos[i + 1]);
//					thisToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, pos);
//				}
			}
		}
		else {
			throw new RuntimeException("unable to find words/tokens in: " + annotation);
		}
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.unmodifiableSet(new ArraySet<Requirement>(PikesAnnotations.DBPS_REQUIREMENT, PikesAnnotations.LINKING_REQUIREMENT));
	}

	@Override
	public Set<Requirement> requires() {
		return TOKENIZE_AND_SSPLIT;
	}
}
