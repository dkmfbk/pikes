package eu.fbk.dkm.pikes.eval;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.rio.RDFHandler;
import org.slf4j.LoggerFactory;

import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import eu.fbk.utils.vocab.NIF;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

public class Converter {

    private static final Set<String> AM_ROLES = ImmutableSet.of("dir", "loc", "mnr", "ext", "rec",
            "prd", "pnc", "cau", "dis", "adv", "mod", "neg");

    private static final URI DUL_ASSOCIATED_WITH = Statements.VALUE_FACTORY
            .createURI("http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#associatedWith");

    public static final Converter FRED_CONVERTER = new Converter(
            "fred",
            "" //
                    + "SELECT ?uri (REPLACE(?t, '_', ' ') AS ?text)\n" //
                    + "WHERE { ?uri a nif:Context ; nif:isString ?t . }\n",
            "" //
                    + "PREFIX fsem: <http://ontologydesignpatterns.org/cp/owl/semiotics.owl#>\n"
                    + "PREFIX eval: <http://pikes.fbk.eu/ontologies/eval#>\n"
                    + "SELECT ?node ?begin ?end ?head\n"
                    + "WHERE {\n"
                    + "  ?m fsem:denotes|fsem:hasInterpretant ?node ;\n"
                    + "  nif:beginIndex ?begin ;\n"
                    + "  nif:endIndex ?end ;\n"
                    + "  OPTIONAL { ?m eval:head ?head }\n"
                    + "  FILTER EXISTS { ?node ?p ?o }\n"
                    + "  FILTER NOT EXISTS { ?s ?node ?o }\n"
                    + "  FILTER NOT EXISTS { ?s a ?node }\n"
                    + "  FILTER NOT EXISTS { ?node a owl:Class }\n" //
                    + "}\n" //
                    + "ORDER BY ?m", //
            (final URI uri) -> {
                String ns = uri.getNamespace();
                String name = uri.getLocalName();
                if (ns.equals("http://www.ontologydesignpatterns.org/ont/vn/abox/role/")
                        || ns.equals("http://www.ontologydesignpatterns.org/ont/boxer/boxer.owl#")
                        && (name.equals("agent") || name.equals("patient") || name.equals("theme"))) {
                    ns = "http://pikes.fbk.eu/ontologies/verbnet#";
                    name = name.toLowerCase();
                } else if (ns.equals("http://www.ontologydesignpatterns.org/ont/vn/data/")) {
                    ns = "http://pikes.fbk.eu/ontologies/verbnet#";
                    final String code = name.substring(name.lastIndexOf('_') + 1);
                    final int l = code.length();
                    final int n1 = l < 2 ? 0 : Integer.parseInt(code.substring(0, 2));
                    final int n2 = l < 4 ? 0 : Integer.parseInt(code.substring(2, 4));
                    final int n3 = l < 5 ? 0 : Character.digit(code.charAt(4), 10);
                    final int n4 = l < 6 ? 0 : Character.digit(code.charAt(5), 10);
                    final int n5 = l < 7 ? 0 : Character.digit(code.charAt(6), 10);
                    final int n6 = l < 8 ? 0 : Character.digit(code.charAt(7), 10);
                    final StringBuilder b = new StringBuilder().append(n1);
                    assert n1 >= 0 && n2 >= 0 && n3 >= 0 && n4 >= 0 && n5 >= 0 && n6 >= 0;
                    if (n2 != 0) {
                        b.append('.').append(n2);
                        if (n3 != 0) {
                            b.append('.').append(n3);
                        }
                    }
                    if (n4 != 0) {
                        b.append('-').append(n4);
                        if (n5 != 0) {
                            b.append('-').append(n5);
                            if (n6 != 0) {
                                b.append('-').append(n6);
                            }
                        }
                    }
                    name = b.toString();
                }
                return Statements.VALUE_FACTORY.createURI(ns, name);
            }, "PREFIX fsem: <http://ontologydesignpatterns.org/cp/owl/semiotics.owl#>\n"
                    + "SELECT ?s (owl:sameAs AS ?p) ?o\n "
                    + "WHERE { ?s fsem:denotes ?o. FILTER EXISTS { ?m fsem:denotes ?s } }");

