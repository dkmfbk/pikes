package eu.fbk.dkm.pikes.rdf.naf;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import eu.fbk.dkm.pikes.rdf.api.Extractor;
import eu.fbk.dkm.pikes.rdf.util.ModelUtil;
import eu.fbk.dkm.pikes.rdf.util.OWLTime.Duration;
import eu.fbk.dkm.pikes.rdf.util.OWLTime.Interval;
import eu.fbk.dkm.pikes.rdf.vocab.BBN;
import eu.fbk.dkm.pikes.rdf.vocab.KS;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.utils.svm.Util;
import eu.fbk.utils.vocab.NIF;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.util.Hash;
import eu.fbk.rdfpro.util.QuadModel;
import ixa.kaflib.*;
import ixa.kaflib.KAFDocument.FileDesc;
import ixa.kaflib.Predicate.Role;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NAFExtractor implements Extractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAFExtractor.class);

    @Override
    public void extract(final Object document, final QuadModel model) {
        final KAFDocument naf = (KAFDocument) document;
        new Extraction(model, naf).run();
    }

    private static boolean isAttributeTerm(final Term term) {
        final String pos = term.getMorphofeat();
        return pos.startsWith("JJ") || pos.startsWith("RB") || pos.startsWith("VB");
    }

    private final class Extraction {

        private final QuadModel model;

        private final KAFDocument document;

        private final ValueFactory vf;

        private final String documentText;

        private final URI documentURI;

        private final Map<Term, InstanceMention> mentions;

        @SuppressWarnings("deprecation")
        Extraction(final QuadModel model, final KAFDocument document) {

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
            this.vf = model.getValueFactory();
            this.documentText = builder.toString();
            this.documentURI = this.vf.createURI(Util.cleanIRI(document.getPublic().uri));
            this.mentions = Maps.newHashMap();
        }

        void run() {

            // 1. Process NAF metadata
            processMetadata();

            // 2. Process <timex3> annotations
            for (final Timex3 timex : this.document.getTimeExs()) {
                try {
                    processTimex(timex);
                } catch (final Throwable ex) {
                    LOGGER.error("Error processing " + NAFUtils.toString(timex) + ", type "
                            + timex.getType() + ", value " + timex.getValue(), ex);
                }
            }

            // 3. Process <entity> annotations
            for (final Entity entity : this.document.getEntities()) {
                try {
                    processEntity(entity);
                } catch (final Throwable ex) {
                    LOGGER.error("Error processing " + NAFUtils.toString(entity) + ", type "
                            + entity.getType(), ex);
                }
            }

            // 4. Process <predicate> annotations; must be done after 1, 2
            for (final Predicate predicate : this.document.getPredicates()) {
                try {
                    processPredicate(predicate);
                } catch (final Throwable ex) {
                    LOGGER.error("Error processing " + NAFUtils.toString(predicate), ex);
                }
            }

            // 5. Process attributes
            for (final Term term : this.document.getTerms()) {
                if (isAttributeTerm(term)) {
                    final Dep dep = this.document.getDepToTerm(term);
                    if (dep == null || !isAttributeTerm(dep.getFrom())) {
                        processAttribute(term);
                    }
                }
            }

            // 6. Process <coref> annotations; must be done after 1, 2, 3
            for (final Coref coref : this.document.getCorefs()) {
                if (!"event".equalsIgnoreCase(coref.getType())) {
                    try {
                        processCoref(coref);
                    } catch (final Throwable ex) {
                        LOGGER.error("Error processing " + NAFUtils.toString(coref), ex);
                    }
                }
            }

            // 7. Process head <term>s in <role> annotations; must be done after 1, 2, 3
            for (final Predicate predicate : this.document.getPredicates()) {
                for (final Role role : predicate.getRoles()) {
                    final Term roleHead = NAFUtils.extractHead(this.document, role.getSpan());
                    if (roleHead != null) {
                        for (final Term argHead : this.document.getTermsByDepAncestors(
                                Collections.singleton(roleHead), "SUB? (COORD CONJ?)*"
                                        + " (PMOD (COORD CONJ?)*)? ((VC OPRD?)|(IM OPRD?))*")) {
                            try {
                                processRole(predicate, role, argHead);
                            } catch (final Throwable ex) {
                                LOGGER.error("Error processing " + NAFUtils.toString(role)
                                        + " of " + NAFUtils.toString(predicate) + ", argument "
                                        + NAFUtils.toString(argHead), ex);
                            }
                        }
                    }
                }
            }
        }

        private void processMetadata() {

            // Obtain URIs of document and NAF resources
            final URI docURI = this.documentURI;
            final URI nafURI = this.vf.createURI(docURI.stringValue() + ".naf");

            // Emit document types
            this.model.add(docURI, RDF.TYPE, KS.RESOURCE);
            this.model.add(docURI, RDF.TYPE, KS.TEXT);

            // Emit title, author and DCT from the <fileDesc> element, if present
            if (this.document.getFileDesc() != null) {
                final FileDesc fd = this.document.getFileDesc();
                this.model.add(docURI, DCTERMS.TITLE, this.vf.createLiteral(fd.title));
                this.model.add(docURI, DCTERMS.CREATOR, this.vf.createLiteral(fd.author));
                this.model.add(docURI, DCTERMS.CREATED, this.vf.createLiteral(fd.creationtime));
            }

            // Emit the document language, if available
            if (this.document.getLang() != null) {
                this.model.add(docURI, DCTERMS.LANGUAGE,
                        ModelUtil.languageCodeToURI(this.document.getLang()));
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
                this.model.add(docURI, KS.TEXT_HASH,
                        this.vf.createLiteral(Hash.murmur3(builder.toString()).toString()));
            }

            // Link document to its NAF annotation
            this.model.add(docURI, KS.ANNOTATED_WITH, nafURI);
            this.model.add(nafURI, KS.ANNOTATION_OF, docURI);

            // Emit types, version and publicId of NAF resource
            this.model.add(nafURI, RDF.TYPE, KS.RESOURCE);
            this.model.add(nafURI, RDF.TYPE, KS.NAF);
            this.model.add(nafURI, KS.VERSION, this.vf.createLiteral(this.document.getVersion()));
            this.model.add(nafURI, DCTERMS.IDENTIFIER,
                    this.vf.createLiteral(this.document.getPublic().publicId));

            // Emit information about linguistic processors: dct:created, dct:creatro, ego:layer
            String timestamp = null;
            for (final Map.Entry<String, List<LinguisticProcessor>> entry : this.document
                    .getLinguisticProcessors().entrySet()) {
                this.model.add(nafURI, KS.LAYER,
                        this.vf.createURI(KS.NAMESPACE, "layer_" + entry.getKey()));
                for (final LinguisticProcessor lp : entry.getValue()) {
                    if (timestamp == null) {
                        if (!Strings.isNullOrEmpty(lp.getBeginTimestamp())) {
                            timestamp = lp.getBeginTimestamp();
                        } else if (!Strings.isNullOrEmpty(lp.getEndTimestamp())) {
                            timestamp = lp.getEndTimestamp();
                        }
                    }
                    final URI lpURI = this.vf.createURI(ModelUtil.cleanIRI(KS.NAMESPACE
                            + lp.getName() + '.' + lp.getVersion()));
                    this.model.add(nafURI, DCTERMS.CREATOR, lpURI);
                    this.model.add(lpURI, DCTERMS.TITLE, this.vf.createLiteral(lp.getName()));
                    this.model.add(lpURI, KS.VERSION, this.vf.createLiteral(lp.getVersion()));
                }
            }
            this.model.add(nafURI, DCTERMS.CREATED, this.vf.createLiteral(timestamp));
        }

        private void processTimex(final Timex3 timex) throws RDFHandlerException {

            // Abort if timex has no span (e.g., the DCT)
            if (timex.getSpan() == null) {
                return;
            }

            // Extract terms, head and label
            final List<Term> terms = this.document.getTermsByWFs(timex.getSpan().getTargets());
            final Term head = NAFUtils.extractHead(this.document, KAFDocument.newTermSpan(terms));
            if (head == null) {
                return;
            }

            // Emit a mention and its triples for the current timex. Abort in case of conflicts
            final URI mentionURI = emitInstanceMention(terms, head, InstanceMention.TIME,
                    InstanceMention.ALL);
            if (mentionURI == null) {
                return;
            }

            // Emit type specific statements
            if (timex.getValue() != null) {
                URI owltimeURI = null;
                final String type = timex.getType().trim().toLowerCase();
                if (type.equals("date") || type.equals("time")) {
                    final Interval interval = Interval.parseTimex(timex.getValue());
                    if (interval != null) {
                        owltimeURI = interval
                                .toRDF(RDFHandlers.wrap(this.model), "owltime:", null);
                    }
                } else if (type.equals("duration")) {
                    final Duration duration = Duration.parseTimex(timex.getValue());
                    if (duration != null) {
                        owltimeURI = duration
                                .toRDF(RDFHandlers.wrap(this.model), "owltime:", null);
                    }
                }
                if (owltimeURI == null) {
                    LOGGER.warn("Could not represent TIMEX value '" + timex.getValue() + "' of "
                            + NAFUtils.toString(timex));
                } else {
                    this.model.add(mentionURI, KS.NORMALIZED_VALUE, owltimeURI);
                }
            }
        }

        private void processEntity(final Entity entity) throws RDFHandlerException {

            // Retrieve terms, head and label
            final List<Term> terms = entity.getSpans().get(0).getTargets();
            final Term head = NAFUtils.extractHead(this.document, entity.getSpans().get(0));
            if (head == null) {
                return;
            }

            // Determine type and exclude masks; abort if cannot be determined
            int typeMask;
            int excludeMask;
            if (entity.isNamed()) {
                typeMask = InstanceMention.NAME;
                excludeMask = InstanceMention.ALL;
            } else {
                final String pos = head.getMorphofeat();
                if (pos.startsWith("NN")) {
                    typeMask = InstanceMention.NOUN;
                    excludeMask = InstanceMention.ALL & ~InstanceMention.PREDICATE;
                } else if (pos.startsWith("PRP") || pos.startsWith("WP")) {
                    typeMask = InstanceMention.PRONOUN;
                    excludeMask = InstanceMention.ALL;
                } else {
                    return;
                }
            }

            // Emit a mention and its triples for the current entity
            final URI mentionURI = emitInstanceMention(terms, head, typeMask, excludeMask);

            // Extract type information (type URI, whether timex or attribute) based on NER tag
            final URI nercType = BBN.resolve(entity.getType());
            if (nercType != null) {
                this.model.add(mentionURI, KS.NERC_TYPE, nercType);
            }

            // Extract links to external references (e.g. DBpedia)
            for (final ExternalRef ref : entity.getExternalRefs()) {
                try {
                    final URI refURI = this.vf.createURI(Util.cleanIRI(ref.getReference()));
                    this.model.add(mentionURI, KS.LINKED_TO, refURI);
                } catch (final Throwable ex) {
                    // ignore: not a URI
                }
            }
        }

        private void processPredicate(final Predicate predicate) throws RDFHandlerException {

            // Retrieve terms, head and label
            final List<Term> terms = predicate.getSpan().getTargets();
            final Term head = NAFUtils.extractHead(this.document, predicate.getSpan());
            if (head == null) {
                return;
            }

            // Extract the roleset URI
            URI rolesetURI = null;
            for (final ExternalRef ref : predicate.getExternalRefs()) {
                if (ref.getSource() != null && !ref.getReference().isEmpty()) {
                    if (ref.getResource().equalsIgnoreCase("nombank")) {
                        rolesetURI = this.vf.createURI(
                                "http://www.newsreader-project.eu/ontologies/nombank/",
                                ref.getReference());
                    } else if (ref.getResource().equals("propbank")) {
                        rolesetURI = this.vf.createURI(
                                "http://www.newsreader-project.eu/ontologies/propbank/",
                                ref.getReference());
                    }
                }
            }
            if (rolesetURI == null) {
                return;
            }

            // Emit a mention and its triples
            final URI mentionURI = emitInstanceMention(terms, head, InstanceMention.PREDICATE,
                    InstanceMention.ALL & ~InstanceMention.NOUN);

            // Emit roleset
            this.model.add(mentionURI, KS.ROLESET, rolesetURI);
        }

        private void processAttribute(final Term head) {

            // Extract URI and readable ID of head WN synset. Abort if not possible
            final ExternalRef synsetRef = NAFUtils.getRef(head, NAFUtils.RESOURCE_WN_SYNSET, null);
            if (synsetRef == null) {
                return;
            }
            final String synsetID = WordNet.getReadableSynsetID(synsetRef.getReference());
            final URI synsetURI = this.vf.createURI("http://wordnet-rdf.princeton.edu/wn30/",
                    synsetRef.getReference());

            // Extract URI and readable ID of modifiers synsets, identifying also the extent
            final List<Term> extent = Lists.newArrayList(head);
            final List<String> modIDs = Lists.newArrayList();
            final List<URI> modURIs = Lists.newArrayList();
            for (final Dep dep : this.document.getDepsFromTerm(head)) {
                if (dep.getRfunc().equals("AMOD") || dep.getRfunc().equals("NMOD")) {
                    final Term modifier = dep.getTo();
                    final ExternalRef modifierRef = NAFUtils.getRef(modifier,
                            NAFUtils.RESOURCE_WN_SYNSET, null);
                    if (modifierRef == null || !isAttributeTerm(modifier)) {
                        continue;
                    }
                    extent.add(modifier);
                    modIDs.add(WordNet.getReadableSynsetID(modifierRef.getReference()));
                    modURIs.add(this.vf.createURI("http://wordnet-rdf.princeton.edu/wn30/",
                            synsetRef.getReference()));
                }
            }

            // Emit a mention and its triples (abort if conflicting with other mention)
            final URI mentionURI = emitInstanceMention(extent, head, InstanceMention.ATTRIBUTE,
                    InstanceMention.ALL);
            if (mentionURI == null) {
                return;
            }

            // Emit synset information
            this.model.add(mentionURI, KS.SYNSET, synsetURI);
            for (final URI modURI : modURIs) {
                this.model.add(mentionURI, KS.MODIFIER_SYNSET, modURI);
            }

            // Emit normalized value
            Collections.sort(modIDs);
            final String id = String.join("_", modIDs) + "_" + synsetID;
            final URI valueURI = this.vf.createURI("attr:", id);
            this.model.add(mentionURI, KS.NORMALIZED_VALUE, valueURI);
        }

        private void processCoref(final Coref coref) {

            // Extract coreferential / coreferential conjunct mentions
            final List<Term> extent = Lists.newArrayList();
            final List<URI> coreferentials = Lists.newArrayList();
            final List<URI> coreferentialConjuncts = Lists.newArrayList();
            for (final Span<Term> span : coref.getSpans()) {
                final Term head = NAFUtils.extractHead(this.document, span);
                if (head != null) {
                    final List<URI> uris = Lists.newArrayList();
                    for (final Term term : this.document.getTermsByDepAncestors(
                            Collections.singleton(head), "(COORD CONJ?)*")) {
                        if (span.getTargets().contains(term)) {
                            final InstanceMention mention = this.mentions.get(term);
                            if (mention.head == term) {
                                uris.add(mention.uri);
                                extent.addAll(mention.extent);
                            }
                        }
                    }
                    if (uris.size() == 1) {
                        coreferentials.addAll(uris);
                    } else if (uris.size() > 1 && coreferentialConjuncts.isEmpty()) {
                        coreferentialConjuncts.addAll(uris);
                    }
                }
            }

            // Abort in case there is only one member in the coref cluster
            if (coreferentials.size() + (coreferentialConjuncts.isEmpty() ? 0 : 1) < 2) {
                return;
            }

            // Emit the mention
            final URI mentionURI = emitRelationMention(extent, KS.COREFERENCE_MENTION);

            // Emit links to coreferential / coreferential conjunct mentions
            for (final URI coreferential : coreferentials) {
                this.model.add(mentionURI, KS.COREFERENTIAL, coreferential);
            }
            for (final URI coreferentialConjunct : coreferentialConjuncts) {
                this.model.add(mentionURI, KS.COREFERENTIAL, coreferentialConjunct);
            }
        }

        private void processRole(final Predicate predicate, final Role role, final Term argHead) {

            // Lookup the instance mention corresponding to the predicate. Abort if missing
            final Term predHead = NAFUtils.extractHead(this.document, predicate.getSpan());
            final InstanceMention predMention = this.mentions.get(predHead);
            if (predMention == null || predMention.head != predHead) {
                return;
            }

            // Lookup the instance mention corresponding to the argument. Abort if missing
            final InstanceMention argMention = this.mentions.get(argHead);
            if (argMention == null || argMention.head != argHead) {
                return;
            }

            // Extract the role name (0-9 for A0-A9, MNR... for AM-MNR etc). Abort if undefined
            String semRole = role.getSemRole();
            if (semRole == null) {
                return;
            }
            semRole = semRole.toLowerCase();
            final int index = semRole.lastIndexOf('-');
            if (index >= 0) {
                semRole = semRole.substring(index + 1);
            }
            if (Character.isDigit(semRole.charAt(semRole.length() - 1))) {
                semRole = semRole.substring(semRole.length() - 1);
            }

            // Extract the role URI, by combining name with predicate sense. Abort if undefined
            URI roleURI = null;
            for (final ExternalRef ref : predicate.getExternalRefs()) {
                if (ref.getSource() != null && !ref.getReference().isEmpty()) {
                    if (ref.getResource().equalsIgnoreCase("nombank")) {
                        roleURI = this.vf.createURI(
                                "http://www.newsreader-project.eu/ontologies/nombank/",
                                ref.getReference() + "_" + semRole);
                    } else if (ref.getResource().equals("propbank")) {
                        roleURI = this.vf.createURI(
                                "http://www.newsreader-project.eu/ontologies/propbank/",
                                ref.getReference() + "_" + semRole);
                    }
                }
            }
            if (roleURI == null) {
                return;
            }

            // Emit a participation mention
            final URI mentionURI = emitRelationMention(
                    Iterables.concat(predMention.extent, argMention.extent),
                    KS.PARTICIPATION_MENTION);

            // Emit the links to predicate and argument, as well as the role triple
            this.model.add(mentionURI, KS.ROLE, roleURI);
            this.model.add(mentionURI, KS.FRAME, predMention.uri);
            this.model.add(mentionURI, KS.ARGUMENT, argMention.uri);
        }

        @Nullable
        private URI emitInstanceMention(final Iterable<Term> extent, final Term head,
                final int typeMask, final int excludeMask) {

            // Do not emit a mention if (i) it overlaps with another mention with different head;
            // or (ii) it overlaps with a mention with same head that is not compatible with its
            // types (based on comparison of types and excludes bit masks)
            final List<Term> terms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(extent);
            for (final Term term : terms) {
                final InstanceMention mention = this.mentions.get(term);
                if (mention != null
                        && (mention.head != head || (mention.excludeMask & typeMask) != 0)) {
                    return null;
                }
            }

            // Either reuse (and update) a previous mention for the same head or create a new one
            InstanceMention mention = this.mentions.get(head);
            if (mention != null) {

                // Enlarge mention extent, if necessary, updating also the 'mentions' index
                boolean modified = false;
                for (final Term term : terms) {
                    if (!mention.extent.contains(term)) {
                        mention.extent.add(term);
                        this.mentions.put(term, mention);
                        modified = true;
                    }
                }
                if (modified) {
                    Collections.sort(mention.extent, Term.OFFSET_COMPARATOR);
                }

            } else {

                // Generate mention URI and NIF triples and create corresponding Mention object
                final URI mentionURI = emitNIF(terms);
                mention = new InstanceMention(mentionURI, head, terms);

                // Link the mention to the document
                this.model.add(mention.uri, KS.MENTION_OF, this.documentURI);

                // Emit mention properties based on the mention head
                final char pos = Character.toUpperCase(head.getPos().charAt(0));
                if (pos == 'N' || pos == 'V') {
                    this.model.add(mention.uri, KS.LEMMA, this.vf.createLiteral(head.getLemma()));
                }
                final ExternalRef sstRef = NAFUtils.getRef(head, NAFUtils.RESOURCE_WN_SST, null);
                if (sstRef != null) {
                    final String sst = sstRef.getReference();
                    final URI uri = this.vf.createURI("http://www.newsreader-project.eu/sst/",
                            sst.substring(sst.lastIndexOf('-') + 1));
                    this.model.add(mention.uri, KS.SST, uri);
                }
                final ExternalRef synsetRef = NAFUtils.getRef(head, NAFUtils.RESOURCE_WN_SYNSET,
                        null);
                if (synsetRef != null) {
                    final URI uri = this.vf.createURI("http://wordnet-rdf.princeton.edu/wn30/",
                            synsetRef.getReference());
                    this.model.add(mention.uri, KS.SYNSET, uri);
                }
                final String p = head.getMorphofeat().toUpperCase();
                if (p.equals("NNS") || p.equals("NNPS")) {
                    this.model.add(mention.uri, KS.PLURAL, this.vf.createLiteral(true));
                }
            }

            // Add mention type
            this.model.add(mention.uri, RDF.TYPE, KS.INSTANCE_MENTION);
            if ((typeMask & InstanceMention.TIME) != 0) {
                this.model.add(mention.uri, RDF.TYPE, KS.TIME_MENTION);
            }
            if ((typeMask & InstanceMention.NAME) != 0) {
                this.model.add(mention.uri, RDF.TYPE, KS.NAME_MENTION);
            }
            if ((typeMask & InstanceMention.PREDICATE) != 0) {
                this.model.add(mention.uri, RDF.TYPE, KS.FRAME_MENTION);
            }
            if ((typeMask & InstanceMention.ATTRIBUTE) != 0) {
                this.model.add(mention.uri, RDF.TYPE, KS.ATTRIBUTE_MENTION);
            }

            // Update exclude mask
            mention.excludeMask |= excludeMask;

            // Return the URI of the mention
            return mention.uri;
        }

        private URI emitRelationMention(final Iterable<Term> extent, final URI type) {
            final List<Term> terms = Ordering.from(Term.OFFSET_COMPARATOR).sortedCopy(extent);
            final URI mentionURI = emitNIF(terms);
            this.model.add(mentionURI, KS.MENTION_OF, this.documentURI);
            this.model.add(mentionURI, RDF.TYPE, type);
            return mentionURI;
        }

        @Nullable
        private URI emitNIF(final List<Term> extent) {

            Preconditions.checkArgument(!extent.isEmpty());

            final String text = this.documentText;
            final List<URI> componentURIs = Lists.newArrayList();
            final int begin = extent.get(0).getOffset();
            int offset = begin;
            int startTermIdx = 0;

            final StringBuilder anchorBuilder = new StringBuilder();
            final StringBuilder uriBuilder = new StringBuilder(this.documentURI.stringValue())
                    .append("#char=").append(begin).append(",");

            for (int i = 0; i < extent.size(); ++i) {
                final Term term = extent.get(i);
                final int termOffset = term.getOffset();
                if (termOffset > offset && !text.substring(offset, termOffset).trim().isEmpty()) {
                    final int start = extent.get(startTermIdx).getOffset();
                    anchorBuilder.append(text.substring(start, offset)).append(" [...] ");
                    uriBuilder.append(offset).append(";").append(termOffset).append(',');
                    componentURIs.add(emitNIF(extent.subList(startTermIdx, i)));
                    startTermIdx = i;
                }
                offset = NAFUtils.getEnd(term);
            }
            if (startTermIdx > 0) {
                componentURIs.add(emitNIF(extent.subList(startTermIdx, extent.size())));
            }
            anchorBuilder.append(text.substring(extent.get(startTermIdx).getOffset(), offset));
            uriBuilder.append(offset);

            final String anchor = anchorBuilder.toString();
            final URI uri = this.vf.createURI(uriBuilder.toString());

            if (!componentURIs.isEmpty()) {
                this.model.add(uri, RDF.TYPE, KS.COMPOUND_STRING);
                for (final URI componentURI : componentURIs) {
                    this.model.add(uri, KS.COMPONENT_SUB_STRING, componentURI);
                }
            }

            this.model.add(uri, NIF.BEGIN_INDEX, this.vf.createLiteral(begin));
            this.model.add(uri, NIF.END_INDEX, this.vf.createLiteral(offset));
            this.model.add(uri, NIF.ANCHOR_OF, this.vf.createLiteral(anchor));

            return uri;
        }

    }

    private static final class InstanceMention {

        static final int TIME = 0x01;

        static final int NAME = 0x02;

        static final int NOUN = 0x04;

        static final int PRONOUN = 0x08;

        static final int PREDICATE = 0x10;

        static final int ATTRIBUTE = 0x20;

        static final int ALL = TIME | NAME | NOUN | PRONOUN | PREDICATE | ATTRIBUTE;

        final URI uri;

        final Term head;

        final List<Term> extent;

        int excludeMask;

        InstanceMention(final URI uri, final Term head, final List<Term> extent) {
            this.uri = uri;
            this.head = head;
            this.extent = Lists.newArrayList(extent);
        }

    }

}
