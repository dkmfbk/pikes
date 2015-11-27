package eu.fbk.dkm.pikes.tintop.orchestrator;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

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

	static Logger logger = Logger.getLogger(TintopClient.class.getName());
	protected TintopServer server;
	private boolean fake = false;

	public void setFake(boolean fake) {
		this.fake = fake;
	}

	public TintopClient(String serverUrl) throws MalformedURLException {
		URL url = new URL(serverUrl);
		this.server = new TintopServer(url);
	}

	public TintopClient(TintopServer server) {
		this.server = server;
	}

	public String call(String text) throws URISyntaxException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
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
			logger.debug(nameValuePairs);
//			StringEntity se = new StringEntity(text, ContentType.APPLICATION_XML);
//			httpPost.setEntity(se);
			UrlEncodedFormEntity form = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
//			form.setContentEncoding(HTTP.UTF_8);
			httpPost.setEntity(form);

			try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
				int statusCode = response.getStatusLine().getStatusCode();
				logger.debug("Status code: " + statusCode);

				if (statusCode != 200) {
					logger.error(uri.toString());
					throw new IOException(String.format("%d: %s", statusCode, response.getStatusLine().getReasonPhrase()));
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
//		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();
//
//		commandLineWithLogger.addOption(
//				OptionBuilder.withDescription("Server").isRequired().hasArg().withArgName("url").withLongOpt("server").create("s"));
//		commandLineWithLogger.addOption(
//				OptionBuilder.withDescription("Input file").isRequired().hasArg().withArgName("file").withLongOpt("input").create("i"));
//
//		CommandLine commandLine = null;
//		try {
//			commandLine = commandLineWithLogger.getCommandLine(args);
//			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
//		} catch (Exception e) {
//			System.exit(1);
//		}
//
//		try {
//
//			String serverUrl = commandLine.getOptionValue("server");
//			String inputFile = commandLine.getOptionValue("input");
//
//			URL url = new URL(serverUrl);
//			TintopServer server = new TintopServer(url);
//			TintopClient client = new TintopClient(server);
//
//			String whole = FileUtils.readFileToString(new File(inputFile));
//			System.out.println(client.call(whole));
//
//		} catch (Exception e) {
//			logger.error(e.getMessage());
//			e.printStackTrace();
//		}
	}

}
