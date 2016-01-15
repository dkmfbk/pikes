package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.rdf.RDFGenerator;
import eu.fbk.dkm.pikes.rdf.Renderer;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import ixa.kaflib.KAFDocument;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.openrdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:30
 * To change this template use File | Settings | File Templates.
 */

public class EverythingHandler extends AbstractHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EverythingHandler.class);

	public EverythingHandler(AnnotationPipeline pipeline) {
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

		String viewString;
		viewString = doc.toString();
		try {

			HashMap<String, Object> demoProperties = new HashMap<>();
			demoProperties.put("renderer.template.title", "PIKES demo");
			if (backLink) {
				demoProperties.put("renderer.template.backlink", "javascript:history.back();");
			}
			else {
				demoProperties.put("renderer.template.backlink", pipeline.getDefaultConfig().getProperty("back_alt_link"));
				demoProperties.put("renderer.template.backlabel", pipeline.getDefaultConfig().getProperty("back_alt_text"));
			}

			boolean fusion = request.getParameter("rdf_fusion") != null;
			boolean normalization = request.getParameter("rdf_compaction") != null;

			demoProperties.put("generator.fusion", fusion);
			demoProperties.put("generator.normalization", normalization);

			NAFFilter filter = NAFFilter.builder().withProperties(pipeline.getDefaultConfig(), "filter").build();
			RDFGenerator generator = RDFGenerator.builder().withProperties(demoProperties, "generator").build();
			Renderer renderer = Renderer.builder().withProperties(demoProperties, "renderer").build();

			filter.filter(doc);
			final Model model = generator.generate(doc, null);
			StringWriter writer = new StringWriter();
			renderer.renderAll(writer, doc, model, null, null);
			viewString = writer.toString();

		} catch (Exception e) {
			e.printStackTrace();
			viewString = "Unable to show graph. <br /><br />\n<pre>" + doc.toString().replace("<", "&lt;").replace(">", "&gt;") + "</pre>";
		}

		super.writeOutput(response, "text/html", viewString);
	}
}
