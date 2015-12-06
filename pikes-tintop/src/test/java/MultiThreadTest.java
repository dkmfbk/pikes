import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import is2.data.Parse;
import is2fbk.data.SentenceData09;
import is2fbk.parser.Options;
import is2fbk.parser.Parser;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.pipeline.Pipeline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Created by alessio on 01/12/15.
 */

public class MultiThreadTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadTest.class);
    private SemanticRoleLabeler mateSrl = null;
    private Parser mateParser = null;

    public MultiThreadTest(String annaModel, String srlModel) throws IOException, ClassNotFoundException {
        LOGGER.info("Loading Anna Parser");
        String[] arrayOfString = { "-model", annaModel };
        Options localOptions = new Options(arrayOfString);
        mateParser = new Parser(localOptions);

        LOGGER.info("Loading Mate Srl");
        ZipFile zipFile;
        zipFile = new ZipFile(srlModel);
        mateSrl = Pipeline.fromZipFile(zipFile);
        zipFile.close();
        Language.setLanguage(Language.L.valueOf("eng"));
    }

    private void parseSentencesOneThread(File nafFolder) throws IOException, JDOMException {
        LOGGER.info("Parsing sentences");

        List<SentenceData09> sentences = new ArrayList<>();

        for (File file : nafFolder.listFiles()) {
            if (!file.isFile()) {
                continue;
            }

            if (!file.getName().endsWith(".naf")) {
                continue;
            }

            KAFDocument document = KAFDocument.createFromFile(file);
            for (int sent = 1; sent <= document.getNumSentences(); sent++) {

                List<Term> terms = document.getSentenceTerms(sent);
                List<String> forms = new ArrayList<>();
                List<String> poss = new ArrayList<>();
                List<String> lemmas = new ArrayList<>();

                forms.add("<root>");
                poss.add("<root>");
                lemmas.add("<root>");

                for (int i = 0; i < terms.size(); i++) {
                    Term term = terms.get(i);
                    forms.add(term.getForm());
                    poss.add(term.getMorphofeat());
                    lemmas.add(term.getLemma());
                }

                SentenceData09 localSentenceData091 = new SentenceData09();
                localSentenceData091.init(forms.toArray(new String[forms.size()]));
                localSentenceData091.setPPos(poss.toArray(new String[poss.size()]));
                localSentenceData091.setLemmas(lemmas.toArray(new String[poss.size()]));

                sentences.add(localSentenceData091);

//                Sentence mateSentence = parseSentenceFromNaf(localSentenceData091);
//                File outputFile = new File(file.getAbsolutePath() + "-" + sent + ".conll");
//                if (!outputFile.exists()) {
//                    Files.write(mateSentence.toString(), outputFile, Charsets.UTF_8);
//                }
            }
        }

        sentences.parallelStream().forEach(this::parseSentenceFromNaf);
    }

    private Sentence parseSentenceFromNaf(SentenceData09 localSentenceData091) {
        SentenceData09 localSentenceData092;

        localSentenceData092 = mateParser.apply(localSentenceData091);
        Sentence mateSentence = ParseMateSentences.createSentenceFromAnna33(localSentenceData092, null);
        synchronized (this) {
            mateSrl.parseSentence(mateSentence);
        }
        return mateSentence;
    }

    public static void main(String[] args) {

        String srlModel = "/Users/alessio/Documents/scripts/mateplus/models/retrain-srl-20140818.model";
        String annaModel = "/Users/alessio/Documents/scripts/mateplus/models/retrain-anna-20140819.model";
        File nafFolder = new File("/Users/alessio/Documents/scripts/mateplus/models/naf/");

        try {

            MultiThreadTest test = new MultiThreadTest(annaModel, srlModel);
            test.parseSentencesOneThread(nafFolder);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
