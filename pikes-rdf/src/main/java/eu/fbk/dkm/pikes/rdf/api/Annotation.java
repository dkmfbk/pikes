package eu.fbk.dkm.pikes.rdf.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import org.openrdf.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.lang.FileFormat;

import eu.fbk.rdfpro.util.IO;

public abstract class Annotation {

    private static final Logger LOGGER = LoggerFactory.getLogger(Annotation.class);

    public static Annotation read(final InputStream in, final Format format) throws IOException {
        return readHelper(in, format);
    }

    public static Annotation read(final Reader in, final Format format) throws IOException {
        return readHelper(in, format);
    }

    private static Annotation readHelper(final Object streamOrReader, final Format format)
            throws IOException {

        Objects.requireNonNull(streamOrReader);
        Objects.requireNonNull(format);

        if (format.readerConstructor == null && format.streamConstructor == null) {
            throw new IllegalArgumentException("No constructor available for reading " + format);
        }

        final Charset charset = format.getCharset() != null ? format.getCharset() : Charsets.UTF_8;
        Constructor<?> constructor = null;
        Object arg = streamOrReader;

        if (arg instanceof InputStream) {
            if (format.streamConstructor != null) {
                constructor = format.streamConstructor;
            } else {
                constructor = format.readerConstructor;
                final InputStream is = (InputStream) arg;
                arg = charset.equals(Charsets.UTF_8) ? IO.utf8Reader(is) : new InputStreamReader(
                        is, charset);
            }
        } else if (arg instanceof Reader) {
            if (format.readerConstructor != null) {
                constructor = format.readerConstructor;
            } else {
                constructor = format.streamConstructor;
                final String str = CharStreams.toString((Reader) arg);
                final byte[] bytes = str.getBytes(charset);
                arg = new ByteArrayInputStream(bytes);
            }
        }

        try {
            return (Annotation) constructor.newInstance(arg);
        } catch (final InvocationTargetException ex) {
            Throwables.propagateIfPossible(ex.getCause(), IOException.class);
            throw Throwables.propagate(ex.getCause());
        } catch (IllegalAccessException | InstantiationException ex) {
            throw new Error("Could not instantiate " + format.getAnnotationClass() + " using "
                    + constructor, ex);
        }
    }

    public abstract URI getURI();

    public abstract String getText();

    public abstract Format getFormat();

    public void write(final OutputStream out) throws IOException {
        final Charset charset = getFormat().getCharset();
        final Writer writer = charset == null || charset.equals(Charsets.UTF_8) ? IO
                .utf8Writer(out) : new OutputStreamWriter(out, charset);
        write(writer);
        writer.flush();
    }

    public void write(final Writer out) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        write(bos);
        final Charset charset = getFormat().getCharset();
        final String str = new String(bos.toByteArray(), charset != null ? charset
                : Charsets.UTF_8);
        out.write(str);
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Annotation)) {
            return false;
        }
        final Annotation other = (Annotation) object;
        return getURI().equals(other.getURI()) && getFormat().equals(other.getFormat());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getURI(), getFormat());
    }

    @Override
    public String toString() {
        return getFormat().getName() + " annotation of <" + getURI() + ">";
    }

    public static final class Format extends FileFormat {

        private static List<Format> formats = ImmutableList.of();

        private final Class<? extends Annotation> annotationClass;

        private final boolean compressible;

        @Nullable
        final Constructor<?> streamConstructor;

        @Nullable
        final Constructor<?> readerConstructor;

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
                formats = ImmutableList.copyOf(Iterables.concat(formats,
                        ImmutableList.of(newFormat)));
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
        public static Format forMIMEType(final String mimeType) {
            return forMIMEType(mimeType, null);
        }

        @Nullable
        public static Format forMIMEType(final String mimeType, @Nullable final Format fallback) {
            return matchMIMEType(mimeType, formats, fallback);
        }

        @Nullable
        public static Format forFileName(final String fileName) {
            return forFileName(fileName, null);
        }

        @Nullable
        public static Format forFileName(final String fileName, @Nullable final Format fallback) {
            return matchFileName(fileName, formats, fallback);
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return this.annotationClass;
        }

        public boolean isCompressible() {
            return this.compressible;
        }

    }

}
