package eu.fbk.dkm.pikes.rdf.naf;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.*;

import eu.fbk.dkm.pikes.rdf.util.OWLTime;
import eu.fbk.dkm.pikes.rdf.api.Document;
import eu.fbk.dkm.pikes.rdf.api.Extractor;
import eu.fbk.dkm.pikes.rdf.util.ModelUtil;
import eu.fbk.dkm.pikes.rdf.vocab.*;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.NAFUtilsUD;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.dkm.pikes.resources.YagoTaxonomy;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Statements;
import eu.fbk.utils.svm.Util;
import ixa.kaflib.*;
import ixa.kaflib.KAFDocument.FileDesc;
import ixa.kaflib.Predicate.Role;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NAFExtractorUD implements Extractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAFExtractorUD.class);

    public void generate(final Object document, final Model model, @Nullable final Iterable<Integer> sentenceIDs) throws Exception {

        // Log beginning of operation
        final long ts = System.currentTimeMillis();

        KAFDocument doc = (KAFDocument) document;
        LOGGER.debug("== Building KEM for {} ==", doc.getPublic().uri);

        IRI IRI = SimpleValueFactory.getInstance().createIRI(doc.getPublic().uri);

        final boolean[] ids = new boolean[doc.getNumSentences() + 1];
        if (sentenceIDs == null) {
            Arrays.fill(ids, true);
        } else {
            for (final Integer sentenceID : sentenceIDs) {
                ids[sentenceID] = true;
            }
        }

        new Extraction(IRI, model,
                doc, ids).run();

        LOGGER.debug("Done in {} ms", System.currentTimeMillis() - ts);
    }
    
    @Override
    public void extract(Document document, Map<String, String> options) {

        // Process all NAFAnnotations in the document
        for (eu.fbk.dkm.pikes.rdf.api.Annotation a : document.getAnnotations()) {
            if (a instanceof NAFAnnotation) {

                // Extract NAF
                KAFDocument naf = ((NAFAnnotation) a).getNAF();

                // Extract sentence IDs: process all sentences unless option 'sentences' is given
                final boolean[] sentenceIDs = new boolean[naf.getNumSentences()];
                if (!options.containsKey("sentences")) {
                    Arrays.fill(sentenceIDs, true);
                } else {
                    Arrays.fill(sentenceIDs, false);
                    for (String i : Splitter.onPattern("[,;\\s]+").omitEmptyStrings().trimResults()
                            .split(options.get("sentences"))) {
                        sentenceIDs[Integer.parseInt(i)] = true;
                    }
                }

                // Perform extraction from current NAFAnnotation
                new Extraction(document.getIRI(), document.getModel(), naf, sentenceIDs).run();
            }
        }
    }

    //todo adapt for UD (not needed)
//    private static final String MODIFIER_REGEX = "(NMOD|AMOD|TMP|LOC|TITLE) PMOD? (COORD CONJ?)* PMOD?";

    //todo adapt for UD
    private static final String PARTICIPATION_REGEX = ""
