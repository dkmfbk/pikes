package eu.fbk.dkm.pikes.eval;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.html.HtmlEscapers;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.TupleExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.eval.PrecisionRecall;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

public final class Evaluation {

    private static final Logger LOGGER = LoggerFactory.getLogger(Evaluation.class);

    private final Stats nodeStats;

    private final Stats unlabelledStats;

    private final Stats labelledStats;

    private final Map<String, Stats> labelledStatsByNS;

    private final Stats typeStats;

    private final Map<String, Stats> typeStatsByNS;

    private final Stats linkingStats;

    private final Stats triplesStats;

    private final String report;

    private Evaluation(final Stats nodeStats, final Stats unlabelledStats,
            final Stats labelledStats, final Map<String, Stats> labelledStatsByNS,
            final Stats typeStats, final Map<String, Stats> typeStatsByNS,
            final Stats linkingStats, final Stats triplesStats, final String report) {
        this.nodeStats = Objects.requireNonNull(nodeStats);
        this.unlabelledStats = Objects.requireNonNull(unlabelledStats);
        this.labelledStats = Objects.requireNonNull(labelledStats);
        this.labelledStatsByNS = ImmutableMap.copyOf(labelledStatsByNS);
        this.typeStats = Objects.requireNonNull(typeStats);
        this.typeStatsByNS = ImmutableMap.copyOf(typeStatsByNS);
        this.linkingStats = Objects.requireNonNull(linkingStats);
        this.triplesStats = Objects.requireNonNull(triplesStats);
        this.report = Objects.requireNonNull(report);
    }

    public static Evaluation evaluate(final Iterable<Statement> model, final boolean simplified) {
        return new Evaluator(model, simplified).get();
    }

    public Stats getNodeStats() {
        return this.nodeStats;
    }

    public Stats getUnlabelledStats() {
        return this.unlabelledStats;
    }

    public Stats getLabelledStats() {
        return this.labelledStats;
    }

    public Map<String, Stats> getLabelledStatsByNS() {
        return this.labelledStatsByNS;
    }

    public Stats getTypeStats() {
        return this.typeStats;
    }

    public Map<String, Stats> getTypeStatsByNS() {
        return this.typeStatsByNS;
    }

    public Stats getLinkingStats() {
        return this.linkingStats;
    }

    public Stats getTriplesStats() {
        return this.triplesStats;
    }

    public String getReport() {
        return this.report;
    }

    public static final class Stats {

        private final Map<String, PrecisionRecall> goldPRs;

        private final Map<String, PrecisionRecall> unionPRs;

        @Nullable
        private final String report;

        Stats(final Map<String, PrecisionRecall> goldPRs,
                final Map<String, PrecisionRecall> unionPRs, final String report) {
            this.goldPRs = goldPRs;
            this.unionPRs = unionPRs;
            this.report = report;
        }

        public static Stats aggregate(final Iterable<Stats> sources) {
            final Map<String, PrecisionRecall.Evaluator> goldEvaluators = Maps.newHashMap();
            final Map<String, PrecisionRecall.Evaluator> unionEvaluators = Maps.newHashMap();
            for (final Stats source : sources) {
                updateHelper(source.goldPRs, goldEvaluators);
                updateHelper(source.unionPRs, unionEvaluators);
            }
            final Map<String, PrecisionRecall> goldPRs = Maps.newHashMap();
            final Map<String, PrecisionRecall> unionPRs = Maps.newHashMap();
            for (final Map.Entry<String, PrecisionRecall.Evaluator> entry : goldEvaluators
                    .entrySet()) {
                goldPRs.put(entry.getKey(), entry.getValue().getResult());
            }
            for (final Map.Entry<String, PrecisionRecall.Evaluator> entry : unionEvaluators
                    .entrySet()) {
                unionPRs.put(entry.getKey(), entry.getValue().getResult());
            }
            return new Stats(goldPRs, unionPRs, null);
        }

        private static void updateHelper(final Map<String, PrecisionRecall> prs,
                final Map<String, PrecisionRecall.Evaluator> evaluators) {
            for (final Map.Entry<String, PrecisionRecall> entry : prs.entrySet()) {
                final String system = entry.getKey();
                final PrecisionRecall pr = entry.getValue();
                if (pr != null) {
                    PrecisionRecall.Evaluator evaluator = evaluators.get(system);
                    if (evaluator == null) {
                        evaluator = PrecisionRecall.evaluator();
                        evaluators.put(system, evaluator);
                    }
                    evaluator.add(pr);
                }
            }
        }

        public List<String> getSystems() {
            return Ordering.natural().immutableSortedCopy(this.goldPRs.keySet());
        }

        public Map<String, PrecisionRecall> getGoldPRs() {
            return this.goldPRs;
        }

        public Map<String, PrecisionRecall> getUnionPRs() {
            return this.unionPRs;
        }

        @Nullable
        public String getReport() {
            return this.report;
        }

    }

