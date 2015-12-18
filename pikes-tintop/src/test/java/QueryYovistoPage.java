import com.fasterxml.jackson.databind.ObjectMapper;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.fbk.cit.hlt.htmldownload.util.PageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by alessio on 15/12/15.
 */

public class QueryYovistoPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryYovistoPage.class);
    private static String DEFAULT_USER_AGENT = "FBK evaluation";

    private static String sendGet(String query, String googleKey, String googleCx) throws Exception {

        StringBuffer url = new StringBuffer();
        url.append("https://www.googleapis.com/customsearch/v1?key=");
        url.append(googleKey);
        url.append("&cx=").append(googleCx);
        url.append("&q=").append(URLEncoder.encode(query, "UTF-8"));

        URL obj = new URL(url.toString());
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

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
                return link;
            }
        }

        return null;
    }

    public static void main(String[] args) {
        String htmlFolder = "/Users/alessio/Desktop/elastic/yovisto/html";
        String nafFolder = "/Users/alessio/Desktop/elastic/yovisto/naf";
        String outputFile = "/Users/alessio/Desktop/elastic/yovisto/output.txt";


        HashMap<String, String> titles = new HashMap<>();
        HashSet<String> found = new HashSet<>();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            Iterator<File> fileIterator;
            ArrayList<String> patterns = new ArrayList<>();
            patterns.add(".entry-content");
            ArrayList<String> titlePatterns = new ArrayList<>();
            titlePatterns.add("h1.entry-title");
            ArrayList<String> imgPatterns = new ArrayList<>();
            PageParser pageParser = new PageParser(patterns, imgPatterns);
            PageParser titleParser = new PageParser(titlePatterns, imgPatterns);

            fileIterator = FileUtils.iterateFiles(new File(nafFolder), null, true);
            while (fileIterator.hasNext()) {
                File file = fileIterator.next();
                KAFDocument document = KAFDocument.createFromFile(file);
                String title = document.getFileDesc().title;
                String docID = document.getPublic().publicId;

                if (!docID.startsWith("d")) {
                    continue;
                }

                String link = sendGet(title, "", "");
                if (link != null) {
                    writer.append(docID).append("\t").append(link).append("\n");
                }
                else {
                    LOGGER.error("File not found: " + title);
                }
            }

            writer.close();

//            fileIterator = FileUtils.iterateFiles(new File(htmlFolder), null, true);
//            while (fileIterator.hasNext()) {
//                File file = fileIterator.next();
//                String htmlContent = FileUtils.readFileToString(file, Charsets.UTF_8);
//
//                String article = pageParser.parseHTML(htmlContent);
//                String title = titleParser.parseHTML(htmlContent);
//
//                if (titles.containsKey(title.replaceAll("[^a-zA-Z]", ""))) {
//                    String docID = titles.get(title);
//                    found.add(docID);
////                    System.out.println(titles.get(title));
////                    System.out.println(article);
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        System.out.println(found.size());

//        ArrayList<String> patterns = new ArrayList<>();
//        patterns.add(".entry-content");
//        ArrayList<String> titlePatterns = new ArrayList<>();
//        titlePatterns.add("h1.entry-title");
//        ArrayList<String> imgPatterns = new ArrayList<>();
//        PageParser pageParser = new PageParser(patterns, imgPatterns);
//        PageParser titleParser = new PageParser(titlePatterns, imgPatterns);
//
//        try {
//            String htmlContent = FileUtils.readFileToString(new File(fileName), Charsets.UTF_8);
//
//            String article = pageParser.parseHTML(htmlContent);
//            String title = titleParser.parseHTML(htmlContent);
//
//            article = article.replaceAll("References and Further Reading.*", "");
//            article = article.replaceAll("^(.{1,100} \\([0-9]+-[0-9]+\\))", "$1.");
//
//            System.out.println(title);
//            System.out.println();
//            System.out.println(article);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
