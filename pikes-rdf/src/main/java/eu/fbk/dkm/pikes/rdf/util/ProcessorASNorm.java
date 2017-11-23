package eu.fbk.dkm.pikes.rdf.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


import com.google.common.collect.Ordering;
import eu.fbk.rdfpro.util.Namespaces;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import eu.fbk.rdfpro.Mapper;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.RDFProcessors;
import eu.fbk.rdfpro.Reducer;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Options;
import eu.fbk.rdfpro.util.Statements;

import javax.annotation.Nullable;

public final class ProcessorASNorm implements RDFProcessor {

    private final String namespace;

    private final Mapper checkedMapper;

    private final Mapper uncheckedMapper;

    private final Reducer factReducer;

    private final Reducer metaReducer;

    private static final Ordering<Value> DEFAULT_VALUE_ORDERING = new ValueOrdering(null);

    private static final Ordering<Statement> DEFAULT_STATEMENT_ORDERING = new StatementOrdering(
            "spoc", new ValueOrdering(ImmutableList.of(RDF.NAMESPACE)));

    static RDFProcessor create(final String name, final String... args) {
        final Options options = Options.parse("!", args);
        final String namespace = options.getPositionalArg(0, String.class);
        return new ProcessorASNorm(namespace);
    }

    public ProcessorASNorm(final String namespace) {
        this.namespace = namespace;
        this.checkedMapper = new CheckedMapper();
        this.uncheckedMapper = new UncheckedMapper();
        this.factReducer = new FactReducer();
        this.metaReducer = new MetaReducer();
    }

    @Override
    public RDFHandler wrap(final RDFHandler handler) {
        return RDFProcessors.mapReduce(this.checkedMapper, this.factReducer, true).wrap(
                RDFProcessors.mapReduce(this.uncheckedMapper, this.metaReducer, true)
                        .wrap(handler));
    }

    private boolean match(final Value value) {
        return value instanceof IRI && ((IRI) value).getNamespace().equals(this.namespace);
    }

    private IRI hash(final Resource subject, final IRI predicate, final Value object) {
        final List<String> list = Lists.newArrayList();
        for (final Value value : new Value[] { subject, predicate, object }) {
            if (value instanceof IRI) {
                list.add("\u0001");
                list.add(value.stringValue());
            } else if (value instanceof BNode) {
                list.add("\u0002");
                list.add(((BNode) value).getID());
            } else if (value instanceof Literal) {
                final Literal l = (Literal) value;
                list.add("\u0003");
                list.add(l.getLabel());
                if (!l.getDatatype().equals(XMLSchema.STRING)) {
                    list.add(l.getDatatype().stringValue());
                } else if (l.getLanguage().isPresent()) {
                    list.add(l.getLanguage().get());
                }
            }
        }
        final String hash = Hash.murmur3(list.toArray(new String[list.size()])).toString();
        return Statements.VALUE_FACTORY.createIRI(this.namespace, hash);
    }

    private IRI hash(final IRI id, final Iterable<Statement> statements) {
        final List<String> list = Lists.newArrayList();
        for (final Statement stmt : statements) {
            for (final Value value : new Value[] { stmt.getSubject(), stmt.getPredicate(),
                    stmt.getObject(), stmt.getContext() }) {
                if (value == null) {
                    list.add("\u0004");
                } else if (value.equals(id)) {
                    list.add("\u0005");
                } else if (value instanceof IRI) {
                    list.add("\u0001");
                    list.add(value.stringValue());
                } else if (value instanceof BNode) {
                    list.add("\u0002");
                    list.add(((BNode) value).getID());
                } else if (value instanceof Literal) {
                    final Literal l = (Literal) value;
                    list.add("\u0003");
                    list.add(l.getLabel());
                    if (!l.getDatatype().equals(XMLSchema.STRING)) {
                        list.add(l.getDatatype().stringValue());
                    } else if (l.getLanguage().isPresent()) {
                        list.add(l.getLanguage().get());
                    }
                }
            }
        }
        final String hash = Hash.murmur3(list.toArray(new String[list.size()])).toString();
        return Statements.VALUE_FACTORY.createIRI(this.namespace, hash);
    }

