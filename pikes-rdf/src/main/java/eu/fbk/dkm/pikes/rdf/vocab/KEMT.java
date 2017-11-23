package eu.fbk.dkm.pikes.rdf.vocab;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import eu.fbk.rdfpro.vocab.VOID;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the KEM Text vocabulary.
 */
public class KEMT {

    /** Recommended prefix for the vocabulary namespace: "kemt". */
    public static final String PREFIX = "kemt";

    /** Vocabulary namespace: "http://knowledgestore.fbk.eu/ontologies/kem/text#". */
    public static final String NAMESPACE = "http://knowledgestore.fbk.eu/ontologies/kem/text#";

    /** Immutable {@link Namespace} constant for the vocabulary namespace. */
    public static final Namespace NS = new SimpleNamespace(VOID.PREFIX, VOID.NAMESPACE);

    // CLASSES

    /** Class kemt:Argument. */
    public static final IRI ARGUMENT_C = createIRI("Argument");

    /** Class kemt:Aspect. */
    public static final IRI ASPECT_C = createIRI("Aspect");

    /** Class kemt:AspectualLink. */
    public static final IRI ASPECTUAL_LINK = createIRI("AspectualLink");

    /** Class kemt:AspectualRelation. */
    public static final IRI ASPECTUAL_RELATION = createIRI("AspectualRelation");

    /** Class kemt:CausalLink. */
    public static final IRI CAUSAL_LINK = createIRI("CausalLink");

    /** Class kemt:CausalRelation. */
    public static final IRI CAUSAL_RELATION = createIRI("CausalRelation");

    /** Class kemt:CompoundWord. */
    public static final IRI COMPOUND_WORD = createIRI("CompoundWord");

    /** Class kemt:ConstituentNode. */
    public static final IRI CONSTITUENT_NODE = createIRI("ConstituentNode");

    /** Class kemt:ConstituentString. */
    public static final IRI CONSTITUENT_STRING = createIRI("ConstituentString");

    /** Class kemt:Coordination. */
    public static final IRI COORDINATION = createIRI("Coordination");

    /** Class kemt:Coreference. */
    public static final IRI COREFERENCE = createIRI("Coreference");

    /** Class kemt:EntityAnnotation. */
    public static final IRI ENTITY_ANNOTATION = createIRI("EntityAnnotation");

    /** Class kemt:EventInstance. */
    public static final IRI EVENT_INSTANCE = createIRI("EventInstance");

    /** Class kemt:EventType. */
    public static final IRI EVENT_TYPE = createIRI("EventType");

    /** Class kemt:FactualityLink. */
    public static final IRI FACTUALITY_LINK = createIRI("FactualityLink");

    /** Class kemt:FactualityRelation. */
    public static final IRI FACTUALITY_RELATION = createIRI("FactualityRelation");

    /** Class kemt:FactualitySource. */
    public static final IRI FACTUALITY_SOURCE = createIRI("FactualitySource");

    /** Class kemt:FactValue. */
    public static final IRI FACT_VALUE_C = createIRI("FactValue");

    /** Class kemt:Feature. */
    public static final IRI FEATURE_C = createIRI("Feature");

    /** Class kemt:FunctionInDocument. */
    public static final IRI FUNCTION_IN_DOCUMENT_C = createIRI("FunctionInDocument");

    /** Class kemt:Link. */
    public static final IRI LINK = createIRI("Link");

    /** Class kemt:NamedEntity. */
    public static final IRI NAMED_ENTITY = createIRI("NamedEntity");

    /** Class kemt:Participation. */
    public static final IRI PARTICIPATION = createIRI("Participation");

    /** Class kemt:Polarity. */
    public static final IRI POLARITY_C = createIRI("Polarity");

    /** Class kemt:Predicate. */
    public static final IRI PREDICATE_C = createIRI("Predicate");

    /** Class kemt:Relation. */
    public static final IRI RELATION_C = createIRI("Relation");

    /** Class kemt:RelationAnnotation. */
    public static final IRI RELATION_ANNOTATION = createIRI("RelationAnnotation");

    /** Class kemt:SubordinateLink. */
    public static final IRI SUBORDINATE_LINK = createIRI("SubordinateLink");

