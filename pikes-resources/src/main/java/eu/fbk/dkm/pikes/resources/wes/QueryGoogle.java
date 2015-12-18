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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 11/12/15.
 */

public class QueryGoogle {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryGoogle.class);
    //    private static String GoogleKey = "";
//    private static String GoogleCx = "";
    private static String DEFAULT_USER_AGENT = "FBK evaluation";

//    private static String nafQueriesFileName = "/Users/alessio/Documents/Resources/wes/wes2015.queries.or.txt";
//    private static String outputFileName = "/Users/alessio/Documents/Resources/wes/google-or.txt";

    private static Pattern wesPattern = Pattern.compile("wes2015\\.(d[0-9]+)\\.naf\\.html");

    private static void sendGet(String query, ArrayList<String> listWithLinks, String googleKey, String googleCx,
            String agent) throws Exception {
        sendGet(query, listWithLinks, googleKey, googleCx, agent, 0);
    }

    private static void sendGet(String query, ArrayList<String> listWithLinks, String googleKey, String googleCx,
            String agent, int start) throws Exception {

        StringBuffer url = new StringBuffer();
        url.append("https://www.googleapis.com/customsearch/v1?key=");
        url.append(googleKey);
        url.append("&cx=").append(googleCx);
        url.append("&q=").append(URLEncoder.encode(query, "UTF-8"));
        if (start > 0) {
            url.append("&start=").append(start);
        }

        URL obj = new URL(url.toString());
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", agent);

        int responseCode = con.getResponseCode();
        LOGGER.info("Queried Google [{}], response code {}", url, responseCode);

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
        ArrayList<?> items = (ArrayList) root.get("items");
        if (items != null) {
            for (Object item : items) {
                String link = (String) ((Map<?, ?>) item).get("link");
                listWithLinks.add(link);
            }
        }

        // Check for next page
        Map<?, ?> queries = (Map) root.get("queries");
        if (queries.containsKey("nextPage")) {
            ArrayList<?> nextPageArray = (ArrayList) queries.get("nextPage");
            Map<?, ?> nextPage = (Map) nextPageArray.get(0);
            int nextStart = (Integer) nextPage.get("startIndex");
            sendGet(query, listWithLinks, googleKey, googleCx, agent, nextStart);
        }
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("query-solr")
                    .withHeader("Send WES queries to a Solr server")
                    .withOption("q", "queries", "CSV file with queries", "FILE", CommandLine.Type.FILE_EXISTING, true,
                            false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("k", "google-key", "Google key", "STRING", CommandLine.Type.STRING, true, false, true)
                    .withOption("c", "google-cx", "Google CX", "STRING", CommandLine.Type.STRING, true, false, true)
                    .withOption("a", "agent", String.format("User agent, default %s", DEFAULT_USER_AGENT), "STRING",
                            CommandLine.Type.STRING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            File outputFile = cmd.getOptionValue("output", File.class);
            File nafQueriesFile = cmd.getOptionValue("queries", File.class);
            String userAgent = cmd.getOptionValue("agent", String.class);

            String googleKey = cmd.getOptionValue("google-key", String.class);
            String googleCx = cmd.getOptionValue("google-cx", String.class);

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

                ArrayList<String> links = new ArrayList<>();
                sendGet(query, links, googleKey, googleCx, userAgent);

                writer.append(id);
                for (String link : links) {
                    Matcher matcher = wesPattern.matcher(link);
                    if (matcher.find()) {
                        writer.append("\t").append(matcher.group(1));
                    }
                }

                writer.append("\n");
            }

            writer.close();

//            sendGet("fame", links);

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
