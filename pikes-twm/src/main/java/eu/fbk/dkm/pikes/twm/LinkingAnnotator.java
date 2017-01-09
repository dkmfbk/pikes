package eu.fbk.dkm.pikes.twm;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.utils.core.PropertiesUtils;

import java.io.EOFException;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class LinkingAnnotator implements Annotator {

    Linking tagger;
    public static final String DBPS_ANNOTATOR = "dbpedia-candidates";

    private static HashMap<String, Class<? extends Linking>> annotators = new HashMap<>();

    static {
        annotators.put("dbpedia-candidates", DBpediaSpotlightCandidates.class);
        annotators.put("dbpedia-annotate", DBpediaSpotlightAnnotate.class);
        annotators.put("ml-annotate", MachineLinking.class);
    }

    public LinkingAnnotator(String annotatorName, Properties props) throws Exception {
        Properties newProps = PropertiesUtils.dotConvertedProperties(props, annotatorName);

        String annotator = newProps.getProperty("annotator", DBPS_ANNOTATOR);
        Class<? extends Linking> myClass = annotators.get(annotator);
        Constructor<? extends Linking> myConstructor = myClass.getConstructor(Properties.class);
        tagger = myConstructor.newInstance(newProps);
    }

    @Override
    public void annotate(Annotation annotation) {
        String text = annotation.get(CoreAnnotations.TextAnnotation.class);
        if (text == null) {
            throw new RuntimeException("Text is null");
        }

        List<LinkingTag> tags = new ArrayList<>();
        try {
            tags = tagger.tag(text);
        } catch (EOFException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // ignore
//            throw new RuntimeException(e);
        }

        List<LinkingTag> annotatedEntities = annotation.get(TWMAnnotations.LinkingAnnotations.class);
        List<LinkingTag> entities = new ArrayList<>();
        if (annotatedEntities != null) {
            entities.addAll(annotatedEntities);
        }

        HashMap<Integer, LinkingTag> index = new HashMap<>();
        for (LinkingTag tag : tags) {
            entities.add(tag);
            int end = tag.getOffset() + tag.getLength();
            for (int i = tag.getOffset(); i < end; i++) {
                index.put(i, tag);
            }
        }

        annotation.set(TWMAnnotations.LinkingAnnotations.class, entities);

        if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

                for (CoreLabel token : tokens) {
                    LinkingTag startEntity = index.get(token.beginPosition());
                    if (startEntity == null) {
                        continue;
                    }

                    LinkingTag endEntity = index.get(token.endPosition() - 1);
                    if (endEntity == null) {
                        continue;
                    }

                    if (startEntity.equals(endEntity)) {
                        token.set(TWMAnnotations.DBpediaSpotlightAnnotation.class, startEntity);
                    }
                }
            }
        } else {
            throw new RuntimeException("unable to find words/tokens in: " + annotation);
        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.unmodifiableSet(
                new ArraySet<Requirement>(TWMAnnotations.DBPS_REQUIREMENT, TWMAnnotations.LINKING_REQUIREMENT));
    }

    @Override
    public Set<Requirement> requires() {
        return Annotator.TOKENIZE_AND_SSPLIT;
    }
}
