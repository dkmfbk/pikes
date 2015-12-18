import com.google.common.base.Charsets;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.fbk.cit.hlt.htmldownload.util.PageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessio on 15/12/15.
 */

public class ParseYovistoPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseYovistoPage.class);
    private static String DEFAULT_USER_AGENT = "FBK evaluation";

    private static String sendGet(String query) throws Exception {

        StringBuffer url = new StringBuffer();
        url.append(query);

        URL obj = new URL(url.toString());
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public static void main(String[] args) {
        String inputFile = "/Users/alessio/Desktop/elastic/yovisto/google-list.txt";
        String outputFolder = "/Users/alessio/Desktop/elastic/yovisto/naf-new";

        ArrayList<String> patterns = new ArrayList<>();
        patterns.add(".entry-content");
        ArrayList<String> titlePatterns = new ArrayList<>();
        titlePatterns.add("h1.entry-title");
        ArrayList<String> imgPatterns = new ArrayList<>();
        PageParser pageParser = new PageParser(patterns, imgPatterns);
        PageParser titleParser = new PageParser(titlePatterns, imgPatterns);

        File outputFolderFile = new File(outputFolder);

        try {

            if (!outputFolderFile.exists()) {
                outputFolderFile.mkdirs();
            }

            List<String> lines = FileUtils.readLines(new File(inputFile), Charsets.UTF_8);

            for (String line : lines) {
                line = line.trim();

                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }

                LOGGER.info(line);
                String[] parts = line.split("\\s+");

                String idStr = parts[0];
                String externalUrl = parts[1];

                String htmlContent = sendGet(externalUrl);
                String article = pageParser.parseHTML(htmlContent);
                article = article.replaceAll("References and Further Reading.*", "");
                article = article.replaceAll("^(.{1,100} \\([0-9]+-[0-9]+\\))", "$1.");
                article = article.replaceAll("^(.{1,100}[^\\. ])( On )", "$1. $2");

                String title = titleParser.parseHTML(htmlContent);

                KAFDocument document = new KAFDocument("en", "v3");
                KAFDocument.FileDesc fileDesc = document.createFileDesc();
                fileDesc.title = title;
                KAFDocument.Public aPublic = document.createPublic();
                aPublic.publicId = idStr;
                aPublic.uri = externalUrl;

                document.setRawText(title + ".\n\n" + article);

                String outputFile = outputFolder + File.separator + "wes2015." + idStr + ".naf";
                document.save(outputFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
