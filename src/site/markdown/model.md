Representation model
===

PIKES represents all the information contents in an **RDF model** organized in three distinct yet interlinked representation layers: **Text layer**, **Mention layer**, and **Instance layer**.
The main classes and properties of the model are shown in the figure below, using an UML-like notation and different colors for the different layers (text in red, mentions in green, instances in blue).

<div style="text-align: center; padding-top: 20px; padding-bottom: 20px">
<img src="images/model.png" alt="Representation model of PIKES"/>
</div>


#### Text layer

This is the textual content from which knowledge is extracted.
It consists of `ks:Resource`s (i.e., documents) identified by URIs.
Each resource consists of a raw text and an accompanying RDF description rooted at the resource URI, including metadata attributes such as `dct:title` and `dct:created`.


#### Mention layer

This layer consists of `ks:Mention`s. Different types of mentions are defined in PIKES model. `ks:InstanceMention`s denote instances of the domain of discourse and are further specialized based on the type of instance: `ks:FrameMention` for frame instances; `ks:NameMention` for named instances; `ks:TimeMention` for time intervals; and `ks:AttributeMention` for instances in the value space of lexical attributes (e.g., ‘very strong’). `ks:ParticipationMention`s link argument instances to participated frame instances (e.g., ‘fight of HIV’ links argument ‘HIV’ to frame ‘fight’). `ks:CoreferenceMention`s comprise spans of text having the same referent.


#### Instance layer

The instance layer describes the things of interest contained in a textual resource, abstracting from the actual ways they are expressed in the text.
The main objects are `ks:Instance`s of persons, organizations, locations, frames, dates and other entities of the domain of discourse.
Instances are typed with respect to various taxonomies, are enriched with textual properties (e.g., `rdfs:label` and `foaf:name`), and are linked by a number of relations, including `owl:sameAs` assertions triggered by `ks:CoreferenceMention`s and frame-argument participation assertions triggered by `ks:ParticipationMention`s where the property conveys the role played by the argument.
In this representation, frame instances reify complex relationships and are the main vehicle for relating instances.


#### Inter-layer relations

Mention and Text layers are related by `ks:mentionOf` that links a `ks:Mention` to the `ks:Resource` it belongs to.
Mention and Instance layers are related by three properties: `ks:denotes`, `ks:implies`, and `ks:expresses`:

  * `ks:denotes` links a `ks:InstanceMention` to the `ks:Instance` it denotes.
  * `ks:implies` links a `ks:FrameMention` to another instance (besides the denoted one) whose existence is implied by that mention, a situation occurring in case of argument nominalization.
  * `ks:expresses` links a `ks:Mention` to the Instance layer assertions it expresses (i.e., that can be derived from it). Its RDF representation makes use of named graphs: each assertion of the Instance layer is placed in a named graph that represents the set of mentions (in some cases a single mention) that `ks:expresses` that particular assertion; `ks:expresses` is then asserted between each mention URI and the graph URI.

Put together, properties `ks:denotes`, `ks:implies` and `ks:expresses` allow any instance and assertion in the Instance layer to be always referred to the mention(s) from where it was derived, thus enabling a fine grained tracking of the specific piece of text from where a bit of knowledge was extracted.


#### PIKES and the KnowledgeStore

PIKES representation model is compliant with (and represents a specialization of) the [KnowledgeStore](https://knowledgestore.fbk.eu/) data model (see [KnowledgeStore core data model](https://knowledgestore.fbk.eu/ontologies/knowledgestore.html), meaning that the input and output consumed and produced by PIKES can be used to populate a KnowledgeStore instance, where all the content processed and produced can be accessed, navigated, and queried in an integrated fashion.