//            + "SUB? (COORD CONJ?)* (PMOD (COORD CONJ?)*)? ((VC OPRD?)|(IM OPRD?))*";
//            + "SUB? ( (COORD CONJ?)* PMOD)? ((VC OPRD?)|(IM OPRD?))*";
            + "nsubj|obj|iobj|csubj|ccomp|xcomp|obl";

    private static final Multimap<String, IRI> DEFAULT_TYPE_MAP = ImmutableMultimap
            .<String, IRI>builder() //
            .put("entity.person", NWR.PERSON) //
            .put("entity.organization", NWR.ORGANIZATION) //
            .put("entity.location", NWR.LOCATION) //
            .put("entity.misc", NWR.MISC) //
            .put("entity.money", GR.PRICE_SPECIFICATION) //
            .put("entity.date", OWLTIME.DATE_TIME_INTERVAL) //
            .put("entity.time", OWLTIME.DATE_TIME_INTERVAL) //
            .put("timex.date", OWLTIME.DATE_TIME_INTERVAL) //
            .put("timex.duration", OWLTIME.PROPER_INTERVAL) //
            .build();

    private static final Map<String, String> DEFAULT_NAMESPACE_MAP = ImmutableMap
            .<String, String>builder()
            .put("propbank", "http://www.newsreader-project.eu/ontologies/propbank/")
            .put("nombank", "http://www.newsreader-project.eu/ontologies/nombank/")
            .put("framenet", "http://www.newsreader-project.eu/ontologies/framenet/")
            .put("verbnet", "http://www.newsreader-project.eu/ontologies/verbnet/")
            .put("premon+propbank", "http://premon.fbk.eu/resource/")
            .put("premon+nombank", "http://premon.fbk.eu/resource/")
            .put("premon+framenet", "http://premon.fbk.eu/resource/")
            .put("premon+verbnet", "http://premon.fbk.eu/resource/")
            .put("premon+propbank+co", "http://premon.fbk.eu/resource/")
            .put("premon+nombank+co", "http://premon.fbk.eu/resource/")
            .put("premon+framenet+co", "http://premon.fbk.eu/resource/")
            .put("premon+verbnet+co", "http://premon.fbk.eu/resource/")
            .put("eso", "http://www.newsreader-project.eu/domain-ontology#")
            .put("framebase", "http://framebase.org/ns/") //
            .put("wordnet","http://sli.uvigo.gal/rdf_galnet/") //
            .put("wn30-ukb","http://wordnet-rdf.princeton.edu/wn30/")
            .put("wn30-sst","http://pikes.fbk.eu/wn/sst/")
            .put("wn30","http://wordnet-rdf.princeton.edu/wn30/")
            .put("bbn","http://pikes.fbk.eu/bbn/")
            .put(KEM.PREFIX, KEM.NAMESPACE) //
            .put(KEMT.PREFIX, KEMT.NAMESPACE) //
            .put("attribute", "attr:")
            // TODO: change this namespace
            .put("syn", "http://wordnet-rdf.princeton.edu/wn30/")
            .put(SUMO.PREFIX, SUMO.NAMESPACE)//
            .put("yago", YagoTaxonomy.NAMESPACE).build();

    private static final String DEFAULT_OWLTIME_NAMESPACE = "http://pikes.fbk.eu/time/";
    private static final String DEFAULT_NER_NAMESPACE = "http://pikes.fbk.eu/ner/";
    private static final String DEFAULT_WN_SST_NAMESPACE = "http://pikes.fbk.eu/wn/sst/";
    private static final String DEFAULT_WN_SYN_NAMESPACE = "http://wordnet-rdf.princeton.edu/wn30/";
    private static final String DEFAULT_BBN_NAMESPACE = "http://pikes.fbk.eu/bbn/";


    private static final String DEFAULT_OLIA_UD_POS = "http://fginter.github.io/docs/u/pos/all.html#";

    public static final NAFExtractorUD DEFAULT = NAFExtractorUD.builder().build();

    private final Multimap<String, IRI> typeMap;

    private final Map<String, String> namespaceMap;

    private final String owltimeNamespace;

    private final boolean merging;

    private final boolean normalization;


    public NAFExtractorUD(final Builder builder) {
        this.typeMap = ImmutableMultimap.copyOf(MoreObjects.firstNonNull(builder.typeMap,
                DEFAULT_TYPE_MAP));
        this.namespaceMap = ImmutableMap.copyOf(MoreObjects.firstNonNull(builder.namespaceMap,
                DEFAULT_NAMESPACE_MAP));
        this.owltimeNamespace = MoreObjects.firstNonNull(builder.owltimeNamespace,
                DEFAULT_OWLTIME_NAMESPACE);
        this.merging = MoreObjects.firstNonNull(builder.merging, Boolean.FALSE);
        this.normalization = MoreObjects.firstNonNull(builder.normalization, Boolean.FALSE);
    }

    private final class Extraction {

        private final Model model;

        private final KAFDocument document;

        private final ValueFactory vf;

        private final String documentText;

        private final IRI documentIRI;

        private final boolean[] sentenceIDs;

        private final BiMap<String, String> mintedIRIs;

        private final IRI contextIRI;

        private final Map<String, Set<Mention>> mentions;
        private final Map<Mention, Set<Annotation>> annotations;
        private final Map<String, Mention> nafIdMentions;



//        private final Map<String, Set<Annotation>> nafIdAnnotations;


        //check if there is already a mention with that head and span
        private Mention getMention(final String head, List<Term> terms){

            Mention mention = null;
            if (this.mentions.containsKey(head)) {
                Set<Mention> mentions = this.mentions.get(head);
                for (Mention m : mentions
                     ) {
                    if (m.extent.equals(terms))
                        mention = m;
                }
            }
            return mention;
        }

        //get all mentions insisting on a term
        private Set<Mention> getAllMentionsForTerm(final String termID){

            Set<Mention> mentions = new HashSet<>();
            if (this.mentions.containsKey(termID))
                mentions = this.mentions.get(termID);
            return mentions;
        }



        //get the BEST mention for a given head (used in coordination, coreference, roles)
        private Mention getBestMention(final String head){

            Mention BestMention = null;
            if (this.mentions.containsKey(head)) {
                Set<Mention> mentions = this.mentions.get(head);
                BestMention =  this.mentions.get(head).iterator().next();
                for (Mention m : mentions
                        ) {
                    if (BestMention.extent.size()<m.extent.size())
                        BestMention = m;
                }
            }
            return BestMention;
        }

        private void safeMentionPutInMap(final String ID, final Mention mention) {
            Set<Mention> mentions;

            if (this.mentions.containsKey(ID))
                mentions = this.mentions.get(ID);
            else
                mentions = Sets.newHashSet();
            mentions.add(mention);
            this.mentions.put(ID, mentions);
        }

        private void safeAnnotationPutInMap(final Mention mention, final Annotation annotation) {
            Set<Annotation> annotations;

            if (this.annotations.containsKey(mention))
                annotations = this.annotations.get(mention);
            else
                annotations = Sets.newHashSet();
            annotations.add(annotation);
            this.annotations.put(mention, annotations);
        }

        Extraction(final IRI IRI, final Model model, final KAFDocument document, final boolean[] sentenceIDs) {

            // Reconstruct the document text using term offsets to avoid alignment issues
            final StringBuilder builder = new StringBuilder();
            for (final WF word : document.getWFs()) {
                final int offset = word.getOffset();
                if (builder.length() > offset) {
                    builder.setLength(offset);
                } else {
                    while (builder.length() < offset) {
                        builder.append(" ");
                    }
                }
                builder.append(word.getForm());
            }

            // Initialize the object
            this.model = model;
            this.document = document;
            this.mintedIRIs = HashBiMap.create();
            this.vf = Statements.VALUE_FACTORY;
            this.documentText = builder.toString();
            this.documentIRI = IRI;
            //contextIRI: nif:Context (from NIF) is the maximal fragment associated to a kemt:TextResource

            //used for processing only some sentences
            this.sentenceIDs = sentenceIDs;

            this.contextIRI = Statements.VALUE_FACTORY.createIRI(this.documentIRI.stringValue() + "#ctx");

            this.model.add(this.contextIRI, NIF.SOURCE_URL, IRI);
            this.model.add(this.contextIRI, RDF.TYPE, NIF.CONTEXT);
            this.model.add(this.contextIRI, NIF.IS_STRING, Statements.VALUE_FACTORY.createLiteral(documentText));
            this.mentions = Maps.newHashMap();
            this.annotations = Maps.newHashMap();
            this.nafIdMentions = Maps.newHashMap();
//            this.nafIdAnnotations = Maps.newHashMap();
        }

        void run() {

            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 0. Processing Metadata");
            processMetadata();
            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 1. Processing Timexes");
            processTimexes();
            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 2. Processing Entities");
            processEntities();
            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 3. Processing Predicates");
            processPredicates();

//            this one have to come after the first four
            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 4. Processing Coordinations");
            processCoordinations();

//            next ones have to come after coordination
            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 5. Processing Coreferences");
            processCoreferences();
            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 6. Processing Predicate Roles");
            processRoles();
        }



        private void processMetadata() {

            // Obtain IRIs of document and NAF resources
            final IRI docIRI = this.documentIRI;
            final IRI nafIRI = this.vf.createIRI(docIRI.stringValue() + ".naf");

            // Emit document types
            emitTriple(docIRI, RDF.TYPE, new IRI[] { KEMT.TEXT_RESOURCE, KS.RESOURCE, KS.TEXT });

            // Emit title, author and DCT from the <fileDesc> element, if present
            if (this.document.getFileDesc() != null) {
                final FileDesc fd = this.document.getFileDesc();
                emitTriple(docIRI, DCTERMS.TITLE, fd.title);
                emitTriple(docIRI, DCTERMS.CREATOR, fd.author);
                emitTriple(docIRI, DCTERMS.CREATED, fd.creationtime);
                emitTriple(docIRI, KS.NAF_FILE_NAME, fd.filename);
                emitTriple(docIRI, KS.NAF_FILE_TYPE, fd.filetype);
                emitTriple(docIRI, KS.NAF_PAGES, fd.pages);
            }

            // Emit the document language, if available
            if (this.document.getLang() != null) {
                emitTriple(docIRI, DCTERMS.LANGUAGE,
                        ModelUtil.languageCodeToIRI(this.document.getLang()));
            }

            // Emit an hash of the whitespace-normalized raw text, if available
            if (this.document.getRawText() != null) {
                final String rawText = this.document.getRawText();
                final StringBuilder builder = new StringBuilder();
                boolean addSpace = false;
                for (int i = 0; i < rawText.length(); ++i) {
                    final char c = rawText.charAt(i);
                    if (Character.isWhitespace(c)) {
                        addSpace = builder.length() > 0;
                    } else {
                        if (addSpace) {
                            builder.append(' ');
                            addSpace = false;
                        }
                        builder.append(c);
                    }
                }
                emitTriple(docIRI, KS.TEXT_HASH, Hash.murmur3(builder.toString()).toString());
            }

            // Link document to its NAF annotation
            emitTriple(docIRI, KS.ANNOTATED_WITH, nafIRI);
            emitTriple(nafIRI, KS.ANNOTATION_OF, docIRI);

            // Emit types, version and publicId of NAF resource
            emitTriple(nafIRI, RDF.TYPE, new IRI[] { KEMT.TEXT_RESOURCE, KS.RESOURCE, KS.NAF });
            emitTriple(nafIRI, KS.VERSION, this.document.getVersion());
            emitTriple(nafIRI, DCTERMS.IDENTIFIER, this.document.getPublic().publicId);

            // Emit information about linguistic processors: dct:created, dct:creatro, ego:layer
            String timestamp = null;
            for (final Map.Entry<String, List<LinguisticProcessor>> entry : this.document
                    .getLinguisticProcessors().entrySet()) {
                emitTriple(nafIRI, KS.LAYER,
                        this.vf.createIRI(KS.NAMESPACE, "layer_" + entry.getKey()));
                for (final LinguisticProcessor lp : entry.getValue()) {
                    if (timestamp == null) {
                        if (!Strings.isNullOrEmpty(lp.getBeginTimestamp())) {
                            timestamp = lp.getBeginTimestamp();
                        } else if (!Strings.isNullOrEmpty(lp.getEndTimestamp())) {
                            timestamp = lp.getEndTimestamp();
                        }
                    }
                    final IRI lpIRI = this.vf.createIRI(ModelUtil.cleanIRI(KS.NAMESPACE
                            + lp.getName() + '.' + lp.getVersion()));
                    emitTriple(nafIRI, DCTERMS.CREATOR, lpIRI);
                    emitTriple(lpIRI, DCTERMS.TITLE, lp.getName());
                    emitTriple(lpIRI, KS.VERSION, lp.getVersion());
                }
            }
            emitTriple(nafIRI, DCTERMS.CREATED, timestamp);

        }



        private void processTimexes() {
            for (final Timex3 timex : ImmutableList.copyOf(this.document.getTimeExs())) {

                //filter only the annotations in the requested sentences
                if (timex.getSpan() == null
                        || this.sentenceIDs[timex.getSpan().getFirstTarget().getSent()]) {
                    try {
                        processTimex(timex);
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing " + NAFUtilsUD.toString(timex) + ", type "
                                + timex.getType() + ", value " + timex.getValue(), ex);
                    }
                }
            }
        }

        private void processTimex(final Timex3 origTimex){

            // Abort if timex has no span (e.g., the DCT)
            if (origTimex.getSpan() == null) {
                return;
            }

            //trim spans and log changes
            Timex3 timex = NAFUtilsUD.trimTimexSpan(this.document,origTimex);
            if (LOGGER.isDebugEnabled()) {
                if (timex==null) {
                    LOGGER.debug("Removed invalid timex "+NAFUtils.toString(origTimex));
                }
                else if (!timex.equals(origTimex)) LOGGER.debug("Replaced timex "+NAFUtils.toString(origTimex)+ " with filtered"+NAFUtils.toString(timex));
            }
            if (timex==null) return;

            // Extract terms, head and label
            final List<Term> terms = this.document.getTermsByWFs(timex.getSpan().getTargets());

//            final String label = NAFUtilsUD.getText(NAFUtilsUD.filterTerms(terms));
            final String type = timex.getType().trim().toLowerCase();

            // create mention if not already existing
            final Mention mention = checkAndGenerateMention(terms,true, true);
            final IRI mentionIRI = mention.mentionIRI;


            //emit semantic annotation of type timex and store in the map of annotation per mention
            final IRI semAnnoIRI = createSemanticAnnotationIRI(timex.getId(),mentionIRI,KEMT.TIMEX);
            Annotation ann = new Annotation(semAnnoIRI,KEMT.TIMEX);
            safeAnnotationPutInMap(mention,ann);

            IRI timexIRI = null;
            // Emit type specific statements
            if (timex.getValue() != null) {
                if (type.equals("date") || type.equals("time")) {
                    if (type.equals("date")) emitTriple(semAnnoIRI, KEMT.TYPE_P, KEMT.TT_DATE);
                    else emitTriple(semAnnoIRI, KEMT.TYPE_P, KEMT.TT_TIME);

                    final OWLTime.Interval interval = OWLTime.Interval
                            .parseTimex(timex.getValue());
                    if (interval != null) {
                        timexIRI = interval.toRDF(RDFHandlers.wrap(this.model),
                                NAFExtractorUD.this.owltimeNamespace, null);
                    } else {
                        LOGGER.debug("Could not represent date/time value '" + timex.getValue()
                                + "' of " + NAFUtilsUD.toString(timex));
                    }

                } else if (type.equals("duration")) {
                    emitTriple(semAnnoIRI, KEMT.TYPE_P, KEMT.TT_DURATION);
                    final OWLTime.Duration duration = OWLTime.Duration
                            .parseTimex(timex.getValue());
                    if (duration != null) {
                        timexIRI = this.vf.createIRI(NAFExtractorUD.this.owltimeNamespace,
                                duration.toString());
                        final IRI durationIRI = duration.toRDF(RDFHandlers.wrap(this.model),
                                NAFExtractorUD.this.owltimeNamespace, null);
                        emitTriple(timexIRI, OWLTIME.HAS_DURATION_DESCRIPTION, durationIRI);
                    } else {
                        LOGGER.debug("Could not represent duration value '" + timex.getValue()
                                + "' of " + NAFUtilsUD.toString(timex));
                    }
                } else {

                    // TODO: support SET?
                    throw new UnsupportedOperationException("Unsupported TIMEX3 type: " + type);
                }
            }

            // Generate a default timex IRI on failure
            if (timexIRI == null) {
                timexIRI = mintIRI(timex.getId(),
                        MoreObjects.firstNonNull(timex.getValue(), timex.getSpan().getStr()));
            }




//            attach timex to semantic annotation
            emitTriple(semAnnoIRI, KEMT.OBJECT_VALUE, timexIRI);

            //attach raw string to timex annotation
            emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created annotation '{}' of type '{}' on mention '{}'",semAnnoIRI.getLocalName(),KEMT.TIMEX.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(mention.extent));
            }

        }


        private void processEntities() {

            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 2.1 Processing Compounds / Flats");
            processCompoundEntities();

            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 2.2 Processing NER entities");
            processNerEntities();

            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 2.3 Processing Linked Entities");
            processLinkedEntities();

            if (LOGGER.isDebugEnabled()) LOGGER.debug("STEP 2.4 Processing Entities from Additional Terms");
            processAdditionalEntities();
        }


        private void processCompoundEntities() {

//       1. Look for flat/compound equivalence classes
            List<Set<Term>> equivalences = new ArrayList<>();
            for (Dep dep : this.document.getDeps()
                    ) {
                if ((dep.getRfunc().toLowerCase().startsWith("flat")) || (dep.getRfunc().toLowerCase().startsWith("compound"))) { //potentially we could handle also mwe here
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

//            2. Create entity for each flat/compound
            for (Set<Term> set : equivalences
                    ) {
                //get span
                Span<Term> span = KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(set));
                //get a list version of the terms
                List<Term> list = new ArrayList(set);

                //abort if the compound contains VERBs
                boolean toBeProcessed = true;
                for (Term term : set
                        ) {
                    if (term.getUpos().equalsIgnoreCase("VERB")) {
                        toBeProcessed = false;
                        break;
                    }
                }
                if (!toBeProcessed) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Removed possible compound [" + span.getStr()
                                + "] containing a VERB");
                    }
                    continue;
                }

                //abort if the compound overlaps a timex
                if (isTermListAnnotatedWithAnnotationType(list,KEMT.TIMEX)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Removed possible compound [" + span.getStr() +
                                "] overlapping with TIMEX3");
                    }
                    continue;
                }

                //abort if spans are not consecutive
                if (!NAFUtilsUD.isSpanConsecutive(this.document, span)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Removed possible compound [" + span.getStr()
                                + "] as span contains non consecutive terms");
                    }
                    continue;
                }

                final Mention mention = checkAndGenerateMention(list,true, true);
                if (mention!=null) {

                    final IRI semAnnoIRI = createSemanticAnnotationIRI("compound"+span.getTargets().get(0).getStr(),mention.mentionIRI,KEMT.ENTITY_ANNOTATION);
                    Annotation ann = new Annotation(semAnnoIRI,KEMT.ENTITY_ANNOTATION);
                    safeAnnotationPutInMap(mention,ann);
                    emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(list));
                    Term head = NAFUtilsUD.extractHead(this.document,span);
                    if (head!=null) emitCommonAttributesAnnotation(semAnnoIRI,head); //this check is not needed has compound/flat always have a head
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Added Compound/flat [" + span.getStr()+"]");
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Created annotation '{}' of type '{}' on mention '{}'",semAnnoIRI.getLocalName(),KEMT.ENTITY_ANNOTATION.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(mention.extent));
                    }
                }
            }
        }

        private void processNerEntities(){

            for (final Entity entity : ImmutableList.copyOf(this.document.getEntities())) {
                final Span<Term> span = entity.getSpans().get(0);
                if (this.sentenceIDs[span.getFirstTarget().getSent()]) {
                   try {
                       processNerEntity(entity);
                   } catch (final Throwable ex) {
                       LOGGER.error("Error processing " + NAFUtilsUD.toString(entity)
                            + ", type " + entity.getType(), ex);
                   }
                }
            }
        }



        private void processNerEntity(final Entity origEntity) throws RDFHandlerException {

            //trim entity span
            Entity entity = NAFUtilsUD.trimEntitySpan(this.document,origEntity);
            if (LOGGER.isDebugEnabled()) {
                if (entity==null) {
                    LOGGER.debug("Removed invalid entity "+NAFUtils.toString(origEntity));
                }
                else if (!entity.equals(origEntity)) LOGGER.debug("Replaced timex "+NAFUtils.toString(origEntity)+ " with filtered"+NAFUtils.toString(entity));
            }
            if (entity==null) return;

            // Retrieve terms, head and label
            final List<Term> terms = entity.getSpans().get(0).getTargets();
            final String label = NAFUtilsUD.getText(NAFUtilsUD.filterTerms(terms));

            //abort if the compound overlaps a timex
            if (isTermListAnnotatedWithAnnotationType(terms,KEMT.TIMEX)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removed entity [" + NAFUtilsUD.toString(entity) +
                            "] overlapping with TIMEX3");
                }
                return;
            }

            // Extract type information (type IRI, whether timex or attribute) based on NER tag
            String type = entity.getType();
            type = type == null ? null : type.toLowerCase();
