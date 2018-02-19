package eu.fbk.dkm.pikes.resources;

import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.svm.Util;
import ixa.kaflib.*;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Opinion.OpinionHolder;
import ixa.kaflib.Opinion.OpinionTarget;
import ixa.kaflib.Predicate;
import ixa.kaflib.Predicate.Role;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A filter for the post-processing of a NAF document.
 * <p>
 * The filter, configured and created using the builder pattern (see {@link #builder()}), performs
 * several optional and configurable operations on a {@code NAFDocumant} that is modified in
 * place. For the operations supported please refer to the javadoc of {@code Builder}.
 * <p>
 * This class is thread-safe.
 * </p>
 */
public final class NAFFilterUD implements Consumer<KAFDocument> {

    public static final String SUMO_NAMESPACE = "http://www.ontologyportal.org/SUMO.owl#";

    public static final IRI SUMO_PROCESS = SimpleValueFactory.getInstance().createIRI(SUMO_NAMESPACE, "Process");

    private static final Logger LOGGER = LoggerFactory.getLogger(NAFFilterUD.class);

    private static final Pattern SRL_ROLE_PATTERN = Pattern.compile("A(\\d).*");

    //todo next one will change with UD
    private static final String PARTICIPATION_REGEX = ""
            + "SUB? (COORD CONJ?)* (PMOD (COORD CONJ?)*)? ((VC OPRD?)|(IM OPRD?))*";

    private static final String[] LINKING_STOP_WORDS;

    private static final BiMap<String, String> MAPPING_PREFIXES = ImmutableBiMap.of("propbank",
            "pb", "nombank", "nb", "verbnet", "vn", "framenet", "fn");

    public static final NAFFilterUD DEFAULT = NAFFilterUD.builder().build();

    static {
        List<String> stopwords = Collections.emptyList();
        try {
            stopwords = Resources.readLines(NAFFilterUD.class.getResource("linking_stopwords"),
                    Charsets.UTF_8);
            LOGGER.info("Loaded {} linking stopwords", stopwords.size());
        } catch (final IOException ex) {
            LOGGER.error("Could not load linking stopwords", ex);
        }
        LINKING_STOP_WORDS = stopwords.toArray(new String[stopwords.size()]);
        for (int i = 0; i < LINKING_STOP_WORDS.length; ++i) {
            LINKING_STOP_WORDS[i] = LINKING_STOP_WORDS[i].toLowerCase();
        }
        Arrays.sort(LINKING_STOP_WORDS);

    }

    private final boolean termSenseFiltering;

    private final boolean termSenseCompletion;

    private final boolean linkingFixing;

    private final boolean srlPreprocess;

    private final boolean srlEnableMate;

    private final boolean srlEnableSemafor;

    private final boolean srlRemoveUnknownPredicates;

    private final boolean srlPredicateAddition;

    private final boolean srlSelfArgFixing;

    private final boolean srlPreMOnIRIs;

    private final boolean depFixFlatHeads;

    private NAFFilterUD(final Builder builder) {

        this.depFixFlatHeads = MoreObjects.firstNonNull(builder.depFixFlatHeads, true);
        this.termSenseFiltering = MoreObjects.firstNonNull(builder.termSenseFiltering, true);
        this.termSenseCompletion = MoreObjects.firstNonNull(builder.termSenseCompletion, true);
        this.linkingFixing = MoreObjects.firstNonNull(builder.linkingFixing, false);
        this.srlPreprocess = MoreObjects.firstNonNull(builder.srlPreprocess, true);
        this.srlEnableMate = MoreObjects.firstNonNull(builder.srlEnableMate, true);
        this.srlEnableSemafor = MoreObjects.firstNonNull(builder.srlEnableSemafor, true);
        this.srlRemoveUnknownPredicates = MoreObjects.firstNonNull(
                builder.srlRemoveUnknownPredicates, false);
        this.srlPredicateAddition = MoreObjects.firstNonNull(builder.srlPredicateAddition, true);
        this.srlSelfArgFixing = MoreObjects.firstNonNull(builder.srlSelfArgFixing, true);

        this.srlPreMOnIRIs = MoreObjects.firstNonNull(builder.srlPreMOnIRIs,
                true);
    }

    @Override
    public void accept(final KAFDocument document) {
        filter(document);
    }

    /**
     * Filters the NAF document specified (the document is modified in-place). Filtering is
     * controlled by the flags specified when creating the {@code NAFFilter} object.
     *
     * @param document
     *            the document to filter
     */
    public void filter(final KAFDocument document) {

        // Check arguments
        Preconditions.checkNotNull(document);

        // Log beginning of operation
        final long ts = System.currentTimeMillis();
        LOGGER.debug("== Filtering {} ==", document.getPublic().uri);

        // Normalize the document
        NAFUtilsUD.normalize(document);

        if (this.depFixFlatHeads) {
            applyDepFixFlatHeads(document);
        }

        // Term-level filtering
        if (this.termSenseFiltering) {
            applyTermSenseFiltering(document);
        }
        if (this.termSenseCompletion) {
            applyTermSenseCompletion(document);
        }
        if (this.linkingFixing) {
            applyLinkingFixing(document);
        }

        // SRL-level filtering
        if (this.srlPreprocess) {
            applySRLPreprocess(document);
        }
        if (this.srlRemoveUnknownPredicates) {
            applySRLRemoveUnknownPredicates(document);
        }
        if (this.srlPredicateAddition) {
            applySRLPredicateAddition(document);
        }
        if (this.srlSelfArgFixing) {
            applySRLSelfArgFixing(document);
        }

        //added for replacing with premon IRIs
        if (this.srlPreMOnIRIs) {
            applySRLPreMOnIRIs(document);
        }

        LOGGER.debug("Done in {} ms", System.currentTimeMillis() - ts);
    }



    private void applyDepFixFlatHeads(final KAFDocument document) {


        List<Set<Term>> equivalences = new ArrayList<>();
        for (Dep dep : document.getDeps()){
            if (dep.getRfunc().toLowerCase().startsWith("flat")) {
                Term to = dep.getTo();
                Term from = dep.getFrom();
                boolean found = false;
                for (Set set : equivalences
                        ) {
                    if ((set.contains(to)) || (set.contains(from))) {
                        set.add(to);
                        set.add(from);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Set set = new HashSet<>();
                    set.add(to);
                    set.add(from);
                    equivalences.add(set);
                }
            }
        }

        //get the new heads
        Map<Term,Term> newHeads = new HashMap<>();
        for(Set<Term> set : equivalences){
            //found token with the highest ID
            Term head = set.stream().max(Comparator.comparing(term -> term.getWFs().get(0).getOffset())).get();
            for (Term t:set
                 ) {
                newHeads.put(t,head);
            }
        }

        //reprocess the dependency tree to fix arcs

        for (Dep dep : document.getDeps()){

            Term from = dep.getFrom();
            Term to = dep.getTo();

            if (!newHeads.containsKey(from)) {
                if (!newHeads.containsKey(to)) {
                    //we don't change anything
                } else {
                    dep.setTo(newHeads.get(to));
                }
            } else {
                if (!newHeads.containsKey(to)) {
                    dep.setFrom(newHeads.get(from));
                } else {
                    if(newHeads.get(to).equals(newHeads.get(from))) {
                        //same cluster
                        dep.setFrom(newHeads.get(from));
                        if (newHeads.get(from).equals(to)) dep.setTo(from);
                    } else {
                        //different clusters
                        dep.setTo(newHeads.get(to));
                        dep.setFrom(newHeads.get(from));
                    }
                }

            }


        }

    }



    private void applySRLPreprocess(final KAFDocument document) {

        // Allocate two maps to store term -> predicate pairs
        final Map<Term, Predicate> matePredicates = Maps.newHashMap();
        final Map<Term, Predicate> semaforPredicates = Maps.newHashMap();

        // Remove predicates with invalid head todo - can this case happen really?
        for (final Predicate predicate : ImmutableList.copyOf(document.getPredicates())) {
            if (NAFUtilsUD.extractHead(document, predicate.getSpan()) == null) {
                document.removeAnnotation(predicate);
                LOGGER.debug("Removed {} without valid head term", predicate);
            }
        }

        // Remove predicates from non-enabled tools (Mate, Semafor)
        for (final Predicate predicate : Lists.newArrayList(document.getPredicates())) {
            final boolean isSemafor = predicate.getId().startsWith("f_pr")
                    || "semafor".equalsIgnoreCase(predicate.getSource());
            if (isSemafor && !this.srlEnableSemafor || !isSemafor && !this.srlEnableMate) {
                document.removeAnnotation(predicate);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removed " + NAFUtilsUD.toString(predicate) + " (disabled)");
                }
            } else {
                // todo the head of a predicate should be within the predicate even with UD
                final Term term = NAFUtilsUD.extractHead(document, predicate.getSpan());
                (isSemafor ? semaforPredicates : matePredicates).put(term, predicate);
            }
        }

        // todo This part below is not needed anymore in KEM
//        // For each Semafor predicate, merge a corresponding Mate predicate for the same term
//        for (final Map.Entry<Term, Predicate> entry : semaforPredicates.entrySet()) {
//            final Term term = entry.getKey();
//            final Predicate semaforPredicate = entry.getValue();
//            final Predicate matePredicate = matePredicates.get(term);
//            if (matePredicate != null) {
//
//                // Determine whether FrameNet predicate corresponds (-> FN data can be merged)
//                final ExternalRef semaforRef = NAFUtilsUD.getRef(semaforPredicate, "FrameNet", null);
//                final ExternalRef mateRef = NAFUtilsUD.getRef(matePredicate, "FrameNet", null);
//                final boolean mergeFramenet = semaforRef != null && mateRef != null
//                        && semaforRef.getReference().equalsIgnoreCase(mateRef.getReference());
//
//                // Merge predicate types
//                for (final ExternalRef ref : NAFUtilsUD.getRefs(matePredicate, null, null)) {
//                    if (!ref.getResource().equalsIgnoreCase("FrameNet")) {
//                        NAFUtilsUD.addRef(semaforPredicate, new ExternalRef(ref));
//                    }
//                }
//
//                // Merge roles
//                // todo here we assume the span from various tools are the same... may not be a problem as long as we don't have mate
//                for (final Role mateRole : matePredicate.getRoles()) {
//                    boolean addRole = true;
//                    final Set<Term> mateTerms = ImmutableSet.copyOf(mateRole.getSpan()
//                            .getTargets());
//                    for (final Role semaforRole : semaforPredicate.getRoles()) {
//                        final Set<Term> semaforTerms = ImmutableSet.copyOf(semaforRole.getSpan()
//                                .getTargets());
//                        if (mateTerms.equals(semaforTerms)) {
//                            addRole = false;
//                            semaforRole.setSemRole(mateRole.getSemRole());
//                            final boolean addFramenetRef = mergeFramenet
//                                    && NAFUtilsUD.getRef(semaforRole, "FrameNet", null) != null;
//                            for (final ExternalRef ref : mateRole.getExternalRefs()) {
//                                if (!ref.getResource().equalsIgnoreCase("FrameNet")
//                                        || addFramenetRef) {
//                                    semaforRole.addExternalRef(new ExternalRef(ref));
//                                }
//                            }
//                        }
//                    }
//                    if (addRole) {
//                        final Role semaforRole = document.newRole(semaforPredicate,
//                                mateRole.getSemRole(), mateRole.getSpan());
//                        semaforPredicate.addRole(semaforRole);
//                        for (final ExternalRef ref : mateRole.getExternalRefs()) {
//                            semaforRole.addExternalRef(new ExternalRef(ref));
//                        }
//                    }
//                }
//
//                // Delete original Mate predicate
//                document.removeAnnotation(matePredicate);
//
//                // Log operation
//                if (LOGGER.isDebugEnabled()) {
//                    LOGGER.debug("Merged " + NAFUtilsUD.toString(matePredicate) + " into "
//                            + NAFUtilsUD.toString(semaforPredicate)
//                            + (mergeFramenet ? " (including FrameNet data)" : ""));
//                }
//
//            }
//        }
    }



    private void applySRLSelfArgFixing(final KAFDocument document) {

        for (final Predicate predicate : document.getPredicates()) {

            // Skip verbs
            final Term predTerm = predicate.getTerms().get(0);
            if (predTerm.getUpos().equalsIgnoreCase("VERB")) {
                continue;
            }

            // Retrieve the NomBank roleset for current predicate, if known. Skip otherwise
            final String rolesetID = NAFUtilsUD.getRoleset(predicate);
            final NomBank.Roleset roleset = rolesetID == null ? null : NomBank
                    .getRoleset(rolesetID);
            if (roleset == null) {
                continue;
            }

            // Retrieve mandatory and optional roles associated to NomBank roleset
            final List<Integer> mandatoryArgs = roleset.getPredMandatoryArgNums();
            final List<Integer> optionalArgs = roleset.getPredOptionalArgNums();


            // Check current role assignment to predicate term. Mark it as invalid if necessary
            int currentNum = -1;
            for (final Role role : ImmutableList.copyOf(predicate.getRoles())) {
                final Term headTerm = document.getTermsHead(role.getTerms());
                // todo to be tested if still OK...
                if (headTerm!=null) {
                    if (headTerm == predTerm && role.getSemRole() != null) {
                        boolean valid = false;
                        final Matcher matcher = SRL_ROLE_PATTERN.matcher(role.getSemRole());
                        if (matcher.matches()) {
                            currentNum = Integer.parseInt(matcher.group(1));
                            valid = roleset.getPredMandatoryArgNums().contains(currentNum)
                                    || roleset.getPredOptionalArgNums().contains(currentNum);
                        }
                        if (!valid) {
                            predicate.removeRole(role);
                            LOGGER.debug("Removed " + NAFUtilsUD.toString(role) + " for "
                                    + NAFUtilsUD.toString(predicate) + " (mandatory " + mandatoryArgs
                                    + ", optional " + optionalArgs + ")");
                        }
                    }
                }
            }

            // Add missing role marking, if necessary
            if (!roleset.getPredMandatoryArgNums().isEmpty()) {
                final List<Integer> args = Lists.newArrayList();
                args.addAll(roleset.getPredMandatoryArgNums());
                args.remove((Object) currentNum);
                for (final Integer arg : args) {

                    //we add a role with the same span of the predicate
//                    final List<Term> terms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(
//                            document.getTermsByDepAncestors(Collections.singleton(predTerm)));

                    // ... and creates a single span from them
                    final Span<Term> span = KAFDocument.newTermSpan(predicate.getTerms(), predTerm);
                    final String semRole = "A" + arg;
                    final Role role = document.newRole(predicate, semRole, span);
                    predicate.addRole(role);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Added " + NAFUtilsUD.toString(role) + " to "
                                + NAFUtilsUD.toString(predicate));
                    }
                }
            }
        }
    }



    private void applySRLPredicateAddition(final KAFDocument document) {

        for (final Term term : document.getTerms()) {

            final String pos = term.getUpos();

//            check if the term is either a VERB, NOUN, ADJ, ADV, and does not already have a predicate annotation
            if (!pos.equalsIgnoreCase("VERB") && !pos.equalsIgnoreCase("NOUN") && !pos.equalsIgnoreCase("ADJ") && !pos.equalsIgnoreCase("ADV")
                    || !document.getPredicatesByTerm(term).isEmpty())
//                    || !document.getTimeExsByWF(term.getWFs().get(0)).isEmpty()) //todo do we need filter on timex? I don't think so...
            {
                continue;
            }

//            todo do we need this? As decision on overlapping entities is done in the KEM generator, I don't think so...
//            // Identify the smallest entity the term belongs to, if any, in which case require
//            // the term to be the head of the entity. This will discard other terms inside an
//            // entity (even if nouns), thus enforcing a policy where entities are indivisible
//            Entity entity = null;
//            for (final Entity e : document.getEntitiesByTerm(term)) {
//                if (entity == null || e.getTerms().size() < entity.getTerms().size()) {
//                    entity = e;
//                    break;
//                }
//            }
//            if (entity != null && term != document.getTermsHead(entity.getTerms())) {
//                continue;
//            }


            // Decide if a predicate can be added and, in case, which is its roleset,
            // distinguishing between verbs (-> PropBank) and other terms (-> NomBank)
            ExternalRef ref = null;
            final String lemma = term.getLemma();
            if (pos.equalsIgnoreCase("VERB")) {
                final List<PropBank.Roleset> rolesets = PropBank.getRolesets(lemma);
                if (rolesets.size() == 1) {
                    final String rolesetID = rolesets.get(0).getID();
                    ref = document.newExternalRef(NAFUtilsUD.RESOURCE_PROPBANK, rolesetID);
                }
            } else {
                final List<NomBank.Roleset> rolesets = NomBank.getRolesetsForLemma(lemma);
                if (rolesets.size() == 1) {
                    final String rolesetID = rolesets.get(0).getId();
                    ref = document.newExternalRef(NAFUtilsUD.RESOURCE_NOMBANK, rolesetID);
                }
            }

            // Create the predicate, if possible
            if (ref != null) {
                final Predicate predicate = document.newPredicate(KAFDocument.newTermSpan(
                        Collections.singletonList(term), term));
                predicate.addExternalRef(ref);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Added " + NAFUtilsUD.toString(predicate) + ", sense '"
                            + ref.getReference() + "'");
                }
            }
        }
    }



    private void applySRLRemoveUnknownPredicates(final KAFDocument document) {

        // Scan all predicates in the SRL layer
        for (final Predicate predicate : Lists.newArrayList(document.getPredicates())) {

            // Determine whether the predicate is a verb and thus which resource to check for>
            // this check should work with UD as predicates from SRL are usually single terms, and the head is the term
            final Term head = document.getTermsHead(predicate.getTerms());
            final boolean isVerb = head.getUpos().equalsIgnoreCase("VERB");
            final String resource = isVerb ? "propbank" : "nombank";

            // Predicate is invalid if its roleset is unknown in NomBank / PropBank
            for (final ExternalRef ref : NAFUtilsUD.getRefs(predicate, resource, null)) {
                final String roleset = ref.getReference();
                if (isVerb && PropBank.getRoleset(roleset) == null || !isVerb
                        && NomBank.getRoleset(roleset) == null) {
                    document.removeAnnotation(predicate);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Removed " + NAFUtilsUD.toString(predicate)
                                + " with unknown sense '" + roleset + "' in resource " + resource);
                    }
                    break;
                }
            }
        }
    }


    /*

BELOW HERE METHODS ARE UD-SAFE

 */



    private void applyTermSenseFiltering(final KAFDocument document) {

        for (final Term term : document.getTerms()) {
            if (term.getUpos() != null && term.getUpos().equalsIgnoreCase("PROPN")) {
                NAFUtilsUD.removeRefs(term, NAFUtilsUD.RESOURCE_WN_SYNSET, null);
                NAFUtilsUD.removeRefs(term, NAFUtilsUD.RESOURCE_WN_SST, null);
                NAFUtilsUD.removeRefs(term, NAFUtilsUD.RESOURCE_BBN, null);
                NAFUtilsUD.removeRefs(term, NAFUtilsUD.RESOURCE_SUMO, null);
                NAFUtilsUD.removeRefs(term, NAFUtilsUD.RESOURCE_YAGO, null);
            }
        }
    }

    private void applyTermSenseCompletion(final KAFDocument document) {

        for (final Term term : document.getTerms()) {

            // Retrieve existing refs
            ExternalRef bbnRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_BBN, null);
            ExternalRef synsetRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_WN_SYNSET, null);
            ExternalRef sstRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_WN_SST, null);

            // Retrieve a missing SST from the WN Synset (works always)
            if (sstRef == null && synsetRef != null) {
                final String sst = WordNet.mapSynsetToSST(synsetRef.getReference());
                if (sstRef == null || !Objects.equal(sstRef.getReference(), sst)) {
                    LOGGER.debug((sstRef == null ? "Added" : "Overridden") + " SST '" + sst
                            + "' of " + NAFUtilsUD.toString(term) + " based on Synset '"
                            + synsetRef.getReference() + "'");
                    sstRef = document.newExternalRef(NAFUtilsUD.RESOURCE_WN_SST, sst);
                    NAFUtilsUD.addRef(term, sstRef);
                }
            }

            // Apply noun-based mapping.
            final boolean isNoun = term.getUpos().equalsIgnoreCase("NOUN");
            if (isNoun) {

                // Retrieve a missing BBN from the WN Synset
                if (bbnRef == null && synsetRef != null) {
                    final String bbn = WordNet.mapSynsetToBBN(synsetRef.getReference());
                    if (bbn != null) {
                        bbnRef = document.newExternalRef(NAFUtilsUD.RESOURCE_BBN, bbn);
                        NAFUtilsUD.addRef(term, bbnRef);
                        LOGGER.debug("Added BBN '" + bbn + "' of " + NAFUtilsUD.toString(term)
                                + " based on Synset '" + synsetRef.getReference() + "'");
                    }

                }

                // Retrieve a missing WN Synset from the BBN
                if (synsetRef == null && bbnRef != null) {
                    final String synsetID = WordNet.mapBBNToSynset(bbnRef.getReference());
                    if (synsetID != null) {
                        synsetRef = document.newExternalRef(NAFUtilsUD.RESOURCE_WN_SYNSET, synsetID);
                        NAFUtilsUD.addRef(term, synsetRef);
                        LOGGER.debug("Added Synset '" + synsetID + "' of "
                                + NAFUtilsUD.toString(term) + " based on BBN '"
                                + bbnRef.getReference() + "'");
                    }
                }

                // Retrieve a missing SST from the BBN
                if (sstRef == null && bbnRef != null) {
                    final String sst = WordNet.mapBBNToSST(bbnRef.getReference());
                    if (sst != null) {
                        sstRef = document.newExternalRef(NAFUtilsUD.RESOURCE_WN_SST, sst);
                        NAFUtilsUD.addRef(term, sstRef);
                        LOGGER.debug("Added SST '" + sst + "' of " + NAFUtilsUD.toString(term)
                                + " based on BBN '" + bbnRef.getReference() + "'");
                    }
                }
            }


        }
    }


    private void applyLinkingFixing(final KAFDocument document) {

        // Check each linked entity, dropping the links if the span is in the stop word list
        final List<ExternalRef> refs = Lists.newArrayList();
        for (final Entity entity : document.getEntities()) {

            // Extract all the <ExternalRef> elements with links for the current entity
            refs.clear();
            for (final ExternalRef ref : entity.getExternalRefs()) {
                if (!NAFUtilsUD.RESOURCE_VALUE.equals(ref.getResource())) {
                    refs.add(ref);
                }
            }

            // If the entity is linked, check its span is not in the stop word list
            if (!refs.isEmpty()) {
                final String[] tokens = Util.hardTokenize(entity.getStr());
                final String normalized = Joiner.on(' ').join(tokens).toLowerCase();
                if (Arrays.binarySearch(LINKING_STOP_WORDS, normalized) >= 0) {
                    for (final ExternalRef ref : refs) {
                        NAFUtilsUD.removeRefs(entity, ref.getResource(), ref.getReference());
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Removed stop-word ref '{}' from {}", ref,
                                    NAFUtilsUD.toString(entity));
                        }
                    }
                }
            }
        }
    }


    private void applySRLPreMOnIRIs(final KAFDocument document) {
        // Process each predicate and role in the SRL layer

        final List<String> models = Arrays.asList(NAFUtilsUD.RESOURCE_FRAMENET, NAFUtilsUD.RESOURCE_VERBNET, NAFUtilsUD.RESOURCE_PROPBANK, NAFUtilsUD.RESOURCE_NOMBANK);

        for (final Predicate predicate : document.getPredicates()) {
            List<ExternalRef> allPredicateExtRefs = predicate.getExternalRefs();
            List<ExternalRef> predicateExtRefToRemove =  Lists.newArrayList();
            for (final ExternalRef predRef : ImmutableList.copyOf(allPredicateExtRefs)) {
                String refStr= predRef.getResource();
                if (models.contains(refStr)) {
                    final String pred = predRef.getReference();
                    final String source = predRef.getSource();

                    final String pos = predicate.getTerms().get(0).getUpos();
                    final String lemma = predicate.getTerms().get(0).getLemma();

                    final IRI premonIRI = NAFUtilsUD.createPreMOnSemanticClassIRIfor(refStr,pred);
                    final IRI premonConcIRI = NAFUtilsUD.createPreMOnConceptualizationIRIfor(refStr,pred,lemma,pos);

                    predicateExtRefToRemove.add(predRef);
                    if (premonIRI != null) {
                        ExternalRef e = new ExternalRef("PreMOn+"+refStr, premonIRI.getLocalName());
                        if (source!=null) e.setSource(source);
                        NAFUtilsUD.setRef(predicate, e);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("PreMOn-ized predicate '{}' to '{}'", pred,
                                    premonIRI.getLocalName());
                        }
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Can't PreMOn-ize predicate '{}'. Removed.", pred);
                        }
                    }

                    if (premonConcIRI!= null) {
                        ExternalRef e = new ExternalRef("PreMOn+"+refStr+"+co", premonConcIRI.getLocalName());
                        if (source!=null) e.setSource(source);
                        NAFUtilsUD.setRef(predicate, e);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("PreMOn-ized Conceptualization for predicate '{}' with lemma '{}' and pos '{}': '{}'", pred, lemma, pos,
                                    premonIRI.getLocalName());
                        }
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Can't PreMOn-ize Conceptualization for predicate '{}' with lemma '{}' and pos '{}'. Removed.", pred, lemma, pos);
                        }
                    }


                }
            }

            //remove old predicate ref
            for (ExternalRef toBeDropped:predicateExtRefToRemove
                 ) {
                allPredicateExtRefs.remove(toBeDropped);
            }

            // Convert FrameNet refs to FrameBase refs at the role level
            for (final Role role : predicate.getRoles()) {
                List<ExternalRef> allRoleExtRefs = role.getExternalRefs();
                List<ExternalRef> roleExtRefToRemove =  Lists.newArrayList();
                for (final ExternalRef roleRef : ImmutableList.copyOf(allRoleExtRefs)) {
                    String refStr= roleRef.getResource();
                    if (models.contains(refStr)) {
                        final String predicateAndRole = roleRef.getReference();
                        final String source = roleRef.getSource();
                        final int index = predicateAndRole.indexOf('@');
                        if (index > 0) {
                            final String pred = predicateAndRole.substring(0, index);
                            final String rol = predicateAndRole.substring(index + 1);
                            final IRI premonIRI = NAFUtilsUD.createPreMOnSemanticRoleIRIfor(refStr,pred,rol);
                            roleExtRefToRemove.add(roleRef);
                            if (premonIRI != null) {
                                ExternalRef e = new ExternalRef("PreMOn+"+refStr, premonIRI.getLocalName());
                                if (source!=null) e.setSource(source);
                                NAFUtilsUD.setRef(role, e);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("PreMOn-ized role '{}' of predicate '{}' to '{}'", rol,pred,
                                            premonIRI.getLocalName());
                                }
                            } else {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Can't PreMOn-ize role '{}' of predicate '{}'. Removed.", rol,pred);
                                }
                            }
                        }
                    }
                }
                //remove old role
                for (ExternalRef toBeRemoved:roleExtRefToRemove
                     ) {
                    allRoleExtRefs.remove(toBeRemoved);
                }
            }
        }
    }


    /**
     * Returns a new configurable {@code Builder} for the instantiation of a {@code NAFFilter}.
     *
     * @return a new {@code Builder}
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new configurable {@code Builder} with all {@code NAFFilter} features either
     * enabled or disabled, based on the supplied parameter.
     *
     * @param enableAll
     *            true, to enable all features; false, to disable all features; null, to maintain
     *            default settings.
     * @return a new {@code Builder}
     */
    public static final Builder builder(@Nullable final Boolean enableAll) {
        return new Builder() //
                .withDepFixFlatHeads(enableAll)
                .withTermSenseFiltering(enableAll)
                .withTermSenseCompletion(enableAll) //
                .withLinkingFixing(enableAll) //
                .withSRLPreprocess(enableAll,enableAll,enableAll)
                .withSRLRemoveUnknownPredicates(enableAll) //
                .withSRLPredicateAddition(enableAll) //
                .withSRLSelfArgFixing(enableAll) //
                .withSRLPreMOnIRIs(enableAll);
    }

    /**
     * Configurable builder object for the creation of {@code NAFFilter}s.
     * <p>
     * Supported properties accepted by {@link #withProperties(Map, String)} and corresponding
     * setter methods:
     * </p>
     * <table border="1">
     * <thead>
     * <tr>
     * <th>Property</th>
     * <th>Values</th>
     * <th>Corresponding method</th>
     * <th>Default</th>
     * </tr>
     * </thead><tbody>
     * <tr>
     * <td>termSenseFiltering</td>
     * <td>true, false</td>
     * <td>{@link #withTermSenseFiltering(Boolean)}</td>
     * <td>true</td>
     * </tr>
     * <tr>
     * <td>termSenseCompletion</td>
     * <td>true, false</td>
     * <td>{@link #withTermSenseCompletion(Boolean)}</td>
     * <td>true</td>
     * </tr>
     * <tr>
     * <td>linkingFixing</td>
     * <td>true, false</td>
     * <td>{@link #withLinkingFixing(Boolean)}</td>
     * <td>false</td>
     * </tr>
     * <tr>
     * <td>srlRemoveUnknownPredicates</td>
     * <td>true, false</td>
     * <td>{@link #withSRLRemoveUnknownPredicates(Boolean)}</td>
     * <td>false</td>
     * </tr>
     * <tr>
     * <td>srlPredicateAddition</td>
     * <td>true, false</td>
     * <td>{@link #withSRLPredicateAddition(Boolean)}</td>
     * <td>true</td>
     * </tr>
     * <tr>
     * <td>srlSelfArgFixing</td>
     * <td>true, false</td>
     * <td>{@link #withSRLSelfArgFixing(Boolean)}</td>
     * <td>true</td>
     * </tr>
     * </tbody>
     * </table>
     */
    public static final class Builder {

        @Nullable
        private Boolean termSenseFiltering;

        @Nullable
        private Boolean termSenseCompletion;

        @Nullable
        private Boolean linkingFixing;

        @Nullable
        private Boolean srlPreprocess;

        @Nullable
        private Boolean srlEnableMate;

        @Nullable
        private Boolean srlEnableSemafor;

        @Nullable
        private Boolean srlRemoveUnknownPredicates;

        @Nullable
        private Boolean srlPredicateAddition;

        @Nullable
        private Boolean srlSelfArgFixing;

        @Nullable
        private Boolean srlPreMOnIRIs;

        @Nullable
        private Boolean depFixFlatHeads;


        Builder() {
        }

        /**
         * Sets all the properties in the map supplied, matching an optional prefix.
         *
         * @param properties
         *            the properties to configure, not null
         * @param prefix
         *            an optional prefix used to select the relevant properties in the map
         * @return this builder object, for call chaining
         */
        public Builder withProperties(final Map<?, ?> properties, @Nullable final String prefix) {
            final String p = prefix == null ? "" : prefix.endsWith(".") ? prefix : prefix + ".";
            for (final Map.Entry<?, ?> entry : properties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null
                        && entry.getKey().toString().startsWith(p)) {
                    final String name = entry.getKey().toString().substring(p.length());
                    final String value = Strings.emptyToNull(entry.getValue().toString());

                    if ("depFixFlatHeads".equals(name)) {
                        withDepFixFlatHeads(Boolean.valueOf(value));
                    } else if ("termSenseFiltering".equals(name)) {
                        withTermSenseFiltering(Boolean.valueOf(value));
                    } else if ("termSenseCompletion".equals(name)) {
                        withTermSenseCompletion(Boolean.valueOf(value));
                    } else if ("linkingFixing".equals(name)) {
                        withLinkingFixing(Boolean.valueOf(value));
                    } else if ("srlPreprocess".equals(name)) {
                        if ("none".equalsIgnoreCase(value)) {
                            withSRLPreprocess(false, false, false);
                        } else if ("basic".equalsIgnoreCase(value)) {
                            withSRLPreprocess(true, false, false);
                        } else if ("mate".equalsIgnoreCase(value)) {
                            withSRLPreprocess(true, true, false);
                        } else if ("semafor".equalsIgnoreCase(value)) {
                            withSRLPreprocess(true, false, true);
                        } else if ("mate+semafor".equalsIgnoreCase(value)) {
                            withSRLPreprocess(true, true, true);
                        }else {
                            throw new IllegalArgumentException("Invalid '" + value +"' srlPreprocess property. Supported: none basic mate semafor mate+semafor");
                        }
                    } else if ("srlRemoveUnknownPredicates".equals(name)) {
                        withSRLRemoveUnknownPredicates(Boolean.valueOf(value));
                    } else if ("srlPredicateAddition".equals(name)) {
                        withSRLPredicateAddition(Boolean.valueOf(value));
                    } else if ("srlSelfArgFixing".equals(name)) {
                        withSRLSelfArgFixing(Boolean.valueOf(value));
                    } else if ("srlPreMOnIRIs".equals(name)){
                        withSRLPreMOnIRIs(Boolean.valueOf(value));
                    }
                }
            }
            return this;
        }