    private static final class Evaluator {

        private static final TupleExpr RELATION_QUERY = Util.parse("" //
                + "PREFIX eval: <http://pikes.fbk.eu/ontologies/eval#>\n"
                + "SELECT DISTINCT ?g ?s ?o ?sm ?om\n"
                + "WHERE {\n" //
                + "  GRAPH ?g {\n" //
                + "    ?s a eval:Node .\n"
                + "    ?o a eval:Node .\n"
                + "    FILTER EXISTS {\n"
                + "      { ?s ?p ?o } UNION { ?o ?p ?s }\n"
                + "      FILTER (?p != eval:associableTo && ?p != eval:notAssociableTo &&\n"
                + "              ?p != eval:classifiableAs && ?p != eval:mappedTo)\n"
                + "    }\n"
                + "    FILTER (MD5(STR(?s)) < MD5(STR(?o)))\n"
                + "    OPTIONAL { ?s eval:mappedTo ?sm }\n"
                + "    OPTIONAL { ?o eval:mappedTo ?om }\n" //
                + "  }\n" //
                + "}\n");

        private static final TupleExpr LABELLED_QUERY = Util.parse("" //
                + "PREFIX eval: <http://pikes.fbk.eu/ontologies/eval#>\n"
                + "SELECT DISTINCT ?g ?s ?p ?o ?sm ?om\n"
                + "WHERE {\n" //
                + "  GRAPH ?g {\n" //
                + "    ?s a eval:Node .\n"
                + "    ?o a eval:Node .\n"
                + "    ?s ?p ?o .\n"
                + "    FILTER (?p != eval:associableTo && ?p != eval:notAssociableTo &&\n"
                + "            ?p != eval:classifiableAs && ?p != eval:mappedTo)\n"
                + "    FILTER (?s != ?o)\n"
                + "    OPTIONAL { ?s eval:mappedTo ?sm }\n"
                + "    OPTIONAL { ?o eval:mappedTo ?om }\n" //
                + "  }\n" //
                + "}\n");

        private static final TupleExpr ATTRIBUTE_QUERY = Util
                .parse("" //
                        + "PREFIX eval: <http://pikes.fbk.eu/ontologies/eval#>\n"
                        + "SELECT DISTINCT ?g ?s ?p ?o ?sm\n"
                        + "WHERE {\n" //
                        + "  GRAPH ?g {\n" //
                        + "    ?s a eval:Node .\n"
                        + "    ?s ?p ?o .\n" //
                        + "    FILTER NOT EXISTS { ?o a eval:Node }\n"
                        + "    FILTER (?o != eval:Node && ?p != eval:mappedTo && ?p != eval:denotedBy &&\n"
                        + "            ?p != eval:associableTo && ?p != eval:notAssociableTo &&\n"
                        + "            ?p != eval:classifiableAs)\n"
                        + "    OPTIONAL { ?s eval:mappedTo ?sm }\n" //
                        + "  }\n" //
                        + "}\n");

        private final QuadModel model;

        private final Map<URI, String> systemMap;

        private final Map<URI, URI> sentenceMap;

        private final List<String> systems;

        private final Multimap<URI, Relation> ignorableRelations;

        private final Multimap<URI, Relation> forbiddenRelations;

        private final String separator;

        private final Evaluation evaluation;

