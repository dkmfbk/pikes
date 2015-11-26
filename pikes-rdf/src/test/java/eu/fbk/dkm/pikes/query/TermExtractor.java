package eu.fbk.dkm.pikes.query;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import net.didion.jwnl.data.PointerType;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.SESAME;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;

import eu.fbk.dkm.pikes.kv.KeyQuadIndex;
import eu.fbk.dkm.pikes.kv.KeyQuadSource;
import eu.fbk.dkm.pikes.query.Term.Layer;
import eu.fbk.dkm.pikes.resources.FrameBase;
import eu.fbk.dkm.pikes.resources.NAFUtils;
import eu.fbk.dkm.pikes.resources.Stemming;
import eu.fbk.dkm.pikes.resources.Sumo;
import eu.fbk.dkm.pikes.resources.WordNet;
import eu.fbk.dkm.pikes.resources.YagoTaxonomy;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.vocab.KS;
import eu.fbk.rdfpro.AbstractRDFHandlerWrapper;
import eu.fbk.rdfpro.RDFHandlers;
import eu.fbk.rdfpro.RDFProcessors;
import eu.fbk.rdfpro.RDFSources;
import eu.fbk.rdfpro.util.IO;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;

public class TermExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TermExtractor.class);

    private static final Set<String> LUCENE_STOP_WORDS = ImmutableSet.of("a", "an", "and", "are",
            "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not",
            "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they",
            "this", "to", "was", "will", "with");

    private static final String NS_DBPEDIA = "http://dbpedia.org/resource/";

    private static Map<String, Layer> TYPE_MAP = ImmutableMap.of(YagoTaxonomy.NAMESPACE,
            Layer.TYPE_YAGO, Sumo.NAMESPACE, Layer.TYPE_SUMO, FrameBase.NAMESPACE,
            Layer.PREDICATE_FRB, "http://www.newsreader-project.eu/ontologies/propbank/",
            Layer.PREDICATE_PB, "http://www.newsreader-project.eu/ontologies/nombank/",
            Layer.PREDICATE_NB);

    private static Map<String, Layer> PROPERTY_MAP = ImmutableMap.of(FrameBase.NAMESPACE,
            Layer.ROLE_FRB, "http://www.newsreader-project.eu/ontologies/propbank/",
            Layer.ROLE_PB, "http://www.newsreader-project.eu/ontologies/nombank/", Layer.ROLE_NB);

    private final KeyQuadSource enrichmentIndex;

    public static void main(final String[] args) {
        try {
            // Parse command line
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("pikes-tex")
                    .withOption("i", "index", "use index at PATH for URI enrichment", "PATH",
                            CommandLine.Type.FILE, true, false, false)
                    .withOption("r", "recursive", "whether to recurse into input directories")
                    .withOption("o", "output", "output base name", "PATH",
                            CommandLine.Type.STRING, true, false, true)
                    .withHeader("parses the Yovisto file and emits NAF files for each document")
                    .parse(args);

            // Extract options
            final boolean recursive = cmd.hasOption("r");
            final File index = cmd.getOptionValue("i", File.class, null);
            final File output = cmd.getOptionValue("o", File.class);
            final List<File> files = cmd.getArgs(File.class);

            // Initialize enrichment index, if enabled
            KeyQuadIndex enrichmentIndex = null;
            if (index != null) {
                enrichmentIndex = new KeyQuadIndex(index);
                LOGGER.info("Loaded enrichment index at {}", index);
            }

            // Perform the extraction
            final TermExtractor extractor = new TermExtractor(enrichmentIndex);
            final List<Term> terms = extractor.extract(files, recursive);

            // Write results
            try (Writer writer = IO.utf8Writer(IO.buffer(IO.write(output.getAbsolutePath())))) {
                final Multiset<Term> termSet = HashMultiset.create(terms);
                for (final Term term : Ordering.natural().sortedCopy(termSet.elementSet())) {
                    writer.append(term.getDocument());
                    writer.append("\t");
                    writer.append(term.getLayer().getID());
                    writer.append("\t");
                    writer.append(term.getToken());
                    writer.append("\t");
                    writer.append(Integer.toString(termSet.count(term)));
                    if (!term.getAttributes().isEmpty()) {
                        for (final String key : Ordering.natural().sortedCopy(
                                term.getAttributes().keySet())) {
                            writer.append("\t");
                            writer.append(key);
                            writer.append("=");
                            writer.append(term.getAttributes().get(key));
                        }
                    }
                    writer.write("\n");
                }
            }

            // Release enrichment index, if used
            if (enrichmentIndex != null) {
                enrichmentIndex.close();
            }

        } catch (final Throwable ex) {
            // Display error information and terminate
            CommandLine.fail(ex);
        }
    }

    public TermExtractor(@Nullable final KeyQuadSource enrichmentIndex) {
        this.enrichmentIndex = enrichmentIndex;
    }

    public List<Term> extract(final Iterable<File> files, final boolean recursive)
            throws IOException {

        // Expand file list if recursive
        final List<File> allFiles = Lists.newArrayList(files);
        if (recursive) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    Iterables.addAll(allFiles, Files.fileTreeTraverser().preOrderTraversal(file));
                }
            }
        }

        // Index NAF files and RDF files by name (without extension and folder
        final Map<String, File> annotationFiles = Maps.newHashMap();
        final Map<String, File> modelFiles = Maps.newHashMap();
        for (final File file : allFiles) {
            if (file.isFile()) {
                if (Rio.getParserFormatForFileName(file.getName()) != null) {
                    modelFiles.put(extractBasename(file.getName()), file);
                } else if (extractExtension(file.getName()).startsWith(".naf")) {
                    annotationFiles.put(extractBasename(file.getName()), file);
                }
            }
        }

        // Log before processing
        final long ts = System.currentTimeMillis();
        LOGGER.info("Processing {} annotation files, {} RDF files", annotationFiles.size(),
                modelFiles.size());

        // Process each annotation / RDF file pair, aggregating the results
        int pairs = 0;
        final List<Term> result = Lists.newArrayList();
        for (final String basename : Ordering.natural().sortedCopy(annotationFiles.keySet())) {
            final File annotationFile = annotationFiles.get(basename);
            final File modelFile = modelFiles.get(basename);
            if (annotationFile != null && modelFile != null) {
                result.addAll(extract(annotationFile, modelFile));
                ++pairs;
            }
        }

        // Log after processing
        LOGGER.info("Processing of {} file pairs completed in {} ms", pairs,
                System.currentTimeMillis() - ts);

        // Return resulting terms
        return result;
    }

    public List<Term> extract(final File annotationFile, final File modelFile) throws IOException {

        // Read annotation file
        final KAFDocument annotation;
        try (Reader reader = IO.utf8Reader(IO.buffer(IO.read(annotationFile.getAbsolutePath())))) {
            annotation = KAFDocument.createFromStream(reader);
        }

        // Read RDF file
        final QuadModel model = QuadModel.create();
        try {
            RDFSources.read(false, true, null, null, modelFile.getAbsolutePath()).emit(
                    new AbstractRDFHandlerWrapper(RDFHandlers.wrap(model)) {

                        @Override
                        public void handleStatement(final Statement stmt)
                                throws RDFHandlerException {
                            super.handleStatement(Statements.VALUE_FACTORY.createStatement(
                                    stmt.getSubject(), stmt.getPredicate(), stmt.getObject()));
                        }

                    }, 1);
        } catch (final RDFHandlerException ex) {
            throw new IOException(ex);
        }

        // Delegate
        return extract(annotation, model);
    }

    public List<Term> extract(final KAFDocument document, final Iterable<Statement> model) {

        // Obtain document ID from NAF document
        String documentID = document.getPublic().publicId;
        if (Strings.isNullOrEmpty(documentID)) {
            documentID = extractBasename(document.getPublic().uri);
        }

        // Obtain a quad model over RDF statements
        final QuadModel quadModel = model instanceof QuadModel ? (QuadModel) model //
                : QuadModel.create(model);

        try {
            // Recursively enrich model URIs if an enrichment index is available
            if (this.enrichmentIndex != null) {
                final Set<URI> uris = Sets.newHashSet();
                for (final Statement stmt : quadModel) {
                    for (final Value value : new Value[] { stmt.getSubject(), stmt.getPredicate(),
                            stmt.getObject(), stmt.getContext() }) {
                        if (value instanceof URI) {
                            uris.add((URI) value);
                        }
                    }
                }
                final int numTriplesBefore = quadModel.size();
                this.enrichmentIndex.getRecursive(uris, null, RDFHandlers.wrap(quadModel));
                LOGGER.debug("Enriched {} URIs with {} triples", uris.size(), quadModel.size()
                        - numTriplesBefore);
            }

            // Perform inference
            final int numTriplesBefore = quadModel.size();
            RDFProcessors.rdfs(RDFSources.wrap(ImmutableList.copyOf(quadModel)), SESAME.NIL, true,
                    true, "rdfs4a", "rdfs4b", "rdfs8").apply(RDFSources.NIL,
                    RDFHandlers.wrap(quadModel), 1);
            LOGGER.debug("Inferred {} triples (total {})", quadModel.size() - numTriplesBefore,
                    quadModel.size());

        } catch (final RDFHandlerException ex) {
            // Wrap and propagate
            Throwables.propagate(ex);
        }

        // Process NAF and model
        final List<Term> terms = Lists.newArrayList();
        extract(documentID, document, terms);
        extract(documentID, quadModel, terms);
        return terms;
    }

    private void extract(final String documentID, final QuadModel model,
            final Collection<Term> terms) {

        // Emit terms for URIs
        for (final Resource entity : model.filter(null, RDF.TYPE, KS.ENTITY).subjects()) {
            if (entity instanceof URI) {
                final URI uri = (URI) entity;
                if (uri.getNamespace().equals(NS_DBPEDIA)) {
                    terms.add(new Term(documentID, Layer.URI_DBPEDIA, uri.getLocalName()));
                } else if (model.contains(uri, FOAF.NAME, null)) {
                    terms.add(new Term(documentID, Layer.URI_CUSTOM, uri.getLocalName()));
                }
            }
        }

        // Emit terms for types and properties
        for (final Statement stmt : model) {
            final URI p = stmt.getPredicate();
            final Value o = stmt.getObject();
            final Layer propertyLayer = PROPERTY_MAP.get(p.getNamespace());
            if (propertyLayer != null) {
                terms.add(new Term(documentID, propertyLayer, p.getLocalName()));
            }
            if (p.equals(RDF.TYPE) && o instanceof URI) {
                final URI uri = (URI) o;
                final Layer typeLayer = TYPE_MAP.get(uri.getNamespace());
                if (typeLayer != null) {
                    terms.add(new Term(documentID, typeLayer, uri.getLocalName()));
                }
            }
        }
    }

    private void extract(final String documentID, final KAFDocument document,
            final Collection<Term> terms) {

        for (final ixa.kaflib.Term term : document.getTerms()) {

            final String wf = term.getStr();
            if (!isValidTerm(wf)) {
                continue;
            }

            final String lemma = term.getLemma();
            final String stem = Stemming.stem("en", wf);
            terms.add(new Term(documentID, Layer.STEM_TEXT, stem));
            terms.add(new Term(documentID, Layer.LEMMA_TEXT, lemma));

            final String pos = term.getMorphofeat();
            final String wnPos;
            if (pos.startsWith("NN")) {
                wnPos = WordNet.POS_NOUN;
            } else if (pos.startsWith("VB")) {
                wnPos = WordNet.POS_VERB;
            } else if (pos.startsWith("JJ")) {
                wnPos = WordNet.POS_ADJECTIVE;
            } else if (pos.startsWith("RB") || pos.equals("WRB")) {
                wnPos = WordNet.POS_ADVERB;
            } else {
                wnPos = null;
            }

            if (wnPos != null) {
                final List<String> synsets = WordNet.getSynsetsForLemma(lemma, wnPos);
                if (!synsets.isEmpty()) {
                    Set<String> synsetsCertain = null;
                    for (final String synset : synsets) {
                        if (synsetsCertain == null) {
                            synsetsCertain = WordNet.getHypernyms(synset, true);
                        } else {
                            synsetsCertain.retainAll(WordNet.getHypernyms(synset, true));
                        }
                    }
                    String synset = null;
                    if (synsets.size() == 1) {
                        synset = synsets.get(0);
                    } else {
                        final ExternalRef synsetRef = NAFUtils.getRef(term, "wn30-ukb", null);
                        if (synsetRef != null) {
                            synset = synsetRef.getReference();
                        }
                    }
                    if (synset != null) {
                        expandSynsets(documentID, synset, 0, synsetsCertain, Sets.newHashSet(),
                                terms);
                        if (synsetsCertain.contains(synset)) {
                            for (final String synonym : WordNet.getLemmas(synset)) {
                                terms.add(new Term(documentID, Layer.LEMMA_SYNONYM, synonym));
                                terms.add(new Term(documentID, Layer.STEM_SYNONYM, Stemming.stem(
                                        "en", synonym)));
                            }
                            final Set<String> relatedSynsets = Sets.newHashSet();
                            for (final PointerType pt : new PointerType[] { PointerType.DERIVED,
                                    PointerType.PERTAINYM, PointerType.NOMINALIZATION,
                                    PointerType.PARTICIPLE_OF }) {
                                relatedSynsets.addAll(WordNet.getGenericSet(synset, pt));
                            }
                            final Set<String> relatedLemmas = Sets.newHashSet();
                            for (final String relatedSynset : relatedSynsets) {
                                relatedLemmas.addAll(WordNet.getLemmas(relatedSynset));
                                terms.add(new Term(documentID, Layer.SYNSET_RELATED,
                                        relatedSynset, "certain", true));
                            }
                            for (final String relatedLemma : relatedLemmas) {
                                terms.add(new Term(documentID, Layer.LEMMA_RELATED, relatedLemma));
                                terms.add(new Term(documentID, Layer.STEM_RELATED, Stemming.stem(
                                        "en", relatedLemma)));
                            }
                        }
                    }
                }
            }
        }
    }

    private void expandSynsets(final String documentID, final String synset, final int len,
            final Set<String> synsetsCertain, final Set<String> synsetsSeen,
            final Collection<Term> terms) {
        if (synsetsSeen.add(synset)) {
            final boolean certain = synsetsCertain == null || synsetsCertain.contains(synset);
            if (len == 0) {
                terms.add(new Term(documentID, Layer.SYNSET_SPECIFIC, synset, "certain", certain));
            } else {
                terms.add(new Term(documentID, Layer.SYNSET_HYPERNYN, synset, "certain", certain,
                        "len", len));
            }
            for (final String hypernym : WordNet.getHypernyms(synset, false)) {
                expandSynsets(documentID, hypernym, len + 1, synsetsCertain, synsetsSeen, terms);
            }
        }
    }

    private static boolean isValidTerm(final String wf) {
        if (!LUCENE_STOP_WORDS.contains(wf.toLowerCase())) {
            for (int i = 0; i < wf.length(); ++i) {
                if (Character.isLetterOrDigit(wf.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String extractBasename(final String location) {
        Objects.requireNonNull(location);
        int extEnd = location.length() - (location.endsWith("/") ? 1 : 0);
        if (location.indexOf(':') >= 0) {
            int index = location.lastIndexOf('#');
            extEnd = index < 0 ? extEnd : index;
            index = location.lastIndexOf('?', extEnd);
            extEnd = index < 0 ? extEnd : index;
        }
        final int nameStart = Math.max(-1, location.lastIndexOf('/', extEnd - 1)) + 1;
        int extStart = location.lastIndexOf('.', extEnd);
        final String ext = extStart < 0 ? "" : location.substring(extStart, extEnd);
        if (ext.equals(".gz") || ext.equals(".bz2") || ext.equals(".xz") || ext.equals(".7z")
                || ext.equals(".lz4")) {
            final int index = location.lastIndexOf('.', extStart - 1);
            extStart = index < 0 ? extStart : index;
        }
        return location.substring(nameStart, extStart);
    }

    private static String extractExtension(final String location) {
        Objects.requireNonNull(location);
        final int index = location.indexOf(':');
        int extEnd = location.length();
        if (index >= 0) {
            if (location.charAt(0) == '.') {
                return location.substring(0, index);
            }
            int index2 = location.lastIndexOf('#');
            extEnd = index2 < 0 ? extEnd : index2;
            index2 = location.lastIndexOf('?', extEnd);
            extEnd = index2 < 0 ? extEnd : index2;
        }
        int extStart = location.lastIndexOf('.', extEnd);
        String ext = extStart < 0 ? "" : location.substring(extStart, extEnd);
        if (ext.equals(".gz") || ext.equals(".bz2") || ext.equals(".xz") || ext.equals(".7z")
                || ext.equals(".lz4")) {
            extStart = location.lastIndexOf('.', extStart - 1);
            ext = extStart < 0 ? "" : location.substring(extStart, extEnd);
        }
        return ext;
    }

}
