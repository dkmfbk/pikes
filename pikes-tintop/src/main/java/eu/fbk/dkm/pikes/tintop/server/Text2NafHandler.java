package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import ixa.kaflib.KAFDocument;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:30
 * This class is used to generate an empty NAF starting from the text.
 */

public class Text2NafHandler extends AbstractHandler {

	static Logger logger = Logger.getLogger(Text2NafHandler.class.getName());
	private AnnotationPipeline pipeline;

	public Text2NafHandler(AnnotationPipeline pipeline) {
		this.pipeline = pipeline;
	}

	@Override
	public void service(Request request, Response response) throws Exception {

		super.service(request, response);

		String text = request.getParameter("text");
		KAFDocument doc = text2naf(text, null);
		doc = pipeline.parseFromNAF(doc, null);

		writeOutput(response, "text/xml", doc.toString());
	}
}
