package eu.fbk.dkm.pikes.naflib;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import ixa.kaflib.Coref;
import ixa.kaflib.Dep;
import ixa.kaflib.Entity;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Predicate;
import ixa.kaflib.Predicate.Role;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

public class NafRenderUtils {

    public static void renderText(final Appendable out, final KAFDocument document,
            final Iterable<Term> terms, final Iterable<Markable> markables) throws IOException {

        final List<Term> termList = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
        final Set<Term> termSet = ImmutableSet.copyOf(termList);
        if (termList.isEmpty()) {
            return;
        }

        final Markable[] markableIndex = indexMarkables(termList, markables);

        final Map<Term, Set<Coref>> corefs = Maps.newHashMap();
        for (final Coref coref : document.getCorefs()) {
            for (final Span<Term> span : coref.getSpans()) {
                for (final Term term : span.getTargets()) {
                    if (termSet.contains(term)) {
                        Set<Coref> set = corefs.get(term);
                        if (set == null) {
                            set = Sets.newHashSet();
                            corefs.put(term, set);
                        }
                        set.add(coref);
                    }
                }
            }
        }

        Markable markable = null;

        int index = termList.get(0).getOffset();
        final int end = Integer.MAX_VALUE;

        List<Coref> lastCorefs = ImmutableList.of();
        for (int i = 0; i < termList.size(); ++i) {

            final Term term = termList.get(i);
            final int termOffset = term.getOffset();
            final int termLength = endOf(term) - termOffset;
            final int termBegin = Math.max(termOffset, index);
            final int termEnd = Math.min(termOffset + termLength, end);
            final List<Coref> termCorefs = document.getCorefsByTerm(term);

            if (termBegin > index) {
                final List<Coref> sameCorefs = Lists.newArrayList(lastCorefs);
                sameCorefs.retainAll(termCorefs);
                out.append(sameCorefs.isEmpty() ? " " : "<span class=\"txt_coref\"> </span>");
            }

            if (markable == null) {
                markable = markableIndex[i];
                if (markable != null) {
                    out.append("<span style=\"background-color: ").append(markable.color)
                            .append("\">");
                }
            }

            out.append("<span class=\"txt_term_tip");
            for (final Coref coref : termCorefs) {
                if (coref.getSpans().size() > 1) {
                    out.append(" txt_coref");
                    break;
                }
            }
            out.append("\" title=\"");
            emitTermTooltip(out, document, term);
            out.append("\">");
            out.append(term.getForm());
            out.append("</span>");

            if (markable != null && term == markable.terms.get(markable.terms.size() - 1)) {
                out.append("</span>");
                markable = null;
            }

            index = termEnd;
            lastCorefs = termCorefs;
        }

        if (markable != null) {
            out.append("</span>");
        }
    }

    public static void renderParsing(final Appendable out, final KAFDocument document,
            final int sentence, final boolean emitDependencies, final boolean emitSRL,
            final Iterable<Markable> markables) throws IOException {
        new ParsingRenderer(out, document, sentence).render(emitDependencies, emitSRL, markables);
    }

