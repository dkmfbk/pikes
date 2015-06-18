package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Charsets;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import eu.fbk.dkm.utils.vocab.SUMO;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Sumo {

    public static final String NAMESPACE = "http://www.ontologyportal.org/SUMO.owl#";

    private static final Map<URI, Concept> URI_INDEX;

    private static final Map<String, Concept> SYNSET_INDEX;

    static {
        try (final BufferedReader reader = Resources.asCharSource(
                NomBank.class.getResource("eu.fbk.dkm.pikes.resources.Sumo.tsv"), Charsets.UTF_8).openBufferedStream()) {

            final Map<String, URI> uriIndex = Maps.newHashMap();
            final Map<URI, Concept> nameIndex = Maps.newHashMap();
            final Map<String, Concept> synsetIndex = Maps.newHashMap();

            String line;
            final String[] empty = new String[0];
            while ((line = reader.readLine()) != null) {

                final String[] tokens = Arrays.copyOf(line.split("\t"), 4);

                final String name = tokens[0].intern();
                final String[] parents = tokens[1] == null ? empty : tokens[1].split("\\|");
                final String[] children = tokens[2] == null ? empty : tokens[2].split("\\|");
                final String[] synsets = tokens[3] == null ? empty : tokens[3].split("\\|");

                for (final String[] array : new String[][] { parents, children, synsets }) {
                    for (int i = 0; i < array.length; ++i) {
                        array[i] = array[i].intern();
                    }
                }

                final URI[][] uriArrays = new URI[3][];
                final String[][] stringArrays = new String[][] { new String[] { name }, parents,
                        children };

                for (int i = 0; i < 3; ++i) {
                    final String[] stringArray = stringArrays[i];
                    final URI[] uriArray = new URI[stringArray.length];
                    uriArrays[i] = uriArray;
                    for (int j = 0; j < stringArray.length; ++j) {
                        final String uriString = SUMO.NAMESPACE + stringArray[j].trim();
                        URI uri = uriIndex.get(uriString);
                        if (uri == null) {
                            uri = new URIImpl(uriString);
                            uriIndex.put(uriString, uri);
                        }
                        uriArray[j] = uri;
                    }
                }

                final URI conceptURI = uriArrays[0][0];

                for (int i = 0; i < synsets.length; ++i) {
                    synsets[i] = synsets[i].trim().intern();
                }

                final Concept concept = new Concept(conceptURI, uriArrays[1], uriArrays[2],
                        synsets);

                nameIndex.put(conceptURI, concept);
                for (final String synset : synsets) {
                    synsetIndex.put(synset, concept);
                }
            }

            URI_INDEX = ImmutableMap.copyOf(nameIndex);
            SYNSET_INDEX = ImmutableMap.copyOf(synsetIndex);

        } catch (final IOException ex) {
            throw new Error("Cannot load eu.fbk.dkm.pikes.resources.PropBank data", ex);
        }
    }

    @Nullable
    public static URI synsetToConcept(@Nullable final String synsetID) {
        if (synsetID == null) {
            return null;
        }
        final Concept concept = SYNSET_INDEX.get(synsetID.toLowerCase());
        return concept == null ? null : concept.uri;
    }

    public static Set<URI> synsetsToConcepts(@Nullable final Iterable<String> synsetIDs) {
        final Set<URI> conceptURIs = Sets.newHashSet();
        for (final String synsetID : synsetIDs) {
            final URI conceptURI = Sumo.synsetToConcept(synsetID);
            if (conceptURI != null) {
                conceptURIs.add(conceptURI);
            }
        }
        return filterAncestors(conceptURIs);
    }

    public static Set<String> conceptToSynsets(@Nullable final URI conceptURI) {
        if (conceptURI == null) {
            return null;
        }
        final Concept concept = URI_INDEX.get(conceptURI);
        return concept == null ? ImmutableSet.of() : ImmutableSet.copyOf(concept.synsets);
    }

    public static Set<URI> filterAncestors(@Nullable final Iterable<? extends URI> conceptURIs) {
        final Set<URI> result = Sets.newHashSet(conceptURIs);
        outer: for (final URI uri1 : conceptURIs) {
            for (final URI uri2 : conceptURIs) {
                if (!uri1.equals(uri2) && isSubClassOf(uri1, uri2)) {
                    continue outer;
                }
            }
            result.add(uri1);
        }
        return result;
    }

    public static Set<URI> getSubClasses(final URI parentURI) {
        final Set<URI> result = Sets.newHashSet();
        final List<URI> queue = Lists.newLinkedList();
        queue.add(parentURI);
        while (!queue.isEmpty()) {
            final Concept concept = URI_INDEX.get(queue.remove(0));
            if (concept != null) {
                for (final URI uri : concept.children) {
                    if (result.add(uri)) {
                        queue.add(uri);
                    }
                }
            }
        }
        return result;
    }

    public static Set<URI> getSuperClasses(URI childURI) {
        final Set<URI> result = Sets.newHashSet();
        final List<URI> queue = Lists.newLinkedList();
        queue.add(childURI);
        while (!queue.isEmpty()) {
            final Concept concept = URI_INDEX.get(queue.remove(0));
            if (concept != null) {
                for (final URI uri : concept.parents) {
                    if (result.add(uri)) {
                        queue.add(uri);
                    }
                }
            }
        }
        return result;
    }

    public static boolean isSubClassOf(final URI childURI, final URI parentURI) {
        final Concept child = URI_INDEX.get(childURI);
        if (child == null) {
            return false;
        }
        if (childURI.equals(parentURI)) {
            return true;
        }
        for (final URI uri : child.parents) {
            if (isSubClassOf(uri, parentURI)) {
                return true;
            }
        }
        return false;
    }

    private static final class Concept {

        public final URI uri;

        public final URI[] parents;

        public final URI[] children;

        public final String[] synsets;

        Concept(final URI uri, final URI[] parents, final URI[] children, final String[] synsets) {
            this.uri = uri;
            this.parents = parents;
            this.children = children;
            this.synsets = synsets;
        }

    }

}
