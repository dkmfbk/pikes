Extraction pipeline
===

PIKES **extraction pipeline** starts from an input text and returns the knowledge encoded in RDF extracted from it. Processing within the pipeline can be divided in two main phases [from Text to Mentions](#text_to_mentions) and [from Mentions to Instances](#mentions_to_instances).


#### <a class="anchor" id="text_to_mentions"></a> From Text to Mentions

Given a text resource, PIKES performs the following NLP tasks:

  * **Tokenization**, **Part-of-Speech tagging** (POS tagging) and **Word Sense Disambiguation** (WSD), to split the text into tokens, each of them annotated with its POS (e.g., proper noun, ...) and disambiguated WordNet synset.
  * **Named Entity Recognition and Classification** (NERC) and **Entity Linking**, to identify spans of text denoting entities of predefined categories (e.g., persons, organizations, ...) and link them to well-known entities in external resources such as DBpedia.
  * **Temporal Expression Recognition and Normalization** (TERN), to identify dates, times and temporal durations in a text. A normalized time value (e.g., the date components), based on the [TimeML](http://www.timeml.org/site/index.html) standard, is extracted. Relative temporal expressions such as ‘yesterday’ are normalized based on the *document creation time*.
  * **Coreference Resolution**, to identify sets of text spans, called coreference sets, denoting the same referent.
  * **Dependency Parsing**, to organize the tokens of each sentence in a tree, with directed edges connecting a governing token (e.g., a verb) to its dependent tokens (e.g., its subject), labeled based on the kind of dependency (e.g., subj).
  * **Semantic Role Labeling** (SRL), to annotate occurrences of frames in the text, identifying predicate, arguments and their roles. PIKES consider the SRL annotation with respect to [PropBank](http://verbs.colorado.edu/~mpalmer/projects/ace.html) and [NomBank](http://nlp.cs.nyu.edu/meyers/NomBank.html) frames. Mapping between different frame systems are available and indeed we exploit them in this work.

For the initial NLP tasks, PIKES wraps several state-of-the-art modules: [Stanford CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml) for POS tagging, NERC, TERN, and coreference resolution; [mate-tools](https://code.google.com/p/mate-tools/) for dependency parsing and SRL; [DBpedia Spotlight](http://spotlight.dbpedia.org/) for entity linking; and, [UKB](http://ixa2.si.ehu.es/ukb/) for WSD.

ks:InstanceMentions are extracted from NLP annotations according to the following rules:

  * a `ks:TimeMention` is created for each temporal expression; storing the normalized time (represented using the [OWL Time ontology](http://www.w3.org/TR/owl-time/)) using property `ks:normalizedValue`.
  * a `ks:NameMention` is created for each named entity and token POS-tagged as proper noun; if available, the entity class is stored using property `ks:nercType`.
  * a `ks:FrameMention` is created for each verb or noun predicate recognized by SRL and associated to the PropBank / NomBank roleset via property `ks:roleset`.
  * a `ks:AttributeMention` is created for each adjective or adverb in the text, including its adjectival or adverbial modifiers in the dependency tree (e.g., ‘very’ in case of ‘very strong’); the mention is associated via
property `ks:normalizedValue` to a normalized lexical value URI built based on token synsets.
  * a generic `ks:InstanceMention` is created for each pronoun or common noun not covered by other mentions.

Properties `nif:beginIndex`, `nif:endIndex` and `nif:anchorOf` (the mention textual extent) from the [NLP Interchange Format (NIF) vocabulary](http://nlp2rdf.org/nif-1-0) are extracted for `ks:InstanceMentions`, whose URIs are minted according to the NIF offset-based URI scheme. DBpedia URIs produced by entity linking are associated to corresponding `ks:InstanceMentions` via property `ks:linkedTo`.

`ks:ParticipationMentions` are extracted from text spans marked as predicate arguments by SRL. Properties `ks:role`, `ks:frame` and `ks:argument` associate the `ks:ParticipationMention` respectively to the argument role, to the `ks:FrameMention` for the predicate, and to one or more `ks:InstanceMentions` corresponding to the argument, which are identified based on a regular expression evaluated on the dependency tree.

`ks:CoreferenceMentions` are extracted from coreference sets. Mentions corresponding to coreferential spans are linked to the `ks:CoreferenceMention` using either property `ks:coreferential` or `ks:coreferentialConjunct`. The first is used for mentions matching exactly a coreferential span, whereas the latter is used for mentions corresponding to coordinated conjuncts in a coreferential span.


#### <a class="anchor" id="mentions_to_instances"></a> From Mentions to Instances

An intermediate version of the Instance layer is obtained by processing data of the Mention Layer with **mapping rules** that match certain mention attributes / patterns and create consequent facts in the Instance layer.
Mapping rules are formulated as SPARQL Update `INSERT... WHERE...` statements that are repeatedly executed until a fixed-point is reached.
Rules are allowed to create new individuals (using custom function `ks:mint()`), can invoke external code by means of custom SPARQL functions and can access and match also data in auxiliary resources (e.g., for mapping purposes) as well as the instance data created so far.
Current rules can be organized in six categories based on their function: [instance creation](#instance_creation), [typing](#typing), [naming](#naming), [dbpedia alignment](#dbpedia), [frame-argument linking](#participation) and [coreference assertion](#coreference).

<a class="anchor" id="instance_creation"></a>
**Instance creation**.
A first set of rules (shown below) creates the instances denoted or implied by each `ks:InstanceMention`.
This is done by choosing a suitable instance URI, by generating a type assertion declaring its existence, and by linking the instance to the mention via a `ks:denotes` or `ks:implies` assertion.
Instance URIs are derived from the normalized lexical attribute or time interval in case of a `ks:AttributeMention` or `ks:TimeMention`, thus acting as globally unique, all-disjoint identifiers.
Fresh instance URIs are minted for other mentions, relying on extracted `owl:sameAs` assertions to possibly ground them to externally well-known DBpedia identifiers.
Exactly one instance is derived from a mention, except for a `ks:FrameMention` whose roleset corresponds to an argument nominalization, which both `ks:denotes` a first instance (the role filler) and also `ks:implies` the existence of a second instance (the frame predicate).

    INSERT { GRAPH ?g { ?i a ks:Attribute. }
             ?m ks:expresses ?g; ks:denotes ?i. }
    WHERE  { ?m a ks:AttributeMention; nif:anchorOf ?a; ks:headModifiersSynsetID ?i.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i a ks:Instance. }
             ?m ks:expresses ?g; ks:denotes ?i. }
    WHERE  { ?m a ks:InstanceMention; nif:anchorOf ?a.
             FILTER NOT EXISTS { ?m a ks:TimeMention. }
             FILTER NOT EXISTS { ?m a ks:AttributeMention. }
             FILTER NOT EXISTS { ?m a ks:FrameMention; ks:roleset ?rs. ?rs a ks:ArgumentNominalization. }
             BIND(rr:mint(?a, ?m) AS ?i)
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i a ks:Instance, ks:Time. }
             ?m ks:expresses ?g; ks:denotes ?i. }
    WHERE  { ?m a ks:TimeMention; ks:timeInterval ?i.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i a ks:Instance. ?if a ks:Instance, ks:Frame. }
             ?m ks:expresses ?g; ks:denotes ?i; ks:implies ?if. }
    WHERE  { ?m a ks:FrameMention; nif:anchorOf ?a; ks:roleset ?rs.
             ?rs a ks:ArgumentNominalization.
             BIND(rr:mint(?a, ?m) AS ?i)
             BIND(rr:mint(concat(?a, "_pred"), ?m) AS ?if)
             BIND(rr:mint(fact:, ?m) AS ?g) }

<a class="anchor" id="typing"></a>
**Typing**.
`rdf:type` assertions are generated for extracted instances based on three mention attributes: the WordNet synset of the head token (`ks:synset`); the NERC named entity class (`ks:nercType`); and the PropBank/NomBank roleset from SRL (`ks:roleset`). The first two are mapped to SUMO and YAGO2 classes. The latter are directly used as classes and also mapped to classes for the corresponding VerbNet and FrameNet frames, which are assigned to the ks:Frame instance denoted or implied by the mention. In both cases, the mappings are obtained by matching ks:mappedTo assertions loaded from an external, customizable mapping resource.

    INSERT { GRAPH ?g { ?i a ?t. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:InstanceMention; ks:denotes ?i; ks:synset ?s.
             ?s ks:mappedTo ?t.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i a ?t. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:NameMention; ks:denotes ?i; ks:nercType ?s.
             ?s ks:mappedTo ?t.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?f a ?t. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:FrameMention; ks:denotes|ks:implies ?f; ks:roleset ?s.
             ?f a ks:Frame. ?s ks:mappedTo ?t.
             BIND(rr:mint(fact:, ?m) AS ?g) }

<a class="anchor" id="naming"></a>
**Naming**
An `rdfs:label` assertion is generated for each `ks:InstanceMention`, linking the denoted instance to the textual extent of the mention (property `nif:anchorOf`).
For proper name mentions (`ks:NameMention`), a `foaf:name` assertion is also generated.

    INSERT { GRAPH ?g { ?i rdfs:label ?a. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:InstanceMention; ks:denotes ?i; nif:anchorOf ?a.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i foaf:name ?a. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:NameMention; ks:denotes ?i; nif:anchorOf ?a.
             BIND(rr:mint(fact:, ?m) AS ?g) }

<a class="anchor" id="dbpedia"></a>
**DBpedia alignment**
`owl:sameAs` or `rdfs:seeAlso` assertions between a denoted instance and the corresponding DBpedia resource are generated for `ks:linkedTo` mention attributes.
`owl:sameAs` is used for proper names whereas `rdfs:seeAlso` is used for other mentions, as it is often unclear whether the DBpedia resources linked in these cases can be treated as individuals or classes; moreover, linking of common nouns to DBpedia is in general too noisy to be used with `owl:sameAs` assertions.

    INSERT { GRAPH ?g { ?i owl:sameAs ?u. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:NameMention; ks:denotes ?i; ks:linkedTo ?u.
             BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i rdfs:seeAlso ?u. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:InstanceMention; ks:denotes ?i; ks:linkedTo ?u.
             FILTER NOT EXISTS { ?m a ks:NameMention. }
             BIND(rr:mint(fact:, ?m) AS ?g) }

<a class="anchor" id="participation"></a>
**Frame-argument linking**.
Assertions linking frame instances to argument instances are generated starting from `ks:ParticipationMentions`, by mapping their PropBank/NomBank role (property `ks:role`) to one or more linking properties.
The mapping is performed using `ks:mappedTo` assertions in another external, customizable mapping resource.

    INSERT { GRAPH ?g { ?if ?p ?ia. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:ParticipationMention; ks:frame ?mf; ks:argument ?ma; ks:role ?r.
             ?mf ks:denotes|ks:implies ?if. ?if a ks:Frame.
             ?ma ks:denotes ?ia.
             ?r ks:mappedTo ?p.
             BIND(rr:mint(fact:, ?m) AS ?g) }

<a class="anchor" id="coreference"></a>
**Coreference assertion**.
`owl:sameAs` assertions are generated to link instances denoted by coreferential mentions, i.e., mentions associated to a `ks:CoreferenceMention` via `ks:coreferential` assertions.
The case of a mention coreferring with the coordination of multiple conjunct mentions (via property `ks:coreferentialConjunct`) is handled with the generation of `ks:include` assertions between the conjunct instances and the group instance.
A specific rule also handles copular (or linking) verbs, i.e., verbs such as ‘become’ that links their subject to some complement and may imply the coreference of the two in some situations (e.g., between ‘Joseph Blatter’ and ‘president of FIFA’ in ‘Joseph Blatter became president of FIFA in 1998’).

    INSERT { GRAPH ?g { ?i1 owl:sameAs ?i2. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:CoreferenceMention; ks:coreferential ?m1, ?m2.
             ?m1 ks:denotes ?i1. ?m2 ks:denotes ?i2.
             FILTER(?m1 != ?m2) BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i1 ks:include ?i2. } ?m ks:expresses ?g. }
    WHERE  { ?m a ks:CoreferenceMention; ks:coreferential ?m1; ks:coreferentialConjunct ?m2.
             ?m1 ks:denotes ?i1. ?m2 ks:denotes ?i2. BIND(rr:mint(fact:, ?m) AS ?g) }

    INSERT { GRAPH ?g { ?i3 owl:sameAs ?i2. } ?m1 ks:expresses ?g. }
    WHERE  { ?m1 a ks:FrameMention; ks:roleset ?rs1.
             ?m2 a ks:FrameMention; ks:denotes ?i2.
             ?m3 a ks:InstanceMention; ks:denotes ?i3.
             ?m12 a ks:ParticipationMention; ks:frame ?m1; ks:argument ?m2; ks:role ?r12.
             ?m13 a ks:ParticipationMention; ks:frame ?m1; ks:argument ?m3; ks:role ?r13.
             ?rs1 a ks:CopularVerb; ks:subjectRole ?r13; ks:complementRole ?r12.
             BIND(ks:mint(fact:, ?m1) AS ?g) }

Data resulting from rule-based mapping is then post-processed to obtain the final version of the Instance layer.
Post-processing serves two goals: materialize implicit knowledge; and compact the resulting knowledge base, so to make it more manageable and easy to use.
The first goal is accomplished by materializing all the inferences computed with [RDFS rules](http://www.w3.org/TR/rdf11-mt/), [OWL 2 RL rules](http://www.w3.org/TR/owl2-profiles/#OWL_2_RL) for `owl:sameAs` knowledge propagation, and a custom rule establishing that whenever an instance participates to a frame, also the instances it ks:includes participate to the frame:

    INSERT { GRAPH ?g { ?if ?p ?i } ?m ks:expresses ?g. }
    WHERE  { GRAPH ?g1 { ?if ?p ?ig. }
             GRAPH ?g2 { ?ig ks:include ?i. }
             { ?m ks:expresses ?g1. } UNION { ?m ks:expresses ?g2. }
             ?if a ks:Frame.
             BIND(ks:mint(?g1, ?g2) AS ?g) }

The second is accomplished in three ways. First, PIKES performs smushing, i.e., it keeps only a canonical URI for each instance and discard the remaining `owl:sameAs` aliases.
Then, PIKES discards every unnamed instance that `ks:includes` some member instance, as the custom rule above already propagated relevant participation information to its members.
Finally, PIKES compacts the RDF representation of the links between a mention and the assertions it `ks:expresses`, ensuring that whenever a set of assertions is expressed by the same set of mentions, then
the assertions are placed in a unique graph associated to all the mentions, resulting in a maximal reduction of the number of RDF quads.
Post-processing is implemented using the [RDFpro](http://rdfpro.fbk.eu/) tool extended with several custom plugins.