    /** Class kemt:SubordinateRelation. */
    public static final IRI SUBORDINATE_RELATION = createIRI("SubordinateRelation");

    /** Class kemt:TemporalElement. */
    public static final IRI TEMPORAL_ELEMENT = createIRI("TemporalElement");

    /** Class kemt:TemporalLink. */
    public static final IRI TEMPORAL_LINK = createIRI("TemporalLink");

    /** Class kemt:TemporalModifier. */
    public static final IRI TEMPORAL_MODIFIER = createIRI("TemporalModifier");

    /** Class kemt:TemporalRelation. */
    public static final IRI TEMPORAL_RELATION = createIRI("TemporalRelation");

    /** Class kemt:Tense. */
    public static final IRI TENSE_C = createIRI("Tense");

    /** Class kemt:TextResource. */
    public static final IRI TEXT_RESOURCE = createIRI("TextResource");

    /** Class kemt:Timex. */
    public static final IRI TIMEX = createIRI("Timex");

    /** Class kemt:TimexType. */
    public static final IRI TIMEX_TYPE = createIRI("TimexType");

    /** Class kemt:Type. */
    public static final IRI TYPE_C = createIRI("Type");

    /** Class kemt:WordComponent. */
    public static final IRI WORD_COMPONENT = createIRI("WordComponent");

    // OBJECT PROPERTIES

    /** Object property kemt:argument. */
    public static final IRI ARGUMENT_P = createIRI("argument");

    /** Object property kemt:aspect. */
    public static final IRI ASPECT_P = createIRI("aspect");

    /** Object property kemt:conjunct. */
    public static final IRI CONJUNCT = createIRI("conjunct");

    /** Object property kemt:conjunctString. */
    public static final IRI CONJUNCT_STRING = createIRI("conjunctString");

    /** Object property kemt:coreferring. */
    public static final IRI COREFERRING = createIRI("coreferring");

    /** Object property kemt:coreferringString. */
    public static final IRI COREFERRING_STRING = createIRI("coreferringString");

    /** Object property kemt:dominates. */
    public static final IRI DOMINATES = createIRI("dominates");

    /** Object property kemt:factValue. */
    public static final IRI FACT_VALUE_P = createIRI("factValue");

    /** Object property kemt:feature. */
    public static final IRI FEATURE_P = createIRI("feature");

    /** Object property kemt:functionInDocument. */
    public static final IRI FUNCTION_IN_DOCUMENT_P = createIRI("functionInDocument");

    /** Object property kemt:group. */
    public static final IRI GROUP = createIRI("group");

    /** Object property kemt:headComponent. */
    public static final IRI HEAD_COMPONENT = createIRI("headComponent");

    /** Object property kemt:headConstituentNode. */
    public static final IRI HEAD_CONSTITUENT_NODE = createIRI("headConstituentNode");

    /** Object property kemt:headWord. */
    public static final IRI HEAD_WORD = createIRI("headWord");

    /** Object property kemt:immediatelyDominates. */
    public static final IRI IMMEDIATELY_DOMINATES = createIRI("immediatelyDominates");

    /** Object property kemt:modifier. */
    public static final IRI MODIFIER = createIRI("modifier");

    /** Object property kemt:objectValue. */
    public static final IRI OBJECT_VALUE = createIRI("objectValue");

    /** Object property kemt:polarity. */
    public static final IRI POLARITY_P = createIRI("polarity");

    /** Object property kemt:predicate. */
    public static final IRI PREDICATE_P = createIRI("predicate");

    /** Object property kemt:rawString. */
    public static final IRI RAW_STRING = createIRI("rawString");

    /** Object property kemt:relates. */
    public static final IRI RELATES = createIRI("relates");

    /** Object property kemt:relation. */
    public static final IRI RELATION_P = createIRI("relation");

    /** Object property kemt:signalString. */
    public static final IRI SIGNAL_STRING = createIRI("signalString");

    /** Object property kemt:source. */
    public static final IRI SOURCE = createIRI("source");

    /** Object property kemt:syntacticCategory. */
    public static final IRI SYNTACTIC_CATEGORY = createIRI("syntacticCategory");

    /** Object property kemt:target. */
    public static final IRI TARGET = createIRI("target");

    /** Object property kemt:tense. */
    public static final IRI TENSE_P = createIRI("tense");

