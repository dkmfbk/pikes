package eu.fbk.dkm.pikes.rdf.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import eu.fbk.dkm.pikes.rdf.vocab.GAF;
import eu.fbk.dkm.pikes.rdf.vocab.KEM;
import eu.fbk.dkm.pikes.rdf.vocab.KS_OLD;
import eu.fbk.dkm.pikes.rdf.vocab.NIF;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.Namespaces;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

// TODO: define RDFModel (quad extension of Model) and KSModel (with methods specific to KS
// schema)

public final class ModelUtil {

    private static final Map<String, IRI> LANGUAGE_CODES_TO_IRIS;

    private static final Map<IRI, String> LANGUAGE_IRIS_TO_CODES;

    static {
        final Map<String, IRI> codesToIRIs = Maps.newHashMap();
        final Map<IRI, String> urisToCodes = Maps.newHashMap();
        for (final String language : Locale.getISOLanguages()) {
            final Locale locale = new Locale(language);
            final IRI uri = Statements.VALUE_FACTORY.createIRI("http://lexvo.org/id/iso639-3/",
                    locale.getISO3Language());
            codesToIRIs.put(language, uri);
            urisToCodes.put(uri, language);
        }
        LANGUAGE_CODES_TO_IRIS = ImmutableMap.copyOf(codesToIRIs);
        LANGUAGE_IRIS_TO_CODES = ImmutableMap.copyOf(urisToCodes);
    }

    public static Set<Resource> getMentions(final QuadModel model) {
        return model.filter(null, RDF.TYPE, KS_OLD.MENTION).subjects();
    }

    public static Set<Resource> getMentions(final QuadModel model, final int beginIndex,
            final int endIndex) {
        final List<Resource> mentionIDs = Lists.newArrayList();
        for (final Resource mentionID : model.filter(null, RDF.TYPE, KS_OLD.MENTION).subjects()) {
            final Literal begin = model.filter(mentionID, NIF.BEGIN_INDEX, null).objectLiteral();
            final Literal end = model.filter(mentionID, NIF.END_INDEX, null).objectLiteral();
            if (begin != null && begin.intValue() >= beginIndex && end != null
                    && end.intValue() <= endIndex) {
                mentionIDs.add(mentionID);
            }
        }
        return ImmutableSet.copyOf(mentionIDs);
    }

