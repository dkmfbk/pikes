package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.depparseannotation.DepParseInfo;
import eu.fbk.dkm.pikes.depparseannotation.DepparseAnnotations;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * Created by alessio on 06/05/15.
 */

public class FakeMstParserAnnotator implements Annotator {

    public FakeMstParserAnnotator(String annotatorName, Properties props) {

    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                SemanticGraph dependencies = sentence.get(
                        SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
                if (dependencies != null) {
                    DepParseInfo info = new DepParseInfo(dependencies);
                    sentence.set(DepparseAnnotations.MstParserAnnotation.class, info);
                }
            }
        } else {
            throw new RuntimeException("unable to find words/tokens in: " + annotation);
        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(PikesAnnotations.MSTPARSE_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requires() {
        return TOKENIZE_SSPLIT_PARSE;
    }
}
