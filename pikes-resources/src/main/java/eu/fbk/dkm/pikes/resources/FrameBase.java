package eu.fbk.dkm.pikes.resources;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.io.Resources;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.CommandLine.Type;
import eu.fbk.rdfpro.AbstractRDFHandler;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.Statements;

public final class FrameBase {

    public static final String NAMESPACE = "http://framebase.org/ns/";

    private static final Map<String, String> CLASS_MAP;

    private static final Map<String, String> PROPERTY_MAP;

    private static final Set<String> NAME_SET;

    static {
        try {
            final ImmutableMap.Builder<String, String> classBuilder = ImmutableMap.builder();
            final ImmutableMap.Builder<String, String> propertyBuilder = ImmutableMap.builder();
            final ImmutableSet.Builder<String> namesBuilder = ImmutableSet.builder();

            final BufferedReader reader = Resources.asCharSource(
                    FrameBase.class.getResource("FrameBase.tsv"), Charsets.UTF_8)
                    .openBufferedStream();

            String line;
            while ((line = reader.readLine()) != null) {
                final String[] tokens = line.split("\t");
                final String name = tokens[0];
                namesBuilder.add(name);
                for (int i = 1; i < tokens.length; ++i) {
                    final String key = tokens[i];
                    if (key.indexOf('@') >= 0) {
                        propertyBuilder.put(key, name);
                    } else {
                        classBuilder.put(key, name);
                    }
                }
            }
            reader.close();

            CLASS_MAP = classBuilder.build();
            PROPERTY_MAP = propertyBuilder.build();
            NAME_SET = namesBuilder.build();

        } catch (final IOException ex) {
            throw new Error("Cannot load eu.fbk.dkm.pikes.resources.FrameBase data", ex);
        }
    }

    public static URI classFor(final String fnFrame, final String predicateLemma,
            final POS predicatePos) {
        final String key = classKeyFor(fnFrame, predicateLemma, predicatePos);
        String name = CLASS_MAP.get(key);
        if (name == null) {
            name = classNameFor(key);
            if (!NAME_SET.contains(name)) {
                return null;
            }
        }
        return Statements.VALUE_FACTORY.createURI(NAMESPACE, name);
    }

    public static URI propertyFor(final String fnFrame, final String fnFE) {
        final String key = propertyKeyFor(fnFrame, fnFE);
        String name = PROPERTY_MAP.get(key);
        if (name == null) {
            name = propertyNameFor(key);
            if (!NAME_SET.contains(name)) {
                return null;
            }
        }
        return Statements.VALUE_FACTORY.createURI(NAMESPACE, name);
    }

    private static String classKeyFor(final String fnFrame, final String predicateLemma,
            final POS predicatePos) {
        return (fnFrame + "#" + predicateLemma + "." + predicatePos.getLetter()) //
                .toLowerCase().replace(' ', '_');
    }

    private static String classNameFor(final String classKey) {
        final int index1 = classKey.lastIndexOf('#');
        final int index2 = classKey.lastIndexOf('.');
        final String frame = Character.toUpperCase(classKey.charAt(0))
                + classKey.substring(1, index1);
        final String lemma = classKey.substring(index1 + 1, index2);
        final String pos = classKey.substring(index2 + 1);
        return "frame-" + frame + "-" + lemma + "." + pos;
    }

    private static String propertyKeyFor(final String fnFrame, final String fnFE) {
        return (fnFrame + '@' + fnFE).toLowerCase().replace(' ', '_');
    }

    private static String propertyNameFor(final String propertyKey) {
        final int index = propertyKey.indexOf('@');
        final String frame = Character.toUpperCase(propertyKey.charAt(0))
                + propertyKey.substring(1, index);
        final String role = Character.toUpperCase(propertyKey.charAt(index + 1))
                + propertyKey.substring(index + 2);
        return "fe-" + frame + "-" + role;
    }

