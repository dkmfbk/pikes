package eu.fbk.dkm.pikes.eval;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.CommandLine.Type;
import eu.fbk.utils.eval.PrecisionRecall;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

public class Aligner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Aligner.class);

    public static List<Statement> align(final Collection<Statement> stmts) {

        final QuadModel model = stmts instanceof QuadModel ? (QuadModel) stmts : QuadModel
                .create(stmts);

        final List<Statement> mappingStmts = Lists.newArrayList();

        for (final Resource sentenceID : model
                .filter(null, RDF.TYPE, EVAL.SENTENCE, EVAL.METADATA).subjects()) {
            final Map<String, URI> graphs = Maps.newHashMap();
            for (final Resource graphID : model.filter(null, DCTERMS.SOURCE, sentenceID,
                    EVAL.METADATA).subjects()) {
                if (model.contains(graphID, RDF.TYPE, EVAL.KNOWLEDGE_GRAPH, EVAL.METADATA)) {
                    final String creator = model
                            .filter(graphID, DCTERMS.CREATOR, null, EVAL.METADATA).objectLiteral()
                            .stringValue();
                    graphs.put(creator, (URI) graphID);
                }
            }
            final URI goldGraphID = graphs.get("gold");
            Preconditions.checkNotNull(goldGraphID);
            final Collection<Statement> goldGraph = model.filter(null, null, null, goldGraphID);
            LOGGER.info("Processing sentence {}, {} gold statements, {} test graphs", sentenceID,
                    goldGraph.size(), graphs.size() - 1);
            for (final String creator : graphs.keySet()) {
                if (!creator.equals("gold")) {
                    final URI testGraphID = graphs.get(creator);
                    final Collection<Statement> testGraph = model.filter(null, null, null,
                            testGraphID);
                    final Map<URI, URI> sentenceMapping = align(goldGraph, testGraph);
                    for (final Map.Entry<URI, URI> entry : sentenceMapping.entrySet()) {
                        mappingStmts.add(Statements.VALUE_FACTORY.createStatement(entry.getKey(),
                                EVAL.MAPPED_TO, entry.getValue(), testGraphID));
                    }
                }
            }
        }

        return mappingStmts;
    }

    public static Map<URI, URI> align(final Iterable<Statement> goldStmts,
            final Iterable<Statement> testStmts) {

        int goldNodesCount = 0;
        final Multimap<URI, URI> goldMap = HashMultimap.create();
        for (final Statement stmt : goldStmts) {
            if (stmt.getPredicate().equals(EVAL.DENOTED_BY)) {
                ++goldNodesCount;
                goldMap.put((URI) stmt.getObject(), (URI) stmt.getSubject());
            }
        }

        int testNodesCount = 0;
        final Multimap<URI, URI> testMap = HashMultimap.create();
        for (final Statement stmt : testStmts) {
            if (stmt.getPredicate().equals(EVAL.DENOTED_BY)) {
                ++testNodesCount;
                if (goldMap.containsKey(stmt.getObject())) {
                    testMap.put((URI) stmt.getObject(), (URI) stmt.getSubject());
                }
            }
        }

        final Map<URI, URI> baseMapping = Maps.newHashMap();
        final List<URI> alternativesTestNodes = Lists.newArrayList();
        final List<URI[]> alternativesGoldNodes = Lists.newArrayList();
        int alternativesCount = 1;
        for (final URI term : testMap.keySet()) {
            final Collection<URI> testNodes = testMap.get(term);
            final Collection<URI> goldNodes = goldMap.get(term);
            for (final URI testNode : testNodes) {
                if (goldNodes.size() == 1) {
                    baseMapping.put(testNode, goldNodes.iterator().next());
                } else {
                    alternativesTestNodes.add(testNode);
                    alternativesGoldNodes.add(goldNodes.toArray(new URI[goldNodes.size()]));
                    alternativesCount *= goldNodes.size();
                }
            }
        }

        final Set<Relation> goldRelations = relationsFor(goldStmts);
        final Set<Relation> testRelations = relationsFor(testStmts);

        Map<URI, URI> bestMapping = baseMapping;
        PrecisionRecall bestPR = null;
        int bestCount = 0;

        final int[] tps = new int[alternativesCount];
        if (alternativesCount == 1) {
            bestPR = evaluate(goldRelations, testRelations, baseMapping);

        } else {
            for (int i = 0; i < alternativesCount; ++i) {
                final Map<URI, URI> mapping = Maps.newHashMap(baseMapping);
                int n = i;
                for (int j = 0; j < alternativesTestNodes.size(); ++j) {
                    final URI testNode = alternativesTestNodes.get(j);
                    final URI[] goldNodes = alternativesGoldNodes.get(j);
                    final URI goldNode = goldNodes[n % goldNodes.length];
                    n = n / goldNodes.length;
                    mapping.put(testNode, goldNode);
                }
                final PrecisionRecall pr = evaluate(goldRelations, testRelations, mapping);
                final int count = ImmutableSet.copyOf(mapping.values()).size();
                if (bestPR == null || pr.getTP() > bestPR.getTP() || pr.getTP() == bestPR.getTP()
                        && count > bestCount) {
                    bestPR = pr;
                    bestCount = count;
                    bestMapping = mapping;
                }
                tps[i] = (int) pr.getTP();
            }
        }

        int numOptimalSolutions = 0;
        for (int i = 0; i < alternativesCount; ++i) {
            if (tps[i] == (int) bestPR.getTP()) {
                ++numOptimalSolutions;
            }
        }
        numOptimalSolutions = Math.max(1, numOptimalSolutions);

        if (LOGGER.isInfoEnabled()) {
            final String creator = ((URI) testStmts.iterator().next().getContext()).getLocalName();
            LOGGER.info(
                    "{} - {} gold nodes, {} test nodes, {} mapped nodes, {} alternatives, best PR ({}): {}",
                    creator, goldNodesCount, testNodesCount, bestMapping.size(),
                    alternativesCount, numOptimalSolutions, bestPR);
        }

        return bestMapping;
    }

    private static PrecisionRecall evaluate(final Set<Relation> goldRelations,
            final Set<Relation> testRelations, final Map<URI, URI> mapping) {

        final Set<Relation> rewrittenTestRelations = new HashSet<>();
        for (final Relation relation : testRelations) {
            final Relation rewrittenRelation = rewrite(relation, mapping);
            if (!rewrittenRelation.getFirst().equals(rewrittenRelation.getSecond())) {
                rewrittenTestRelations.add(rewrittenRelation);
            }
        }

        final int tp = Sets.intersection(goldRelations, rewrittenTestRelations).size();
        final int fp = rewrittenTestRelations.size() - tp;
        final int fn = goldRelations.size() - tp;

        return PrecisionRecall.forCounts(tp, fp, fn);
    }

    private static Relation rewrite(final Relation relation, final Map<URI, URI> mapping) {
        final URI first = (URI) rewrite(relation.getFirst(), mapping);
        final URI second = (URI) rewrite(relation.getSecond(), mapping);
        return first == relation.getFirst() && second == relation.getSecond() ? relation
                : new Relation(first, second, relation.isExtra());
    }

    private static Value rewrite(final Value value, final Map<URI, URI> mapping) {
        if (value instanceof URI) {
            final URI mappedValue = mapping.get(value);
            if (mappedValue != null) {
                return mappedValue;
            }
        }
        return value;
    }

    private static Set<Relation> relationsFor(final Iterable<Statement> stmts) {
        final Set<URI> nodes = Sets.newHashSet();
        for (final Statement stmt : stmts) {
            if (stmt.getPredicate().equals(EVAL.DENOTED_BY)) {
                nodes.add((URI) stmt.getSubject());
            }
        }
        final Set<Relation> relations = Sets.newHashSet();
        for (final Statement stmt : stmts) {
            if (!stmt.getPredicate().equals(EVAL.CLASSIFIABLE_AS)
                    && !stmt.getPredicate().equals(EVAL.ASSOCIABLE_TO)
                    && !stmt.getPredicate().equals(EVAL.NOT_ASSOCIABLE_TO)
                    && !stmt.getSubject().equals(stmt.getObject()) //
                    && nodes.contains(stmt.getSubject()) //
                    && (nodes.contains(stmt.getObject()) || stmt.getPredicate().equals(RDF.TYPE))) {
                relations
                        .add(new Relation((URI) stmt.getSubject(), (URI) stmt.getObject(), false));
            }
        }
        return relations;
    }

    public static void main(final String[] args) {

        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("eval-aligner")
                    .withHeader("Alignes the knowledge graphs produced by different tools " //
                            + "againsts a gold graph")
                    .withOption("o", "output", "the output file", "FILE", Type.STRING, true,
                            false, true) //
                    .withLogger(LoggerFactory.getLogger("eu.fbk")) //
                    .parse(args);

            // Extract options
            final String outputFile = cmd.getOptionValue("o", String.class);
            final List<String> inputFiles = cmd.getArgs(String.class);

            // Read the input
            final Map<String, String> namespaces = Maps.newHashMap();
            final QuadModel input = QuadModel.create();
            RDFSources.read(false, false, null, null,
                    inputFiles.toArray(new String[inputFiles.size()])).emit(
                    RDFHandlers.wrap(input, namespaces), 1);

            // Perform the alignment
            final List<Statement> mappingStmts = align(input);
            input.addAll(mappingStmts);

            // Write the output
            final RDFHandler out = RDFHandlers.write(null, 1000, outputFile);
            out.startRDF();
            namespaces.put(DCTERMS.PREFIX, DCTERMS.NAMESPACE);
            for (final Map.Entry<String, String> entry : namespaces.entrySet()) {
                if (!entry.getKey().isEmpty()) {
                    out.handleNamespace(entry.getKey(), entry.getValue());
                }
            }
            for (final Statement stmt : Ordering.from(
                    Statements.statementComparator("cspo",
                            Statements.valueComparator(RDF.NAMESPACE))).sortedCopy(input)) {
                out.handleStatement(stmt);
            }
            out.endRDF();

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

}
