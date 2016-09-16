package eu.fbk.dkm.pikes.tintop.ita.simpserver;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.PikesAnnotations;
import eu.fbk.dkm.pikes.tintop.annotators.raw.LinkingTag;
import eu.fbk.utils.core.FrequencyHashSet;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:30
 * This class convert a raw NAF to a parsed one
 */

public class SimpHandler extends HttpHandler {

    static Logger logger = Logger.getLogger(SimpHandler.class.getName());
    StanfordCoreNLP pipeline;
    HashMap<String, GlossarioEntry> glossario;
    private HashMap<Integer, HashMultimap<String, String>> easyWords;
    static HashSet<String> contentPos = new HashSet<>();
    static HashSet<String> easyPos = new HashSet<>();
    static HashMap<String, String> posDescription = new HashMap<>();
    static Integer SENTENCE_MAX_SIZE = 25;

    static {
        contentPos.add("S");
        contentPos.add("A");
        contentPos.add("V");
        contentPos.add("B");
        easyPos.add("S");
        easyPos.add("V");
        posDescription.put("A", "Adjective");
        posDescription.put("B", "Adverb");
        posDescription.put("S", "Noun");
        posDescription.put("E", "Preposition");
        posDescription.put("C", "Conjunction");
        posDescription.put("P", "Pronoun");
        posDescription.put("R", "Determiner");
        posDescription.put("F", "Punctuation");
        posDescription.put("D", "Adj. (det.)");
        posDescription.put("V", "Verb");
        posDescription.put("X", "Other");
        posDescription.put("N", "Number");
    }

    public void writeOutput(Response response, String contentType, String output) throws IOException {
        response.setContentType(contentType);
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.getWriter().write(output);
    }

    public SimpHandler(StanfordCoreNLP pipeline,
            HashMap<String, GlossarioEntry> glossario,
            HashMap<Integer, HashMultimap<String, String>> easyWords) {
        super();
        this.pipeline = pipeline;
        this.glossario = glossario;
        this.easyWords = easyWords;
    }

    public static List<Integer> findAllOccurrences(String haystack, String needle) {

        List<Integer> ret = new ArrayList<>();

        int index = haystack.indexOf(needle);
        while (index >= 0) {
            try {
                String afterChar = haystack.substring(index + needle.length(), index + needle.length() + 1);
                if (!afterChar.matches("\\w+")) {
                    ret.add(index);
                }
            } catch (Exception e) {
                // ignore
            }
            index = haystack.indexOf(needle, index + 1);
        }

        return ret;
    }

    static class StringLenComparator implements Comparator<String> {

        public int compare(String s1, String s2) {
            return s1.length() - s2.length();
        }
    }

    @Override
    public void service(Request request, Response response) throws Exception {

        long startTime = System.currentTimeMillis();

        logger.debug("Starting service");
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String text = request.getParameter("text");
        text = text.replaceAll("([Aa])rt\\.", "$1rt");
        Annotation annotation = new Annotation(text);

        long annotationTime = System.currentTimeMillis();
        pipeline.annotate(annotation);
        annotationTime = System.currentTimeMillis() - annotationTime;

        long postAnnotationTime = System.currentTimeMillis();

        FrequencyHashSet<String> posStatisticsSimple = new FrequencyHashSet<>();
        FrequencyHashSet<String> posStatistics = new FrequencyHashSet<>();

        int tokenSize = annotation.get(CoreAnnotations.TokensAnnotation.class).size();
        int sentenceSize = annotation.get(CoreAnnotations.SentencesAnnotation.class).size();

        int chars = 0;
        int wordSize = 0;

        int contentWordSize = 0;
        int easyWordSize = 0;
        int level1WordSize = 0;
        int level2WordSize = 0;
        int level3WordSize = 0;

//        for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
//            if (token.get(CoreAnnotations.PartOfSpeechAnnotation.class).startsWith("F")) {
//                continue;
//            }
//            chars += token.endPosition() - token.beginPosition();
//            wordSize++;
//        }

        int documentLengthWithSpaces = text.length();
        int documentLengthWithoutSpaces = text.replaceAll("\\s+", "").length();

        // Do stuff
        StringWriter w = new StringWriter();

        JsonFactory f = new JsonFactory();
        JsonGenerator g = f.createGenerator(w);
        g.writeStartObject();
        g.writeStringField("text", text);

        g.writeArrayFieldStart("sentences");
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            Integer sentenceStartIndex = sentence.get(CoreAnnotations.TokensAnnotation.class).get(0)
                    .get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);

