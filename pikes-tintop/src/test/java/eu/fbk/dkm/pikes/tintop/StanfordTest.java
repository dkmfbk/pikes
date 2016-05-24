package eu.fbk.dkm.pikes.tintop;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * Created by alessio on 26/02/15.
 */

public class StanfordTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StanfordTest.class);

    private static void printOutput(Annotation annotation) {
        List<CoreMap> sents = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap thisSent : sents) {
            List<CoreLabel> tokens = thisSent.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                System.out.println(token);
                System.out.println(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
                System.out.println(token.get(CoreAnnotations.LemmaAnnotation.class));
                System.out.println(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                System.out.println();
            }
            System.out.println("---");
        }
    }

    public static void main(String[] args) {

        String ITAtext = "NEW YORK - L'ultimo colpo di scena della campagna presidenziale potrebbe essere questo: ci si aspettava una convention repubblicana \"contestata\", e se invece toccasse a quella democratica essere segnata letteralmente dalle contestazioni? Le due primarie del Kentucky e Oregon hanno confermato che Bernie Sanders è ancora in gara e non intende mollare. Nel Kentucky Hillary Clinton ha vinto con un margine così microscopico che il risultato assomiglia piuttosto a un pareggio. L'Oregon è andato a Sanders. Donald Trump ha vinto le primarie dell'Oregon. Al miliardario mancano meno di 100 delegati per raggiungere la soglia dei 1.237 necessari per la nomination prima della convention di luglio.\n"
                + "\n"
                + "Ma l'attenzione da ieri si è concentrata sulla \"rissa del Nevada\". Un brutto episodio che segnala l'inasprirsi del confronto tra i due campi democratici. Nel Nevada la primaria è passata da un bel po', si tenne a metà febbraio. Tre mesi dopo il partito democratico si è riunito a livello locale per designare i delegati che andranno alla convention di luglio. Al momento della designazione i rappresentanti di Sanders hanno denunciato brogli e irregolarità. Non si sono fermati lì. Ci sono stati tafferugli, la tensione è salita. Dal campo di Sanders sono partite aggressioni verbali feroci: fino alle minacce fisiche contro la capa locale dal partito e i suoi familiari. Insulti e minacce che sono circolati ampiamente sui social media. Immediata la reazione dell'organizzazione Clinton e anche dei vertici del partito. Hanno chiesto - e alla fine ottenuto - che Sanders si dissociasse dai più virulenti tra i suoi fan, e condannasse le violenze sia fisiche che verbali.\n"
                + "\n"
                + "L'episodio del Nevada rivela una sconcertante divergenza. I democratici, via via che si avvicina la resa dei conti finale, dovrebbero lavorare a riunificare la base per far convergere il massimo di consensi sul candidato vincente (che quasi certamente sarà la Clinton). Invece no, gli animi si scaldano, la temperatura sale, cresce la diffidenza e l'antipatìa tra le due ali del partito. Sanders è deciso a dare battaglia fino in fondo, punta su un colpo di scena finale nella più grossa di tutte le primarie, la California che si esprimerà a giugno. Nel clima rovente, i vertici del partito cominciano a temere che anche la convention di Philadelphia sarà litigiosa. Diversi gruppi radicali già preparano per Philadelphia proteste all'insegna della \"disobbedienza civile\". Col rischio che diventi più complicato poi ricucire, per fare il pieno di voti a novembre.  \n"
                + "\n"
                + "Al contrario, è nel partito repubblicano che ora si segnala un clima di ricompattamento in favore di Donald Trump. I vertici del partito e The Donald hanno raggiunto un accordo sulla raccolta fondi da qui a novembre. Un'altra prova di disgelo l'ha data ieri il più potente dei media di destra, la Fox News di Rupert Murdoch. Ieri è andata in onda la prima puntata dell'attesissimo programma di Megyn Kelly, \"la donna che osò sfidare Trump\". L'anchorwoman nel luglio scorso fu protagonista di un duro scontro con The Donald, al primo dibattito televisivo tra i candidati repubblicani. Dopo aver rivolto una domanda \"cattiva\" a Trump, sul suo atteggiamento verso le donne, l'anchorwoman fu insultata a sua volta. Ma ieri sera l'intervista si è svolta all'insegna della riconciliazione. La Kelly ha cercato di ottenere le scuse di Trump, senza riuscirci veramente. L'atteso duello non c'è stato, la giornalista non ha mai messo alle strette il candidato. Non una sola domanda veramente cattiva, sulle sue posizioni più controverse, sulle sue costanti giravolte in politica estera, sulla mancata presentazione delle sue dichiarazioni dei redditi.\n"
                + "\n"
                + "Insomma anche la Fox sembra seguire il movimento dell'establishment repubblicano, che lentamente si \"converte\" a Trump pur di non ritrovarsi la Clinton alla Casa Bianca. E ieri Trump è riuscito di nuovo a \"spararle grosse\", secondo la tattica che gli è consueta: ha annunciato che una volta eletto presidente sarebbe pronto a incontrarsi col dittatore nordcoreano Kim Jong-un, e che abolirebbe l'intera riforma dei mercati finanziari varata da Barack Obama, la legge Dodd-Frank osteggiata dai banchieri di Wall Street. Trump ha anche dichiarato di essere intenzionato a \"rinegoziare gli accordi sul clima di Parigi\" e dichiarato i suoi redditi: oltre 557 milioni di dollari, senza però entrare nel dettaglio.";
        String ENGtext = "Washington D.C. is the capital of the United States. It was named after George Washington, the first president of the U.S.";

        Properties props;
        Annotation annotation;

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos");

        props.setProperty("ita_toksent.conf_folder", "/Users/alessio/Documents/Resources/ita-models/conf");
        props.setProperty("pos.model", "/Users/alessio/Documents/Resources/ita-models/italian6.tagger");
        props.setProperty("ita_lemma.fstan_command", "/Users/alessio/Documents/Resources/ita-models/MorphoPro/bin/fstan/x86_64/fstan");
        props.setProperty("ita_lemma.fstan_model", "/Users/alessio/Documents/Resources/ita-models/MorphoPro/models/italian-utf8.fsa");
//        props.setProperty("ner.model", "/Users/alessio/Documents/Resources/ita-models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser");

        props.setProperty("customAnnotatorClass.ita_toksent", "eu.fbk.dkm.pikes.tintop.annotators.ITA_TokenAnnotator");
        props.setProperty("customAnnotatorClass.ita_lemma", "eu.fbk.dkm.pikes.tintop.annotators.ITA_LemmaAnnotator");
        StanfordCoreNLP ITApipeline = new StanfordCoreNLP(props);
        annotation = new Annotation(ITAtext);
        ITApipeline.annotate(annotation);
        printOutput(annotation);

        System.exit(1);

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        StanfordCoreNLP ENGpipeline = new StanfordCoreNLP(props);
        annotation = new Annotation(ENGtext);
        ENGpipeline.annotate(annotation);
        printOutput(annotation);


    }
}
