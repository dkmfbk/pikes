package eu.fbk.dkm.pikes.rdf.api;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.dkm.pikes.rdf.vocab.NIF;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.util.Statements;

public class Document implements Cloneable, Comparable<Document>, Serializable {

    public static final RDFProcessor SOURCE_EXTRACTOR = null; // TODO

    public static final RDFProcessor KEM_EXTRACTOR = null; // TODO

    public static final RDFProcessor KNOWLEDGE_EXTRACTOR = null; // TODO

    public static final RDFProcessor INFLATER = null; // TODO

    public static final RDFProcessor DEFLATER = null; // TODO

    private static final long serialVersionUID = 1L;

    private String id;

    private Model model;

    private List<Annotation> annotations;

    public Document() {
        this(null, null);
    }

    public Document(@Nullable final Model model,
            @Nullable final Iterable<? extends Annotation> annotations) {
        this.model = model != null ? model : new LinkedHashModel();
        this.annotations = annotations == null ? ImmutableList.of()
                : ImmutableList.copyOf(annotations);
    }

    public Document(final IRI iri, final String text, final Annotation... annotations) {
        this(null, Arrays.asList(annotations));
        initModel(this.model, iri, text);
    }

    public String getId() {
        return this.id;
    }

    public void setId(@Nullable final String id) {
        this.id = id;
    }

    @Nullable
    public IRI getIRI() {
        return (IRI) Iterables.getOnlyElement(this.model.filter(null, NIF.SOURCE_URL, null), null)
                .getObject();
    }

    @Nullable
    public String getText() {
        return Iterables.getOnlyElement(this.model.filter(null, NIF.SOURCE_URL, null), null)
                .getObject().stringValue();
    }

    public Model getModel() {
        return this.model;
    }

    public void setModel(@Nullable final Model graph) {
        this.model = graph != null ? graph : new LinkedHashModel();
    }

    public List<Annotation> getAnnotations() {
        return this.annotations;
    }

    public void setAnnotations(@Nullable final Iterable<? extends Annotation> annotations) {
        this.annotations = annotations == null ? ImmutableList.of()
                : ImmutableList.copyOf(annotations);
    }

    public void clear() {

        // Clear the model, preserving document IRI and text, if available
        try {
            final IRI iri = getIRI();
            final String text = getText();
            this.model.clear();
            initModel(this.model, iri, text);
        } catch (final Throwable ex) {
            this.model.clear();
        }

        // Clear NLP annotations
        this.annotations = ImmutableList.of();
    }

    @Override
    public Document clone() {

        // Clone this object (exception not expected)
        final Document document;
        try {
            document = (Document) super.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new Error(ex);
        }

        // Clone RDF model
        document.model = new LinkedHashModel(document.model);

        // Clone all NLP annotations that are Cloneable
        final List<Annotation> newAnnotations = Lists.newArrayList();
        for (Annotation annotation : this.annotations) {
            if (annotation instanceof Cloneable) {
                try {
                    annotation = (Annotation) annotation.getClass().getMethod("clone")
                            .invoke(annotation);
                } catch (final NoSuchMethodException | IllegalAccessException ex) {
                    throw new Error("Cannot clone Cloneable object of type "
                            + annotation.getClass().getName(), ex);
                } catch (final InvocationTargetException ex) {
                    Throwables.throwIfUnchecked(ex);
                    throw new RuntimeException(ex);
                }
            }
            newAnnotations.add(annotation);
        }
        document.annotations = ImmutableList.copyOf(newAnnotations);

        // Return resulting clone object
        return document;
    }

