package eu.fbk.dkm.pikes.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.core.Range;
import ixa.kaflib.Coref;
import ixa.kaflib.Dep;
import ixa.kaflib.Entity;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.Factuality;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Opinion.OpinionHolder;
import ixa.kaflib.Opinion.OpinionTarget;
import ixa.kaflib.Predicate;
import ixa.kaflib.Predicate.Role;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import ixa.kaflib.Timex3;
import ixa.kaflib.WF;

import eu.fbk.rdfpro.util.IO;
import org.eclipse.rdf4j.model.IRI;

public final class NAFUtils {

    public static final String RESOURCE_PROPBANK = "PropBank";

    public static final String RESOURCE_NOMBANK = "NomBank";

    public static final String RESOURCE_VERBNET = "VerbNet";

    public static final String RESOURCE_FRAMENET = "FrameNet";

    public static final String RESOURCE_BBN = "BBN";

    public static final String RESOURCE_WN_SYNSET = "wn30-ukb";

    public static final String RESOURCE_WN_SST = "wn30-sst";

    public static final String RESOURCE_SUMO = "SUMO";

    public static final String RESOURCE_ENTITY_REF = "NAFFilter-EntityRef";

    public static final String RESOURCE_ENTITY_COREF = "NAFFilter-EntityCoref";

    public static final String RESOURCE_PREDICATE_REF = "NAFFilter-PredicateRef";

    public static final String RESOURCE_PREDICATE_COREF = "NAFFilter-PredicateCoref";

    public static final String RESOURCE_TIMEX_REF = "NAFFilter-TimexRef";

    public static final String RESOURCE_TIMEX_COREF = "NAFFilter-TimexCoref";

    public static final String RESOURCE_VALUE = "value";

    public static final String RESOURCE_YAGO = "Yago";

    public static final String PREMON_NAMESPACE = "http://premon.fbk.eu/resource/";
    public static final String PREMON_FNPREFIX = "fn15";
    public static final String PREMON_VNPREFIX = "vb32";
    public static final String PREMON_PBPREFIX = "pb17";
    public static final String PREMON_NBPREFIX = "nb10";
    public static final String PREMON_ARGUMENT_SEPARATOR = "@";
    public static final String PREMON_RESOURCE_PROPBANK = "PreMOn+PropBank";

    public static final String PREMON_RESOURCE_NOMBANK = "PreMOn+NomBank";

    public static final String PREMON_RESOURCE_VERBNET = "PreMOn+VerbNet";

    public static final String PREMON_RESOURCE_FRAMENET = "PreMOn+FrameNet";

    public static final Ordering<Opinion> OPINION_COMPARATOR = new Ordering<Opinion>() {

        @Override
        public int compare(final Opinion left, final Opinion right) {
            final int leftOffset = left.getOpinionExpression().getSpan().getTargets().get(0)
                    .getOffset();
            final int rightOffset = right.getOpinionExpression().getSpan().getTargets().get(0)
                    .getOffset();
            return leftOffset - rightOffset;
        }

    };

    private static final Pattern WF_EXCLUSION_PATTERN = Pattern.compile("[^A-Za-z0-9]*");

    private static final Set<String> SYMBOLS = ImmutableSet.of("$", "#", "&", "€");

    public static void normalize(final KAFDocument document) {

        // Convert SST, synset and BBN attributes to external refs
        for (final Term term : document.getTerms()) {
            boolean hasBBN = false;
            boolean hasSynset = false;
            boolean hasSST = false;
            for (final ExternalRef ref : term.getExternalRefs()) {
                hasBBN |= RESOURCE_BBN.equalsIgnoreCase(ref.getResource());
                hasSynset |= RESOURCE_WN_SYNSET.equalsIgnoreCase(ref.getResource());
                hasSST |= RESOURCE_WN_SST.equalsIgnoreCase(ref.getResource());
            }
            if (!hasBBN && term.getBBNTag() != null) {
                term.addExternalRef(document.newExternalRef(RESOURCE_BBN, term.getBBNTag()));
            }
            if (!hasSynset && term.getWordnetSense() != null) {
                term.addExternalRef(document.newExternalRef(RESOURCE_WN_SYNSET,
                        term.getWordnetSense()));
            }
            if (!hasSST && term.getSupersenseTag() != null) {
                term.addExternalRef(document.newExternalRef(RESOURCE_WN_SST,
                        term.getSupersenseTag()));
            }
            term.setBBNTag(null);
            term.setWordnetSense(null);
            term.setSupersenseTag(null);
        }

        // Remove duplicate external refs
        for (final Predicate predicate : document.getPredicates()) {
            normalizeRefs(getRefs(predicate));
            for (final Role role : predicate.getRoles()) {
                normalizeRefs(getRefs(role));
            }
        }
    }

