PIKES Example
===

We provide here an example of the processing of a short text sentence in PIKES, reporting and discussing the intermediate results produced by PIKES two phases: *linguistic feature extraction* and *knowledge distillation*. We consider the following English text:

```html
Donald Trump and Hillary Clinton are competitors for the White House.
```


## Linguistic Feature Extraction

Given the example text as input, in this phase PIKES extracts an RDF mention graph that summarizes the NLP annotations relevant for Knowledge Extraction, abstracting from the tools producing them. We report below the most relevant triples of the mention graph (note that for clarity's sake we use human-readable URIs for mentions, instances, and named graphs, instead of the actual URIs that are necessarily more complex due to the need to avoid clashes): 

```html
mention:trump        a ks:NameMention;   nif:anchorOf "Donald Trump";    ks:linkedTo dbpedia:Donald_Trump.
mention:clinton      a ks:NameMention;   nif:anchorOf "Hillary Clinton"; ks:linkedTo dbpedia:Hillary_Rodham_Clinton.
mention:whitehouse   a ks:NameMention;   nif:anchorOf "White House";     ks:linkedTo dbpedia:White_House.
mention:competitors  a ks:FrameMention;  nif:anchorOf "competitors";     ks:predicate pm:nb10-competitor.01;
                     ks:synset wn30:10533013-n.

mention:competitors  a ks:ParticipationMention;     ks:role pm:nb10-competitor.01-arg0;
                     ks:frame mention:competitors;  ks:argument mention:competitors.

mention:competitors_whitehouse  a ks:ParticipationMention;     ks:role pm:nb10-competitor.01-arg2;
                                ks:frame mention:competitors;  ks:argument mention:whitehouse.

mention:trump_clinton_competitors  a ks:CoreferenceMention; ks:coreferential mention:competitors; 
                                   ks:coreferentialConjunct mention:trump, mention:clinton.
```

In the graph, "Donald Trump", "Hillary Clinton" and "White House" are recognized via NERC as mentions of named entities (resp. `mention:trump`, `mention:clinton`, and `mention:whitehouse`) that are linked to the corresponding DBpedia URIs (via EL); "competitors" (`mention:competitors`) is recognized via SRL as a predicate denoting a competing event. This event frame is participated by some "competitors" persons and the "White House" (see participation mention `mention:competitors_whitehouse`); "competitors" - intended as the set of persons - also corefers with the pair "Donald Trump" and "Hillary Clinton" (see coreference mention `mention:trump_clinton_competitors`). 


## Knowledge Distillation

The knowledge distillation phase consists in the iterative, fix-point application of mapping rules to the mention graph and a static set of *mapping triples*, to derive Instance layer triples, which are subsequently post-processed to derive the final knowledge graph. 

### Mapping Rules Evaluation

The following mapping triples are relevant for the considered example: they specify that `pm:nb10-competitor.01` (a NomBank predicate) triggers argument nominalization (i.e., it stands both for the frame event and for one of its arguments), and they provide a number of mappings between predicates, roles, and synsets in the mention graph to TBox classes and properties of the knowledge graph, as well as TBox axioms involving these classes and properties (e.g., subsumption):  

```html
pm:nb10-competitor.01 a ks:ArgumentNominalization.

_:mapping1 a ks:Mapping;  ks:synset wn30:10533013-n;           ks:class dbyago:Rival110533013.
_:mapping2 a ks:Mapping;  ks:predicate pm:nb10-competitor.01;  ks:class framebase:frame-Competition-compete.v.
_:mapping3 a ks:Mapping;  ks:role pm:nb10-competitor.01-arg0;  ks:property framebase:fe-Competition-Participants.
_:mapping4 a ks:Mapping;  ks:role pm:nb10-competitor.01-arg2;  ks:property framebase:fe-Competition-Prize.

dbyago:Rival110533013 rdfs:subClassOf dbyago:Contestant109613191, dbyago:Person100007846, dbyago:CausalAgent100007347,
                                      dbyago:Organism100004475, dbyago:YagoLegalActor, dbyago:PhysicalEntity10000193,
                                      dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo, dbyago:Whole100003553,
                                      dbyago:Object100002684.

framebase:frame-Competition-compete.v rdfs:subClassOf framebase:frame-Competition, framebase:frame-Frame.

framebase:fe-Competition-Participants rdfs:subPropertyOf framebase:fe-Frame-Participants.
framebase:fe-Competition-Prize rdfs:subPropertyOf framebase:fe-Frame-Prize.

foaf:name rdfs:subClassOf rdfs:label.
```

The fix-point evaluation of mapping rules on the input mention graph and on the mapping triples takes two iterations. 
In the first iteration, the mapping rules related to instance creation are triggered and generate the following triples, which assert the existence of five instances (by means of `rdf:type` triples placed in named graphs to track provenance) linking them to their mentions in the text (by means of `ks:denotes`, `ks:implies`, and `ks:expresses` triples). Note that due to argument nominalization being triggered by `pm:nb10-competitor.01`, two instances `instance:competitors` (the competing persons) and `instance:competitors_pred` (the competing frame) are derived from `mention:competitors`.  

```html
graph:trump       { instance:trump            a ks:Instance }
graph:clinton     { instance:clinton          a ks:Instance }
graph:whitehouse  { instance:whitehouse       a ks:Instance }
graph:competitors { instance:competitors      a ks:Instance.
                    instance:competitors_pred a ks:Instance, ks:Frame }

mention:trump       ks:denotes instance:trump;            ks:expresses graph:trump.
mention:clinton     ks:denotes instance:clinton;          ks:expresses graph:clinton.
mention:whitehouse  ks:denotes instance:whitehouse;       ks:expresses graph:whitehouse.
mention:competitors ks:denotes instance:competitors;      ks:expresses graph:competitors;
                    ks:implies instance:competitors_pred.
```

In the second iteration, the remaining mapping rules for typing, naming, linking, participation and coreference handling are triggered. The knowledge graph resulting after this last iteration is reported below (newly added triples in <i>italics</i>):

<pre class="html">graph:trump       { instance:trump            a ks:Instance;  <i>foaf:name "Donald Trump";     owl:sameAs dbpedia:Donald_Trump</i> }
graph:clinton     { instance:clinton          a ks:Instance;  <i>foaf:name "Hillary Clinton";  owl:sameAs dbpedia:Hillary_Rodham_Clinton</i> }
graph:whitehouse  { instance:whitehouse       a ks:Instance;  <i>foaf:name "White House";      owl:sameAs dbpedia:White_House</i> }
graph:competitors { instance:competitors      a ks:Instance,  <i>dbyago:Rival110533013.</i>
                    instance:competitors_pred a ks:Instance, ks:Frame, <i>framebase:frame-Competition-compete.v</i>; 
                                              <i>framebase:fe-Competition-Participants instance:competitors</i> }

<i>graph:competitors_whitehouse</i>    <i>{ instance:competitors_pred framebase:fe-Competition-Prize instance:whitehouse }</i>
<i>graph:trump_clinton_competitors { instance:competitors ks:includes instance:trump, instance:clinton }</i>

mention:trump       ks:denotes instance:trump;            ks:expresses graph:trump.
mention:clinton     ks:denotes instance:clinton;          ks:expresses graph:clinton.
mention:whitehouse  ks:denotes instance:whitehouse;       ks:expresses graph:whitehouse.
mention:competitors ks:denotes instance:competitors;      ks:expresses graph:competitors;
                    ks:implies instance:competitors_pred.

<i>mention:competitors_whitehouse    ks:expresses graph:competitors_whitehouse.</i>
<i>mention:trump_clinton_competitors ks:expresses graph:trump_clinton_competitors.</i>
</pre>


### Post-processing

Post-processing consists in the sequential application of four processing tasks to the RDF resulting from mapping rules: (i) *inference*; (ii) *smushing*; (iii) *redundancy elimination*; (iv) *compaction*.

**Inference** -- Named graphs-aware OWL 2 RL inference rules (excluded axioms for `owl:sameAs`, which are considered in the next step), as well as rules propagating triples on group instances to the instances they include, are applied to knowledge graph triples to materialize implicit triples that can be derived base on the TBox axioms contained in mapping triples, e.g., to materialize super-class membership axioms. The results of inference for the considered example are reported below (newly added triples in <i>italics</i>):

<pre class="html">graph:trump       { instance:trump            a ks:Instance;                owl:sameAs dbpedia:Donald_Trump;
                                              foaf:name "Donald Trump";     <i>rdfs:label "Donald Trump"</i> }
graph:clinton     { instance:clinton          a ks:Instance;                owl:sameAs dbpedia:Hillary_Rodham_Clinton;
                                              foaf:name "Hillary Clinton";  <i>rdfs:label "Hillary Clinton";</i> }
graph:whitehouse  { instance:whitehouse       a ks:Instance;                owl:sameAs dbpedia:White_House;
                                              foaf:name "White House";      <i>rdfs:label "White House";</i> }
graph:competitors { instance:competitors      a ks:Instance, dbyago:Rival110533013, <i>dbyago:Contestant109613191, dbyago:Person100007846,</i>
                                                <i>dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor,</i>
                                                <i>dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,</i>
                                                <i>dbyago:Whole100003553, dbyago:Object100002684.</i>
                    instance:competitors_pred a ks:Instance, ks:Frame, framebase:frame-Competition-compete.v,
                                                <i>framebase:frame-Competition, framebase:frame-Frame</i>;
                                              framebase:fe-Competition-Participants instance:competitors;
                                              <i>framebase:fe-Frame-Participants instance:competitors</i> }

<i><b>graph:g1 { instance:competitors_pred  framebase:fe-Competition-Participants instance:trump, instance:clinton;</b></i>
                                      <i><b>framebase:fe-Frame-Participants instance:trump, instance:clinton.</b></i>
           <i><b>instance:trump</b></i>             <i><b>a dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,</b></i>
                                        <i><b>dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor,</b></i>
                                        <i><b>dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,</b></i>
                                        <i><b>dbyago:Whole100003553, dbyago:Object100002684.</b></i>
           <i><b>instance:clinton</b></i>           <i><b>a dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,</b></i>
                                        <i><b>dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor,</b></i>
                                        <i><b>dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,</b></i>
                                        <i><b>dbyago:Whole100003553, dbyago:Object100002684. }</b></i>

graph:competitors_whitehouse    { instance:competitors_pred framebase:fe-Competition-Prize instance:whitehouse;
                                                            <i>framebase:fe-Frame-Prize instance:whitehouse</i> }
graph:trump_clinton_competitors { instance:competitors ks:includes instance:trump, instance:clinton }

mention:trump       ks:denotes instance:trump;            ks:expresses graph:trump.
mention:clinton     ks:denotes instance:clinton;          ks:expresses graph:clinton.
mention:whitehouse  ks:denotes instance:whitehouse;       ks:expresses graph:whitehouse.
mention:competitors ks:denotes instance:competitors, <i><b>instance:trump, instance:clinton</b></i>;
                    ks:implies instance:competitors_pred;
                    ks:expresses graph:competitors, <i><b>graph:g1</b></i>.

mention:competitors_whitehouse    ks:expresses graph:competitors_whitehouse.
mention:trump_clinton_competitors ks:expresses graph:trump_clinton_competitors, <i><b>graph:g1</b></i>.
</pre>

As can be seen, inference mainly amounts to materializing super-classes and super-properties (due to limited expressivity of Yago and FrameBase TBoxes, which are essentially class and property hierarchies). In the RDF above, the newly added triples written in <i><b>bold italics</b></i> derive from the rules propagating triples on group instances (`instance:competitors`) to the instances they include (`instance:trump` and `instance:clinton`). As the evaluation of rules is iterative until fix-point, these propagated triples include also the super-classes and super-properties inferred for the group entity. In the future, we expect to include (at least) domain and range axioms in the TBox by mapping selectional constraints in predicate models (FrameNet, VerbNet), so to materialize class membership triples for frame arguments.


**Smushing** -- Smushing replaces URIs linked by `owl:sameAs` triples with a unique representative URI, which in PIKES is chosen (if possible) from DBpedia. The result of smushing for our example is reported below (new/modified triples in <i>italics</i>). Previously generated instance URIs `instance:trump`, `instance:clinton`, `instance:whitehouse` are discarded in favor of the corresponding DBpedia URIs, which are chose as representative URIs.

<pre class="html"><i>graph:trump</i>       { <i>dbpedia:Donald_Trump</i>            <i>a ks:Instance;</i>  <i>foaf:name "Donald Trump";</i>     <i>rdfs:label "Donald Trump"</i> }
<i>graph:clinton</i>     { <i>dbpedia:Hillary_Rodham_Clinton</i>  <i>a ks:Instance;</i>  <i>foaf:name "Hillary Clinton";</i>  <i>rdfs:label "Hillary Clinton"</i> }
<i>graph:whitehouse</i>  { <i>dbpedia:White_House</i>             <i>a ks:Instance;</i>  <i>foaf:name "White House";</i>      <i>rdfs:label "White House"</i> }
graph:competitors { instance:competitors            a ks:Instance, dbyago:Rival110533013, dbyago:Contestant109613191,
                                                      dbyago:Person100007846, dbyago:CausalAgent100007347, dbyago:Organism100004475,
                                                      dbyago:YagoLegalActor, dbyago:PhysicalEntity10000193,
                                                      dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo, dbyago:Whole100003553,
                                                      dbyago:Object100002684.
                    instance:competitors_pred       a ks:Instance, ks:Frame, framebase:frame-Competition-compete.v,
                                                      framebase:frame-Competition, framebase:frame-Frame;
                                                    framebase:fe-Competition-Participants instance:competitors;
                                                    framebase:fe-Frame-Participants instance:competitors }

<i>graph:g1</i> { <i>instance:competitors_pred</i>      <i>framebase:fe-Competition-Participants dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton;</i>
                                          <i>framebase:fe-Frame-Participants dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton.</i>
           <i>dbpedia:Donald_Trump</i>           <i>a ks:Instance, dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,</i>
                                            <i>dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor,</i>
                                            <i>dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,</i>
                                            <i>dbyago:Whole100003553, dbyago:Object100002684.</i>
           <i>dbpedia:Hillary_Rodham_Clinton</i> <i>a ks:Instance, dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,</i>
                                            <i>dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor,</i>
                                            <i>dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,</i>
                                            <i>dbyago:Whole100003553, dbyago:Object100002684.</i> }

<i>graph:competitors_whitehouse</i>    { <i>instance:competitors_pred framebase:fe-Competition-Prize dbpedia:White_House;</i>
                                                            <i>framebase:fe-Frame-Prize dbpedia:White_House</i> }
<i>graph:trump_clinton_competitors</i> { <i>instance:competitors ks:includes dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton</i> }

mention:trump       <i>ks:denotes dbpedia:Donald_Trump;</i>            ks:expresses graph:trump.
mention:clinton     <i>ks:denotes dbpedia:Hillary_Rodham_Clinton;</i>  ks:expresses graph:clinton.
mention:whitehouse  <i>ks:denotes dbpedia:White_House;</i>             ks:expresses graph:whitehouse.
mention:competitors ks:denotes instance:competitors, <i>dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton</i>;
                    ks:implies instance:competitors_pred;
                    ks:expresses graph:competitors, graph:g1.

mention:competitors_whitehouse    ks:expresses graph:competitors_whitehouse.
mention:trump_clinton_competitors ks:expresses graph:trump_clinton_competitors, graph:g1.
</pre>


**Redundancy Elimination** -- In this processing step we discard every unnamed group instance that `ks:includes` member instances, leveraging the fact that its triples have been already propagated to members. For the considered example, this step consists in removing instance `instance:competitors`, obtaining the following RDF:

<pre class="html">graph:trump       { dbpedia:Donald_Trump            a ks:Instance;  foaf:name "Donald Trump";     rdfs:label "Donald Trump" }
graph:clinton     { dbpedia:Hillary_Rodham_Clinton  a ks:Instance;  foaf:name "Hillary Clinton";  rdfs:label "Hillary Clinton" }
graph:whitehouse  { dbpedia:White_House             a ks:Instance;  foaf:name "White House";      rdfs:label "White House" }
graph:competitors { instance:competitors_pred       a ks:Instance, ks:Frame, framebase:frame-Competition-compete.v,
                                                      framebase:frame-Competition, framebase:frame-Frame }

graph:g1 { instance:competitors_pred      framebase:fe-Competition-Participants dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton;
                                          framebase:fe-Frame-Participants dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton.
           dbpedia:Donald_Trump           a ks:Instance, dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,
                                            dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor, 
                                            dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,
                                            dbyago:Whole100003553, dbyago:Object100002684.
           dbpedia:Hillary_Rodham_Clinton a ks:Instance, dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,
                                            dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor, 
                                            dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,
                                            dbyago:Whole100003553, dbyago:Object100002684. }

graph:competitors_whitehouse    { instance:competitors_pred framebase:fe-Competition-Prize dbpedia:White_House;
                                                            framebase:fe-Frame-Prize dbpedia:White_House }

mention:trump       ks:denotes dbpedia:Donald_Trump;            ks:expresses graph:trump.
mention:clinton     ks:denotes dbpedia:Hillary_Rodham_Clinton;  ks:expresses graph:clinton.
mention:whitehouse  ks:denotes dbpedia:White_House;             ks:expresses graph:whitehouse.
mention:competitors ks:denotes dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton;
                    ks:implies instance:competitors_pred;
                    ks:expresses graph:competitors, graph:g1.

mention:competitors_whitehouse    ks:expresses graph:competitors_whitehouse.
mention:trump_clinton_competitors ks:expresses graph:trump_clinton_competitors, graph:g1.
</pre>


**Compaction** -- This last processing steps optimizes the use of named graphs for annotating triples. In PIKES representation, a triple `T` is annotated with all the triples having as subject or object a named graph containing `T`. For instance, if `T` is `dbpedia:Donald_Trump a ks:Instance`, appearing in graphs `graph:trump` and `graph:g1`, its annotations are `mention:trump ks:expresses T`, `mention:competitors ks:expresses T`, and `mention:trump_clinton_competitors ks:expresses T` (note that these annotations cannot be directly expressed as triples since T is a triple itself). Given this model, the compaction step aims at (i) enforcing that each triple appears exactly in one graph (so that one can drop graphs and corresponding annotations without having to deal with duplicate triples); and (ii) placing in the same graph triples having the same annotations.
In the case of the considered example, the result of compaction -- and thus the final knowledge graph returned by PIKES -- is reported below (modified triples in <i>italics</i>):

<pre class="html">graph:trump       { dbpedia:Donald_Trump            foaf:name "Donald Trump";     rdfs:label "Donald Trump" }
graph:clinton     { dbpedia:Hillary_Rodham_Clinton  foaf:name "Hillary Clinton";  rdfs:label "Hillary Clinton" }
graph:whitehouse  { dbpedia:White_House             a ks:Instance;  foaf:name "White House";      rdfs:label "White House" }
graph:competitors { instance:competitors_pred       a ks:Instance, ks:Frame, framebase:frame-Competition-compete.v,
                                                      framebase:frame-Competition, framebase:frame-Frame }

graph:g1 { instance:competitors_pred      framebase:fe-Competition-Participants dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton;
                                          framebase:fe-Frame-Participants dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton.
           dbpedia:Donald_Trump           a dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,
                                            dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor, 
                                            dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,
                                            dbyago:Whole100003553, dbyago:Object100002684.
           dbpedia:Hillary_Rodham_Clinton a dbyago:Rival110533013, dbyago:Contestant109613191, dbyago:Person100007846,
                                            dbyago:CausalAgent100007347, dbyago:Organism100004475, dbyago:YagoLegalActor, 
                                            dbyago:PhysicalEntity10000193, dbyago:LivingThing100004258, dbyago:YagoLegalActorGeo,
                                            dbyago:Whole100003553, dbyago:Object100002684. }
<i>graph:g2</i> { <i>dbpedia:Donald_Trump</i>           <i>a ks:Instance</i> }
<i>graph:g3</i> { <i>dbpedia:Hillary_Rodham_Clinton</i> <i>a ks:Instance</i> }

graph:competitors_whitehouse    { instance:competitors_pred framebase:fe-Competition-Prize dbpedia:White_House;
                                                            framebase:fe-Frame-Prize dbpedia:White_House }

mention:trump       ks:denotes dbpedia:Donald_Trump;            ks:expresses graph:trump, <i>graph:g2</i>.
mention:clinton     ks:denotes dbpedia:Hillary_Rodham_Clinton;  ks:expresses graph:clinton, <i>graph:g3</i>.
mention:whitehouse  ks:denotes dbpedia:White_House;             ks:expresses graph:whitehouse.
mention:competitors ks:denotes dbpedia:Donald_Trump, dbpedia:Hillary_Rodham_Clinton;
                    ks:implies instance:competitors_pred;
                    ks:expresses graph:g1, <i>graph:g2, graph:g3</i>.

mention:competitors_whitehouse    ks:expresses graph:competitors_whitehouse.
mention:trump_clinton_competitors ks:expresses graph:trump_clinton_competitors, graph:g1, <i>graph:g2, graph:g3</i>.
</pre>

In this case, compaction has removed triple `mention:competitors ks:expresses graph:competitors`, as that graph was no more used as a result of previous steps. In addition, triples `dbpedia:Donald_Trump a ks:Instance` and `dbpedia:Hillary_Rodham_Clinton a ks:Instance` that were previously associated to two named graphs each, are now places in unique named graphs `graph:g2` and `graph:g3`. No additional merging were possible in this case, as there are no triples having the same set of annotations (something that usually happens with longer texts). 
