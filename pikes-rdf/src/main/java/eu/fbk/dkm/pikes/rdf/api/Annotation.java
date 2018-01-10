package eu.fbk.dkm.pikes.rdf.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.rdfpro.util.IO;

public abstract class Annotation implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Annotation.class);

    public static Annotation read(final Path path, @Nullable Format format) throws IOException {
        format = format != null ? format : Format.forFileName(path.toString()).get();
        try (InputStream in = Files.newInputStream(path)) {
            return read(in, format);
        }
    }

    public static Annotation read(final InputStream in, final Format format) throws IOException {
        return format.read(in);
    }

    public static Annotation read(final Reader in, final Format format) throws IOException {
        return format.read(in);
    }

    /**
     * Returns (or reconstructs, as far as possible) the document RDF graph from which this
     * annotation has been derived.
     *
     * @return a possibly empty RDF graph
     */
    public abstract Model getSource();

    public static Model getSource(final Iterable<Annotation> annotations) {
        // TODO
        return null;
    }

    /**
     * Returns the annotation format.
     *
     * @return the annotation format
     */
    public abstract Format getFormat();

    public final void write(final Path path) throws IOException {

        final Format pathFormat = Format.forFileName(path.toString()).get();
        final Format thisFormat = getFormat();

        if (!thisFormat.equals(pathFormat)) {
            throw new IllegalArgumentException(
                    "Path " + path + " not compatible with format " + thisFormat);
        }

        try (OutputStream out = IO.buffer(IO.write(path.toString()))) {
            write(out);
        }
    }

    public void write(final OutputStream out) throws IOException {
        final Charset charset = getFormat().getCharset();
        final Writer writer = charset == null || charset.equals(Charsets.UTF_8)
                ? IO.utf8Writer(out) : new OutputStreamWriter(out, charset);
        write(writer);
        writer.flush();
    }

    public abstract void write(final Writer out) throws IOException;

    @Override
    public Annotation clone() {
        final Annotation annotation;
        try {
            annotation = (Annotation) super.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new Error(ex);
        }
        return annotation;
    }

    @Override
    public String toString() {
        return getFormat().getName() + " annotation";
    }

    public static final class Format extends FileFormat {

        private static List<Format> formats = ImmutableList.of();

        private final Class<? extends Annotation> annotationClass;

        private final boolean compressible;

        @Nullable
        final Constructor<?> streamConstructor;

        @Nullable
        final Constructor<?> readerConstructor;

        Annotation read(final Object streamOrReader) throws IOException {

            Objects.requireNonNull(streamOrReader);

            if (this.readerConstructor == null && this.streamConstructor == null) {
                throw new IllegalArgumentException("No constructor available for reading " + this);
            }

            final Charset charset = getCharset() != null ? getCharset() : Charsets.UTF_8;
            Constructor<?> constructor = null;
            Object arg = streamOrReader;

            if (arg instanceof InputStream) {
                if (this.streamConstructor != null) {
                    constructor = this.streamConstructor;
                } else {
                    constructor = this.readerConstructor;
                    final InputStream is = (InputStream) arg;
                    arg = charset.equals(Charsets.UTF_8) ? IO.utf8Reader(is)
                            : new InputStreamReader(is, charset);
                }
            } else if (arg instanceof Reader) {
                if (this.readerConstructor != null) {
                    constructor = this.readerConstructor;
                } else {
                    constructor = this.streamConstructor;
                    final String str = CharStreams.toString((Reader) arg);
                    final byte[] bytes = str.getBytes(charset);
                    arg = new ByteArrayInputStream(bytes);
                }
            }

            try {
                return (Annotation) constructor.newInstance(arg);
            } catch (final InvocationTargetException ex) {
                Throwables.propagateIfPossible(ex.getCause(), IOException.class);
                throw new RuntimeException(ex.getCause());
            } catch (IllegalAccessException | InstantiationException ex) {
                throw new Error(
                        "Could not instantiate " + getAnnotationClass() + " using " + constructor,
                        ex);
            }
        }

        private Format(final String name, final List<String> mimeTypes,
                @Nullable final Charset charset, final List<String> fileExtensions,
                final Class<? extends Annotation> annotationClass, final boolean compressible) {

            super(name, mimeTypes, charset, fileExtensions);

            Constructor<?> streamConstructor = null;
            Constructor<?> readerConstructor = null;
            for (final Constructor<?> constructor : annotationClass.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 1) {
                    final Class<?> argClass = constructor.getParameters()[0].getType();
                    if (argClass.isAssignableFrom(InputStream.class)) {
                        streamConstructor = constructor;
                    } else if (argClass.isAssignableFrom(Reader.class)) {
                        readerConstructor = constructor;
                    }
                }
            }

            if (streamConstructor != null) {
                streamConstructor.setAccessible(true);
            }
            if (readerConstructor != null) {
                readerConstructor.setAccessible(true);
            }

            this.annotationClass = annotationClass;
            this.compressible = compressible;
            this.streamConstructor = streamConstructor;
            this.readerConstructor = readerConstructor;
        }

        public static Format register(final String name, final Iterable<String> mimeTypes,
                @Nullable final Charset charset, final Iterable<String> fileExtensions,
                final Class<? extends Annotation> annotationClass, final boolean compressible) {

            Objects.requireNonNull(name);
            Objects.requireNonNull(annotationClass);

            final List<String> mimeTypeList = ImmutableList.copyOf(mimeTypes);
            final List<String> fileExtensionList = ImmutableList.copyOf(fileExtensions);
            Preconditions.checkArgument(!mimeTypeList.isEmpty());
            Preconditions.checkArgument(!fileExtensionList.isEmpty());

            final Format newFormat = new Format(name, mimeTypeList, charset, fileExtensionList,
                    annotationClass, compressible);

            synchronized (Format.class) {
                // Return an existing format with exactly the same data, or replace it if it has
                // the same name of the new format
                for (final Format oldFormat : formats) {
                    if (oldFormat.equals(newFormat)) {
                        if (oldFormat.getMIMETypes().equals(mimeTypeList)
                                && Objects.equals(oldFormat.getCharset(), charset)
                                && oldFormat.getFileExtensions().equals(fileExtensionList)
                                && oldFormat.getAnnotationClass().equals(annotationClass)
                                && oldFormat.compressible == compressible) {

                            // Registering the same format with same attributes twice
                            return oldFormat;

                        } else {
                            // Replace old format with new one with same name
                            LOGGER.debug("Registered (updated) annotation format {}", newFormat);
                            final List<Format> newFormats = Lists.newArrayList(formats);
                            newFormats.remove(oldFormat);
                            newFormats.add(newFormat);
                            formats = newFormats;
                            return newFormat;
                        }
                    }
                }

                // Append new format
                LOGGER.debug("Registered (added) annotation format {}", newFormat);
                formats = ImmutableList
                        .copyOf(Iterables.concat(formats, ImmutableList.of(newFormat)));
                return newFormat;
            }
        }

        public static List<Format> values() {
            return formats;
        }

        @Nullable
        public static Format valueOf(final String name) {
            for (final Format format : formats) {
                if (format.getName().equalsIgnoreCase(name)) {
                    return format;
                }
            }
            return null;
        }

        @Nullable
        public static Optional<Format> forMIMEType(final String mimeType) {
            return matchMIMEType(mimeType, formats);
        }

        @Nullable
        public static Optional<Format> forFileName(final String fileName) {
            return matchFileName(fileName, formats);
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return this.annotationClass;
        }

        public boolean isCompressible() {
            return this.compressible;
        }

        static {
            try {
                for (final Enumeration<URL> e = Annotation.class.getClassLoader().getResources(
                        "META-INF/services/" + Annotation.class.getName()); e.hasMoreElements();) {
                    for (String line : Resources.readLines(e.nextElement(), Charsets.UTF_8)) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }
                        try {
                            final Class<?> clazz = Class.forName(line.trim());
                            if (!Annotation.class.isAssignableFrom(clazz)) {
                                LOGGER.warn("Invalid annotation class " + line);
                            }
                        } catch (final Throwable ex) {
                            LOGGER.warn("Could not load class " + line, ex);
                        }
                    }
                }
            } catch (final IOException ex) {
                throw new Error(ex);
            }
        }

    }

}