        Evaluator(final Iterable<Statement> alignedStmts, final boolean simplified) {

            this.model = alignedStmts instanceof QuadModel ? (QuadModel) alignedStmts : QuadModel
                    .create(alignedStmts);

            this.systemMap = Maps.newHashMap();
            this.sentenceMap = Maps.newHashMap();
            for (final Resource graphID : this.model.filter(null, RDF.TYPE, EVAL.KNOWLEDGE_GRAPH,
                    EVAL.METADATA).subjects()) {
                this.systemMap.put((URI) graphID,
                        this.model.filter(graphID, DCTERMS.CREATOR, null, //
                                EVAL.METADATA).objectLiteral().stringValue());
                this.sentenceMap.put((URI) graphID,
                        this.model.filter(graphID, DCTERMS.SOURCE, null, //
                                EVAL.METADATA).objectURI());
            }

            this.systems = Lists.newArrayList(Sets.newHashSet(this.systemMap.values()));
            this.systems.remove("gold");
            // this.systems.remove("fred");
            Collections.sort(this.systems);

            this.ignorableRelations = HashMultimap.create();
            this.forbiddenRelations = HashMultimap.create();
            for (final Statement stmt : Iterables.concat(
                    this.model.filter(null, EVAL.ASSOCIABLE_TO, null),
                    this.model.filter(null, EVAL.NOT_ASSOCIABLE_TO, null))) {
                final URI sentenceID = this.sentenceMap.get(stmt.getContext());
                final String system = this.systemMap.get(stmt.getContext());
                if (sentenceID != null && system.equals("gold")) {
                    final Relation relation = new Relation((URI) stmt.getSubject(),
                            (URI) stmt.getObject(), false);
                    (EVAL.ASSOCIABLE_TO.equals(stmt.getPredicate()) ? this.ignorableRelations
                            : this.forbiddenRelations).put(sentenceID, relation);
                }
            }

            this.separator = "\n" + Strings.repeat("-", 30 + 63 * (this.systems.size() + 1));

            final Stats nodeStats = nodeEvaluation();
            final Stats unlabelledStats = unlabelledEvaluation();

            final Set<String> labelledNS = simplified ? namespacesFor("vn", "owl")
                    : namespacesFor("vn", "fn", "pb", "nb", "owl");
            final Stats labelledStats = labelledEvaluation(labelledNS);
            final Map<String, Stats> labelledStatsByNS = Maps.newHashMap();
            for (final String ns : labelledNS) {
                labelledStatsByNS.put(ns, labelledEvaluation(ImmutableSet.of(ns)));
            }

            final Set<String> typeNS = simplified ? namespacesFor("vn", "fn") : namespacesFor(
                    "vn", "fn", "pb", "nb");
            final Stats typeStats = attributeEvaluation(RDF.TYPE, typeNS);
            final Map<String, Stats> typeStatsByNS = Maps.newHashMap();
            for (final String ns : typeNS) {
                typeStatsByNS.put(ns, attributeEvaluation(RDF.TYPE, ImmutableSet.of(ns)));
            }

            final Stats linkingStats = attributeEvaluation(OWL.SAMEAS, null);

            final Stats triplesStats = Stats.aggregate(ImmutableList.of(labelledStats, typeStats,
                    linkingStats));

            final StringBuilder out = new StringBuilder();
            emitSection(out, "NODES");
            out.append(nodeStats.getReport());
            emitSection(out, "UNLABELLED");
            out.append(unlabelledStats.getReport());
            emitSection(out, "LABELLED (ALL)");
            out.append(labelledStats.getReport());
            for (final String ns : labelledNS) {
                emitSection(out, "LABELLED (" + Util.NAMESPACES.prefixFor(ns).toUpperCase() + ")");
                out.append(labelledStatsByNS.get(ns).getReport());
            }
            emitSection(out, "TYPES (ALL)");
            out.append(typeStats.getReport());
            for (final String ns : typeNS) {
                emitSection(out, "TYPES (" + Util.NAMESPACES.prefixFor(ns).toUpperCase() + ")");
                out.append(typeStatsByNS.get(ns).getReport());
            }
            emitSection(out, "LINKING");
            out.append(linkingStats.getReport());
            emitSection(out, "SUMMARY");
            emitStatsHeader(out, this.systems);
            emitStats(out, nodeStats, "instances");
            emitStats(out, unlabelledStats, "unlabelled");
            emitStats(out, labelledStats, "labelled");
            for (final String ns : labelledNS) {
                emitStats(out, labelledStatsByNS.get(ns), "  " + Util.NAMESPACES.prefixFor(ns));
            }
            emitStats(out, typeStats, "types");
            for (final String ns : typeNS) {
                emitStats(out, typeStatsByNS.get(ns), "  " + Util.NAMESPACES.prefixFor(ns));
            }
            emitStats(out, linkingStats, "linking");
            emitStats(out, triplesStats, "triples");

            this.evaluation = new Evaluation(nodeStats, unlabelledStats, labelledStats,
                    labelledStatsByNS, typeStats, typeStatsByNS, linkingStats, triplesStats,
                    out.toString());
        }

        Evaluation get() {
            return this.evaluation;
        }

        private final String rowBegin = "";

        private final String rowEnd = "";

        private final String rowMid = " | ";

        // private final String rowBegin = "<tr><td>";
        //
        // private final String rowEnd = "</td></tr>";
        //
        // private final String rowMid = "</td><td>";

        private String escape(final String string) {
            return HtmlEscapers.htmlEscaper().escape(string);
        }

