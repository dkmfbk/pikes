package eu.fbk.dkm.pikes.rdf.api;

import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.pikes.rdf.util.Reflect;
import eu.fbk.rdfpro.util.Environment;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.Tracker;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import eu.fbk.utils.svm.Util;

public final class LocalService implements Service, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalService.class);

    private final Annotator annotator;

    private final Extractor extractor;

    private final Distiller distiller;

    private final Renderer renderer;

    private final Map<String, String> defaultOptions;

    public LocalService(final Map<?, ?> properties, @Nullable String prefix) {

        prefix = prefix == null ? "" : prefix.endsWith(".") ? prefix : prefix + ".";

        final Map<String, String> defaultOptions = Maps.newLinkedHashMap();
        for (final Entry<?, ?> entry : properties.entrySet()) {
            final String key = entry.getKey().toString();
            if (key.startsWith("default.")) {
                defaultOptions.put(key.substring("default.".length()),
                        entry.getValue().toString());
            }
        }

        this.annotator = Annotator.concat(Reflect.instantiateAll(Annotator.class, properties,
                prefix + "annotator", "class", "enabled", "order").values());
        this.extractor = Extractor.concat(Reflect.instantiateAll(Extractor.class, properties,
                prefix + "extractor", "class", "enabled", "order").values());
        this.distiller = Distiller.concat(Reflect.instantiateAll(Distiller.class, properties,
                prefix + "distiller", "class", "enabled", "order").values());
        this.renderer = MoreObjects.firstNonNull(Reflect.instantiate(Renderer.class, properties,
                prefix + "annotator", "class", "enabled"), Renderer.NIL);
        this.defaultOptions = ImmutableMap.copyOf(defaultOptions);
    }

    public LocalService(@Nullable final Annotator annotator, @Nullable final Extractor extractor,
            @Nullable final Distiller distiller, @Nullable final Renderer renderer,
            @Nullable final Map<String, String> defaultOptions) {

        this.annotator = annotator != null ? annotator : Annotator.NIL;
        this.extractor = extractor != null ? extractor : Extractor.NIL;
        this.distiller = distiller != null ? distiller : Distiller.NIL;
        this.renderer = renderer != null ? renderer : Renderer.NIL;
        this.defaultOptions = defaultOptions != null ? ImmutableMap.copyOf(defaultOptions)
                : ImmutableMap.of();
    }

    @Override
    public void annotate(final Document document, @Nullable final Map<String, String> options) {
        this.annotator.annotate(Objects.requireNonNull(document), applyDefaultOptions(options));
    }

    @Override
    public void extract(final Document document, @Nullable final Map<String, String> options) {
        this.extractor.extract(Objects.requireNonNull(document), applyDefaultOptions(options));
    }

    @Override
    public void distill(final Document document, @Nullable final Map<String, String> options) {
        this.distiller.distill(Objects.requireNonNull(document), applyDefaultOptions(options));
    }

    @Override
    public void render(final Document document, final Appendable out,
            @Nullable final Map<String, String> options) {
        this.renderer.render(Objects.requireNonNull(document), Objects.requireNonNull(out),
                applyDefaultOptions(options));
    }

    @Override
    public void close() {
        IO.closeQuietly(this.annotator);
        IO.closeQuietly(this.extractor);
        IO.closeQuietly(this.distiller);
        IO.closeQuietly(this.renderer);
    }

    private Map<String, String> applyDefaultOptions(@Nullable final Map<String, String> options) {
        if (options == null) {
            return this.defaultOptions;
        } else if (this.defaultOptions.isEmpty()) {
            return ImmutableMap.copyOf(options);
        } else {
            final Map<String, String> expandedOptions = Maps.newLinkedHashMap();
            expandedOptions.putAll(this.defaultOptions);
            expandedOptions.putAll(options);
            return ImmutableMap.copyOf(expandedOptions);
        }
    }

    public static void main(final String... args) {
        try {
            // Parse command line
            final CommandLine cmd = CommandLine.parser().withName("pikes-rdf")
                    .withOption("c", "config", "specifies the PIKES configuration FILE to use",
                            "FILE", Type.FILE_EXISTING, true, false, false)
                    .withOption("a", "actions",
                            "performs the requested ACTION: 'process', "
                                    + "'annotate', 'extract', 'distill', 'render' "
                                    + "(default 'process')",
                            "ACTION", Type.STRING, true, false, false)
                    .withOption("r", "recursive",
                            "recurse on sub-directories when reading input data")
                    .withOption(null, "clear",
                            "clear annotations, mentions and instances " //
                                    + "and reprocess the document from scratch")
                    .withOption("o", "output",
                            "the PATH of the output file (for single file " //
                                    + "output), or output directory (must exist)",
                            "PATH", Type.STRING, true, false, false)
                    .withOption("f", "format",
                            "the FORMAT of output files (chosen automatically " //
                                    + "if unspecified)",
                            "FORMAT", Type.STRING, true, false, false)
                    .withHeader("Runs PIKES RDF knowledge extraction service "
                            + "as a command line tool. Options:")
                    .parse(args);

            // Extract options
            final boolean clear = cmd.hasOption("c");
            final boolean recursive = cmd.hasOption("r");
            final Path configPath = cmd.getOptionValue("p", Path.class);
            final List<Path> inputPaths = cmd.getArgs(Path.class);
            final Path outputPath = cmd.getOptionValue("o", Path.class,
                    Paths.get(System.getProperty("user.dir")));
            final int port = cmd.getOptionValue("p", Integer.class, 8080);
            final String format = cmd.getOptionValue("f", String.class);
            final String action = cmd.getOptionValue("a", String.class, "process").toLowerCase();
            if (!ImmutableSet.of("process", "annotate", "extract", "distill", "render")
                    .contains(action)) {
                throw new CommandLine.Exception("Unknown action " + action);
            }

            // Load configuration
            final Properties properties = new Properties();
            try (Reader in = Files.newBufferedReader(configPath, Charsets.UTF_8)) {
                properties.load(in);
            }

            // Instantiate the PIKES RDF service
            final Service service = new LocalService(properties, null);

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
            final Tracker tracker = new Tracker(LOGGER, null,
                    "Processed %d files " + "(%d file/s avg)",
                    "Processed %d files (%d file/s, %d file/s avg)");
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
                        final Document document = new Document(); // TODO inPath.toString(), "");
                        if (clear) {
                            document.clear();
                        }
                        Files.createDirectories(outPath.getParent());
                        try (OutputStream out = IO.buffer(IO.write(outPath.toString()))) {
                            if (action.equals("process")) {
                                service.annotate(document, null);

                            } else if (action.equals("annotate")) {
                                service.annotate(document, null);
                                document.getAnnotations().get(0).write(out);
                            } else if (action.equals("extract")) {
                                service.extract(document, null);
                                // writeGraph(document.getGraph(), out, outPath.toString()); TODO
                            } else if (action.equals("distill")) {
                                service.distill(document, null);
                                // writeGraph(document.getGraph(), out, outPath.toString()); TODO
                            } else if (action.equals("render")) {
                                service.render(document, IO.utf8Writer(out), null);
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

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

}
