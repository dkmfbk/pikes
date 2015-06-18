package eu.fbk.dkm.pikes.rdf;

import com.google.common.collect.*;
import eu.fbk.dkm.utils.vocab.GAF;
import eu.fbk.dkm.utils.vocab.KS;
import eu.fbk.dkm.utils.vocab.NIF;
import eu.fbk.rdfpro.util.Statements;
import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// TODO: define RDFModel (quad extension of Model) and KSModel (with methods specific to KS
// schema)

public final class ModelUtil {

    private static final Map<String, URI> LANGUAGE_CODES_TO_URIS;

    private static final Map<URI, String> LANGUAGE_URIS_TO_CODES;

    static {
        final Map<String, URI> codesToURIs = Maps.newHashMap();
        final Map<URI, String> urisToCodes = Maps.newHashMap();
        for (final String language : Locale.getISOLanguages()) {
            final Locale locale = new Locale(language);
            final URI uri = Statements.VALUE_FACTORY.createURI("http://lexvo.org/id/iso639-3/",
                    locale.getISO3Language());
            codesToURIs.put(language, uri);
            urisToCodes.put(uri, language);
        }
        LANGUAGE_CODES_TO_URIS = ImmutableMap.copyOf(codesToURIs);
        LANGUAGE_URIS_TO_CODES = ImmutableMap.copyOf(urisToCodes);
    }

    public static Set<Resource> getMentions(final Model model) {
        return model.filter(null, RDF.TYPE, KS.MENTION).subjects();
    }

    public static Set<Resource> getMentions(final Model model, final int beginIndex,
            final int endIndex) {
        final List<Resource> mentionIDs = Lists.newArrayList();
        for (final Resource mentionID : model.filter(null, RDF.TYPE, KS.MENTION).subjects()) {
            final Literal begin = model.filter(mentionID, NIF.BEGIN_INDEX, null).objectLiteral();
            final Literal end = model.filter(mentionID, NIF.END_INDEX, null).objectLiteral();
            if (begin != null && begin.intValue() >= beginIndex && end != null
                    && end.intValue() <= endIndex) {
                mentionIDs.add(mentionID);
            }
        }
        return ImmutableSet.copyOf(mentionIDs);
    }

    public static Model getSubModel(final Model model,
            final Iterable<? extends Resource> mentionIDs) {

        final Model result = new LinkedHashModel();
        final Set<Resource> nodes = Sets.newHashSet();

        // Add all the triples (i) describing the mention; (ii) linking the mention to denoted
        // entities or expressed facts; (iii) describing expressed facts; (iv) expressed by the
        // mention; and (v) reachable by added resources and not expressed by some mention
        for (final Resource mentionID : mentionIDs) {
            result.addAll(model.filter(mentionID, null, null));
            for (final Statement triple : model.filter(null, null, mentionID)) {
                result.add(triple);
                if (triple.getPredicate().equals(KS.EXPRESSED_BY)) {
                    final Resource factID = triple.getSubject();
                    result.addAll(model.filter(factID, null, null));
                    for (final Statement factTriple : model.filter(null, null, null, factID)) {
                        result.add(factTriple);
                        final Resource factSubj = factTriple.getSubject();
                        final URI factPred = factTriple.getPredicate();
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
                    if (model.filter(context, KS.EXPRESSED_BY, null).isEmpty()) {
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

    public static URI languageCodeToURI(@Nullable final String code)
            throws IllegalArgumentException {
        if (code == null) {
            return null;
        }
        final int length = code.length();
        if (length == 2) {
            final URI uri = LANGUAGE_CODES_TO_URIS.get(code);
            if (uri != null) {
                return uri;
            }
        } else if (length == 3) {
            final URI uri = Statements.VALUE_FACTORY.createURI("http://lexvo.org/id/iso639-3/"
                    + code);
            if (LANGUAGE_URIS_TO_CODES.containsKey(uri)) {
                return uri;
            }
        }
        throw new IllegalArgumentException("Invalid language code: " + code);
    }

    @Nullable
    public static String languageURIToCode(@Nullable final URI uri)
            throws IllegalArgumentException {
        if (uri == null) {
            return null;
        }
        final String code = LANGUAGE_URIS_TO_CODES.get(uri);
        if (code != null) {
            return code;
        }
        throw new IllegalArgumentException("Invalid language URI: " + uri);
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
        // exactly should I do?'), extended to deal with IRIs instead of URIs:
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
                    || c >= 0xA0 && c <= 0xD7FF || c >= 0xF900 && c <= 0xFDCF || c >= 0xFDF0
                    && c <= 0xFFEF) {
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

        // Return the cleaned IRI (no Java validation as it is an IRI, not a URI)
        return builder.toString();
    }

}