    public static void main(final String... args) {

        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("eu.fbk.dkm.pikes.resources.FrameBase")
                    .withHeader(
                            "Generate a TSV file with indexed eu.fbk.dkm.pikes.resources.FrameBase data")
                    .withOption("i", "input", "the input file containing FrameBase RDF data",
                            "FILE", Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "output file", "FILE", Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final File inputFile = cmd.getOptionValue("i", File.class);
            final File outputFile = cmd.getOptionValue("o", File.class);

            final ValueFactory vf = Statements.VALUE_FACTORY;
            final URI inheritsFrom = vf.createURI("http://framebase.org/ns/inheritsFrom");
            final URI denotedBy = vf.createURI("http://www.w3.org/ns/lemon/ontolex#isDenotedBy");
            final String self = "<SELF>";

            final Multimap<String, String> map = HashMultimap.create();

            final Map<String, String> frameParents = Maps.newHashMap();
            final Multimap<String, String> frameEntries = HashMultimap.create();

            final RDFSource source = RDFSources.read(false, true, null, null,
                    inputFile.getAbsolutePath());
            source.emit(new AbstractRDFHandler() {

                @Override
                public void handleStatement(final Statement stmt) throws RDFHandlerException {
                    if (stmt.getSubject() instanceof URI && stmt.getObject() instanceof URI) {

                        final URI s = (URI) stmt.getSubject();
                        final URI p = stmt.getPredicate();
                        final URI o = (URI) stmt.getObject();
                        final String sn = s.getLocalName();
                        final String on = o.getLocalName();

                        if (p.equals(RDFS.DOMAIN)) {
                            if (sn.startsWith("fe-") && on.startsWith("frame-")) {
                                final String frame = on.substring("frame-".length());
                                final String fe = sn.substring("fe-".length() + frame.length() + 1);
                                final String key = propertyKeyFor(frame, fe);
                                final String name = propertyNameFor(key);
                                if (!name.equals(sn)) {
                                    map.put(sn, key);
                                }
                                map.put(sn, self);
                            }

                        } else if (p.equals(denotedBy)) {
                            if (sn.startsWith("frame-")) {
                                frameEntries.put(sn, on);
                            }

                        } else if (p.equals(inheritsFrom)) {
                            if (sn.startsWith("frame-") && on.startsWith("frame-")) {
                                frameParents.put(sn, on);
                            }
                        }
                    }
                }

            }, 1);

            for (final Map.Entry<String, String> entry : frameEntries.entries()) {
                final int index = entry.getValue().indexOf("-");
                final POS pos = POS.forFrameNetTag(entry.getValue().substring(0, index));
                final String lemma = entry.getValue().substring(index + 1);
                final String frame = frameParents.get(entry.getKey()).substring("frame-".length());
                final String key = classKeyFor(frame, lemma, pos);
                final String name = classNameFor(key);
                if (!name.equals(entry.getKey())) {
                    map.put(entry.getKey(), key);
                }
                map.put(entry.getKey(), self);
            }

            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(
                    new FileOutputStream(outputFile)), Charsets.UTF_8)) {
                for (final String name : Ordering.natural().sortedCopy(map.keySet())) {
                    writer.write(name);
                    for (final String key : Ordering.natural().sortedCopy(map.get(name))) {
                        if (!self.equals(key)) {
                            writer.write("\t");
                            writer.write(key);
                        }
                    }
                    writer.write("\n");
                }
            }

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

    private FrameBase() {
    }

    public enum POS {

        NOUN('n'),

        VERB('v'),

        ADJECTIVE('a'),

        OTHER('c');

        private final char letter;

        private POS(final char letter) {
            this.letter = letter;
        }

        public char getLetter() {
            return this.letter;
        }

        public static POS forFrameNetTag(String tag) {
            tag = tag.toLowerCase();
            if ("n".equals(tag)) {
                return NOUN;
            } else if ("a".equals(tag)) {
                return ADJECTIVE;
            } else if ("v".equals(tag)) {
                return VERB;
            } else {
                return OTHER;
            }
        }

        public static POS forPennTag(String tag) {
            tag = tag.toUpperCase();
            if (tag.startsWith("NN")) {
                return NOUN;
            } else if (tag.startsWith("VB")) {
                return VERB;
            } else if (tag.startsWith("JJ")) {
                return ADJECTIVE;
            } else {
                return OTHER;
            }
        }

    }

}
