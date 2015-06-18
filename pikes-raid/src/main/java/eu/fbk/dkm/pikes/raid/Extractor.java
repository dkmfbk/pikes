package eu.fbk.dkm.pikes.raid;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.CommandLine.Type;
import eu.fbk.dkm.utils.Corpus;
import eu.fbk.dkm.utils.Util;
import eu.fbk.rdfpro.util.Tracker;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public abstract class Extractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Extractor.class);

    public final void extract(final Iterable<KAFDocument> documents, final String outLabel,
            final Component... components) {

        // Validate components and process supplied documents using parallelization
        final EnumSet<Component> componentSet = Component.toSet(components);
        StreamSupport.stream(documents.spliterator(), true).forEach(document -> {
            Preconditions.checkNotNull(document);
            synchronized (document) {
                extract(document, outLabel, componentSet);
            }
        });
    }

    public final void extract(final KAFDocument document, @Nullable final String outLabel,
            final Component... components) {

        // Validate document and components, then delegate
        Preconditions.checkNotNull(document);
        final EnumSet<Component> componentSet = Component.toSet(components);
        synchronized (document) {
            extract(document, outLabel, componentSet);
        }
    }

    public final void refine(final Iterable<KAFDocument> documents,
            @Nullable final Iterable<String> inLabels, @Nullable final String outLabel,
            final Component... components) {

        // Validate components and process supplied documents, possibly using parallelization
        final EnumSet<Component> componentSet = Component.toSet(components);
        StreamSupport.stream(documents.spliterator(), true).forEach(document -> {
            Preconditions.checkNotNull(document);
            synchronized (document) {
                refine(document, inLabels, outLabel, componentSet);
            }
        });
    }

    public final void refine(final KAFDocument document,
            @Nullable final Iterable<String> inLabels, @Nullable final String outLabel,
            final Component... components) {

        // Validate document and components, then delegate
        Preconditions.checkNotNull(document);
        final EnumSet<Component> componentSet = Component.toSet(components);
        synchronized (document) {
            refine(document, inLabels, outLabel, componentSet);
        }
    }

    private void extract(final KAFDocument document, @Nullable final String outLabel,
            final EnumSet<Component> components) {

        // Filter the document
        doFilter(document);

        // Remove all the opinions for the output label
        for (final Opinion opinion : document.getOpinions(outLabel)) {
            document.removeAnnotation(opinion);
        }

        // Process the document one sentence at a time
        final int numSentences = document.getNumSentences() + 1;
        for (int i = 0; i < numSentences; ++i) {

            // Extract opinions from current sentence graph
            final Iterable<Opinion> opinions = doExtract(document, i, components);

            // Ensure that extracted opinions contain only requested components
            Opinions.retain(opinions, null, components);
            for (final Opinion opinion : opinions) {
                opinion.setLabel(outLabel);
            }
        }
    }

    private void refine(final KAFDocument document, @Nullable final Iterable<String> inLabels,
            @Nullable final String outLabel, final EnumSet<Component> components) {

        // Filter the document
        doFilter(document);

        // Identify the opinions to modify
        List<Opinion> opinions;
        if (inLabels == null || Iterables.isEmpty(inLabels)) {
            opinions = Lists.newArrayList(document.getOpinions());
        } else {
            opinions = Lists.newArrayList();
            for (final String inLabel : inLabels) {
                opinions.addAll(document.getOpinions(inLabel));
            }
        }

        // Index the resulting opinions by sentence
        final ListMultimap<Integer, Opinion> inOpinions = ArrayListMultimap.create();
        for (final Opinion opinion : opinions) {
            final OpinionExpression exp = opinion.getOpinionExpression();
            inOpinions.put(exp.getSpan().getTargets().get(0).getSent(), opinion);
        }

        // Remove all the opinions for the output label
        document.removeAnnotations(document.getOpinions(outLabel));

        // Perform refining, processing all the sentences for which at least an opinion is defined
        for (final Map.Entry<Integer, Collection<Opinion>> entry : inOpinions.asMap().entrySet()) {

            // Process all the opinions of the sentence
            final int sentenceID = entry.getKey();
            final List<Opinion> refinedOpinions = Lists.newArrayList();
            for (final Opinion inOpinion : entry.getValue()) {

                // Perform refining
                final Iterable<Opinion> outOpinions = doRefine(document, sentenceID, components,
                        inOpinion);
                if (Iterables.isEmpty(outOpinions)) {
                    System.out.println("*******");
                }
                Iterables.addAll(refinedOpinions, outOpinions);

                // Ensure resulting opinions preserve components that don't have to be refined
                Opinions.retain(outOpinions, inOpinion, components);
                for (final Opinion outOpinion : outOpinions) {
                    outOpinion.setLabel(outLabel);
                }
            }
            Opinions.deduplicate(document, refinedOpinions);
        }
    }

    protected void doFilter(final KAFDocument document) {
        // can be overridden by subclasses
    }

    protected abstract Iterable<Opinion> doExtract(KAFDocument document, int sentenceID,
            EnumSet<Component> components);

    protected abstract Iterable<Opinion> doRefine(KAFDocument document, int sentenceID,
            EnumSet<Component> components, Opinion opinion);

    protected void doWrite(final Properties properties, final Path path) throws IOException {
        // can be overridden by subclasses
    }

    public final void writeTo(final Path path) throws IOException {

        final Path p = Util.openVFS(path, true);
        try {
            // Create properties
            final Properties properties = new Properties();

            // Store custom entries
            doWrite(properties, p);

            // Store name of implementation class
            properties.put("class", getClass().getName());

            // Add a file for the properties
            try (BufferedWriter writer = Files.newBufferedWriter(p.resolve("properties"),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                properties.store(writer, "");
            }

        } finally {
            Util.closeVFS(p);
        }
    }

    public static Extractor readFrom(final Path path, @Nullable final Properties customProperties)
            throws IOException {

        final Path p = Util.openVFS(path, false);
        try {
            // Read stored properties entry and apply custom properties
            final Properties properties = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(p.resolve("properties"))) {
                properties.load(reader);
            }
            if (customProperties != null) {
                properties.putAll(customProperties);
            }

            // Select the implementation class and delegate to its doRead() static method
            final String implementationName = properties.getProperty("class");
            try {
                final Class<?> implementationClass = Class.forName(implementationName);
                final Constructor<?> constructor = implementationClass.getDeclaredConstructor(
                        Properties.class, Path.class);
                constructor.setAccessible(true);
                return (Extractor) constructor.newInstance(properties, p);
            } catch (final InvocationTargetException ex) {
                Throwables.propagateIfInstanceOf(ex, IOException.class);
                throw Throwables.propagate(ex);
            } catch (final NoSuchMethodException | ClassNotFoundException | IllegalAccessException
                    | InstantiationException ex) {
                throw new IllegalArgumentException("Could not instantiate class "
                        + implementationName);
            }

        } finally {
            Util.closeVFS(p);
        }
    }

    public static void main(final String... args) {

        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("fssa-extract")
                    .withHeader(
                            "Extracts opinion expressions, holders and targets "
                                    + "from one or multiple NAF files.")
                    .withOption("p", "properties", "a sequence of key=value properties, used to " //
                            + "select and configure the trainer", "PROPS", Type.STRING, true,
                            false, false)
                    .withOption("c", "components", "the opinion components to consider: " //
                            + "(e)xpression, (h)older, (t)arget, (p)olarity", "COMP", Type.STRING,
                            true, false, false)
                    .withOption("m", "model", "the extractor model", "FILE", Type.FILE_EXISTING,
                            true, false, true)
                    .withOption("l", "labels", "the labels of the existing opinions to modify " //
                            + "(refine mode), comma separated  (no spaces)", "LABELS",
                            Type.STRING, true, false, false)
                    .withOption("b", "outlabel", "the label to associate to produced opinions",
                            "LABEL", Type.STRING, true, false, false)
                    .withOption("r", "recursive",
                            "recurse into subdirectories of specified input paths")
                    .withOption("@", "list",
                            "interprets input as list of file names, one per line")
                    .withOption("o", "output", "the output file or directory name", "FILE",
                            Type.FILE, true, false, false)
                    .withOption("f", "format", "the output format", "FMT", Type.STRING, true,
                            false, false)
                    .withOption("j", "junk",
                            "junk path structure in input files and emits a flat list of files")
                    .withOption(null, "wordnet", "wordnet dict path", "PATH",
                            Type.DIRECTORY_EXISTING, true, false, false)
                    .withFooter(
                            "Zero or more input paths can be specified, corresponding either "
                                    + "to NAF files or directories that are scanned for NAF "
                                    + "files. If the list is empty, an input NAF file will be "
                                    + "read from the standard input. If no output path is "
                                    + "specified (-o), output is written to standard output.")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            // Extract options
            final Properties properties = Util.parseProperties(cmd.getOptionValue("p",
                    String.class, ""));
            final Component[] components = Component.forLetters(
                    cmd.getOptionValue("c", String.class, "")).toArray(new Component[0]);
            final Set<String> inputLabels = ImmutableSet.copyOf(Splitter.on(',')
                    .omitEmptyStrings().split(cmd.getOptionValue("l", String.class, "")));
            final String outputLabel = cmd.getOptionValue("b", String.class, null);
            final Path modelPath = cmd.getOptionValue("m", Path.class);
            final boolean recursive = cmd.hasOption("r");
            final boolean list = cmd.hasOption("@");
            final File outputPath = cmd.getOptionValue("o", File.class, null);
            final String format = cmd.getOptionValue("f", String.class, ".naf");
            final boolean junk = cmd.hasOption("j");
            final List<Path> inputPaths = Lists.newArrayList(cmd.getArgs(Path.class));

            final String wordnetPath = cmd.getOptionValue("wordnet", String.class);
            if (wordnetPath != null) {
                WordNet.setPath(wordnetPath);
            }

            // Setup the opinion extractor
            final Extractor extractor = readFrom(modelPath, properties);

            // Identify input
            final List<Path> files = Util.fileMatch(inputPaths, ImmutableList.of(".naf",
                    ".naf.gz", ".naf.bz2", ".naf.xz", ".xml", ".xml.gz", ".xml.bz2", ".xml.xz"),
                    recursive, list);
            final Iterable<KAFDocument> documents = files != null ? Corpus.create(false, files)
                    : ImmutableList.of(NAFUtils.readDocument(null));

            // Perform the extraction
            final Function<String, String> namer = outputPath == null ? null //
                    : Util.fileRenamer("", outputPath.getAbsolutePath() + "/", format, junk);
            final Tracker tracker = new Tracker(LOGGER, null, //
                    "Processed %d NAF files (%d NAF/s avg)", //
                    "Processed %d NAF files (%d NAF/s, %d NAF/s avg)");
            tracker.start();
            StreamSupport.stream(documents.spliterator(), false).forEach(
                    (final KAFDocument document) -> {
                        if (inputLabels.isEmpty()) {
                            extractor.extract(document, outputLabel, components);
                        } else {
                            extractor.refine(document, inputLabels, outputLabel, components);
                        }
                        try {
                            NAFUtils.writeDocument(document, namer == null ? null //
                                    : Paths.get(namer.apply(document.getPublic().publicId)));
                        } catch (final IOException ex) {
                            throw Throwables.propagate(ex);
                        }
                        tracker.increment();
                    });
            tracker.end();

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

}
