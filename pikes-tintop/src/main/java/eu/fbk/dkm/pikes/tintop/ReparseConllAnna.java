package eu.fbk.dkm.pikes.tintop;

import edu.stanford.nlp.ling.CoreAnnotations;
import eu.fbk.dkm.pikes.resources.util.corpus.Corpus;
import eu.fbk.dkm.pikes.resources.util.corpus.Sentence;
import eu.fbk.dkm.pikes.resources.util.corpus.Word;
import eu.fbk.dkm.pikes.tintop.annotators.AnnotatorUtils;
import eu.fbk.dkm.pikes.tintop.annotators.models.AnnaParseModel;
import is2fbk.data.SentenceData09;
import is2fbk.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alessio on 26/02/15.
 */

public class ReparseConllAnna {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReparseConllAnna.class);

    public static void main(String[] args) {

        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);

        // /Users/alessio/Documents/scripts/mateplus/models/retrain-anna-20140819.model
        File annaModel = new File(args[2]);

        Parser parser = AnnaParseModel.getInstance(annaModel).getParser();


        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            Corpus conll2009 = Corpus.readDocumentFromFile(inputFile, "conll2009");

            conll2009.getSentences().parallelStream().forEach((Sentence sentence) -> {

                List<String> forms = new ArrayList<>();
                List<String> poss = new ArrayList<>();
                List<String> lemmas = new ArrayList<>();

                forms.add("<root>");
                poss.add("<root>");
                lemmas.add("<root>");

                for (Word word : sentence) {
                    String form = word.getForm();
                    String pos = word.getPos();
                    String lemma = word.getLemma();

                    form = AnnotatorUtils.codeToParenthesis(form);
                    lemma = AnnotatorUtils.codeToParenthesis(lemma);
                    pos = AnnotatorUtils.codeToParenthesis(pos);

                    forms.add(form);
                    poss.add(pos);
                    lemmas.add(lemma);
                }

                SentenceData09 localSentenceData091 = new SentenceData09();
                localSentenceData091.init(forms.toArray(new String[forms.size()]));
                localSentenceData091.setPPos(poss.toArray(new String[poss.size()]));

                SentenceData09 localSentenceData092;
                synchronized (parser) {
                    localSentenceData092 = parser.apply(localSentenceData091);
                }

//                System.out.println(Arrays.toString(localSentenceData092.plabels));
//                System.out.println(Arrays.toString(localSentenceData092.pheads));

                String[] plabels = localSentenceData092.plabels;
                for (int i = 0; i < plabels.length; i++) {
                    String plabel = plabels[i];
                    int phead = localSentenceData092.pheads[i];
                    sentence.getWords().get(i).setDepLabel(plabel);
                    sentence.getWords().get(i).setDepParent(phead);
                }

//                System.out.println(sentence.toConllString());

                synchronized (writer) {
                    try {
                        writer.append(sentence.toConllString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

//            for (Sentence sentence : conll2009) {
//
//                StringBuilder stanfordSentenceBuilder = new StringBuilder();
//
//                for (Word word : sentence) {
//                    stanfordSentenceBuilder.append(" ").append(word.getForm().replaceAll("\\s+", "_"));
//                }
//
//                String stanfordSentence = stanfordSentenceBuilder.toString().trim();
//
//                Annotation annotation = new Annotation(stanfordSentence);
//                pipeline.annotate(annotation);
//
//                CoreMap coreMap = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0);
//
//                SemanticGraph dependencies = coreMap
//                        .get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
//                DepParseInfo info = new DepParseInfo(dependencies);
//
//                for (Integer id : info.getDepParents().keySet()) {
//                    sentence.getWords().get(id - 1).setDepParent(info.getDepParents().get(id));
//                }
//                for (Integer id : info.getDepLabels().keySet()) {
//                    sentence.getWords().get(id - 1).setDepLabel(info.getDepLabels().get(id));
//                }
//
//            }

            writer.close();

//            Properties props = new Properties();
//            props.setProperty("annotators", "tokenize, ssplit, lemma");
//            props.setProperty("enforceRequirements", "false");
//
//            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//
//            String sentText = "They_PRP are_VBP hunting_VBG dogs_NNS ._.";
//            String simpleText = "They are hunting dogs .";
//
//            List<CoreLabel> sentence = new ArrayList<>();
//
//            String[] parts = sentText.split("\\s");
//            for (String p : parts) {
//                String[] split = p.split("_");
//                CoreLabel clToken = new CoreLabel();
//                clToken.setValue(split[0]);
//                clToken.setWord(split[0]);
//                clToken.setOriginalText(split[0]);
//                clToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, split[1]);
//                sentence.add(clToken);
//            }
//
//            Annotation s = new Annotation(simpleText);
//            s.set(CoreAnnotations.TokensAnnotation.class, sentence);
//            s.set(CoreAnnotations.TokenBeginAnnotation.class, 0);
//            int tokenOffset = sentence.size();
//            s.set(CoreAnnotations.TokenEndAnnotation.class, tokenOffset);
//            s.set(CoreAnnotations.SentenceIndexAnnotation.class, sentence.size());
//
//            List<CoreMap> sentences = new ArrayList<>();
//            sentences.add(s);
//
//            pipeline.annotate(s);
//
//            System.out.println(sentText);
//            System.out.println();
//
//            List<CoreLabel> tokens = s.get(CoreAnnotations.TokensAnnotation.class);
//
//            for (CoreLabel token : tokens) {
//                System.out.println(token);
//                System.out.println(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
//                System.out.println();
//            }
//
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

//        String onlyText = "G. W. Bush and Bono are very strong supporters of the fight of HIV in Africa. Their March 2002 meeting resulted in a 5 billion dollar aid.";
//        Annotation s = new Annotation(onlyText);
//
//        Annotation myDoc = new Annotation(s);
//        pipeline.annotate(myDoc);
//
//        List<CoreMap> sents = myDoc.get(CoreAnnotations.SentencesAnnotation.class);
//        for (CoreMap thisSent : sents) {
//            SemanticGraph dependencies = thisSent.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
//            DepParseInfo info = new DepParseInfo(dependencies);
//
//            System.out.println(info.getDepParents().size());
//            System.out.println(info.getDepLabels().size());
//
//            System.out.println(dependencies);
//
//			}

    }
}