//            final boolean isLinked = !entity.getExternalRefs().isEmpty();
            final boolean isProperty = "money".equals(type) || "cardinal".equals(type)
                    || "ordinal".equals(type) || "percent".equals(type) || "language".equals(type)
                    || "norp".equals(type) || "quantity".equals(type);

            //check if named entity
            final boolean named = entity.isNamed() || "romanticism".equalsIgnoreCase(label)
                    || "operant conditioning chamber".equalsIgnoreCase(label); // TODO

//          Discard attributes in modifier position (DROPPER
//            final Dep dep = this.document.getDepToTerm(head);
//            if (isProperty && dep != null) {
//                final String depLabel = dep.getRfunc().toUpperCase();
//                if (depLabel.contains("NMOD") || depLabel.contains("AMOD")) {
//                    return;
//                }
//            }

            // create mention
            Mention mention = checkAndGenerateMention(terms,true, true);
            final IRI mentionIRI=mention.mentionIRI;

            //CREATE THE NER ANNOTATION(S)
            //check external ref for other NERC types
            boolean hasOtherNercTypes = false;
            for (final ExternalRef ref : entity.getExternalRefs()) {
                    final String resource = ref.getResource();
                    if ((resource.equals("value-confidence"))||(resource.equals("nerc-probmodel"))) {
                        hasOtherNercTypes=true;
                        //                        emit semantic annotation
                        String reference = ref.getReference();
                        //emit semantic annotation and store in the map of annotation per mention
                        final IRI semAnnoIRI = createSemanticAnnotationIRI(entity.getId()+reference,mentionIRI,KEMT.ENTITY_ANNOTATION);
                        Annotation ann = new Annotation(semAnnoIRI,KEMT.ENTITY_ANNOTATION);
                        safeAnnotationPutInMap(mention,ann);
                        //emit type
                        emitTriple(semAnnoIRI, ITSRDF.TA_CLASS_REF, this.vf.createIRI(DEFAULT_NER_NAMESPACE+reference));
                        //emit confidence if available
                        if (ref.hasConfidence()) emitTriple(semAnnoIRI,NIF.CONFIDENCE , ref.getConfidence());
                        if (named) {
                            emitTriple(semAnnoIRI, RDF.TYPE, KEMT.NAMED_ENTITY);
                            emitTriple(semAnnoIRI, KEMT.PROPER_NAME, label);
                        }
                        //attach raw string to annotation
                        emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));
                        Term head = NAFUtilsUD.extractHead(this.document,entity.getSpans().get(0));
                        if (head!=null) emitCommonAttributesAnnotation(semAnnoIRI,head);

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Created annotation '{}' of type '{}' on mention '{}'",semAnnoIRI.getLocalName(),named? KEMT.NAMED_ENTITY.getLocalName():KEMT.ENTITY_ANNOTATION.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(mention.extent));
                        }

                    }
            }
            //there are no other nerc types in external ref, use the type attribute of the entity
            if ((!hasOtherNercTypes)&&(type!=null)) {
                //emit semantic annotation of type timex and store in the map of annotation per mention
                final IRI semAnnoIRI = createSemanticAnnotationIRI(entity.getId()+type,mentionIRI,KEMT.ENTITY_ANNOTATION);
                Annotation ann = new Annotation(semAnnoIRI,KEMT.ENTITY_ANNOTATION);
                safeAnnotationPutInMap(mention,ann);
                emitTriple(semAnnoIRI, ITSRDF.TA_CLASS_REF, this.vf.createIRI(DEFAULT_NER_NAMESPACE+type));
                if (isProperty) {
                    emitEntityAttributes(entity, semAnnoIRI);
                }
                if (named) {
                    emitTriple(semAnnoIRI, RDF.TYPE, KEMT.NAMED_ENTITY);
                    emitTriple(semAnnoIRI, KEMT.PROPER_NAME, label);
                }
                //attach raw string to annotation
                emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));
                Term head = NAFUtilsUD.extractHead(this.document,entity.getSpans().get(0));
                if (head!=null) emitCommonAttributesAnnotation(semAnnoIRI,head);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created annotation '{}' of type '{}' on mention '{}'",semAnnoIRI.getLocalName(),named? KEMT.NAMED_ENTITY.getLocalName():KEMT.ENTITY_ANNOTATION.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(mention.extent));
                }
            }
        }

        private void processLinkedEntities(){

            for (final LinkedEntity entity : ImmutableList.copyOf(this.document.getLinkedEntities())) {

                final List<Term> terms = document.getTermsByWFs(entity.getWFs().getTargets());
                if (this.sentenceIDs[terms.get(0).getSent()]) {
                    try {
                        processLinkedEntity(entity);
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing Linked Entity " + NAFUtilsUD.toString(entity)
                                + ", type ", ex);
                    }
                }
            }

        }


        private void processLinkedEntity(LinkedEntity origLinkedEntity){

            //trim entity span
            LinkedEntity linkedEntity = NAFUtilsUD.trimLinkedEntitySpan(this.document,origLinkedEntity);
            if (LOGGER.isDebugEnabled()) {
                if (linkedEntity==null) {
                    LOGGER.debug("Removed invalid linked entity "+NAFUtils.toString(origLinkedEntity));
                }
                else if (!linkedEntity.equals(origLinkedEntity)) LOGGER.debug("Replaced linked entity on "+NAFUtils.toString(origLinkedEntity)+ " with filtered"+NAFUtils.toString(linkedEntity));
            }
            if (linkedEntity==null) return;

            // Retrieve terms, head and label
            final List<Term> terms = document.getTermsByWFs(linkedEntity.getWFs().getTargets());
            final String label = NAFUtilsUD.getText(NAFUtilsUD.filterTerms(terms));

            //abort if the compound overlaps a timex
            if (isTermListAnnotatedWithAnnotationType(terms,KEMT.TIMEX)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removed linked entity [" + NAFUtilsUD.toString(linkedEntity) +
                            "] overlapping with TIMEX3");
                }
                return;
            }

            final String resource = linkedEntity.getResource();
            if (resource.startsWith("dbpedia-")){
                // create mention
                Mention mention = checkAndGenerateMention(terms,true, true);
                final IRI mentionIRI=mention.mentionIRI;
                final IRI refIRI = this.vf.createIRI(Util.cleanIRI(linkedEntity.getReference()));

                final IRI semAnnoIRI = createSemanticAnnotationIRI(linkedEntity.getId()+"_"+refIRI.getLocalName(),mentionIRI,KEMT.ENTITY_ANNOTATION);
                Annotation ann = new Annotation(semAnnoIRI,KEMT.ENTITY_ANNOTATION);
                safeAnnotationPutInMap(mention,ann);
                //emit linking
                emitTriple(semAnnoIRI, ITSRDF.TA_IDENT_REF, refIRI);
                //emit confidence (it seems there is no way to check if the value is available...)
                emitTriple(semAnnoIRI,NIF.CONFIDENCE , linkedEntity.getConfidence());
                //attach raw string to annotation
                emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));

                Term head = NAFUtilsUD.extractHead(this.document,KAFDocument.newTermSpan(terms));
                if (head!=null) emitCommonAttributesAnnotation(semAnnoIRI,head);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created annotation '{}' of type '{}' on mention '{}'",semAnnoIRI.getLocalName(),KEMT.ENTITY_ANNOTATION.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(mention.extent));
                }
            }
        }



        private void processAdditionalEntities(){

            for (final Term term : document.getTerms()) {

                if (this.sentenceIDs[term.getSent()]) {
                    try {
                        processAdditionalEntity(term);
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing Additional Entity on " + NAFUtilsUD.toString(term)
                                + ", type ", ex);
                    }
                }


            }
        }


        private void processAdditionalEntity(Term term){


            final String pos = term.getUpos();

            //            final Dep dep = document.getDepToTerm(term);
            //check if it is part of a name
//            final boolean namePart = pos.equalsIgnoreCase("PROPN") && dep != null
//                    && dep.getRfunc().toLowerCase().contains("name")
//                    && dep.getFrom().getUpos().equalsIgnoreCase("PROPN")
//                    && document.getEntitiesByTerm(dep.getFrom()).isEmpty();

//            final boolean namePart = pos.equalsIgnoreCase("PROPN");

//            if (!pos.equalsIgnoreCase("PROPN")  && !pos.equalsIgnoreCase("NOUN")  && !pos.equalsIgnoreCase("PRON") && !pos.equalsIgnoreCase("DET") || namePart
//                    || !document.getTimeExsByWF(term.getWFs().get(0)).isEmpty() //
//                    || !document.getEntitiesByTerm(term).isEmpty()) {
//                return;
//            }

            //abort if there is already a timex
            if (isTermAnnotatedWithAnnotationType(term,KEMT.TIMEX)) return;
            //abort if there is already a timex
            final boolean named = pos.equalsIgnoreCase("PROPN") && (getBestMention(term.getId())==null);



            //let's start just with nouns that are not part of timex
            if (!pos.equalsIgnoreCase("NOUN") && !pos.equalsIgnoreCase("PRON") && !named) {
                return;
            }

            // Determine the entity type based on BBN first, WN synset then and SST last
            String type = null;
            final ExternalRef bbnRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_BBN, null);
            if (bbnRef != null) {
                type = bbnRef.getReference();
            } else {
                final ExternalRef synsetRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_WN_SYNSET,
                        null);
                if (synsetRef != null) {
                    type = WordNet.mapSynsetToBBN(synsetRef.getReference());
                } else {
                    final ExternalRef sstRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_WN_SST,
                            null);
                    if (sstRef != null) {
                        String sst = sstRef.getReference();
                        sst = sst.substring(sst.lastIndexOf('.') + 1);
                        type = NAFUtilsUD.ENTITY_SST_TO_TYPES.get(sst);
                    }
                }
            }

            //multiword should be already covered by compound / flat
            //restrict only to noun with a type?

            Mention mention = checkAndGenerateMention(Arrays.asList(term),true, true);
            final IRI semAnnoIRI = createSemanticAnnotationIRI(term.getId()+"_"+type, mention.mentionIRI, KEMT.ENTITY_ANNOTATION);
            Annotation ann = new Annotation(semAnnoIRI, KEMT.ENTITY_ANNOTATION);
            safeAnnotationPutInMap(mention, ann);
            if (type!=null) emitTriple(semAnnoIRI, ITSRDF.TA_CLASS_REF, this.vf.createIRI(DEFAULT_NER_NAMESPACE + type));
            if (named) {
                emitTriple(semAnnoIRI, RDF.TYPE, KEMT.NAMED_ENTITY);
                emitTriple(semAnnoIRI, KEMT.PROPER_NAME, term.getStr());
            }
            //attach raw string to annotation
            emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(Arrays.asList(term)));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Added " + NAFUtilsUD.toString(term) + " with type '" + type + "'");
                LOGGER.debug("Created annotation '{}' of type '{}' on mention '{}'",semAnnoIRI.getLocalName(),named?KEMT.NAMED_ENTITY.getLocalName():KEMT.ENTITY_ANNOTATION.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(mention.extent));
            }
            //CREATE TERM ANNOTATIONS (WSD, SST)
            emitCommonAttributesAnnotation(semAnnoIRI,term);

        }

        private void processPredicates(){
            for (final Predicate predicate : ImmutableList.copyOf(this.document.getPredicates())) {
                //filter only the annotations in the requested sentences
                if (this.sentenceIDs[predicate.getSpan().getFirstTarget().getSent()]) {
                    try {
                        processPredicate(predicate);
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing " + NAFUtilsUD.toString(predicate), ex);
                    }
                }
            }
        }



        private void processPredicate(final Predicate predicate) throws RDFHandlerException {

            // Retrieve terms, head and label
            final List<Term> terms = predicate.getSpan().getTargets();
            final String label = NAFUtilsUD.getText(NAFUtilsUD.filterTerms(terms));
            final Term head = NAFUtilsUD.extractHead(this.document, predicate.getSpan()); //head always exists for predicates

            //abort if the compound overlaps a timex
            if (isTermListAnnotatedWithAnnotationType(terms,KEMT.TIMEX)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removed predicate [" + NAFUtilsUD.toString(predicate) +
                            "] overlapping with TIMEX3");
                }
                this.document.removeAnnotation(predicate);
                return;
            }

            // Determine the lemma, handling multiwords
            final StringBuilder builder = new StringBuilder();
            for (final Term term : terms) {
                builder.append(builder.length() == 0 ? "" : "_");
                builder.append(term.getLemma().toLowerCase());
            }
            final String lemma = builder.toString();
            final String POS = head.getUpos();

            // create mention if not already existing
            Mention mention = checkAndGenerateMention(terms,true, true);
            final IRI mentionIRI = mention.mentionIRI;

            this.nafIdMentions.put(predicate.getId(),mention); //used later for participations

            //add lemma and pos for framebase mappings
            emitTriple(mentionIRI,NIF.LEMMA,lemma);
            emitTriple(mentionIRI,NIF.OLIA_LINK,this.vf.createIRI(DEFAULT_OLIA_UD_POS+POS));

            IRI lastSemAnnoIRI = null;
            // Process framenet/verbnet/etc external refs
            for (final ExternalRef ref : predicate.getExternalRefs()) {
//                we don't wnat dbpedia on predicates'
                if (ref.getResource().startsWith("dbpedia")){
                    continue;
                }
                if ("".equals(ref.getReference())) {
                    continue;
                }
                final IRI typeIRI = mintRefIRI(ref.getResource(), ref.getReference());
                //emit semantic annotation of type timex and store in the map of annotation per mention
                final IRI semAnnoIRI = createSemanticAnnotationIRI(predicate.getId()+"_"+typeIRI.getLocalName(),mentionIRI,KEMT.PREDICATE_C);
                lastSemAnnoIRI = semAnnoIRI;
                Annotation ann = new Annotation(semAnnoIRI,KEMT.PREDICATE_C);
                safeAnnotationPutInMap(mention,ann);

                emitTriple(semAnnoIRI,ITSRDF.TA_CLASS_REF,typeIRI);

                //attach raw string to annotation
                emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created annotation '{}' of type '{}' on mention '{}'",semAnnoIRI.getLocalName(),KEMT.PREDICATE_C.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(mention.extent));
                }

            }
            //CREATE TERM ANNOTATIONS (WSD, SST)
            if ((lastSemAnnoIRI!=null)&&(head!=null)) emitCommonAttributesAnnotation(lastSemAnnoIRI,head);
        }



        private void  processCoordinations (){

            Map<Integer, Set<Mention>> sentenceMentions = Maps.newHashMap();
            List<Set<Mention>> coordinatedMentionList = new ArrayList<>();

//       1. Look for coordinations term equivalence classes
            List<Set<Term>> equivalences = new ArrayList<>();
            for (Dep dep : this.document.getDeps()
                    ) {
                if (dep.getRfunc().toLowerCase().startsWith("conj")){
                    Term from = dep.getFrom();
                    if (!this.sentenceIDs[from.getSent()]) continue; //skip this dependency if the sentence is not among the ones to consider
                    Term to = dep.getTo();
                    boolean found = false;
                    for (Set set : equivalences
                            ) {
                        if ((set.contains(to)) || (set.contains(from))) {
                            set.add(from);
                            set.add(to);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Set set = new HashSet<>();
                        set.add(from);
                        set.add(to);
                        equivalences.add(set);
                    }
                }
            }


//          2.  for each cordination equivalence, get the corresponding mentions
            for (Set<Term> coordinatedTerms : equivalences){
                if (coordinatedTerms.size()>1) {
                    Set<Mention> coordinatedMentions = new HashSet<>();
                    for (final Term term : coordinatedTerms) {
                        final Mention depMen = getBestMention(term.getId());
                        if (depMen != null)
                            coordinatedMentions.add(depMen);
                    }
                    if (!coordinatedMentions.isEmpty()) coordinatedMentionList.add(coordinatedMentions);
                }
            }

//            3. materialize the coordinations

            for (Set<Mention> coordinatedMentions :coordinatedMentionList
                 ) {

                List<Term> terms = Lists.newArrayList();
                List<IRI> mentionsIRI = Lists.newArrayList();
                List<IRI> coordinatedIRI = Lists.newArrayList();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Found Coordination between {} mentions:",coordinatedMentions.size());
                }
                for (Mention depMen: coordinatedMentions
                        ) {
                    terms.addAll(depMen.extent);
                    mentionsIRI.add(depMen.mentionIRI);
                    //emit the entity annotation for each coordinated entity
                    final IRI semAnnoIRI = createSemanticAnnotationIRI("coordItem",depMen.mentionIRI,KEMT.ENTITY_ANNOTATION);
                    coordinatedIRI.add(semAnnoIRI);
                    final Annotation ann = new Annotation(semAnnoIRI,KEMT.ENTITY_ANNOTATION);
                    safeAnnotationPutInMap(depMen,ann);
                    //attach raw string to annotation (here is the same as the mention)
                    emitTriple(semAnnoIRI, KEMT.RAW_STRING, depMen.mentionIRI);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("  Mention '{}'",Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(depMen.extent));
                    }
                }

                //emit group entity mention (it can't already exists)
                final Mention groupEntityMention = checkAndGenerateMention(terms,false,false);
                final IRI groupEntityMentionIRI = groupEntityMention.mentionIRI;

                //emit group entity annotation
                final IRI groupEntityIRI = createSemanticAnnotationIRI("group",groupEntityMentionIRI,KEMT.ENTITY_ANNOTATION);
                final Annotation groupEntityAnn = new Annotation(groupEntityIRI,KEMT.ENTITY_ANNOTATION);
                safeAnnotationPutInMap(groupEntityMention,groupEntityAnn);

                //attach raw string to annotation (here is the same as the mention)
                emitTriple(groupEntityIRI, KEMT.RAW_STRING, groupEntityMentionIRI);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created Group Entity '{}' on mention '{}'",groupEntityIRI.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(groupEntityMention.extent));
                }
                //emit coordination mention (for the time being, we reuse the group entity one)
                final Mention coordinationMention = groupEntityMention;
                final IRI coordinationMentionIRI = coordinationMention.mentionIRI;

                //emit semantic annotation of type coordination
                final IRI coordinationIRI = createSemanticAnnotationIRI("coord",coordinationMentionIRI,KEMT.COORDINATION);
                final Annotation coordinationAnn = new Annotation(groupEntityIRI,KEMT.COORDINATION);
                safeAnnotationPutInMap(coordinationMention,coordinationAnn);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created Coordination Entity '{}' on mention '{}'",coordinationIRI.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(coordinationMention.extent));
                }

                //attach raw string to annotation (here is the same as the mention)
                emitTriple(coordinationIRI, KEMT.RAW_STRING, coordinationMentionIRI);
                emitTriple(coordinationIRI,KEMT.GROUP,groupEntityIRI);
                for (IRI conjunctIRI:coordinatedIRI
                        )
                    emitTriple(coordinationIRI,KEMT.CONJUNCT,conjunctIRI);
                for (IRI conjunctMentionIRI:mentionsIRI)
                    emitTriple(coordinationIRI,KEMT.CONJUNCT_STRING,conjunctMentionIRI);
            }
        }



        private void processCoreferences() {
            for (final Coref coref : this.document.getCorefs()) {
                if ("event".equalsIgnoreCase(coref.getType())) {
                    continue;
                }
                final List<Span<Term>> spans = Lists.newArrayList();
                for (final Span<Term> span : coref.getSpans()) {
                    if (this.sentenceIDs[span.getFirstTarget().getSent()]) {
                        spans.add(span);
                    }
                }
                if (!spans.isEmpty()) {
                    try {
                        processCoref(spans,coref.getId());
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing " + NAFUtilsUD.toString(coref), ex);
                    }
                }
            }
        }

        @SuppressWarnings("Duplicates")
        private void processCoref(final List<Span<Term>> spans, String corefID) {

            // Build three correlated lists containing, for each member of the coref cluster, its
            // span, the head terms in the span and the associated IRIs
            final List<Span<Term>> corefSpans = Lists.newArrayList();
            final List<Term> corefRawTerms = Lists.newArrayList();
            final List<Mention> corefMentions = Lists.newArrayList();
            final List<Term> corefMentionTerms = Lists.newArrayList();

            //iterate over all spans of a coreference, keep only the spans for which there exists a mention
            for (final Span<Term> span : spans) {

                final Term head = NAFUtilsUD.extractHead(this.document, span); //todo what about cases we have no head in the span? May it happen?
                if (head != null) {
                    Mention correspondingMention = getBestMention(head.getId());
                    if (correspondingMention!=null) {
                        corefMentions.add(correspondingMention);
                        corefSpans.add(span);
                        corefMentionTerms.addAll(correspondingMention.extent);
                        corefRawTerms.addAll(span.getTargets());
                    }
                }
            }

            // Abort in case there is only one remaining member in the coref cluster
            if (corefSpans.size() <= 1) {
                return;
            }

            //there is no need to create a Mention object, as cooreferences are relations (i.e., we will not attach a role to a coreference mention...
            //emit coreference mention (it can't already exists)
            final IRI coreferenceMentionIRI = emitMention(corefMentionTerms);
            //emit coreference annotation annotation
            final IRI coreferenceIRI = createSemanticAnnotationIRI(corefID,coreferenceMentionIRI,KEMT.COREFERENCE);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Created Coreference annotation '{}' on mention '{}'",coreferenceIRI.getLocalName(),Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(corefMentionTerms));

            for (int i = 0; i < corefMentions.size(); i++) {
                //emit coreferent
                final IRI coreferentIRI = createSemanticAnnotationIRI(corefID,corefMentions.get(i).mentionIRI,KEMT.ENTITY_ANNOTATION);
                emitTriple(coreferenceIRI,KEMT.COREFERRING,coreferentIRI);
                //emit coreferent raw string (i.e., its original span)
                emitTriple(coreferentIRI, KEMT.RAW_STRING, emitFragment(corefSpans.get(i).getTargets()));
            }

            //emit coreference raw string (i.e., its original span)
            emitTriple(coreferenceIRI, KEMT.RAW_STRING, emitFragment(corefRawTerms));
        }

        private void processRoles() {
            for (final Predicate predicate : this.document.getPredicates()) {
                for (final Role role : predicate.getRoles()) {


                    if (this.sentenceIDs[role.getSpan().getFirstTarget().getSent()]) {
                        try {
                            processRole(predicate, role);
                        } catch (final Throwable ex) {
                            LOGGER.error("Error processing " + NAFUtilsUD.toString(role)
                                    + " of " + NAFUtilsUD.toString(predicate), ex);
                        }
                    }
                }
            }
        }









        private void processRole(final Predicate predicate, final Role role) {


            //get predicate mention
            final Mention predMention = this.nafIdMentions.get(predicate.getId());

            //get the head if it exists
            final Term head = NAFUtilsUD.extractHead(this.document, role.getSpan());

            Mention correspondingMention=null;

            //if the head exists, get the best span on the head
            if (head!=null)  correspondingMention = getBestMention(head.getId());
            else {
                //try to see if there is a headless mention with exacltly the same span
                Set<Mention> candidates = getAllMentionsForTerm(role.getSpan().getTargets().get(0).getId());
                for (Mention m:candidates
                     ) {
                    if (role.getSpan().getTargets().equals(m.extent))
                        correspondingMention=m;
                }
            }

            // this covers several cases ( no mention on that head, no mention with the same span, etc...
            if (correspondingMention==null) return;

            //emit fake predicate and role for participation relation
            final IRI fakePredIRI = createSemanticAnnotationIRI(predicate.getId(),predMention.mentionIRI,KEMT.PREDICATE_C);
            final IRI fakeRoleIRI = createSemanticAnnotationIRI(role.getId(),correspondingMention.mentionIRI,KEMT.ARGUMENT_C);

            //emit fake predicate and role raw string
            final IRI fakePredRawString = emitFragment(predicate.getSpan().getTargets());
            emitTriple(fakePredIRI,KEMT.RAW_STRING,fakePredRawString);
            final IRI fakeRoleRawString = emitFragment(role.getSpan().getTargets());
            emitTriple(fakeRoleIRI,KEMT.RAW_STRING,fakeRoleRawString);

            //emit participation mention
            final IRI partMentionIRI = emitMention(Stream.concat(predMention.extent.stream(), correspondingMention.extent.stream())
                    .collect(Collectors.toList()));
            //emit participation raw string
            final IRI partRawIRI = emitMention(Stream.concat(predicate.getSpan().getTargets().stream(), role.getSpan().getTargets().stream())
                    .collect(Collectors.toList()));
            //emit participation annotation
            final IRI participationIRI = createSemanticAnnotationIRI(predicate.getId()+"_"+role.getId(),partMentionIRI,KEMT.PARTICIPATION);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created participation annotation '{}' between predicate '{}' and role '{}'",participationIRI.getLocalName(),NAFUtilsUD.toString(predicate),NAFUtilsUD.toString(role));
            }

            emitTriple(participationIRI,KEMT.PREDICATE_P,fakePredIRI);
            emitTriple(participationIRI,KEMT.ARGUMENT_P,fakeRoleIRI);
            emitTriple(participationIRI,KEMT.RAW_STRING,partRawIRI);

            for (final ExternalRef ref : role.getExternalRefs()) {
                if ("".equals(ref.getReference())) {
                    continue;
                }
                //emit role annotation
                final IRI typeIRI = mintRefIRI(ref.getResource(), ref.getReference());
                final IRI roleIRI = createSemanticAnnotationIRI(role.getId()+"_"+typeIRI.getLocalName(),correspondingMention.mentionIRI,KEMT.ARGUMENT_C);
                Annotation ann = new Annotation(roleIRI,KEMT.ARGUMENT_C);
                safeAnnotationPutInMap(correspondingMention,ann);
                emitTriple(roleIRI,ITSRDF.TA_PROP_REF,typeIRI);
                emitTriple(roleIRI,KEMT.RAW_STRING,fakeRoleRawString);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created participation annotation '{}' of type '{}' between predicate '{}' and role '{}'",roleIRI.getLocalName(),typeIRI,NAFUtilsUD.toString(predicate),NAFUtilsUD.toString(role));
                }
            }
        }



        private Mention checkAndGenerateMention(List<Term> terms, boolean emitContainment, boolean emitSameHead) {

            //get set version of the list
            final Set<Term> termsSet = new HashSet<>(terms);
            Set<Mention> overlappingMentions = new HashSet<>();

            for (Term term:terms
                 ) {
                overlappingMentions.addAll(getAllMentionsForTerm(term.getId()));
            }

            final Set<Mention> containedIn = new HashSet<>(); //for storing the mentions which contain the new one
            final Set<Mention> containing = new HashSet<>(); //for storing the mentions contained in the new one

            for (Mention existingMention:overlappingMentions
                    ) {
                final Set<Term> existingTermsSet = new HashSet<>(existingMention.extent);
                if ((!termsSet.containsAll(existingTermsSet))&&(!existingTermsSet.containsAll(termsSet))) {
                    //Houston we have a problem....
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Bad mention overlapping for [" + NAFUtilsUD.toString(KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(termsSet)))
                                + "]");
                    }
                    return null;
                }
                if ((termsSet.containsAll(existingTermsSet))&&(!existingTermsSet.containsAll(termsSet))) {
                    containing.add(existingMention);
                }
                if ((!termsSet.containsAll(existingTermsSet))&&(existingTermsSet.containsAll(termsSet))) {
                    containedIn.add(existingMention);
                }
            }

            // we don't care if we already generated the mention
            final IRI mentionIRI = emitMention(terms);
            final Mention mention = new Mention(terms.get(0),terms,mentionIRI);
            for (Term term :termsSet) {
                safeMentionPutInMap(term.getId(),mention);
            }

