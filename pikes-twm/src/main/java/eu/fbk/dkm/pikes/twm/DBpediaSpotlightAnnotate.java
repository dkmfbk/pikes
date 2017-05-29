package eu.fbk.dkm.pikes.twm;

import org.codehaus.jackson.map.ObjectMapper;

import java.util.*;

public class DBpediaSpotlightAnnotate extends Linking {

    private static String LABEL = "dbpedia-annotate";
    private String confidence;
    private String allowedTypes;
    private static final String DBPS_ADDRESS = "http://spotlight.sztaki.hu:2222/rest";
    private static final double DBPS_MIN_CONFIDENCE = 0.33;

    public DBpediaSpotlightAnnotate(Properties properties) {
        super(properties, properties.getProperty("address", DBPS_ADDRESS) + "/annotate");
        confidence = properties.getProperty("min_confidence", Double.toString(DBPS_MIN_CONFIDENCE));
        allowedTypes = properties.getProperty("types", null);
    }

    public List<LinkingTag> tag(String text) throws Exception {
        ArrayList<LinkingTag> ret = new ArrayList<>();

        Map<String, String> pars = new HashMap<>();
        pars.put("confidence", confidence);
        if (allowedTypes != null) {
            pars.put("types", allowedTypes);
        }
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
        properties.setProperty("address", "http://model.dbpedia-spotlight.org/en");
        properties.setProperty("min_confidence", "0.05");
        properties.setProperty("types", "Drug,Disease,Chemical_compound");
        properties.setProperty("timeout", "2000");

        DBpediaSpotlightAnnotate s = new DBpediaSpotlightAnnotate(properties);
        try {
            String text = "My doctor has suggested to take a pill of Aspirin.";
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
