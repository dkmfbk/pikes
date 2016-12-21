package eu.fbk.dkm.pikes.raid;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;

import eu.fbk.dkm.pikes.naflib.Corpus;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import eu.fbk.utils.svm.Util;
import eu.fbk.rdfpro.util.Tracker;

public abstract class Trainer<T extends Extractor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Trainer.class);

    private final EnumSet<Component> components;

    private final ReadWriteLock lock;

    private final AtomicInteger numOpinions;

    private boolean trained;

    protected Trainer(final Component... components) {
        this.components = Component.toSet(components);
        this.lock = new ReentrantReadWriteLock(false);
        this.numOpinions = new AtomicInteger(0);
        this.trained = false;
    }

    public final Set<Component> components() {
        return this.components;
    }

    public final Trainer<T> add(final Iterable<KAFDocument> documents,
            @Nullable final Iterable<String> goldLabels) {

        // Process all the documents using parallelization, holding a read lock meanwhile
        this.lock.readLock().lock();
        try {
            checkNotTrained();
            StreamSupport.stream(documents.spliterator(), true).forEach(document -> {
                Preconditions.checkNotNull(document);
                doAdd(document, goldLabels);
            });
        } finally {
            this.lock.readLock().unlock();
        }
        return this;
    }

    public final Trainer<T> add(final KAFDocument document,
            @Nullable final Iterable<String> goldLabels) {

        // Validate and process the document, holding a read lock meanwhile
        Preconditions.checkNotNull(document);
        this.lock.readLock().lock();
        try {
            checkNotTrained();
            doAdd(document, goldLabels);
        } finally {
            this.lock.readLock().unlock();
        }
        return this;
    }

    public final T train() {

        // Complete training ensuring that no add() methods are active meanwhile
        this.lock.writeLock().lock();
        try {
            checkNotTrained();
            LOGGER.info("Extracted {} opinions", this.numOpinions.get());
            return doTrain();
        } catch (final Throwable ex) {
            throw Throwables.propagate(ex);
        } finally {
            this.trained = true;
            this.lock.writeLock().unlock();
        }
    }

    private void doAdd(final KAFDocument document, @Nullable final Iterable<String> goldLabels) {

        // Filter the document
        doFilter(document);

        // Normalize (split) input opinions and index the resulting opinions by sentence
        final ListMultimap<Integer, Opinion> opinionsBySentence = ArrayListMultimap.create();
        synchronized (document) {
            List<Opinion> opinions;
            if (goldLabels == null || Iterables.isEmpty(goldLabels)) {
                opinions = Lists.newArrayList(document.getOpinions());
            } else {
                opinions = Lists.newArrayList();
                for (final String goldLabel : goldLabels) {
                    opinions.addAll(document.getOpinions(goldLabel));
                }
            }
            // TODO: this is an hack to deal with VUA non-opinionated fake opinions
            for (final Iterator<Opinion> i = opinions.iterator(); i.hasNext();) {
                final Opinion opinion = i.next();
                if (opinion.getPolarity() != null
                        && opinion.getPolarity().equalsIgnoreCase("NON-OPINIONATED")) {
                    i.remove();
                    LOGGER.info("Skipping non-opinionated opinion {}", opinion.getId());
                }
            }
            for (final Opinion opinion : opinions) {
                final OpinionExpression exp = opinion.getOpinionExpression();
                opinionsBySentence.put(exp.getSpan().getTargets().get(0).getSent(), opinion);
            }
        }

        // Perform training, processing all the sentences in the document even if without opinions
        final int numSentences = document.getNumSentences();
        for (int sentID = 1; sentID <= numSentences; ++sentID) {

            // Extract all the opinions in the sentence
            final Opinion opinions[] = opinionsBySentence.get(sentID).toArray(new Opinion[0]);

            // Perform training
            try {
                doAdd(document, sentID, opinions);
                this.numOpinions.addAndGet(opinions.length);
            } catch (final Throwable ex) {
                Throwables.propagate(ex);
            }
        }
    }

    private void checkNotTrained() {
        Preconditions.checkState(!this.trained, "Training already completed");
    }

    protected void doFilter(final KAFDocument document) {
        // can be overridden by subclasses
    }

    protected abstract void doAdd(KAFDocument document, int sentence, Opinion[] opinions)
            throws Throwable;

    protected abstract T doTrain() throws Throwable;

    @SuppressWarnings("unchecked")
    public static Trainer<? extends Extractor> create(final Properties properties,
            final Component... components) {

        // Select the implementation class and delegate to its constructor
        String implementationName = properties.getProperty("class");
        if (implementationName == null) {
            implementationName = "eu.fbk.dkm.pikes.raid.pipeline.PipelineTrainer"; // default
        }
        try {
            final Class<?> implementationClass = Class.forName(implementationName);
            final Constructor<?> constructor = implementationClass.getDeclaredConstructor(
                    Properties.class, Component[].class);
            constructor.setAccessible(true);
            return (Trainer<? extends Extractor>) constructor.newInstance(properties, components);
        } catch (final InvocationTargetException ex) {
            throw Throwables.propagate(ex);
        } catch (final NoSuchMethodException | ClassNotFoundException | IllegalAccessException
                | InstantiationException ex) {
            throw new IllegalArgumentException("Could not instantiate class " + implementationName);
        }
    }

    public static void main(final String... args) {

        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("fssa-train")
                    .withHeader(
                            "Train the extractor of opinion expressions, holders and targets "
                                    + "given a set of annotated NAF files.")
                    .withOption("p", "properties", "a sequence of key=value properties, used to " //
                            + "select and configure the trainer", "PROPS", Type.STRING, true,
                            false, false)
                    .withOption("c", "components", "the opinion components to consider: " //
                            + "(e)xpression, (h)older, (t)arget, (p)olarity", "COMP", Type.STRING,
                            true, false, false)
                    .withOption("l", "labels", "the labels of gold opinions to consider, comma " //
                            + "separated  (no spaces)", "LABELS", Type.STRING, true, false, false)
                    .withOption("r", "recursive",
                            "recurse into subdirectories of specified input paths")
                    .withOption("@", "list",
                            "interprets input as list of file names, one per line")
                    .withOption(null, "wordnet", "wordnet dict path", "PATH",
                            Type.DIRECTORY_EXISTING, true, false, false)
                    .withOption("o", "output", "the output model file", "FILE", Type.FILE, true,
                            false, false)
                    .withOption("s", "split",
                            "splits the supplied NAF files based on the supplied " //
                                    + "seed:ratio spec, using only the first part for training",
                            "RATIO", Type.STRING, true, false, false)
                    .withFooter(
                            "Zero or more input paths can be specified, corresponding either "
                                    + "to NAF files or directories that are scanned for NAF "
                                    + "files. If the list is empty, an input NAF file will be "
                                    + "read from the standard input. If no output path is "
                                    + "specified (-o), the model is written to standard output.")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            // Extract options
            final Properties properties = Util.parseProperties(cmd.getOptionValue("p",
                    String.class, ""));
            final Component[] components = Component.forLetters(
                    cmd.getOptionValue("c", String.class, "")).toArray(new Component[0]);
            final Set<String> labels = ImmutableSet.copyOf(Splitter.on(',').omitEmptyStrings()
                    .split(cmd.getOptionValue("l", String.class, "")));
            final boolean recursive = cmd.hasOption("r");
            final boolean list = cmd.hasOption("@");
            final Path outputPath = cmd.getOptionValue("o", Path.class, null);
            final String split = cmd.getOptionValue("s", String.class);
            final List<Path> inputPaths = Lists.newArrayList(cmd.getArgs(Path.class));

            final String wordnetPath = cmd.getOptionValue("wordnet", String.class);
            if (wordnetPath != null) {
                WordNet.setPath(wordnetPath);
            }

            // Setup the trainer
            final Trainer<? extends Extractor> trainer = create(properties, components);

            // Identify input
            final List<Path> files = Util.fileMatch(inputPaths, ImmutableList.of(".naf",
                    ".naf.gz", ".naf.bz2", ".naf.xz", ".xml", ".xml.gz", ".xml.bz2", ".xml.xz"),
                    recursive, list);
            Iterable<KAFDocument> documents = files != null ? Corpus.create(false, files)
                    : ImmutableList.of(NAFUtils.readDocument(null));

            // Split training set, if required
            if (split != null && documents instanceof Corpus) {
                final int index = split.indexOf(':');
                long seed = 0;
                float ratio = 1.0f;
                if (index >= 0) {
                    seed = Long.parseLong(split.substring(0, index));
                    ratio = Float.parseFloat(split.substring(index + 1));
                } else {
                    ratio = Float.parseFloat(split);
                }
                final Corpus corpus = (Corpus) documents;
                documents = corpus.split(seed, ratio, 1.0f - ratio)[0];
            }

            // Perform the extraction
            final Tracker tracker = new Tracker(LOGGER, null, //
                    "Processed %d NAF files (%d NAF/s avg)", //
                    "Processed %d NAF files (%d NAF/s, %d NAF/s avg)");
            tracker.start();
            StreamSupport.stream(documents.spliterator(), false).forEach(
                    (final KAFDocument document) -> {
                        trainer.add(document, labels);
                        tracker.increment();
                    });
            tracker.end();

            // Complete the training and save the model
            final Extractor extractor = trainer.train();
            extractor.writeTo(outputPath);

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }
}