    private static void emitTermTooltip(final Appendable out, final KAFDocument document,
            final Term term) throws IOException {

        // Emit basic term-level information: ID, POS
        out.append("<strong>Term ").append(term.getId()).append("</strong>");
        if (term.getPos() != null && term.getMorphofeat() != null) {
            out.append(": pos ").append(term.getPos()).append('/').append(term.getMorphofeat());
        }

        // Emit detailed term-level information: lemma, dep tree link, sst, synset, bbn, sumo
        if (term.getLemma() != null) {
            out.append(", lemma '").append(term.getLemma().replace("\"", "&quot;")).append("'");
        }

        final Dep dep = document.getDepToTerm(term);
        if (dep != null) {
            out.append(", ").append(dep.getRfunc()).append(" of '")
                    .append(dep.getFrom().getForm().replace("\"", "&quot;")).append("' (")
                    .append(dep.getFrom().getId()).append(")");
        }
        for (final ExternalRef ref : term.getExternalRefs()) {
            out.append(", ").append(ref.getResource()).append(' ').append(ref.getReference());
        }

        // Emit predicate info, if available
        final List<Predicate> predicates = document.getPredicatesByTerm(term);
        if (!predicates.isEmpty()) {
            final Predicate predicate = predicates.get(0);
            out.append("<br/><b>Predicate ").append(predicate.getId()).append("</b>: sense ");
            final boolean isNoun = term.getPos().toUpperCase().equals("N");
            for (final ExternalRef ref : predicate.getExternalRefs()) {
                final String resource = ref.getResource().toLowerCase();
                if ("propbank".equals(resource) && !isNoun || "nombank".equals(resource) && isNoun) {
                    out.append(ref.getReference());
                    break;
                }
            }
        }

        // Emit entity info, if available
        final List<Entity> entities = document.getEntitiesByTerm(term);
        if (!entities.isEmpty()) {
            final Entity entity = entities.get(0);
            out.append("<br/><b>Entity ").append(entity.getId()).append("</b>: type ")
                    .append(entity.getType());
            String separator = ", sense ";
            for (final ExternalRef ref : entity.getExternalRefs()) {
                out.append(separator);
                try {
                    String s = ref.getReference();
                    if (s.startsWith("http://dbpedia.org/resource/")) {
                        s = "dbpedia:" + ref.getReference().substring(28);
                    }
                    out.append(s);
                } catch (final Throwable ex) {
                    out.append(ref.getReference());
                }
                separator = " ";
            }
        }

        // Emit coref info, if available and enabled
        for (final Coref coref : document.getCorefsByTerm(term)) {
            if (coref.getSpans().size() > 1) {
                out.append("<br/><b>Coref ").append(coref.getId()).append("</b>: ");
                String separator = "";
                for (final Span<Term> span : coref.getSpans()) {
                    out.append(separator);
                    out.append(span.getTargets().get(0).getId());
                    out.append(" '").append(span.getStr()).append("'");
                    separator = ", ";
                }
            }
        }
    }

    private static Markable[] indexMarkables(final List<Term> terms,
            final Iterable<Markable> markables) {

        final Map<Term, Integer> termIndex = Maps.newHashMap();
        for (int i = 0; i < terms.size(); ++i) {
            termIndex.put(terms.get(i), i);
        }

        final Markable[] markableIndex = new Markable[terms.size()];
        for (final Markable markable : markables) {
            for (final Term term : markable.getTerms()) {
                final Integer index = termIndex.get(term);
                if (index != null) {
                    markableIndex[index] = markable;
                }
            }
        }

        return markableIndex;
    }

    private static int endOf(final Term term) {
        final List<WF> wfs = term.getWFs();
        final WF wf = wfs.get(wfs.size() - 1);
        final String str = wf.getForm();
        if (str.equals("-LSB-") || str.equals("-RSB-") || str.equals("''")) {
            return wf.getOffset() + 1;
        }
        return wf.getOffset() + wf.getLength();
    }

    private static final class ParsingRenderer {

        private final Appendable out;

        private final KAFDocument document;

        private final int sentence;

        private final List<Term> terms;

        private final List<Dep> deps;

        private final Map<Term, Integer> indexes;

        ParsingRenderer(final Appendable out, final KAFDocument document, final int sentence) {
            this.out = out;
            this.document = document;
            this.sentence = sentence;
            this.terms = document.getTermsBySent(sentence);
            this.deps = Lists.newArrayListWithCapacity(this.terms.size());
            this.indexes = Maps.newIdentityHashMap();
            for (int index = 0; index < this.terms.size(); ++index) {
                final Term term = this.terms.get(index);
                this.deps.add(document.getDepToTerm(term));
                this.indexes.put(term, index);
            }
        }

        void render(final boolean emitDependencies, final boolean emitSRL,
                final Iterable<Markable> markables) throws IOException {

            this.out.append("<table class=\"txt\" cellspacing=\"0\" cellpadding=\"0\">\n");

            if (emitDependencies) {
                renderDependencies();
            }

            renderTerms(markables);

            if (emitSRL) {
                renderSRL();
            }

            this.out.append("</table>\n");
        }