    /** Object property kemt:type. */
    public static final IRI TYPE_P = createIRI("type");

    // DATATYPE PROPERTIES

    /** Datatype property kemt:frequency. */
    public static final IRI FREQUENCY = createIRI("frequency");

    /** Datatype property kemt:literalValue. */
    public static final IRI LITERAL_VALUE = createIRI("literalValue");

    /** Datatype property kemt:modality. */
    public static final IRI MODALITY = createIRI("modality");

    /** Datatype property kemt:properName. */
    public static final IRI PROPER_NAME = createIRI("properName");

    /** Datatype property kemt:quantifier. */
    public static final IRI QUANTIFIER = createIRI("quantifier");

    /** Datatype property kemt:temporalFunction. */
    public static final IRI TEMPORAL_FUNCTION = createIRI("temporalFunction");

    /** Datatype property kemt:unit. */
    public static final IRI UNIT = createIRI("unit");

    // NAMED INDIVIDUALS

    /** Named individual kemt:arel_continues. */
    public static final IRI AREL_CONTINUES = createIRI("arel_continues");

    /** Named individual kemt:arel_culminates. */
    public static final IRI AREL_CULMINATES = createIRI("arel_culminates");

    /** Named individual kemt:arel_initiates. */
    public static final IRI AREL_INITIATES = createIRI("arel_initiates");

    /** Named individual kemt:arel_reinitiates. */
    public static final IRI AREL_REINITIATES = createIRI("arel_reinitiates");

    /** Named individual kemt:arel_terminates. */
    public static final IRI AREL_TERMINATES = createIRI("arel_terminates");

    /** Named individual kemt:aspect_none. */
    public static final IRI ASPECT_NONE = createIRI("aspect_none");

    /** Named individual kemt:aspect_perfective. */
    public static final IRI ASPECT_PERFECTIVE = createIRI("aspect_perfective");

    /** Named individual kemt:aspect_perfective_progressive. */
    public static final IRI ASPECT_PERFECTIVE_PROGRESSIVE = createIRI(
            "aspect_perfective_progressive");

    /** Named individual kemt:aspect_progressive. */
    public static final IRI ASPECT_PROGRESSIVE = createIRI("aspect_progressive");

    /** Named individual kemt:cause. */
    public static final IRI CAUSE = createIRI("cause");

    /** Named individual kemt:et_aspectual. */
    public static final IRI ET_ASPECTUAL = createIRI("et_aspectual");

    /** Named individual kemt:et_i_action. */
    public static final IRI ET_I_ACTION = createIRI("et_i_action");

    /** Named individual kemt:et_i_state. */
    public static final IRI ET_I_STATE = createIRI("et_i_state");

    /** Named individual kemt:et_occurrence. */
    public static final IRI ET_OCCURRENCE = createIRI("et_occurrence");

    /** Named individual kemt:et_perception. */
    public static final IRI ET_PERCEPTION = createIRI("et_perception");

    /** Named individual kemt:et_reporting. */
    public static final IRI ET_REPORTING = createIRI("et_reporting");

    /** Named individual kemt:et_state. */
    public static final IRI ET_STATE = createIRI("et_state");

    /** Named individual kemt:fn_creation_time. */
    public static final IRI FN_CREATION_TIME = createIRI("fn_creation_time");

    /** Named individual kemt:fn_expiration_time. */
    public static final IRI FN_EXPIRATION_TIME = createIRI("fn_expiration_time");

    /** Named individual kemt:fn_modification_time. */
    public static final IRI FN_MODIFICATION_TIME = createIRI("fn_modification_time");

    /** Named individual kemt:fn_none. */
    public static final IRI FN_NONE = createIRI("fn_none");

    /** Named individual kemt:fn_publication_time. */
    public static final IRI FN_PUBLICATION_TIME = createIRI("fn_publication_time");

    /** Named individual kemt:fn_reception_time. */
    public static final IRI FN_RECEPTION_TIME = createIRI("fn_reception_time");

    /** Named individual kemt:fn_release_time. */
    public static final IRI FN_RELEASE_TIME = createIRI("fn_release_time");

    /** Named individual kemt:frel_event_origin. */
    public static final IRI FREL_EVENT_ORIGIN = createIRI("frel_event_origin");

