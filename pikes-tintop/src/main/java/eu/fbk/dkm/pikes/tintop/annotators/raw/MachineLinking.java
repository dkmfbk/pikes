package eu.fbk.dkm.pikes.tintop.annotators.raw;

import org.codehaus.jackson.map.ObjectMapper;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public class MachineLinking extends Linking {

    private static String LABEL = "ml-annotate";

    public MachineLinking() {
        super();
    }

    public MachineLinking(Properties properties) {
        super(properties);
    }

    @Override
    public List<LinkingTag> tag(String text) throws Exception {

        ArrayList<LinkingTag> ret = new ArrayList<>();
        Map<String, String> pars;

        pars = new HashMap<>();
        pars.put("min_weight", config.getProperty("min_confidence"));
        pars.put("disambiguation", "1");
        pars.put("topic", "1");
        pars.put("include_text", "0");
        pars.put("image", "0");
        pars.put("class", "1");
        pars.put("app_id", "0");
        pars.put("app_key", "0");
        pars.put("text", text);

        LOGGER.debug("Pars: {}", pars);

        Map<String, Object> userData;
        String output = request(pars);

        ObjectMapper mapper = new ObjectMapper();
        userData = mapper.readValue(output, Map.class);

        LinkedHashMap annotation = (LinkedHashMap) userData.get(new String("annotation"));
        if (annotation != null) {
            ArrayList<LinkedHashMap> keywords = (ArrayList<LinkedHashMap>) annotation.get(new String("keyword"));
            if (keywords != null) {
                for (LinkedHashMap keyword : keywords) {
                    LinkedHashMap sense = (LinkedHashMap) keyword.get("sense");
                    ArrayList dbpClass = (ArrayList) keyword.get("class");
                    ArrayList<LinkedHashMap> ngrams = (ArrayList<LinkedHashMap>) keyword.get("ngram");
                    for (LinkedHashMap ngram : ngrams) {
                        String originalText = (String) ngram.get("form");
                        LinkedHashMap span = (LinkedHashMap) ngram.get("span");

                        Integer start = (Integer) span.get("start");
                        Integer end = (Integer) span.get("end");

                        LinkingTag tag = new LinkingTag(
                                start,
                                String.format("http://dbpedia.org/resource/%s", (String) sense.get("page")),
                                Double.parseDouble(keyword.get("rel").toString()),
                                originalText,
                                end - start,
                                LABEL
                        );
                        if (extractTypes) {
                            tag.addTypesFromML(dbpClass);
                        }
                        ret.add(tag);
                    }
                }
            }
        }

        return ret;
    }

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("address", "http://ml.apnetwork.it/annotate");
        properties.setProperty("min_confidence", "0.5");
        properties.setProperty("timeout", "2000");

        MachineLinking s = new MachineLinking(properties);
        try {
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