    @SuppressWarnings("unchecked")
    private <T extends Value, R extends Value> T replace(final T value, final R matchedValue,
            final R newValue) {
        if (value != null && value.equals(matchedValue)) {
            return (T) newValue;
        } else {
            return value;
        }
    }

    private void emit(final RDFHandler handler, final Value oldID, final IRI newID,
            final Statement factStmt, final Iterable<Statement> metaStmts)
            throws RDFHandlerException {

        if (oldID.equals(newID)) {
            // If annotation ID equal to old ID, emit fact and metadata unchanged
            handler.handleStatement(factStmt);
            for (final Statement metaStmt : metaStmts) {
                handler.handleStatement(metaStmt);
            }

        } else {
            // Else, replace old ID with new one
            handler.handleStatement(Statements.VALUE_FACTORY.createStatement(
                    factStmt.getSubject(), factStmt.getPredicate(), factStmt.getObject(), newID));
            for (final Statement metaStmt : metaStmts) {
                final Resource metaSubj = replace(metaStmt.getSubject(), oldID, newID);
                final IRI metaPred = replace(metaStmt.getPredicate(), oldID, newID);
                final Value metaObj = replace(metaStmt.getObject(), oldID, newID);
                final Resource metaCtx = replace(metaStmt.getContext(), oldID, newID);
                if (metaCtx == null) {
                    handler.handleStatement(Statements.VALUE_FACTORY.createStatement(metaSubj,
                            metaPred, metaObj));
                } else {
                    handler.handleStatement(Statements.VALUE_FACTORY.createStatement(metaSubj,
                            metaPred, metaObj, metaCtx));
                }
            }
        }
    }

    private final class CheckedMapper implements eu.fbk.rdfpro.Mapper {

        @Override
        public Value[] map(final Statement statement) throws RDFHandlerException {
            final String message = "Multiple annotation IDs in same statement";
            Value key = null;
            if (match(statement.getSubject())) {
                key = statement.getSubject();
            }
            if (match(statement.getContext())) {
                Preconditions.checkArgument(key == null, message);
                key = statement.getContext();
            }
            if (match(statement.getObject())) {
                Preconditions.checkArgument(key == null, message);
                key = statement.getObject();
            }
            if (match(statement.getPredicate())) {
                Preconditions.checkArgument(key == null, message);
                key = statement.getPredicate();
            }
            if (key == null) {
                key = Mapper.BYPASS_KEY;
            }
            return new Value[] { key };
        }

    }

    private final class UncheckedMapper implements eu.fbk.rdfpro.Mapper {

        @Override
        public Value[] map(final Statement statement) throws RDFHandlerException {
            if (match(statement.getSubject())) {
                return new Value[] { statement.getSubject() };
            } else if (match(statement.getContext())) {
                return new Value[] { statement.getContext() };
            } else if (match(statement.getObject())) {
                return new Value[] { statement.getObject() };
            } else if (match(statement.getPredicate())) {
                return new Value[] { statement.getPredicate() };
            } else {
                return new Value[] { eu.fbk.rdfpro.Mapper.BYPASS_KEY };
            }
        }

    }

    private final class FactReducer implements Reducer {

        @Override
        public void reduce(final Value id, final Statement[] stmts, final RDFHandler handler)
                throws RDFHandlerException {

            // Split statements into facts and meta
            final List<Statement> factStmts = Lists.newArrayListWithCapacity(stmts.length);
            final List<Statement> metaStmts = Lists.newArrayListWithCapacity(stmts.length);
            for (final Statement stmt : stmts) {
                if (id.equals(stmt.getContext())) {
                    factStmts.add(stmt);
                } else {
                    metaStmts.add(stmt);
                }
            }

            // Emit each fact statement with its own metadata statements, possibly changing IDs
            for (final Statement factStmt : factStmts) {
                final IRI newID = hash(factStmt.getSubject(), factStmt.getPredicate(),
                        factStmt.getObject());
                emit(handler, id, newID, factStmt, metaStmts);
            }
        }

    }

    private final class MetaReducer implements Reducer {

        private final Comparator<Statement> comparator = statementOrdering("spoc",
                valueOrdering(ProcessorASNorm.this.namespace));

