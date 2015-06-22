package eu.fbk.dkm.pikes.naflib;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import eu.fbk.dkm.utils.Util;
import eu.fbk.rdfpro.util.IO;
import ixa.kaflib.KAFDocument;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Corpus implements Iterable<KAFDocument>, Serializable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Corpus.class);

    private static final long serialVersionUID = 1L;

    private static final Corpus EMPTY = new Corpus(new Path[0], null);

    private final Path[] files;

    @Nullable
    private final BiConsumer<Path, KAFDocument> transformer;

    @Nullable
    private transient Set<Path> fileSet;

    @Nullable
    private transient Path path;

    public static Corpus create(final boolean recursive, final Object... filesOrDirs) {
        return create(recursive, Arrays.asList(filesOrDirs));
    }

    public static Corpus create(final boolean recursive, final Iterable<?> filesOrDirs) {

        final List<Path> paths = Lists.newArrayList();
        for (final Object fileOrDir : filesOrDirs) {
            if (fileOrDir instanceof Path) {
                paths.add((Path) fileOrDir);
            } else if (fileOrDir instanceof File) {
                paths.add(((File) fileOrDir).toPath());
            } else {
                paths.add(Paths.get(fileOrDir.toString()));
            }
        }

        final List<Path> files = Util.fileMatch(paths, ImmutableList.of(".naf", ".naf.gz",
                ".naf.bz2", ".naf.xz", ".xml", ".xml.gz", ".xml.bz2", ".xml.xz"), recursive);

        if (files.isEmpty()) {
            return EMPTY;
        } else {
            return new Corpus(files.toArray(new Path[files.size()]), null);
        }
    }

    private Corpus(final Path[] files, @Nullable final BiConsumer<Path, KAFDocument> transformer) {
        this.files = files;
        this.transformer = transformer;
    }

    public Path path() {
        if (this.path == null) {
            String prefix = this.files[0].toString();
            for (final Path file : this.files) {
                prefix = Strings.commonPrefix(prefix, file.toString());
            }
            this.path = Paths.get(prefix);
        }
        return this.path;
    }

    public int size() {
        return this.files.length;
    }

    public boolean isEmpty() {
        return this.files.length == 0;
    }

    public KAFDocument get(final Object key) {
        try {
            int index;
            if (key instanceof Number) {
                index = ((Number) key).intValue();
            } else if (key instanceof File) {
                index = Arrays.binarySearch(this.files, ((File) key).toPath());
            } else if (key instanceof Path) {
                index = Arrays.binarySearch(this.files, key);
            } else {
                index = Arrays.binarySearch(this.files, Paths.get(key.toString()));
            }
            if (index < 0 || index >= this.files.length) {
                throw new IllegalArgumentException("No file in this corpus for " + key);
            }
            final Path file = this.files[index];
            KAFDocument document = null;
            try (Reader reader = IO.utf8Reader(IO.buffer(IO.read(file.toString())))) {
                document = KAFDocument.createFromStream(reader);
            } catch (final Throwable ex) {
                LOGGER.warn("Failed to parse document " + file, ex);
                return null;
            }
            final String relativePath = file.toString().substring(path().toString().length());
            document.getPublic().publicId = relativePath;
            if ("http://www.example.com".equals(document.getPublic().uri)) {
                document.getPublic().uri = "doc:" + relativePath;
            }
            if (this.transformer != null) {
                this.transformer.accept(file, document);
            }
            return document;

        } catch (final Throwable ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public Iterator<KAFDocument> iterator() {
        return new UnmodifiableIterator<KAFDocument>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return this.index < Corpus.this.files.length;
            }

            @Override
            public KAFDocument next() {
                return get(this.index++);
            }

        };
    }

    @Override
    public Spliterator<KAFDocument> spliterator() {
        return spliteratorHelper(Arrays.spliterator(this.files));
    }

    private Spliterator<KAFDocument> spliteratorHelper(final Spliterator<Path> delegate) {
        return new Spliterator<KAFDocument>() {

            @Override
            public boolean tryAdvance(final Consumer<? super KAFDocument> action) {
                return delegate.tryAdvance(file -> {
                    action.accept(get(file));
                });
            }

            @Override
            public Spliterator<KAFDocument> trySplit() {
                final Spliterator<Path> splittedDelegate = delegate.trySplit();
                return splittedDelegate == null ? null : spliteratorHelper(splittedDelegate);
            }

            @Override
            public long estimateSize() {
                return delegate.estimateSize();
            }

            @Override
            public int characteristics() {
                return Spliterator.IMMUTABLE | Spliterator.DISTINCT | Spliterator.NONNULL
                        | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
            }

        };
    }

    public Stream<KAFDocument> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<KAFDocument> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    public Set<Path> files() {
        if (this.fileSet == null) {
            this.fileSet = new AbstractSet<Path>() {

                @Override
                public int size() {
                    return Corpus.this.files.length;
                }

                @Override
                public boolean contains(final Object object) {
                    return object instanceof File
                            && Arrays.binarySearch(Corpus.this.files, object) >= 0;
                }

                @Override
                public Iterator<Path> iterator() {
                    return Iterators.forArray(Corpus.this.files);
                }

                @Override
                public Spliterator<Path> spliterator() {
                    return Arrays.spliterator(Corpus.this.files);
                }

            };
        }
        return this.fileSet;
    }

    public Corpus transform(final Consumer<KAFDocument> transformer) {
        return transform((final Path file, final KAFDocument document) -> {
            transformer.accept(document);
        });
    }

    public Corpus transform(final BiConsumer<Path, KAFDocument> transformer) {
        return new Corpus(this.files, this.transformer == null ? transformer
                : this.transformer.andThen(transformer));
    }

    public Corpus fixURIs() {
        return transform((final Path file, final KAFDocument document) -> {
            final String relativePath = file.toString().substring(path().toString().length());
            document.getPublic().uri = "doc:" + relativePath;
            document.getPublic().publicId = relativePath;
        });
    }

    public Corpus[] split(@Nullable final Long shuffleSeed, final float... percentages) {

        // Shuffle the files if necessary, using the supplied seed
        Path[] files = this.files;
        if (shuffleSeed != null) {
            final List<Path> list = Lists.newArrayList(files);
            final Random random = new Random(shuffleSeed);
            Collections.shuffle(list, random);
            files = list.toArray(new Path[list.size()]);
        }

        // Split the (shuffled) file array based on supplied percentages
        final Corpus[] corpora = new Corpus[percentages.length];
        int index = 0;
        float cumulated = 0.0f;
        for (int i = 0; i < percentages.length; ++i) {
            cumulated += percentages[i];
            if (cumulated > 1.0f) {
                throw new IllegalArgumentException("Invalid percentages (sum must be 1.0f): "
                        + Arrays.toString(percentages));
            }
            final int endIndex = (int) Math.ceil(files.length * cumulated);
            final Path[] partition = Arrays.copyOfRange(files, index, endIndex);
            if (shuffleSeed != null) {
                Arrays.sort(partition);
            }
            corpora[i] = new Corpus(partition, this.transformer);
            index = endIndex;
        }
        return corpora;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Corpus)) {
            return false;
        }
        final Corpus other = (Corpus) object;
        return Arrays.equals(this.files, other.files)
                && Objects.equal(this.transformer, other.transformer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Arrays.hashCode(this.files), this.transformer);
    }

    @Override
    public String toString() {
        if (this.files.length == 0) {
            return "Empty corpus";
        } else {
            return this.files.length + " document(s) corpus (path: " + path() + ")";
        }
    }

}
