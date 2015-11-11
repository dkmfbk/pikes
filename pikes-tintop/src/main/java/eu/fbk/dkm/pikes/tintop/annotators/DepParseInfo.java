package eu.fbk.dkm.pikes.tintop.annotators;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.Generics;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.*;

/**
 * Created by alessio on 10/11/15.
 */

public class DepParseInfo {

    private HashMap<Integer, Integer> depParents = new HashMap<>();
    private HashMap<Integer, String> depLabels = new HashMap<>();

    private static String space(int width) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < width; i++) {
            b.append(' ');
        }
        return b.toString();
    }

    // helper for toString()
    private static void recToString(IndexedWord curr, StringBuilder sb, int offset, Set<IndexedWord> used,
            SemanticGraph dependencies, HashMap<Integer, String> depLabels, HashMap<Integer, Integer> depParents) {
        used.add(curr);
        List<SemanticGraphEdge> edges = dependencies.outgoingEdgeList(curr);
        Collections.sort(edges);
        for (SemanticGraphEdge edge : edges) {
            IndexedWord target = edge.getTarget();

            depParents.put(target.index(), edge.getSource().index());
            depLabels.put(target.index(), edge.getRelation().toString());

            sb.append(space(2 * offset)).append("-> ").append(target).append(" (").append(edge.getRelation()).append(")\n");

            if (!used.contains(target)) { // recurse
                recToString(target, sb, offset + 1, used, dependencies, depLabels, depParents);
            }
        }
    }

    public DepParseInfo(HashMap<Integer, Integer> depParents,
            HashMap<Integer, String> depLabels) {
        this.depParents = depParents;
        this.depLabels = depLabels;
    }

    public DepParseInfo(SemanticGraph dependencies) {
        Collection<IndexedWord> rootNodes = dependencies.getRoots();
        if (rootNodes.isEmpty()) {
            // Shouldn't happen, but return something!
            return;
        }

        StringBuilder sb = new StringBuilder();
        Set<IndexedWord> used = Generics.newHashSet();
        for (IndexedWord root : rootNodes) {
            depParents.put(root.index(), 0);
            depLabels.put(root.index(), "root");
            sb.append("-> ").append(root).append(" (root)\n");
            recToString(root, sb, 1, used, dependencies, depLabels, depParents);
        }

        Set<IndexedWord> nodes = Generics.newHashSet(dependencies.vertexSet());
        nodes.removeAll(used);
        while (!nodes.isEmpty()) {
            IndexedWord node = nodes.iterator().next();
            sb.append(node).append("\n");
            recToString(node, sb, 1, used, dependencies, depLabels, depParents);
            nodes.removeAll(used);
        }

    }

    public HashMap<Integer, Integer> getDepParents() {
        return depParents;
    }

    public HashMap<Integer, String> getDepLabels() {
        return depLabels;
    }

    @Override public String toString() {
        return "DepParseInfo{" +
                "depParents=" + depParents +
                ", depLabels=" + depLabels +
                '}';
    }
}
