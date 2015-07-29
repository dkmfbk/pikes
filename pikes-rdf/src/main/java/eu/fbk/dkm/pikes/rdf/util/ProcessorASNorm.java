package eu.fbk.dkm.pikes.rdf.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import eu.fbk.dkm.utils.Util;
import eu.fbk.rdfpro.Mapper;
import eu.fbk.rdfpro.RDFProcessor;
import eu.fbk.rdfpro.RDFProcessors;
import eu.fbk.rdfpro.Reducer;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Options;
import eu.fbk.rdfpro.util.Statements;

public final class ProcessorASNorm implements RDFProcessor {

    private final String namespace;

    private final Mapper checkedMapper;

    private final Mapper uncheckedMapper;

    private final Reducer factReducer;

    private final Reducer metaReducer;

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
        return value instanceof URI && ((URI) value).getNamespace().equals(this.namespace);
    }

    private URI hash(final Resource subject, final URI predicate, final Value object) {
        final List<String> list = Lists.newArrayList();
        for (final Value value : new Value[] { subject, predicate, object }) {
            if (value instanceof URI) {
                list.add("\u0001");
                list.add(value.stringValue());
            } else if (value instanceof BNode) {
                list.add("\u0002");
                list.add(((BNode) value).getID());
            } else if (value instanceof Literal) {
                final Literal l = (Literal) value;
                list.add("\u0003");
                list.add(l.getLabel());
                if (l.getDatatype() != null) {
                    list.add(l.getDatatype().stringValue());
                } else if (l.getLanguage() != null) {
                    list.add(l.getLanguage());
                }
            }
        }
        final String hash = Hash.murmur3(list.toArray(new String[list.size()])).toString();
        return Statements.VALUE_FACTORY.createURI(this.namespace, hash);
    }

    private URI hash(final URI id, final Iterable<Statement> statements) {
        final List<String> list = Lists.newArrayList();
        for (final Statement stmt : statements) {
            for (final Value value : new Value[] { stmt.getSubject(), stmt.getPredicate(),
                    stmt.getObject(), stmt.getContext() }) {
                if (value == null) {
                    list.add("\u0004");
                } else if (value.equals(id)) {
                    list.add("\u0005");
                } else if (value instanceof URI) {
                    list.add("\u0001");
                    list.add(value.stringValue());
                } else if (value instanceof BNode) {
                    list.add("\u0002");
                    list.add(((BNode) value).getID());
                } else if (value instanceof Literal) {
                    final Literal l = (Literal) value;
                    list.add("\u0003");
                    list.add(l.getLabel());
                    if (l.getDatatype() != null) {
                        list.add(l.getDatatype().stringValue());
                    } else if (l.getLanguage() != null) {
                        list.add(l.getLanguage());
                    }
                }
            }
        }
        final String hash = Hash.murmur3(list.toArray(new String[list.size()])).toString();
        return Statements.VALUE_FACTORY.createURI(this.namespace, hash);
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

    private void emit(final RDFHandler handler, final Value oldID, final URI newID,
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
                final URI metaPred = replace(metaStmt.getPredicate(), oldID, newID);
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
                final URI newID = hash(factStmt.getSubject(), factStmt.getPredicate(),
                        factStmt.getObject());
                emit(handler, id, newID, factStmt, metaStmts);
            }
        }

    }

    private final class MetaReducer implements Reducer {

        private final Comparator<Statement> comparator = Util.statementOrdering("spoc",
                Util.valueOrdering(ProcessorASNorm.this.namespace));

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
            final URI metadataID = hash((URI) key, metaStmts);
            emit(handler, key, metadataID, factStmt, metaStmts);
        }

    }

}
