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
 * This class convert a raw NAF to a parsed one
 */

public class NafHandler extends AbstractHandler {

	static Logger logger = Logger.getLogger(NafHandler.class.getName());

	public NafHandler(AnnotationPipeline pipeline) {
		super(pipeline);
	}

	@Override
	public void service(Request request, Response response) throws Exception {

		logger.debug("Starting service");
		super.service(request, response);

		String naf = request.getParameter("naf");
		KAFDocument doc = pipeline.parseFromString(naf);

		logger.trace(doc.toString());

		writeOutput(response, "text/xml", doc.toString());
	}
}