        private Stats nodeEvaluation() {

            final StringBuilder out = new StringBuilder();

            final Table<URI, String, List<URI>> nodesTable = HashBasedTable.create();
            for (final Statement stmt : this.model.filter(null, RDF.TYPE, EVAL.NODE)) {
                final URI sentenceID = this.sentenceMap.get(stmt.getContext());
                final String system = this.systemMap.get(stmt.getContext());
                if (sentenceID != null && system != null) {
                    getList(nodesTable, sentenceID, system).add((URI) stmt.getSubject());
                }
            }

            final Table<URI, String, Multimap<URI, URI>> alignmentTable = HashBasedTable.create();
            for (final Statement stmt : this.model.filter(null, EVAL.MAPPED_TO, null)) {
                final URI graphID = (URI) stmt.getContext();
                final URI sentenceID = this.sentenceMap.get(graphID);
                final String system = this.systemMap.get(graphID);
                if (sentenceID != null && system != null) {
                    final URI goldNode = (URI) stmt.getObject();
                    final URI testNode = (URI) stmt.getSubject();
                    getMultimap(alignmentTable, sentenceID, system).put(goldNode, testNode);
                }
            }

            final Map<String, PrecisionRecall.Evaluator> goldEvaluators = initPR();
            final Map<String, PrecisionRecall.Evaluator> unionEvaluators = initPR();
            emitHeader(out);

            String sentenceURICell = "";
            for (final URI sentenceURI : Util.VALUE_ORDERING.sortedCopy(nodesTable.rowKeySet())) {
                final Multimap<String, URI> alignedNodes = HashMultimap.create();
                sentenceURICell = Util.format(null, sentenceURI);
                out.append(this.separator);
                final List<URI> goldNodes = Util.VALUE_ORDERING.sortedCopy(nodesTable.get(
                        sentenceURI, "gold"));
                for (final URI goldNode : goldNodes) {
                    out.append(String.format("\n" + this.rowBegin + "%-30s" + this.rowMid
                            + "%-60s", escape(sentenceURICell),
                            escape(Util.format(sentenceURI, goldNode))));
                    sentenceURICell = "";
                    for (final String system : this.systems) {
                        final Multimap<URI, URI> alignments = alignmentTable.get(sentenceURI,
                                system);
                        final Collection<URI> testNodes = alignments == null ? ImmutableSet.of()
                                : alignments.get(goldNode);
                        if (testNodes.isEmpty()) {
                            goldEvaluators.get(system).addFN(1);
                        } else {
                            goldEvaluators.get(system).addTP(1);
                            unionEvaluators.get(system).addTP(1);
                            alignedNodes.putAll(system, testNodes);
                        }
                        out.append(String.format(this.rowMid + "%-60s",
                                escape(Util.format(sentenceURI, testNodes.toArray()))));
                    }
                    out.append(this.rowEnd);
                }
                for (final String system : this.systems) {
                    final Set<URI> testNodes = Sets
                            .newHashSet(nodesTable.get(sentenceURI, system));
                    testNodes.removeAll(alignedNodes.get(system));
                    goldEvaluators.get(system).addFP(testNodes.size());
                    unionEvaluators.get(system).addFP(testNodes.size());
                    for (final URI testNode : Util.VALUE_ORDERING.sortedCopy(testNodes)) {
                        out.append(String.format("\n" + this.rowBegin + "%30s" + this.rowMid
                                + "%60s", "", ""));
                        for (final String s : this.systems) {
                            out.append(String.format(this.rowMid + "%-60s",
                                    s.equals(system) ? escape(Util.format(sentenceURI, testNode))
                                            : ""));
                        }
                        out.append(this.rowEnd);
                    }
                }
                final Set<URI> union = Sets.newHashSet();
                for (final String system : this.systems) {
                    union.addAll(getMultimap(alignmentTable, sentenceURI, system).keySet());
                }
                union.retainAll(goldNodes);
                for (final String system : this.systems) {
                    unionEvaluators.get(system).addFN(
                            Sets.difference(union,
                                    getMultimap(alignmentTable, sentenceURI, system).keySet())
                                    .size());
                }
            }

            final Map<String, PrecisionRecall> goldPRs = finalizePR(goldEvaluators);
            final Map<String, PrecisionRecall> unionPRs = finalizePR(unionEvaluators);
            emitPR(out, goldPRs, unionPRs);
            return new Stats(goldPRs, unionPRs, out.toString());
        }