        private void renderDependencies() throws IOException {

            // every term is mapped to 4 consecutive horizontal cells
            // - line between cells 0 and 1 (left vertical) is used for outgoing leftward edges
            // - line between cells 1 and 2 (center vertical) is used for incoming edge
            // - line between cells 2 and 3 (right vertical) is used for outgoing rightward edges
            // three arrays are used to control the rendering of these lines; they are initialized
            // to consider the dependency roots, then they are progressively filled
            final boolean[] leftVerticalLines = new boolean[this.terms.size()];
            final boolean[] centerVerticalLines = new boolean[this.terms.size()];
            final boolean[] rightVerticalLines = new boolean[this.terms.size()];
            for (int i = 0; i < this.terms.size(); ++i) {
                if (this.deps.get(i) == null) {
                    centerVerticalLines[this.indexes.get(this.terms.get(i))] = true;
                }
            }

            // allocate the dependency arcs to table rows and render each of them
            final List<List<Term>> rows = computeDependencyRows();
            for (int j = 0; j < rows.size(); ++j) {
                final List<Term> row = rows.get(j);

                // open the table row
                this.out.append("<tr class=\"txt_dep\">\n");

                // label array: i-th element contains label of edge from/to element i (with other
                // endpoint j > i)
                final String[] labels = new String[this.terms.size()];

                // update arrays for labels and vertical lines
                for (final Term term : row) {
                    final int termIndex = this.indexes.get(term);
                    final Dep termDep = this.deps.get(termIndex);
                    final Term parent = termDep == null ? term : termDep.getFrom();
                    final int parentIndex = this.indexes.get(parent);
                    final String label = termDep == null ? "" : termDep.getRfunc().toLowerCase();
                    centerVerticalLines[termIndex] = true;
                    if (termIndex < parentIndex) { // term <-- parent (right to left)
                        leftVerticalLines[parentIndex] = true;
                        labels[termIndex] = label;
                    } else if (termIndex > parentIndex) { // parent --> term (left to right)
                        rightVerticalLines[parentIndex] = true;
                        labels[parentIndex] = label;
                    }
                }

                // generate the table row, by emitting TDs (spanning multiple cells) each
                // corresponding to a blank space or to an horizontal, labelled dep edge
                String label = null;
                boolean arrow = false;
                int start = 0;
                int end = 0;
                for (int i = 0; i < this.terms.size(); ++i) {
                    ++end;
                    if (leftVerticalLines[i]) {
                        renderDependencyCell(start, end, label, arrow);
                        start = end;
                        label = null;
                        arrow = false;
                    }
                    ++end;
                    if (centerVerticalLines[i]) {
                        renderDependencyCell(start, end, label, arrow);
                        start = end;
                        label = rightVerticalLines[i] ? null : labels[i];
                        arrow = j == rows.size() - 1;
                    }
                    ++end;
                    if (rightVerticalLines[i]) {
                        renderDependencyCell(start, end, label, arrow);
                        start = end;
                        label = labels[i];
                        arrow = false;
                    }
                    ++end;
                }
                renderDependencyCell(start, end, null, arrow); // emit remaining blank TD

                // close the table row
                this.out.append("</tr>\n");
            }

            // emit a final row to extend the vertical edges departing from terms
            this.out.append("<tr>\n");
            for (int i = 0; i < this.terms.size(); ++i) {
                final boolean left = leftVerticalLines[i];
                final boolean right = rightVerticalLines[i];
                this.out.append("<td class=\"txt_dep_co").append(left ? " rb" : "")
                        .append("\"></td>");
                this.out.append("<td class=\"txt_dep_ci").append(left ? " lb" : "")
                        .append("\"></td>");
                this.out.append("<td class=\"txt_dep_ci").append(right ? " rb" : "")
                        .append("\"></td>");
                this.out.append("<td class=\"txt_dep_co").append(right ? " lb" : "")
                        .append("\"></td>\n");
            }
            this.out.append("</tr>\n");
        }