            g.writeStartObject();
            g.writeNumberField("begin", sentenceStartIndex);
            g.writeStringField("text", sentence.get(CoreAnnotations.TextAnnotation.class));
            g.writeArrayFieldStart("tokens");

            int sentenceContentTokens = 0;
            int sentenceEasyContentTokens = 0;
//            int sentenceTokens = 0;
            int sentenceWords = 0;

            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {

                LinkingTag tag = token.get(PikesAnnotations.DBpediaSpotlightAnnotation.class);
                String linkingPage = tag == null ? "" : tag.getPage();
                String linkingPageImage = (tag == null || tag.getImage() == null) ? "" : tag.getImage();
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);

//                sentenceTokens++;
                if (!pos.startsWith("F")) {
                    sentenceWords++;
                    chars += token.endPosition() - token.beginPosition();
                }

                String simplePos = pos.substring(0, 1);
                if (contentPos.contains(simplePos)) {
                    sentenceContentTokens++;

                    if (easyWords.get(1).get(simplePos).contains(lemma)) {
                        level1WordSize++;
                    }
                    if (easyWords.get(2).get(simplePos).contains(lemma)) {
                        level2WordSize++;
                    }
                    if (easyWords.get(3).get(simplePos).contains(lemma)) {
                        level3WordSize++;
                    }
                }
                if (easyPos.contains(simplePos)) {
                    sentenceEasyContentTokens++;
                }

                posStatisticsSimple.add(simplePos);
                posStatistics.add(pos);

                g.writeStartObject();
                g.writeStringField("token", token.get(CoreAnnotations.TextAnnotation.class));
                g.writeStringField("lemma", lemma);
                g.writeStringField("pos", pos);
                g.writeStringField("ner", token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                g.writeStringField("linking", linkingPage);
                g.writeStringField("linkingImage", linkingPageImage);
                g.writeNumberField("begin", token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class));
                g.writeNumberField("end", token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
                g.writeEndObject();
            }

            wordSize += sentenceWords;
            contentWordSize += sentenceContentTokens;
            easyWordSize += sentenceEasyContentTokens;

            g.writeEndArray();

            g.writeBooleanField("tooLong", sentenceWords > SENTENCE_MAX_SIZE);

            // Glossario stuff

//            HashMap<Integer, String> forms = new HashMap<>();
            TreeMap<Integer, DescriptionForm> forms = new TreeMap<>();

            String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class);
            HashMap<Integer, Integer> lemmaIndexes = new HashMap<>();
            HashMap<Integer, Integer> tokenIndexes = new HashMap<>();
            StringBuilder buffer = new StringBuilder();
            int i = 0;
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                lemmaIndexes.put(buffer.length(), i);
                tokenIndexes
                        .put(token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) - sentenceStartIndex, i);
                i++;
                buffer.append(token.get(CoreAnnotations.LemmaAnnotation.class)).append(" ");
            }
            String lemmaText = buffer.toString().trim();

            List<String> glossarioKeys = new ArrayList<>(glossario.keySet());
            Collections.sort(glossarioKeys, new StringLenComparator());

            for (String form : glossarioKeys) {

                int numberOfTokens = form.split("\\s+").length;
                List<Integer> allOccurrences = findAllOccurrences(sentenceText, form);
                List<Integer> allLemmaOccurrences = findAllOccurrences(lemmaText, form);

                for (Integer occurrence : allOccurrences) {
                    addDescriptionForm(form, tokenIndexes, occurrence, sentence, numberOfTokens, forms);
                }
                for (Integer occurrence : allLemmaOccurrences) {
                    addDescriptionForm(form, lemmaIndexes, occurrence, sentence, numberOfTokens, forms);
                }
            }

