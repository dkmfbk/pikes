package eu.fbk.dkm.pikes.rdf.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.rdfpro.util.Namespaces;
import eu.fbk.rdfpro.util.QuadModel;

final class HttpRestAnnotator implements Annotator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRestAnnotator.class);

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{[^\\}]+\\}");

    private final String url;

    private final boolean post;

    private final Map<String, String> parameters;

    private final Map<String, String> headers;

    @Nullable
    private final Annotation.Format format;

    public HttpRestAnnotator(final String url, final boolean post,
            final Map<String, String> parameters, final Map<String, String> headers,
            @Nullable final Annotation.Format format) {

        this.url = Objects.requireNonNull(url);
        this.post = post;
        this.parameters = parameters == null ? ImmutableMap.of() : ImmutableMap.copyOf(parameters);
        this.headers = headers == null ? ImmutableMap.of() : ImmutableMap.copyOf(headers);
        this.format = format;
    }

    @Override
    public Annotation annotate(final QuadModel model) throws Exception {

        // Build the args map
        final Map<String, Value> args = Maps.newHashMap();
        try {
            final Statement stmt = Iterables.getOnlyElement( //
                    model.filter(null, NIF.SOURCE_URL, null));
            final URI uri = (URI) stmt.getSubject();
            args.put("uri", uri);
            args.put("text", model.filter((Resource) stmt.getObject(), NIF.IS_STRING, null)
                    .objectLiteral());
            for (final Statement stmt2 : model.filter(uri, null, null)) {
                final URI p = stmt2.getPredicate();
                final Value o = stmt2.getObject();
                args.put(p.stringValue(), o);
                args.put(p.getLocalName(), o);
                for (final String prefix : Namespaces.DEFAULT.prefixesFor(p.getNamespace())) {
                    args.put(prefix + ":" + p.getLocalName(), o);
                }
            }
        } catch (final Throwable ex) {
            throw new IllegalArgumentException("Cannot find (unique) document URI and text", ex);
        }

        // Build a URL encoded parameters string
        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, String> entry : this.parameters.entrySet()) {
            final String name = entry.getKey();
            final String value = format(entry.getValue(), args);
            if (!Strings.isNullOrEmpty(value)) {
                final Escaper escaper = UrlEscapers.urlFormParameterEscaper();
                builder.append(builder.length() == 0 ? "" : "&");
                builder.append(escaper.escape(name));
                builder.append("=");
                builder.append(escaper.escape(value));
            }
        }

        // Build the URL to invoke by appending parameters as the query string, if necessary
        final URL url = new URL(this.post || builder.length() == 0 ? this.url //
                : this.url + "?" + builder.toString());

        // Open a connection
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(this.post ? "POST" : "GET");

        // Set request headers
        for (final Map.Entry<String, String> entry : this.headers.entrySet()) {
            final String name = entry.getKey();
            final String value = format(entry.getValue(), args);
            if (!Strings.isNullOrEmpty(value)) {
                connection.setRequestProperty(name, value);
            }
        }

        // Send request body in case of post
        if (this.post) {
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setDoOutput(true);
            connection.getOutputStream().write(builder.toString().getBytes(Charsets.UTF_8));
            connection.getOutputStream().close();
        }

        // Obtain the response status code. Throw an error on failure
        final int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException(status + " returned by " + (this.post ? "POST" : "GET") + " "
                    + url + (!this.post ? "" : "with body '" + builder.toString() + "'"));
        }

        // Determine the annotation of the response, if not using a fixed one
        Annotation.Format format = this.format;
        if (format == null) {
            final String mimeType = connection.getContentType();
            if (mimeType != null) {
                format = Annotation.Format.forMIMEType(mimeType);
            }
            if (format == null) {
                throw new IOException("Could not determine annotation format");
            }
        }

        // Retrieve and return an Annotation instance by parsing the response body
        try {
            final Annotation annotation = Annotation.read(connection.getInputStream(), format);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} {} with parameters {} --> {}", this.post ? "POST" : "GET",
                        this.url, builder.toString(), annotation);
            }
            return annotation;
        } finally {
            connection.getInputStream().close();
        }
    }

    private static String format(final String template, final Map<String, Value> args) {
        int offset = 0;
        final StringBuilder builder = new StringBuilder();
        final Matcher matcher = PARAMETER_PATTERN.matcher(template);
        while (matcher.find()) {
            builder.append(template.substring(offset, matcher.start()));
            offset = matcher.end();
            final String name = template.substring(matcher.start() + 2, matcher.end() - 1);
            final Value value = args.get(name);
            if (value != null) {
                builder.append(value.stringValue());
            }
        }
        builder.append(template.substring(offset));
        return builder.toString();
    }

}
