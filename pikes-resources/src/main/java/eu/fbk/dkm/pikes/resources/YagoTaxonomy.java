package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Charsets;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.CommandLine.Type;
import eu.fbk.rdfpro.AbstractRDFHandler;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.tql.TQL;
import eu.fbk.rdfpro.util.IO;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class YagoTaxonomy {

    public static final String NAMESPACE_DBPEDIA_YAGO = "http://dbpedia.org/class/yago/";

    private static final Map<Long, String> OFFSET_TO_LEMMA;

    private static final Logger LOGGER = LoggerFactory.getLogger(YagoTaxonomy.class);

    static {
        try {
            final ImmutableMap.Builder<Long, String> builder = ImmutableMap.builder();
            for (final String line : Resources.readLines(
                    YagoTaxonomy.class.getResource("eu.fbk.dkm.pikes.resources.YagoTaxonomy.tsv"), Charsets.UTF_8)) {
                final String[] tokens = line.split("\t");
                builder.put(Long.valueOf(tokens[0]), tokens[1]);
            }
            OFFSET_TO_LEMMA = builder.build();
        } catch (final Exception ex) {
            throw new Error(ex);
        }
    }

    @Nullable
    public static URI getDBpediaYagoURI(@Nullable final String synsetID) {
        if (synsetID != null) {
            final Long offset = Long.valueOf(synsetID.substring(0, synsetID.length() - 2));
            final String lemma = OFFSET_TO_LEMMA.get(offset);
            if (lemma != null) {
                return ValueFactoryImpl.getInstance().createURI(
                        String.format("%s%s1%08d", NAMESPACE_DBPEDIA_YAGO, lemma, offset));
            }
        }
        return null;
    }

    public static Set<URI> getDBpediaYagoURIs(@Nullable final Iterable<String> synsetIDs) {
        final Set<URI> uris = Sets.newHashSet();
        final Set<String> hypernyms = Sets.newHashSet();
        final List<String> queue = Lists.newLinkedList();
        if (synsetIDs != null) {
            Iterables.addAll(queue, synsetIDs);
        }
        while (!queue.isEmpty()) {
            final String synsetID = queue.remove(0);
            final URI uri = getDBpediaYagoURI(synsetID);
            if (uri != null) {
                uris.add(uri);
            } else {
                for (final String hypernym : WordNet.getHypernyms(synsetID)) {
                    if (hypernyms.add(hypernym)) {
                        queue.add(hypernym);
                    }
                }
            }
        }
        return uris;
    }

    @Nullable
    public static String getSynsetID(@Nullable final URI dbpediaYagoURI) {
        if (dbpediaYagoURI != null
                && dbpediaYagoURI.stringValue().startsWith(NAMESPACE_DBPEDIA_YAGO)) {
            final String s = dbpediaYagoURI.stringValue();
            final int l = s.length();
            if (l > 9) {
                for (int i = l - 9; i < l; ++i) {
                    if (!Character.isDigit(s.charAt(i))) {
                        return null;
                    }
                }
                return s.substring(l - 8) + "-n";
            }
        }
        return null;
    }

    public static void main(final String... args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("eu.fbk.dkm.pikes.resources.YagoTaxonomy")
                    .withHeader(
                            "Generate a TSV file with mappings from offsets to DBpedia Yago URIs")
                    .withOption("i", "input", "the input RDF file with the DBpedia Yago taxonomy",
                            "FILE", Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "the output TSV file", "FILE", Type.FILE, true,
                            false, true).withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final File input = cmd.getOptionValue("i", File.class);
            final File output = cmd.getOptionValue("o", File.class);

            TQL.register();

            final Set<URI> uris = Sets.newHashSet();
            final RDFSource source = RDFSources.read(false, true, null, null,
                    input.getAbsolutePath());
            source.emit(new AbstractRDFHandler() {

                @Override
                public void handleStatement(final Statement statement) throws RDFHandlerException {
                    if (statement.getSubject() instanceof URI) {
                        process((URI) statement.getSubject());
                    }
                    if (statement.getObject() instanceof URI) {
                        process((URI) statement.getObject());
                    }
                }

                private void process(final URI uri) {
                    if (getSynsetID(uri) == null) {
                        return;
                    }
                    uris.add(uri);
                }

            }, 1);

            final Map<Long, String> map = Maps.newHashMap();
            for (final URI uri : uris) {
                final String synsetID = getSynsetID(uri);
                final Long offset = Long.valueOf(synsetID.substring(0, 8));
                final String name = uri.stringValue().substring(NAMESPACE_DBPEDIA_YAGO.length());
                final String lemma = name.substring(0, name.length() - 9);
                map.put(offset, lemma);
            }

            try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(output.getAbsolutePath())))) {
                for (final Long offset : Ordering.natural().immutableSortedCopy(map.keySet())) {
                    writer.write(offset + "\t" + map.get(offset) + "\n");
                }
            }

            LOGGER.info("Emitted {} mappings", map.size());

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

}