        private Stats unlabelledEvaluation() {

            final StringBuilder out = new StringBuilder();

            final Table<URI, String, List<Relation>> relationTable = HashBasedTable.create();
            final Table<URI, String, Multimap<Relation, Relation>> mappingTable = HashBasedTable
                    .create();
            for (final BindingSet bindings : Util.query(this.model, RELATION_QUERY)) {
                final URI g = (URI) bindings.getValue("g");
                final URI sentenceID = this.sentenceMap.get(g);
                final String system = this.systemMap.get(g);
                if (sentenceID != null && system != null) {
                    final URI s = (URI) bindings.getValue("s");
                    final URI o = (URI) bindings.getValue("o");
                    final URI sm = (URI) bindings.getValue("sm");
                    final URI om = (URI) bindings.getValue("om");
                    if (sm != null && om != null && sm.equals(om)) {
                        continue; // self relation after mapping
                    }
                    final Relation r = new Relation(s, o, true);
                    final Relation rm = new Relation(sm != null ? sm : s, om != null ? om : o,
                            sm == null || om == null);
                    getList(relationTable, sentenceID, system).add(rm);
                    getMultimap(mappingTable, sentenceID, system).put(rm, r);
                }
            }

            final Map<String, PrecisionRecall.Evaluator> goldEvaluators = initPR();
            final Map<String, PrecisionRecall.Evaluator> unionEvaluators = initPR();
            emitHeader(out);

            String sentenceURICell = "";
            for (final URI sentenceURI : Util.VALUE_ORDERING.sortedCopy(relationTable.rowKeySet())) {

                sentenceURICell = Util.format(null, sentenceURI);
                out.append(this.separator);

                final List<Relation> goldRelations = relationTable.get(sentenceURI, "gold");
                for (final Relation goldRelation : Ordering.natural().sortedCopy(goldRelations)) {
                    out.append(String.format("\n" + this.rowBegin + "%-30s" + this.rowMid
                            + "%-60s", escape(sentenceURICell),
                            escape(goldRelation.toString(sentenceURI))));
                    sentenceURICell = "";
                    for (final String system : this.systems) {
                        final Multimap<Relation, Relation> alignments = getMultimap(mappingTable,
                                sentenceURI, system);
                        final Collection<Relation> testRelations = alignments == null ? ImmutableSet
                                .of() : alignments.get(goldRelation);
                        if (testRelations.isEmpty()) {
                            goldEvaluators.get(system).addFN(1);
                        } else {
                            goldEvaluators.get(system).addTP(1);
                            unionEvaluators.get(system).addTP(1);
                        }
                        out.append(String.format(this.rowMid + "%-60s",
                                escape(Util.format(sentenceURI, testRelations.toArray()))));
                    }
                    out.append(this.rowEnd);
                }

                final Set<Relation> unknownRelations = Sets.newHashSet();
                final Set<Relation> goldRelationSet = ImmutableSet.copyOf(goldRelations);
                for (final String system : this.systems) {
                    final Multimap<Relation, Relation> multimap = getMultimap(mappingTable,
                            sentenceURI, system);
                    for (final Relation keyRelation : Ordering.natural().sortedCopy(
                            multimap.keySet())) {
                        if (!goldRelationSet.contains(keyRelation)) {
                            final boolean ignore = keyRelation.isExtra()
                                    || this.ignorableRelations.containsEntry(sentenceURI,
                                            keyRelation);
                            if (!ignore
                                    && !this.forbiddenRelations.containsEntry(sentenceURI,
                                            keyRelation)) {
                                unknownRelations.add(keyRelation);
                            }
                            if (!ignore) {
                                goldEvaluators.get(system).addFP(1);
                                unionEvaluators.get(system).addFP(1);
                            }
                            out.append(String.format("\n" + this.rowBegin + "%30s" + this.rowMid
                                    + "%60s", "", ""));
                            for (final String s : this.systems) {
                                out.append(String.format(
                                        this.rowMid + "%-60s",
                                        !s.equals(system) ? "" : (ignore ? "* " : "")
                                                + escape(Util.format(sentenceURI,
                                                        multimap.get(keyRelation).toArray()))));
                            }
                            out.append(this.rowEnd);
                        }
                    }
                }

                if (!unknownRelations.isEmpty()) {
                    LOGGER.warn("Unknown relations for sentence " + sentenceURI + ":\n"
                            + Joiner.on('\n').join(unknownRelations));
                }

                final Set<Relation> union = Sets.newHashSet();
                for (final String system : this.systems) {
                    union.addAll(getList(relationTable, sentenceURI, system));
                }
                union.retainAll(goldRelationSet);
                for (final String system : this.systems) {
                    unionEvaluators.get(system).addFN(
                            Sets.difference(union,
                                    getMultimap(mappingTable, sentenceURI, system).keySet())
                                    .size());
                }
            }

            final Map<String, PrecisionRecall> goldPRs = finalizePR(goldEvaluators);
            final Map<String, PrecisionRecall> unionPRs = finalizePR(unionEvaluators);
            emitPR(out, goldPRs, unionPRs);
            return new Stats(goldPRs, unionPRs, out.toString());
        }