    /** Named individual kemt:frel_factuality_assignment. */
    public static final IRI FREL_FACTUALITY_ASSIGNMENT = createIRI("frel_factuality_assignment");

    /** Named individual kemt:frel_source_introduction. */
    public static final IRI FREL_SOURCE_INTRODUCTION = createIRI("frel_source_introduction");

    /** Named individual kemt:fv_CTn. */
    public static final IRI FV_CTN = createIRI("fv_CTn");

    /** Named individual kemt:fv_CTp. */
    public static final IRI FV_CTP = createIRI("fv_CTp");

    /** Named individual kemt:fv_CTu. */
    public static final IRI FV_CTU = createIRI("fv_CTu");

    /** Named individual kemt:fv_na. */
    public static final IRI FV_NA = createIRI("fv_na");

    /** Named individual kemt:fv_other. */
    public static final IRI FV_OTHER = createIRI("fv_other");

    /** Named individual kemt:fv_PRn. */
    public static final IRI FV_PRN = createIRI("fv_PRn");

    /** Named individual kemt:fv_PRp. */
    public static final IRI FV_PRP = createIRI("fv_PRp");

    /** Named individual kemt:fv_PRu. */
    public static final IRI FV_PRU = createIRI("fv_PRu");

    /** Named individual kemt:fv_PSn. */
    public static final IRI FV_PSN = createIRI("fv_PSn");

    /** Named individual kemt:fv_PSp. */
    public static final IRI FV_PSP = createIRI("fv_PSp");

    /** Named individual kemt:fv_PSu. */
    public static final IRI FV_PSU = createIRI("fv_PSu");

    /** Named individual kemt:fv_Uu. */
    public static final IRI FV_UU = createIRI("fv_Uu");

    /** Named individual kemt:mod_after. */
    public static final IRI MOD_AFTER = createIRI("mod_after");

    /** Named individual kemt:mod_approx. */
    public static final IRI MOD_APPROX = createIRI("mod_approx");

    /** Named individual kemt:mod_before. */
    public static final IRI MOD_BEFORE = createIRI("mod_before");

    /** Named individual kemt:mod_end. */
    public static final IRI MOD_END = createIRI("mod_end");

    /** Named individual kemt:mod_equal_or_less. */
    public static final IRI MOD_EQUAL_OR_LESS = createIRI("mod_equal_or_less");

    /** Named individual kemt:mod_equal_or_more. */
    public static final IRI MOD_EQUAL_OR_MORE = createIRI("mod_equal_or_more");

    /** Named individual kemt:mod_less_than. */
    public static final IRI MOD_LESS_THAN = createIRI("mod_less_than");

    /** Named individual kemt:mod_mid. */
    public static final IRI MOD_MID = createIRI("mod_mid");

    /** Named individual kemt:mod_more_than. */
    public static final IRI MOD_MORE_THAN = createIRI("mod_more_than");

    /** Named individual kemt:mod_on_or_after. */
    public static final IRI MOD_ON_OR_AFTER = createIRI("mod_on_or_after");

    /** Named individual kemt:mod_on_or_before. */
    public static final IRI MOD_ON_OR_BEFORE = createIRI("mod_on_or_before");

    /** Named individual kemt:mod_start. */
    public static final IRI MOD_START = createIRI("mod_start");

    /** Named individual kemt:polarity_neg. */
    public static final IRI POLARITY_NEG = createIRI("polarity_neg");

    /** Named individual kemt:polarity_pos. */
    public static final IRI POLARITY_POS = createIRI("polarity_pos");

    /** Named individual kemt:srel_conditional. */
    public static final IRI SREL_CONDITIONAL = createIRI("srel_conditional");

    /** Named individual kemt:srel_counter_factive. */
    public static final IRI SREL_COUNTER_FACTIVE = createIRI("srel_counter_factive");

    /** Named individual kemt:srel_evidential. */
    public static final IRI SREL_EVIDENTIAL = createIRI("srel_evidential");

    /** Named individual kemt:srel_factive. */
    public static final IRI SREL_FACTIVE = createIRI("srel_factive");

    /** Named individual kemt:srel_modal. */
    public static final IRI SREL_MODAL = createIRI("srel_modal");

    /** Named individual kemt:srel_neg_evidential. */
    public static final IRI SREL_NEG_EVIDENTIAL = createIRI("srel_neg_evidential");

