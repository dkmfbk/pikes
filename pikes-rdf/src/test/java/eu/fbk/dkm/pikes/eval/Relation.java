package eu.fbk.dkm.pikes.eval;

import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;

public final class Relation implements Comparable<Relation> {

    private final IRI first;

    private final IRI second;

    private final boolean extra;

    public Relation(final IRI first, final IRI second, final boolean extra) {
        final boolean swap = Util.VALUE_ORDERING.compare(first, second) > 0;
        this.first = swap ? second : first;
        this.second = swap ? first : second;
        this.extra = extra;
    }

    public IRI getFirst() {
        return this.first;
    }

    public IRI getSecond() {
        return this.second;
    }

    public boolean isExtra() {
        return this.extra;
    }

    @Override
    public int compareTo(final Relation other) {
        int result = Util.VALUE_ORDERING.compare(this.first, other.first);
        if (result == 0) {
            result = Util.VALUE_ORDERING.compare(this.second, other.second);
        }
        return result;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Relation)) {
            return false;
        }
        final Relation other = (Relation) object;
        return this.first.equals(other.first) && this.second.equals(other.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.first, this.second);
    }

    public String toString(@Nullable final IRI baseIRI) {
        return "(" + Util.format(baseIRI, this.first) + ", " + Util.format(baseIRI, this.second)
                + ")";
    }

    @Override
    public String toString() {
        return toString(null);
    }

}