        private Stats labelledEvaluation(@Nullable final Set<String> namespaces) {

            final StringBuilder out = new StringBuilder();

            final ValueFactory vf = Statements.VALUE_FACTORY;
            final URI extraCtx = vf.createURI("eval:Extra");

            final Table<URI, String, List<Statement>> stmtTable = HashBasedTable.create();
            final Table<URI, String, Multimap<Statement, Statement>> mappingTable = HashBasedTable
                    .create();
            for (final BindingSet bindings : Util.query(this.model, LABELLED_QUERY)) {
                final URI g = (URI) bindings.getValue("g");
                final URI sentenceID = this.sentenceMap.get(g);
                final String system = this.systemMap.get(g);
                if (sentenceID != null && system != null) {
                    final URI s = (URI) bindings.getValue("s");
                    final URI p = (URI) bindings.getValue("p");
                    final URI o = (URI) bindings.getValue("o");
                    final URI sm = (URI) bindings.getValue("sm");
                    final URI om = (URI) bindings.getValue("om");
                    if (namespaces != null && !namespaces.contains(p.getNamespace())) {
                        continue;
                    }
                    if (sm != null && om != null && sm.equals(om)) {
                        continue; // self relation after mapping
                    }
                    final Statement stmt = vf.createStatement(s, p, o);
                    final Statement stmtm = vf.createStatement(sm != null ? sm : s, p,
                            om != null ? om : o, sm == null || om == null ? extraCtx : null);
                    getList(stmtTable, sentenceID, system).add(stmtm);
                    getMultimap(mappingTable, sentenceID, system).put(stmtm, stmt);
                }
            }

            final Map<String, PrecisionRecall.Evaluator> goldEvaluators = initPR();
            final Map<String, PrecisionRecall.Evaluator> unionEvaluators = initPR();
            emitHeader(out);

            String sentenceURICell = "";
            for (final URI sentenceURI : Util.VALUE_ORDERING.sortedCopy(stmtTable.rowKeySet())) {

                sentenceURICell = Util.format(null, sentenceURI);
                out.append(this.separator);

                final List<Statement> goldStmts = MoreObjects.firstNonNull(
                        stmtTable.get(sentenceURI, "gold"), ImmutableList.<Statement>of());
                for (final Statement goldStmt : Util.STMT_ORDERING.sortedCopy(goldStmts)) {
                    out.append(String.format("\n" + this.rowBegin + "%-30s" + this.rowMid
                            + "%-60s", escape(sentenceURICell),
                            escape(Util.format(sentenceURI, goldStmt))));
                    sentenceURICell = "";
                    for (final String system : this.systems) {
                        final Multimap<Statement, Statement> alignments = getMultimap(
                                mappingTable, sentenceURI, system);
                        final Collection<Statement> testStmts = alignments == null ? ImmutableSet
                                .of() : alignments.get(goldStmt);
                        if (testStmts.isEmpty()) {
                            goldEvaluators.get(system).addFN(1);
                        } else {
                            goldEvaluators.get(system).addTP(1);
                            unionEvaluators.get(system).addTP(1);
                        }
                        out.append(String.format(this.rowMid + "%-60s",
                                escape(Util.format(sentenceURI, testStmts.toArray()))));
                    }
                    out.append(this.rowEnd);
                }

                final Set<Statement> goldStmtSet = ImmutableSet.copyOf(goldStmts);
                for (final String system : this.systems) {
                    final Multimap<Statement, Statement> multimap = getMultimap(mappingTable,
                            sentenceURI, system);
                    for (final Statement keyStmt : Util.STMT_ORDERING
                            .sortedCopy(multimap.keySet())) {
                        if (!goldStmtSet.contains(keyStmt)) {
                            final Relation keyRelation = keyStmt.getSubject() instanceof URI
                                    && keyStmt.getObject() instanceof URI ? new Relation(
                                    (URI) keyStmt.getSubject(), (URI) keyStmt.getObject(), false)
                                    : null;
                            final boolean ignore = extraCtx.equals(keyStmt.getContext())
                                    || this.ignorableRelations.containsEntry(sentenceURI,
                                            keyRelation);
                            if (!ignore) {
                                goldEvaluators.get(system).addFP(1);
                                unionEvaluators.get(system).addFP(1);
                            }
                            out.append(String.format("\n" + this.rowBegin + "%30s" + this.rowMid
                                    + "%60s", "", ""));
                            for (final String s : this.systems) {
                                out.append(String.format(
                                        this.rowMid + "%-60s",
                                        !s.equals(system) ? "" : (ignore ? "* " : "")
                                                + escape(Util.format(sentenceURI,
                                                        multimap.get(keyStmt).toArray()))));
                            }
                            out.append(this.rowEnd);
                        }
                    }
                }

                final Set<Statement> union = Sets.newHashSet();
                for (final String system : this.systems) {
                    union.addAll(getList(stmtTable, sentenceURI, system));
                }
                union.retainAll(goldStmtSet);
                for (final String system : this.systems) {
                    unionEvaluators.get(system).addFN(
                            Sets.difference(union,
                                    getMultimap(mappingTable, sentenceURI, system).keySet())
                                    .size());
                }
            }

            final Map<String, PrecisionRecall> goldPRs = finalizePR(goldEvaluators);
            final Map<String, PrecisionRecall> unionPRs = finalizePR(unionEvaluators);
            emitPR(out, goldPRs, unionPRs);
            return new Stats(goldPRs, unionPRs, out.toString());
        }

