package eu.fbk.dkm.pikes.rdf.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.github.mustachejava.Mustache;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.pikes.rdf.naf.NAFAnnotation;
import eu.fbk.dkm.pikes.rdf.naf.NAFExtractor;
import eu.fbk.dkm.pikes.rdf.util.Templates;
import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.CommandLine.Type;
import eu.fbk.dkm.utils.Util;
import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.dkm.utils.vocab.OWLTIME;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.Environment;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.rdfpro.util.Tracker;

public abstract class Service implements Annotator, Extractor, Distiller, Renderer, HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

    private static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZone(ZoneOffset.UTC)
            .withLocale(Locale.US);

    private static final Mustache FORM_TEMPLATE = Templates.load(Service.class
            .getResource("ui/form.html"));

    public static Service create(final Annotator annotator, final Extractor extractor,
            final Distiller distiller, @Nullable final Renderer renderer) {
        return new LocalService(Objects.requireNonNull(annotator),
                Objects.requireNonNull(extractor), Objects.requireNonNull(distiller),
                renderer != null ? renderer : Renderer.newTemplateRenderer(null, null, null));
    }

    public static Service create(final String serverURL) {
        return new RemoteService(Objects.requireNonNull(serverURL));
    }

    @Override
    public abstract void annotate(final Document document) throws Exception;

    @Override
    public abstract void extract(final Document document) throws Exception;

    @Override
    public abstract void distill(final Document document) throws Exception;

    @Override
    public abstract void render(final Document document, final Appendable out) throws Exception;

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

    public static void main(final String... args) {

        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("pikes-rdf")
                    .withOption("a", "action", "performs the requested ACTION: 'annotate', " //
                            + "'extract', 'distill', 'render', 'server' (default 'distill' if " //
                            + "input files specified, 'server' otherwise)", "ACTION", Type.STRING,
                            true, false, false)
                    .withOption("r", "recursive",
                            "recurse on sub-directories when reading input data")
                    .withOption("c", "clear", "clear annotations, mentions and instances " //
                            + "and reprocess the document from scratch")
                    .withOption("o", "output", "the PATH of the output file (for single file " //
                            + "output), or output directory (must exist)", "PATH", Type.STRING,
                            true, false, false)
                    .withOption("f", "format", "the FORMAT of output files (chosen automatically " //
                            + "if unspecified)", "FORMAT", Type.STRING, true, false, false)
                    .withOption("p", "port", "the PORT to be used by the server (only for action " //
                            + "'server', default 8080)", "PORT", Type.INTEGER, true, false, false)
                    .withHeader(
                            "Runs PIKES RDF knowledge extraction service either as an HTTP ReST "
                                    + "server (with UI) or as a command line tool. Options:")
                    .parse(args);

            // Extract options
            final boolean clear = cmd.hasOption("c");
            final boolean recursive = cmd.hasOption("r");
            final List<Path> inputPaths = cmd.getArgs(Path.class);
            final Path outputPath = cmd.getOptionValue("o", Path.class,
                    Paths.get(System.getProperty("user.dir")));
            final int port = cmd.getOptionValue("p", Integer.class, 8080);
            final Action action = cmd.getOptionValue("a", Action.class,
                    inputPaths.isEmpty() ? Action.SERVER : Action.DISTILL);
            final String format = cmd
                    .getOptionValue("f", String.class, action.defaultOutputFormat);

            // Create Service
            // TODO
            final Annotator annotator = Annotator.newHttpAnnotator(
                    "https://knowledgestore2.fbk.eu/pikes-demo/api/text2naf",
                    true,
                    ImmutableMap.<String, String>builder().put("meta_author", "${dct:author}")
                            .put("meta_date", "${dct:created}").put("meta_uri", "${uri}")
                            .put("meta_title", "${dct:title}").put("outputformat", "output_naf")
                            .put("text", "${text}").put("annotator_anna_pos", "on")
                            .put("annotator_cross_srl", "on").put("annotator_dcoref", "on")
                            .put("annotator_dep_parse", "on").put("annotator_lemma", "on")
                            .put("annotator_linking", "on").put("annotator_naf_filter", "on")
                            .put("annotator_ner", "on").put("annotator_parse", "on")
                            .put("annotator_sentiment", "on").put("annotator_simple_pos", "on")
                            .put("annotator_srl", "on").put("annotator_ssplit", "on")
                            .put("annotator_sst", "on").put("annotator_tokenize", "on")
                            .put("annotator_ukb", "on").build(), null, NAFAnnotation.FORMAT);
            final Extractor extractor = new NAFExtractor();
            final Distiller distiller = Distiller.newRuleDistiller();
            final Renderer renderer = Renderer.newTemplateRenderer(null, null, null);
            final Service service = Service.create(annotator, extractor, distiller, renderer);

            // Either start the server or perform batch processing
            if (action == Action.SERVER) {

                // Start server
                final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.setExecutor(Environment.getPool());
                server.createContext("/", service);
                server.start();
                LOGGER.info("Server listening on port {} - send 'q' or CTRL-C to terminate", port);
                final AtomicBoolean running = new AtomicBoolean(true);
                final Runnable stopJob = () -> {
                    synchronized (running) {
                        if (running.compareAndSet(true, false)) {
                            server.stop(0);
                            LOGGER.info("Server stopped");
                        }
                    }
                };
                Runtime.getRuntime().addShutdownHook(new Thread(stopJob));
                try {
                    while (Character.toLowerCase((char) System.in.read()) != 'q') {
                    }
                } catch (final Throwable ex) {
                    // ignore
                }
                stopJob.run();

            } else {

                // Build a renamer
                final Function<String, String> renamer;
                if (Files.isDirectory(outputPath)) {
                    String basePath = null;
                    for (final Path inputPath : inputPaths) {
                        final String str = inputPath.toString();
                        basePath = basePath == null ? str : Strings.commonPrefix(basePath, str);
                    }
                    renamer = Util.fileRenamer(basePath, outputPath.toString(), format, false);
                } else {
                    renamer = null;
                }

                // Process the files as requested. Note: we match any file in (recursively)
                // included dirs. Filtering is done in a second moment
                final Tracker tracker = new Tracker(LOGGER, null, "Processed %d files "
                        + "(%d file/s avg)", "Processed %d files (%d file/s, %d file/s avg)");
                final List<Runnable> fileJobs = Lists.newArrayList();
                final AtomicInteger failed = new AtomicInteger(0);
                for (final Path inPath : Util.fileMatch(inputPaths, null, recursive)) {
                    fileJobs.add(() -> {
                        tracker.increment();
                        try {
                            final Path outPath = renamer == null ? outputPath //
                                    : Paths.get(renamer.apply(inPath.toString()));
                            // if (Files.exists(outPath)) {
                            // throw new IOException("Output file " + outPath +
                            // " already exists");
                            // }
                            final Document document = new Document(inPath.toString());
                            if (clear) {
                                document.clear();
                            }
                            Files.createDirectories(outPath.getParent());
                            try (OutputStream out = IO.buffer(IO.write(outPath.toString()))) {
                                if (action == Action.ANNOTATE) {
                                    service.annotate(document);
                                    document.getAnnotations().get(0).write(out);
                                } else if (action == Action.EXTRACT) {
                                    service.extract(document);
                                    writeGraph(document.getGraph(), out, outPath.toString());
                                } else if (action == Action.DISTILL) {
                                    service.extract(document);
                                    writeGraph(document.getGraph(), out, outPath.toString());
                                } else if (action == Action.RENDER) {
                                    service.render(document, IO.utf8Writer(out));
                                }
                            }
                        } catch (final Throwable ex) {
                            failed.incrementAndGet();
                            LOGGER.error("Processing failed for " + inPath, ex);
                        }
                    });
                }
                tracker.start();
                Environment.run(fileJobs);
                tracker.end();
                if (failed.get() > 0) {
                    LOGGER.info("Processing failed for {} files", failed);
                }
            }

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

    private static void writeGraph(final QuadModel graph, final OutputStream stream,
            final String fileName) throws IOException {

        final RDFFormat rdfFormat = Rio.getWriterFormatForFileName(fileName);
        if (rdfFormat == null) {
            throw new IOException("Unsupported RDF format for " + fileName);
        }

        try {
            final RDFWriter writer = Rio.createWriter(rdfFormat, stream);
            final List<Statement> stmts = Lists.newArrayList(graph);
            Collections.sort(stmts, Statements.statementComparator("spoc", //
                    Statements.valueComparator(RDF.NAMESPACE)));
            final Set<Namespace> namespaces = Sets.newLinkedHashSet(graph.getNamespaces());
            namespaces.add(KS.NS);
            namespaces.add(NIF.NS);
            namespaces.add(DCTERMS.NS);
            namespaces.add(OWLTIME.NS);
            namespaces.add(XMLSchema.NS);
            namespaces.add(OWL.NS); // not strictly necessary
            namespaces.add(RDF.NS); // not strictly necessary
            namespaces.add(RDFS.NS);
            namespaces.add(new NamespaceImpl("dbpedia", "http://dbpedia.org/resource/"));
            namespaces.add(new NamespaceImpl("wn30", "http://wordnet-rdf.princeton.edu/wn30/"));
            namespaces.add(new NamespaceImpl("sst", "http://www.newsreader-project.eu/sst/"));
            namespaces.add(new NamespaceImpl("bbn", "http://dkm.fbk.eu/ontologies/bbn#"));
            namespaces.add(new NamespaceImpl("pb",
                    "http://www.newsreader-project.eu/ontologies/propbank/"));
            namespaces.add(new NamespaceImpl("nb",
                    "http://www.newsreader-project.eu/ontologies/nombank/"));
            RDFSources.wrap(stmts, namespaces).emit(writer, 1);
        } catch (final Throwable ex) {
            throw new IOException(ex);
        }
    }

    private static enum Action {

        ANNOTATE(".gz"),

        EXTRACT(".tql.gz"),

        DISTILL(".tql.gz"),

        RENDER(".html.gz"),

        SERVER(null);

        public final String defaultOutputFormat;

        private Action(final String defaultOutputFormat) {
            this.defaultOutputFormat = defaultOutputFormat;
        }

    }

    private static final class LocalService extends Service {

        private final Annotator annotator;

        private final Extractor extractor;

        private final Distiller distiller;

        private final Renderer renderer;

        private LocalService(final Annotator annotator, final Extractor extractor,
                final Distiller distiller, final Renderer renderer) {

            this.annotator = annotator;
            this.extractor = extractor;
            this.distiller = distiller;
            this.renderer = renderer;
        }

        @Override
        public void annotate(final Document document) throws Exception {

            // Abort in case the document is already annotated
            if (document.hasAnnotations()) {
                return;
            }

            // Delegate
            this.annotator.annotate(document);
        }

        @Override
        public void extract(final Document document) throws Exception {

            // Abort in case the mention layer already exists
            if (document.hasMentions()) {
                return;
            }

            // Annotate the document if necessary
            annotate(document);

            // Delegate
            this.extractor.extract(document);
        }

        @Override
        public void distill(final Document document) throws Exception {

            // Abort in case the instance layer already exists
            if (document.hasInstances()) {
                return;
            }

            // Generate the mention layer if missing
            extract(document);

            // Delegate
            this.distiller.distill(document);
        }

        @Override
        public void render(final Document document, final Appendable out) throws Exception {

            // Generate mention and instance layers if missing (annotations may also be generated)
            extract(document);
            distill(document);

            // Delegate
            this.renderer.render(document, out);
        }

    }

    private static final class RemoteService extends Service {

        private RemoteService(final String url) {
            // TODO
        }

        @Override
        public void annotate(final Document document) throws Exception {
            // TODO
        }

        @Override
        public void extract(final Document document) throws Exception {
            // TODO
        }

        @Override
        public void distill(final Document document) throws Exception {
            // TODO
        }

        @Override
        public void render(final Document document, final Appendable out) throws Exception {
            // TODO
        }

    }

}