    public static List<Term> filterTerms(final Iterable<Term> terms) {
        final List<Term> result = Lists.newArrayList();
        boolean atBeginning = true;
        for (final Term term : Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms)) {
            final char pos = Character.toUpperCase(term.getPos().charAt(0));
            if (atBeginning && (pos == 'D' || pos == 'P')) {
                continue;
            }
            for (final WF word : term.getWFs()) {
                final String text = word.getForm();
                if (SYMBOLS.contains(text) || !WF_EXCLUSION_PATTERN.matcher(text).matches()) {
                    result.add(term);
                    atBeginning = false;
                    break;
                }
            }
        }
        return result;
    }

    public static String getText(final Iterable<Term> terms) {
        final StringBuilder builder = new StringBuilder();
        boolean atBeginning = true;
        for (final Term term : Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms)) {
            final boolean properNoun = term.getMorphofeat().startsWith("NNP");
            for (final WF word : term.getWFs()) {
                builder.append(atBeginning ? "" : " ");
                builder.append(properNoun ? word.getForm() : word.getForm().toLowerCase());
                atBeginning = false;
            }
        }
        return builder.toString();
    }

    @Nullable

    public static Term extractHead(final KAFDocument document, @Nullable final Span<Term> span) {
        if (span == null) {
            return null;
        }
        Term head = null; // span.getHead(); TODO
        if (head == null) {

            head = document.getTermsHead(span.getTargets()); // (re)compute
        }
        return head;
    }

    public static Set<Term> extractHeads(final KAFDocument document,
            @Nullable final Iterable<Term> ancestors, @Nullable final Iterable<Term> span,
            @Nullable final java.util.function.Predicate<Term> predicate) {

        Set<Term> ancestorSet;
        if (ancestors != null) {
            ancestorSet = ImmutableSet.copyOf(ancestors);
        } else {
            ancestorSet = Sets.newHashSet();
            final Set<Term> termSet = Sets.newHashSet(span);
            for (final Term term : termSet) {
                final Dep dep = document.getDepToTerm(term);
                if (dep == null || !termSet.contains(dep.getFrom())) {
                    ancestorSet.add(term);
                }
            }
        }

        final Set<Term> result = Sets.newHashSet();
        for (final Term ancestor : ancestorSet) {
            extractHeadsHelper(document, ancestor, predicate, result);
        }
        if (span != null) {
            result.retainAll(ImmutableSet.copyOf(span));
        }
        // System.err.println(document.getPublic().uri + " -> " + termFilter + " / " + ancestors
        // + " -> " + result);
        return result;
    }


    //todo adapt POS and DEP (UD)
    private static boolean extractHeadsHelper(final KAFDocument document, final Term term,
            final java.util.function.Predicate<Term> predicate, final Collection<Term> result) {
        final String pos = extendedPos(document, term);
        boolean accepted = false;
        if (pos.startsWith("V")) {
            final Term srlHead = syntacticToSRLHead(document, term);
            if (!term.equals(srlHead)) {
                accepted = extractHeadsHelper(document, srlHead, predicate, result);
            }
        }
        if (!accepted && (predicate == null || predicate.test(term))) {
            result.add(term);
            accepted = true;
        }
        if (accepted) {
            for (final Dep dep : document.getDepsFromTerm(term)) {
                if (dep.getRfunc().toUpperCase().contains("COORD")) {
                    extractHeadsHelper(document, dep.getTo(), predicate, result);
                }
            }
        } else {
            for (final Dep dep : document.getDepsFromTerm(term)) {
                extractHeadsHelper(document, dep.getTo(), predicate, result);
            }
        }
        return accepted;
    }

    public static boolean hasHead(final KAFDocument document, final Object annotation,
            final Term head) {
        List<Span<Term>> spans;
        if (annotation instanceof Coref) {
            spans = ((Coref) annotation).getSpans();
        } else if (annotation instanceof Entity) {
            spans = ((Entity) annotation).getSpans();
        } else if (annotation instanceof Timex3) {
            spans = ImmutableList.of(KAFDocument.newTermSpan(document
                    .getTermsByWFs(((Timex3) annotation).getSpan().getTargets())));
        } else if (annotation instanceof Predicate) {
            spans = ImmutableList.of(((Predicate) annotation).getSpan());
        } else if (annotation instanceof Role) {
            spans = ImmutableList.of(((Role) annotation).getSpan());
        } else {
            throw new IllegalArgumentException("Unsupported annotation: " + annotation);
        }
        for (final Span<Term> span : spans) {
            if (head == extractHead(document, span)) {
                return true;
            }
        }
        return false;
    }

    public static Span<Term> getNominalSpan(final KAFDocument document, final Term term,
            final boolean includeCoord, final boolean includeModifiers) {

        // Start from the supplied term
        final Set<Term> terms = Sets.newHashSet(term);

        // Identify head and terms of all NE and TIMEX markables containing supplied term
        final Map<Term, List<Term>> markables = Maps.newHashMap();
        for (final Entity entity : document.getEntitiesByTerm(term)) {
            markables.put(document.getTermsHead(entity.getTerms()), entity.getTerms());
        }
        for (final WF wf : term.getWFs()) {
            for (final Timex3 timex : document.getTimeExsByWF(wf)) {
                final List<Term> span = document.getTermsByWFs(timex.getSpan().getTargets());
                markables.put(document.getTermsHead(span), span);
            }
        }

        // Add the terms of the smallest markable 'matching' the term (i.e., whose head matches
        // the term or a term ancestor in the dependency tree)
        if (!markables.isEmpty()) {
            Term t = term;
            while (true) {
                final List<Term> parent = markables.get(t);
                if (parent != null) {
                    terms.addAll(parent);
                    break;
                }
                final Dep dep = document.getDepToTerm(t);
                if (dep == null) {
                    break;
                }
                t = dep.getFrom();
            }
        }

        // Identify head
        final Term head = document.getTermsHead(terms);

        // Add all terms reachable from the head using a regex
        final String regex = includeCoord ? includeModifiers ? "(COORD CONJ?)* ((NAME|NMOD|AMOD|TMP) .*)?"
                : "(COORD CONJ?)* NAME"
                : includeModifiers ? "((NAME|NMOD|AMOD|TMP) .*)?" : "NAME";
        terms.addAll(document.getTermsByDepAncestors(Collections.singleton(head), regex));

        // Sort obtained terms by offset and return resulting list
        return KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms),
                head);
    }

    @Nullable
    public static String extractLemma(@Nullable final String rolesetOrRole) {
        if (rolesetOrRole == null) {
            return null;
        }
        int index = rolesetOrRole.indexOf('.');
        if (index < 0) {
            index = rolesetOrRole.indexOf('@');
        }
        return (index >= 0 ? rolesetOrRole.substring(0, index) : rolesetOrRole).toLowerCase();
    }

    @Nullable
    public static Integer extractSense(@Nullable final String rolesetOrRole) {
        if (rolesetOrRole == null) {
            return null;
        }
        final int start = Math.max(0, rolesetOrRole.indexOf('.') + 1);
        int end = rolesetOrRole.indexOf('@');
        end = end > 0 ? end : rolesetOrRole.length();
        try {
            return Integer.valueOf(rolesetOrRole.substring(start, end));
        } catch (final Throwable ex) {
            return null;
        }
    }

    @Nullable
    public static Integer extractArgNum(@Nullable final String role) {
        if (role == null) {
            return null;
        }
        int index = role.length();
        while (index > 0 && Character.isDigit(role.charAt(index - 1))) {
            --index;
        }
        return index == role.length() ? null : Integer.valueOf(role.substring(index));
    }

    // OFFSETS

    public static int getBegin(final Term term) {
        return term.getOffset();
    }

    public static int getEnd(final Term term) {
        final List<WF> wfs = term.getWFs();
        final WF wf = wfs.get(wfs.size() - 1);
        final String str = wf.getForm();
        if (str.equals("-LSB-") || str.equals("-RSB-") || str.equals("''")) {
            return wf.getOffset() + 1;
        }
        return wf.getOffset() + wf.getLength();
    }

    public static int getLength(final Term term) {
        return getEnd(term) - term.getOffset();
    }

    public static String getRoleset(final Predicate predicate) {
        final String res = predicate.getTerms().get(0).getPos().equalsIgnoreCase("V") ? RESOURCE_PROPBANK
                : RESOURCE_NOMBANK;
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
        return roleset;
    }

    // EXTERNAL REFS

    @Nullable
    public static ExternalRef getRef(@Nullable final Object annotation,
            @Nullable final String resource, @Nullable final String reference) {
        ExternalRef result = null;
        for (final ExternalRef ref : getRefs(annotation)) {
            if (matchRef(ref, resource, reference)) {
                if (result != null) {
                    throw new IllegalStateException("Multiple ExternalRef matched for resource "
                            + resource + ", reference " + reference + ": " + ref.getReference()
                            + ", " + result.getReference());
                }
                result = ref;
            }
        }
        return result;
    }

    public static List<ExternalRef> getRefs(final Object annotation,
            @Nullable final String resource, @Nullable final String reference) {
        final List<ExternalRef> result = Lists.newArrayList();
        for (final ExternalRef ref : getRefs(annotation)) {
            if (matchRef(ref, resource, reference)) {
                result.add(ref);
            }
        }
        return result;
    }

    public static void removeRefs(final Object annotation, @Nullable final String resource,
            @Nullable final String reference) {
        final List<ExternalRef> refs = getRefs(annotation);
        for (final Iterator<ExternalRef> i = refs.iterator(); i.hasNext();) {
            final ExternalRef ref = i.next();
            if (matchRef(ref, resource, reference)) {
                i.remove();
            }
        }
    }

    public static void addRef(final Object annotation, final ExternalRef ref) {
        getRefs(annotation).add(ref);
    }

    public static void setRef(final Object annotation, final ExternalRef ref) {
        removeRefs(annotation, ref.getResource(), ref.getReference());
        getRefs(annotation).add(ref);
    }

    public static String toString(final Object annotation) {
        if (annotation instanceof Term) {
            final Term term = (Term) annotation;
            return "term " + term.getId() + " '" + term + "'";
        } else if (annotation instanceof Entity) {
            final Entity entity = (Entity) annotation;
            return "entity " + entity.getId() + " '" + entity.getStr() + "'";
        } else if (annotation instanceof Timex3) {
            final Timex3 timex = (Timex3) annotation;
            return "timex " + timex.getId() + " '" + timex.getSpan().getStr() + "'";
        } else if (annotation instanceof Predicate) {
            final Predicate pred = (Predicate) annotation;
            return "predicate " + pred.getId() + " '" + pred.getSpan().getStr() + "'";
        } else if (annotation instanceof Role) {
            final Role role = (Role) annotation;
            return "role " + role.getId() + " '" + role.getStr() + "' (" + role.getSemRole() + ")";
        } else if (annotation instanceof Opinion) {
            return "opinion " + ((Opinion) annotation).getId();
        } else if (annotation instanceof OpinionTarget) {
            return "opinion target '" + ((OpinionTarget) annotation).getSpan().getStr() + "'";
        } else if (annotation instanceof OpinionHolder) {
            return "opinion holder '" + ((OpinionHolder) annotation).getSpan().getStr() + "'";
        } else if (annotation instanceof OpinionExpression) {
            return "opinion expression '" + ((OpinionExpression) annotation).getSpan().getStr()
                    + "'";
        } else if (annotation instanceof Factuality) {
            final Factuality fact = (Factuality) annotation;
            return "factuality " + fact.getId() + " '" + fact.getWord().getStr() + "'";
        } else if (annotation instanceof Coref) {
            return "coref " + ((Coref) annotation).getId();
        } else {
            throw new IllegalArgumentException("Unsupported annotation object: " + annotation);
        }
    }

    private static List<ExternalRef> getRefs(final Object annotation) {
        List<ExternalRef> refs = ImmutableList.of();
        if (annotation instanceof Term) {
            refs = ((Term) annotation).getExternalRefs();
        } else if (annotation instanceof Entity) {
            refs = ((Entity) annotation).getExternalRefs();
        } else if (annotation instanceof Predicate) {
            refs = ((Predicate) annotation).getExternalRefs();
        } else if (annotation instanceof Role) {
            refs = ((Role) annotation).getExternalRefs();
        } else if (annotation instanceof Opinion) {
            refs = ((Opinion) annotation).getExternalRefs();
        } else if (annotation instanceof OpinionExpression) {
            refs = ((OpinionExpression) annotation).getExternalRefs();
        } else if (annotation instanceof OpinionTarget) {
            refs = ((OpinionTarget) annotation).getExternalRefs();
        } else if (annotation instanceof OpinionHolder) {
            refs = ((OpinionHolder) annotation).getExternalRefs();
        } else {
            throw new IllegalArgumentException("Unsupported annotation object: " + annotation);
        }
        return refs;
    }

    private static boolean matchRef(final ExternalRef ref, @Nullable final String resource,
            @Nullable final String reference) {
        return (resource == null || resource.equalsIgnoreCase(ref.getResource()))
                && (reference == null || reference.equals(ref.getReference()));
    }

    private static void normalizeRefs(final Collection<ExternalRef> refs) {
        final Set<String> seen = Sets.newHashSet();
        for (final Iterator<ExternalRef> i = refs.iterator(); i.hasNext();) {
            final ExternalRef ref = i.next();
            final String key = ref.getResource() + "|" + ref.getReference();
            if (!seen.add(key)) {
                i.remove();
            }
        }
    }

    public static List<Range> termRangesFor(final KAFDocument document, final Iterable<Term> terms) {
        final List<Range> ranges = Lists.newArrayList();
        int startIndex = -1;
        int lastIndex = -2;
        for (final Term term : Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms)) {
            final int termIndex = document.getTerms().indexOf(term);
            if (termIndex - lastIndex > 1) {
                if (startIndex >= 0) {
                    ranges.add(Range.create(startIndex, lastIndex + 1));
                }
                startIndex = termIndex;
            }
            lastIndex = termIndex;
        }
        if (startIndex != -1 && lastIndex >= startIndex) {
            ranges.add(Range.create(startIndex, lastIndex + 1));
        }
        return ranges;
    }

    public static List<Range> rangesFor(final KAFDocument document, final Iterable<Term> terms) {
        final List<Range> ranges = Lists.newArrayList();
        int startOffset = -1;
        int endOffset = -1;
        int termIndex = -2;
        for (final Term term : Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms)) {
            final int lastTermIndex = termIndex;
            termIndex = document.getTerms().indexOf(term);
            if (termIndex - lastTermIndex > 1) {
                if (startOffset != -1) {
                    ranges.add(Range.create(startOffset, endOffset));
                }
                startOffset = term.getOffset();
            }
            endOffset = NAFUtils.getEnd(term);
        }
        if (startOffset != -1 && endOffset > startOffset) {
            ranges.add(Range.create(startOffset, endOffset));
        }
        return ranges;
    }

    public static Range rangeFor(final Term term) {
        return Range.create(NAFUtils.getBegin(term), NAFUtils.getEnd(term));
    }

    public static Range rangeFor(final Iterable<Term> terms) {
        int begin = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (final Term term : terms) {
            begin = Math.min(begin, getBegin(term));
            end = Math.max(end, getEnd(term));
        }
        return Range.create(begin, end);
    }

    @Nullable
    public static Span<Term> trimSpan(@Nullable final Span<Term> span, final int sentenceID) {
        if (span == null || span.isEmpty()) {
            return null;
        }
        boolean sameSentence = true;
        for (final Term term : span.getTargets()) {
            if (term.getSent() != sentenceID) {
                sameSentence = false;
                break;
            }
        }
        if (sameSentence) {
            return span;
        }
        final List<Term> filteredTerms = Lists.newArrayList();
        for (final Term term : span.getTargets()) {
            if (term.getSent() == sentenceID) {
                filteredTerms.add(term);
            }
        }
        final Span<Term> result = KAFDocument.newTermSpan(filteredTerms);
        for (final Term head : span.getHeads()) {
            if (head.getSent() == sentenceID) {
                result.getHeads().add(head);
            }
        }
        return result;
    }

    // Span methods

    public static Span<Term> normalizeSpan(final KAFDocument document,
            @Nullable final Span<Term> span) {

        // Handle null and empty span
        if (span == null || Iterables.isEmpty(span.getTargets())) {
            return KAFDocument.newTermSpan();
        }

        // Identify all the 'root' terms in the span whose dep tree parent is outside the span
        final Set<Term> roots = Sets.newHashSet();
        final Set<Term> terms = ImmutableSet.copyOf(span.getTargets());
        for (final Term term : terms) {
            final Dep dep = document.getDepToTerm(term);
            if (dep == null || !terms.contains(dep.getFrom())) {
                roots.add(term);
            }
        }

        // If only one 'root', return the normalized span having that root as the head
        if (roots.size() == 1) {
            return KAFDocument.newTermSpan(span.getTargets(), roots.iterator().next());
        }

        // Otherwise, look for the closest head outside the span. First compute all the paths from
        // the dep tree roots to the 'root' terms identified before
        final List<List<Term>> paths = Lists.newArrayList();
        for (final Term root : roots) {
            final List<Term> path = Lists.newArrayList(root);
            for (Dep dep = document.getDepToTerm(root); dep != null; dep = document
                    .getDepToTerm(dep.getFrom())) {
                path.add(dep.getFrom());
            }
            Collections.reverse(path);
            paths.add(path);
        }

        // Then look for the deepest node common to all those paths
        int depth = 0;
        Term externalHead = null;
        outer: for (; depth < paths.get(0).size(); ++depth) {
            final Term t = paths.get(0).get(depth);
            for (int i = 1; i < paths.size(); ++i) {
                final List<Term> path = paths.get(i);
                if (depth >= path.size() || !path.get(depth).equals(t)) {
                    break outer;
                }
            }
            externalHead = t;
        }

        // If found, compute the terms for the external span
        Set<Term> externalTerms = null;
        if (externalHead != null) {
            externalTerms = Sets.newHashSet(terms);
            externalTerms.add(externalHead);
            for (final List<Term> path : paths) {
                externalTerms.addAll(path.subList(depth, path.size()));
            }
        }

        // Now look for the internal head that covers the most part terms of the span. Start by
        // associating to each candidate internal head the terms it would cover
        final Multimap<Term, Term> map = HashMultimap.create();
        for (final Term term : terms) {
            Dep dep = document.getDepToTerm(term);
            if (dep == null) {
                map.put(term, term);
            } else {
                for (; dep != null; dep = document.getDepToTerm(dep.getFrom())) {
                    if (!terms.contains(dep.getFrom())) {
                        map.put(dep.getTo(), term);
                        break;
                    }
                }
            }
        }

        // Then identify the best internal head
        Term internalHead = null;
        Collection<Term> internalTerms = null;
        for (final Map.Entry<Term, Collection<Term>> entry : map.asMap().entrySet()) {
            if (internalHead == null || entry.getValue().size() >= internalTerms.size()) {
                internalTerms = entry.getValue();
                internalHead = entry.getKey();
            }
        }

        // Return either the external span (if defined) or the internal one, based on which one is
        // most similar in size to the original span (if equal, prefer external one).
        if (externalTerms != null
                && externalTerms.size() - terms.size() <= terms.size() - internalTerms.size()) {
            return KAFDocument.newTermSpan(
                    Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(externalTerms), externalHead);
        } else {
            return KAFDocument.newTermSpan(
                    Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(internalTerms), internalHead);
        }
    }

    public static List<Span<Term>> mergeSpans(final KAFDocument document,
            final Iterable<Span<Term>> spans, final boolean canAddTerms) {

        // Build a map associating to each span head the other heads it is coordinated with
        final Map<Term, List<Term>> extents = Maps.newHashMap();
        final Map<Term, Set<Term>> clusters = Maps.newHashMap();
        for (final Span<Term> span : spans) {
            final Term head = extractHead(document, span);
            clusters.put(head, Sets.newHashSet(head));
            extents.put(head, span.getTargets());
        }
        for (final Term head : clusters.keySet()) {
            for (Dep dep = document.getDepToTerm(head); dep != null
                    && ("CONJ".equals(dep.getRfunc()) || "COORD".equals(dep.getRfunc())); dep = document
                    .getDepToTerm(dep.getFrom())) {
                if (clusters.keySet().contains(dep.getFrom())) {
                    clusters.get(head).add(dep.getFrom());
                    clusters.get(dep.getFrom()).add(head);
                } else if ("CO".indexOf(dep.getFrom().getPos()) < 0) {
                    break; // don't include intermediate terms that are not conjunctions or commas
                }
            }
        }

        // Create a span for each cluster of heads, including intermediate conjunctions
        final List<Span<Term>> result = Lists.newArrayList();
        while (!clusters.isEmpty()) {
            final Set<Term> heads = clusters.values().iterator().next();
            final Set<Term> terms = Sets.newHashSet();
            Term spanHead = heads.iterator().next();
            for (final Term head : heads) {
                clusters.remove(head);
                terms.addAll(extents.get(head));
                final List<Term> path = Lists.newArrayList();
                for (Dep dep = document.getDepToTerm(head); dep != null; dep = document
                        .getDepToTerm(dep.getFrom())) {
                    final Term term = dep.getFrom();
                    path.add(term);
                    if (heads.contains(term)) {
                        terms.addAll(path);
                        path.clear();
                        spanHead = term;
                    }
                }
            }
            List<Term> spanTerms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
            if (canAddTerms) {
                final List<Term> docTerms = document.getTerms();
                spanTerms = Lists.newArrayList(docTerms.subList(
                        docTerms.indexOf(spanTerms.get(0)),
                        docTerms.indexOf(spanTerms.get(spanTerms.size() - 1)) + 1));
            }
            result.add(KAFDocument.newTermSpan(spanTerms, spanHead));
        }
        return result;
    }

    public static final List<Span<Term>> splitSpans(final KAFDocument document,
            final Iterable<Span<Term>> spans) {

        // Identify all the heads taking coordination into consideration
        final Set<Term> heads = Sets.newHashSet();
        final Set<Term> terms = Sets.newHashSet();
        for (final Span<Term> span : spans) {
            final Term head = extractHead(document, span);
            heads.add(head);
            terms.addAll(span.getTargets());

            final List<Term> queue = Lists.newLinkedList();
            queue.add(head);
            while (!queue.isEmpty()) {
                final Term term = queue.remove(0);
                for (final Dep dep : document.getDepsFromTerm(term)) {
                    final String func = dep.getRfunc();
                    if ("COORD".equals(func) || "CONJ".equals(func)) {
                        final Term t = dep.getTo();
                        queue.add(t);
                        if ("CC".equals(t.getMorphofeat())
                                || !Character.isLetter(t.getMorphofeat().charAt(0))) {
                            heads.add(term);
                        }
                    }
                }
            }
        }

        // Build and return a span for each head
        final Set<Term> excluded = document.getTermsByDepDescendants(heads);
        final List<Span<Term>> result = Lists.newArrayList();
        for (final Term head : heads) {
            final Set<Term> extent = document.getTermsByDepAncestors(ImmutableSet.of(head));
            extent.removeAll(excluded);
            extent.add(head);
            extent.retainAll(terms);
            if (!extent.isEmpty()) {
                result.add(KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR)
                        .sortedCopy(extent), head));
            }
        }
        return result;
    }

    public static final List<Span<Term>> splitSpan(final KAFDocument document,
            final Span<Term> span, final Iterable<Term> heads) {

        final Set<Term> excludedTerms = document.getTermsByDepDescendants(heads);
        final List<Span<Term>> spans = Lists.newArrayList();
        for (final Term head : heads) {
            final Set<Term> terms = document.getTermsByDepAncestors(ImmutableSet.of(head));
            terms.removeAll(excludedTerms);
            terms.add(head);
            terms.retainAll(span.getTargets());
            if (!terms.isEmpty()) {
                spans.add(KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR)
                        .sortedCopy(terms), head));
            }
        }
        return spans;
    }

    // End

    public static KAFDocument readDocument(@Nullable final Path path) throws IOException {
        final KAFDocument document;
        if (path == null) {
            document = KAFDocument.createFromStream(IO.utf8Reader(IO.buffer(System.in)));
            document.getPublic().publicId = "";
        } else {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                document = KAFDocument.createFromStream(reader);
                document.getPublic().publicId = path.toString();
            }
        }
        return document;
    }

    public static void writeDocument(final KAFDocument document, @Nullable final Path location)
            throws IOException {
        if (location == null) {
            System.out.write(document.toString().getBytes(Charsets.UTF_8));
        } else {
            try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(location.toString())))) {
                writer.write(document.toString());
            }
        }
    }

    //todo adapt DEP (UD): check VC and IM
    public static Term syntacticToSRLHead(final KAFDocument document, final Term term) {
        for (final Dep dep : document.getDepsFromTerm(term)) {
            final String func = dep.getRfunc();
            if ("VC".equals(func) || "IM".equals(func)) {
                return syntacticToSRLHead(document, dep.getTo());
            }
        }
        return term;
    }

    public static Term srlToSyntacticHead(final KAFDocument document, final Term term) {
        final Dep dep = document.getDepToTerm(term);
        if (dep != null) {
            final String func = dep.getRfunc();
            if ("VC".equals(func) || "IM".equals(func)) {
                return srlToSyntacticHead(document, dep.getFrom());
            }
        }
        return term;
    }

    // Accounts for demonstrative pronouns

    public static String extendedPos(final KAFDocument document, final Term term) {
        final String pos = term.getMorphofeat();
        final String lemma = term.getLemma().toLowerCase();
        if ("some".equals(lemma) || "many".equals(lemma) || "all".equals(lemma)
                || "few".equals(lemma) || "this".equals(lemma) || "these".equals(lemma)
                || "that".equals(lemma) || "those".equals(lemma)) {
            final Dep dep = document.getDepToTerm(term);
            if (dep == null || !"NMOD".equals(dep.getRfunc())) {
                return pos + "P"; // determiner (DT) or adj (JJ) used as demonstrative pronoun
            }
        }
        return pos;
    }

    public static Boolean isActiveForm(final KAFDocument document, final Term term) {
        final String word = term.getStr().toLowerCase();
        final String pos = term.getMorphofeat();
        if (!pos.startsWith("V")) {
            return null;
        }
        if (word.equals("been") || !pos.equals("VBN")) {
            return Boolean.TRUE;
        }
        return isActiveFormHelper(document, term);
    }

    private static Boolean isActiveFormHelper(final KAFDocument document, final Term term) {
        final Dep dep = document.getDepToTerm(term);
        if (dep == null) {
            return Boolean.FALSE;
        }
        final Term parent = dep.getFrom();
        final String word = parent.getStr().toLowerCase();
        final String pos = parent.getMorphofeat();
        if (pos.startsWith("NN")) {
            return Boolean.FALSE;
        }
        if (word.matches("am|are|is|was|were|be|been|being")) {
            return Boolean.FALSE;
        }
        if (word.matches("ha(ve|s|d|ving)")) {
            return Boolean.TRUE;
        }

        if (pos.matches("VBZ|VBD|VBP|MD")) {
            return Boolean.FALSE;
        }
        return isActiveFormHelper(document, parent);
    }

    public static java.util.function.Predicate<Term> matchExtendedPos(final KAFDocument document,
            final String... posPrefixes) {
        return new java.util.function.Predicate<Term>() {

            @Override
            public boolean test(final Term term) {
                final String pos = extendedPos(document, term);
                for (final String prefix : posPrefixes) {
                    if (pos.startsWith(prefix)) {
                        return true;
                    }
                }
                return false;
            }

        };
    }

    // extracts descendents that are consecutive with the supplied head
    public static Set<Term> getTermsByDepAncestor(final KAFDocument document, final Term head,
            final boolean consecutive) {
        final Set<Term> descendants = document.getTermsByDepAncestors(ImmutableSet.of(head));
        if (consecutive) {
            final List<Term> sortedTerms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(
                    descendants);
            final int[] indexes = new int[sortedTerms.size()];
            for (int i = 0; i < sortedTerms.size(); ++i) {
                indexes[i] = document.getTerms().indexOf(sortedTerms.get(i));
            }
            final int h = sortedTerms.indexOf(head);
            boolean filtered = false;
            for (int i = h + 1; i < indexes.length; ++i) {
                filtered |= indexes[i] > indexes[i - 1] + 1;
                if (filtered) {
                    descendants.remove(sortedTerms.get(i));
                }
            }
            filtered = false;
            for (int i = h - 1; i >= 0; --i) {
                filtered |= indexes[i] < indexes[i + 1] - 1;
                if (filtered) {
                    descendants.remove(sortedTerms.get(i));
                }
            }
        }
        return descendants;
    }


    public static IRI createPreMOnSemanticClassIRIfor(String model, String predicate){

        String prefix = "";
        switch (model) {

            case RESOURCE_FRAMENET : prefix+=PREMON_FNPREFIX+"-"; break;
            case RESOURCE_VERBNET : prefix+=PREMON_VNPREFIX+"-"; break;
            case RESOURCE_PROPBANK  : prefix+=PREMON_PBPREFIX+"-"; break;
            case RESOURCE_NOMBANK  : prefix+=PREMON_NBPREFIX+"-"; break;

        }

        //works for fn15,pb17,vn32,nb10... in case of other version, some cautions have to be take on predicate (e.g.m FedEx or UPS in pb215)
        String localname=prefix+predicate.toLowerCase();

        return Statements.VALUE_FACTORY.createIRI(PREMON_NAMESPACE, localname);

    }


    public static IRI createPreMOnSemanticRoleIRIfor(String model, String predicate, String role){

        String prefix = "";

        //works for fn15,pb17,vn32,nb10... in case of other version, some cautions have to be take on predicate (e.g.m FedEx or UPS in pb215)
        //expect role as follow
        //PB,NB: A0,AA, AM-TMP
        //VB,FN: don't care
        switch (model) {
            case RESOURCE_FRAMENET : prefix+=PREMON_FNPREFIX+"-";
                role=role.toLowerCase();
                break;
            case RESOURCE_VERBNET : prefix+=PREMON_VNPREFIX+"-";
                role=role.toLowerCase();
                break;
            case RESOURCE_PROPBANK  : prefix+=PREMON_PBPREFIX+"-";
                role=role.toLowerCase();//.replace("arg-","a").replace("a","arg");
                if (!role.contains("am-")) role=role.replace("a","arg");
                else role=role.replace("am-","arg");
                break;
            case RESOURCE_NOMBANK  : prefix+=PREMON_NBPREFIX+"-";
                role=role.toLowerCase();//.replace("arg-","a").replace("a","arg");
                if (!role.contains("am-")) role=role.replace("a","arg");
                else role=role.replace("am-","arg");
                break;
        }

        String localname=prefix+predicate.toLowerCase()+PREMON_ARGUMENT_SEPARATOR+role;

        return Statements.VALUE_FACTORY.createIRI(PREMON_NAMESPACE, localname);

    }

}
