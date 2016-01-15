package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.models.MateSrlBeModel;
import eu.fbk.dkm.pikes.tintop.annotators.models.MateSrlModel;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;

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
    int maxLen;

    public MateSrlAnnotator(String annotatorName, Properties props) {

        String model = props.getProperty(annotatorName + ".model", Defaults.MATE_MODEL);
        maxLen = Defaults.getInteger(props.getProperty(annotatorName + ".maxlen"), Defaults.MAXLEN);
        String modelBe = props.getProperty(annotatorName + ".model_be", Defaults.MATE_MODEL_BE);

        labeler = MateSrlModel.getInstance(new File(model)).getLabeler();

        if (modelBe != null) {
            labelerBe = MateSrlBeModel.getInstance(new File(modelBe)).getLabeler();
        }
    }

    public static Sentence createMateSentence(CoreMap stanfordSentence) {
        Sentence ret;

        java.util.List<CoreLabel> get = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);
        int size = get.size();

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

        for (int i = 0; i < get.size(); i++) {
            CoreLabel token = get.get(i);
            forms[i + 1] = token.get(CoreAnnotations.TextAnnotation.class);
            poss[i + 1] = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            lemmas[i + 1] = token.get(CoreAnnotations.LemmaAnnotation.class);
            feats[i + 1] = null;

            labels[i] = token.get(CoreAnnotations.CoNLLDepTypeAnnotation.class);
            parents[i] = token.get(CoreAnnotations.CoNLLDepParentIndexAnnotation.class) + 1;
        }

        ret = new Sentence(forms, lemmas, poss, feats);

        ret.setHeadsAndDeprels(parents, labels);

        return ret;
    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap stanfordSentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {

                Sentence sentence;
                try {
                    sentence = createMateSentence(stanfordSentence);
                } catch (Exception e) {
                    // NullPointerException
                    continue;
                }

                labeler.parseSentence(sentence);

                for (Word word : sentence) {
                    int tokenID = word.getIdx() - 1;
                    if (tokenID < 0) {
                        continue;
                    }
                    try {
                        stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID)
                                .set(PikesAnnotations.MateTokenAnnotation.class, word);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                for (Predicate predicate : sentence.getPredicates()) {
                    int tokenID = predicate.getIdx() - 1;
                    try {
                        stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID)
                                .set(PikesAnnotations.MateAnnotation.class, predicate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (labelerBe != null) {
                    Sentence sentenceBe = createMateSentence(stanfordSentence);
                    labelerBe.parseSentence(sentenceBe);

                    for (Predicate predicate : sentenceBe.getPredicates()) {
                        int tokenID = predicate.getIdx() - 1;
                        String lemma = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID).get(
                                CoreAnnotations.LemmaAnnotation.class);
                        if (lemma.equals("be")) {
                            try {
                                stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID)
                                        .set(PikesAnnotations.MateAnnotation.class, predicate);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } else {
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
