package eu.fbk.dkm.pikes.rdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.util.Environment;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * Main method.
     *
     * @param args
     *            command line arguments
     */
    public static void main(final String... args) {

        System.setProperty("rdfpro.environment.sources", "pikesrdf.properties");
        
        try {
            Class.forName("eu.fbk.rdfpro.tql.TQL");
        } catch (final Throwable ex) {
            // ignore - TQL will not be supported
        }

        boolean showHelp = false;
        boolean showVersion = false;
        boolean verbose = false;

        int index = 0;
        while (index < args.length) {
            final String arg = args[index];
            if (!arg.startsWith("-")) {
                break;
            }
            showHelp |= arg.equals("-h");
            showVersion |= arg.equals("-v");
            verbose |= arg.equals("-V");
            ++index;
        }
        showHelp |= index == args.length;

        try {
            for (final String name : new String[] { "eu.fbk.dkm", "org.openrdf" }) {
                final Logger logger = LoggerFactory.getLogger(name);
                final Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
                final Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
                final Object level = levelClass.getDeclaredMethod("valueOf", String.class).invoke(
                        null, verbose ? "DEBUG" : "INFO");
                loggerClass.getDeclaredMethod("setLevel", levelClass).invoke(logger, level);
            }
        } catch (final Throwable ex) {
            // ignore - no control on logging level
        }

        if (showVersion) {
            System.out.println(String.format("NAF Tools %s\nJava %s bit (%s) %s\n"
                    + "This is free software released into the public domain",
                    readVersion("eu.fbk.dkm.pikes", "pikes-rdf", "unknown version"),
                    System.getProperty("sun.arch.data.model"), System.getProperty("java.vendor"),
                    System.getProperty("java.version")));
            System.exit(0);
        }

        if (showHelp) {
            System.out.println(String.format(readResource(Main.class.getResource("help")),
                    readVersion("eu.fbk.dkm.pikes", "pikes-rdf", "unknown version"),
                    readPluginDocs()));
            System.exit(0);
        }

        Runnable runnable = null;
        try {
            final String command = args[index];
            runnable = Environment.newPlugin(Runnable.class, command,
                    Arrays.copyOfRange(args, index + 1, args.length));
        } catch (final IllegalArgumentException ex) {
            System.err.println("INVOCATION ERROR. " + ex.getMessage() + "\n");
            System.exit(1);
        }

        try {
            final long ts = System.currentTimeMillis();
            runnable.run();
            LOGGER.info("Done in {} s", (System.currentTimeMillis() - ts) / 1000);
            System.exit(0);

        } catch (final Throwable ex) {
            System.err.println("EXECUTION FAILED. " + ex.getMessage() + "\n");
            ex.printStackTrace();
            System.exit(2);
        }
    }

    @Nullable
    private static String readVersion(final String groupId, final String artifactId,
            @Nullable final String defaultValue) {

        final URL url = RDFProcessor.class.getClassLoader().getResource(
                "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");

        if (url != null) {
            try {
                final InputStream stream = url.openStream();
                try {
                    final Properties properties = new Properties();
                    properties.load(stream);
                    return properties.getProperty("version").trim();
                } finally {
                    stream.close();
                }

            } catch (final IOException ex) {
                LOGGER.warn("Could not parse version string in " + url);
            }
        }

        return defaultValue;
    }

    private static String readResource(final URL url) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(
                    url.openStream(), Charset.forName("UTF-8")));
            final StringBuilder builder = new StringBuilder();
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            } finally {
                reader.close();
            }
            return builder.toString();
        } catch (final Throwable ex) {
            throw new Error("Could not load resource " + url);
        }
    }

    private static String readPluginDocs() {

        final Map<String, String> descriptions = Environment.getPlugins(Runnable.class);
        final List<String> names = new ArrayList<String>(descriptions.keySet());
        Collections.sort(names);

        final StringBuilder builder = new StringBuilder();
        for (final String name : names) {
            builder.append(builder.length() == 0 ? "" : "\n\n");
            builder.append(descriptions.get(name).trim());
        }

        return builder.toString();
    }

}