        private Stats attributeEvaluation(@Nullable final URI predicate,
                @Nullable final Set<String> valueNS) {

            final StringBuilder out = new StringBuilder();

            final ValueFactory vf = Statements.VALUE_FACTORY;
            final URI extraCtx = vf.createURI("eval:Extra");

            final Table<URI, String, List<Statement>> stmtTable = HashBasedTable.create();
            final Table<URI, String, Multimap<Statement, Statement>> mappingTable = HashBasedTable
                    .create();
            for (final BindingSet bindings : Util.query(this.model, ATTRIBUTE_QUERY)) {
                final URI g = (URI) bindings.getValue("g");
                final URI sentenceID = this.sentenceMap.get(g);
                final String system = this.systemMap.get(g);
                if (sentenceID != null && system != null) {
                    final URI s = (URI) bindings.getValue("s");
                    final URI p = (URI) bindings.getValue("p");
                    final Value o = bindings.getValue("o");
                    final URI sm = (URI) bindings.getValue("sm");
                    if (predicate != null && !p.equals(predicate) //
                            || valueNS != null && (!(o instanceof URI) || //
                            !valueNS.contains(((URI) o).getNamespace()))) {
                        continue;
                    }
                    final Statement stmt = vf.createStatement(s, p, o);
                    final Statement stmtm = vf.createStatement(sm != null ? sm : s, p, o,
                            sm == null ? extraCtx : null);
                    getList(stmtTable, sentenceID, system).add(stmtm);
                    getMultimap(mappingTable, sentenceID, system).put(stmtm, stmt);
                }
            }

            final Map<String, PrecisionRecall.Evaluator> goldEvaluators = initPR();
            final Map<String, PrecisionRecall.Evaluator> unionEvaluators = initPR();
            emitHeader(out);

            String sentenceURICell = "";
            for (final URI sentenceURI : Util.VALUE_ORDERING.sortedCopy(stmtTable.rowKeySet())) {

                sentenceURICell = Util.format(null, sentenceURI);
                out.append(this.separator);

                final List<Statement> goldStmts = MoreObjects.firstNonNull(
                        stmtTable.get(sentenceURI, "gold"), ImmutableList.<Statement>of());
                for (final Statement goldStmt : Util.STMT_ORDERING.sortedCopy(goldStmts)) {
                    out.append(String.format("\n" + this.rowBegin + "%-30s" + this.rowMid
                            + "%-60s", escape(sentenceURICell),
                            escape(Util.format(sentenceURI, goldStmt))));
                    sentenceURICell = "";
                    for (final String system : this.systems) {
                        final Multimap<Statement, Statement> alignments = getMultimap(
                                mappingTable, sentenceURI, system);
                        final Collection<Statement> testStmts = alignments == null ? ImmutableSet
                                .of() : alignments.get(goldStmt);
                        if (testStmts.isEmpty()) {
                            goldEvaluators.get(system).addFN(1);
                        } else {
                            goldEvaluators.get(system).addTP(1);
                            unionEvaluators.get(system).addTP(1);
                        }
                        out.append(String.format(this.rowMid + "%-60s",
                                escape(Util.format(sentenceURI, testStmts.toArray()))));
                    }
                    out.append(this.rowEnd);
                }

                final Set<Statement> goldStmtSet = ImmutableSet.copyOf(goldStmts);
                for (final String system : this.systems) {
                    final Multimap<Statement, Statement> multimap = getMultimap(mappingTable,
                            sentenceURI, system);
                    for (final Statement keyStmt : Util.STMT_ORDERING
                            .sortedCopy(multimap.keySet())) {
                        if (!goldStmtSet.contains(keyStmt)) {
                            final boolean ignore = extraCtx.equals(keyStmt.getContext());
                            if (!ignore) {
                                goldEvaluators.get(system).addFP(1);
                                unionEvaluators.get(system).addFP(1);
                            }
                            out.append(String.format("\n" + this.rowBegin + "%30s" + this.rowMid
                                    + "%60s", "", ""));
                            for (final String s : this.systems) {
                                out.append(String.format(
                                        this.rowMid + "%-60s",
                                        !s.equals(system) ? "" : (ignore ? "* " : "")
                                                + escape(Util.format(sentenceURI,
                                                        multimap.get(keyStmt).toArray()))));
                            }
                            out.append(this.rowEnd);
                        }
                    }
                }

                final Set<Statement> union = Sets.newHashSet();
                for (final String system : this.systems) {
                    union.addAll(getList(stmtTable, sentenceURI, system));
                }
                union.retainAll(goldStmtSet);
                for (final String system : this.systems) {
                    unionEvaluators.get(system).addFN(
                            Sets.difference(union,
                                    getMultimap(mappingTable, sentenceURI, system).keySet())
                                    .size());
                }
            }

            final Map<String, PrecisionRecall> goldPRs = finalizePR(goldEvaluators);
            final Map<String, PrecisionRecall> unionPRs = finalizePR(unionEvaluators);
            emitPR(out, goldPRs, unionPRs);
            return new Stats(goldPRs, unionPRs, out.toString());
        }

        private Map<String, PrecisionRecall.Evaluator> initPR() {
            final Map<String, PrecisionRecall.Evaluator> evaluators = Maps.newHashMap();
            for (final String system : this.systems) {
                evaluators.put(system, PrecisionRecall.evaluator());
            }
            return evaluators;
        }

        private Map<String, PrecisionRecall> finalizePR(
                final Map<String, PrecisionRecall.Evaluator> evaluators) {
            final ImmutableMap.Builder<String, PrecisionRecall> builder = ImmutableMap.builder();
            for (final String system : this.systems) {
                builder.put(system, evaluators.get(system).getResult());
            }
            return builder.build();
        }