        private void renderDependencyCell(final int from, final int to, final String label,
                final boolean arrow) throws IOException {

            // open table cell
            this.out.append("<td class=\"");

            // emit CSS classes for left, right and top borders
            String separator = "";
            if (from != 0) {
                this.out.append(separator).append("txt_lb");
                separator = " ";
            }
            if (to != 4 * this.terms.size()) {
                this.out.append(separator).append("txt_rb");
                separator = " ";
            }
            if (label != null) {
                this.out.append(separator).append("txt_tb");
            }
            this.out.append("\"");

            // emit colspan attribute to control the length of the arc
            if (to - from > 1) {
                this.out.append(" colspan=\"").append(Integer.toString(to - from)).append("\"");
            }

            // emit the cell content (i.e., the dependency labels, if any)
            this.out.append("><div><span>") //
                    .append(label != null ? label : "&nbsp;") //
                    .append("</span></div>");

            // emit the <div> displaying the downward arrow, if requested
            if (arrow) {
                this.out.append("<div class=\"txt_ab\"></div>");
            }

            // close table cell
            this.out.append("</td>\n");
        }

        private List<List<Term>> computeDependencyRows() {

            // allocate a table for the result
            final List<List<Term>> rows = Lists.newArrayList();

            // start with all the terms, pick up the ones of the first row and then drop them and
            // repeat for the second row and so on, until all terms (=edges) have been considered
            final Set<Term> remaining = Sets.newHashSet(this.terms);
            while (!remaining.isEmpty()) {
                final List<Term> candidates = ImmutableList.copyOf(remaining);
                final List<Term> row = Lists.newArrayList();
                rows.add(row);

                // consider each candidate term for inclusion in the row
                for (final Term t1 : candidates) {

                    // retrieve dep parent, start / end indexes of the dep edge t1
                    final int s1 = this.indexes.get(t1);
                    final Dep dep1 = this.deps.get(s1);
                    final Term p1 = dep1 == null ? t1 : dep1.getFrom();
                    final int e1 = this.indexes.get(p1);

                    // can emit t1 only if its edge does not contain (horizontally) the edge of
                    // another candidate (in which case we pick up the other candidate)
                    boolean canEmit = true;
                    for (final Term t2 : candidates) {
                        if (t2 != t1) { // don't compare t1 with itself

                            // Retrieve dep parent, start / end indexes of dep edge t2
                            final int s2 = this.indexes.get(t2);
                            final Dep dep2 = this.deps.get(s2);
                            final Term p2 = dep2 == null ? t2 : dep2.getFrom();
                            final int e2 = this.indexes.get(p2);

                            // Compare t1 and t2. If t1 would contain t2 (in the graph) drop it
                            if (Math.min(s1, e1) <= Math.min(s2, e2)
                                    && Math.max(s1, e1) >= Math.max(s2, e2)) {
                                canEmit = false;
                                break;
                            }
                        }
                    }

                    // emit t1 iff it satisfied all the tests. Do not consider it anymore
                    if (canEmit) {
                        row.add(t1);
                        remaining.remove(t1);
                    }
                }
            }

            // add an initial empty row with no dep edges (will only contain the vertical line to
            // the dep root)
            rows.add(ImmutableList.<Term>of());

            // reverse the row so that the first one is the first to be emitted in the table
            Collections.reverse(rows);
            return rows;
        }

        private void renderTerms(final Iterable<Markable> markables) throws IOException {

            final Markable[] markableIndex = indexMarkables(this.terms, markables);

            // open the TR row
            this.out.append("<tr class=\"txt_terms\">\n");

            // emit the TD cells for each term, possibly adding entity / predicate highlighting
            for (int i = 0; i < this.terms.size(); ++i) {
                final Term term = this.terms.get(i);
                this.out.append("<td colspan=\"4\"><div class=\"");
                final Markable markable = markableIndex[i];
                if (markable == null) {
                    this.out.append("txt_term_c\">");
                } else {
                    final boolean start = i == 0 || markable != markableIndex[i - 1];
                    final boolean end = i == this.terms.size() - 1
                            || markable != markableIndex[i + 1];
                    this.out.append(start ? end ? "txt_term_lcr" : "txt_term_lc"
                            : end ? "txt_term_cr" : "txt_term_c");
                    this.out.append("\" style=\"background-color: ").append(markable.color)
                            .append("\">");
                }
                this.out.append("<span class=\"txt_term_tip\" title=\"");
                emitTermTooltip(this.out, this.document, term);
                this.out.append("\">").append(term.getForm().replace(' ', '_')).append("</span>");
                this.out.append("</div></td>\n");
            }

            // close the TR row
            this.out.append("</tr>\n");
        }

