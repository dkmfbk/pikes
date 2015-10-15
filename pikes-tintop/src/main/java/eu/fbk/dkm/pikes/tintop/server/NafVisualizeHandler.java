package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.rdf.RDFGenerator;
import eu.fbk.dkm.pikes.rdf.Renderer;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import ixa.kaflib.KAFDocument;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.openrdf.model.Model;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:30
 * This class render a NAF
 */

public class NafVisualizeHandler extends AbstractHandler {

	public NafVisualizeHandler(AnnotationPipeline pipeline) {
		super(pipeline);
	}

	@Override
	public void service(Request request, Response response) throws Exception {

		super.service(request, response);

		String naf = request.getParameter("naf");
		KAFDocument document = KAFDocument.createFromStream(new StringReader(naf));

		NAFFilter filter = NAFFilter.builder().withSRLRoleLinking(false, false).withOpinionLinking(false, false).build();
		RDFGenerator generator = RDFGenerator.DEFAULT;
		Renderer renderer = Renderer.DEFAULT;

		filter.filter(document);
		final Model model = generator.generate(document, null);
		StringWriter writer = new StringWriter();
		renderer.renderAll(writer, document, model, null, null);
		String res = writer.toString();

		super.writeOutput(response, "text/html", res);
	}
}
