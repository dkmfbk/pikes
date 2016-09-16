package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import eu.fbk.utils.core.StaxParser;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NomBank {

    private static final List<Roleset> ROLESETS;

    private static final Map<String, Roleset> ID_INDEX;

    private static final ListMultimap<String, Roleset> LEMMA_INDEX;

    private static final ListMultimap<String, Roleset> PB_ID_INDEX;

    static {
        try {
            final Map<String, Roleset> idIndex = Maps.newLinkedHashMap();
            final ListMultimap<String, Roleset> lemmaIndex = ArrayListMultimap.create();
            final ListMultimap<String, Roleset> pbIdIndex = ArrayListMultimap.create();

            final BufferedReader reader = Resources.asCharSource(
                    NomBank.class.getResource("NomBank.tsv"), Charsets.UTF_8).openBufferedStream();

            String line;
            while ((line = reader.readLine()) != null) {
                final String[] tokens = line.split("\t");
                final String id = tokens[0];
                final String pbId = Strings.emptyToNull(tokens[1]);
                final String lemma = tokens[2];
                final String descr = tokens[3];
                final String[] argDescr = Arrays.copyOfRange(tokens, 4, 13);
                byte[] argPBNums = null;
                if (pbId != null) {
                    argPBNums = new byte[argDescr.length];
                    for (int i = 0; i < argPBNums.length; ++i) {
                        argPBNums[i] = Byte.parseByte(tokens[14 + i]);
                    }
                }
                final List<Integer> mandatoryArgs = Lists.newArrayList();
                final List<Integer> optionalArgs = Lists.newArrayList();
                if (tokens.length > 24 && !tokens[24].equals("")) {
                    for (final String arg : Ordering.natural().sortedCopy(
                            Arrays.asList(tokens[24].split("\\s+")))) {
                        mandatoryArgs.add(Integer.parseInt(arg));
                    }
                }
                if (tokens.length > 25 && !tokens[25].equals("")) {
                    for (final String arg : Ordering.natural().sortedCopy(
                            Arrays.asList(tokens[25].split("\\s+")))) {
                        optionalArgs.add(Integer.parseInt(arg));
                    }
                }
                final Roleset roleset = new Roleset(id, pbId, lemma, descr, argPBNums, argDescr,
                        mandatoryArgs, optionalArgs);
                idIndex.put(id, roleset);
                lemmaIndex.put(lemma, roleset);
                if (pbId != null) {
                    pbIdIndex.put(pbId, roleset);
                }
            }

            reader.close();

            ROLESETS = ImmutableList.copyOf(idIndex.values());
            ID_INDEX = ImmutableMap.copyOf(idIndex);
            LEMMA_INDEX = ImmutableListMultimap.copyOf(lemmaIndex);
            PB_ID_INDEX = ImmutableListMultimap.copyOf(pbIdIndex);

        } catch (final IOException ex) {
            throw new Error("Cannot load eu.fbk.dkm.pikes.resources.PropBank data", ex);
        }
    }

    public static Set<String> getIds() {
        return ID_INDEX.keySet();
    }

    public static Set<String> getLemmas() {
        return LEMMA_INDEX.keySet();
    }

    @Nullable
    public static Roleset getRoleset(@Nullable final String id) {
        return ID_INDEX.get(id.toLowerCase());
    }

    public static List<Roleset> getRolesetsForLemma(@Nullable final String lemma) {
        if (lemma == null) {
            return new ArrayList<>();
        }
        return LEMMA_INDEX.get(lemma.toLowerCase());
    }

    public static List<Roleset> getRolesetsForPBId(final String pbId) {
        return PB_ID_INDEX.get(pbId.toLowerCase());
    }

    public static List<Roleset> getRolesets() {
        return ROLESETS;
    }

    public static void main(final String[] args) throws IOException, XMLStreamException {

        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("eu.fbk.dkm.pikes.resources.NomBank")
                    .withHeader("Generate a TSV file with indexed eu.fbk.dkm.pikes.resources.NomBank data")
                    .withOption("f", "frames", "the directory containing frame definitions",
                            "DIR", Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("a", "annotations",
                            "the eu.fbk.dkm.pikes.resources.NomBank annotation file (e.g., eu.fbk.dkm.pikes.resources.NomBank.1.0)", "FILE",
                            Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "output file", "FILE", Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk.nafview")).parse(args);

            final File dir = cmd.getOptionValue("f", File.class);
            final File annotations = cmd.getOptionValue("a", File.class);
            final File output = cmd.getOptionValue("o", File.class);

            final Writer writer = new OutputStreamWriter(new BufferedOutputStream(
                    new FileOutputStream(output)), Charsets.UTF_8);

            final File[] files = dir.listFiles();
            Arrays.sort(files);

            final Map<String, Multiset<Integer>> roles = getPredicateRoles(annotations);

            // Manual corrections due to lack of samples
            addPredicateRole(roles, "1-slash-10th.01", -1);
            addPredicateRole(roles, "bagger.01", 0);
            addPredicateRole(roles, "bearer.01", 0);
            addPredicateRole(roles, "being.01", 0);
            addPredicateRole(roles, "being.01", -1);
            addPredicateRole(roles, "caliber.01", 2);
            addPredicateRole(roles, "calling.01", -1);
            addPredicateRole(roles, "clogging.02", -1);
            addPredicateRole(roles, "counting.01", -1);
            addPredicateRole(roles, "crusher.01", 0);
            addPredicateRole(roles, "doer.01", 0);
            addPredicateRole(roles, "dropper.01", -1);
            addPredicateRole(roles, "esteem.01", -1);
            addPredicateRole(roles, "fidelity.01", -1);
            addPredicateRole(roles, "finder.01", 0);
            addPredicateRole(roles, "getter.01", 0);
            addPredicateRole(roles, "goer.01", 0);
            addPredicateRole(roles, "grinder.01", 0);
            addPredicateRole(roles, "implant.01", -1);
            addPredicateRole(roles, "incrimination.01", -1);
            addPredicateRole(roles, "interdiction.01", -1);
            addPredicateRole(roles, "kicker.03", 0);
            addPredicateRole(roles, "purification.01", -1);
            addPredicateRole(roles, "purity.01", -1);
            addPredicateRole(roles, "starter.01", 0);
            addPredicateRole(roles, "stocking.01", -1);
            addPredicateRole(roles, "tech.01", -1);
            addPredicateRole(roles, "tilth.01", -1);
            addPredicateRole(roles, "trick.02", -1);
            addPredicateRole(roles, "tuning.01", -1);

            for (final File file : files) {
                if (file.getName().endsWith(".xml")) {
                    System.out.println("Processing " + file);
                    final Reader reader = new BufferedReader(new FileReader(file));
                    try {
                        new Parser(reader, roles).parse(writer);
                    } finally {
                        reader.close();
                    }
                }
            }

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

    private static void addPredicateRole(final Map<String, Multiset<Integer>> map,
            final String sense, final int arg) {
        Multiset<Integer> set = map.get(sense);
        if (set == null) {
            set = HashMultiset.create();
            map.put(sense, set);
        }
        set.add(-1);
        if (arg != -1) {
            set.add(arg);
        }
    }

    private static Map<String, Multiset<Integer>> getPredicateRoles(final File annotations)
            throws IOException {
        // eg: wsj/22/wsj_2278.mrg 26 18 prisoner 01 9:1-ARG0 12:0,14:0-Support 18:0-ARG1 18:0-rel
        final Pattern rolePattern = Pattern.compile(".*-ARG(\\d).*");
        final Map<String, Multiset<Integer>> map = Maps.newHashMap();
        final BufferedReader reader = new BufferedReader(new FileReader(annotations));
        try {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                ++count;
                final String[] tokens = line.split("\\s+");
                final String sense = tokens[3] + "." + tokens[4];
                Multiset<Integer> set = map.get(sense);
                if (set == null) {
                    set = HashMultiset.create();
                    map.put(sense, set);
                }
                String relIndex = null;
                for (int i = 5; i < tokens.length; ++i) {
                    final String t = tokens[i].toUpperCase();
                    if (t.endsWith("-REL")) {
                        relIndex = t.substring(0, t.length() - 4);
                    }
                    if (t.contains("-H")) {
                        relIndex = null; // multi-words detected: skip sample
                        break;
                    }
                }
                if (relIndex != null) {
                    set.add(-1); // used for counting total sense occurrences
                    for (int i = 5; i < tokens.length; ++i) {
                        final String t = tokens[i].toUpperCase();
                        if (t.startsWith(relIndex)) {
                            final Matcher matcher = rolePattern.matcher(t);
                            if (matcher.matches()) {
                                set.add(Integer.parseInt(matcher.group(1)));
                            }
                        }
                    }
                }
            }
            System.out.println(count + " annotated propositions parsed, " + map.keySet().size()
                    + " senses found");
        } finally {
            reader.close();
        }
        return map;
    }

    private static class Parser extends StaxParser {

        private final Map<String, Multiset<Integer>> roles;

        Parser(final Reader reader, final Map<String, Multiset<Integer>> roles) {
            super(reader);
            this.roles = roles;
        }

        void parse(final Writer writer) throws IOException, XMLStreamException {
            final Pattern rolePattern = Pattern.compile("(\\d).*");
            enter("frameset");
            while (tryEnter("predicate")) {
                final String lemma = attribute("lemma").trim().replace('_', ' ').toLowerCase();
                while (tryEnter("roleset")) {
                    final String id = attribute("id").trim();
                    Multiset<Integer> set = this.roles.get(id);
                    if (set == null) {
                        set = HashMultiset.create();
                        this.roles.put(id, set);
                    }
                    String pbId = attribute("source");
                    if (pbId != null && pbId.startsWith("verb-")) {
                        pbId = pbId.substring(5);
                    }
                    final String name = attribute("name").trim();
                    final String[] argDescr = new String[10];
                    final byte[] argPBNums = new byte[10];
                    Arrays.fill(argPBNums, (byte) -1);
                    if (tryEnter("roles")) {
                        while (tryEnter("role")) {
                            try {
                                final int n = Integer.parseInt(attribute("n"));
                                argDescr[n] = attribute("descr").trim();
                                if (pbId != null) {
                                    final String pbNum = attribute("source");
                                    argPBNums[n] = pbNum == null ? (byte) n : Byte
                                            .parseByte(pbNum);
                                }
                            } catch (final NumberFormatException ex) {
                                // ignore
                            }
                            leave();
                        }
                        leave();
                    }
                    while (tryEnter("example")) {
                        String rel = null;
                        final List<String> args = Lists.newArrayList(Collections.<String>nCopies(
                                10, null));
                        while (tryEnter(null)) {
                            final String num = attribute("n");
                            if (num == null) {
                                rel = content().trim().toLowerCase();
                            } else if (num != null) {
                                final Matcher matcher = rolePattern.matcher(num);
                                if (matcher.matches()) {
                                    args.set(Integer.parseInt(matcher.group(1)), content().trim()
                                            .toLowerCase());
                                }
                            }
                            leave();
                        }
                        // starts with lemma = constraint to drop multi-words (e.g. spy-chaser)
                        if (rel != null && rel.startsWith(lemma)) {
                            set.add(-1);
                            for (int i = 0; i < args.size(); ++i) {
                                if (rel.equals(args.get(i))) {
                                    set.add(i);
                                }
                            }
                        }
                        leave();
                    }

                    if (set.isEmpty()) {
                        System.out.println("WARNING: no predicate roles computed for " + id);
                    }
                    final List<Integer> mandatoryArgs = Lists.newArrayList();
                    final List<Integer> optionalArgs = Lists.newArrayList();
                    final int sampleCount = set.count(-1);
                    for (final Integer num : set.elementSet()) {
                        if (num != -1) {
                            final int argCount = set.count(num);
                            if (argCount >= sampleCount) {
                                mandatoryArgs.add(num);
                            } else if (argCount > 0) {
                                optionalArgs.add(num);
                            }
                        }
                    }
                    Collections.sort(mandatoryArgs);
                    Collections.sort(optionalArgs);

                    writer.write(id);
                    writer.write('\t');
                    if (pbId != null) {
                        writer.write(pbId);
                    }
                    writer.write('\t');
                    writer.write(lemma);
                    writer.write('\t');
                    writer.write(name);
                    for (int i = 0; i < 10; ++i) {
                        writer.write('\t');
                        writer.write(Strings.nullToEmpty(argDescr[i]));
                    }
                    for (int i = 0; i < 10; ++i) {
                        writer.write('\t');
                        writer.write(Byte.toString(argPBNums[i]));
                    }
                    writer.write('\t');
                    writer.write(Joiner.on(' ').join(mandatoryArgs));
                    writer.write('\t');
                    writer.write(Joiner.on(' ').join(optionalArgs));
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

        private static final Interner<List<Integer>> INTERNER = Interners.newStrongInterner();

        private final String id;

        @Nullable
        private final String pbId;

        private final String lemma;

        private final String descr;

        @Nullable
        private final List<Integer> argNums;

        @Nullable
        private final byte[] argPBNums;

        private final String[] argDescr;

        private final List<Integer> predMandatoryArgNums;

        private final List<Integer> predOptionalArgNums;

        Roleset(final String id, @Nullable final String pbId, final String lemma,
                final String descr, @Nullable final byte[] argPBNums, final String[] argDescr,
                final Iterable<Integer> predMandatoryArgNums,
                final Iterable<Integer> predOptionalArgNums) {

            this.id = id;
            this.pbId = pbId;
            this.lemma = lemma;
            this.descr = descr;
            this.argPBNums = argPBNums;
            this.argDescr = argDescr;

            final ImmutableList.Builder<Integer> builder = ImmutableList.builder();
            for (int i = 0; i < this.argDescr.length; ++i) {
                if (this.argDescr[i] != null) {
                    builder.add(i);
                }
            }
            this.argNums = INTERNER.intern(builder.build());
            this.predMandatoryArgNums = INTERNER.intern(Ordering.natural().sortedCopy(
                    predMandatoryArgNums));
            this.predOptionalArgNums = INTERNER.intern(Ordering.natural().sortedCopy(
                    predOptionalArgNums));
        }

        public String getId() {
            return this.id;
        }

        @Nullable
        public String getPBId() {
            return this.pbId;
        }

        public String getLemma() {
            return this.lemma;
        }

        public String getDescr() {
            return this.descr;
        }

        public List<Integer> getArgNums() {
            return this.argNums;
        }

        public String getArgDescr(final int argNum) {
            return this.argDescr[argNum];
        }

        @Nullable
        public int getArgPBNum(final int argNum) {
            if (this.argPBNums == null || this.argDescr[argNum] == null) {
                return -1;
            }
            return this.argPBNums[argNum];
        }

        public List<Integer> getPredMandatoryArgNums() {
            return this.predMandatoryArgNums;
        }

        public List<Integer> getPredOptionalArgNums() {
            return this.predOptionalArgNums;
        }

        @Override
        public String toString() {
            return this.id;
        }

    }

}
