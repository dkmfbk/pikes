package eu.fbk.dkm.pikes.rdf.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import eu.fbk.rdfpro.util.Hash;

public final class Templates {

    public static Mustache load(final Object spec) {
        // Accepts Mustache, URL, File, String (url / filename / template)
        Preconditions.checkNotNull(spec);
        try {
            if (spec instanceof Mustache) {
                return (Mustache) spec;
            }
            final DefaultMustacheFactory factory = new DefaultMustacheFactory();
            // factory.setExecutorService(Environment.getPool()); // BROKEN
            URL url = spec instanceof URL ? (URL) spec : null;
            if (url == null) {
                try {
                    url = Templates.class.getClassLoader().getResource(spec.toString());
                } catch (final Throwable ex) {
                    // ignore
                }
            }
            if (url == null) {
                final File file = spec instanceof File ? (File) spec : new File(spec.toString());
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
            if (url != null) {
                return factory.compile(new InputStreamReader(url.openStream(), Charsets.UTF_8),
                        url.toString());
            } else {
                return factory.compile(new StringReader(spec.toString()),
                        Hash.murmur3(spec.toString()).toString());
            }
        } catch (final IOException ex) {
            throw new IllegalArgumentException("Could not create Mustache template for " + spec);
        }
    }

}
