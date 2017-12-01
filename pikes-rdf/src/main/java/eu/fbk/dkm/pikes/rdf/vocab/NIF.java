package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the NIF 2.0 Core Ontology (draft).
 *
 * @see <a href="http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html">
 * vocabulary specification</a>
 */
public final class NIF {

    /** Recommended prefix for the vocabulary namespace: "nif". */
    public static final String PREFIX = "nif";

    /** Vocabulary namespace: "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#". */
    public static final String NAMESPACE = "http://persistence.uni-leipzig.org"
            + "/nlp2rdf/ontologies/nif-core#";

    /** Immutable {@link Namespace} constant for the vocabulary namespace. */
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // CLASSES

    /** Class nif:ArbitraryString. */
    public static final IRI ARBITRARY_STRING = createIRI("ArbitraryString");

    /** Class nif:CollectionOccurrence. */
    public static final IRI COLLECTION_OCCURRENCE = createIRI("CollectionOccurrence");

    /** Class nif:Context. */
    public static final IRI CONTEXT = createIRI("Context");

    /** Class nif:ContextHashBasedString. */
    public static final IRI CONTEXT_HASH_BASED_STRING = createIRI("ContextHashBasedString");

    /** Class nif:ContextOccurrence. */
    public static final IRI CONTEXT_OCCURRENCE = createIRI("ContextOccurrence");

    /** Class nif:LabelString. */
    public static final IRI LABEL_STRING = createIRI("LabelString");

    /** Class nif:NormalizedCollectionOccurrence. */
    public static final IRI NORMALIZED_COLLECTION_OCCURRENCE = //
            createIRI("NormalizedCollectionOccurrence");

    /** Class nif:NormalizedContextOccurrence. */
    public static final IRI NORMALIZED_CONTEXT_OCCURRENCE = //
            createIRI("NormalizedContextOccurrence");

    /** Class nif:OccurringString. */
    public static final IRI OCCURRING_STRING = createIRI("OccurringString");

    /** Class nif:OffsetBasedString. */
    public static final IRI OFFSET_BASED_STRING = createIRI("OffsetBasedString");

    /** Class nif:Paragraph. */
    public static final IRI PARAGRAPH = createIRI("Paragraph");

    /** Class nif:Phrase. */
    public static final IRI PHRASE = createIRI("Phrase");

    /** Class nif:RFC5147String. */
    public static final IRI RFC5147_STRING = createIRI("RFC5147String");

    /** Class nif:Sentence. */
    public static final IRI SENTENCE = createIRI("Sentence");

    /** Class nif:String. */
    public static final IRI STRING = createIRI("String");

    /** Class nif:Structure. */
    public static final IRI STRUCTURE = createIRI("Structure");

    /** Class nif:Title. */
    public static final IRI TITLE = createIRI("Title");

    /** Class nif:IRIScheme. */
    public static final IRI IRISCHEME = createIRI("IRIScheme");

    /** Class nif:Word. */
    public static final IRI WORD = createIRI("Word");

    // PROPERTIES

    /** Property nif:confidence. */
    public static final IRI CONFIDENCE = createIRI("confidence");

    /** Property nif:after. */
    public static final IRI AFTER = createIRI("after");

    /** Property nif:anchorOf. */
    public static final IRI ANCHOR_OF = createIRI("anchorOf");

    /** Property nif:annotation. */
    public static final IRI ANNOTATION = createIRI("annotation");

    /** Property nif:before. */
    public static final IRI BEFORE = createIRI("before");

    /** Property nif:beginIndex. */
    public static final IRI BEGIN_INDEX = createIRI("beginIndex");

    /** Property nif:broaderContext. */
    public static final IRI BROADER_CONTEXT = createIRI("broaderContext");

    /** Property nif:class. */
    public static final IRI CLASS = createIRI("class");

    /** Property nif:classAnnotation. */
    public static final IRI CLASS_ANNOTATION = createIRI("classAnnotation");

    /** Property nif:endIndex. */
    public static final IRI END_INDEX = createIRI("endIndex");

