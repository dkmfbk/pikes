package eu.fbk.dkm.pikes.rdf.vocab;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

public final class KS {

    public static final String PREFIX = "ks";

    public static final String NAMESPACE = "http://dkm.fbk.eu/ontologies/knowledgestore#";

    public static final Namespace NS = new NamespaceImpl(PREFIX, NAMESPACE);

    // RESOURCE LAYER

    public static final URI RESOURCE = createURI("Resource");

    public static final URI TEXT = createURI("Text");

    public static final URI NAF = createURI("NAF");

    public static final URI TEXT_HASH = createURI("textHash");

    public static final URI ANNOTATED_WITH = createURI("annotatedWith");

    public static final URI ANNOTATION_OF = createURI("annotationOf");

    public static final URI VERSION = createURI("version");

    public static final URI LAYER = createURI("layer");

    public static final URI NAF_FILE_NAME = createURI("nafFileName");

    public static final URI NAF_FILE_TYPE = createURI("nafFileType");

    public static final URI NAF_PAGES = createURI("nafPages");

    // MENTION LAYER

    public static final URI MENTION = createURI("Mention");

    public static final URI INSTANCE_MENTION = createURI("InstanceMention");

    public static final URI ATTRIBUTE_MENTION = createURI("AttributeMention");

    public static final URI TIME_MENTION = createURI("TimeMention");

    public static final URI FRAME_MENTION = createURI("FrameMention");

    public static final URI NAME_MENTION = createURI("NameMention");

    public static final URI PARTICIPATION_MENTION = createURI("ParticipationMention");

    public static final URI COREFERENCE_MENTION = createURI("CoreferenceMention");

    public static final URI LINKED_TO = createURI("linkedTo");

    public static final URI SYNSET = createURI("synset");

    public static final URI MODIFIER_SYNSET = createURI("modifierSynset");

    public static final URI NORMALIZED_VALUE = createURI("normalizedValue");

    public static final URI BEGIN_POINT = createURI("normalizedValue"); // TODO

    public static final URI END_POINT = createURI("endPointValue"); // TODO

    public static final URI ANCHOR_TIME = createURI("anchorTime"); // TODO

    public static final URI ROLESET = createURI("roleset");

    public static final URI FACTUALITY = createURI("factuality"); // TODO

    public static final URI POLARITY = createURI("polarity"); // TODO

    public static final URI NERC_TYPE = createURI("nercType");

    public static final URI FRAME_PROPERTY = createURI("frame");

    public static final URI ARGUMENT = createURI("argument");

    public static final URI ROLE = createURI("role");

    public static final URI COREFERENTIAL = createURI("coreferential");

    public static final URI COREFERENTIAL_CONJUNCT = createURI("coreferentialConjunct");

    public static final URI EXPRESSES = createURI("expresses");

    public static final URI DENOTES = createURI("denotes");

    public static final URI IMPLIES = createURI("implies");

    public static final URI MENTION_OF = createURI("mentionOf");

    public static final URI COMPOUND_STRING = createURI("CompoundString");

    public static final URI COMPONENT_SUB_STRING = createURI("componentSubString");

    public static final URI LEMMA = createURI("lemma"); // TODO

    public static final URI SST = createURI("sst"); // TODO

    public static final URI PLURAL = createURI("plural"); // TODO

    // ENTITY LAYER

    public static final URI INSTANCE = createURI("Instance");

    public static final URI ATTRIBUTE = createURI("Attribute");

    public static final URI TIME = createURI("Time");

    public static final URI FRAME = createURI("Frame");

    public static final URI INCLUDE = createURI("include");

    // MAPPING

    public static final URI ARGUMENT_NOMINALIZATION = createURI("ArgumentNominalization");

    public static final URI MAPPED_TO = createURI("mappedTo");

    public static final URI SUBJECT_ROLE = createURI("subjectRole");

    public static final URI COMPLEMENT_ROLE = createURI("complementRole");

    // HELPER METHODS

    private static URI createURI(final String localName) {
        return ValueFactoryImpl.getInstance().createURI(NAMESPACE, localName);
    }

    private KS() {
    }

}
