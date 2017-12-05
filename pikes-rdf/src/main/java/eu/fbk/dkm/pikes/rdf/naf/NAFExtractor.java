package eu.fbk.dkm.pikes.rdf.naf;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.*;

import eu.fbk.dkm.pikes.rdf.util.OWLTime;
import eu.fbk.dkm.pikes.rdf.vocab.*;
import eu.fbk.dkm.pikes.resources.YagoTaxonomy;
import eu.fbk.utils.svm.Util;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ixa.kaflib.Coref;
import ixa.kaflib.Dep;
import ixa.kaflib.Entity;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.KAFDocument.FileDesc;
import ixa.kaflib.LinguisticProcessor;
import ixa.kaflib.Predicate;
import ixa.kaflib.Predicate.Role;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import ixa.kaflib.Timex3;
import ixa.kaflib.WF;

import eu.fbk.dkm.pikes.rdf.api.Extractor;
import eu.fbk.dkm.pikes.rdf.util.ModelUtil;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.Statements;

public class NAFExtractor implements Extractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAFExtractor.class);

    public void generate(final Object document, final Model model, @Nullable final Iterable<Integer> sentenceIDs) throws Exception {
        KAFDocument doc = (KAFDocument) document;
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
    }

    @Override
    public void extract(final Object document, final Model model, final boolean[] sentenceIDs) throws Exception {
        KAFDocument doc = (KAFDocument) document;
        IRI IRI = SimpleValueFactory.getInstance().createIRI(doc.getPublic().uri);
        new Extraction(IRI, model,
                doc, sentenceIDs).run();
    }


    //todo adapt for UD (not needed)
    private static final String MODIFIER_REGEX = "(NMOD|AMOD|TMP|LOC|TITLE) PMOD? (COORD CONJ?)* PMOD?";

    //todo adapt for UD
    private static final String PARTICIPATION_REGEX = ""
