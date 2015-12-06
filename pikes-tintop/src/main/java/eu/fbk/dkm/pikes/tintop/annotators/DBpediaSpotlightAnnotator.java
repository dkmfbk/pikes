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
			entities.add(entity);
			for (int i = entity.getStartIndex(); i < entity.getEndIndex(); i++) {
				index.put(i, entity);
			}
		}

		annotation.set(PikesAnnotations.DBpediaSpotlightAnnotations.class, entities);

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
