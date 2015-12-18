package eu.fbk.dkm.pikes.resources.wes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by alessio on 11/12/15.
 */

public class QuerySolr {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuerySolr.class);

//    private static String nafQueriesFileName = "/Users/alessio/Documents/Resources/wes/wes2015.queries.solr.txt";
//    private static String outputFileName = "/Users/alessio/Documents/Resources/wes/solr-2.txt";
    // Query pattern example "http://dkm-server-1:8983/solr/demo2/select?q=%s&fl=id&df=texttitle&wt=json&indent=true&rows=350"

    private static String DEFAULT_USER_AGENT = "FBK evaluation";

    private static ArrayList<String> sendGet(String query, String agent) throws Exception {
        URL obj = new URL(query);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        ArrayList<String> ret = new ArrayList<>();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", agent);

        int responseCode = con.getResponseCode();
        LOGGER.debug("Queried Google [{}], response code {}", query, responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> root = mapper.readValue(response.toString(), Map.class);
        Map<?, ?> response2 = (Map) root.get("response");
        ArrayList<?> docs = (ArrayList) response2.get("docs");
        if (docs != null) {
            for (Object item : docs) {
                String id = (String) ((Map<?, ?>) item).get("id");
                ret.add(id);
            }
        }

        return ret;
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("query-solr")
                    .withHeader("Send WES queries to a Solr server")
                    .withOption("q", "queries", "CSV file with queries", "FILE", CommandLine.Type.FILE_EXISTING, true,
                            false, true)
                    .withOption("p", "pattern", "Query pattern (use %s as placeholder for the query)", "URL",
                            CommandLine.Type.STRING, true, false, true)
                            // Query pattern example: http://dkm-server-1:8983/solr/demo2/select?q=%s&fl=id&df=texttitle&wt=json&indent=true&rows=350
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("a", "agent", String.format("User agent, default %s", DEFAULT_USER_AGENT), "STRING",
                            CommandLine.Type.STRING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            File outputFile = cmd.getOptionValue("output", File.class);
            File nafQueriesFile = cmd.getOptionValue("queries", File.class);
            String userAgent = cmd.getOptionValue("agent", String.class);
            String queryPattern = cmd.getOptionValue("pattern", String.class);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            List<String> lines = Files.readLines(nafQueriesFile, Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\t");
                String id = parts[0];
                String query = parts[1];

                LOGGER.info(query);
                query = URLEncoder.encode(query, "UTF-8");
                query = String.format(queryPattern, query);
                ArrayList<String> ids = sendGet(query, userAgent);

                writer.append(id);
                for (String s : ids) {
                    writer.append("\t").append(s);
                }
                writer.append("\n");
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
