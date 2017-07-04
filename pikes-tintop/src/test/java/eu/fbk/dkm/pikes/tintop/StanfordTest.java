package eu.fbk.dkm.pikes.tintop;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.utils.corenlp.CustomAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by alessio on 26/02/15.
 */

public class StanfordTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StanfordTest.class);
    private static final String prefix = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&explaintext=&exlimit=1&titles=";

    private static void printOutput(Annotation annotation) {
        List<CoreMap> sents = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap thisSent : sents) {

            List<CoreLabel> tokens = thisSent.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                System.out.println("Token: " + token);
                System.out.println("Index: " + token.index());
                System.out.println("Sent index: " + token.sentIndex());
                System.out.println("Begin: " + token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class));
                System.out.println("End: " + token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
                System.out.println("NER: " + token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                System.out.println(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                System.out.println(token.get(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class));
                System.out.println(token.get(CoreAnnotations.ValueAnnotation.class));
//                System.out.println(token.get(TimeExpression.Annotation.class));
//                System.out.println(token.get(TimeExpression.TimeIndexAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.DistSimAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.NumericCompositeTypeAnnotation.class));
                System.out.println(token.get(TimeAnnotations.TimexAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.NumericValueAnnotation.class));
//                System.out.println(token.get(TimeExpression.ChildrenAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.NumericTypeAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.ShapeAnnotation.class));
//                System.out.println(token.get(Tags.TagsAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.NumerizedTokensAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.AnswerAnnotation.class));
//                System.out.println(token.get(CoreAnnotations.NumericCompositeValueAnnotation.class));

                System.out.println();
            }

            System.out.println("---");
            System.out.println();
        }
    }

    public static String downloadPage(String sURL) {
        String s = new String();
        try {
            URL obj = new URL(sURL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request for URL: " + sURL);
//            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            s = response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public static void main(String[] args) throws IOException {

        String text;
        text = "Donald Trump set off a fierce new controversy Tuesday with remarks about the right to bear arms that were interpreted by many as a threat of violence against Hillary Clinton.";
        text = "Vladimir \"Vladi\" Luxuria (born Wladimiro Guadagno in Foggia, Apulia, on June 24, 1965) is an Italian actress, writer, politician and television host. Luxuria was a Communist Refoundation Party member of the Italian parliament, belonging to Romano Prodi's L'Unione coalition. She was the first openly transgender member of Parliament in Europe, and the world's second openly transgender MP after New Zealander Georgina Beyer. She lost her seat in the election of April, 2008.\n"
                + "\n"
                + "In the 2006 general election, Luxuria was elected to the Chamber of Deputies by the Lazio 1 constituency in Rome. She lost her seat in the 2008 election. After the retirement of Beyer and Luxuria, there were no transgender MPs reported in the world, until 2011, when Anna Grodzka was elected to the Polish parliament.";

        String page = "Maria Montessori";
        String url = prefix + URLEncoder.encode(page, "UTF-8");
        String rawText = downloadPage(url);

        JsonParser parser = new JsonParser();

        JsonObject o = parser.parse(rawText).getAsJsonObject();
        String id_page = "";
        for (Map.Entry<String, JsonElement> stringJsonElementEntry : o.getAsJsonObject("query").getAsJsonObject("pages").entrySet()) {
            id_page = stringJsonElementEntry.getKey();
            break;
        }
        text = o.getAsJsonObject("query").getAsJsonObject("pages").getAsJsonObject(id_page).get("extract").getAsString();

        Properties props;
        Annotation annotation;

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, wikipediacoref");
        props.setProperty("customAnnotatorClass.wikipediacoref", "eu.fbk.fcw.wikipedia.WikipediaCorefAnnotator");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        annotation = new Annotation(text);
        annotation.set(CoreAnnotations.DocTitleAnnotation.class, "Maria Montessori");
        pipeline.annotate(annotation);

//        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
//            System.out.println(sentence);
//        }

//        HashMultimap<Integer, Integer> simpleCoref = annotation.get(CustomAnnotations.SimpleCorefAnnotation.class);
//        System.out.println(simpleCoref);

//        System.out.println(annotation.get(CoreAnnotations.TextAnnotation.class));

//        Map<Integer, CorefChain> coreferenceGraph = annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
//        for (Object c : coreferenceGraph.keySet()) {
//            CorefChain chain = coreferenceGraph.get(c);
//            Map<IntPair, Set<CorefChain.CorefMention>> mentionMap = chain.getMentionMap();
//
//            System.out.println(mentionMap);
//            for (IntPair p : mentionMap.keySet()) {
//                for (CorefChain.CorefMention m : mentionMap.get(p)) {
//                    System.out.println(m.sentNum);
//                    System.out.println(m.startIndex);
//                    System.out.println(m.endIndex);
//                }
//            }
//        }

//        if (text.length() < 1000) {
//            printOutput(annotation);
//        }

    }
}
