package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.ita.token.TokenPro;

import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class TP_TokenAnnotator implements Annotator {

    TokenPro tokenPro = null;

    public TP_TokenAnnotator(String annotatorName, Properties props) {
        String confFolder = props.getProperty(annotatorName + ".conf_folder");
        tokenPro = new TokenPro(confFolder);
    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.has(CoreAnnotations.TextAnnotation.class)) {

            List<CoreMap> sentences = new ArrayList<>();

            String text = annotation.get(CoreAnnotations.TextAnnotation.class);
            ArrayList<ArrayList<CoreLabel>> sTokens = tokenPro.analyze(text);
            ArrayList<CoreLabel> tokens = new ArrayList<>();
            for (ArrayList<CoreLabel> sentence : sTokens) {
                CoreMap sent = new ArrayCoreMap(1);
                sent.set(CoreAnnotations.TokensAnnotation.class, sentence);
                sentences.add(sent);
                tokens.addAll(sentence);
            }

            annotation.set(CoreAnnotations.TokensAnnotation.class, tokens);
            annotation.set(CoreAnnotations.SentencesAnnotation.class, sentences);

        } else {
            throw new RuntimeException("Tokenizer unable to find text in annotation: " + annotation);
        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return TOKENIZE_AND_SSPLIT;

    }

    @Override
    public Set<Requirement> requires() {
        return Collections.emptySet();
    }
}
