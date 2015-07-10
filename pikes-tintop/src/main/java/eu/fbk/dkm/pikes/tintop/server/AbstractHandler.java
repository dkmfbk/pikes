package eu.fbk.dkm.pikes.tintop.server;

import ixa.kaflib.KAFDocument;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 23/07/14
 * Time: 09:12
 * To change this template use File | Settings | File Templates.
 */

public class AbstractHandler extends HttpHandler {

	static Logger logger = Logger.getLogger(AbstractHandler.class.getName());

	@Override
	public void service(Request request, Response response) throws Exception {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
	}

	public void writeOutput(Response response, String contentType, String output) throws IOException {
		response.setContentType(contentType);
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.getWriter().write(output);
	}

	public KAFDocument text2naf(String text, HashMap<String, String> meta) {
		KAFDocument doc = new KAFDocument("en", "v3");
		doc.setRawText(text);

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH);

		String date = "";
		try {
			date = format.format(new Date());
			if (meta.get("date") != null) {
				date = format.format(format.parse(meta.get("date")));
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		KAFDocument.FileDesc fileDesc = doc.createFileDesc();
		KAFDocument.Public aPublic = doc.createPublic();

		if (meta != null) {
			fileDesc.author = meta.get("author");
			fileDesc.title = meta.get("title");
			fileDesc.filename = meta.get("filename");
			fileDesc.creationtime = date;
			aPublic.publicId = meta.get("id");
			aPublic.uri = meta.get("uri");
		}

//		KAFDocument.Public p = doc.createPublic();
//		p.uri = "http://www.example.com";
//		p.publicId = "0";
//
//		KAFDocument.FileDesc d = doc.createFileDesc();
//		d.creationtime = date;
//		d.author = "Unknown author";
//		d.filename = "test.xml";
//		d.title = "Unknown title";

		return doc;
	}
}
