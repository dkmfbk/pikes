package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class KS_OLD {

    public static final String PREFIX = "ks";

    public static final String NAMESPACE = "https://dkm.fbk.eu/ontologies/knowledgestore#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // Not emitted by RDFGenerator (if opinion layer not present in NAF)

    public static final IRI TERM = createIRI("Term");

    public static final IRI ROOT_TERM = createIRI("RootTerm");

    public static final IRI OPINION = createIRI("Opinion");

    public static final IRI NEUTRAL_OPINION = createIRI("NeutralOpinion");

    public static final IRI POSITIVE_OPINION = createIRI("PositiveOpinion");

    public static final IRI NEGATIVE_OPINION = createIRI("NegativeOpinion");

    public static final IRI WORD = createIRI("word");

    public static final IRI STEM = createIRI("stem");

    public static final IRI POS = createIRI("pos");

    public static final IRI MORPHOFEAT = createIRI("morphofeat");

    public static final IRI HYPERNYM = createIRI("hypernym");

    public static final IRI BBN = createIRI("bbn");

    public static final IRI INDEX = createIRI("index");

    public static final IRI OFFSET = createIRI("offset");

    public static final IRI HAS_TERM = createIRI("term");

    public static final IRI HAS_HEAD = createIRI("head");

    public static final IRI EXPRESSION = createIRI("expression");

    public static final IRI HOLDER = createIRI("holder");

    public static final IRI TARGET = createIRI("target");

    public static final IRI EXPRESSION_SPAN = createIRI("expressionSpan");

    public static final IRI HOLDER_SPAN = createIRI("holderSpan");

    public static final IRI TARGET_SPAN = createIRI("targetSpan");

    // public static final IRI SENTENCE = createIRI("sentence");

    // public static final IRI DOCUMENT = createIRI("document");

    // RESOURCE LAYER

    public static final IRI RESOURCE = createIRI("Resource");

    public static final IRI TEXT = createIRI("Text");

    public static final IRI NAF = createIRI("NAF");

    public static final IRI TEXT_HASH = createIRI("textHash");

    public static final IRI ANNOTATED_WITH = createIRI("annotatedWith");

    public static final IRI ANNOTATION_OF = createIRI("annotationOf");

    public static final IRI VERSION = createIRI("version");

    public static final IRI LAYER = createIRI("layer");

    public static final IRI NAF_FILE_NAME = createIRI("nafFileName");

    public static final IRI NAF_FILE_TYPE = createIRI("nafFileType");

    public static final IRI NAF_PAGES = createIRI("nafPages");

    // Mention layer

    public static final IRI MENTION = createIRI("Mention");

    public static final IRI ENTITY_MENTION = createIRI("EntityMention");

    public static final IRI TIME_MENTION = createIRI("TimeMention");

    public static final IRI PREDICATE_MENTION = createIRI("PredicateMention");

    public static final IRI ATTRIBUTE_MENTION = createIRI("AttributeMention");

    public static final IRI NAME_MENTION = createIRI("NameMention");

    public static final IRI PARTICIPATION_MENTION = createIRI("ParticipationMention");

    public static final IRI COREFERENCE_MENTION = createIRI("CoreferenceMention"); // TODO

    public static final IRI EXPRESSED_BY = createIRI("expressedBy"); // TODO

    public static final IRI LEMMA = createIRI("lemma");

    public static final IRI SYNSET = createIRI("synset");

    public static final IRI SST = createIRI("sst");

    public static final IRI MENTION_OF = createIRI("mentionOf"); // TODO only one needed

    public static final IRI HAS_MENTION = createIRI("hasMention");

    public static final IRI COMPOUND_STRING = createIRI("CompoundString");

    public static final IRI COMPONENT_SUB_STRING = createIRI("componentSubString");

    // public static final IRI CONFIDENCE = createIRI("confidence"); // double

    // ENTITY LAYER

    public static final IRI ENTITY = createIRI("Entity");

    public static final IRI PREDICATE = createIRI("Predicate");

    public static final IRI TIME = createIRI("Time");

    public static final IRI ATTRIBUTE = createIRI("Attribute");

    public static final IRI INCLUDE = createIRI("include");

    // public static final IRI ARGUMENT = createIRI("argument");

    public static final IRI PROVENANCE = createIRI("provenance"); // TODO string or IRI?

    public static final IRI LANGUAGE = createIRI("language"); // TODO string or IRI

    public static final IRI PLURAL = createIRI("plural"); // boolean

    public static final IRI QUANTITY = createIRI("quantity"); // decimal or string

    public static final IRI RANK = createIRI("rank"); // int or string

    public static final IRI PERCENTAGE = createIRI("percentage"); // decimal or string

    public static final IRI MOD = createIRI("mod");

    public static final IRI HEAD_SYNSET = createIRI("headSynset");

    public static final IRI FACTUALITY = createIRI("factuality");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private KS_OLD() {
    }

}
