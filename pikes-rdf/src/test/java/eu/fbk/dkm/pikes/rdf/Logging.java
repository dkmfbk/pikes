/*
 * RDFpro - An extensible tool for building stream-oriented RDF processing libraries.
 * 
 * Written in 2014 by Francesco Corcoglioniti <francesco.corcoglioniti@gmail.com> with support by
 * Marco Rospocher, Marco Amadori and Michele Mostarda.
 * 
 * To the extent possible under law, the author has dedicated all copyright and related and
 * neighboring rights to this software to the public domain worldwide. This software is
 * distributed without any warranty.
 * 
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package eu.fbk.dkm.pikes.rdf;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.util.EnvUtil;

public final class Logging {

    public static final String MDC_CONTEXT = "context";

    public static void setLevel(final Logger logger, final String level) {
        final Level l = Level.valueOf(level);
        ((ch.qos.logback.classic.Logger) logger).setLevel(l);
    }

    public static final class NormalConverter extends
            ForegroundCompositeConverterBase<ILoggingEvent> {

        @Override
        protected String getForegroundColorCode(final ILoggingEvent event) {
            final ch.qos.logback.classic.Level level = event.getLevel();
            switch (level.toInt()) {
            case ch.qos.logback.classic.Level.ERROR_INT:
                return ANSIConstants.RED_FG;
            case ch.qos.logback.classic.Level.WARN_INT:
                return ANSIConstants.MAGENTA_FG;
            default:
                return ANSIConstants.DEFAULT_FG;
            }
        }

    }

    public static final class BoldConverter extends
            ForegroundCompositeConverterBase<ILoggingEvent> {

        @Override
        protected String getForegroundColorCode(final ILoggingEvent event) {
            final ch.qos.logback.classic.Level level = event.getLevel();
            switch (level.toInt()) {
            case ch.qos.logback.classic.Level.ERROR_INT:
                return ANSIConstants.BOLD + ANSIConstants.RED_FG;
            case ch.qos.logback.classic.Level.WARN_INT:
                return ANSIConstants.BOLD + ANSIConstants.MAGENTA_FG;
            default:
                return ANSIConstants.BOLD + ANSIConstants.DEFAULT_FG;
            }
        }

    }

    public static final class ContextConverter extends ClassicConverter {

        @Override
        public String convert(final ILoggingEvent event) {
            final String context = MDC.get(MDC_CONTEXT);
            final String logger = event.getLevel().toInt() >= Level.WARN_INT ? event
                    .getLoggerName() : null;
            if (context == null) {
                return logger == null ? "" : "[" + logger + "] ";
            } else {
                return logger == null ? "[" + context + "] " : "[" + context + "][" + logger
                        + "] ";
            }
        }

    }

    public static final class StatusAppender<E> extends UnsynchronizedAppenderBase<E> {

        private static final int MAX_STATUS_LENGTH = 256;

        private boolean withJansi;

        private Encoder<E> encoder;

        public synchronized boolean isWithJansi() {
            return this.withJansi;
        }

        public synchronized void setWithJansi(final boolean withJansi) {
            if (isStarted()) {
                addStatus(new ErrorStatus("Cannot configure appender named \"" + this.name
                        + "\" after it has been started.", this));
            }
            this.withJansi = withJansi;
        }

        public synchronized Encoder<E> getEncoder() {
            return this.encoder;
        }

        public synchronized void setEncoder(final Encoder<E> encoder) {
            if (isStarted()) {
                addStatus(new ErrorStatus("Cannot configure appender named \"" + this.name
                        + "\" after it has been started.", this));
            }
            this.encoder = encoder;
        }

        @SuppressWarnings("resource")
        @Override
        public synchronized void start() {

            // Abort if already started
            if (this.started) {
                return;
            }

            // Abort with error if there is no encoder attached to the appender
            if (this.encoder == null) {
                addStatus(new ErrorStatus("No encoder set for the appender named \"" + this.name
                        + "\".", this));
                return;
            }

            // Abort if there is no console attached to the process
            if (System.console() == null) {
                return;
            }

            // Setup streams required for generating and displaying status information
            final PrintStream out = System.out;
            final StatusAcceptorStream acceptor = new StatusAcceptorStream(out);
            OutputStream generator = new StatusGeneratorStream(acceptor);

            // Install Jansi if on Windows and enabled
            if (EnvUtil.isWindows() && this.withJansi) {
                try {
                    final Class<?> clazz = Class
                            .forName("org.fusesource.jansi.WindowsAnsiOutputStream");
                    final Constructor<?> constructor = clazz.getConstructor(OutputStream.class);
                    generator = (OutputStream) constructor.newInstance(generator);
                } catch (final Throwable ex) {
                    // ignore
                }
            }

            try {
                // Setup encoder. On success, replace System.out and start the appender
                this.encoder.init(generator);
                System.setOut(new PrintStream(acceptor));
                super.start();
            } catch (final IOException ex) {
                addStatus(new ErrorStatus("Failed to initialize encoder for appender named \""
                        + this.name + "\".", this, ex));
            }
        }

        @Override
        public synchronized void stop() {
            if (!isStarted()) {
                return;
            }
            try {
                this.encoder.close();
                // no need to restore System.out (due to buffering, better not to do that)

            } catch (final IOException ex) {
                addStatus(new ErrorStatus("Failed to write footer for appender named \""
                        + this.name + "\".", this, ex));
            } finally {
                super.stop();
            }
        }

        @Override
        protected synchronized void append(final E event) {
            if (!isStarted()) {
                return;
            }
            try {
                if (event instanceof DeferredProcessingAware) {
                    ((DeferredProcessingAware) event).prepareForDeferredProcessing();
                }
                this.encoder.doEncode(event);
            } catch (final IOException ex) {
                stop();
                addStatus(new ErrorStatus("IO failure in appender named \"" + this.name + "\".",
                        this, ex));
            }
        }

        private static final class StatusAcceptorStream extends FilterOutputStream {

            private static final int ESC = 27;

            private byte[] status;

            private boolean statusEnabled;

            public StatusAcceptorStream(final OutputStream stream) {
                super(stream);
                this.status = null;
                this.statusEnabled = true;
            }

            @Override
            public synchronized void write(final int b) throws IOException {
                enableStatus(false);
                this.out.write(b);
                enableStatus(b == '\n');
            }

            @Override
            public synchronized void write(final byte[] b) throws IOException {
                enableStatus(false);
                super.write(b);
                enableStatus(b[b.length - 1] == '\n');
            }

            @Override
            public synchronized void write(final byte[] b, final int off, final int len)
                    throws IOException {
                enableStatus(false);
                super.write(b, off, len);
                enableStatus(len > 0 && b[off + len - 1] == '\n');
            }

            synchronized void setStatus(final byte[] status) {
                final boolean oldEnabled = this.statusEnabled;
                enableStatus(false);
                this.status = status;
                enableStatus(oldEnabled);
            }

            private void enableStatus(final boolean enabled) {
                try {
                    if (enabled == this.statusEnabled) {
                        return;
                    }
                    this.statusEnabled = enabled;
                    if (this.status == null) {
                        return;
                    } else if (enabled) {
                        final int length = Math.min(this.status.length, MAX_STATUS_LENGTH);
                        this.out.write(this.status, 0, length);
                        this.out.write('\n'); // move cursor out of the way and cause flush
                    } else {
                        final int length = Math.min(this.status.length, MAX_STATUS_LENGTH);
                        int newlines = 1;
                        for (int i = 0; i < length; ++i) {
                            if (this.status[i] == '\n') {
                                ++newlines;
                            }
                        }
                        // move cursor up of # lines previously written
                        this.out.write(ESC);
                        this.out.write('[');
                        this.out.write(Integer.toString(newlines).getBytes());
                        this.out.write('A');
                        // we emit a newline to move cursor down one line and to column 1, then we
                        // move up one line, being sure to end up in column 1
                        this.out.write('\n');
                        this.out.write(ESC);
                        this.out.write('[');
                        this.out.write('1');
                        this.out.write('A');
                        // discard everything after the cursor; due to trick above we also discard
                        // text entered by the user (but not newline - they can be managed by
                        // saving and restoring cursor position, but many terminals do not handle
                        // these calls)
                        this.out.write(ESC);
                        this.out.write('[');
                        this.out.write('0');
                        this.out.write('J');
                    }
                } catch (final Throwable ex) {
                    if (ex instanceof Error) {
                        throw (Error) ex;
                    } else if (ex instanceof RuntimeException) {
                        throw (RuntimeException) ex;
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

        private static final class StatusGeneratorStream extends OutputStream {

            private final StatusAcceptorStream stream;

            private final byte[] buffer;

            private int offset;

            public StatusGeneratorStream(final StatusAcceptorStream stream) {
                this.stream = stream;
                this.buffer = new byte[MAX_STATUS_LENGTH];
                this.offset = 0;
            }

            @Override
            public void write(final int b) throws IOException {
                int emitCount = -1;
                if (b == '\n') {
                    if (this.offset < MAX_STATUS_LENGTH) {
                        emitCount = this.offset;
                    }
                    this.offset = 0;
                } else if (this.offset < MAX_STATUS_LENGTH) {
                    this.buffer[this.offset++] = (byte) b;
                    if (this.offset == MAX_STATUS_LENGTH) {
                        emitCount = this.offset;
                    }
                }
                if (emitCount >= 0) {
                    final byte[] status = new byte[emitCount];
                    System.arraycopy(this.buffer, 0, status, 0, emitCount);
                    this.stream.setStatus(status);
                }
            }

            @Override
            public void write(final byte[] b) throws IOException {
                for (int i = 0; i < b.length; ++i) {
                    write(b[i]);
                }
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                final int to = off + len;
                for (int i = off; i < to; ++i) {
                    write(b[i]);
                }
            }

        }

    }

}
