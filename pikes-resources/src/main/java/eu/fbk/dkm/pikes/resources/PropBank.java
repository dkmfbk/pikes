package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.StaxParser;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PropBank {

    private static final List<Roleset> ROLESETS;

    private static final Map<String, Roleset> ID_INDEX;

    private static final ListMultimap<String, Roleset> LEMMA_INDEX;

    static {
        try {
            final Map<String, int[]> corefMap = Maps.newHashMap();
            for (final String line : Resources.readLines(
                    PropBank.class.getResource("eu.fbk.dkm.pikes.resources.PropBank.coref"), Charsets.UTF_8)) {
                final String[] tokens = line.split("\\s+");
                final int[] roles = new int[] { Integer.parseInt(tokens[1]),
                        Integer.parseInt(tokens[2]) };
                corefMap.put(tokens[0], roles);
            }

            final Map<String, Roleset> idIndex = Maps.newLinkedHashMap();
            final ListMultimap<String, Roleset> lemmaIndex = ArrayListMultimap.create();

            final BufferedReader reader = Resources.asCharSource(
                    PropBank.class.getResource("eu.fbk.dkm.pikes.resources.PropBank.tsv"), Charsets.UTF_8)
                    .openBufferedStream();

            String line;
            while ((line = reader.readLine()) != null) {

                // Extract frame data
                final String[] tokens = Iterables.toArray(Splitter.on('\t').split(line),
                        String.class);
                final String id = tokens[0];
                final String lemma = tokens[1];
                final String name = tokens[2];
                final List<String> vnFrames = Splitter.on('|').splitToList(tokens[3]);
                final List<String> fnFrames = Splitter.on('|').splitToList(tokens[4]);
                final List<String> eventTypes = Splitter.on('|').splitToList(tokens[5]);

                // Extract role data
                final List<String> argDescr = Lists.newArrayList();
                final List<List<String>> argVNRoles = Lists.newArrayList();
                final List<List<String>> argFNRoles = Lists.newArrayList();
                for (int i = 0; i < 6; ++i) {
                    argDescr.add(null);
                    argVNRoles.add(null);
                    argFNRoles.add(null);
                }
                for (int i = 6; i + 3 < tokens.length; i += 4) {
                    final int num = Integer.parseInt(tokens[i]);
                    argDescr.set(num, tokens[i + 1]);
                    argVNRoles.set(num, Splitter.on('|').splitToList(tokens[i + 2]));
                    argFNRoles.set(num, Splitter.on('|').splitToList(tokens[i + 3]));
                }

                // Create and index the roleset
                final int[] corefRoles = corefMap.get(id);
                final int entityRole = corefRoles == null ? -1 : corefRoles[0];
                final int predicateRole = corefRoles == null ? -1 : corefRoles[1];
                final Roleset roleset = new Roleset(id, lemma, name, vnFrames, fnFrames,
                        eventTypes, argDescr, argVNRoles, argFNRoles, entityRole, predicateRole);
                idIndex.put(id, roleset);
                lemmaIndex.put(lemma, roleset);
            }

            reader.close();

            ROLESETS = ImmutableList.copyOf(idIndex.values());
            ID_INDEX = ImmutableMap.copyOf(idIndex);
            LEMMA_INDEX = ImmutableListMultimap.copyOf(lemmaIndex);

        } catch (final IOException ex) {
            throw new Error("Cannot load eu.fbk.dkm.pikes.resources.PropBank data", ex);
        }
    }

    @Nullable
    public static Roleset getRoleset(@Nullable final String id) {
        return ID_INDEX.get(id == null ? null : id.toLowerCase());
    }

    public static List<Roleset> getRolesets(@Nullable final String lemma) {
        return LEMMA_INDEX.get(lemma == null ? null : lemma.toLowerCase());
    }

    public static List<Roleset> getRolesets() {
        return ROLESETS;
    }

    public static void main(final String[] args) throws IOException, XMLStreamException {

        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("PropBankBank")
                    .withHeader(
                            "Generate a TSV file with indexed eu.fbk.dkm.pikes.resources.PropBank data, "
                                    + "including mapping to eu.fbk.dkm.pikes.resources.VerbNet and eu.fbk.dkm.pikes.resources.FrameNet from the PredicateMatrix")
                    .withOption("f", "frames", "the directory containing frame definitions",
                            "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("m", "matrix", "the file containing the predicate matrix", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk.nafview")).parse(args);

            final File dir = cmd.getOptionValue("f", File.class);
            final File pm = cmd.getOptionValue("m", File.class);
            final File output = cmd.getOptionValue("o", File.class);

            // Parse the predicate matrix
            final Matrix matrix = new Matrix(pm);

            final Writer writer = new OutputStreamWriter(new BufferedOutputStream(
                    new FileOutputStream(output)), Charsets.UTF_8);

            final File[] files = dir.listFiles();
            Arrays.sort(files);

            for (final File file : files) {
                if (file.getName().endsWith(".xml")) {
                    System.out.println("Processing " + file);
                    final Reader reader = new BufferedReader(new FileReader(file));
                    try {
                        new Parser(reader, matrix).parse(writer);
                    } finally {
                        reader.close();
                    }
                }
            }

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

    private static class Matrix {

        final Multimap<String, String> vnFrames;

        final Multimap<String, String> fnFrames;

        final Multimap<String, String> eventTypes;

        final Multimap<String, String> vnRoles;

        final Multimap<String, String> fnRoles;

        Matrix(final File file) throws IOException {

            this.vnFrames = HashMultimap.create();
            this.fnFrames = HashMultimap.create();
            this.eventTypes = HashMultimap.create();
            this.vnRoles = HashMultimap.create();
            this.fnRoles = HashMultimap.create();

            parseMatrix(file);
        }

        private void parseMatrix(final File matrixFile) throws IOException {

            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(matrixFile), Charsets.UTF_8));

            try {
                // Process the predicate matrix file one line at a time
                String line;
                while ((line = in.readLine()) != null) {

                    // Split the line in its cells. Skip line if there are not enough cells
                    final String[] tokens = line.split("\t");
                    if (tokens.length <= 18) {
                        continue;
                    }

                    // Extract the eu.fbk.dkm.pikes.resources.PropBank frame and role. Skip line if NULL
                    final String pbFrame = parseMatrixValue(tokens[11]);
                    if (pbFrame == null) {
                        continue;
                    }
                    final String pbRole = parseMatrixValue(tokens[12]);
                    final String pbFrameRole = pbFrame + pbRole;

                    // Extract and index eu.fbk.dkm.pikes.resources.VerbNet data: class, subclass, role
                    final String vnClass = parseMatrixValue(tokens[0]);
                    final String vnSubClass = parseMatrixValue(tokens[2]);
                    final String vnFrame = vnSubClass != null ? vnSubClass : vnClass;
                    final String vnRole = parseMatrixValue(tokens[5]);
                    if (vnSubClass != null && vnClass != null && !vnSubClass.startsWith(vnClass)) {
                        System.err.println("Unexpected VN class / subclass pair: " + vnClass
                                + ", " + vnSubClass);
                    }
                    if (vnFrame != null) {
                        this.vnFrames.put(pbFrame, vnFrame);
                        if (vnRole != null) {
                            this.vnRoles.put(pbFrameRole, vnRole);
                        }
                    }

                    // Extract and index eu.fbk.dkm.pikes.resources.FrameNet data: frame and frame element
                    final String fnFrame = parseMatrixValue(tokens[8]);
                    final String fnRole = parseMatrixValue(tokens[10]);
                    if (fnFrame != null) {
                        this.fnFrames.put(pbFrame, fnFrame);
                        if (fnRole != null) {
                            this.fnRoles.put(pbFrameRole, fnRole);
                        }
                    }

                    // Extract and index event type
                    final String eventType = parseMatrixValue(tokens[17]);
                    if (eventType != null) {
                        this.eventTypes.put(pbFrame, eventType);
                    }
                }
            } finally {
                in.close();
            }
        }

        @Nullable
        private static String parseMatrixValue(@Nullable String string) {

            if (string != null) {

                // Skip an optional prefix (e.g., pb:)
                final int index = string.indexOf(':');
                if (index > 0) {
                    string = string.substring(index + 1);
                }

                // Return the value only if not NULL
                if (!"NULL".equalsIgnoreCase(string)) {
                    return string;
                }
            }
            return null;
        }

    }

    private static class Parser extends StaxParser {

        private final Matrix matrix;

        Parser(final Reader reader, @Nullable final Matrix matrix) throws IOException {
            super(reader);
            this.matrix = matrix;
        }

        void parse(final Writer writer) throws IOException, XMLStreamException {
            enter("frameset");
            while (tryEnter("predicate")) {

                // Extract the lemma (may be different from the one in the ID
                final String lemma = attribute("lemma").trim().replace('_', ' ').toLowerCase();

                // Process rolesets for the current predicate lemma
                while (tryEnter("roleset")) {

                    // Extract eu.fbk.dkm.pikes.resources.PropBank sense and associated description
                    final String id = attribute("id").trim();
                    final String name = attribute("name").trim();

                    // Retrieve frame data from the predicate matrix
                    final String vnFrames = Joiner.on('|').join(
                            Ordering.natural().sortedCopy(this.matrix.vnFrames.get(id)));
                    final String fnFrames = Joiner.on('|').join(
                            Ordering.natural().sortedCopy(this.matrix.fnFrames.get(id)));
                    final String eventTypes = Joiner.on('|').join(
                            Ordering.natural().sortedCopy(this.matrix.eventTypes.get(id)));

                    // Emit frame data
                    writer.write(id);
                    writer.write('\t');
                    writer.write(lemma);
                    writer.write('\t');
                    writer.write(name);
                    writer.write('\t');
                    writer.write(vnFrames);
                    writer.write('\t');
                    writer.write(fnFrames);
                    writer.write('\t');
                    writer.write(eventTypes);

                    // Process eu.fbk.dkm.pikes.resources.PropBank roles for current roleset
                    if (tryEnter("roles")) {
                        while (tryEnter("role")) {
                            try {

                                // Extract role number and associated description
                                final int n = Integer.parseInt(attribute("n"));
                                final String descr = attribute("descr").trim();

                                // Retrieve role data from the predicate matrix
                                final String roleId = id + n;
                                final String vnRoles = Joiner.on('|').join(
                                        Ordering.natural().sortedCopy(
                                                this.matrix.vnRoles.get(roleId)));
                                final String fnRoles = Joiner.on('|').join(
                                        Ordering.natural().sortedCopy(
                                                this.matrix.fnRoles.get(roleId)));

                                // Emit role data
                                writer.write('\t');
                                writer.write(Integer.toString(n));
                                writer.write('\t');
                                writer.write(Strings.nullToEmpty(descr));
                                writer.write('\t');
                                writer.write(vnRoles);
                                writer.write('\t');
                                writer.write(fnRoles);

                            } catch (final NumberFormatException ex) {
                                // ignore
                            }
                            leave();
                        }
                        leave();
                    }

                    // End and flush the line
                    writer.write('\n');
                    writer.flush();
                    leave();
                }
                leave();
            }
            leave();
        }

    }

    public static final class Roleset {

        private static final Interner<Object> INTERNER = Interners.newStrongInterner();

        private final String id;

        private final String lemma;

        private final String descr;

        private final List<String> vnFrames;

        private final List<String> fnFrames;

        private final List<String> eventTypes;

        private final String[] argDescr;

        private final List<String>[] argVNRoles;

        private final List<String>[] argFNRoles;

        private final int coreferenceEntityArg;

        private final int coreferencePredicateArg;

        @Nullable
        private List<Integer> argNums;

        Roleset(final String id, final String lemma, final String descr,
                final Iterable<String> argDescr) {
            this(id, lemma, descr, null, null, null, argDescr, null, null, -1, -1);
        }

        Roleset(final String id, final String lemma, final String descr,
                final Iterable<String> vnFrames, final Iterable<String> fnFrames,
                final Iterable<String> eventTypes, final Iterable<String> argDescr,
                final Iterable<? extends Iterable<String>> argVNRoles,
                final Iterable<? extends Iterable<String>> argFNRoles,
                final int coreferenceEntityArg, final int coreferencePredicateArg) {

            this.id = id;
            this.lemma = (String) INTERNER.intern(lemma);
            this.descr = descr;
            this.vnFrames = internList(vnFrames);
            this.fnFrames = internList(fnFrames);
            this.eventTypes = internList(eventTypes);
            this.argDescr = Iterables.toArray(argDescr, String.class);
            this.argVNRoles = internListArray(argVNRoles);
            this.argFNRoles = internListArray(argFNRoles);
            this.argNums = null;
            this.coreferenceEntityArg = coreferenceEntityArg;
            this.coreferencePredicateArg = coreferencePredicateArg;
        }

        public String getID() {
            return this.id;
        }

        public String getLemma() {
            return this.lemma;
        }

        public String getDescr() {
            return this.descr;
        }

        public List<String> getVNFrames() {
            return this.vnFrames;
        }

        public List<String> getFNFrames() {
            return this.fnFrames;
        }

        public List<String> getEventTypes() {
            return this.eventTypes;
        }

        @SuppressWarnings("unchecked")
        public List<Integer> getArgNums() {
            if (this.argNums == null) {
                final ImmutableList.Builder<Integer> builder = ImmutableList.builder();
                for (int i = 0; i < this.argDescr.length; ++i) {
                    if (!Strings.isNullOrEmpty(this.argDescr[i])) {
                        builder.add(i);
                    }
                }
                this.argNums = (List<Integer>) INTERNER.intern(builder.build());
            }
            return this.argNums;
        }

        public String getArgDescr(final int argNum) {
            return this.argDescr[argNum];
        }

        public List<String> getArgVNRoles(final int argNum) {
            return argNum < this.argVNRoles.length ? this.argVNRoles[argNum] : ImmutableList
                    .<String>of();
        }

        public List<String> getArgFNRoles(final int argNum) {
            return argNum < this.argFNRoles.length ? this.argFNRoles[argNum] : ImmutableList
                    .<String>of();
        }

        public int getCoreferenceEntityArg() {
            return this.coreferenceEntityArg;
        }

        public int getCoreferencePredicateArg() {
            return this.coreferencePredicateArg;
        }

        @Override
        public String toString() {
            return this.id;
        }

        @SuppressWarnings("unchecked")
        private static List<String> internList(@Nullable final Iterable<String> strings) {
            List<String> list = Lists.newArrayList();
            if (strings != null) {
                for (final String string : strings) {
                    if (string != null) {
                        list.add((String) INTERNER.intern(string));
                    }
                }
            }
            Collections.sort(list);
            list = ImmutableList.copyOf(list);
            return (List<String>) INTERNER.intern(list);
        }

        @SuppressWarnings({ "unchecked" })
        private static List<String>[] internListArray(
                @Nullable final Iterable<? extends Iterable<String>> stringLists) {
            final List<List<String>> list = Lists.newArrayList();
            if (stringLists != null) {
                for (final Iterable<String> stringList : stringLists) {
                    list.add(internList(stringList));
                }
            }
            return list.toArray(new List[list.size()]);
        }

    }

}
