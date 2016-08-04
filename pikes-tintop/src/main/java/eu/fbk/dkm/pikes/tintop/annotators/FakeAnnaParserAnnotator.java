package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by alessio on 06/05/15.
 */

public class FakeAnnaParserAnnotator implements Annotator {

    public FakeAnnaParserAnnotator(String annotatorName, Properties props) {

    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                SemanticGraph dependencies = sentence.get(
                        SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
                DepParseInfo info = new DepParseInfo(dependencies);
                System.out.println(info);
                if (dependencies != null) {
                    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                    for (int i = 0; i < tokens.size(); i++) {
                        CoreLabel token = tokens.get(i);
                        token.set(CoreAnnotations.CoNLLDepTypeAnnotation.class, info.getDepLabels().get(i + 1));
                        token.set(CoreAnnotations.CoNLLDepParentIndexAnnotation.class,
                                info.getDepParents().get(i + 1) - 1);
                    }
                }
            }
        } else {
            throw new RuntimeException("unable to find words/tokens in: " + annotation);
        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(PikesAnnotations.CONLLPARSE_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requires() {
        return TOKENIZE_SSPLIT_POS_DEPPARSE;
    }
}
