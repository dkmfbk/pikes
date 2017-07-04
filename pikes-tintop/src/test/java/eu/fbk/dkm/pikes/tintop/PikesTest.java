package eu.fbk.dkm.pikes.tintop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by alessio on 14/06/17.
 */

public class PikesTest {

    public static void main(String[] args) {
        final String USER_AGENT = "Mozilla/5.0";

        String fileName = "/Users/alessio/Downloads/lista_persone_demo.txt";
        List<String> list = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {

            //br returns as stream and convert it into a List
            list = br.lines().collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String line : list) {
            try {
                String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&explaintext=&exlimit=1&titles=" +
                        URLEncoder.encode(line, "UTF-8");

                URL obj = new URL(url);
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

                // optional default is GET
                con.setRequestMethod("GET");

                //add request header
                con.setRequestProperty("User-Agent", USER_AGENT);

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'GET' request from  : " + line);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JsonParser parser = new JsonParser();

                JsonObject o = parser.parse(response.toString()).getAsJsonObject();
                //print result

                String id_page = "";
                String page_content = "";
                for (Map.Entry<String, JsonElement> stringJsonElementEntry : o.getAsJsonObject("query").getAsJsonObject("pages").entrySet()) {
                    id_page = stringJsonElementEntry.getKey();
                    break;
                }
                page_content = o.getAsJsonObject("query").getAsJsonObject("pages").getAsJsonObject(id_page).get("extract").getAsString();
//                page_content = page_content.substring(0, 1000);

                url = "http://rhodes.fbk.eu:50010/text2json";
                URL url_base = new URL(url);

                HttpURLConnection conn = (HttpURLConnection) url_base.openConnection();

                //add request header
                conn.setRequestMethod("POST");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                String urlParameters = "text=" + URLEncoder.encode(page_content, "UTF-8");

                // Send post request
                conn.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                System.out.println("\nSending 'Pikes' request for : " + line);

                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
//                System.out.println(response.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