//        Methods for NAF


        /**
         * Fixes head for flat cluster in dependency trees, moving it to the term with highest token ID
         *
         * @param depFixFlatHeads
         *            true to enable dependency tree flat fixing, null to use default value
         * @return this builder object, for call chaining
         */
        public Builder withDepFixFlatHeads(@Nullable final Boolean depFixFlatHeads) {
            this.depFixFlatHeads = depFixFlatHeads;
            return this;
        }



        /**
         * Specifies whether term senses (BBN, SST, WN Synset, SUMO mapping, YAGO) for proper
         * names should be removed.
         *
         * @param termSenseFiltering
         *            true to enable term sense filtering, null to use default value
         * @return this builder object, for call chaining
         */
        public Builder withTermSenseFiltering(@Nullable final Boolean termSenseFiltering) {
            this.termSenseFiltering = termSenseFiltering;
            return this;
        }

        /**
         * Specifies whether missing term senses (BBN, SST, WN Synset, SUMO mapping ) should be
         * completed by applying sense mappings.
         *
         * @param termSenseCompletion
         *            true to enable term sense completion, null to use default value
         * @return this builder object, for call chaining
         */
        public Builder withTermSenseCompletion(@Nullable final Boolean termSenseCompletion) {
            this.termSenseCompletion = termSenseCompletion;
            return this;
        }

        /**
         * Specifies whether removal of inaccurate entity links to DBpedia should occur. If
         * enabled, links for entities whose span is part of a stop word list are removed. The
         * stop word list contains (multi-)words that are known to be ambiguous from an analysis
         * of Wikipedia data.
         *
         * @param linkingFixing
         *            true to enable linking fixing; null, to use the default setting
         * @return this builder object, for call chaining
         */
        //KEEP
        public Builder withLinkingFixing(@Nullable final Boolean linkingFixing) {
            this.linkingFixing = linkingFixing;
            return this;
        }

        /**
         * Specifies whether SRL predicates with unknown PropBank/NomBank rolesets/roles in the
         * NAF should be removed. A roleset/role is wrong if it does not appear in
         * PropBank/NomBank frame files (SRL tools such as Mate may detect predicates for unknown
         * rolesets, to increase recall).
         *
         * @param srlRemoveUnknownPredicates
         *            true, if removal of predicates with unknown PB/NB rolesets/roles has to be
         *            enabled
         * @return this builder object, for call chaining
         */
        //KEEP
        public Builder withSRLRemoveUnknownPredicates(
                @Nullable final Boolean srlRemoveUnknownPredicates) {
            this.srlRemoveUnknownPredicates = srlRemoveUnknownPredicates;
            return this;
        }

        /**
         * Specifies whether new predicates can be added for verbs, noun and adjectives having
         * exactly one sense in PropBank or NomBank but not marked in the text.
         *
         * @param srlPredicateAddition
         *            true, to enable predicate addition; null to use the default setting
         * @return this builder object, for call chaining
         */
        //KEEP
        public Builder withSRLPredicateAddition(@Nullable final Boolean srlPredicateAddition) {
            this.srlPredicateAddition = srlPredicateAddition;
            return this;
        }

        /**
         * Specifies whether 'self-roles' can be added for predicates where missing or removed
         * where wrongly added. If set, for each recognized predicate the filter checks whether
         * the predicate term has also been marked as role. IF it is not marked in the NAF but it
         * is always marked in NomBank training set THEN the filter adds a new role for the
         * predicate term, using the semantic role in NomBank training set. If already marked
         * whereas no marking should happen based on previous criteria, then the role is removed.
         *
         * @param srlSelfArgFixing
         *            true if role addition is enabled
         * @return this builder object, for call chaining
         */
        //KEEP?
        public Builder withSRLSelfArgFixing(@Nullable final Boolean srlSelfArgFixing) {
            this.srlSelfArgFixing = srlSelfArgFixing;
            return this;
        }

        /**
         * Specifies whether to preprocess SRL layer, enabling Mate and/or Semafor outputs. If
         * both tools are enabled, they are combined in such a way that semafor takes precedence
         * in case two predicates refer to the same token.
         *
         * @param srlPreprocess
         *            true, to enable preprocessing of SRL layer
         * @param srlEnableMate
         *            true, to enable Mate output
         * @param srlEnableSemafor
         *            true, to enable Semafor output
         * @return this builder object, for call chaining
         */
        //DISABLE
        public Builder withSRLPreprocess(@Nullable final Boolean srlPreprocess,
                                         @Nullable final Boolean srlEnableMate, @Nullable final Boolean srlEnableSemafor) {
            this.srlPreprocess = srlPreprocess;
            this.srlEnableMate = srlEnableMate;
            this.srlEnableSemafor = srlEnableSemafor;
            return this;
        }

        /**
         * Specifies replace reference of predicate models in NAF with premon IRIs
         *
         * @param srlPreMOnIRIs
         *            true to enable IRI replacement, null to use default value
         * @return this builder object, for call chaining
         */
        public Builder withSRLPreMOnIRIs(@Nullable final Boolean srlPreMOnIRIs) {
            this.srlPreMOnIRIs = srlPreMOnIRIs;
            return this;
        }


        /**
         * Creates a {@code NAFFilter} based on the flags specified on this builder object.
         *
         * @return the constructed {@code NAFFilter}
         */
        public NAFFilterUD build() {
            return new NAFFilterUD(this);
        }

    }


}