    @Override
    public int compareTo(final Document other) {
        final IRI thisIRI = getIRI();
        final IRI otherIRI = other.getIRI();
        if (thisIRI == null) {
            return otherIRI == null ? 0 : 1;
        } else {
            return otherIRI == null ? -1 : thisIRI.stringValue().compareTo(otherIRI.stringValue());
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Document)) {
            return false;
        }
        final Document other = (Document) object;
        return getIRI().equals(other.getIRI());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIRI());
    }

    @Override
    public String toString() {
        try {
            return getIRI().stringValue();
        } catch (final Throwable ex) {
            return "<invalid document>";
        }
    }

    private static void initModel(final Model graph, final IRI iri, @Nullable final String text) {
        final IRI ctxURI = Statements.VALUE_FACTORY.createIRI(iri.stringValue() + "#ctx");
        graph.add(iri, RDF.TYPE, KS.RESOURCE);
        graph.add(ctxURI, NIF.SOURCE_URL, iri);
        graph.add(ctxURI, RDF.TYPE, NIF.CONTEXT);
        if (text != null) {
            graph.add(ctxURI, NIF.IS_STRING, Statements.VALUE_FACTORY.createLiteral(text));
        }
    }

    public void write(final Location location, final boolean replace) {
        // TODO
    }

    public static Document read(final Location location, final boolean lazy) {
        // TODO
        return null;
    }

    public static final class Location implements Serializable, Comparable<Location> {

        private static final long serialVersionUID = 1L;

        private final URL url;

        private final Set<String> extensions;

        @Nullable
        private transient Path path;

        @Nullable
        private transient Boolean hasPath;

        private Location(final URL url, final Iterable<String> extensions,
                @Nullable final Path path) {
            this.url = Objects.requireNonNull(url);
            this.path = path;
            this.extensions = ImmutableSet.copyOf(extensions);
            Preconditions.checkNotNull(extensions);
        }

        public URL getBaseUrl() {
            return this.url;
        }

        @Nullable
        public Path getBasePath() {
            if (this.hasPath == null) {
                try {
                    this.path = new File(this.url.toURI()).toPath();
                    this.hasPath = Boolean.TRUE;
                } catch (final Throwable ex) {
                    this.hasPath = Boolean.FALSE;
                }
            }
            return this.path;
        }

        public Set<String> getExtensions() {
            return this.extensions;
        }

        @Override
        public int compareTo(final Location other) {
            return this.url.toString().compareTo(other.url.toString());
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof Location)) {
                return false;
            }
            final Location other = (Location) object;
            return this.url.equals(other.url);
        }

        @Override
        public int hashCode() {
            return this.url.hashCode();
        }

        @Override
        public String toString() {
            return this.url.toString() + " " + this.extensions;
        }

        public static Location create(final URL baseUrl, final Iterable<String> extensions) {
            return new Location(baseUrl, extensions, null);
        }

        public static Location create(final Path basePath, final Iterable<String> extensions) {
            try {
                return new Location(basePath.toUri().toURL(), extensions, basePath);
            } catch (final MalformedURLException ex) {
                throw new IllegalArgumentException(ex);
            }
        }

        public static Location find(final Path path) {
            try {
                final String base = split(path)[0];
                final List<String> extensions = Lists.newArrayList();
                Files.list(path.getParent()).forEach(p -> {
                    final String[] parts = split(p);
                    if (parts[0].equals(base)) {
                        extensions.add(parts[1]);
                    }
                });
                return create(path, extensions);
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        public static Stream<Location> findAll(final Path folder, final boolean recursive) {
            try {
                Stream<Location> s = null;
                final Multimap<String, String> files = HashMultimap.create();
                for (final Path path : Files.list(folder).collect(Collectors.toList())) {
                    if (!Files.isDirectory(path)) {
                        final String[] parts = split(path);
                        files.put(parts[0], parts[1]);
                    } else if (recursive) {
                        final int c = Spliterator.DISTINCT | Spliterator.IMMUTABLE
                                | Spliterator.NONNULL | Spliterator.ORDERED;
                        final Stream<Location> childStream = StreamSupport
                                .stream(() -> findAll(path, recursive).spliterator(), c, false);
                        s = s == null ? childStream : Stream.concat(s, childStream);
                    }
                }
                return Stream.concat(files.asMap().entrySet().stream()
                        .map(e -> create(Paths.get(e.getKey()), e.getValue())), s);
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        private static String[] split(final Path path) {

            final String s = path.toString();

            final int i = s.lastIndexOf('.');
            if (i < 0) {
                return new String[] { s, "" };
            }

            final String ext = s.substring(i + 1);
            if (ext.equals("gz") || ext.equals("bzip2") || ext.equals("xz")) {
                final int i2 = s.lastIndexOf('.', i - 1);
                if (i2 > 0) {
                    return new String[] { s.substring(0, i2), s.substring(i2 + 1) };
                }
            }

            return new String[] { s.substring(0, i), s.substring(i + 1) };
        }

    }

}