        private void renderSRL() throws IOException {

            // retrieve all the predicate in the sentence
            // final List<Predicate> predicates =
            // this.document.getPredicatesBySent(this.sentence);

            // retrieve all the SRL proposition in the sentence
            final List<SRLElement> propositions = Lists.newArrayList();
            for (final Predicate predicate : this.document.getPredicatesBySent(this.sentence)) {
                propositions.add(new SRLElement(null, predicate, true));
            }

            // allocate propositions to 'proposition' rows, each one reporting one or more
            // predicates starting from the ones with smallest extent
            for (final List<SRLElement> propositionRow : computeSRLRows(propositions)) {

                // emit a blank TR to visually separate predicate rows
                this.out.append("<tr class=\"txt_empty\"><td").append(" colspan=\"")
                        .append(Integer.toString(4 * this.terms.size())).append("\"")
                        .append("></td></tr>\n");

                // extract all the markables (Predicate/Role) allocated to the row
                final List<SRLElement> markables = Lists.newArrayList();
                for (final SRLElement proposition : propositionRow) {
                    final Predicate predicate = (Predicate) proposition.element;
                    markables.add(new SRLElement(proposition, predicate, false));
                    for (final Role role : predicate.getRoles()) {
                        markables.add(new SRLElement(proposition, role, false));
                    }
                }

                // allocate the markables to concrete TR 'markable' rows, to account for markables
                // containing one each other; emit these rows one at a time
                for (final List<SRLElement> markableRow : computeSRLRows(markables)) {

                    // open the TR row
                    this.out.append("<tr class=\"txt_srl\">\n");

                    // determine which cells in the TR row must have a left/right vertical line;
                    // this is done w.r.t. the subset of predicates rendered in the TR row
                    final boolean[] leftBorders = new boolean[this.terms.size()];
                    final boolean[] rightBorders = new boolean[this.terms.size()];
                    for (final SRLElement markable : markableRow) {
                        final SRLElement proposition = markable.parent;
                        final int s = this.indexes.get(proposition.terms.get(0));
                        final int e = this.indexes.get(proposition.terms.get(proposition.terms
                                .size() - 1));
                        leftBorders[s] = true;
                        rightBorders[e] = true;
                        if (s > 0) {
                            rightBorders[s - 1] = true;
                        }
                        if (e < this.terms.size() - 1) {
                            leftBorders[e + 1] = true;
                        }
                    }

                    // associate each term=cell to the markable it possibly represent
                    final SRLElement[] cells = new SRLElement[this.terms.size()];
                    for (final SRLElement markable : markableRow) {
                        for (final Term term : markable.terms) {
                            cells[this.indexes.get(term)] = markable;
                        }
                    }

                    // emit the cells of the TR row, each one being blank or corresponding to a
                    // predicate or argument; this is done by scanning terms from left to right
                    int start = 0;
                    int end = start + 1;
                    while (start < this.terms.size()) {

                        // determine where to end the current TD cell
                        final SRLElement markable = cells[start];
                        while (end < this.terms.size() && cells[end] == markable
                                && !leftBorders[end]) {
                            ++end;
                        }

                        // open the TD cell, emitting CSS classes for left/right borders
                        final boolean lb = leftBorders[start]; // left border
                        final boolean rb = rightBorders[end - 1]; // right border
                        this.out.append("<td colspan=\"")
                                .append(Integer.toString(4 * (end - start)))
                                .append("\"")
                                .append(lb ? rb ? "class=\"txt_lb txt_rb\"" : "class=\"txt_lb\""
                                        : rb ? "class=\"txt_rb\"" : "") //
                                .append(">");

                        // emit the predicate/argument for the current cell, if any
                        if (markable != null) {
                            this.out.append("<div>");
                            final Object element = markable.element;
                            if (element instanceof Predicate) {
                                final Predicate predicate = (Predicate) element;
                                final String res = predicate.getTerms().get(0).getPos()
                                        .equalsIgnoreCase("V") ? "propbank" : "nombank";
                                String roleset = null;
                                for (final ExternalRef ref : predicate.getExternalRefs()) {
                                    if (res.equalsIgnoreCase(ref.getResource())) {
                                        if (ref.getSource() != null) {
                                            roleset = ref.getReference();
                                            break;
                                        } else if (roleset == null) {
                                            roleset = ref.getReference();
                                        }
                                    }
                                }
                                if (roleset != null) {
                                    this.out.append(roleset);
                                }
                            } else {
                                this.out.append(((Role) element).getSemRole());
                            }
                            this.out.append("</div>");
                        }

                        // close the TD cell
                        this.out.append("</td>\n");

                        // update start/end indexes
                        start = end;
                        ++end;
                    }

                    // close the TR row
                    this.out.append("</tr>\n");
                }
            }
        }

