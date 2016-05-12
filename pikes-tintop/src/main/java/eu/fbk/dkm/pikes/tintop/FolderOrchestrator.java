package eu.fbk.dkm.pikes.tintop;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.fbk.dkm.pikes.tintopclient.TintopSession;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.FrequencyHashSet;
import eu.fbk.rdfpro.util.IO;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by alessio on 19/01/16.
 */

public class FolderOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(FolderOrchestrator.class);
    static final private int DEFAULT_MAX_ERR_ON_FILE = 5;
    static final private int DEFAULT_MAX_SIZE = 50000;
    static final private int DEFAULT_SIZE = 10;
    static final private int DEFAULT_SLEEPING_TIME = 60000;

    private int maxErrOnFile = DEFAULT_MAX_ERR_ON_FILE;
    private int maxSize = DEFAULT_MAX_SIZE;
    private FrequencyHashSet<String> fileOnError = new FrequencyHashSet<>();
    private String fileCache = null;
    private int skipped = 0;
    public static String[] DEFAULT_EXTENSIONS = new String[] { "xml", "naf" };

    public FolderOrchestrator() {
    }

    public int getMaxErrOnFile() {
        return maxErrOnFile;
    }

    public void setMaxErrOnFile(int maxErrOnFile) {
        this.maxErrOnFile = maxErrOnFile;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    synchronized public void markFileAsNotDone(String filename) {
        fileOnError.add(filename);
        if (fileOnError.get(filename) <= maxErrOnFile) {
            fileCache = filename;

        } else {
            logger.warn(String.format("File %s skipped, more than %d errors", filename, DEFAULT_MAX_ERR_ON_FILE));
        }
    }

    public class LocalTintopClient implements Runnable {

        TintopSession session;
        AnnotationPipeline pipeline;

        public LocalTintopClient(TintopSession session, AnnotationPipeline pipeline) {
            this.session = session;
            this.pipeline = pipeline;
        }

        @Override
        public void run() {
            while (true) {
                String filename = null;
                try {
                    filename = getNextFile(session);
                    if (filename == null) {
                        break;
                    }

                    File file = new File(filename);
                    if (!file.exists()) {
                        break;
                    }

                    File outputFile = getOutputFile(file, session);

                    // todo: use parameters
                    outputFile = new File(outputFile.getAbsolutePath() + ".gz");

                    logger.debug("Output file: " + outputFile);

                    logger.info("Loading file: " + filename);
                    BufferedReader reader = new BufferedReader(new FileReader(filename));
                    String whole = IOUtils.toString(reader);
                    reader.close();

                    KAFDocument doc;

                    String naf;
                    try {
                        doc = pipeline.parseFromString(whole);
                        naf = doc.toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new Exception(e);
                    }

                    logger.debug(naf);
                    if (naf != null) {
                        logger.info("Writing file " + outputFile);
                        Files.createParentDirs(outputFile);
                        try (Writer w = IO.utf8Writer(IO.buffer(IO.write(outputFile.getAbsolutePath())))) {
                            w.write(naf);
                        }
                    }

                } catch (final Throwable ex) {
                    logger.error(filename + " --- " + ex.getMessage());
                    markFileAsNotDone(filename);

                    try {
                        logger.info("Sleeping...");
                        Thread.sleep(DEFAULT_SLEEPING_TIME);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }

    }

    private File getOutputFile(File inputFile, TintopSession session) {
        String outputFile = session.getOutput().getAbsolutePath() + inputFile.getAbsolutePath()
                .substring(session.getInput().getAbsolutePath().length());
        return new File(outputFile);
    }

    synchronized public String getNextFile(TintopSession session) {

        fIter:
        while (fileCache != null || session.getFileIterator().hasNext()) {

            File file;
            if (fileCache != null) {
                file = new File(fileCache);
                fileCache = null;
            } else {
                file = session.getFileIterator().next();
            }

            File outputFile = getOutputFile(file, session);

            // todo: use parameters
            outputFile = new File(outputFile.getAbsolutePath() + ".gz");

            logger.debug("Output file: " + outputFile);

            if (outputFile.exists()) {
                logger.debug("Skipping file (it exists): " + file);
                continue fIter;
            }

            for (String p : session.getSkipPatterns()) {
                if (file.toString().contains(p)) {
                    logger.debug("Skipping file (skip pattern): " + file);
                    continue fIter;
                }
            }

            if (maxSize > 0 && file.length() > maxSize) {
                logger.debug("Skipping file (too big, " + file.length() + "): " + file);
                skipped++;
                continue fIter;
            }

            // File is empty
            if (file.length() < 1000) {
                try {
                    KAFDocument document = KAFDocument.createFromFile(file);
                    if (document.getRawText() == null || document.getRawText().trim().length() == 0) {
                        logger.info("File is empty: " + file);
                        logger.info("Writing empty file " + outputFile);
                        Files.createParentDirs(outputFile);
                        try (Writer w = IO.utf8Writer(IO.buffer(IO.write(outputFile.getAbsolutePath())))) {
                            w.write(document.toString());
                        }
                        continue fIter;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    skipped++;
                    continue fIter;
                } catch (JDOMException e) {
                    e.printStackTrace();
                    skipped++;
                    continue fIter;
                }
            }

            return file.getAbsolutePath();
        }

        return null;
    }

    public void run(TintopSession session, AnnotationPipeline pipeline, int size) {
        logger.info(String.format("Started process with %d server(s)", size));

        final ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("client-%02d").build();
        final ExecutorService executor = Executors.newCachedThreadPool(factory);
        try {
            for (int i = 0; i < size; i++) {
                executor.submit(new LocalTintopClient(session, pipeline));
            }

            executor.shutdown();
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);

        } catch (final InterruptedException ex) {
            // ignore

        } finally {
            executor.shutdownNow();
        }

    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./orchestrator-folder")
                    .withHeader("Run the Tintop Orchestrator in a particular folder")
                    .withOption("i", "input", "Input folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption(null, "skip", "Text file with list of file patterns to skip (one per line)", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, false)
                    .withOption("m", "max-fail",
                            String.format("Max fails on a single file to skip (default %d)", DEFAULT_MAX_ERR_ON_FILE),
                            "INT", CommandLine.Type.INTEGER, true, false, false)
                    .withOption("z", "max-size",
                            String.format("Max size of a NAF empty file (default %d)", DEFAULT_MAX_SIZE),
                            "INT", CommandLine.Type.INTEGER, true, false, false)
                    .withOption("s", "size",
                            String.format("Number of threads (default %d)", DEFAULT_SIZE),
                            "INT", CommandLine.Type.INTEGER, true, false, false)
                    .withOption("c", "config", "Configuration file", "FILE", CommandLine.Type.FILE_EXISTING, true,
                            false, false)
                    .withOption(null, "properties", "Additional properties", "PROPS", CommandLine.Type.STRING, true,
                            true, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File input = cmd.getOptionValue("input", File.class);
            File output = cmd.getOptionValue("output", File.class);
            File skip = cmd.getOptionValue("skip", File.class);
            File configFile = cmd.getOptionValue("config", File.class);

            Integer maxFail = cmd.getOptionValue("max-fail", Integer.class, DEFAULT_MAX_ERR_ON_FILE);
            Integer maxSize = cmd.getOptionValue("max-size", Integer.class, DEFAULT_MAX_SIZE);
            Integer size = cmd.getOptionValue("size", Integer.class, DEFAULT_SIZE);

            List<String> addProperties = cmd.getOptionValues("properties", String.class);
            Properties additionalProps = new Properties();
            for (String property : addProperties) {
                try {
                    additionalProps.load(new StringReader(property));
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }
            }

            HashSet<String> skipPatterns = new HashSet<>();
            if (skip != null) {
                BufferedReader reader = new BufferedReader(new FileReader(skip));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    skipPatterns.add(line);
                }
                reader.close();
            }

            if (!input.exists()) {
                logger.error("Input folder does not exist");
                System.exit(1);
            }

            if (!output.exists()) {
                if (!output.mkdirs()) {
                    logger.error("Unable to create output folder");
                    System.exit(1);
                }
            }

            AnnotationPipeline pipeline = null;
            try {
                pipeline = new AnnotationPipeline(configFile, additionalProps);
                pipeline.loadModels();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                System.exit(1);
            }

            FolderOrchestrator orchestrator = new FolderOrchestrator();
            orchestrator.setMaxErrOnFile(maxFail);
            orchestrator.setMaxSize(maxSize);

            String[] extensions = DEFAULT_EXTENSIONS;
            Iterator<File> fileIterator = FileUtils.iterateFiles(input, extensions, true);

            TintopSession session = new TintopSession(input, output, fileIterator, skipPatterns);
            orchestrator.run(session, pipeline, size);

            logger.info("Skipped: {}", orchestrator.skipped);

//            String naf = request.getParameter("naf");
//            KAFDocument doc;
//
//            try {
//                doc = pipeline.parseFromString(naf);
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new Exception(e);
//            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }
}