    /** Named individual kemt:tense_future. */
    public static final IRI TENSE_FUTURE = createIRI("tense_future");

    /** Named individual kemt:tense_infinitive. */
    public static final IRI TENSE_INFINITIVE = createIRI("tense_infinitive");

    /** Named individual kemt:tense_none. */
    public static final IRI TENSE_NONE = createIRI("tense_none");

    /** Named individual kemt:tense_past. */
    public static final IRI TENSE_PAST = createIRI("tense_past");

    /** Named individual kemt:tense_pastpart. */
    public static final IRI TENSE_PASTPART = createIRI("tense_pastpart");

    /** Named individual kemt:tense_present. */
    public static final IRI TENSE_PRESENT = createIRI("tense_present");

    /** Named individual kemt:tense_prespart. */
    public static final IRI TENSE_PRESPART = createIRI("tense_prespart");

    /** Named individual kemt:trel_after. */
    public static final IRI TREL_AFTER = createIRI("trel_after");

    /** Named individual kemt:trel_before. */
    public static final IRI TREL_BEFORE = createIRI("trel_before");

    /** Named individual kemt:trel_begins. */
    public static final IRI TREL_BEGINS = createIRI("trel_begins");

    /** Named individual kemt:trel_begun_by. */
    public static final IRI TREL_BEGUN_BY = createIRI("trel_begun_by");

    /** Named individual kemt:trel_during. */
    public static final IRI TREL_DURING = createIRI("trel_during");

    /** Named individual kemt:trel_during_inv. */
    public static final IRI TREL_DURING_INV = createIRI("trel_during_inv");

    /** Named individual kemt:trel_ended_by. */
    public static final IRI TREL_ENDED_BY = createIRI("trel_ended_by");

    /** Named individual kemt:trel_ends. */
    public static final IRI TREL_ENDS = createIRI("trel_ends");

    /** Named individual kemt:trel_iafter. */
    public static final IRI TREL_IAFTER = createIRI("trel_iafter");

    /** Named individual kemt:trel_ibefore. */
    public static final IRI TREL_IBEFORE = createIRI("trel_ibefore");

    /** Named individual kemt:trel_identity. */
    public static final IRI TREL_IDENTITY = createIRI("trel_identity");

    /** Named individual kemt:trel_includes. */
    public static final IRI TREL_INCLUDES = createIRI("trel_includes");

    /** Named individual kemt:trel_is_included. */
    public static final IRI TREL_IS_INCLUDED = createIRI("trel_is_included");

    /** Named individual kemt:trel_simultaneous. */
    public static final IRI TREL_SIMULTANEOUS = createIRI("trel_simultaneous");

    /** Named individual kemt:trel_timex_anchor_time. */
    public static final IRI TREL_TIMEX_ANCHOR_TIME = createIRI("trel_timex_anchor_time");

    /** Named individual kemt:trel_timex_begin_point. */
    public static final IRI TREL_TIMEX_BEGIN_POINT = createIRI("trel_timex_begin_point");

    /** Named individual kemt:trel_timex_end_point. */
    public static final IRI TREL_TIMEX_END_POINT = createIRI("trel_timex_end_point");

    /** Named individual kemt:tt_date. */
    public static final IRI TT_DATE = createIRI("tt_date");

    /** Named individual kemt:tt_duration. */
    public static final IRI TT_DURATION = createIRI("tt_duration");

    /** Named individual kemt:tt_set. */
    public static final IRI TT_SET = createIRI("tt_set");

    /** Named individual kemt:tt_time. */
    public static final IRI TT_TIME = createIRI("tt_time");

    // ALL TERMS

