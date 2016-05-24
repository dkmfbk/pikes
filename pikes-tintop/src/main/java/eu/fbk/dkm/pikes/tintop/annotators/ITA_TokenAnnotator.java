package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.ita.token.TokenPro;

import java.util.*;

import static org.json.zip.JSONzip.end;

/**
 * Created by alessio on 06/05/15.
 */

public class ITA_TokenAnnotator implements Annotator {

    TokenPro tokenPro = null;

    public ITA_TokenAnnotator(String annotatorName, Properties props) {
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

            int sIndex = 0;
            int tokenIndex = 0;

            for (ArrayList<CoreLabel> sentence : sTokens) {
                if (sentence.size() == 0) {
                    continue;
                }

                CoreMap sent = new ArrayCoreMap(1);
                sent.set(CoreAnnotations.TokensAnnotation.class, sentence);

                sent.set(CoreAnnotations.SentenceIndexAnnotation.class, sIndex++);
                sent.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, sentence.get(0).beginPosition());
                sent.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, sentence.get(sentence.size() - 1).endPosition());

                sent.set(CoreAnnotations.TokenBeginAnnotation.class, tokenIndex);
                tokenIndex += sentence.size();
                sent.set(CoreAnnotations.TokenEndAnnotation.class, tokenIndex);

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
