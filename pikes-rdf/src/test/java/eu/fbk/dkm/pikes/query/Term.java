package eu.fbk.dkm.pikes.query;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

public final class Term implements Comparable<Term> {

    private final String document;

    private final Term.Layer layer;

    private final String token;

    private final Map<String, String> attributes;

    private static Map<String, String> mapFor(final Object... attributeKeyValues) {
        if (attributeKeyValues.length == 0) {
            return ImmutableMap.of();
        } else {
            ImmutableMap.Builder<String, String> builder = null;
            builder = ImmutableMap.builder();
            for (int i = 0; i < attributeKeyValues.length; i += 2) {
                builder.put(attributeKeyValues[i].toString(), attributeKeyValues[i + 1].toString());
            }
            return builder.build();
        }
    }

    public Term(final String document, final Term.Layer layer, final String token,
            final Object... attributeKeyValues) {
        this(document, layer, token, mapFor(attributeKeyValues));
    }

    public Term(final String document, final Term.Layer layer, final String token,
            @Nullable final Map<String, String> attributes) {
        this.document = Objects.requireNonNull(document);
        this.layer = Objects.requireNonNull(layer);
        this.token = Objects.requireNonNull(token);
        this.attributes = attributes == null ? ImmutableMap.of() : ImmutableMap.copyOf(attributes);
    }

    public String getDocument() {
        return this.document;
    }

    public Term.Layer getLayer() {
        return this.layer;
    }

    public String getToken() {
        return this.token;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    @Override
    public int compareTo(final Term other) {
        int result = this.document.compareTo(other.document);
        if (result == 0) {
            result = this.layer.compareTo(other.layer);
            if (result == 0) {
                result = this.token.compareTo(other.token);
                if (result == 0) {
                    result = this.attributes.hashCode() - other.attributes.hashCode(); // TODO
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Term)) {
            return false;
        }
        final Term other = (Term) object;
        return this.layer == other.layer && this.document.equals(other.document)
                && this.token.equals(other.token) && this.attributes.equals(other.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.document, this.layer, this.token, this.attributes);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.document);
        builder.append("\t");
        builder.append(this.layer.getID());
        builder.append("\t");
        builder.append(this.token);
        if (!this.attributes.isEmpty()) {
            for (final String key : Ordering.natural().sortedCopy(this.attributes.keySet())) {
                builder.append("\t");
                builder.append(key);
                builder.append("=");
                builder.append(this.attributes.get(key));
            }
        }
        return builder.toString();
    }

    public static enum Layer {

        RAW("raw"),

        STEM_TEXT("stem.text"),

        STEM_SYNONYM("stem.synonym"),

        STEM_RELATED("stem.related"),

        STEM_SUBWORD("stem.subword"),

        LEMMA_TEXT("lemma.text"),

        LEMMA_SYNONYM("lemma.synonym"),

        LEMMA_RELATED("lemma.related"),

        SYNSET_SPECIFIC("synset.specific"),

        SYNSET_RELATED("synset.related"),

        SYNSET_HYPERNYN("synset.hypernym"),

        URI_DBPEDIA("uri.dbpedia"),

        URI_CUSTOM("uri.custom"),

        URI_RELATED("uri.related"),

        TYPE_YAGO("type.yago"),

        TYPE_SUMO("type.sumo"),

        PREDICATE_FRB("predicate.frb"),

        PREDICATE_PB("predicate.pb"),

        PREDICATE_NB("predicate.nb"),

        ROLE_FRB("role.frb"),

        ROLE_PB("role.pb"),

        ROLE_NB("role.nb"),

        CONCEPT("concept");

        private final String id;

        private Layer(final String id) {
            this.id = id;
        }

        public String getID() {
            return this.id;
        }

    }

}