package eu.fbk.dkm.pikes.kv;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import eu.fbk.rdfpro.AbstractRDFHandlerWrapper;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.util.QuadModel;

public interface KeyQuadSource {

    default QuadModel get(final Value key) {
        Objects.requireNonNull(key);
        final QuadModel model = QuadModel.create();
        get(key, model);
        return model;
    }

    boolean get(Value key, RDFHandler handler) throws RDFHandlerException;

    default boolean get(final Value key, final Collection<Statement> model) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(model);
        try {
            return get(key, RDFHandlers.wrap(model));
        } catch (final RDFHandlerException ex) {
            throw new Error(ex);
        }
    }

    default QuadModel getAll(final Iterable<? extends Value> keys) {
        Objects.requireNonNull(keys);
        final QuadModel model = QuadModel.create();
        getAll(keys, model);
        return model;
    }

    default int getAll(final Iterable<? extends Value> keys, final RDFHandler handler)
            throws RDFHandlerException {
        Objects.requireNonNull(keys);
        Objects.requireNonNull(handler);
        int result = 0;
        for (final Value key : keys instanceof Set ? keys : ImmutableSet.copyOf(keys)) {
            final boolean found = get(key, handler);
            result += found ? 1 : 0;
        }
        return result;
    }

    default int getAll(final Iterable<? extends Value> keys, final Collection<Statement> model) {
        Objects.requireNonNull(keys);
        Objects.requireNonNull(model);
        try {
            return getAll(keys, RDFHandlers.wrap(model));
        } catch (final RDFHandlerException ex) {
            throw new Error(ex);
        }
    }

    default QuadModel getRecursive(final Iterable<? extends Value> keys,
            @Nullable final Predicate<Value> matcher) {
        Objects.requireNonNull(keys);
        final QuadModel model = QuadModel.create();
        getRecursive(keys, matcher, model);
        return model;
    }

    default int getRecursive(final Iterable<? extends Value> keys,
            @Nullable final Predicate<Value> matcher, final RDFHandler handler)
            throws RDFHandlerException {

        Objects.requireNonNull(keys);

        final Set<Value> visited = Sets.newHashSet();
        final List<Value> queue = Lists.newLinkedList(keys);

        final RDFHandler sink = new AbstractRDFHandlerWrapper(handler) {

            @Override
            public void handleStatement(final Statement stmt) throws RDFHandlerException {
                super.handleStatement(stmt);
                enqueueIfMatches(stmt.getSubject());
                enqueueIfMatches(stmt.getPredicate());
                enqueueIfMatches(stmt.getObject());
                enqueueIfMatches(stmt.getContext());
            }

            private void enqueueIfMatches(@Nullable final Value value) {
                if (value != null && (matcher == null || matcher.test(value))) {
                    queue.add(value);
                }
            }

        };

        int result = 0;
        while (!queue.isEmpty()) {
            final Value key = queue.remove(0);
            if (visited.add(key)) {
                final boolean found = get(key, sink);
                result += found ? 1 : 0;
            }
        }
        return result;
    }

    default int getRecursive(final Iterable<? extends Value> keys,
            @Nullable final Predicate<Value> matcher, final Collection<Statement> model) {
        Objects.requireNonNull(keys);
        Objects.requireNonNull(model);
        try {
            return getRecursive(keys, matcher, RDFHandlers.wrap(model));
        } catch (final RDFHandlerException ex) {
            throw new Error(ex);
        }
    }

}
