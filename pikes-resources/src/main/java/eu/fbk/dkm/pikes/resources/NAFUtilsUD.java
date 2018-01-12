package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Charsets;
import com.google.common.collect.*;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.Statements;
import ixa.kaflib.*;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Opinion.OpinionHolder;
import ixa.kaflib.Opinion.OpinionTarget;
import ixa.kaflib.Predicate.Role;
import org.eclipse.rdf4j.model.IRI;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class NAFUtilsUD {

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

    public static final Map<String, String> ENTITY_SST_TO_TYPES = ImmutableMap
            .<String, String>builder().put("person", "PER").put("group", "ORG")
            .put("location", "LOC").put("quantity", "QUANTITY").put("artifact", "PRODUCT")
            .put("act", "EVENT").put("event", "EVENT").put("phenomenon", "EVENT")
            .put("process", "EVENT").put("state", "EVENT").put("animal", "MISC")
            .put("plant", "MISC").put("body", "MISC").put("shape", "MISC").put("motive", "MISC")
            .put("object", "MISC").put("substance", "MISC").build();

    private static final Pattern WF_EXCLUSION_PATTERN = Pattern.compile("[^A-Za-z0-9]*");

    private static final Set<String> SYMBOLS = ImmutableSet.of("$", "#", "&", "€");



    //todo may not need to be changed for UD
    @Nullable
    public static Term extractHeadWithFlat(final KAFDocument document, @Nullable final Span<Term> span) {
        if (span == null) {
            return null;
        }
        Term head = null;
        if (head == null) {

            head = document.getTermsHead(span.getTargets()); // (re)compute
        }
        //if head is still null it may be a flat case (e.g. President Barack Obama)
        if (head == null) {

            Term externalHead=document.getDepToTerm(span.getTargets().get(0)).getFrom();
            for (Term term:span.getTargets()
                 ) {
                Dep dep = document.getDepToTerm(term);
                if (!dep.getRfunc().equalsIgnoreCase("flat")) return null; //if not flat, exit
                else if (!dep.getFrom().equals(externalHead)) return null; //if different heads, problem
            }
            //if we are here, all terms have the same external head ==> it's a flat cluster ==> we pick as head the last Term
            head = Ordering.from(Term.OFFSET_COMPARATOR).reverse().sortedCopy(span.getTargets()).get(0);
        }

        return head;
    }









    //todo may not need to be changed for UD
    @Nullable
    public static Term extractHead(final KAFDocument document, @Nullable final Span<Term> span) {
        if (span == null) {
            return null;
        }
        Term head = null;
        if (head == null) {

            head = document.getTermsHead(span.getTargets()); // (re)compute
        }
        return head;
    }

    //todo may not need to be changed for UD
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


    //todo adapt DEP (UD)
    private static boolean extractHeadsHelper(final KAFDocument document, final Term term,
            final java.util.function.Predicate<Term> predicate, final Collection<Term> result) {

        final String pos = term.getUpos();
        boolean accepted = false;

        if (pos.equals("VERB")) {
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

    //shouldn't change with UD
    //check if head is the head of the given annotation
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

    //todo check with FRA what for...
    //WE MAY NOT NEED IT ANYMORE, or at least the part on modifiers and coordination
    public static Span<Term> getNominalSpan(final KAFDocument document, final Term term) {

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

//        // Add all terms reachable from the head using a regex
//        final String regex = includeCoord
//                ? includeModifiers
//                    ? "(COORD CONJ?)* ((NAME|NMOD|AMOD|TMP) .*)?"
//                    : "(COORD CONJ?)* NAME"
//                : includeModifiers
//                    ? "((NAME|NMOD|AMOD|TMP) .*)?"
//                    : "NAME";
//        terms.addAll(document.getTermsByDepAncestors(Collections.singleton(head), regex));

        // Sort obtained terms by offset and return resulting list
        return KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms),
                head);
    }


/*

BELOW HERE METHODS ARE UD-SAFE

 */

    public static boolean areTermsOverlappingTimex (KAFDocument document, List<Term> terms) {

        Set<WF> wfs = new HashSet<>();
        for (Term term : terms
             ) {
            wfs.addAll(term.getWFs());
        }

        for (final WF wf : wfs) {
            //getTimeExsByWF seems to be broken....
            final List<Timex3> timex = document.getTimeExsByWF(wf);
            if (!timex.isEmpty()) return true;
//                if (LOGGER.isDebugEnabled()) {
//                    LOGGER.debug("Removed " + NAFUtilsUD.toString(entity)
//                            + " overlapping with TIMEX3 '" + NAFUtilsUD.toString(timex));
//                }
//                continue outer;
//            }

        }

        return false;
    }



    public static Entity trimEntitySpan(KAFDocument document, Entity entity){

        // Remove initial determiners and prepositions, plus all the terms not containing at
        // least a letter or a digit. Move to next entity if no change was applied
        final List<Term> filteredTerms = NAFUtilsUD.filterTerms(entity.getTerms());
        if (filteredTerms.size() == entity.getTerms().size()) {
            return entity;
        }

        // Remove the old entity
        document.removeAnnotation(entity);

        // If some term remained, add the filtered entity, reusing old type, named flag and
        // external references
        Entity newEntity = null;
        if (!filteredTerms.isEmpty()) {
            newEntity = document.newEntity(ImmutableList.of(KAFDocument
                    .newTermSpan(filteredTerms)));
            newEntity.setType(entity.getType());
            newEntity.setNamed(entity.isNamed());
            for (final ExternalRef ref : entity.getExternalRefs()) {
                newEntity.addExternalRef(ref);
            }
        }

        return newEntity;
    }



    public static Timex3 trimTimexSpan(KAFDocument document, Timex3 timex){

        // Remove initial determiners and prepositions, plus all the terms not containing at
        // least a letter or a digit.
        List<Term> timexTerms = document.getTermsByWFs(timex.getSpan().getTargets());

        final List<Term> filteredTerms = NAFUtilsUD.filterTerms(timexTerms);
        if (filteredTerms.size() == timexTerms.size()) {
            return timex;
        }

        List<WF> filteredWFs = new ArrayList<>();
        for (Term term:filteredTerms
             ) {
            filteredWFs.addAll(term.getWFs());
        }

        // Remove the old times
        document.removeAnnotation(timex);

        // If some term remained, add the filtered times, reusing old type and value
        Timex3 newTimex = null;
        if (!filteredTerms.isEmpty()) {
            newTimex = document.newTimex3(KAFDocument
                    .newWFSpan(filteredWFs),timex.getType());
            newTimex.setValue(timex.getValue());
        }

        return newTimex;
    }


    public static LinkedEntity trimLinkedEntitySpan(KAFDocument document, LinkedEntity linkedEntity){

        // Remove initial determiners and prepositions, plus all the terms not containing at
        // least a letter or a digit.
        final List<Term> terms = document.getTermsByWFs(linkedEntity.getWFs().getTargets());


        final List<Term> filteredTerms = NAFUtilsUD.filterTerms(terms);
        if (filteredTerms.size() == terms.size()) {
            return linkedEntity;
        }

        List<WF> filteredWFs = new ArrayList<>();
        for (Term term:filteredTerms
                ) {
            filteredWFs.addAll(term.getWFs());
        }

        // Remove the old
        document.removeAnnotation(linkedEntity);

        // If some term remained, add the filtered times, reusing old type and value
        LinkedEntity newLinkedEntity = null;
        if (!filteredTerms.isEmpty()) {
            newLinkedEntity = document.newLinkedEntity(KAFDocument
                    .newWFSpan(filteredWFs));
            newLinkedEntity.setResource(linkedEntity.getResource());
            newLinkedEntity.setReference(linkedEntity.getReference());
            newLinkedEntity.setConfidence(linkedEntity.getConfidence());
            newLinkedEntity.setSpotted(linkedEntity.isSpotted());
            newLinkedEntity.setTypes(linkedEntity.getTypes());
        }

        return newLinkedEntity;
    }



    public static boolean isSpanConsecutive(KAFDocument document, final Span<Term> span) {

        final List<Term> sortedTerms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(span.getTargets());
        final List<Term> sentenceTerms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(document.getTermsBySent(sortedTerms.get(0).getSent()));
        return (Collections.indexOfSubList(sentenceTerms , sortedTerms) != -1); //restricted to the sentence

    }





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

            final String pos = term.getUpos().toUpperCase();
            if (atBeginning && (pos.equals("DET") || pos.equals("PRON") || pos.equals("PART"))) {
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

            final boolean properNoun = term.getUpos().equals("PROPN");
            for (final WF word : term.getWFs()) {
                builder.append(atBeginning ? "" : " ");
                builder.append(properNoun ? word.getForm() : word.getForm().toLowerCase());
                atBeginning = false;
            }
        }
        return builder.toString();
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
        //replace next with for UD pipe
        final String res = predicate.getTerms().get(0).getUpos().equalsIgnoreCase("VERB") ? RESOURCE_PROPBANK
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
        } else if (annotation instanceof LinkedEntity) {
            final LinkedEntity entity = (LinkedEntity) annotation;
            return "entity " + entity.getId() + " '" + entity.getSpanStr() + "'";
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


    public static Term syntacticToSRLHead(final KAFDocument document, final Term term) {
//    may not be needed anymore with UD, verb chain and infinite marker are handled differently (i.e. they are indirectly attached to the heads)
//        for (final Dep dep : document.getDepsFromTerm(term)) {
//            final String func = dep.getRfunc();
//
//            if ("VC".equals(func) || "IM".equals(func)) {
//                return syntacticToSRLHead(document, dep.getTo());
//            }
//        }
        return term;
    }


    public static IRI createPreMOnConceptualizationIRIfor(String model, String predicate, String lemma, String upos){

        //works for fn15,pb17,vn32,nb10... in case of other version, some cautions have to be taken on predicate (e.g.m FedEx or UPS in pb215)
        String prefix = "co";
        String pos="";
        switch (upos.toLowerCase()) {
            case "noun" : pos="n"; break;
            case "propn" : pos="n"; break; //added for some capitalization issues
            case "verb" : pos="v"; break;
            case "adj" : pos="adj"; break;
            case "adv" : pos="adv"; break;
        }
        lemma=lemma.toLowerCase();
        if (pos.isEmpty()||lemma.isEmpty()) return null;
        prefix = prefix+"-"+pos+"-"+lemma+"-";
        switch (model) {
            case RESOURCE_FRAMENET : prefix+=PREMON_FNPREFIX+"-"; break;
            case RESOURCE_VERBNET : prefix+=PREMON_VNPREFIX+"-"; break;
            case RESOURCE_PROPBANK  : prefix+=PREMON_PBPREFIX+"-"; break;
            case RESOURCE_NOMBANK  : prefix+=PREMON_NBPREFIX+"-"; break;
        }
        String localname=prefix+predicate.toLowerCase();
        return Statements.VALUE_FACTORY.createIRI(PREMON_NAMESPACE, localname);
    }

    public static IRI createPreMOnSemanticClassIRIfor(String model, String predicate){

        //works for fn15,pb17,vn32,nb10... in case of other version, some cautions have to be taken on predicate (e.g.m FedEx or UPS in pb215)
        String prefix = "";
        switch (model) {
            case RESOURCE_FRAMENET : prefix+=PREMON_FNPREFIX+"-"; break;
            case RESOURCE_VERBNET : prefix+=PREMON_VNPREFIX+"-"; break;
            case RESOURCE_PROPBANK  : prefix+=PREMON_PBPREFIX+"-"; break;
            case RESOURCE_NOMBANK  : prefix+=PREMON_NBPREFIX+"-"; break;
        }
        String localname=prefix+predicate.toLowerCase();
        return Statements.VALUE_FACTORY.createIRI(PREMON_NAMESPACE, localname);
    }


    public static IRI createPreMOnSemanticRoleIRIfor(String model, String predicate, String role){

        //works for fn15,pb17,vn32,nb10... in case of other version, some cautions have to be taken on predicate (e.g.m FedEx or UPS in pb215)
        //expect role as follow
        //PB,NB: A0,AA, AM-TMP
        //VB,FN: don't care
        String prefix = "";
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
