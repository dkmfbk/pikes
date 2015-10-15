package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import ixa.kaflib.KAFDocument;
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

	public Text2NafHandler(AnnotationPipeline pipeline) {
		super(pipeline);
	}

	@Override
	public void service(Request request, Response response) throws Exception {

		super.service(request, response);

		String text = request.getParameter("text");
		KAFDocument doc = text2naf(text, meta);
		doc = pipeline.parseFromNAF(doc, annotators);

		writeOutput(response, "text/xml", doc.toString());
	}
}
