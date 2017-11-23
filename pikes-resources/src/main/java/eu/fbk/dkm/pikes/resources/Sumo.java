package eu.fbk.dkm.pikes.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import eu.fbk.rdfpro.util.Statements;
import org.eclipse.rdf4j.model.IRI;

public final class Sumo {

    public static final String SUMO_NAMESPACE = "http://www.ontologyportal.org/SUMO.owl#";

    private static final Map<IRI, Concept> IRI_INDEX;

    private static final Map<String, Concept> SYNSET_INDEX;

    static {
        try (final BufferedReader reader = Resources.asCharSource(
                NomBank.class.getResource("Sumo.tsv"), Charsets.UTF_8).openBufferedStream()) {

            final Map<String, IRI> uriIndex = Maps.newHashMap();
            final Map<IRI, Concept> nameIndex = Maps.newHashMap();
            final Map<String, Concept> synsetIndex = Maps.newHashMap();

            String line;
            while ((line = reader.readLine()) != null) {

                final String[] tokens = Arrays.copyOf(line.split("\t"), 4);

                final String name = tokens[0].intern();
                final List<String> parents = tokens[1] == null ? ImmutableList.of() : Splitter
                        .on('|').trimResults().omitEmptyStrings().splitToList(tokens[1]);
                final List<String> children = tokens[2] == null ? ImmutableList.of() : Splitter
                        .on('|').trimResults().omitEmptyStrings().splitToList(tokens[2]);
                final List<String> synsets = tokens[3] == null ? ImmutableList.of() : Splitter
                        .on('|').trimResults().omitEmptyStrings().splitToList(tokens[3]);

                final IRI[][] uriArrays = new IRI[3][];
                final List<List<String>> stringLists = ImmutableList.of(ImmutableList.of(name),
                        parents, children);

                for (int i = 0; i < 3; ++i) {
                    final List<String> stringList = stringLists.get(i);
                    final IRI[] uriArray = new IRI[stringList.size()];
                    uriArrays[i] = uriArray;
                    for (int j = 0; j < stringList.size(); ++j) {
                        final String uriString = (SUMO_NAMESPACE + stringList.get(j).trim())
                                .intern();
                        IRI uri = uriIndex.get(uriString);
                        if (uri == null) {
                            uri = Statements.VALUE_FACTORY.createIRI(uriString);
                            uriIndex.put(uriString, uri);
                        }
                        uriArray[j] = uri;
                    }
                }

                final IRI conceptIRI = uriArrays[0][0];

                final String[] synsetsArray = new String[synsets.size()];
                for (int i = 0; i < synsets.size(); ++i) {
                    synsetsArray[i] = synsets.get(i).trim().intern();
                }

                final Concept concept = new Concept(conceptIRI, uriArrays[1], uriArrays[2],
                        synsetsArray);

                nameIndex.put(conceptIRI, concept);
                for (final String synset : synsets) {
                    synsetIndex.put(synset, concept);
                }
            }

            IRI_INDEX = ImmutableMap.copyOf(nameIndex);
            SYNSET_INDEX = ImmutableMap.copyOf(synsetIndex);

        } catch (final IOException ex) {
            throw new Error("Cannot load PropBank data", ex);
        }
    }

    @Nullable
    public static IRI synsetToConcept(@Nullable final String synsetID) {
        if (synsetID == null) {
            return null;
        }
        final Concept concept = SYNSET_INDEX.get(synsetID.toLowerCase());
        return concept == null ? null : concept.uri;
    }

    public static Set<IRI> synsetsToConcepts(@Nullable final Iterable<String> synsetIDs) {
        final Set<IRI> conceptIRIs = Sets.newHashSet();
        for (final String synsetID : synsetIDs) {
            final IRI conceptIRI = Sumo.synsetToConcept(synsetID);
            if (conceptIRI != null) {
                conceptIRIs.add(conceptIRI);
            }
        }
        return filterAncestors(conceptIRIs);
    }

    public static Set<String> conceptToSynsets(@Nullable final IRI conceptIRI) {
        if (conceptIRI == null) {
            return null;
        }
        final Concept concept = IRI_INDEX.get(conceptIRI);
        return concept == null ? ImmutableSet.of() : ImmutableSet.copyOf(concept.synsets);
    }

    public static Set<IRI> filterAncestors(@Nullable final Iterable<? extends IRI> conceptIRIs) {
        final Set<IRI> result = Sets.newHashSet(conceptIRIs);
        outer: for (final IRI uri1 : conceptIRIs) {
            for (final IRI uri2 : conceptIRIs) {
                if (!uri1.equals(uri2) && isSubClassOf(uri1, uri2)) {
                    continue outer;
                }
            }
            result.add(uri1);
        }
        return result;
    }

    public static Set<IRI> getSubClasses(final IRI parentIRI) {
        final Set<IRI> result = Sets.newHashSet();
        final List<IRI> queue = Lists.newLinkedList();
        queue.add(parentIRI);
        while (!queue.isEmpty()) {
            final Concept concept = IRI_INDEX.get(queue.remove(0));
            if (concept != null) {
                for (final IRI uri : concept.children) {
                    if (result.add(uri)) {
                        queue.add(uri);
                    }
                }
            }
        }
        return result;
    }

    public static Set<IRI> getSuperClasses(final IRI childIRI) {
        final Set<IRI> result = Sets.newHashSet();
        final List<IRI> queue = Lists.newLinkedList();
        queue.add(childIRI);
        while (!queue.isEmpty()) {
            final Concept concept = IRI_INDEX.get(queue.remove(0));
            if (concept != null) {
                for (final IRI uri : concept.parents) {
                    if (result.add(uri)) {
                        queue.add(uri);
                    }
                }
            }
        }
        return result;
    }

    public static boolean isSubClassOf(final IRI childIRI, final IRI parentIRI) {
        final Concept child = IRI_INDEX.get(childIRI);
        if (child == null) {
            return false;
        }
        if (childIRI.equals(parentIRI)) {
            return true;
        }
        for (final IRI uri : child.parents) {
            if (isSubClassOf(uri, parentIRI)) {
                return true;
            }
        }
        return false;
    }

    private static final class Concept {

        public final IRI uri;

        public final IRI[] parents;

        public final IRI[] children;

        public final String[] synsets;

        Concept(final IRI uri, final IRI[] parents, final IRI[] children, final String[] synsets) {
            this.uri = uri;
            this.parents = parents;
            this.children = children;
            this.synsets = synsets;
        }

    }

}
