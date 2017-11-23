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

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandlerException;
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
    public static IRI getDBpediaYagoIRI(@Nullable final String synsetID) {
        if (synsetID != null) {
            final Integer offset = Integer.valueOf(synsetID.substring(0, synsetID.length() - 2));
            final Concept concept = OFFSET_INDEX.get(offset);
            if (concept != null) {
                return SimpleValueFactory.getInstance() .createIRI(NAMESPACE + concept.id);
            }
        }
        return null;
    }

    public static Set<IRI> getDBpediaYagoIRIs(@Nullable final Iterable<String> synsetIDs) {
        final Set<IRI> uris = Sets.newHashSet();
        final Set<String> hypernyms = Sets.newHashSet();
        final List<String> queue = Lists.newLinkedList();
        if (synsetIDs != null) {
            Iterables.addAll(queue, synsetIDs);
        }
        while (!queue.isEmpty()) {
            final String synsetID = queue.remove(0);
            final IRI uri = getDBpediaYagoIRI(synsetID);
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
    public static String getSynsetID(@Nullable final IRI dbpediaYagoIRI) {
        if (dbpediaYagoIRI != null && dbpediaYagoIRI.stringValue().startsWith(NAMESPACE)) {
            final String s = dbpediaYagoIRI.stringValue();
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

    public static Set<IRI> getSubClasses(final IRI parentIRI, final boolean recursive) {
        final Set<IRI> result = Sets.newHashSet();
        final List<IRI> queue = Lists.newLinkedList();
        queue.add(parentIRI);
        while (!queue.isEmpty()) {
            final IRI uri = queue.remove(0);
            final String id = uri.stringValue().substring(NAMESPACE.length());
            final Concept concept = ID_INDEX.get(id);
            if (concept != null) {
                for (final String childID : concept.children) {
                    final IRI childIRI = SimpleValueFactory.getInstance().createIRI(
                            NAMESPACE + childID);
                    if (result.add(childIRI) && recursive) {
                        queue.add(childIRI);
                    }
                }
            }
        }
        return result;
    }

    public static Set<IRI> getSuperClasses(final IRI childIRI, final boolean recursive) {
        final Set<IRI> result = Sets.newHashSet();
        final List<IRI> queue = Lists.newLinkedList();
        queue.add(childIRI);
        while (!queue.isEmpty()) {
            final IRI uri = queue.remove(0);
            final String id = uri.stringValue().substring(NAMESPACE.length());
            final Concept concept = ID_INDEX.get(id);
            if (concept != null) {
                for (final String parentID : concept.parents) {
                    final IRI parentIRI = SimpleValueFactory.getInstance().createIRI(
                            NAMESPACE + parentID);
                    if (result.add(parentIRI) && recursive) {
                        queue.add(parentIRI);
                    }
                }
            }
        }
        return result;
    }

    public static boolean isSubClassOf(final IRI childIRI, final IRI parentIRI) {
        if (childIRI.equals(parentIRI)) {
            return true;
        }
        final String childID = childIRI.stringValue().substring(NAMESPACE.length());
        final Concept child = ID_INDEX.get(childID);
        if (child == null) {
            return false;
        }
        for (final String parentID : child.parents) {
            final IRI uri = SimpleValueFactory.getInstance().createIRI(NAMESPACE + parentID);
            if (isSubClassOf(uri, parentIRI)) {
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
                            "Generate a TSV file with mappings from offsets to DBpedia Yago IRIs")
                    .withOption("i", "input", "the input RDF file with the DBpedia Yago taxonomy",
                            "FILE", Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "the output TSV file", "FILE", Type.FILE, true,
                            false, true).withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            final File input = cmd.getOptionValue("i", File.class);
            final File output = cmd.getOptionValue("o", File.class);

            final Set<String> ids = Sets.newHashSet();
            final Multimap<String, String> parents = HashMultimap.create();
            final RDFSource source = RDFSources.read(false, true, null, null, null, true,
                    input.getAbsolutePath());
            source.emit(new AbstractRDFHandler() {

                @Override
                public void handleStatement(final Statement stmt) throws RDFHandlerException {
                    final Resource s = stmt.getSubject();
                    final IRI p = stmt.getPredicate();
                    final Value o = stmt.getObject();
                    if (p.equals(RDFS.SUBCLASSOF) && s instanceof IRI && o instanceof IRI
                            && s.stringValue().startsWith(NAMESPACE)
                            && o.stringValue().startsWith(NAMESPACE)) {
                        final String childID = s.stringValue().substring(NAMESPACE.length());
                        final String parentID = o.stringValue().substring(NAMESPACE.length());
                        if (getSynsetID((IRI) o) != null) {
                            ids.add(parentID);
                        }
                        if (getSynsetID((IRI) s) != null) {
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