    /** Property nif:firstWord. */
    public static final IRI FIRST_WORD = createIRI("firstWord");

    /** Property nif:head. */
    public static final IRI HEAD = createIRI("head");

    /** Property nif:inter. */
    public static final IRI INTER = createIRI("inter");

    /** Property nif:isString. */
    public static final IRI IS_STRING = createIRI("isString");

    /** Property nif:lastWord. */
    public static final IRI LAST_WORD = createIRI("lastWord");

    /** Property nif:lemma. */
    public static final IRI LEMMA = createIRI("lemma");

    /** Property nif:literalAnnotation. */
    public static final IRI LITERAL_ANNOTATION = createIRI("literalAnnotation");

    /** Property nif:narrowerContext. */
    public static final IRI NARROWER_CONTEXT = createIRI("narrowerContext");

    /** Property nif:nextSentence. */
    public static final IRI NEXT_SENTENCE = createIRI("nextSentence");

    /** Property nif:nextSentenceTrans. */
    public static final IRI NEXT_SENTENCE_TRANS = createIRI("nextSentenceTrans");

    /** Property nif:nextWord. */
    public static final IRI NEXT_WORD = createIRI("nextWord");

    /** Property nif:nextWordTrans. */
    public static final IRI NEXT_WORD_TRANS = createIRI("nextWordTrans");

    /** Property nif:occurrence. */
    public static final IRI OCCURRENCE = createIRI("occurrence");

    /** Property nif:oliaCategory. */
    public static final IRI OLIA_CATEGORY = createIRI("oliaCategory");

    /** Property nif:oliaCategoryConf. */
    public static final IRI OLIA_CATEGORY_CONF = createIRI("oliaCategoryConf");

    /** Property nif:oliaLink. */
    public static final IRI OLIA_LINK = createIRI("oliaLink");

    /** Property nif:oliaLinkConf. */
    public static final IRI OLIA_LINK_CONF = createIRI("oliaLinkConf");

    /** Property nif:opinion. */
    public static final IRI OPINION = createIRI("opinion");

    /** Property nif:posTag. */
    public static final IRI POS_TAG = createIRI("posTag");

    /** Property nif:previousSentence. */
    public static final IRI PREVIOUS_SENTENCE = createIRI("previousSentence");

    /** Property nif:previousSentenceTrans. */
    public static final IRI PREVIOUS_SENTENCE_TRANS = createIRI("previousSentenceTrans");

    /** Property nif:previousWord. */
    public static final IRI PREVIOUS_WORD = createIRI("previousWord");

    /** Property nif:previousWordTrans. */
    public static final IRI PREVIOUS_WORD_TRANS = createIRI("previousWordTrans");

    /** Property nif:referenceContext. */
    public static final IRI REFERENCE_CONTEXT = createIRI("referenceContext");

    /** Property nif:sentence. */
    public static final IRI SENTENCE_PROPERTY = createIRI("sentence");

    /** Property nif:sentimentValue. */
    public static final IRI SENTIMENT_VALUE = createIRI("sentimentValue");

    /** Property nif:sourceUrl. */
    public static final IRI SOURCE_URL = createIRI("sourceUrl");

    /** Property nif:stem. */
    public static final IRI STEM = createIRI("stem");

    /** Property nif:subString. */
    public static final IRI SUB_STRING = createIRI("subString");

    /** Property nif:subStringTrans. */
    public static final IRI SUB_STRING_TRANS = createIRI("subStringTrans");

    /** Property nif:superString. */
    public static final IRI SUPER_STRING = createIRI("superString");

    /** Property nif:superStringTrans. */
    public static final IRI SUPER_STRING_TRANS = createIRI("superStringTrans");

    /** Property nif:wasConvertedFrom. */
    public static final IRI WAS_CONVERTED_FROM = createIRI("wasConvertedFrom");

    /** Property nif:word. */
    public static final IRI WORD_PROPERTY = createIRI("word");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private NIF() {
    }

}
