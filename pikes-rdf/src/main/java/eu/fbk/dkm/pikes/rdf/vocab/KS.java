package eu.fbk.dkm.pikes.rdf.vocab;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class KS {

    public static final String PREFIX = "ks";

    public static final String NAMESPACE = "http://dkm.fbk.eu/ontologies/knowledgestore#";

    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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

    // MENTION LAYER

    public static final IRI MENTION = createIRI("Mention");

    public static final IRI INSTANCE_MENTION = createIRI("InstanceMention");

    public static final IRI ATTRIBUTE_MENTION = createIRI("AttributeMention");

    public static final IRI TIME_MENTION = createIRI("TimeMention");

    public static final IRI FRAME_MENTION = createIRI("FrameMention");

    public static final IRI NAME_MENTION = createIRI("NameMention");

    public static final IRI PARTICIPATION_MENTION = createIRI("ParticipationMention");

    public static final IRI COREFERENCE_MENTION = createIRI("CoreferenceMention");

    public static final IRI LINKED_TO = createIRI("linkedTo");

    public static final IRI SYNSET = createIRI("synset");

    public static final IRI MODIFIER_SYNSET = createIRI("modifierSynset");

    public static final IRI NORMALIZED_VALUE = createIRI("normalizedValue");

    public static final IRI BEGIN_POINT = createIRI("normalizedValue"); // TODO

    public static final IRI END_POINT = createIRI("endPointValue"); // TODO

    public static final IRI ANCHOR_TIME = createIRI("anchorTime"); // TODO

    public static final IRI ROLESET = createIRI("roleset");

    public static final IRI FACTUALITY = createIRI("factuality"); // TODO

    public static final IRI POLARITY = createIRI("polarity"); // TODO

    public static final IRI NERC_TYPE = createIRI("nercType");

    public static final IRI FRAME_PROPERTY = createIRI("frame");

    public static final IRI ARGUMENT = createIRI("argument");

    public static final IRI ROLE = createIRI("role");

    public static final IRI COREFERENTIAL = createIRI("coreferential");

    public static final IRI COREFERENTIAL_CONJUNCT = createIRI("coreferentialConjunct");

    public static final IRI EXPRESSES = createIRI("expresses");

    public static final IRI DENOTES = createIRI("denotes");

    public static final IRI IMPLIES = createIRI("implies");

    public static final IRI MENTION_OF = createIRI("mentionOf");

    public static final IRI COMPOUND_STRING = createIRI("CompoundString");

    public static final IRI COMPONENT_SUB_STRING = createIRI("componentSubString");

    public static final IRI LEMMA = createIRI("lemma"); // TODO

    public static final IRI SST = createIRI("sst"); // TODO

    public static final IRI PLURAL = createIRI("plural"); // TODO

    // ENTITY LAYER

    public static final IRI INSTANCE = createIRI("Instance");

    public static final IRI ATTRIBUTE = createIRI("Attribute");

    public static final IRI TIME = createIRI("Time");

    public static final IRI FRAME = createIRI("Frame");

    public static final IRI INCLUDE = createIRI("include");

    // MAPPING

    public static final IRI ARGUMENT_NOMINALIZATION = createIRI("ArgumentNominalization");

    public static final IRI MAPPED_TO = createIRI("mappedTo");

    public static final IRI SUBJECT_ROLE = createIRI("subjectRole");

    public static final IRI COMPLEMENT_ROLE = createIRI("complementRole");

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
    }

    private KS() {
    }

}