//          add substring for entities contained in the new mention
            for (Mention me :containing) {
                if (emitContainment) emitTriple(me.mentionIRI,NIF.SUB_STRING,mentionIRI);
                if (emitSameHead) emitSameHeadOnMentions(me,mention);
            }
//          add substring for entities which contains the new mention
            for (Mention me :containedIn) {
                if (emitContainment) emitTriple(mentionIRI,NIF.SUB_STRING,me.mentionIRI);
                if (emitSameHead) emitSameHeadOnMentions(mention,me);
            }

            return mention;
        }


        private void emitSameHeadOnMentions(final Mention A, final Mention B) {

            Term headA = NAFUtilsUD.extractHead(this.document,KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(A.extent)));
            Term headB = NAFUtilsUD.extractHead(this.document,KAFDocument.newTermSpan(Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(B.extent)));
            if (headA==null) return;
            if (headB==null) return;
            if (headA.getId().equals(headB.getId())) emitTriple(A.mentionIRI,KEMT.HAS_SAME_HEAD_AS,B.mentionIRI);
        }



        @Nullable
        private IRI emitMention(final Iterable<Term> terms) {

            final List<Term> sortedTerms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
            final int numTerms = sortedTerms.size();
            if (numTerms == 0) {
                return null;
            }

            final IRI mentionID = emitFragment(sortedTerms);
            emitTriple(mentionID, RDF.TYPE, KEM.MENTION);
            return mentionID;
        }


        private IRI emitFragment(final Iterable<Term> terms) {

            final List<Term> sortedTerms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(terms);
            final int numTerms = sortedTerms.size();
            if (numTerms == 0) {
                return null;
            }

            final String text = this.documentText;
            final List<IRI> componentIRIs = Lists.newArrayList();
            final int begin = NAFUtilsUD.getBegin(sortedTerms.get(0));
            int offset = begin;
            int startTermIdx = 0;

            final StringBuilder anchorBuilder = new StringBuilder();
            final StringBuilder uriBuilder = new StringBuilder(this.documentIRI.stringValue())
                    .append("#char=").append(begin).append(",");

            for (int i = 0; i < numTerms; ++i) {
                final Term term = sortedTerms.get(i);
                final int termOffset = NAFUtilsUD.getBegin(term);
                if (termOffset > offset && !text.substring(offset, termOffset).trim().isEmpty()) {
                    final int start = NAFUtilsUD.getBegin(sortedTerms.get(startTermIdx));
                    anchorBuilder.append(text.substring(start, offset)).append(" [...] ");
                    uriBuilder.append(offset).append(";").append(termOffset).append(',');
                    componentIRIs.add(emitFragment(sortedTerms.subList(startTermIdx, i)));
                    startTermIdx = i;
                }
                offset = NAFUtilsUD.getEnd(term);
            }
            if (startTermIdx > 0) {
                componentIRIs.add(emitFragment(sortedTerms.subList(startTermIdx, numTerms)));
            }


            anchorBuilder.append(text.substring(NAFUtilsUD.getBegin(sortedTerms.get(startTermIdx)),
                    offset));
            uriBuilder.append(offset);

            final String anchor = anchorBuilder.toString();
            final IRI fragmentID = this.vf.createIRI(uriBuilder.toString());
            emitTriple(fragmentID, KEM.FRAGMENT_OF, this.documentIRI);


//            if not composite --> RFC5147
            if (!componentIRIs.isEmpty()) {
                emitTriple(fragmentID, RDF.TYPE, KEM.COMPOSITE_FRAGMENT);
                for (final IRI componentIRI : componentIRIs) {
                    emitTriple(fragmentID, KEM.HAS_COMPONENT, componentIRI);
                }
            } else emitTriple(fragmentID, RDF.TYPE, NIF.RFC5147_STRING);

            emitTriple(fragmentID, NIF.BEGIN_INDEX, this.vf.createLiteral(begin));
            emitTriple(fragmentID, NIF.END_INDEX, this.vf.createLiteral(offset));
            emitTriple(fragmentID, NIF.ANCHOR_OF, this.vf.createLiteral(anchor));

            return fragmentID;
        }


        private IRI createSemanticAnnotationIRI(final String id, final IRI mentionIRI, final IRI type){

            final IRI semanticAnnotationIRI = this.vf.createIRI(mentionIRI.toString()+id);
            this.model.add(semanticAnnotationIRI,RDF.TYPE,type);
            this.model.add(mentionIRI,KEM.HAS_ANNOTATION,semanticAnnotationIRI);

            return semanticAnnotationIRI;

        }

        private void emitTriple(@Nullable final IRI subject, @Nullable final IRI property,
                              @Nullable final Object objects) {
            if (subject != null && property != null) {
                for (final Value object : extract(Value.class, objects,
                        RDF.TYPE.equals(property) ? NAFExtractorUD.this.typeMap : null)) {
                    this.model.add(this.vf.createStatement(subject, property, object));
                }
            }
        }

        private IRI mintIRI(final String id, @Nullable final String suggestedLocalName) {
            String localName = this.mintedIRIs.get(id);
            if (localName == null) {
                final String name = MoreObjects.firstNonNull(suggestedLocalName, id);
                final StringBuilder builder = new StringBuilder();
                for (int i = 0; i < name.length(); ++i) {
                    final char c = name.charAt(i);
                    builder.append(Character.isWhitespace(c) ? '_' : c);
                }
                final String base = builder.toString();
                int counter = 1;
                while (true) {
                    localName = base + (counter == 1 ? "" : "_" + counter);
                    if (!this.mintedIRIs.inverse().containsKey(localName)) {
                        this.mintedIRIs.put(id, localName);
                        break;
                    }
                    ++counter;
                }
            }
            return this.vf.createIRI(Util.cleanIRI(this.documentIRI + "#" + localName));
        }


        @Nullable
        private IRI mintRefIRI(@Nullable final String resource, @Nullable final String reference) {
            if (!Strings.isNullOrEmpty(resource) && !Strings.isNullOrEmpty(reference)) {
                final String normResource = resource.toLowerCase();
                final String namespace = NAFExtractorUD.this.namespaceMap.get(normResource);
                if (namespace != null) {
                    return this.vf
                            .createIRI(Util.cleanIRI(namespace + reference.replace('#', '.')));
                } else System.out.println(normResource);
            }
            return null;
        }


        private void emitEntityAttributes(final Entity entity, final IRI subject)
                throws RDFHandlerException {

            // Retrieve normalized value and NER tag
            final ExternalRef valueRef = NAFUtilsUD.getRef(entity, "value", null);
            String nerTag = entity.getType();
            nerTag = nerTag == null ? null : nerTag.toLowerCase();

                if (valueRef != null) {
                // Otherwise, we use the normalized value from Stanford
                try {
                    final String s = valueRef.getReference().trim();
                    if (s.isEmpty()) {
                        return;
                    }
                    if (Objects.equal(nerTag, "cardinal") || Objects.equal(nerTag, "quantity")) {
                        emitTriple(subject, KEMT.OBJECT_VALUE, Double.parseDouble(s));

                    } else if (Objects.equal(nerTag, "ordinal")) {
                        emitTriple(subject, KEMT.OBJECT_VALUE, Double.parseDouble(s));

                    } else if (Objects.equal(nerTag, "percent")) {
                        final int index = s.indexOf('%');
                        emitTriple(subject, KEMT.OBJECT_VALUE, Double.parseDouble(s.substring(index + 1)));
                    } else if (Objects.equal(nerTag, "money")) {
                        int index = 0;
                        while (index < s.length()) {
                            final char c = s.charAt(index);
                            if (c == '€') {
                                emitTriple(subject, KEMT.UNIT, "EUR");
                            } else if (c == '$') {
                                emitTriple(subject, KEMT.UNIT, "USD");
                            } else if (c == '¥') {
                                emitTriple(subject, KEMT.UNIT, "YEN");
                            } else if (Character.isDigit(c)) {
                                break;
                            }
                            ++index;
                        }
                        emitTriple(subject, KEMT.OBJECT_VALUE, Double.parseDouble(s.substring(index)));
                    }
                } catch (final NumberFormatException ex) {
                    LOGGER.debug("Could not process normalized value: " + valueRef.getReference());
                }
            }
        }



        private void emitCommonAttributesAnnotation(final IRI semanticAnnotationIRI, final Term head)
                throws RDFHandlerException {

            //create this semann only if there is at least an external ref

            final ExternalRef sstRef = NAFUtilsUD.getRef(head, NAFUtilsUD.RESOURCE_WN_SST, null);
            final ExternalRef synsetRef = NAFUtilsUD.getRef(head, NAFUtilsUD.RESOURCE_WN_SYNSET, null);
            final ExternalRef bbnRef = NAFUtilsUD.getRef(head, NAFUtilsUD.RESOURCE_BBN, null);

            if ((sstRef != null)||(synsetRef != null)||(bbnRef != null)) {

                //WN SST
                if (sstRef != null) {
                    final String sst = sstRef.getReference();
                    final IRI uri = this.vf.createIRI(DEFAULT_WN_SST_NAMESPACE,
                            sst.substring(sst.lastIndexOf('-') + 1));
                    emitTriple(semanticAnnotationIRI, ITSRDF.TERM_INFO_REF, uri);
                }

                //WN SYNSET
                if (synsetRef != null) {
                    final IRI uri = this.vf.createIRI(DEFAULT_WN_SYN_NAMESPACE,
                            synsetRef.getReference());
                    emitTriple(semanticAnnotationIRI, ITSRDF.TERM_INFO_REF, uri);
                }

                //BBN
                if (bbnRef != null) {
                    final IRI uri = this.vf.createIRI(DEFAULT_BBN_NAMESPACE,
                            bbnRef.getReference());
                    emitTriple(semanticAnnotationIRI, ITSRDF.TERM_INFO_REF, uri);
                }
            }
        }


        private boolean isTermListAnnotatedWithAnnotationType (List<Term> terms, IRI annotationType){

            for (Term term:terms
                 ) {
                if (isTermAnnotatedWithAnnotationType(term,annotationType)) return true;
            }
            return false;
        }

        //check if on the term we have an annotation of a given type (KEMT IRI)
        private boolean isTermAnnotatedWithAnnotationType (Term term, IRI annotationType){

            Set<Mention> mentionList = getAllMentionsForTerm(term.getId());
            for (Mention mention:mentionList
                    ) {
                Set<Annotation> annotations = this.annotations.get(mention);
                for (Annotation annotation: annotations
                        ) {
                    if (annotation.type.equals(annotationType)) return true;
                }

            }
            return false;
        }

        private <T extends Value> Collection<T> extract(final Class<T> clazz,
                                                        @Nullable final Object object, @Nullable final Multimap<String, ? extends T> map) {
            if (object == null) {
                return ImmutableList.of();
            } else if (clazz.isInstance(object)) {
                return ImmutableList.of((T) object);
            } else if (object instanceof Iterable<?>) {
                final List<T> list = Lists.newArrayList();
                for (final Object element : (Iterable<?>) object) {
                    list.addAll(extract(clazz, element, map));
                }
                return list;
            } else if (object.getClass().isArray()) {
                final List<T> list = Lists.newArrayList();
                final int length = Array.getLength(object);
                for (int i = 0; i < length; ++i) {
                    list.addAll(extract(clazz, Array.get(object, i), map));
                }
                return list;
            } else if (map != null) {
                return (Collection<T>) map.get(object.toString());
            } else {
                return ImmutableList.of(Statements.convert(object, clazz));
            }
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private Multimap<String, IRI> typeMap;

        @Nullable
        private Multimap<String, IRI> propertyMap;

        @Nullable
        private Map<String, String> namespaceMap;

        @Nullable
        private String owltimeNamespace;

        @Nullable
        private Boolean merging;

        @Nullable
        private Boolean normalization;

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
                    if ("fusion".equals(name)) {
                        withMerging(Boolean.valueOf(value));
                    } else if ("normalization".equals(name)) {
                        withNormalization(Boolean.valueOf(value));
                    }
                }
            }
            return this;
        }

        public Builder withTypeMap(@Nullable final Multimap<String, IRI> typeMap) {
            this.typeMap = typeMap;
            return this;
        }

        public Builder withPropertyMap(@Nullable final Multimap<String, IRI> propertyMap) {
            this.propertyMap = propertyMap;
            return this;
        }

        public Builder withNamespaceMap(@Nullable final Map<String, String> namespaceMap) {
            this.namespaceMap = namespaceMap;
            return this;
        }

        public Builder withOWLTimeNamespace(@Nullable final String owltimeNamespace) {
            this.owltimeNamespace = owltimeNamespace;
            return this;
        }

        public Builder withMerging(@Nullable final Boolean merging) {
            this.merging = merging;
            return this;
        }

        public Builder withNormalization(@Nullable final Boolean normalization) {
            this.normalization = normalization;
            return this;
        }

        public NAFExtractorUD build() {



            return new NAFExtractorUD(this);
        }

    }


    private static final class Mention {

        IRI mentionIRI;
        final Term head;
        final List<Term> extent;

        Mention(final Term head, final Iterable<Term> extent, final IRI mentionIRI) {
            this.head = head;
            this.extent = ImmutableList.copyOf(extent);
            this.mentionIRI = mentionIRI;
        }
    }


    private static final class Annotation {

        IRI annotationIRI;
        IRI type;

        Annotation(final IRI annotationIRI, final IRI type) {
            this.annotationIRI = annotationIRI;
            this.type = type;
        }
    }
}

