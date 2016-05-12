package eu.fbk.dkm.pikes.tintopclient;

import eu.fbk.dkm.utils.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessio on 02/03/15.
 */

public class TintopClient {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TintopClient.class);

    static public Integer DEFAULT_TIMEOUT = 60000;

    protected TintopServer server;
    private Integer timeout;
    private boolean fake = false;

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public TintopClient(String serverUrl) throws MalformedURLException {
        URL url = new URL(serverUrl);
        this.server = new TintopServer(url);
    }

    public TintopClient(TintopServer server) {
        this(server, DEFAULT_TIMEOUT);
    }

    public TintopClient(TintopServer server, Integer timeout) {
        this.server = server;
        this.timeout = timeout;
    }

    public String call(String text) throws URISyntaxException, IOException {

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();

        CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
        URI uri = new URIBuilder()
                .setScheme(server.getProtocol())
                .setHost(server.getHost())
                .setPort(server.getPort())
                .setPath(server.getPath())
                .build();
        logger.debug("Calling URI " + uri.toString());

        if (!fake) {
            HttpPost httpPost = new HttpPost(uri);

            List<NameValuePair> nameValuePairs = new ArrayList<>(1);
            nameValuePairs.add(new BasicNameValuePair("naf", text));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
            httpPost.setEntity(form);

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.debug("Status code: " + statusCode);

                if (statusCode != 200) {
                    logger.error(uri.toString());
                    throw new IOException(
                            String.format("%d: %s", statusCode, response.getStatusLine().getReasonPhrase()));
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (InputStream instream = entity.getContent()) {
                        logger.info("NAF retrieved");
                        return IOUtils.toString(instream);
                    }
                }
            }
        }

        throw new IOException("Null result");
    }

    public static void main(String[] args) {
        try {
            final eu.fbk.dkm.utils.CommandLine cmd = eu.fbk.dkm.utils.CommandLine
                    .parser()
                    .withName("./tintop-client")
                    .withHeader("Run the Tintop Client")
                    .withOption("i", "input", "Input file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("s", "server", "Server address", "URL:PORT",
                            CommandLine.Type.STRING, true, false, true)
                    .withOption("t", "timeout", String.format("Timeout (default %d ms)", DEFAULT_TIMEOUT),
                            "milliseconds", CommandLine.Type.INTEGER, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            String serverUrl = cmd.getOptionValue("server", String.class);
            File inputFile = cmd.getOptionValue("input", File.class);
            Integer timeout = cmd.getOptionValue("timeout", Integer.class, DEFAULT_TIMEOUT);

            URL url = new URL(serverUrl);
            TintopServer server = new TintopServer(url);
            TintopClient client = new TintopClient(server, timeout);

            String whole = FileUtils.readFileToString(inputFile);
            System.out.println(client.call(whole));
        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }

}