    public static final Converter GOLD_CONVERTER = new Converter("gold", "" //
            + "SELECT ?uri ?text\n" //
            + "WHERE { ?uri rdfs:label ?text . }\n", "" //
            + "PREFIX fsem: <http://ontologydesignpatterns.org/cp/owl/semiotics.owl#>\n"
            + "PREFIX eval: <http://pikes.fbk.eu/ontologies/eval#>\n"
            + "SELECT DISTINCT ?node (?node AS ?head)\n" //
            + "WHERE {\n"
            + "  { ?node a eval:Node } UNION\n"
            + "  { ?node a eval:Entity } UNION\n"
            + "  { ?node a eval:Frame } UNION\n"
            + "  { ?node a eval:Quality }\n" //
            + "}\n" //
            + "ORDER BY ?m", //
            (final URI uri) -> {
                final String ns = uri.getNamespace();
                String name = uri.getLocalName();
                if (ns.equals("http://pikes.fbk.eu/ontologies/verbnet#")) {
                    final int index = name.indexOf('-');
                    if (index > 0) {
                        name = name.substring(index + 1);
                    }
                }
                return Statements.VALUE_FACTORY.createURI(ns, name);
            });

    public static final Converter PIKES_CONVERTER = new Converter("pikes", "" //
            + "PREFIX eval: <http://pikes.fbk.eu/ontologies/eval#>\n" //
            + "SELECT ?uri ?text\n" //
            + "WHERE { ?uri a eval:Sentence ; rdfs:label ?text . }\n", "" //
            + "PREFIX gaf: <http://groundedannotationframework.org/gaf#>\n"
            + "PREFIX eval: <http://pikes.fbk.eu/ontologies/eval#>\n"
            + "SELECT ?node ?begin ?end ?head (?m AS ?sentence)\n" //
            + "WHERE {\n" //
            + "  ?node gaf:denotedBy ?m .\n"
            + "  ?m nif:beginIndex ?begin ;\n"
            + "     nif:endIndex ?end ;\n" //
            + "  OPTIONAL { ?m eval:head ?head }\n" + "}\n" //
            + "ORDER BY ?m", //
            (final URI uri) -> {
                String ns = uri.getNamespace();
                String name = uri.getLocalName();
                boolean rewriteName = false;
                if (ns.equals("http://www.newsreader-project.eu/ontologies/propbank/")) {
                    ns = "http://pikes.fbk.eu/ontologies/propbank#";
                    rewriteName = true;
                } else if (ns.equals("http://www.newsreader-project.eu/ontologies/nombank/")) {
                    ns = "http://pikes.fbk.eu/ontologies/nombank#";
                    rewriteName = true;
                } else if (ns.equals("http://www.newsreader-project.eu/ontologies/verbnet/")) {
                    ns = "http://pikes.fbk.eu/ontologies/verbnet#";
                    final int index = name.indexOf('-');
                    if (index > 0) {
                        name = name.substring(index + 1);
                    }
                } else if (ns.equals("http://www.newsreader-project.eu/ontologies/framenet/")) {
                    ns = "http://pikes.fbk.eu/ontologies/framenet#";
                } else if (ns.equals("http://dkm.fbk.eu/ontologies/knowledgestore#")
                        && name.equals("mod")) {
                    ns = "http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#";
                    name = "associatedWith";
                }
                if (rewriteName) {
                    if (AM_ROLES.contains(name.toLowerCase())) {
                        name = "am-" + name.toLowerCase();
                    } else if (name.endsWith("_0") || name.endsWith("_1") || name.endsWith("_2")
                            || name.endsWith("_3") || name.endsWith("_4") || name.endsWith("_5")) {
                        name = "a" + name.charAt(name.length() - 1);
                    }
                }
                return Statements.VALUE_FACTORY.createURI(ns, name);
            });

    private static final Set<URI> IGNORABLE_TERMS = ImmutableSet.of( //
            new URIImpl("http://www.newsreader-project.eu/ontologies/propbank/adv"), //
            new URIImpl("http://www.newsreader-project.eu/ontologies/nombank/adv"), //
            new URIImpl("http://groundedannotationframework.org/gaf#denotedBy"), //
            new URIImpl("http://www.ontologydesignpatterns.org/ont/fred/pos.owl#boxerpos"), //
            new URIImpl("http://ontologydesignpatterns.org/cp/owl/semiotics.owl#denotes"), //
            new URIImpl("http://ontologydesignpatterns.org/cp/owl/semiotics.owl#hasInterpretant"), //
            NIF.OFFSET_BASED_STRING, NIF.BEGIN_INDEX, NIF.END_INDEX, NIF.REFERENCE_CONTEXT);