        @Override
        public void reduce(final Value key, final Statement[] stmts, final RDFHandler handler)
                throws RDFHandlerException {

            // Split statements into fact (unique) and meta
            Statement factStmt = null;
            final List<Statement> metaStmts = Lists.newArrayListWithCapacity(stmts.length);
            for (final Statement stmt : stmts) {
                if (key.equals(stmt.getContext())) {
                    assert factStmt == null;
                    factStmt = stmt;
                } else {
                    metaStmts.add(stmt);
                }
            }
            assert factStmt != null;

            // Emit statements changing the annotation ID
            Collections.sort(metaStmts, this.comparator);
            final IRI metadataID = hash((IRI) key, metaStmts);
            emit(handler, key, metadataID, factStmt, metaStmts);
        }

    }

    private  static Ordering<Statement> statementOrdering(@Nullable final String components,
                                                          @Nullable final Comparator<? super Value> valueComparator) {
        if (components == null) {
            return valueComparator == null ? DEFAULT_STATEMENT_ORDERING //
                    : new StatementOrdering("spoc", valueComparator);
        } else {
            return new StatementOrdering(components,
                    valueComparator == null ? DEFAULT_VALUE_ORDERING : valueComparator);
        }
    }

    public static Ordering<Value> valueOrdering(final String... rankedNamespaces) {
        return rankedNamespaces == null || rankedNamespaces.length == 0 ? DEFAULT_VALUE_ORDERING
                : new ValueOrdering(Arrays.asList(rankedNamespaces));
    }


    private static final class ValueOrdering extends Ordering<Value> {

        private final List<String> rankedNamespaces;

        public ValueOrdering(@Nullable final Iterable<? extends String> rankedNamespaces) {
            this.rankedNamespaces = rankedNamespaces == null ? ImmutableList.of() : ImmutableList
                    .copyOf(rankedNamespaces);
        }

        @Override
        public int compare(final Value v1, final Value v2) {
            if (v1 instanceof IRI) {
                if (v2 instanceof IRI) {
                    final int rank1 = this.rankedNamespaces.indexOf(((IRI) v1).getNamespace());
                    final int rank2 = this.rankedNamespaces.indexOf(((IRI) v2).getNamespace());
                    if (rank1 >= 0 && (rank1 < rank2 || rank2 < 0)) {
                        return -1;
                    } else if (rank2 >= 0 && (rank2 < rank1 || rank1 < 0)) {
                        return 1;
                    }
                    final String string1 = Statements.formatValue(v1, Namespaces.DEFAULT);
                    final String string2 = Statements.formatValue(v2, Namespaces.DEFAULT);
                    return string1.compareTo(string2);
                } else {
                    return -1;
                }
            } else if (v1 instanceof BNode) {
                if (v2 instanceof BNode) {
                    return ((BNode) v1).getID().compareTo(((BNode) v2).getID());
                } else if (v2 instanceof IRI) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (v1 instanceof Literal) {
                if (v2 instanceof Literal) {
                    return ((Literal) v1).getLabel().compareTo(((Literal) v2).getLabel());
                } else if (v2 instanceof Resource) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                if (v1 == v2) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }

    }

    private static final class StatementOrdering extends Ordering<Statement> {

        private final String components;

        private final Comparator<? super Value> valueComparator;

        public StatementOrdering(final String components,
                                 final Comparator<? super Value> valueComparator) {
            this.components = components.trim().toLowerCase();
            this.valueComparator = Preconditions.checkNotNull(valueComparator);
            for (int i = 0; i < this.components.length(); ++i) {
                final char c = this.components.charAt(i);
                if (c != 's' && c != 'p' && c != 'o' && c != 'c') {
                    throw new IllegalArgumentException("Invalid components: " + components);
                }
            }
        }

        @Override
        public int compare(final Statement s1, final Statement s2) {
            for (int i = 0; i < this.components.length(); ++i) {
                final char c = this.components.charAt(i);
                final Value v1 = getValue(s1, c);
                final Value v2 = getValue(s2, c);
                final int result = this.valueComparator.compare(v1, v2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }

        private Value getValue(final Statement statement, final char component) {
            switch (component) {
                case 's':
                    return statement.getSubject();
                case 'p':
                    return statement.getPredicate();
                case 'o':
                    return statement.getObject();
                case 'c':
                    return statement.getContext();
                default:
                    throw new Error();
            }
        }

    }
}
