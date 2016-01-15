package eu.fbk.dkm.pikes.tintop.annotators.raw;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public class DBpediaSpotlightAnnotate extends Linking {

    private static String LABEL = "dbpedia-annotate";

    public DBpediaSpotlightAnnotate() {
        super();
    }

    public DBpediaSpotlightAnnotate(Properties properties) {
        super(properties);
    }

    public List<LinkingTag> tag(String text) throws Exception {

        ArrayList<LinkingTag> ret = new ArrayList<>();

        Map<String, String> pars = new HashMap<>();
        pars.put("confidence", config.getProperty("min_confidence"));
        pars.put("text", text);

        Map<String, Object> userData;
        String output = request(pars);

        ObjectMapper mapper = new ObjectMapper();
        userData = mapper.readValue(output, Map.class);

        ArrayList<LinkedHashMap> annotation = (ArrayList<LinkedHashMap>) userData.get(new String("Resources"));
        if (annotation != null) {
            for (LinkedHashMap keyword : annotation) {
                String originalText = (String) keyword.get("@surfaceForm");
                if (originalText != null) {
                    LinkingTag tag = new LinkingTag(
                            Integer.parseInt((String) keyword.get("@offset")),
                            (String) keyword.get("@URI"),
                            Double.parseDouble((String) keyword.get("@similarityScore")),
                            originalText,
                            originalText.length(),
                            LABEL
                    );
                    if (extractTypes) {
                        tag.addTypesFromDBpedia((String) keyword.get("@types"));
                    }
                    ret.add(tag);
                }
            }
        }

        return ret;
    }

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("address", "https://knowledgestore2.fbk.eu/dbps/rest/annotate");
        properties.setProperty("use_proxy", "0");
        properties.setProperty("proxy_url", "proxy.fbk.eu");
        properties.setProperty("proxy_port", "3128");
        properties.setProperty("min_confidence", "0.05");
        properties.setProperty("timeout", "2000");

        DBpediaSpotlightAnnotate s = new DBpediaSpotlightAnnotate(properties);
        try {
            String text = Files.toString(new File("/Users/alessio/Desktop/elastic/test-dbps.txt"), Charsets.UTF_8);
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