    private final String creator;

    private final TupleExpr textQuery;

    private final TupleExpr nodeQuery;

    private final Function<URI, URI> uriRewriter;

    private final TupleExpr[] expandQueries;

    public Converter(final String creator, final String textQuery, final String nodeQuery,
            @Nullable final Function<URI, URI> uriRewriter, final String... expandQueries) {
        this.creator = Objects.requireNonNull(creator);
        this.textQuery = Util.parse(textQuery);
        this.nodeQuery = Util.parse(nodeQuery);
        this.uriRewriter = uriRewriter;
        this.expandQueries = new TupleExpr[expandQueries.length];
        for (int i = 0; i < expandQueries.length; ++i) {
            this.expandQueries[i] = Util.parse(expandQueries[i]);
        }

    }

    public QuadModel convert(final QuadModel model) throws Throwable {

        final ValueFactory vf = Statements.VALUE_FACTORY;
        final QuadModel result = QuadModel.create();

        final Map<URI, Sentence> sentences = new HashMap<>();
        for (final BindingSet binding : Util.query(model, this.textQuery)) {
            final URI uri = vf.createURI(((URI) binding.getValue("uri")).getNamespace());
            final String text = binding.getValue("text").stringValue().trim();
            sentences.put(uri, new Sentence(text));
        }

        final Map<Value, URI> nodeSentences = Maps.newHashMap();
        final Multimap<Value, String> nodeTerms = HashMultimap.create();
        for (final BindingSet binding : Util.query(model, this.nodeQuery)) {
            final URI node = (URI) binding.getValue("node");
            final URI head = (URI) binding.getValue("head");
            URI sentenceURI = (URI) binding.getValue("sentence");
            sentenceURI = sentenceURI != null ? vf.createURI(sentenceURI.getNamespace()) : vf
                    .createURI(node.getNamespace());
            final Sentence sentence = sentences.get(sentenceURI);
            final String term;
            if (head != null) {
                term = sentence.getTerm(head.getLocalName());
            } else {
                final int begin = ((Literal) binding.getValue("begin")).intValue();
                final int end = ((Literal) binding.getValue("end")).intValue();
                term = sentence.getTerm(begin, end);
            }
            nodeTerms.put(node, term);
            nodeSentences.put(node, sentenceURI);
        }

        final Set<Statement> splittingStmts = Sets.newHashSet();
        for (final Statement stmt : model) {
            if (EVAL.METADATA.equals(stmt.getContext())) {
                splittingStmts.add(stmt);
            }
        }

        for (final Map.Entry<URI, Sentence> entry : sentences.entrySet()) {
            final URI sentenceURI = entry.getKey();
            final URI graphURI = vf.createURI(sentenceURI + "graph_" + this.creator);
            result.add(sentenceURI, RDF.TYPE, EVAL.SENTENCE, EVAL.METADATA);
            result.add(sentenceURI, RDFS.LABEL, vf.createLiteral(entry.getValue().getText()),
                    EVAL.METADATA);
            result.add(graphURI, RDF.TYPE, EVAL.KNOWLEDGE_GRAPH, EVAL.METADATA);
            result.add(graphURI, DCTERMS.SOURCE, sentenceURI, EVAL.METADATA);
            result.add(graphURI, DCTERMS.CREATOR, vf.createLiteral(this.creator), EVAL.METADATA);
        }

        for (final Value node : nodeTerms.keySet()) {
            final URI sentenceURI = nodeSentences.get(node);
            final URI graphURI = vf.createURI(sentenceURI + "graph_" + this.creator);
            final Collection<String> terms = nodeTerms.get(node);
            for (final String term : terms) {
                final URI termURI = vf.createURI(sentenceURI + "term_" + term);
                final URI nodeURI = terms.size() == 1 ? (URI) node : vf.createURI(node + "_"
                        + term);
                result.add(nodeURI, RDF.TYPE, EVAL.NODE, graphURI);
                result.add(nodeURI, EVAL.DENOTED_BY, termURI, graphURI);
            }
        }

        final Set<Statement> expanded = Sets.newHashSet();
        for (final TupleExpr expandQuery : this.expandQueries) {
            for (final BindingSet bindings : Util.query(model, expandQuery)) {
                final Value s = bindings.getValue("s");
                final Value p = bindings.getValue("p");
                final Value o = bindings.getValue("o");
                if (s instanceof Resource && p instanceof URI && o instanceof Value) {
                    expanded.add(vf.createStatement((Resource) s, (URI) p, o));
                }
            }
        }

        for (final Statement stmt : Iterables.concat(model, expanded)) {
            URI pred = stmt.getPredicate();
            Value obj = stmt.getObject();
            if (EVAL.METADATA.equals(stmt.getContext())) {
                continue;
            }
            final Resource subj = stmt.getSubject();
            if (IGNORABLE_TERMS.contains(pred) || pred.equals(RDF.TYPE)
                    && IGNORABLE_TERMS.contains(obj)) {
                continue;
            }
            if (this.uriRewriter != null) {
                pred = this.uriRewriter.apply(pred);
                if (pred.equals(RDF.TYPE) && obj instanceof URI) {
                    obj = this.uriRewriter.apply((URI) obj);
                }
            }
            final Collection<String> subjTerms = nodeTerms.get(subj);
            if (!subjTerms.isEmpty()) {
                final URI sentenceURI = nodeSentences.get(subj);
                final URI graphURI = vf.createURI(sentenceURI + "graph_" + this.creator);
                final List<Value> subjURIs = split(subj, subjTerms);
                final List<Value> objValues = split(obj, nodeTerms.get(obj));
                corefer(result, graphURI, subjURIs);
                corefer(result, graphURI, objValues);
                boolean added = false;
                final boolean splitting = subjURIs.size() > 1 || objValues.size() > 1;
                for (final Value subjURI : subjURIs) {
                    for (final Value objValue : objValues) {
                        final Statement s = vf.createStatement((URI) subjURI, pred, objValue,
                                graphURI);
                        if (!splitting || splittingStmts.contains(s)) {
                            result.add(s);
                            added = true;
                        }
                    }
                }
                if (!added) {
                    throw new IllegalArgumentException("Could not split statement: "
                            + vf.createStatement(subj, pred, obj, stmt.getContext()) + "\nsubj: "
                            + subjURIs + "\nobj: " + objValues);
                }
            }
        }

        return result;
    }

