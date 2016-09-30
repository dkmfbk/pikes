package eu.fbk.dkm.pikes.resources;

import java.io.File;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import eu.fbk.rdfpro.AbstractRDFHandler;
import eu.fbk.rdfpro.RDFSource;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.tql.TQL;
import eu.fbk.rdfpro.util.IO;

public final class YagoTaxonomy {

    public static final String NAMESPACE = "http://dbpedia.org/class/yago/";

    private static final Map<String, Concept> ID_INDEX;

    private static final Map<Integer, Concept> OFFSET_INDEX;

    private static final Logger LOGGER = LoggerFactory.getLogger(YagoTaxonomy.class);

    static {
        try {
            final List<String> ids = Lists.newArrayList();
            final Map<Integer, String> offsetMap = Maps.newHashMap();
            final Multimap<Integer, Integer> parentsMap = HashMultimap.create();
            final Multimap<Integer, Integer> childrenMap = HashMultimap.create();
            for (final String line : Resources.readLines(
                    YagoTaxonomy.class.getResource("YagoTaxonomy.tsv"), Charsets.UTF_8)) {
                final String[] tokens = line.split("\t");
                if (tokens.length > 0) {
                    final int num = ids.size();
                    final String id = tokens[0];
                    ids.add(id);
                    final int len = id.length();
                    if (len > 9) {
                        try {
                            final int offset = Integer.parseInt(id.substring(len - 8));
                            offsetMap.put(offset, id);
                        } catch (final NumberFormatException ex) {
                            // Ignore
                        }
                    }
                    for (int i = 1; i < tokens.length; ++i) {
                        final int parentNum = Integer.parseInt(tokens[i]);
                        parentsMap.put(num, parentNum);
                        childrenMap.put(parentNum, num);
                    }
                }
            }

            final String[] emptyIDs = new String[0];
            final ImmutableMap.Builder<String, Concept> idIndexBuilder = ImmutableMap.builder();
            for (int num = 0; num < ids.size(); ++num) {
                final String id = ids.get(num);
                final Collection<Integer> parentNums = parentsMap.get(num);
                final Collection<Integer> childrenNums = childrenMap.get(num);
                final int numParents = parentNums.size();
                final int numChildren = childrenNums.size();
                final String[] parentIDs = numParents == 0 ? emptyIDs : new String[numParents];
                final String[] childrenIDs = numChildren == 0 ? emptyIDs : new String[numChildren];
                int index = 0;
                for (final Integer parentNum : parentNums) {
                    parentIDs[index++] = ids.get(parentNum);
                }
                index = 0;
                for (final Integer childrenNum : childrenNums) {
                    childrenIDs[index++] = ids.get(childrenNum);
                }
                final Concept concept = new Concept(id, parentIDs, childrenIDs);
                idIndexBuilder.put(id, concept);
            }
            ID_INDEX = idIndexBuilder.build();

            final ImmutableMap.Builder<Integer, Concept> offsetIndexBuilder = ImmutableMap
                    .builder();
            for (final Map.Entry<Integer, String> entry : offsetMap.entrySet()) {
                offsetIndexBuilder.put(entry.getKey(), ID_INDEX.get(entry.getValue()));
            }
            OFFSET_INDEX = offsetIndexBuilder.build();

        } catch (final Exception ex) {
            throw new Error(ex);
        }
    }