    /** Set of terms defined in this vocabulary. */
    public static Set<IRI> TERMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ARGUMENT_C, ASPECT_C, ASPECTUAL_LINK, ASPECTUAL_RELATION, CAUSAL_LINK, CAUSAL_RELATION,
            COMPOUND_WORD, CONSTITUENT_NODE, CONSTITUENT_STRING, COORDINATION, COREFERENCE,
            ENTITY_ANNOTATION, EVENT_INSTANCE, EVENT_TYPE, FACTUALITY_LINK, FACTUALITY_RELATION,
            FACTUALITY_SOURCE, FACT_VALUE_C, FEATURE_C, FUNCTION_IN_DOCUMENT_C, LINK, NAMED_ENTITY,
            PARTICIPATION, POLARITY_C, PREDICATE_C, RELATION_C, RELATION_ANNOTATION,
            SUBORDINATE_LINK, SUBORDINATE_RELATION, TEMPORAL_ELEMENT, TEMPORAL_LINK,
            TEMPORAL_MODIFIER, TEMPORAL_RELATION, TENSE_C, TEXT_RESOURCE, TIMEX, TIMEX_TYPE,
            TYPE_C, WORD_COMPONENT, ARGUMENT_P, ASPECT_P, CONJUNCT, CONJUNCT_STRING, COREFERRING,
            COREFERRING_STRING, DOMINATES, FACT_VALUE_P, FEATURE_P, FUNCTION_IN_DOCUMENT_P, GROUP,
            HEAD_COMPONENT, HEAD_CONSTITUENT_NODE, HEAD_WORD, IMMEDIATELY_DOMINATES, MODIFIER,
            OBJECT_VALUE, POLARITY_P, PREDICATE_P, RAW_STRING, RELATES, RELATION_P, SIGNAL_STRING,
            SOURCE, SYNTACTIC_CATEGORY, TARGET, TENSE_P, TYPE_P, FREQUENCY, LITERAL_VALUE,
            MODALITY, PROPER_NAME, QUANTIFIER, TEMPORAL_FUNCTION, UNIT, AREL_CONTINUES,
            AREL_CULMINATES, AREL_INITIATES, AREL_REINITIATES, AREL_TERMINATES, ASPECT_NONE,
            ASPECT_PERFECTIVE, ASPECT_PERFECTIVE_PROGRESSIVE, ASPECT_PROGRESSIVE, CAUSE,
            ET_ASPECTUAL, ET_I_ACTION, ET_I_STATE, ET_OCCURRENCE, ET_PERCEPTION, ET_REPORTING,
            ET_STATE, FN_CREATION_TIME, FN_EXPIRATION_TIME, FN_MODIFICATION_TIME, FN_NONE,
            FN_PUBLICATION_TIME, FN_RECEPTION_TIME, FN_RELEASE_TIME, FREL_EVENT_ORIGIN,
            FREL_FACTUALITY_ASSIGNMENT, FREL_SOURCE_INTRODUCTION, FV_CTN, FV_CTP, FV_CTU, FV_NA,
            FV_OTHER, FV_PRN, FV_PRP, FV_PRU, FV_PSN, FV_PSP, FV_PSU, FV_UU, MOD_AFTER, MOD_APPROX,
            MOD_BEFORE, MOD_END, MOD_EQUAL_OR_LESS, MOD_EQUAL_OR_MORE, MOD_LESS_THAN, MOD_MID,
            MOD_MORE_THAN, MOD_ON_OR_AFTER, MOD_ON_OR_BEFORE, MOD_START, POLARITY_NEG,
            POLARITY_POS, SREL_CONDITIONAL, SREL_COUNTER_FACTIVE, SREL_EVIDENTIAL, SREL_FACTIVE,
            SREL_MODAL, SREL_NEG_EVIDENTIAL, TENSE_FUTURE, TENSE_INFINITIVE, TENSE_NONE,
            TENSE_PAST, TENSE_PASTPART, TENSE_PRESENT, TENSE_PRESPART, TREL_AFTER, TREL_BEFORE,
            TREL_BEGINS, TREL_BEGUN_BY, TREL_DURING, TREL_DURING_INV, TREL_ENDED_BY, TREL_ENDS,
            TREL_IAFTER, TREL_IBEFORE, TREL_IDENTITY, TREL_INCLUDES, TREL_IS_INCLUDED,
            TREL_SIMULTANEOUS, TREL_TIMEX_ANCHOR_TIME, TREL_TIMEX_BEGIN_POINT,
            TREL_TIMEX_END_POINT, TT_DATE, TT_DURATION, TT_SET, TT_TIME)));

    // HELPER METHODS

    private static IRI createIRI(final String localName) {
        return SimpleValueFactory.getInstance().createIRI(VOID.NAMESPACE, localName);
    }

    private KEMT() {
    }

}
