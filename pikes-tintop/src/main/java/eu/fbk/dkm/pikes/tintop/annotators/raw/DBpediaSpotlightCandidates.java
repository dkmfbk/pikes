package eu.fbk.dkm.pikes.tintop.annotators.raw;

import eu.fbk.dkm.pikes.tintop.annotators.Defaults;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public class DBpediaSpotlightCandidates extends Linking {

    private static String LABEL = "dbpedia-candidates";
    private String firstAttemptConfidence;
    private String confidence;

    public DBpediaSpotlightCandidates(Properties properties) {
        super(properties, properties.getProperty("address", Defaults.DBPS_ADDRESS) + "/candidates");
        firstAttemptConfidence = properties
                .getProperty("first_confidence", Double.toString(Defaults.DBPSC_FIRST_CONFIDENCE));
        confidence = properties.getProperty("min_confidence", Double.toString(Defaults.DBPSC_MIN_CONFIDENCE));
    }

    private List<LinkingTag> attempt(Map<String, String> pars) throws IOException {
        ArrayList<LinkingTag> ret = new ArrayList<>();

        Map<String, Object> userData;
        String output = request(pars);
        LOGGER.trace(output);

        ObjectMapper mapper = new ObjectMapper();
        userData = mapper.readValue(output, Map.class);

        LinkedHashMap annotation = (LinkedHashMap) userData.get(new String("annotation"));
        if (annotation != null) {
            ArrayList<LinkedHashMap> surfaceForms = new ArrayList<>();
            Object surfaceFormJson = annotation.get(new String("surfaceForm"));
            if (surfaceFormJson instanceof ArrayList) {
                surfaceForms = (ArrayList) surfaceFormJson;
            } else {
                surfaceForms.add((LinkedHashMap) surfaceFormJson);
            }
            for (LinkedHashMap keyword : surfaceForms) {
                Object res = keyword.get("resource");
                if (res instanceof ArrayList) {
                    ArrayList resources = (ArrayList) res;
                    for (Object resourceObj : resources) {
                        LinkedHashMap resource = (LinkedHashMap) resourceObj;
                        LinkingTag tag = tagFromResource(resource, keyword);
                        ret.add(tag);
                    }
                } else {
                    LinkedHashMap resource = (LinkedHashMap) res;
                    LinkingTag tag = tagFromResource(resource, keyword);
                    ret.add(tag);
                }
            }
        }

        return ret;
    }

    @Override
    public List<LinkingTag> tag(String text) throws Exception {

        ArrayList<LinkingTag> ret = new ArrayList<>();
        HashSet<Integer> offsets = new HashSet<>();
        Map<String, String> pars;

        LOGGER.debug("First attempt with confidence {}", firstAttemptConfidence);
        pars = new HashMap<>();
        pars.put("confidence", firstAttemptConfidence);
        pars.put("text", text);
        List<LinkingTag> firstAttempt = attempt(pars);

        for (LinkingTag tag : firstAttempt) {
            offsets.add(tag.getOffset());
        }

        LOGGER.debug("Second attempt with confidence {}", confidence);
        pars = new HashMap<>();
        pars.put("confidence", confidence);
        pars.put("text", text);
        List<LinkingTag> secondAttempt = attempt(pars);

        for (LinkingTag tag : secondAttempt) {
            if (offsets.contains(tag.getOffset())) {
                ret.add(tag);
            } else {
                tag.setSpotted(false);
                ret.add(tag);
            }
        }

        return ret;
    }

    private LinkingTag tagFromResource(LinkedHashMap resource, LinkedHashMap keyword) {
        String originalText = (String) keyword.get("@name");
        LinkingTag tag = new LinkingTag(
                Integer.parseInt((String) keyword.get("@offset")),
                String.format("http://dbpedia.org/resource/%s", (String) resource.get("@uri")),
                Double.parseDouble((String) resource.get("@finalScore")),
                originalText,
                originalText.length(),
                LABEL
        );
        if (extractTypes) {
            tag.addTypesFromDBpedia((String) resource.get("@types"));
        }
        return tag;
    }

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("address", "https://knowledgestore2.fbk.eu/dbps/rest/candidates");
        properties.setProperty("use_proxy", "0");
        properties.setProperty("proxy_url", "proxy.fbk.eu");
        properties.setProperty("proxy_port", "3128");
        properties.setProperty("min_confidence", "0.01");
        properties.setProperty("timeout", "2000");

        DBpediaSpotlightCandidates s = new DBpediaSpotlightCandidates(properties);
        try {
//            String text = Files.toString(new File("/Users/alessio/Desktop/elastic/test-dbps.txt"), Charsets.UTF_8);
            String text = "First documented in the 13th century, Berlin was the capital of the Kingdom of Prussia (1701–1918), the German Empire (1871–1918), the Weimar Republic (1919–33) and the Third Reich (1933–45).";
            List<LinkingTag> tags = s.tag(text);
            for (LinkingTag tag : tags) {
                System.out.println(tag);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
}
