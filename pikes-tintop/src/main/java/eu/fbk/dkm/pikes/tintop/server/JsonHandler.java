package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import ixa.kaflib.KAFDocument;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:30
 * To change this template use File | Settings | File Templates.
 */

public class JsonHandler extends AbstractHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonHandler.class);

	public JsonHandler(AnnotationPipeline pipeline) {
		super(pipeline);
	}

	@Override
	public void service(Request request, Response response) throws Exception {

		super.service(request, response);

		String host = request.getHeader("x-forwarded-for");

		String referer = request.getHeader("referer");
		String okReferer = pipeline.getDefaultConfig().getProperty("back_referer");

		boolean backLink = false;
		if (referer != null && okReferer != null && referer.equals(okReferer)) {
			backLink = true;
		}

		String text = request.getParameter("text");

		// Log for stats
		LOGGER.info("[SENTENCE]");
		LOGGER.info("Host: {}", host);
		LOGGER.info("Text: {}", text);

		KAFDocument doc = text2naf(text, meta);

		doc = pipeline.parseFromString(doc.toString());

		String viewString = doc.toJsonString();
		System.out.println(viewString);
		super.writeOutput(response, "text/json", viewString);
	}
}