    public static QuadModel getSubModel(final QuadModel model,
            final Iterable<? extends Resource> mentionIDs) {

        final QuadModel result = QuadModel.create();
        final Set<Resource> nodes = Sets.newHashSet();

        // Add all the triples (i) describing the mention; (ii) linking the mention to denoted
        // entities or expressed facts; (iii) describing expressed facts; (iv) expressed by the
        // mention; and (v) reachable by added resources and not expressed by some mention
        for (final Resource mentionID : mentionIDs) {
            result.addAll(model.filter(mentionID, null, null));
            for (final Statement triple : model.filter(null, null, mentionID)) {
                result.add(triple);
                if (triple.getPredicate().equals(KS_OLD.EXPRESSED_BY)) {
                    final Resource factID = triple.getSubject();
                    result.addAll(model.filter(factID, null, null));
                    for (final Statement factTriple : model.filter(null, null, null, factID)) {
                        result.add(factTriple);
                        final Resource factSubj = factTriple.getSubject();
                        final IRI factPred = factTriple.getPredicate();
                        final Value factObj = factTriple.getObject();
                        nodes.add(factSubj);
                        if (factObj instanceof Resource && !factPred.equals(GAF.DENOTED_BY)) {
                            nodes.add((Resource) factObj);
                        }
                    }
                } else {
                    nodes.add(triple.getSubject());
                }
            }
        }

        // Add all the triples not linked to some mention rooted at some node previously extracted
        final List<Resource> queue = Lists.newLinkedList(nodes);
        while (!queue.isEmpty()) {
            final Resource node = queue.remove(0);
            for (final Statement triple : model.filter(node, null, null)) {
                if (triple.getContext() != null) {
                    final Resource context = triple.getContext();
                    if (model.filter(context, KS_OLD.EXPRESSED_BY, null).isEmpty()) {
                        result.add(triple);
                        if (triple.getObject() instanceof Resource) {
                            final Resource obj = (Resource) triple.getObject();
                            if (nodes.add(obj)) {
                                queue.add(obj);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static IRI languageCodeToIRI(@Nullable final String code)
            throws IllegalArgumentException {
        if (code == null) {
            return null;
        }
        final int length = code.length();
        if (length == 2) {
            final IRI uri = LANGUAGE_CODES_TO_IRIS.get(code);
            if (uri != null) {
                return uri;
            }
        } else if (length == 3) {
            final IRI uri = Statements.VALUE_FACTORY
                    .createIRI("http://lexvo.org/id/iso639-3/" + code);
            if (LANGUAGE_IRIS_TO_CODES.containsKey(uri)) {
                return uri;
            }
        }
        throw new IllegalArgumentException("Invalid language code: " + code);
    }

    @Nullable
    public static String languageIRIToCode(@Nullable final IRI uri)
            throws IllegalArgumentException {
        if (uri == null) {
            return null;
        }
        final String code = LANGUAGE_IRIS_TO_CODES.get(uri);
        if (code != null) {
            return code;
        }
        throw new IllegalArgumentException("Invalid language IRI: " + uri);
    }

    /**
     * Clean an illegal IRI string, trying to make it legal (as per RFC 3987).
     *
     * @param string
     *            the IRI string to clean
     * @return the cleaned IRI string (possibly the input unchanged) upon success
     * @throws IllegalArgumentException
     *             in case the supplied input cannot be transformed into a legal IRI
     */
    @Nullable
    public static String cleanIRI(@Nullable final String string) throws IllegalArgumentException {

        // TODO: we only replace illegal characters, but we should also check and fix the IRI
        // structure

        // We implement the cleaning suggestions provided at the following URL (section 'So what
        // exactly should I do?'), extended to deal with IRIs instead of IRIs:
        // https://unspecified.wordpress.com/2012/02/12/how-do-you-escape-a-complete-uri/

        // Handle null input
        if (string == null) {
            return null;
        }

        // Illegal characters should be percent encoded. Illegal IRI characters are all the
        // character that are not 'unreserved' (A-Z a-z 0-9 - . _ ~ 0xA0-0xD7FF 0xF900-0xFDCF
        // 0xFDF0-0xFFEF) or 'reserved' (! # $ % & ' ( ) * + , / : ; = ? @ [ ])
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            final char c = string.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= '?' && c <= '[' || c >= '&' && c <= ';' || c == '#'
                    || c == '$' || c == '!' || c == '=' || c == ']' || c == '_' || c == '~'
                    || c >= 0xA0 && c <= 0xD7FF || c >= 0xF900 && c <= 0xFDCF
                    || c >= 0xFDF0 && c <= 0xFFEF) {
                builder.append(c);
            } else if (c == '%' && i < string.length() - 2
                    && Character.digit(string.charAt(i + 1), 16) >= 0
                    && Character.digit(string.charAt(i + 2), 16) >= 0) {
                builder.append('%'); // preserve valid percent encodings
            } else {
                builder.append('%').append(Character.forDigit(c / 16, 16))
                        .append(Character.forDigit(c % 16, 16));
            }
        }

        // Return the cleaned IRI (no Java validation as it is an IRI, not a IRI)
        return builder.toString();
    }

    public static void write(final Iterable<Statement> model, final String location)
            throws IOException {

        final RDFFormat format = Rio.getWriterFormatForFileName(location).get();
        if (format == null) {
            throw new IOException("Unsupported RDF format for " + location);
        }

        try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(location)))) {
            write(model, format, writer);
        }
    }

    public static void write(final Iterable<Statement> stmts, final RDFFormat format,
            final Writer writer) throws IOException {

        final RDFWriter sink = Rio.createWriter(format, writer);

        final boolean isTrig = format.equals(RDFFormat.TRIG);
        final boolean isTurtle = !isTrig && format.equals(RDFFormat.TURTLE);

        // Default efficient writing for non-prettified formats
        if (!isTrig && !isTurtle) {
            RDFSources.wrap(stmts).emit(sink, 1);
            return;
        }

        final Map<String, String> prefixes = Maps.newHashMap();
        final Set<Resource> instanceContexts = Sets.newHashSet();
        for (final Statement stmt : stmts) {
            final Resource s = stmt.getSubject();
            final IRI p = stmt.getPredicate();
            final Value o = stmt.getObject();
            final Resource c = stmt.getContext();
            collectPrefixes(s, prefixes);
            collectPrefixes(p, prefixes);
            collectPrefixes(o, prefixes);
            collectPrefixes(c, prefixes);
            if (o instanceof Resource && (p.equals(KEM.SUBSTANTIATES) || p.equals(KEM.CONVEYS))) {
                instanceContexts.add((Resource) stmt.getObject());
            }
        }

        final Set<Resource> resources = Sets.newHashSet();
        final Set<Resource> mentions = Sets.newHashSet();
        final Set<Resource> annotations = Sets.newHashSet();
        final Set<Resource> instances = Sets.newHashSet();
        final List<Statement> instanceStmts = Lists.newArrayList();
        final List<Statement> mentionStmts = Lists.newArrayList();
        for (final Statement stmt : stmts) {
            boolean skip = false;
            if (stmt.getObject() instanceof Resource) {
                final Resource s = stmt.getSubject();
                final IRI p = stmt.getPredicate();
                final Resource o = (Resource) stmt.getObject();
                if (p.equals(RDF.TYPE)) {
                    if (o instanceof BNode) {
                        skip = true;
                    } else if (o.equals(KEM.RESOURCE)) {
                        resources.add(s);
                    } else if (o.equals(KEM.MENTION)) {
                        mentions.add(o);
                    } else if (o.equals(KEM.ANNOTATION)) {
                        annotations.add(o);
                    }
                } else if (p.equals(KEM.FRAGMENT_OF)) {
                    mentions.add(stmt.getSubject());
                    resources.add(o);
                } else if (p.equals(KEM.HAS_ANNOTATION)) {
                    mentions.add(stmt.getSubject());
                    annotations.add(o);
                } else if (p.equals(KEM.IS_ABOUT) || p.equals(KEM.REFERS_TO)) {
                    mentions.add(stmt.getSubject());
                    instances.add(o);
                } else if (p.equals(KEM.SUBJECT) || p.equals(KEM.INVOLVES)) {
                    annotations.add(stmt.getSubject());
                    instances.add(o);
                }
            }
            if (!skip) {
                (instanceContexts.contains(stmt.getContext()) ? instanceStmts : mentionStmts)
                        .add(stmt);
            }
        }

        String base = null;
        if (!resources.isEmpty()) {
            base = StringUtils.getCommonPrefix(
                    resources.stream().map(r -> r.toString()).toArray(size -> new String[size]));
        }

        sink.startRDF();

        for (final String name : Ordering.natural().sortedCopy(prefixes.keySet())) {
            final String prefix = prefixes.get(name);
            if (prefix != null) {
                sink.handleNamespace(prefix, name);
            } else {
                prefixes.remove(name);
            }
        }

        if (base != null) {
            writer.append("@base <").append(base).append("> .\n");
        }

        if (!instanceStmts.isEmpty()) {
            writer.write("\n#\n# Instance layer: " + instances.size() + " instance(s), "
                    + instanceStmts.size() + " statement(s), " + instanceContexts.size()
                    + " graph(s)\n#\n\n");

            Collections.sort(instanceStmts, Statements.statementComparator("spoc",
                    Statements.valueComparator(false, RDF.NAMESPACE)));
            if (isTurtle) {
                for (final Statement stmt : instanceStmts) {
                    sink.handleStatement(rewriteWithRelativeIRIs(base, stmt));
                }
            } else {
                final Namespaces namespaces = Namespaces.forPrefixMap(prefixes, false);
                for (final Statement stmt : instanceStmts) {
                    final Statement rewStmt = rewriteWithRelativeIRIs(base, stmt);
                    Statements.formatValue(rewStmt.getContext(), namespaces, writer);
                    writer.append(" { ");
                    Statements.formatValue(rewStmt.getSubject(), namespaces, writer);
                    writer.append(" ");
                    if (stmt.getPredicate().equals(RDF.TYPE)) {
                        writer.append("a");
                    } else {
                        Statements.formatValue(rewStmt.getPredicate(), namespaces, writer);
                    }
                    writer.append(" ");
                    Statements.formatValue(rewStmt.getObject(), namespaces, writer);
                    writer.append(" }\n");
                }
            }
        }

        if (!mentionStmts.isEmpty()) {
            writer.write("\n#\n# Mention layer: " + resources.size() + " resource(s), "
                    + mentions.size() + " mention(s), " + annotations.size() + " annotation(s), "
                    + mentionStmts.size() + " statement(s)\n#\n");

            Collections.sort(mentionStmts, Statements.statementComparator("cspo",
                    Statements.valueComparator(false, RDF.NAMESPACE)));
            for (final Statement stmt : mentionStmts) {
                sink.handleStatement(rewriteWithRelativeIRIs(base, stmt));
            }
        }

        sink.endRDF();
    }

    private static void collectPrefixes(final Value value, final Map<String, String> prefixes) {
        if (value instanceof IRI) {
            final String ns = ((IRI) value).getNamespace();
            if (!prefixes.containsKey(ns)) {
                final String prefix = Namespaces.DEFAULT.prefixFor(ns);
                prefixes.put(ns, prefix);
            }
        } else if (value instanceof Literal) {
            final Literal literal = (Literal) value;
            if (literal.getDatatype() != null) {
                collectPrefixes(literal.getDatatype(), prefixes);
            }
        }
    }

    @Nullable
    private static Statement rewriteWithRelativeIRIs(@Nullable final String base,
            @Nullable final Statement stmt) {
        if (base != null && stmt != null) {
            final Resource s = rewriteWithRelativeIRIs(base, stmt.getSubject());
            final IRI p = rewriteWithRelativeIRIs(base, stmt.getPredicate());
            final Value o = rewriteWithRelativeIRIs(base, stmt.getObject());
            final Resource c = rewriteWithRelativeIRIs(base, stmt.getContext());
            if (s != stmt.getSubject() || p != stmt.getPredicate() || o != stmt.getObject()
                    || c != stmt.getContext()) {
                final ValueFactory vf = SimpleValueFactory.getInstance();
                return c == null ? vf.createStatement(s, p, o) : vf.createStatement(s, p, o, c);
            }
        }
        return stmt;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T extends Value> T rewriteWithRelativeIRIs(@Nullable final String base,
            @Nullable final T value) {
        if (base != null && value instanceof IRI) {
            final IRI iri = (IRI) value;
            if (Namespaces.DEFAULT.prefixFor(iri.getNamespace()) == null) {
                final String iriString = value.toString();
                if (iriString.length() > base.length() && iriString.startsWith(base)) {
                    return (T) new RelativeIRI(iriString.substring(base.length()));
                }
            }
        }
        return value;
    }

    private final static class RelativeIRI implements IRI {

        private static final long serialVersionUID = 1L;

        private final String iriString;

        RelativeIRI(final String iriString) {
            this.iriString = iriString;
        }

        @Override
        public String toString() {
            return this.iriString;
        }

        @Override
        public String stringValue() {
            return this.iriString;
        }

        @Override
        public String getNamespace() {
            return "";
        }

        @Override
        public String getLocalName() {
            return this.iriString;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof IRI)) {
                return false;
            }
            final IRI other = (IRI) object;
            return this.iriString.equals(other.toString());
        }

        @Override
        public int hashCode() {
            return this.iriString.hashCode();
        }

    }

    // namespaces.add(KS.NS);
    // namespaces.add(DCTERMS.NS);
    // namespaces.add(OWLTIME.NS);
    // namespaces.add(XMLSchema.NS);
    // namespaces.add(OWL.NS); // not strictly necessary
    // namespaces.add(ITSRDF.NS);
    // namespaces.add(new SimpleNamespace("bbn", "http://pikes.fbk.eu/bbn/"));
    // namespaces.add(new SimpleNamespace("pm", "http://premon.fbk.eu/resource/"));
    // namespaces.add(new SimpleNamespace("ili", "http://sli.uvigo.gal/rdf_galnet/"));
    // add missing namespace http://premon.fbk.eu/resource/, http://pikes.fbk.eu/ner/

}
