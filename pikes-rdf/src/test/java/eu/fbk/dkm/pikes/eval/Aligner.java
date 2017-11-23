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

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandler;
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
            final Map<String, IRI> graphs = Maps.newHashMap();
            for (final Resource graphID : model.filter(null, DCTERMS.SOURCE, sentenceID,
                    EVAL.METADATA).subjects()) {
                if (model.contains(graphID, RDF.TYPE, EVAL.KNOWLEDGE_GRAPH, EVAL.METADATA)) {
                    final String creator = model
                            .filter(graphID, DCTERMS.CREATOR, null, EVAL.METADATA).objectLiteral()
                            .stringValue();
                    graphs.put(creator, (IRI) graphID);
                }
            }
            final IRI goldGraphID = graphs.get("gold");
            Preconditions.checkNotNull(goldGraphID);
            final Collection<Statement> goldGraph = model.filter(null, null, null, goldGraphID);
            LOGGER.info("Processing sentence {}, {} gold statements, {} test graphs", sentenceID,
                    goldGraph.size(), graphs.size() - 1);
            for (final String creator : graphs.keySet()) {
                if (!creator.equals("gold")) {
                    final IRI testGraphID = graphs.get(creator);
                    final Collection<Statement> testGraph = model.filter(null, null, null,
                            testGraphID);
                    final Map<IRI, IRI> sentenceMapping = align(goldGraph, testGraph);
                    for (final Map.Entry<IRI, IRI> entry : sentenceMapping.entrySet()) {
                        mappingStmts.add(Statements.VALUE_FACTORY.createStatement(entry.getKey(),
                                EVAL.MAPPED_TO, entry.getValue(), testGraphID));
                    }
                }
            }
        }

        return mappingStmts;
    }

    public static Map<IRI, IRI> align(final Iterable<Statement> goldStmts,
            final Iterable<Statement> testStmts) {

        int goldNodesCount = 0;
        final Multimap<IRI, IRI> goldMap = HashMultimap.create();
        for (final Statement stmt : goldStmts) {
            if (stmt.getPredicate().equals(EVAL.DENOTED_BY)) {
                ++goldNodesCount;
                goldMap.put((IRI) stmt.getObject(), (IRI) stmt.getSubject());
            }
        }

        int testNodesCount = 0;
        final Multimap<IRI, IRI> testMap = HashMultimap.create();
        for (final Statement stmt : testStmts) {
            if (stmt.getPredicate().equals(EVAL.DENOTED_BY)) {
                ++testNodesCount;
                if (goldMap.containsKey(stmt.getObject())) {
                    testMap.put((IRI) stmt.getObject(), (IRI) stmt.getSubject());
                }
            }
        }

        final Map<IRI, IRI> baseMapping = Maps.newHashMap();
        final List<IRI> alternativesTestNodes = Lists.newArrayList();
        final List<IRI[]> alternativesGoldNodes = Lists.newArrayList();
        int alternativesCount = 1;
        for (final IRI term : testMap.keySet()) {
            final Collection<IRI> testNodes = testMap.get(term);
            final Collection<IRI> goldNodes = goldMap.get(term);
            for (final IRI testNode : testNodes) {
                if (goldNodes.size() == 1) {
                    baseMapping.put(testNode, goldNodes.iterator().next());
                } else {
                    alternativesTestNodes.add(testNode);
                    alternativesGoldNodes.add(goldNodes.toArray(new IRI[goldNodes.size()]));
                    alternativesCount *= goldNodes.size();
                }
            }
        }

        final Set<Relation> goldRelations = relationsFor(goldStmts);
        final Set<Relation> testRelations = relationsFor(testStmts);

        Map<IRI, IRI> bestMapping = baseMapping;
        PrecisionRecall bestPR = null;
        int bestCount = 0;

        final int[] tps = new int[alternativesCount];
        if (alternativesCount == 1) {
            bestPR = evaluate(goldRelations, testRelations, baseMapping);

        } else {
            for (int i = 0; i < alternativesCount; ++i) {
                final Map<IRI, IRI> mapping = Maps.newHashMap(baseMapping);
                int n = i;
                for (int j = 0; j < alternativesTestNodes.size(); ++j) {
                    final IRI testNode = alternativesTestNodes.get(j);
                    final IRI[] goldNodes = alternativesGoldNodes.get(j);
                    final IRI goldNode = goldNodes[n % goldNodes.length];
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
            final String creator = ((IRI) testStmts.iterator().next().getContext()).getLocalName();
            LOGGER.info(
                    "{} - {} gold nodes, {} test nodes, {} mapped nodes, {} alternatives, best PR ({}): {}",
                    creator, goldNodesCount, testNodesCount, bestMapping.size(),
                    alternativesCount, numOptimalSolutions, bestPR);
        }

        return bestMapping;
    }

    private static PrecisionRecall evaluate(final Set<Relation> goldRelations,
            final Set<Relation> testRelations, final Map<IRI, IRI> mapping) {

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

    private static Relation rewrite(final Relation relation, final Map<IRI, IRI> mapping) {
        final IRI first = (IRI) rewrite(relation.getFirst(), mapping);
        final IRI second = (IRI) rewrite(relation.getSecond(), mapping);
        return first == relation.getFirst() && second == relation.getSecond() ? relation
                : new Relation(first, second, relation.isExtra());
    }

    private static Value rewrite(final Value value, final Map<IRI, IRI> mapping) {
        if (value instanceof IRI) {
            final IRI mappedValue = mapping.get(value);
            if (mappedValue != null) {
                return mappedValue;
            }
        }
        return value;
    }

    private static Set<Relation> relationsFor(final Iterable<Statement> stmts) {
        final Set<IRI> nodes = Sets.newHashSet();
        for (final Statement stmt : stmts) {
            if (stmt.getPredicate().equals(EVAL.DENOTED_BY)) {
                nodes.add((IRI) stmt.getSubject());
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
                        .add(new Relation((IRI) stmt.getSubject(), (IRI) stmt.getObject(), false));
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
            RDFSources.read(false, false, null, null, null,true,
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