//            + "SUB? (COORD CONJ?)* (PMOD (COORD CONJ?)*)? ((VC OPRD?)|(IM OPRD?))*";
            + "SUB? ( (COORD CONJ?)* PMOD)? ((VC OPRD?)|(IM OPRD?))*";

    //todo adapt for UD
    private static final String COORDINATION_REGEX = "(COORD CONJ?)*";

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
    private static final String DEFAULT_OLIA_PENN_POS = "http://purl.org/olia/penn.owl#";

    public static final NAFExtractor DEFAULT = NAFExtractor.builder().build();

    private final Multimap<String, IRI> typeMap;

    private final Map<String, String> namespaceMap;

    private final String owltimeNamespace;

    private final boolean merging;

    private final boolean normalization;


    public NAFExtractor(final Builder builder) {
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

//        private final Map<Term, InstanceMention> mentions;

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

//            order in 0-3 doesn't matter'
            processMetadata(); // 0. Process NAF metadata DONE
            processTimexes(); // 1. Process all <timex3> annotations DONE
            processEntities(); // 2. Process all <entity> annotations DONE
            processPredicates(); // 3. Process <predicate> annotations DONE?

//            next one has to come after 0-3
            processCoordinations(); // 4. Process all <entity> annotations which are involved in a coordination

//            next ones have to come after coordination
            processCoreferences(); // 6. Process <coref> annotations
            processRoles(); // 7. Process head <term>s in <role> annotations
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
            for (final Timex3 timex : this.document.getTimeExs()) {

                //filter only the annotations in the requested sentences
                if (timex.getSpan() == null
                        || this.sentenceIDs[timex.getSpan().getFirstTarget().getSent()]) {
                    try {
                        processTimex(timex);
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing " + NAFUtils.toString(timex) + ", type "
                                + timex.getType() + ", value " + timex.getValue(), ex);
                    }
                }
            }
        }

        private void processTimex(final Timex3 timex){

            // Abort if timex has no span (e.g., the DCT)
            if (timex.getSpan() == null) {
                return;
            }

            // Extract terms, head and label
            final List<Term> terms = this.document.getTermsByWFs(timex.getSpan().getTargets());
            final Term head = NAFUtils.extractHead(this.document, KAFDocument.newTermSpan(terms));
            final String label = NAFUtils.getText(NAFUtils.filterTerms(terms));
            final String type = timex.getType().trim().toLowerCase();


            // create mention if not already existing
            Mention mention = getMention(head.getId(),terms);
            final IRI mentionIRI;
            if (mention==null) {
                //emit mentions
                mentionIRI = emitMention(terms);
                mention = new Mention(head,terms,mentionIRI);
                safeMentionPutInMap(head.getId(),mention);
            } else
                //reuse mention IRI
                mentionIRI = mention.mentionIRI;

            this.nafIdMentions.put(timex.getId(),mention);

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
                                NAFExtractor.this.owltimeNamespace, null);
                    } else {
                        LOGGER.debug("Could not represent date/time value '" + timex.getValue()
                                + "' of " + NAFUtils.toString(timex));
                    }

                } else if (type.equals("duration")) {
                    emitTriple(semAnnoIRI, KEMT.TYPE_P, KEMT.TT_DURATION);
                    final OWLTime.Duration duration = OWLTime.Duration
                            .parseTimex(timex.getValue());
                    if (duration != null) {
                        timexIRI = this.vf.createIRI(NAFExtractor.this.owltimeNamespace,
                                duration.toString());
                        final IRI durationIRI = duration.toRDF(RDFHandlers.wrap(this.model),
                                NAFExtractor.this.owltimeNamespace, null);
                        emitTriple(timexIRI, OWLTIME.HAS_DURATION_DESCRIPTION, durationIRI);
                    } else {
                        LOGGER.debug("Could not represent duration value '" + timex.getValue()
                                + "' of " + NAFUtils.toString(timex));
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

        }



        private void processEntities() {
            for (final Entity entity : this.document.getEntities()) {
                for (final Span<Term> span : entity.getSpans()) {
                    //filter only the annotations in the requested sentences
                    if (this.sentenceIDs[span.getFirstTarget().getSent()]) {
                        try {
                            processEntity(entity);
                        } catch (final Throwable ex) {
                            LOGGER.error("Error processing " + NAFUtils.toString(entity)
                                    + ", type " + entity.getType(), ex);
                        }
                        break; // move to next entity
                    }
                }
            }
        }

        private void processEntity(final Entity entity) throws RDFHandlerException {

            // Retrieve terms, head and label
            final List<Term> terms = entity.getSpans().get(0).getTargets();
            final String label = NAFUtils.getText(NAFUtils.filterTerms(terms));
            final Term head = NAFUtils.extractHead(this.document, entity.getSpans().get(0));
            if (head == null) {
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

            // Discard attributes in modifier position
            final Dep dep = this.document.getDepToTerm(head);
            if (isProperty && dep != null) {
                final String depLabel = dep.getRfunc().toUpperCase();
                if (depLabel.contains("NMOD") || depLabel.contains("AMOD")) {
                    return;
                }
            }

            // create mention if not already existing
            Mention mention = getMention(head.getId(),terms);
            final IRI mentionIRI;
            if (mention==null) {
                //emit mentions
                mentionIRI = emitMention(terms);
                mention = new Mention(head,terms,mentionIRI);
                safeMentionPutInMap(head.getId(),mention);
            } else
                //reuse mention IRI
                mentionIRI = mention.mentionIRI;

            this.nafIdMentions.put(entity.getId(),mention);

            //CREATE THE NER ANNOTATION(S)
            //check external ref for other NERC types
            boolean typeAnnotation = false;
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
                        typeAnnotation=true;
                        //emit confidence if available
                        if (ref.hasConfidence()) emitTriple(semAnnoIRI,NIF.CONFIDENCE , ref.getConfidence());
                        if (named) {
                            emitTriple(semAnnoIRI, RDF.TYPE, KEMT.NAMED_ENTITY);
                            emitTriple(semAnnoIRI, KEMT.PROPER_NAME, label);
                        }
                        //attach raw string to annotation
                        emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));
                    }
            }
            //there are no other nerc types in external ref, use the type attribute of the entity
            if ((!hasOtherNercTypes)&&(type!=null)) {
                //emit semantic annotation of type timex and store in the map of annotation per mention
                final IRI semAnnoIRI = createSemanticAnnotationIRI(entity.getId()+type,mentionIRI,KEMT.ENTITY_ANNOTATION);
                Annotation ann = new Annotation(semAnnoIRI,KEMT.ENTITY_ANNOTATION);
                safeAnnotationPutInMap(mention,ann);
                emitTriple(semAnnoIRI, ITSRDF.TA_CLASS_REF, this.vf.createIRI(DEFAULT_NER_NAMESPACE+type));
                typeAnnotation=true;
                if (isProperty) {
                    emitEntityAttributes(entity, semAnnoIRI);
                }
                if (named) {
                    emitTriple(semAnnoIRI, RDF.TYPE, KEMT.NAMED_ENTITY);
                    emitTriple(semAnnoIRI, KEMT.PROPER_NAME, label);
                }
                //attach raw string to annotation
                emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));
            }

            boolean linkingAnnotation = false;
            //CREATE THE LINKING ANNOTATION(S)
            for (final ExternalRef ref : entity.getExternalRefs()) {
                final String resource = ref.getResource();
                if (resource.startsWith("dbpedia-")) {
                    final IRI refIRI = this.vf.createIRI(Util.cleanIRI(ref.getReference()));
                    final IRI semAnnoIRI = createSemanticAnnotationIRI(entity.getId()+"_"+refIRI.getLocalName(),mentionIRI,KEMT.ENTITY_ANNOTATION);
                    Annotation ann = new Annotation(semAnnoIRI,KEMT.ENTITY_ANNOTATION);
                    safeAnnotationPutInMap(mention,ann);
                    //emit linking
                    emitTriple(semAnnoIRI, ITSRDF.TA_IDENT_REF, refIRI);
                    linkingAnnotation = true;
                    //emit confidence if available
                    if (ref.hasConfidence()) emitTriple(semAnnoIRI,NIF.CONFIDENCE , ref.getConfidence());
                    //attach raw string to annotation
                    emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));
                }
            }


            //forceSemanticAnnotationCreation
            //CREATE TERM ANNOTATIONS (WSD, SST)
            emitCommonAttributesAnnotation(entity.getId()+"_semann",mention,head,terms, (!linkingAnnotation)&&(!typeAnnotation));

        }



        private void processPredicates(){
            for (final Predicate predicate : this.document.getPredicates()) {
                //filter only the annotations in the requested sentences
                if (this.sentenceIDs[predicate.getSpan().getFirstTarget().getSent()]) {
                    try {
                        processPredicate(predicate);
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing " + NAFUtils.toString(predicate), ex);
                    }
                }
            }
        }



        private void processPredicate(final Predicate predicate) throws RDFHandlerException {

            // Retrieve terms, head and label
            final List<Term> terms = predicate.getSpan().getTargets();
            final String label = NAFUtils.getText(NAFUtils.filterTerms(terms));
            final Term head = NAFUtils.extractHead(this.document, predicate.getSpan());

            // Determine the lemma, handling multiwords
            final StringBuilder builder = new StringBuilder();
            for (final Term term : terms) {
                builder.append(builder.length() == 0 ? "" : "_");
                builder.append(term.getLemma().toLowerCase());
            }
            final String lemma = builder.toString();
//            todo next should become for UD --> final String POS = head.getUpos();
            final String POS = head.getPos();

            // create mention if not already existing
            Mention mention = getMention(head.getId(),terms);
            final IRI mentionIRI;
            if (mention==null) {
                //emit mentions
                mentionIRI = emitMention(terms);
                mention = new Mention(head,terms,mentionIRI);
                safeMentionPutInMap(head.getId(),mention);
            } else
                //reuse mention IRI
                mentionIRI = mention.mentionIRI;

            this.nafIdMentions.put(predicate.getId(),mention);

            //add lemma and pos for framebase mappings
            emitTriple(mentionIRI,NIF.LEMMA,lemma);
            //            todo next should become for UD
//          emitTriple(mentionIRI,NIF.OLIA_LINK,this.vf.createIRI(DEFAULT_OLIA_UD_POS+POS));
            emitTriple(mentionIRI,NIF.OLIA_LINK,this.vf.createIRI(DEFAULT_OLIA_PENN_POS+POS));

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
                Annotation ann = new Annotation(semAnnoIRI,KEMT.PREDICATE_C);
                safeAnnotationPutInMap(mention,ann);

                emitTriple(semAnnoIRI,ITSRDF.TA_CLASS_REF,typeIRI);

                //attach raw string to annotation
                emitTriple(semAnnoIRI, KEMT.RAW_STRING, emitFragment(terms));

            }

            //CREATE TERM ANNOTATIONS (WSD, SST)
            emitCommonAttributesAnnotation(predicate.getId()+"_semann",mention,head,terms,false);
        }



        private void  processCoordinations (){

//            hashmap (sentenceID, set of entity annotation in that sentence)
            Map<Integer, Set<Mention>> sentenceMentions = Maps.newHashMap();
//            hashmap (entityA, set of entity depending from entity A via cordination)
            Map<Mention, Set<Mention>> coordinatedMentions = Maps.newHashMap();


            // iterate over all entities, and populate an
            for (String headID: this.mentions.keySet()
                    ) {

                final Mention mention = getBestMention(headID);
                final Term head = mention.head;
                final Integer sentenceID = head.getSent();

//                    store the mention in its sentence bucket
                Set<Mention> mentions;
                if (sentenceMentions.containsKey(sentenceID))
                    mentions = sentenceMentions.get(sentenceID);
                else
                    mentions = Sets.newHashSet();
                mentions.add(mention);
                sentenceMentions.put(mention.head.getSent(), mentions);

//                    dependency pattern for coordination
                Set<Term> coordinatedTerms = this.document.getTermsByDepAncestors(
                        Collections.singleton(head), NAFExtractor.COORDINATION_REGEX);

//                    there have to be at least two coordinated term, otherwise nothing to do
                if (coordinatedTerms.size()>1) {
                    for (final Term term : coordinatedTerms) {

//                        term is an entry in the dependency that is linked to the head of the mention via (COORD CONJ?)*
                        final Mention depMen = getBestMention(term.getId());
                        if (depMen != null) {

//                                store the dependent annotation in the head annotation bucket
                            Set<Mention> depMentions;
                            if (coordinatedMentions.containsKey(mention))
                                depMentions = coordinatedMentions.get(mention);
                            else
                                depMentions = Sets.newHashSet();
                            depMentions.add(depMen);
                            coordinatedMentions.put(mention, depMentions);

                        }
                    }

                }

            }

//            Now cycle over sentences and keep maximal coordinatedEntities
            for (Integer sentenceID:sentenceMentions.keySet()
                    ) {

//                retrieve the mentions in that sentence
                Set<Mention> sentMen = sentenceMentions.get(sentenceID);
                Set<Mention> mentionsToKeep = Sets.newHashSet();

                for (Mention A:sentMen) {
//                    check if it has coordinated terms
                    if (!coordinatedMentions.containsKey(A)) continue;
                    if (coordinatedMentions.get(A).size()==1) continue;
                    boolean keep = true;
                    for (Mention B : sentMen) {
                        if (A.equals(B)) continue;
//                        check if it has coordinated terms
                        if (!coordinatedMentions.containsKey(B)) continue;
                        if (coordinatedMentions.get(B).contains(A)) {
//                            A is a coordinated term of B, drop A
                            keep=false;
                            break;
                        }
                    }
                    if (keep) mentionsToKeep.add(A);
                }

//                mentionsToKeep contains all the head of "independent" coordination dependency paths to keep
                Integer counter = 0;
                for (Mention men:mentionsToKeep
                        ) {

                    counter++;
//                    collect extents and URI of coordinated mentions
                    List<Term> terms = Lists.newArrayList();
                    List<IRI> mentionsIRI = Lists.newArrayList();
                    List<IRI> coordinatedIRI = Lists.newArrayList();

                    for (Mention depMen: coordinatedMentions.get(men)
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


                    }

                    //emit group entity mention (it can't already exists)
                    final IRI groupEntityMentionIRI = emitMention(terms);
                    final Mention groupEntityMention = new Mention(men.head,terms,groupEntityMentionIRI);
                    safeMentionPutInMap(men.head.getId(),groupEntityMention);

                    //emit group entity annotation
                    final IRI groupEntityIRI = createSemanticAnnotationIRI("group",groupEntityMentionIRI,KEMT.ENTITY_ANNOTATION);
                    final Annotation groupEntityAnn = new Annotation(groupEntityIRI,KEMT.ENTITY_ANNOTATION);
                    safeAnnotationPutInMap(groupEntityMention,groupEntityAnn);

                    //attach raw string to annotation (here is the same as the mention)
                    emitTriple(groupEntityIRI, KEMT.RAW_STRING, groupEntityMentionIRI);

                    //emit coordination mention (for the time being, we reuse the group entity one)
                    final IRI coordinationMentionIRI = groupEntityMentionIRI;
                    final Mention coordinationMention = new Mention(men.head,terms,coordinationMentionIRI);
                    safeMentionPutInMap(men.head.getId(),coordinationMention);

                    //emit semantic annotation of type coordination
                    final IRI coordinationIRI = createSemanticAnnotationIRI("coord",coordinationMentionIRI,KEMT.COORDINATION);
                    final Annotation coordinationAnn = new Annotation(groupEntityIRI,KEMT.COORDINATION);
                    safeAnnotationPutInMap(coordinationMention,coordinationAnn);

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
                        LOGGER.error("Error processing " + NAFUtils.toString(coref), ex);
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
                final Term head = NAFUtils.extractHead(this.document, span);
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
                    final Term roleHead = NAFUtils.extractHead(this.document, role.getSpan());
                    if (roleHead != null) {


                        final Set<Term> argHeads = this.document.getTermsByDepAncestors(
                                Collections.singleton(roleHead), PARTICIPATION_REGEX);

                        for (final Term argHead : argHeads) {
                            try {
                                processRole(predicate, role, argHead);
                            } catch (final Throwable ex) {
                                LOGGER.error("Error processing " + NAFUtils.toString(role)
                                        + " of " + NAFUtils.toString(predicate)
                                        + ", argument " + NAFUtils.toString(argHead), ex);
                            }
                        }
                    }
                }
            }
        }


        private void processRole(final Predicate predicate, final Role role, final Term argHead) {


            //get predicate mention
            final Mention predMention = this.nafIdMentions.get(predicate.getId());

            //get the role mention
            Mention correspondingMention = getBestMention(argHead.getId());
            if (correspondingMention==null) return;

            //emit fake predicate and role for participation relation
            final IRI fakePredIRI = createSemanticAnnotationIRI(predicate.getId(),predMention.mentionIRI,KEMT.PREDICATE_C);
            final IRI fakeRoleIRI = createSemanticAnnotationIRI(role.getId()+"_"+argHead.getId(),correspondingMention.mentionIRI,KEMT.ARGUMENT_C);

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
            final IRI participationIRI = createSemanticAnnotationIRI(predicate.getId()+"_"+role.getId()+"_"+argHead.getId(),partMentionIRI,KEMT.PARTICIPATION);

            emitTriple(participationIRI,KEMT.PREDICATE_P,fakePredIRI);
            emitTriple(participationIRI,KEMT.ARGUMENT_P,fakeRoleIRI);
            emitTriple(participationIRI,KEMT.RAW_STRING,partRawIRI);

            for (final ExternalRef ref : role.getExternalRefs()) {
                if ("".equals(ref.getReference())) {
                    continue;
                }
                //emit role annotation
                final IRI typeIRI = mintRefIRI(ref.getResource(), ref.getReference());
                final IRI roleIRI = createSemanticAnnotationIRI(role.getId()+"_"+argHead.getId()+"_"+typeIRI.getLocalName(),correspondingMention.mentionIRI,KEMT.ARGUMENT_C);
                Annotation ann = new Annotation(roleIRI,KEMT.ARGUMENT_C);
                safeAnnotationPutInMap(correspondingMention,ann);
                emitTriple(roleIRI,ITSRDF.TA_PROP_REF,typeIRI);
                emitTriple(roleIRI,KEMT.RAW_STRING,fakeRoleRawString);
            }
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
            final int begin = NAFUtils.getBegin(sortedTerms.get(0));
            int offset = begin;
            int startTermIdx = 0;

            final StringBuilder anchorBuilder = new StringBuilder();
            final StringBuilder uriBuilder = new StringBuilder(this.documentIRI.stringValue())
                    .append("#char=").append(begin).append(",");

            for (int i = 0; i < numTerms; ++i) {
                final Term term = sortedTerms.get(i);
                final int termOffset = NAFUtils.getBegin(term);
                if (termOffset > offset && !text.substring(offset, termOffset).trim().isEmpty()) {
                    final int start = NAFUtils.getBegin(sortedTerms.get(startTermIdx));
                    anchorBuilder.append(text.substring(start, offset)).append(" [...] ");
                    uriBuilder.append(offset).append(";").append(termOffset).append(',');
                    componentIRIs.add(emitFragment(sortedTerms.subList(startTermIdx, i)));
                    startTermIdx = i;
                }
                offset = NAFUtils.getEnd(term);
            }
            if (startTermIdx > 0) {
                componentIRIs.add(emitFragment(sortedTerms.subList(startTermIdx, numTerms)));
            }


            anchorBuilder.append(text.substring(NAFUtils.getBegin(sortedTerms.get(startTermIdx)),
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
                        RDF.TYPE.equals(property) ? NAFExtractor.this.typeMap : null)) {
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
                final String namespace = NAFExtractor.this.namespaceMap.get(normResource);
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
            final ExternalRef valueRef = NAFUtils.getRef(entity, "value", null);
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



        private void emitCommonAttributesAnnotation(final String id, final Mention mention, final Term head, final List<Term> terms, final boolean forceSemanticAnnotationCreation)
                throws RDFHandlerException {

            //create semann only if

            final ExternalRef sstRef = NAFUtils.getRef(head, NAFUtils.RESOURCE_WN_SST, null);
            final ExternalRef synsetRef = NAFUtils.getRef(head, NAFUtils.RESOURCE_WN_SYNSET, null);
            final ExternalRef bbnRef = NAFUtils.getRef(head, NAFUtils.RESOURCE_BBN, null);

            if ((forceSemanticAnnotationCreation)||(sstRef != null)||(synsetRef != null)||(bbnRef != null)) {

                final IRI semanticAnnotationIRI = createSemanticAnnotationIRI(id, mention.mentionIRI, KEMT.ENTITY_ANNOTATION);
                Annotation ann = new Annotation(semanticAnnotationIRI, KEM.SEMANTIC_ANNOTATION);
                safeAnnotationPutInMap(mention, ann);

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

                //attach raw string to timex annotation
                emitTriple(semanticAnnotationIRI, KEMT.RAW_STRING, emitFragment(terms));
            }
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

        public NAFExtractor build() {
            return new NAFExtractor(this);
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