    public static void replaceNominalFrames(final QuadModel model) {

        for (final Resource graphID : model.contexts()) {

            final Map<URI, URI> terms = Maps.newHashMap();
            for (final Statement stmt : model.filter(null, EVAL.DENOTED_BY, null, graphID)) {
                terms.put((URI) stmt.getSubject(), (URI) stmt.getObject());
            }

            final Set<URI> allPreds = Sets.newHashSet();
            final Set<URI> nbPreds = Sets.newHashSet();
            final Set<URI> pbPreds = Sets.newHashSet();
            for (final Statement stmt : model.filter(null, RDF.TYPE, null, graphID)) {
                if (stmt.getObject() instanceof URI) {
                    final String ns = ((URI) stmt.getObject()).getNamespace();
                    if (isFrameNS(ns)) {
                        final URI pred = (URI) stmt.getSubject();
                        allPreds.add(pred);
                        if (ns.equals("http://pikes.fbk.eu/ontologies/propbank#")) {
                            pbPreds.add(pred);
                        }
                        if (ns.equals("http://pikes.fbk.eu/ontologies/nombank#")) {
                            nbPreds.add(pred);
                        }
                    }
                }
            }
            final Set<URI> nomPreds = Sets.newHashSet();
            nomPreds.addAll(nbPreds);
            nomPreds.addAll(Sets.difference(allPreds, pbPreds));

            for (final URI pred : nomPreds) {
                final URI predTerm = terms.get(pred);
                final List<Statement> stmts = Lists.newArrayList(model.filter(pred, null, null,
                        graphID));
                URI newSubj = pred;
                for (final Statement stmt : stmts) {
                    final URI argTerm = terms.get(stmt.getObject());
                    if (predTerm.equals(argTerm)) {
                        newSubj = (URI) stmt.getObject();
                        break;
                    }
                }
                for (final Statement stmt : stmts) {
                    final boolean isFrameRole = isFrameNS(stmt.getPredicate().getNamespace());
                    final boolean isFrameType = !isFrameRole && stmt.getObject() instanceof URI
                            && isFrameNS(((URI) stmt.getObject()).getNamespace());
                    if (isFrameRole && !newSubj.equals(stmt.getObject())) {
                        model.add(newSubj, DUL_ASSOCIATED_WITH, stmt.getObject(), graphID);
                    }
                    if (isFrameRole || isFrameType || newSubj != pred) {
                        model.remove(stmt);
                    }
                }
            }
        }
    }

