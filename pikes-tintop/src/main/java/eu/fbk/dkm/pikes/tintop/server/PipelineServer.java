package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import eu.fbk.dkm.pikes.tintop.CommandLineWithLogger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.glassfish.grizzly.http.server.HttpServer;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */

public class PipelineServer {

	static Logger logger = Logger.getLogger(PipelineServer.class.getName());

	public static final String DEFAULT_HOST = "localhost";
	public static final String DEFAULT_PORT = "8011";

	public PipelineServer(String host, String port, @Nullable String configFile) {
		logger.info("starting " + host + "\t" + port + " (" + new Date() + ")...");

		AnnotationPipeline pipeline = null;
		try {
			pipeline = new AnnotationPipeline(configFile);
			pipeline.loadModels();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			System.exit(1);
		}

		int timeoutInSeconds = -1;
		try {
			if (pipeline.getDefaultConfig().getProperty("timeout") != null) {
				timeoutInSeconds = Integer.parseInt(pipeline.getDefaultConfig().getProperty("timeout"));
				logger.info("Timeout set to: " + timeoutInSeconds);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		HttpServer httpServer = HttpServer.createSimpleServer(host, new Integer(port).intValue());
		httpServer.getServerConfiguration().setSessionTimeoutSeconds(timeoutInSeconds);
		httpServer.getServerConfiguration().setMaxPostSize(4194304);
		httpServer.getServerConfiguration().addHttpHandler(new NafHandler(pipeline), "/naf");
		httpServer.getServerConfiguration().addHttpHandler(new NafVisualizeHandler(pipeline), "/view");
		httpServer.getServerConfiguration().addHttpHandler(new NafGenerateHandler(pipeline), "/text");
		httpServer.getServerConfiguration().addHttpHandler(new EverythingHandler(pipeline), "/all");
		httpServer.getServerConfiguration().addHttpHandler(new Text2NafHandler(pipeline), "/text2naf");
		httpServer.getServerConfiguration().addHttpHandler(new TriplesHandler(pipeline), "/text2rdf");

//		CompressionConfig compressionConfig = httpServer.getListener("grizzly").getCompressionConfig();
//		compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON); // the mode
//		compressionConfig.setCompressionMinSize(1); // the min amount of bytes to compress
//		compressionConfig.setCompressableMimeTypes("text/plain", "text/html"); // the mime types to compress

		try {
			httpServer.start();
			Thread.currentThread().join();
			//LOGGER.info("Press any key to stop the server...");
			//System.in.read();
			//System.exit(0);
		} catch (Exception e) {
			logger.error("error running " + host + ":" + port);
			logger.error(e);
		}
	}

	public static void main(String[] args) {
		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();

		commandLineWithLogger.addOption(OptionBuilder.withArgName("host").hasArg().withDescription("host name").withLongOpt("host").create("o"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("port").hasArg().withDescription("host port").withLongOpt("port").create("p"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("configuration file").withLongOpt("config").create("c"));

		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		String host = commandLine.getOptionValue("host");
		if (host == null) {
			host = DEFAULT_HOST;
		}
		String port = commandLine.getOptionValue("port");
		if (port == null) {
			port = DEFAULT_PORT;
		}

		String configFile = commandLine.getOptionValue("config");
		new PipelineServer(host, port, configFile);

	}
}
