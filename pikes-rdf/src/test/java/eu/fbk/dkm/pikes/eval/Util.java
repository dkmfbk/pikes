package eu.fbk.dkm.pikes.eval;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.TupleExpr;

import eu.fbk.dkm.utils.eval.PrecisionRecall;
import eu.fbk.rdfpro.util.Algebra;
import eu.fbk.rdfpro.util.Namespaces;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

final class Util {

    public static final Ordering<Value> VALUE_ORDERING = Ordering.from(
            Statements.valueComparator()).nullsLast();

    public static final Ordering<Statement> STMT_ORDERING = Ordering.from(Statements
            .statementComparator("spoc", VALUE_ORDERING));

    public static final Namespaces NAMESPACES;

    static {
        final Map<String, String> map = Maps.newHashMap(Namespaces.DEFAULT.uriMap());
        map.put("pb", "http://pikes.fbk.eu/ontologies/propbank#");
        map.put("nb", "http://pikes.fbk.eu/ontologies/nombank#");
        map.put("vn", "http://pikes.fbk.eu/ontologies/verbnet#");
        map.put("fn", "http://pikes.fbk.eu/ontologies/framenet#");
        map.put("dul", "http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#");
        map.put("wni", "http://www.w3.org/2006/03/wn/wn30/instances/");
        map.put("fbox", "http://www.ontologydesignpatterns.org/ont/boxer/boxer.owl#");
        map.put("fpos", "http://www.ontologydesignpatterns.org/ont/fred/pos.owl#");
        map.put("fboxing", "http://www.ontologydesignpatterns.org/ont/boxer/boxing.owl#");
        map.put("d0", "http://www.ontologydesignpatterns.org/ont/d0.owl#");
        map.put("wn30", "http://wordnet-rdf.princeton.edu/wn30/");
        map.put("nwronto", "http://www.newsreader-project.eu/ontologies/");
        NAMESPACES = Namespaces.forURIMap(map);
    }

    public static TupleExpr parse(final String query) {
        try {
            return Algebra.parseQuery(query, null, Namespaces.DEFAULT.uriMap()).getTupleExpr();
        } catch (final MalformedQueryException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static FluentIterable<BindingSet> query(final QuadModel model, final TupleExpr query) {
        return new FluentIterable<BindingSet>() {

            @Override
            public Iterator<BindingSet> iterator() {
                return model.evaluate(query, null, null);
            }

        };
    }

    public static String format(@Nullable final URI baseURI, final Object... objects) {
        String result = "";
        for (final Object object : objects) {
            String s = "";
            if (object instanceof Value) {
                final Value value = (Value) object;
                final boolean abbreviate = value instanceof URI && baseURI != null
                        && ((URI) value).getNamespace().equals(baseURI.stringValue());
                s = abbreviate ? ":" + ((URI) value).getLocalName() : Statements.formatValue(
                        value, Util.NAMESPACES);
            } else if (object instanceof Relation) {
                s = ((Relation) object).toString(baseURI);
            } else if (object instanceof Statement) {
                final Statement stmt = (Statement) object;
                return format(baseURI, stmt.getSubject()) + " "
                        + format(baseURI, stmt.getPredicate()) + " "
                        + format(baseURI, stmt.getObject());
            } else if (object instanceof PrecisionRecall) {
                final PrecisionRecall pr = (PrecisionRecall) object;
                return String.format("f1=%5.3f p=%5.3f r=%5.3f tp=%d fp=%d fn=%d", pr.getF1(),
                        pr.getPrecision(), pr.getRecall(), (int) pr.getTP(), (int) pr.getFP(),
                        (int) pr.getFN());
            } else if (object != null) {
                s = object.toString();
            }
            result = result.isEmpty() ? s : result + ", " + s;
        }
        return result;
    }

}
