package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import ixa.kaflib.KAFDocument;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.File;

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
        KAFDocument doc;

        try {
            doc = pipeline.parseFromString(naf);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }

        File temp = File.createTempFile("temp-file-name", ".tmp");
        logger.info("Created temp file " + temp.getAbsolutePath());
        doc.save(temp);

        logger.trace(doc.toString());

        writeOutput(response, "text/xml", doc.toString());
    }
}
