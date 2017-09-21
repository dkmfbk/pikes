package eu.fbk.dkm.pikes.tintop.server;

import eu.fbk.dkm.pikes.tintop.AnnotationPipeline;
import eu.fbk.utils.core.CommandLine;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */

public class PipelineServer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PipelineServer.class);

    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final Integer DEFAULT_PORT = 8011;

    public PipelineServer(String host, Integer port) {
        this(host, port, null, null);
    }

    public PipelineServer(String host, Integer port, @Nullable File configFile) {
        this(host, port, configFile, null);
    }

    public PipelineServer(String host, Integer port, @Nullable File configFile,
            @Nullable Properties additionalProperties) {
        logger.info("starting " + host + "\t" + port + " (" + new Date() + ")...");

        AnnotationPipeline pipeline = null;
        try {
            pipeline = new AnnotationPipeline(configFile, additionalProperties);
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

//        HttpServer httpServer = HttpServer.createSimpleServer(null, host, port);
        final HttpServer httpServer = new HttpServer();
        NetworkListener nl = new NetworkListener("pikes-web", host, port);
        httpServer.addListener(nl);

        httpServer.getServerConfiguration().setSessionTimeoutSeconds(timeoutInSeconds);
        httpServer.getServerConfiguration().setMaxPostSize(4194304);
        httpServer.getServerConfiguration().addHttpHandler(new NafHandler(pipeline), "/naf");
        httpServer.getServerConfiguration().addHttpHandler(new NafVisualizeHandler(pipeline), "/view");
        httpServer.getServerConfiguration().addHttpHandler(new NafGenerateHandler(pipeline), "/text");
        httpServer.getServerConfiguration().addHttpHandler(new EverythingHandler(pipeline), "/all");
        httpServer.getServerConfiguration().addHttpHandler(new Text2NafHandler(pipeline), "/text2naf");
        httpServer.getServerConfiguration().addHttpHandler(new TriplesHandler(pipeline), "/text2rdf");
        httpServer.getServerConfiguration().addHttpHandler(new JsonHandler(pipeline), "/text2json");

        httpServer.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "webdemo/"), "/");

        // Fix
        // see: http://stackoverflow.com/questions/35123194/jersey-2-render-swagger-static-content-correctly-without-trailing-slash
//        httpServer.getServerConfiguration().addHttpHandler(
//                new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "webdemo/static/"), "/static/");

        try {
            httpServer.start();
            Thread.currentThread().join();
        } catch (Exception e) {
//            logger.error("error running " + host + ":" + port);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./tintop-server")
                    .withHeader("Run the Tintop Server")
                    .withOption("c", "config", "Configuration file", "FILE", CommandLine.Type.FILE_EXISTING, true,
                            false, false)
                    .withOption("p", "port", String.format("Host port (default %d)", DEFAULT_PORT), "NUM",
                            CommandLine.Type.INTEGER, true, false, false)
                    .withOption("h", "host", String.format("Host address (default %s)", DEFAULT_HOST), "NUM",
                            CommandLine.Type.STRING, true, false, false)
                    .withOption(null, "properties", "Additional properties", "PROPS", CommandLine.Type.STRING, true,
                            true, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            String host = cmd.getOptionValue("host", String.class, DEFAULT_HOST);
            Integer port = cmd.getOptionValue("port", Integer.class, DEFAULT_PORT);
            File configFile = cmd.getOptionValue("config", File.class);

            List<String> addProperties = cmd.getOptionValues("properties", String.class);

            Properties additionalProps = new Properties();
            for (String property : addProperties) {
                try {
                    additionalProps.load(new StringReader(property));
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }
            }

            PipelineServer pipelineServer = new PipelineServer(host, port, configFile, additionalProps);

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }
}