        private void emitSection(final StringBuilder out, final String name) {
            out.append(String.format("\n\n\n\n=== %s ===\n\n\n", name));
        }

        private void emitHeader(final StringBuilder out) {
            out.append(String.format("%-30s | %-60s", "sentence", "gold"));
            for (final String system : this.systems) {
                out.append(String.format(" | %-60s", system));
            }
        }

        private void emitPR(final StringBuilder out,
                @Nullable final Map<String, PrecisionRecall> goldPRs,
                @Nullable final Map<String, PrecisionRecall> unionPRs) {
            out.append(this.separator);
            if (goldPRs != null) {
                out.append(String.format("\n%" + 93 + "s", "gold p/r   "));
                for (final String system : this.systems) {
                    final PrecisionRecall pr = goldPRs.get(system);
                    out.append(String.format(" | %-60s", Util.format(null, pr)));
                }
            }
            if (goldPRs != null) {
                out.append(String.format("\n%" + 93 + "s", "union p/r   "));
                for (final String system : this.systems) {
                    final PrecisionRecall pr = unionPRs.get(system);
                    out.append(String.format(" | %-60s", Util.format(null, pr)));
                }
            }
        }

        private void emitStatsHeader(final StringBuilder out, final List<String> systems) {
            final String blank = Strings.repeat(" ", 16);
            final String prStr = "  p     r     f1   ";
            final String prStrs = Strings.repeat(prStr, systems.size());
            final int count = prStrs.length() + 4;
            out.append(String.format("%s  %-" + count + "s  %-" + count + "s\n", blank,
                    "gold p/r", "union p/r"));
            out.append(blank);
            for (int i = 0; i < 2; ++i) {
                out.append("  gold");
                for (final String system : systems) {
                    out.append(String.format("  %-" + (prStr.length() - 2) + "s", system));
                }
            }
            out.append(String.format("\n%s     #%s     #%s\n", blank, prStrs, prStrs));
            out.append(Strings.repeat("-", 16 + prStrs.length() * 2 + 8 + 4));
            out.append("\n");
        }

        @SuppressWarnings({ "unchecked" })
        private void emitStats(final StringBuilder out, final Stats stats, final String label) {
            out.append(String.format("%-16s", label));
            for (final Map<String, PrecisionRecall> map : new Map[] { stats.getGoldPRs(),
                    stats.getUnionPRs() }) {
                final PrecisionRecall countPR = map.values().iterator().next();
                final int count = (int) (countPR.getTP() + countPR.getFN());
                out.append("  ").append(String.format("%4d", count));
                for (final String system : stats.getSystems()) {
                    final PrecisionRecall pr = map.get(system);
                    out.append("  ");
                    // out.append(String.format("%5.0f %5.0f %5.0f", pr.getTP(),
                    // pr.getFP(), pr.getFN()));
                    out.append(String.format("%5.3f %5.3f %5.3f", pr.getPrecision(),
                            pr.getRecall(), pr.getF1()));
                }
            }
            out.append("\n");
        }

        private static <R, C, T> List<T> getList(final Table<R, C, List<T>> table, final R row,
                final C col) {
            List<T> list = table.get(row, col);
            if (list == null) {
                list = Lists.newArrayList();
                table.put(row, col, list);
            }
            return list;
        }

        private static <R, C, K, V> Multimap<K, V> getMultimap(
                final Table<R, C, Multimap<K, V>> table, final R row, final C col) {
            Multimap<K, V> multimap = table.get(row, col);
            if (multimap == null) {
                multimap = HashMultimap.create();
                table.put(row, col, multimap);
            }
            return multimap;
        }

        private static Set<String> namespacesFor(final String... elements) {
            final Set<String> set = Sets.newHashSet();
            for (final String element : elements) {
                final String uri = Util.NAMESPACES.uriFor(element);
                set.add(uri != null ? uri : element);
            }
            return set;
        }

    }

    public static void main(final String[] args) {
        try {
            // Parse command line
            final CommandLine cmd = CommandLine.parser().withName("eval-evaluate")
                    .withOption("s", "simplified", "use simplified gold standard")
                    .withHeader("Evaluates precision/recall given aligned data.").parse(args);

            // Extract options
            final List<String> inputFiles = cmd.getArgs(String.class);
            final boolean simplified = cmd.hasOption("s");

            // Read the input
            final Map<String, String> namespaces = Maps.newHashMap();
            final List<Statement> stmts = Lists.newArrayList();
            RDFSources.read(false, false, null, null,
                    inputFiles.toArray(new String[inputFiles.size()])).emit(
                    RDFHandlers.wrap(stmts, namespaces), 1);

            // Perform the evaluation
            final Evaluation evaluation = Evaluation.evaluate(stmts, simplified);
            LOGGER.info("Evaluation results:\n\n{}", evaluation.getReport());

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

}
