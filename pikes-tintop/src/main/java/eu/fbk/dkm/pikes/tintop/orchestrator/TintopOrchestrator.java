package eu.fbk.dkm.pikes.tintop.orchestrator;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.FrequencyHashSet;
import eu.fbk.rdfpro.util.IO;
import ixa.kaflib.KAFDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.JDOMException;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by alessio on 25/02/15.
 */

public class TintopOrchestrator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TintopOrchestrator.class);
    static final private int DEFAULT_MAX_ERR_ON_FILE = 5;
    static final private int DEFAULT_MAX_SIZE = 50000;
    static final private int DEFAULT_SLEEPING_TIME = 60000;

    private ArrayList<TintopServer> servers;
    private boolean fake;
    private String fileCache = null;
    public static String[] DEFAULT_EXTENSIONS = new String[] { "xml", "naf" };

    private int skipped = 0;

    private FrequencyHashSet<String> fileOnError = new FrequencyHashSet<>();

    private int maxErrOnFile = DEFAULT_MAX_ERR_ON_FILE;
    private int maxSize = DEFAULT_MAX_SIZE;
    private int timeout = TintopClient.DEFAULT_TIMEOUT;
    private int sleepingTime = DEFAULT_SLEEPING_TIME;

    public class RunnableTintopClient extends TintopClient implements Runnable {

        TintopSession session;

        public RunnableTintopClient(TintopServer server, TintopSession session) {
            this(server, session, false);
        }

        public RunnableTintopClient(TintopServer server, TintopSession session, boolean fake) {
            super(server);
            super.setFake(fake);
            this.session = session;
            setTimeout(timeout);
        }

        @Override
        public void run() {
            Thread.currentThread().setName(server.getId() + " - " + server.getShortName());

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

                    String naf = call(whole);
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
                        Thread.sleep(sleepingTime);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }

    }

    public TintopOrchestrator(ArrayList<TintopServer> servers, boolean fake) {
        this.servers = servers;
        this.fake = fake;
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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getSleepingTime() {
        return sleepingTime;
    }

    public void setSleepingTime(int sleepingTime) {
        this.sleepingTime = sleepingTime;
    }

    private File getOutputFile(File inputFile, TintopSession session) {
        String outputFile = session.getOutput().getAbsolutePath() + inputFile.getAbsolutePath()
                .substring(session.getInput().getAbsolutePath().length());
        return new File(outputFile);
    }

    synchronized public void markFileAsNotDone(String filename) {
        fileOnError.add(filename);
        if (fileOnError.get(filename) <= maxErrOnFile) {
            fileCache = filename;

        } else {
            logger.warn(String.format("File %s skipped, more than %d errors", filename, DEFAULT_MAX_ERR_ON_FILE));
        }
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

    public void run(TintopSession session) {
        logger.info(String.format("Started process with %d server(s)", servers.size()));

        final ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("client-%02d").build();
        final ExecutorService executor = Executors.newCachedThreadPool(factory);
        try {
            for (TintopServer server : servers) {
                executor.submit(new RunnableTintopClient(server, session, fake));
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
            final eu.fbk.dkm.utils.CommandLine cmd = eu.fbk.dkm.utils.CommandLine
                    .parser()
                    .withName("./orchestrator")
                    .withHeader("Run the Tintop Orchestrator")
                    .withOption("i", "input", "Input folder", "FOLDER",
                            eu.fbk.dkm.utils.CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            eu.fbk.dkm.utils.CommandLine.Type.DIRECTORY, true, false, true)
                    .withOption("l", "list", "Text file with list of server (one per line)", "FILE",
                            eu.fbk.dkm.utils.CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("s", "skip", "Text file with list of file patterns to skip (one per line)", "FILE",
                            eu.fbk.dkm.utils.CommandLine.Type.FILE_EXISTING, true, false, false)
                    .withOption("m", "max-fail",
                            String.format("Max fails on a single file to skip (default %d)", DEFAULT_MAX_ERR_ON_FILE),
                            "INT", CommandLine.Type.INTEGER, true, false, false)
                    .withOption("z", "max-size",
                            String.format("Max size of a NAF empty file (default %d)", DEFAULT_MAX_SIZE),
                            "INT", CommandLine.Type.INTEGER, true, false, false)
                    .withOption("t", "timeout",
                            String.format("Timeout in ms (default %d)", TintopClient.DEFAULT_TIMEOUT),
                            "INT", CommandLine.Type.INTEGER, true, false, false)
                    .withOption(null, "sleeping-time",
                            String.format("Sleeping time for servers in ms (default %d)", DEFAULT_SLEEPING_TIME),
                            "INT", CommandLine.Type.INTEGER, true, false, false)
                    .withOption("F", "fake", "Fake execution")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File input = cmd.getOptionValue("input", File.class);
            File output = cmd.getOptionValue("output", File.class);
            File serverList = cmd.getOptionValue("list", File.class);
            File skip = cmd.getOptionValue("skip", File.class);

            Integer maxFail = cmd.getOptionValue("max-fail", Integer.class, DEFAULT_MAX_ERR_ON_FILE);
            Integer maxSize = cmd.getOptionValue("max-size", Integer.class, DEFAULT_MAX_SIZE);
            Integer timeout = cmd.getOptionValue("timeout", Integer.class, TintopClient.DEFAULT_TIMEOUT);
            Integer sleepingTime = cmd.getOptionValue("sleeping-time", Integer.class, DEFAULT_SLEEPING_TIME);

            boolean fake = cmd.hasOption("fake");

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

            ArrayList<TintopServer> tintopServers = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(serverList));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                try {
                    TintopServer c = new TintopServer(line.trim());
                    tintopServers.add(c);
                } catch (Exception e) {
                    // ignore
                }
            }

            String[] extensions = DEFAULT_EXTENSIONS;
            Iterator<File> fileIterator = FileUtils.iterateFiles(input, extensions, true);

            TintopOrchestrator orchestrator = new TintopOrchestrator(tintopServers, fake);
            orchestrator.setMaxErrOnFile(maxFail);
            orchestrator.setMaxSize(maxSize);
            orchestrator.setTimeout(timeout);
            orchestrator.setSleepingTime(sleepingTime);

            TintopSession session = new TintopSession(input, output, fileIterator, skipPatterns);
            orchestrator.run(session);

            logger.info("Skipped: {}", orchestrator.skipped);

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
