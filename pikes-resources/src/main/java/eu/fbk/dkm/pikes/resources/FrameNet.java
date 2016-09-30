package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Charsets;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FrameNet {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameNet.class);

    private static final Map<Relation, Multimap<String, String>> RELATIONS;

    static {
        try {
            final Map<Relation, ImmutableMultimap.Builder<String, String>> map = Maps.newHashMap();
            for (final Relation relation : Relation.values()) {
                map.put(relation, ImmutableMultimap.builder());
            }
            for (final String line : Resources.readLines(
                    FrameNet.class.getResource("FrameNet.tsv"), Charsets.UTF_8)) {
                final String[] tokens = line.split("\t");
                final Relation relation = Relation.valueOf(tokens[0]);
                final String from = tokens[1];
                final String to = tokens[2];
                map.get(relation).put(from, to);
                if (relation == Relation.USES) {
                    map.get(Relation.IS_USED_BY).put(to, from);
                } else if (relation == Relation.INHERITS_FROM) {
                    map.get(Relation.IS_INHERITED_BY).put(to, from);
                } else if (relation == Relation.PRECEDES) {
                    map.get(Relation.IS_PRECEDED_BY).put(to, from);
                } else if (relation == Relation.PERSPECTIVE_ON) {
                    map.get(Relation.IS_PERSPECTIVIZED_IN).put(to, from);
                } else if (relation == Relation.SUBFRAME_OF) {
                    map.get(Relation.HAS_SUBFRAME).put(to, from);
                }
            }
            final ImmutableMap.Builder<Relation, Multimap<String, String>> mapBuilder = ImmutableMap
                    .builder();
            for (final Map.Entry<Relation, ImmutableMultimap.Builder<String, String>> entry : map
                    .entrySet()) {
                mapBuilder.put(entry.getKey(), entry.getValue().build());
            }
            RELATIONS = mapBuilder.build();

        } catch (final IOException ex) {
            throw new Error("Could not load eu.fbk.dkm.pikes.resources.FrameNet data from classpath", ex);
        }
    }

    public static Set<String> getRelatedFrames(final boolean recursive,
            final String sourceFrameID, final Relation... relations) {
        final Set<String> ids = Sets.newHashSet();
        final List<String> queue = Lists.newLinkedList();
        queue.add(sourceFrameID);
        while (!queue.isEmpty()) {
            final String id = queue.remove(0);
            for (final Relation relation : relations) {
                for (final String relatedID : RELATIONS.get(relation).get(id)) {
                    if (ids.add(relatedID) && recursive) {
                        queue.add(relatedID);
                    }
                }
            }
        }
        return ids;
    }

    public static void main(final String[] args) throws IOException, XMLStreamException {

        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("eu.fbk.dkm.pikes.resources.FrameNet")
                    .withHeader("Generate a TSV file with indexed eu.fbk.dkm.pikes.resources.FrameNet data")
                    .withOption("f", "frames", "the directory containing frame definitions",
                            "DIR", Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "output file", "FILE", Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final File dir = cmd.getOptionValue("f", File.class);
            final File output = cmd.getOptionValue("o", File.class);

            final Set<String> lines = Sets.newHashSet();
            for (final File file : dir.listFiles()) {
                if (!file.getName().endsWith(".xml")) {
                    continue;
                }
                LOGGER.info("Processing {}", file);
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line = null;
                    String from = null;
                    Relation relation = null;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("<frame")) {
                            final int start = line.indexOf(" name=\"") + 7;
                            final int end = line.indexOf('"', start);
                            from = line.substring(start, end).trim().replace(' ', '_');
                        } else if (line.contains("<frameRelation")) {
                            final int start = line.indexOf(" type=\"") + 7;
                            int end = line.indexOf('(', start);
                            if (end < 0) {
                                end = line.length();
                            }
                            end = Math.min(end, line.indexOf('"', start));
                            relation = Relation.valueOf(line.substring(start, end).trim()
                                    .toUpperCase().replace(' ', '_'));
                        } else if (line.contains("<relatedFrame")) {
                            final int start = line.indexOf(">") + 1;
                            final int end = line.indexOf('<', start);
                            final String to = line.substring(start, end).trim().replace(' ', '_');
                            if (relation == Relation.IS_USED_BY) {
                                lines.add(Relation.USES + "\t" + to + "\t" + from);
                            } else if (relation == Relation.IS_INHERITED_BY) {
                                lines.add(Relation.INHERITS_FROM + "\t" + to + "\t" + from);
                            } else if (relation == Relation.IS_PRECEDED_BY) {
                                lines.add(Relation.PRECEDES + "\t" + to + "\t" + from);
                            } else if (relation == Relation.IS_PERSPECTIVIZED_IN) {
                                lines.add(Relation.PERSPECTIVE_ON + "\t" + to + "\t" + from);
                            } else if (relation == Relation.HAS_SUBFRAME) {
                                lines.add(Relation.SUBFRAME_OF + "\t" + to + "\t" + from);
                            } else {
                                lines.add(relation + "\t" + from + "\t" + to);
                            }
                        }
                    }
                }
            }

            final List<String> sortedLines = Ordering.natural().immutableSortedCopy(lines);
            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(
                    new FileOutputStream(output)), Charsets.UTF_8)) {
                for (final String line : sortedLines) {
                    writer.write(line);
                    writer.write('\n');
                }
            }

            LOGGER.info("Extracted {} relations", sortedLines.size());

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

    public enum Relation {

        USES,

        IS_USED_BY,

        INHERITS_FROM,

        IS_INHERITED_BY,

        PRECEDES,

        IS_PRECEDED_BY,

        PERSPECTIVE_ON,

        IS_PERSPECTIVIZED_IN,

        SUBFRAME_OF,

        HAS_SUBFRAME,

        IS_CAUSATIVE_OF,

        IS_INCHOATIVE_OF,

        SEE_ALSO;

    }

}
