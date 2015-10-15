package eu.fbk.dkm.pikes.rdf.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.annotation.Nullable;

import com.github.mustachejava.Mustache;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.pikes.rdf.util.Templates;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.CommandLine.Type;
import eu.fbk.rdfpro.util.Environment;
import eu.fbk.rdfpro.util.IO;

public class Service implements AutoCloseable, HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

    private final Annotator annotator;

    private final Extractor extractor;

    private final Distiller mapper;

    private final Renderer renderer;

    private Service(final Annotator annotator, final Extractor extractor, final Distiller mapper,
            final Renderer renderer) {
        this.annotator = annotator; // TODO: check not null
        this.extractor = extractor; // TODO: check not null
        this.mapper = mapper; // TODO: check not null
        this.renderer = renderer; // TODO: check not null
    }

    public static Service create(final Annotator annotator, final Extractor extractor,
            final Distiller mapper, @Nullable final Renderer renderer) {
        return new Service(annotator, extractor, mapper, renderer != null ? renderer
                : Renderer.newTemplateRenderer(null, null, null));
    }

    public Annotator getAnnotator() {
        return this.annotator;
    }

    public Extractor getExtractor() {
        return this.extractor;
    }

    public Distiller getMapper() {
        return this.mapper;
    }

    public Renderer getRenderer() {
        return this.renderer;
    }

    private static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZone(ZoneOffset.UTC)
            .withLocale(Locale.US);

    private static final Mustache FORM_TEMPLATE = Templates.load(Service.class
            .getResource("ui/form.html"));

    @Override
    public void handle(final HttpExchange exchange) throws IOException {

        try {
            LOGGER.debug("HTTP {} {}", exchange.getRequestMethod().toUpperCase(),
                    exchange.getRequestURI());

            final HttpContext context = exchange.getHttpContext();
            final String relativePath = exchange.getRequestURI().getPath()
                    .substring(context.getPath().length());

            exchange.getResponseHeaders().set("Server", "PIKES");
            exchange.getResponseHeaders().set("Date",
                    HTTP_DATE_FORMATTER.format(ZonedDateTime.now()));

            if (relativePath.isEmpty()) {
                exchange.getResponseHeaders().add("Location", context.getPath() + "ui/index.html");
                exchange.sendResponseHeaders(303, 0);

            } else if (relativePath.equals("ui/index.html")) {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final Writer writer = IO.utf8Writer(bos);
                FORM_TEMPLATE.execute(writer, ImmutableMap.of());
                final byte[] body = bos.toByteArray();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);

            } else {
                final URL resourceURL = getClass().getResource(relativePath);
                if (resourceURL == null) {
                    exchange.sendResponseHeaders(404, 0);
                } else {
                    final URLConnection connection = resourceURL.openConnection();
                    if (relativePath.endsWith(".png")) {
                        exchange.getResponseHeaders().set("Content-Type", "image/png");
                    }
                    exchange.getResponseHeaders().set("Cache-Control",
                            "public, s-maxage=86400, max-age=86400, must-revalidate");
                    exchange.sendResponseHeaders(200, connection.getContentLength());
                    ByteStreams.copy(connection.getInputStream(), exchange.getResponseBody());
                }
            }

        } catch (final Throwable ex) {
            // Log and return error message to the client
            LOGGER.error("HTTP processing failed", ex);
            final byte[] body = ex.getMessage().getBytes(Charsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);

        } finally {
            // Consume and close streams (request, response)
            LOGGER.debug("HTTP {}", exchange.getResponseCode());
            exchange.close();
        }
    }

    @Override
    public void close() {
        IO.closeQuietly(this.annotator);
        IO.closeQuietly(this.extractor);
        IO.closeQuietly(this.mapper);
        IO.closeQuietly(this.renderer);
    }

    public static void main(final String... args) {

        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("pikes-rdf")
                    .withOption("p", "port", "start a server at specified PORT", "PORT",
                            Type.INTEGER, true, false, false)
                    .withHeader(
                            "Runs PIKES RDF knowledge extraction service either as a server "
                                    + "or as a command line tool.").parse(args);

            // Extract options
            final int port = cmd.getOptionValue("p", Integer.class, 0);

            // Create Service
            // TODO
            final Service service = Service.create(null, null, null, null);

            // Start server if requested
            if (port != 0) {
                final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.setExecutor(Environment.getPool());
                server.createContext("/", service);
                server.start();
                LOGGER.info("Server listening on port {} - press 'q' or send CTRL-C to terminate",
                        port);
                Runtime.getRuntime().addShutdownHook(new Thread() {

                    @Override
                    public void run() {
                        server.stop(0);
                        LOGGER.info("Server stopped");
                    }

                });
                try {
                    while (true) {
                        final int ch = System.in.read();
                        if (ch == 'q' || ch == 'Q') {
                            server.stop(0);
                            break;
                        }
                    }
                } catch (final Throwable ex) {
                    // ignore
                }
            }

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

}