//    //    todo check if need to be revised for UD (part on dependency)
//    private void applyEntityAddition(final KAFDocument document) {
//
//        for (final Term term : document.getTerms()) {
//
//            final String pos = term.getUpos();
//            final Dep dep = document.getDepToTerm(term);
//            final boolean namePart = pos.equalsIgnoreCase("PROPN") && dep != null
//                    && dep.getRfunc().toLowerCase().contains("name")
//                    && Character.toUpperCase(dep.getFrom().getPos().charAt(0)) == 'R'
//                    && document.getEntitiesByTerm(dep.getFrom()).isEmpty();
//            if (!pos.equalsIgnoreCase("PROPN")  && !pos.equalsIgnoreCase("NOUN")  && !pos.equalsIgnoreCase("PRON") && !pos.equalsIgnoreCase("DET") || namePart
//                    || !document.getTimeExsByWF(term.getWFs().get(0)).isEmpty() //
//                    || !document.getEntitiesByTerm(term).isEmpty()) {
//                continue;
//            }
//
//            // Determine the entity type based on NER tag first, WN synset then and SST last
//            String type = null;
//            final ExternalRef bbnRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_BBN, null);
//            if (bbnRef != null) {
//                type = bbnRef.getReference();
//            } else {
//                final ExternalRef synsetRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_WN_SYNSET,
//                        null);
//                if (synsetRef != null) {
//                    type = WordNet.mapSynsetToBBN(synsetRef.getReference());
//                } else {
//                    final ExternalRef sstRef = NAFUtilsUD.getRef(term, NAFUtilsUD.RESOURCE_WN_SST,
//                            null);
//                    if (sstRef != null) {
//                        String sst = sstRef.getReference();
//                        sst = sst.substring(sst.lastIndexOf('.') + 1);
//                        type = ENTITY_SST_TO_TYPES.get(sst);
//                    }
//                }
//            }
//
//            // Determine the terms for the nominal node.
//            // TODO: consider multiwords
//            final Span<Term> span = NAFUtilsUD.getNominalSpan(document, term, false, false);
//
//            // Add the entity, setting its type and 'named' flag
//            final Entity entity = document.newEntity(ImmutableList.of(span));
//            if (type!= null) entity.setType(type.toUpperCase().replace("PERSON","PER").replace("ORGANIZATION","ORG").replace("LOCATION","LOC"));
//            entity.setNamed(pos.equalsIgnoreCase("PROPN"));
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("Added " + (entity.isNamed() ? "named " : "")
//                        + NAFUtilsUD.toString(entity) + " with type '" + type + "'");
//            }
//        }
//    }



//    @SuppressWarnings("deprecation")
//    //    todo check if need to be revised for UD (part on remove spans without valid head)
//    private void applyCorefSpanFixing(final KAFDocument document) {
//
//        // Process each <coref> element in the NAF document
//        for (final Coref coref : ImmutableList.copyOf(document.getCorefs())) {
//
//            // Remove spans without valid head
//            for (final Span<Term> span : ImmutableList.copyOf(coref.getSpans())) {
//                final Term head = NAFUtilsUD.extractHead(document, span);
//                if (head == null) {
//                    coref.getSpans().remove(span);
//                    if (LOGGER.isDebugEnabled()) {
//                        LOGGER.debug("Removed span with invalid head '{}' from {}", span.getStr(),
//                                NAFUtilsUD.toString(coref));
//                    }
//                } else {
//                    span.setHead(head);
//                }
//            }
//
//            // Remove spans containing smaller spans + determine if there is span with NNP head
//            boolean hasProperNounHead = false;
//            boolean isEvent = false;
//            final List<Span<Term>> spans = ImmutableList.copyOf(coref.getSpans());
//            outer: for (final Span<Term> span1 : spans) {
//                for (final Span<Term> span2 : spans) {
//                    if (span1.size() > span2.size()
//                            && span1.getTargets().containsAll(span2.getTargets())) {
//                        coref.getSpans().remove(span1);
//                        if (LOGGER.isDebugEnabled()) {
//                            LOGGER.debug("Removed span '{}' including smaller span '{}' from {}",
//                                    span1.getStr(), span2.getStr(), NAFUtilsUD.toString(coref));
//                        }
//                        continue outer;
//                    }
//                }
//                hasProperNounHead |= span1.getHead().getUpos().equalsIgnoreCase("PROPN");
//                if (!isEvent) {
//                    for (final ExternalRef ref : NAFUtilsUD.getRefs(span1.getHead(),
//                            NAFUtilsUD.RESOURCE_SUMO, null)) {
//                        final IRI sumoID = Statements.VALUE_FACTORY.createIRI(SUMO_NAMESPACE + ref.getReference());
//                        if (Sumo.isSubClassOf(sumoID, SUMO_PROCESS)) {
//                            isEvent = true;
//                        }
//                    }
//                }
//            }
//
//            // Shrink spans containing a proper name, if head of another span is proper name
//            if (hasProperNounHead) {
//
//                // Drop spans not corresponding to non-role predicates
//                for (final Span<Term> span : ImmutableList.copyOf(coref.getSpans())) {
//                    final Term head = span.getHead();
//                    if (!head.getUpos().equalsIgnoreCase("PROPN") && !isEvent) {
//                        if (head.getUpos().equalsIgnoreCase("VERB")) {
//                            coref.getSpans().remove(span);
//                            LOGGER.debug("Removed span with VB head '{}' from {}", span.getStr(),
//                                    NAFUtilsUD.toString(coref));
//                        } else {
//                            outer: for (final Predicate predicate : document
//                                    .getPredicatesByTerm(head)) {
//                                for (final ExternalRef ref : NAFUtilsUD.getRefs(predicate,
//                                        NAFUtilsUD.RESOURCE_NOMBANK, null)) {
//                                    final NomBank.Roleset roleset = NomBank.getRoleset(ref
//                                            .getReference());
//                                    if (roleset != null
//                                            && roleset.getPredMandatoryArgNums().isEmpty()
//                                            && roleset.getPredOptionalArgNums().isEmpty()) {
//                                        // Not a role
//                                        coref.getSpans().remove(span);
//                                        LOGGER.debug("Removed span with non-role predicate "
//                                                        + "head '{}' from {}", span.getStr(),
//                                                NAFUtilsUD.toString(coref));
//                                        break outer;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//            } else {
//
//                // Split the coreference set into multiple sets, one for each sentence
//                final Multimap<Integer, Span<Term>> spansBySentence = HashMultimap.create();
//                for (final Span<Term> span : coref.getSpans()) {
//                    final int sentID = span.getTargets().get(0).getSent();
//                    spansBySentence.put(sentID, span);
//                }
//                if (spansBySentence.keySet().size() > 1) {
//                    coref.getSpans().clear();
//                    for (final Collection<Span<Term>> sentSpans : spansBySentence.asMap().values()) {
//                        if (sentSpans.size() > 1) {
//                            document.newCoref(Lists.newArrayList(sentSpans));
//                        }
//                    }
//                }
//
//            }
//
//            // Drop coref in case no span remains.
//            if (coref.getSpans().isEmpty()) {
//                document.removeAnnotation(coref);
//                LOGGER.debug("Removed empty coref set {}", NAFUtilsUD.toString(coref));
//            }
//        }
//    }




