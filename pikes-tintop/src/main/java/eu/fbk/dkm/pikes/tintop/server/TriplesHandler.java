package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.rdf.RDFGenerator;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import eu.fbk.rdfpro.RDFProcessors;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import ixa.kaflib.KAFDocument;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:30
 * To change this template use File | Settings | File Templates.
 */

public class TriplesHandler extends AbstractHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TriplesHandler.class);
	private AnnotationPipeline pipeline;

	private static Pattern annotatorPattern = Pattern.compile("^annotator_(.*)");
	private static Pattern metaPattern = Pattern.compile("^meta_(.*)");

	public TriplesHandler(AnnotationPipeline pipeline) {
		this.pipeline = pipeline;
	}

	@Override
	public void service(Request request, Response response) throws Exception {

		super.service(request, response);

		String host = request.getHeader("x-forwarded-for");

		String referer = request.getHeader("referer");
		String okReferer = pipeline.getConfig().getProperty("back_referer");

		boolean backLink = false;
		if (referer != null && okReferer != null && referer.equals(okReferer)) {
			backLink = true;
		}

		HashSet<String> annotators = new HashSet<>();
		HashMap<String, String> meta = new HashMap<>();

		for (String parameterLabel : request.getParameterMap().keySet()) {

			Matcher matcher;

			matcher = annotatorPattern.matcher(parameterLabel);
			if (matcher.find()) {
				annotators.add(matcher.group(1));
			}

			matcher = metaPattern.matcher(parameterLabel);
			if (matcher.find()) {
				String key = matcher.group(1);
				meta.put(key, request.getParameter(parameterLabel));
			}
		}

		if (meta.get("uri") == null || meta.get("uri").length() == 0) {
			meta.put("uri", pipeline.getConfig().getProperty("default_uri"));
		}

		String text = request.getParameter("text");

		// Log for stats
		LOGGER.info("[SENTENCE]");
		LOGGER.info("Host: {}", host);
		LOGGER.info("Text: {}", text);

		KAFDocument doc = text2naf(text, meta);

		doc = pipeline.parseFromString(doc.toString(), annotators);

		String viewString;
		try {

			HashMap<String, Object> demoProperties = new HashMap<>();
			demoProperties.put("renderer.template.title", "PIKES demo");
			if (backLink) {
				demoProperties.put("renderer.template.backlink", "javascript:history.back();");
			}
			else {
				demoProperties.put("renderer.template.backlink", pipeline.getConfig().getProperty("back_alt_link"));
				demoProperties.put("renderer.template.backlabel", pipeline.getConfig().getProperty("back_alt_text"));
			}

			boolean fusion = request.getParameter("rdf_fusion") != null;
			boolean normalization = request.getParameter("rdf_compaction") != null;

			demoProperties.put("generator.fusion", fusion);
			demoProperties.put("generator.normalization", normalization);

			NAFFilter filter = NAFFilter.builder().withProperties(pipeline.getConfig(), "filter").build();
			RDFGenerator generator = RDFGenerator.builder().withProperties(demoProperties, "generator").build();
//			Renderer renderer = Renderer.builder().withProperties(demoProperties, "renderer").build();

			StringWriter writer = new StringWriter();
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TRIG, writer);
			filter.filter(doc);
			List<Statement> statementList = new ArrayList<>();

			generator.generate(doc, null, statementList);
			RDFSource rdfSource = RDFSources.wrap(statementList);
			RDFProcessors.prefix(null).wrap(rdfSource).emit(rdfWriter, 1);

//			final Model model = generator.generate(doc, null);
//			StringWriter writer = new StringWriter();
//			renderer.renderAll(writer, doc, model, null, null);
			viewString = writer.toString();

		} catch (Exception e) {
			e.printStackTrace();
			viewString = "Unable to show graph. <br /><br />\n<pre>" + doc.toString().replace("<", "&lt;").replace(">", "&gt;") + "</pre>";
		}

		super.writeOutput(response, "text/plain", viewString);
	}
}
