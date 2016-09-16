package eu.fbk.dkm.pikes.tintop.annotators.raw;

import org.apache.commons.lang.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public abstract class Linking {

    protected static final Logger LOGGER = LoggerFactory.getLogger(Linking.class);
    private static final Integer DEFAULT_TIMEOUT = 2000;

    protected String urlAddress;
    private Properties config = new Properties();
    protected Boolean extractTypes = true;

    public Linking(Properties properties, String address) {
        config = properties;
        urlAddress = address;
        if (properties.getProperty("extract_types", "1").equals("0")) {
            extractTypes = false;
        }
    }

    protected String request(Map<String, String> pars) throws IOException {
        return request(pars, null);
    }

    protected String request(Map<String, String> pars, String customAddress) throws IOException {
        String thisRequest = "";
        String fromServer = null;

        for (String key : pars.keySet()) {
            String value = pars.get(key);
            try {
                thisRequest += "&" + key + "=" + URLEncoder.encode(value, "utf-8");
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }

        URL serverAddress;
        if (customAddress != null) {
            serverAddress = new URL(customAddress);
        } else {
            serverAddress = new URL(urlAddress);
        }
        LOGGER.debug("URL: " + serverAddress);
        LOGGER.trace("Request: " + thisRequest);

        boolean useProxy = config.getProperty("use_proxy", "0").equals("1");

        HttpURLConnection connection;

        if (useProxy) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProperty("proxy_url", ""),
                    Integer.parseInt(config.getProperty("proxy_port", "0"))));
            connection = (HttpURLConnection) serverAddress.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) serverAddress.openConnection();
        }

        LOGGER.debug("Send POST request");

        StringBuilder sb = new StringBuilder();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(Integer.parseInt(config.getProperty("timeout", DEFAULT_TIMEOUT.toString())));
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write(thisRequest.getBytes(CharEncoding.UTF_8));
            connection.connect();

            // read the result from the server
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((fromServer = rd.readLine()) != null) {
                sb.append(fromServer + '\n');
            }

        } catch (Throwable e) {
            LOGGER.error("Linking error: {}", e.getMessage());
        }
        LOGGER.debug("Request ended");

        return sb.toString();
    }

    public abstract List<LinkingTag> tag(String text) throws Exception;
}