    private static boolean isFrameNS(final String ns) {
        return ns.equals("http://pikes.fbk.eu/ontologies/propbank#")
                || ns.equals("http://pikes.fbk.eu/ontologies/nombank#")
                || ns.equals("http://pikes.fbk.eu/ontologies/verbnet#")
                || ns.equals("http://pikes.fbk.eu/ontologies/framenet#");
    }

    private static List<Value> split(final Value value, final Collection<String> terms) {
        if (terms.size() <= 1) {
            return ImmutableList.of(value);
        } else {
            final List<Value> values = Lists.newArrayListWithCapacity(terms.size());
            for (final String term : terms) {
                values.add(Statements.VALUE_FACTORY.createURI(value + "_" + term));
            }
            return ImmutableList.copyOf(values);
        }
    }

    private static void corefer(final QuadModel model, final Resource graph,
            @Nullable final Collection<Value> values) {
        if (values != null && values.size() > 1) {
            for (final Value value1 : values) {
                for (final Value value2 : values) {
                    if (Util.VALUE_ORDERING.compare(value1, value2) < 0) {
                        model.add((Resource) value1, OWL.SAMEAS, (Resource) value2, graph);
                    }
                }
            }
        }
    }

    public static void main(final String... args) {

        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("eval-converter")
                    .withHeader("Convert a tool output in the format used for the evaluation.")
                    .withOption("o", "output", "the output file", "FILE", Type.STRING, true,
                            false, true)
                    .withOption("f", "format", "the format (fred, pikes, gold)", "FMT",
                            Type.STRING, true, false, true)
                    .withOption("n", "replace-nominal",
                            "replaces nominal frames with association " //
                                    + " relations (for FRED compatibility)")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            // Extract options
            final String format = cmd.getOptionValue("f", String.class).trim().toLowerCase();
            final String outputFile = cmd.getOptionValue("o", String.class);
            final List<String> inputFiles = cmd.getArgs(String.class);
            final boolean replaceNominalFrames = cmd.hasOption("n");

            // Obtain the converter corresponding to the format specified
            Converter converter;
            if (format.equalsIgnoreCase("fred")) {
                converter = FRED_CONVERTER;
            } else if (format.equalsIgnoreCase("gold")) {
                converter = GOLD_CONVERTER;
            } else if (format.equalsIgnoreCase("pikes")) {
                converter = PIKES_CONVERTER;
            } else {
                throw new IllegalArgumentException("Unknown format: " + format);
            }

            // Read the input
            final Map<String, String> namespaces = Maps.newHashMap();
            final QuadModel input = QuadModel.create();
            RDFSources.read(false, false, null, null,
                    inputFiles.toArray(new String[inputFiles.size()])).emit(
                    RDFHandlers.wrap(input, namespaces), 1);

            // Perform the conversion
            final QuadModel output = converter.convert(input);

            // Replace nominal frames if requested
            if (replaceNominalFrames) {
                replaceNominalFrames(output);
            }

            // Write the output
            final RDFHandler out = RDFHandlers.write(null, 1000, outputFile);
            out.startRDF();
            namespaces.put(DCTERMS.PREFIX, DCTERMS.NAMESPACE);
            namespaces.put("pb", "http://pikes.fbk.eu/ontologies/propbank#");
            namespaces.put("nb", "http://pikes.fbk.eu/ontologies/nombank#");
            namespaces.put("vn", "http://pikes.fbk.eu/ontologies/verbnet#");
            namespaces.put("fn", "http://pikes.fbk.eu/ontologies/framenet#");
            namespaces.put("dul", "http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#");
            final Set<String> outputNS = Sets.newHashSet();
            collectNS(outputNS, output);
            for (final Map.Entry<String, String> entry : namespaces.entrySet()) {
                if (!entry.getKey().isEmpty() && outputNS.contains(entry.getValue())) {
                    out.handleNamespace(entry.getKey(), entry.getValue());
                }
            }
            for (final Statement stmt : Ordering.from(
                    Statements.statementComparator("cspo",
                            Statements.valueComparator(RDF.NAMESPACE))).sortedCopy(output)) {
                out.handleStatement(stmt);
            }
            out.endRDF();

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

    private static void collectNS(final Collection<String> ns, final Iterable<Statement> stmts) {
        for (final Statement stmt : stmts) {
            collectNS(ns, stmt.getSubject());
            collectNS(ns, stmt.getPredicate());
            collectNS(ns, stmt.getObject());
            collectNS(ns, stmt.getContext());
        }
    }

    private static void collectNS(final Collection<String> ns, @Nullable final Value value) {
        if (value instanceof URI) {
            ns.add(((URI) value).getNamespace());
        }
    }

    private static class Sentence {

        private final String text;

        private final int[] beginIndexes;

        private final int[] endIndexes;

        private final List<String> termList;

        private final Set<String> termSet;

        public Sentence(final String text) {

            final int[] begins = new int[text.length()];
            final int[] ends = new int[text.length()];
            final List<String> termList = Lists.newArrayList();
            final Set<String> termSet = Sets.newHashSet();
            int count = 0;

            final Set<String> ambiguousTerms = Sets.newHashSet();
            boolean insideTerm = false;
            for (int i = 0; i < text.length(); ++i) {
                final char ch = text.charAt(i);
                final boolean letter = Character.isLetter(ch) || ch == '-' || ch == '_';
                if (letter && !insideTerm) {
                    begins[count] = i;
                    insideTerm = true;
                } else if (!letter && insideTerm) {
                    ends[count] = i;
                    final String term = text.substring(begins[count], ends[count]);
                    termList.add(term);
                    if (!termSet.add(term)) {
                        ambiguousTerms.add(term);
                    }
                    ++count;
                    insideTerm = false;
                }
            }

            for (final String term : ambiguousTerms) {
                int index = 0;
                termSet.remove(term);
                for (int i = 0; i < termList.size(); ++i) {
                    if (termList.get(i).equals(term)) {
                        final String t = term + "_" + (++index);
                        termList.set(i, t);
                        termSet.add(t);
                    }
                }
            }

            this.text = text;
            this.beginIndexes = Arrays.copyOfRange(begins, 0, count);
            this.endIndexes = Arrays.copyOfRange(ends, 0, count);
            this.termList = termList;
            this.termSet = termSet;
        }

        public String getText() {
            return this.text;
        }

        public String getTerm(final String localName) {
            int index = localName.length();
            while (true) {
                final String candidate = localName.substring(0, index);
                for (final String term : this.termList) {
                    if (candidate.equalsIgnoreCase(term)) {
                        return term;
                    }
                }
                index = localName.lastIndexOf('_', index);
                if (index < 0) {
                    throw new IllegalArgumentException("Cannot map " + localName
                            + " to a term\nterms: " + this.termSet);
                }
            }
        }

        public String getTerm(final int beginIndex, final int endIndex) {
            final List<String> matches = Lists.newArrayList();
            for (int i = 0; i < this.beginIndexes.length; ++i) {
                if (beginIndex < this.endIndexes[i] && endIndex > this.beginIndexes[i]) {
                    matches.add(this.termList.get(i));
                }
            }
            if (matches.size() == 0) {
                throw new IllegalArgumentException("No term matching indexes " + beginIndex + ", "
                        + endIndex);
            }
            if (matches.size() > 1) {
                throw new IllegalArgumentException("Multiple terms matching indexes " + beginIndex
                        + ", " + endIndex + "\ntext: " + this.text + "\nbegins: "
                        + Arrays.toString(this.beginIndexes) + "\nends: "
                        + Arrays.toString(this.endIndexes));
            }
            return matches.get(0);
        }

    }

}