    @Nullable
    public static URI getDBpediaYagoURI(@Nullable final String synsetID) {
        if (synsetID != null) {
            final Integer offset = Integer.valueOf(synsetID.substring(0, synsetID.length() - 2));
            final Concept concept = OFFSET_INDEX.get(offset);
            if (concept != null) {
                return ValueFactoryImpl.getInstance().createURI(NAMESPACE + concept.id);
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
        if (dbpediaYagoURI != null && dbpediaYagoURI.stringValue().startsWith(NAMESPACE)) {
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

    public static Set<URI> getSubClasses(final URI parentURI, final boolean recursive) {
        final Set<URI> result = Sets.newHashSet();
        final List<URI> queue = Lists.newLinkedList();
        queue.add(parentURI);
        while (!queue.isEmpty()) {
            final URI uri = queue.remove(0);
            final String id = uri.stringValue().substring(NAMESPACE.length());
            final Concept concept = ID_INDEX.get(id);
            if (concept != null) {
                for (final String childID : concept.children) {
                    final URI childURI = ValueFactoryImpl.getInstance().createURI(
                            NAMESPACE + childID);
                    if (result.add(childURI) && recursive) {
                        queue.add(childURI);
                    }
                }
            }
        }
        return result;
    }

    public static Set<URI> getSuperClasses(final URI childURI, final boolean recursive) {
        final Set<URI> result = Sets.newHashSet();
        final List<URI> queue = Lists.newLinkedList();
        queue.add(childURI);
        while (!queue.isEmpty()) {
            final URI uri = queue.remove(0);
            final String id = uri.stringValue().substring(NAMESPACE.length());
            final Concept concept = ID_INDEX.get(id);
            if (concept != null) {
                for (final String parentID : concept.parents) {
                    final URI parentURI = ValueFactoryImpl.getInstance().createURI(
                            NAMESPACE + parentID);
                    if (result.add(parentURI) && recursive) {
                        queue.add(parentURI);
                    }
                }
            }
        }
        return result;
    }

    public static boolean isSubClassOf(final URI childURI, final URI parentURI) {
        if (childURI.equals(parentURI)) {
            return true;
        }
        final String childID = childURI.stringValue().substring(NAMESPACE.length());
        final Concept child = ID_INDEX.get(childID);
        if (child == null) {
            return false;
        }
        for (final String parentID : child.parents) {
            final URI uri = ValueFactoryImpl.getInstance().createURI(NAMESPACE + parentID);
            if (isSubClassOf(uri, parentURI)) {
                return true;
            }
        }
        return false;
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

            final Set<String> ids = Sets.newHashSet();
            final Multimap<String, String> parents = HashMultimap.create();
            final RDFSource source = RDFSources.read(false, true, null, null,
                    input.getAbsolutePath());
            source.emit(new AbstractRDFHandler() {

                @Override
                public void handleStatement(final Statement stmt) throws RDFHandlerException {
                    final Resource s = stmt.getSubject();
                    final URI p = stmt.getPredicate();
                    final Value o = stmt.getObject();
                    if (p.equals(RDFS.SUBCLASSOF) && s instanceof URI && o instanceof URI
                            && s.stringValue().startsWith(NAMESPACE)
                            && o.stringValue().startsWith(NAMESPACE)) {
                        final String childID = s.stringValue().substring(NAMESPACE.length());
                        final String parentID = o.stringValue().substring(NAMESPACE.length());
                        if (getSynsetID((URI) o) != null) {
                            ids.add(parentID);
                        }
                        if (getSynsetID((URI) s) != null) {
                            ids.add(childID);
                            parents.put(childID, parentID);
                        }
                    }
                }

            }, 1);

            final List<String> sortedIDs = Ordering.natural().immutableSortedCopy(ids);

            int counter = 0;
            final Map<String, Integer> nums = Maps.newHashMap();
            for (final String id : sortedIDs) {
                nums.put(id, counter++);
            }

            try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(output.getAbsolutePath())))) {
                for (int childNum = 0; childNum < sortedIDs.size(); ++childNum) {
                    final String childID = sortedIDs.get(childNum);
                    writer.write(childID);
                    for (final String parentID : parents.get(childID)) {
                        final Integer parentNum = nums.get(parentID);
                        if (parentNum != null) {
                            writer.write("\t");
                            writer.write(Integer.toString(parentNum));
                        }
                    }
                    writer.write("\n");
                }
            }

            LOGGER.info("Emitted {} mappings", sortedIDs.size());

        } catch (final Throwable ex) {
            CommandLine.fail(ex);
        }
    }

    private static final class Concept {

        public final String id;

        public final String[] parents;

        public final String[] children;

        Concept(final String id, final String[] parents, final String[] children) {
            this.id = id;
            this.parents = parents;
            this.children = children;
        }

    }

}