//            forms.values().forEach(System.out::println);
            g.writeFieldName("descriptions");
            g.writeStartArray();
            for (Integer key : forms.keySet()) {
                DescriptionForm descriptionForm = forms.get(key);
                g.writeStartObject();
                g.writeNumberField("begin", descriptionForm.getStart());
                g.writeNumberField("end", descriptionForm.getEnd());
                g.writeStringField("form", descriptionForm.getDescription().getForms()[0]);
                g.writeStringField("text", descriptionForm.getDescription().getDescription());
                g.writeEndObject();
            }
            g.writeEndArray();

            g.writeEndObject();
        }
        g.writeEndArray();

        double gulpease = 89 + (300 * sentenceSize - 10 * chars) / (wordSize * 1.0);

        g.writeFieldName("statistics");
        g.writeStartObject();

        g.writeNumberField("contentWordSize", contentWordSize);
        g.writeNumberField("contentEasyWordSize", easyWordSize);

        g.writeNumberField("level1WordSize", level1WordSize);
        g.writeNumberField("level2WordSize", level2WordSize);
        g.writeNumberField("level3WordSize", level3WordSize);

//        g.writeNumberField("level1Ratio", level1WordSize * 1.0 / easyWordSize);
//        g.writeNumberField("level2Ratio", level2WordSize * 1.0 / contentWordSize);
//        g.writeNumberField("level3Ratio", level3WordSize * 1.0 / contentWordSize);

        g.writeNumberField("docLenWithSpaces", documentLengthWithSpaces);
        g.writeNumberField("docLenWithoutSpaces", documentLengthWithoutSpaces);
        g.writeNumberField("docLenLettersOnly", chars);

        g.writeNumberField("sentenceCount", sentenceSize);
        g.writeNumberField("tokenCount", tokenSize);
        g.writeNumberField("wordCount", wordSize);

        g.writeNumberField("gulpease", gulpease);

        g.writeFieldName("pos");
        g.writeStartObject();
        for (String key : posStatistics.keySet()) {
            Integer value = posStatistics.get(key);
            g.writeNumberField(key, value);
        }
        g.writeEndObject();

        g.writeFieldName("spos");
        g.writeStartObject();
        for (String key : posStatisticsSimple.keySet()) {
            Integer value = posStatisticsSimple.get(key);
            String posDefinition = posDescription.getOrDefault(key, key);
            g.writeNumberField(posDefinition, value);
        }
        g.writeEndObject();

        g.writeEndObject();

//        g.writeObjectFieldStart("annotation");
//        // g.writeStringField("status", "OK");
//        if (annotateParameter.includesId()) {
//            g.writeStringField("id", annotateParameter.getId());
//        }

        // Call common code!
//        toJSonCommon(g, keywordArray, topicList, locale, serviceTime, cost, annotateParameter);

        g.writeFieldName("timing");
        g.writeStartObject();
        long totalTime = System.currentTimeMillis() - startTime;
        postAnnotationTime = System.currentTimeMillis() - postAnnotationTime;
        g.writeNumberField("total", totalTime);
        g.writeNumberField("annotation", annotationTime);
        g.writeNumberField("post-annotation", postAnnotationTime);
        g.writeStringField("stanford-timing-info", pipeline.timingInformation());
        g.writeEndObject();

        g.writeEndObject();
        g.close();

        writeOutput(response, "text/json", w.toString());
    }

    private void addDescriptionForm(String form, HashMap<Integer, Integer> indexes, int start, CoreMap sentence,
            int numberOfTokens,
            TreeMap<Integer, DescriptionForm> forms) {
        Integer lemmaIndex = indexes.get(start);
        if (lemmaIndex == null) {
            return;
        }

        CoreLabel firstToken = sentence.get(CoreAnnotations.TokensAnnotation.class).get(lemmaIndex);
        CoreLabel endToken = sentence.get(CoreAnnotations.TokensAnnotation.class)
                .get(lemmaIndex + numberOfTokens - 1);
        Integer beginOffset = firstToken.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
        Integer endOffset = endToken.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

        GlossarioEntry glossarioEntry = glossario.get(form);
        if (glossarioEntry == null) {
            return;
        }

        DescriptionForm descriptionForm = new DescriptionForm(
                beginOffset, endOffset, glossarioEntry);

        forms.put(beginOffset, descriptionForm);
    }
}