        private List<List<SRLElement>> computeSRLRows(final Iterable<SRLElement> elements) {

            // allocate the resulting row list
            final List<List<SRLElement>> rows = Lists.newArrayList();

            // select a non-overlapping subset of supplied elements to form the first row, then
            // discard them and repeat to form next rows, until all elements have been allocated
            final Set<SRLElement> remaining = Sets.newHashSet(elements);
            while (!remaining.isEmpty()) {

                // allocate a new table row
                final List<SRLElement> row = Lists.newArrayList();
                rows.add(row);

                // rank the remaining elements in order of increasing (# terms) length
                final List<SRLElement> ranking = Ordering.natural().sortedCopy(remaining);

                // try to add as many remaining elements from the ranking, ensuring there are no
                // overlappings in the added ones; we use the ranking as an heuristic that should
                // lead to a 'good' selection (no optimality guarantee, whatever optimality means)
                for (final SRLElement candidate : ranking) {

                    // check for overlapping with already chosen elements in the current rows
                    boolean canEmit = true;
                    for (final SRLElement element : row) {
                        if (candidate.overlaps(element)) {
                            canEmit = false;
                            break;
                        }
                    }

                    // add the element upon success, and do not consider it anymore
                    if (canEmit) {
                        row.add(candidate);
                        remaining.remove(candidate);
                    }
                }
            }
            return rows;
        }

    }

    private static final class SRLElement implements Comparable<SRLElement> {

        final SRLElement parent;

        final Object element;

        final List<Term> terms;

        final int begin;

        final int end;

        SRLElement(final SRLElement parent, final Object element, final boolean useProposition) {
            this.parent = parent;
            this.element = element;
            if (useProposition) {
                final Predicate predicate = (Predicate) element;
                final Set<Term> termSet = Sets.newHashSet();
                termSet.addAll(predicate.getTerms());
                for (final Role role : predicate.getRoles()) {
                    termSet.addAll(role.getTerms());
                }
                this.terms = Ordering.from(Term.OFFSET_COMPARATOR).immutableSortedCopy(termSet);
            } else if (element instanceof Predicate) {
                this.terms = ((Predicate) element).getTerms();
            } else {
                this.terms = ((Role) element).getTerms();
            }
            this.begin = this.terms.get(0).getOffset();
            this.end = endOf(this.terms.get(this.terms.size() - 1));
        }

        boolean overlaps(final SRLElement other) {
            return this.end > other.begin && this.begin < other.end;
        }

        @Override
        public int compareTo(final SRLElement other) {
            int result = 0;
            if (other != this) {
                result = this.terms.size() - other.terms.size();
                if (result == 0) {
                    result = System.identityHashCode(this.element)
                            - System.identityHashCode(other.element);
                }
            }
            return result;
        }

    }

    public static final class Markable {

        private final List<Term> terms;

        private final String color;

        public Markable(final Iterable<Term> terms, final String color) {
            this.terms = ImmutableList.copyOf(terms);
            this.color = color;
        }

        public List<Term> getTerms() {
            return this.terms;
        }

        public String getColor() {
            return this.color;
        }

    }

}
