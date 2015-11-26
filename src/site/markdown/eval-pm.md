Evaluation using predicate models TBox
===


<h3 id="overview">Overview</h3>

We evaluate PIKES performances with respect to the state of the art, assessing precision and recall in the extraction of frame structures described using traditional predicate models - [PropBank](https://verbs.colorado.edu/~mpalmer/projects/ace.html) (PB), [NomBank](http://nlp.cs.nyu.edu/meyers/NomBank.html) (NB), [VerbNet](https://verbs.colorado.edu/~mpalmer/projects/verbnet.html) (VN), and [FrameNet](https://framenet.icsi.berkeley.edu/fndrupal/) (FN) as done by tools such as [FRED](http://wit.istc.cnr.it/stlab-tools/fred) [\[2\]](#ekaw2012), [Lodifier](http://www.aifb.kit.edu/web/LODifier/en) and [NewsReader](http://www.newsreader-project.eu/).
Specifically, we compute PIKES precision and recall in extracting the following knowledge graph components from a gold standard text: instances, edges (i.e., instance/instance unlabeled relations), and triples, considered either globally and divided by category: links to [DBpedia](http://dbpedia.org/), VN/FN/PB/NB types, VN/FN/PB/NB participation relations, `owl:sameAs` relations.
We also compare PIKES performances with the ones of FRED, the state-of-the-art tool most similar to PIKES, on the same gold text.

This evaluation has been carried out the first time in September 2015 for the [SAC 2016 paper](publications.html), and we provide here its results. However, as PIKES continues to evolve, we provide also results obtained with the latest version of PIKES (as of November 2015). Latest results are overall better, especially for what concerns FrameNet types and properties due to the use of two SRL tools - Semafor and Mate.

In the following, we describe the [sentences and graphs](#data) used in the evaluation, covering both how we produced the gold graphs and how we obtained the graphs for FRED and PIKES (SAC 2016 and latest graphs).
Then, we report the results of the [separate evaluation](#separate) of PIKES against the gold standard (SAC 2016 and latest results), which covers a large amounts of the features provided by PIKES but with the exclusion of FrameBase types and properties which are evaluated [separately](eval-framebase.html)).
Finally, we report the results of the [comparative evaluation](#comparative) of PIKES and FRED on a simplified gold standard (derived from the one manually built) where both tools are comparable (SAC 2016 and latest results).

This page, the detailed alignment reports linked from this page, and all the compared graphs (gold graphs, FRED graphs, PIKES graphs) are available in a downloadable [ZIP file](eval-pm/pm-evaluation.zip). The original evaluation material submitted to SAC 2016 (with PIKES mentioned as X due to blind submission requirements) remains available at the [URL](http://bit.ly/sac2016evaluation) reported in the paper.


<h3 id="data">Sentences and graphs</h3>

The following table lists the sentences of the gold standard used for the evaluation, each one associated to multiple knowledge graphs: the manually annotated gold graph; the graph produced by FRED and the graph produced by PIKES (SAC 2016 and latest).
The same 8 sentences used in [\[1\]](#eswc2013) have been used, with the minor exception of sentence S7 that was slightly shortened due the unprocessability of its full version with FRED online demo (tested Sep. 2015).

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th rowspan="2">Sentence</th><th rowspan="2">Text</th><th rowspan="2" width="45px">Gold graph</th><th rowspan="2" width="45px">FRED graph</th><th colspan="2">PIKES graph</th></tr>
<tr><th width="45px">SAC</th><th width="45px">latest</th></tr>
</thead>
<tbody>
<tr>
<td>S1</td>
<td>The lone Syrian rebel group with an explicit stamp of approval from Al Qaeda has become one of the uprising most effective fighting forces, posing a stark challenge to the United States and other countries that want to support the rebels but not Islamic extremists.</td>
<td><a href="eval-pm/gold/gold.1.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.1.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.1.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.1.ttl">.ttl</a></td>
</tr>
<tr>
<td>S2</td>
<td>Money flows to the group, the Nusra Front, from like-minded donors abroad.</td>
<td><a href="eval-pm/gold/gold.2.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.2.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.2.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.2.ttl">.ttl</a></td>
</tr>
<tr>
<td>S3</td>
<td>Its fighters, a small minority of the rebels, have the boldness and skill to storm fortified positions and lead other battalions to capture military bases and oil fields.</td>
<td><a href="eval-pm/gold/gold.3.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.3.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.3.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.3.ttl">.ttl</a></td>
</tr>
<tr>
<td>S4</td>
<td>As their successes mount, they gather more weapons and attract more fighters.</td>
<td><a href="eval-pm/gold/gold.4.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.4.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.4.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.4.ttl">.ttl</a></td>
</tr>
<tr>
<td>S5</td>
<td>The group is a direct offshoot of Al Qaeda in Iraq, Iraqi officials and former Iraqi insurgents say, which has contributed veteran fighters and weapons.</td>
<td><a href="eval-pm/gold/gold.5.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.5.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.5.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.5.ttl">.ttl</a></td>
</tr>
<tr>
<td>S6</td>
<td>This is just a simple way of returning the favor to our Syrian brothers that fought with us on the lands of Iraq, said a veteran of Al Qaeda in Iraq, who said he helped lead the Nusra Front's efforts in Syria.</td>
<td><a href="eval-pm/gold/gold.6.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.6.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.6.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.6.ttl">.ttl</a></td>
</tr>
<tr>
<td>S7</td>
<td>The United States, sensing that time may be running out for Syria president Bashar al-Assad, hopes to isolate the group to prevent it from inheriting Syria.</td>
<td><a href="eval-pm/gold/gold.7.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.7.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.7.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.7.ttl">.ttl</a></td>
</tr>
<tr>
<td>S8</td>
<td>As the United States pushes the Syrian opposition to organize a viable alternative government, it plans to blacklist the Nusra Front as a terrorist organization, making it illegal for Americans to have financial dealings with the group and prompting similar sanctions from Europe.</td>
<td><a href="eval-pm/gold/gold.8.ttl">.ttl</a></td>
<td><a href="eval-pm/fred/fred.8.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes-sac/pikes-sac.8.ttl">.ttl</a></td>
<td><a href="eval-pm/pikes/pikes.8.ttl">.ttl</a></td>
</tr>
</tbody>
</table>

The gold graphs, collaboratively built by two annotators, consist of the relevant RDF triples that should be included in the output of a frame-oriented KE system when applied to the corresponding evaluation sentences:

  * The **nodes** of a graph are the instances mentioned in the corresponding sentence (entities, frames, attributes). Each instance is anchored to exactly one mention, with coreferring mentions giving rise to distinct instances. Instances are linked by `owl:sameAs` triples to matching entities in DBpedia, and typed with respect to classes encoding VN, FN, PB and NB frame types (most specific types represented).
  * The **edges** of a graph are given by triples connecting different instances. They express `owl:sameAs` equivalence relations (to explicitly represent and evaluate coreference resolution), instance-attribute association relations, and frame-argument participation relations whose RDF properties encode VN, FN, PB and NB thematic roles.

In order to simplify the manual construction of gold graphs, the link between an instance in a gold graph and the corresponding mention is implicit and given by the instance URI, whose local name corresponds to the head token of the mention in the text. In case of ambiguities, i.e., if there are multiple occurrences of a word in the sentence, a sequential index is added (e.g., in sentence S7, `:syria_1` and `:syria_2` refer respectively to the first and second occurrences of Syria).

FRED graphs were obtained by invoking the public [online demo](http://wit.istc.cnr.it/stlab-tools/fred/demo) of FRED.
The RDF graph for an input sentence was produced according to the following process:

  * We invoked FRED a first time without requiring FN triples, which causes FRED to extract frames based on VerbNet.
  * We invoked FRED a second time requiring FN triples, which causes FRED to omit VN data and, instead, to return frame types encoded in the URIs of frame instances (e.g., `:Hostile_encounter_1`). As FRED authors claim to perform frame detection w.r.t. FN, we extract these frame types from instance URIs and attach them by means of `rdf:type` to the instances obtained from the first invocation of FRED. The result is a single RDF file including both VN and FN frame data.
  * Finally, we rewrite the RDF file by introducing some new prefix and reordering its triples, so to improve readability.

PIKES graphs were obtained using the public demo of [PIKES](https://knowledgestore2.fbk.eu/pikes-demo/). No specific post-processing was necessary, apart a conversion from TriG to Turtle to get rid of unneeded provenance information (for what concerns this evaluation).


<h3 id="separate">Separate evaluation results</h3>

The following table reports the results (SAC 2016, latest) of evaluating PIKES alone against the full gold standard described above, including the number of gold elements (instances, triples, edges), True Positives (# TP), False Positives (# FP), and False Negatives (# FN) for each evaluated component.
The links under the 'alignment' column lead to separate reports showing how elements of the gold standard and elements from PIKES have been matched in the evaluation, which clearly indicate where false positive and false negative errors occurred.

<div>
<ul class="nav nav-tabs" role="tablist">
<li role="presentation"><a href="#separate-sac" data-toggle="tab" role="tab">SAC 2016 results (Sept. 2015)</a></li>
<li role="presentation" class="active"><a href="#separate-latest" data-toggle="tab" role="tab">Latest results (Nov. 2015)</a></li>
</ul>
<div class="tab-content">
<div class="tab-pane" role="tabpanel" id="separate-sac">

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th width="20%">Component</th><th width="10%"># Gold items</th><th width="10%"># TP</th><th width="10%"># FP</th><th width="10%"># FN</th><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th><th width="10%">Alignments</th></tr>
</thead>
<tbody>
<tr><td>Instances</td><td>152</td><td>146</td><td>10</td><td>6</td><td>.936</td><td>.961</td><td>.948</td><td><a href="eval-pm/separate-sac/separate-sac.instances.html">view</a></td></tr>
<tr><td>Triples</td><td>613</td><td>303</td><td>120</td><td>310</td><td>.716</td><td>.494</td><td>.585</td><td>(see below)</td></tr>
<tr><td style="padding-left: 2em">DBpedia links</td><td>18</td><td>13</td><td>6</td><td>5</td><td>.684</td><td>.722</td><td>.703</td><td><a href="eval-pm/separate-sac/separate-sac.links.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (VN)</td><td>44</td><td>24</td><td>10</td><td>20</td><td>.706</td><td>.545</td><td>.615</td><td><a href="eval-pm/separate-sac/separate-sac.types.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (FN)</td><td>59</td><td>19</td><td>7</td><td>40</td><td>.731</td><td>.322</td><td>.447</td><td><a href="eval-pm/separate-sac/separate-sac.types.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (PB)</td><td>53</td><td>38</td><td>7</td><td>15</td><td>.844</td><td>.717</td><td>.776</td><td><a href="eval-pm/separate-sac/separate-sac.types.pb.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (NB)</td><td>37</td><td>29</td><td>13</td><td>8</td><td>.690</td><td>.784</td><td>.734</td><td><a href="eval-pm/separate-sac/separate-sac.types.nb.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (VN)</td><td>94</td><td>46</td><td>16</td><td>48</td><td>.742 </td><td>.489</td><td>.590</td><td><a href="eval-pm/separate-sac/separate-sac.roles.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (FN)</td><td>120</td><td>28</td><td>28</td><td>92</td><td>.500</td><td>.233</td><td>.318</td><td><a href="eval-pm/separate-sac/separate-sac.roles.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (PB)</td><td>119</td><td>69</td><td>13</td><td>50</td><td>.841</td><td>.580</td><td>.687</td><td><a href="eval-pm/separate-sac/separate-sac.roles.pb.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (NB)</td><td>55</td><td>32</td><td>18</td><td>23</td><td>.640</td><td>.582</td><td>.610</td><td><a href="eval-pm/separate-sac/separate-sac.roles.nb.html">view</a></td></tr>
<tr><td style="padding-left: 2em"><tt>owl:sameAs</tt></td><td>14</td><td>5</td><td>2</td><td>9</td><td>.714</td><td>.357</td><td>.476</td><td><a href="eval-pm/separate-sac/separate-sac.sameas.html">view</a></td></tr>
<tr><td>Edges</td><td>172</td><td>133</td><td>13</td><td>39</td><td>.911</td><td>.773</td><td>.836</td><td><a href="eval-pm/separate-sac/separate-sac.edges.html">view</a></td></tr>
</tbody>
</table>

</div>
<div class="tab-pane active" role="tabpanel" id="separate-latest">

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th width="20%">Component</th><th width="10%"># Gold items</th><th width="10%"># TP</th><th width="10%"># FP</th><th width="10%"># FN</th><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th><th width="10%">Alignments</th></tr>
</thead>
<tbody>
<tr><td>Instances</td><td>152</td><td>146</td><td>14</td><td>6</td><td>.913</td><td>.961</td><td>.936</td><td><a href="eval-pm/separate/separate.instances.html">view</a></td></tr>
<tr><td>Triples</td><td>613</td><td>334</td><td>135</td><td>279</td><td>.712</td><td>.545</td><td>.617</td><td>(see below)</td></tr>
<tr><td style="padding-left: 2em">DBpedia links</td><td>18</td><td>13</td><td>6</td><td>5</td><td>.684</td><td>.722</td><td>.703</td><td><a href="eval-pm/separate/separate.links.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (VN)</td><td>44</td><td>24</td><td>10</td><td>20</td><td>.706</td><td>.545</td><td>.615</td><td><a href="eval-pm/separate/separate.types.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (FN)</td><td>59</td><td>38</td><td>25</td><td>21</td><td>.603</td><td>.644</td><td>.623</td><td><a href="eval-pm/separate/separate.types.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (PB)</td><td>53</td><td>37</td><td>7</td><td>16</td><td>.841</td><td>.698</td><td>.763</td><td><a href="eval-pm/separate/separate.types.pb.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (NB)</td><td>37</td><td>24</td><td>7</td><td>13</td><td>.774</td><td>.649</td><td>.706</td><td><a href="eval-pm/separate/separate.types.nb.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (VN)</td><td>94</td><td>47</td><td>15</td><td>47</td><td>.758</td><td>.500</td><td>.603</td><td><a href="eval-pm/separate/separate.roles.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (FN)</td><td>120</td><td>47</td><td>31</td><td>73</td><td>.603</td><td>.392</td><td>.475</td><td><a href="eval-pm/separate/separate.roles.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (PB)</td><td>119</td><td>68</td><td>15</td><td>51</td><td>.819</td><td>.571</td><td>.673</td><td><a href="eval-pm/separate/separate.roles.pb.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (NB)</td><td>55</td><td>31</td><td>18</td><td>24</td><td>.633</td><td>.564</td><td>.596</td><td><a href="eval-pm/separate/separate.roles.nb.html">view</a></td></tr>
<tr><td style="padding-left: 2em"><tt>owl:sameAs</tt></td><td>14</td><td>5</td><td>1</td><td>9</td><td>.833</td><td>.357</td><td>.500</td><td><a href="eval-pm/separate/separate.sameas.html">view</a></td></tr>
<tr><td>Edges</td><td>172</td><td>136</td><td>18</td><td>36</td><td>.883</td><td>.791</td><td>.834</td><td><a href="eval-pm/separate/separate.edges.html">view</a></td></tr>
</tbody>
</table>

</div>
</div>
</div>

<h3 id="comparative">Comparative evaluation results</h3>

In order to fairly compare PIKES with FRED we had to modify/simplify the gold standard to account for two aspects of FRED:

  * FRED does not return PB/NB frame types and PB/NB/FN frame roles, so we removed them from the gold standard;
  * FRED does not support nominal predicates and argument nominalization, although it often represents the associated participation relations with arbitrary triples. To exemplify, the span 'Its fighters’ in sentence S3 is represented by FRED using triple
    `:fighter_1 :fighterOf :neuter_1`
    (where `:fighter_1` and `:neuter_1` are the instances denoted by mentions 'fighters' and 'Its', respectively), whereas the gold standard and PIKES employ the nominal frame (disambiguated w.r.t. FN)
    `:fighter_frame rdf:type fn:Irregular_combatants; fn:combatant :fighter_1; fn:side1 :neuter_1`
    (where `:fighter_frame` is a frame instance also denoted by 'fighters'). Thus, we automatically transform the latter representation - both in the gold standard and in PIKES output - into FRED one.

The following table show the results (SAC 2016, latest) of evaluating FRED and PIKES against the simplified gold standard, including the number of gold elements for each evaluated knowledge graph component and links to separate reports showing how gold elements and elements from FRED and PIKES have been aligned.
PIKES exhibits better precision and recall than FRED for all the considered components, with differences in terms of F1 ranging from .059 to .195 for SAC 2016 results and from 0.042 to 0.187 for latest results.

<div>
<ul class="nav nav-tabs" role="tablist">
<li role="presentation"><a href="#comparative-sac" data-toggle="tab" role="tab">SAC 2016 results (Sept. 2015)</a></li>
<li role="presentation" class="active"><a href="#comparative-latest" data-toggle="tab" role="tab">Latest results (Nov. 2015)</a></li>
</ul>
<div class="tab-content">
<div class="tab-pane" role="tabpanel" id="comparative-sac">

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th width="20%" rowspan="2">Component</th><th width="10%" rowspan="2"># Gold items</th><th width="30%" colspan="3">FRED</th><th width="30%" colspan="3">PIKES</th><th width="10%" rowspan="2">Alignments</th></tr>
<tr><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th></tr>
</thead>
<tbody>
<tr><td>Instances</td><td>136</td><td>.922</td><td>.868</td><td>.894</td><td>.930</td><td>.978</td><td>.953</td><td><a href="eval-pm/comparative-sac/comparative-sac.instances.html">view</a></td></tr>
<tr><td>Triples</td><td>167</td><td>.548</td><td>.413</td><td>.471</td><td>.711</td><td>.545</td><td>.617</td><td>(see below)</td></tr>
<tr><td style="padding-left: 2em">DBpedia links</td><td>18</td><td>.615</td><td>.444</td><td>.516</td><td>.684</td><td>.722</td><td>.703</td><td><a href="eval-pm/comparative-sac/comparative-sac.links.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (VN)</td><td>31</td><td>.593</td><td>.516</td><td>.552</td><td>.667</td><td>.581</td><td>.621</td><td><a href="eval-pm/comparative-sac/comparative-sac.types.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (FN)</td><td>28</td><td>.550</td><td>.393</td><td>.458</td><td>.762</td><td>.571</td><td>.653</td><td><a href="eval-pm/comparative-sac/comparative-sac.types.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (VN)</td><td>76</td><td>.558</td><td>.382</td><td>.453</td><td>.722</td><td>.513</td><td>.600</td><td><a href="eval-pm/comparative-sac/comparative-sac.roles.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em"><tt>owl:sameAs</tt></td><td>14</td><td>.357</td><td>.357</td><td>.357</td><td>.714</td><td>.357</td><td>.476</td><td><a href="eval-pm/comparative-sac/comparative-sac.sameas.html">view</a></td></tr>
<tr><td>Edges</td><td>155</td><td>.888</td><td>.561</td><td>.688</td><td>.937</td><td>.768</td><td>.844</td><td><a href="eval-pm/comparative-sac/comparative-sac.edges.html">view</a></td></tr>
</tbody>
</table>

</div>
<div class="tab-pane active" role="tabpanel" id="comparative-latest">

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th width="20%" rowspan="2">Component</th><th width="10%" rowspan="2"># Gold items</th><th width="30%" colspan="3">FRED</th><th width="30%" colspan="3">PIKES</th><th width="10%" rowspan="2">Alignments</th></tr>
<tr><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th></tr>
</thead>
<tbody>
<tr><td>Instances</td><td>136</td><td>.922</td><td>.868</td><td>.894</td><td>.904</td><td>.971</td><td>.936</td><td><a href="eval-pm/comparative/comparative.instances.html">view</a></td></tr>
<tr><td>Triples</td><td>167</td><td>.548</td><td>.413</td><td>.471</td><td>.693</td><td>.569</td><td>.625</td><td>(see below)</td></tr>
<tr><td style="padding-left: 2em">DBpedia links</td><td>18</td><td>.615</td><td>.444</td><td>.516</td><td>.684</td><td>.722</td><td>.703</td><td><a href="eval-pm/comparative/comparative.links.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (VN)</td><td>31</td><td>.593</td><td>.516</td><td>.552</td><td>.667</td><td>.581</td><td>.621</td><td><a href="eval-pm/comparative/comparative.types.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (FN)</td><td>28</td><td>.550</td><td>.393</td><td>.458</td><td>.613</td><td>.679</td><td>.644</td><td><a href="eval-pm/comparative/comparative.types.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (VN)</td><td>76</td><td>.558</td><td>.382</td><td>.453</td><td>.741</td><td>.526</td><td>.615</td><td><a href="eval-pm/comparative/comparative.roles.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em"><tt>owl:sameAs</tt></td><td>14</td><td>.357</td><td>.357</td><td>.357</td><td>.833</td><td>.357</td><td>.500</td><td><a href="eval-pm/comparative/comparative.sameas.html">view</a></td></tr>
<tr><td>Edges</td><td>155</td><td>.888</td><td>.561</td><td>.688</td><td>.917</td><td>.787</td><td>.847</td><td><a href="eval-pm/comparative/comparative.edges.html">view</a></td></tr>
</tbody>
</table>

</div>
</div>
</div>

In line with the approach of [\[1\]](#eswc2013), we also compare PIKES and FRED against an additional gold graph obtained by merging the outputs of both tools, cleaned up of incorrect triples.
By definition, this gold graph is a subset of the simplified gold standard discussed above. The goal of this additional evaluation, as noted in [\[1\]](#eswc2013), is to comparatively evaluate each tool within the knowledge extraction tool space (i.e., considering only correct triples that can be extracted by at least one tool).
The following table reports the results obtained (SAC 2016, latest). Again, PIKES exhibits better precision and recall than FRED for all the considered components, with differences in terms of F1 ranging from .060 to .269 for SAC 2016 results and from .042 to .333 for latest results.
The larger difference is due to PIKES recall being generally higher than FRED recall, which means that the gold graph here defined tends to coincide with the correct answers by PIKES (this can be seen as a limit of this kind of evaluation, which favors the system with higher recall).

<div>
<ul class="nav nav-tabs" role="tablist">
<li role="presentation"><a href="#comparative-merged-sac" data-toggle="tab" role="tab">SAC 2016 results (Sept. 2015)</a></li>
<li role="presentation" class="active"><a href="#comparative-merged-latest" data-toggle="tab" role="tab">Latest results (Nov. 2015)</a></li>
</ul>
<div class="tab-content">
<div class="tab-pane" role="tabpanel" id="comparative-merged-sac">

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th width="20%" rowspan="2">Component</th><th width="10%" rowspan="2"># Gold elements</th><th width="30%" colspan="3">FRED</th><th width="30%" colspan="3">PIKES</th><th width="10%" rowspan="2">Alignments</th></tr>
<tr><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th></tr>
</thead>
<tbody>
<tr><td>Instances</td><td>135</td><td>.922</td><td>.874</td><td>.897</td><td>.930</td><td>.985</td><td>.957</td><td><a href="eval-pm/comparative-sac/comparative-sac.instances.html">view</a></td></tr>
<tr><td>Triples</td><td>114</td><td>.548</td><td>.605</td><td>.575</td><td>.711</td><td>.798</td><td>.752</td><td>(see below)</td></tr>
<tr><td style="padding-left: 2em">DBpedia links</td><td>13</td><td>.615</td><td>.615</td><td>.615</td><td>.684</td><td>1</td><td>.813</td><td><a href="eval-pm/comparative-sac/comparative-sac.links.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (VN)</td><td>27</td><td>.593</td><td>.593</td><td>.593</td><td>.667</td><td>.667</td><td>.667</td><td><a href="eval-pm/comparative-sac/comparative-sac.types.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (FN)</td><td>17</td><td>.550</td><td>.647</td><td>.595</td><td>.762</td><td>.941</td><td>.842</td><td><a href="eval-pm/comparative-sac/comparative-sac.types.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (VN)</td><td>51</td><td>.558</td><td>.569</td><td>.563</td><td>.722</td><td>.765</td><td>.743</td><td><a href="eval-pm/comparative-sac/comparative-sac.roles.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em"><tt>owl:sameAs</tt></td><td>6</td><td>.357</td><td>.833</td><td>.500</td><td>.714</td><td>.833</td><td>.769</td><td><a href="eval-pm/comparative-sac/comparative-sac.sameas.html">view</a></td></tr>
<tr><td>Edges</td><td>135</td><td>.888</td><td>.644</td><td>.747</td><td>.937</td><td>.881</td><td>.908</td><td><a href="eval-pm/comparative-sac/comparative-sac.edges.html">view</a></td></tr>
</tbody>
</table>

</div>
<div class="tab-pane active" role="tabpanel" id="comparative-merged-latest">

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th width="20%" rowspan="2">Component</th><th width="10%" rowspan="2"># Gold elements</th><th width="30%" colspan="3">FRED</th><th width="30%" colspan="3">PIKES</th><th width="10%" rowspan="2">Alignments</th></tr>
<tr><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th><th width="10%">Precision</th><th width="10%">Recall</th><th width="10%">F1</th></tr>
</thead>
<tbody>
<tr><td>Instances</td><td>134</td><td>.922</td><td>.881</td><td>.901</td><td>.904</td><td>.985</td><td>.943</td><td><a href="eval-pm/comparative/comparative.instances.html">view</a></td></tr>
<tr><td>Triples</td><td>116</td><td>.548</td><td>.595</td><td>.570</td><td>.693</td><td>.819</td><td>.751</td><td>(see below)</td></tr>
<tr><td style="padding-left: 2em">DBpedia links</td><td>13</td><td>.615</td><td>.615</td><td>.615</td><td>.684</td><td>1</td><td>.813</td><td><a href="eval-pm/comparative/comparative.links.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (VN)</td><td>27</td><td>.593</td><td>.593</td><td>.593</td><td>.667</td><td>.667</td><td>.667</td><td><a href="eval-pm/comparative/comparative.types.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">types (FN)</td><td>19</td><td>.550</td><td>.579</td><td>.564</td><td>.613</td><td>1</td><td>.760</td><td><a href="eval-pm/comparative/comparative.types.fn.html">view</a></td></tr>
<tr><td style="padding-left: 2em">roles (VN)</td><td>51</td><td>.558</td><td>.569</td><td>.563</td><td>.741</td><td>.784</td><td>.762</td><td><a href="eval-pm/comparative/comparative.roles.vn.html">view</a></td></tr>
<tr><td style="padding-left: 2em"><tt>owl:sameAs</tt></td><td>6</td><td>.357</td><td>.833</td><td>.500</td><td>.833</td><td>.833</td><td>.833</td><td><a href="eval-pm/comparative/comparative.sameas.html">view</a></td></tr>
<tr><td>Edges</td><td>137</td><td>.888</td><td>.635</td><td>.740</td><td>.917</td><td>.891</td><td>.904</td><td><a href="eval-pm/comparative/comparative.edges.html">view</a></td></tr>
</tbody>
</table>

</div>
</div>
</div>


<h3>References</h3>

  1. <span id="eswc2013"/>
     **A Comparison of Knowledge Extraction Tools for the Semantic Web.**<br/>
     By Aldo Gangemi.<br/>
     In ESWC 2013 Proceedings, Springer Berlin Heidelberg, volume 7882, pages 351-366, 2013.<br/>
     [\[online version\]](http://eswc-conferences.org/sites/default/files/papers2013/gangemi.pdf)
  2. <span id="ekaw2012"/>
     **Knowledge Extraction Based on Discourse Representation Theory and Linguistic Frames.**<br/>
     By Valentina Presutti, Francesco Draicchio, Aldo Gangemi.<br/>
     In EKAW 2012 Proceedings, Springer-Verlag Berlin, pages 114-129, 2012.<br/>
     [\[online version\]](http://dx.doi.org/10.1007/978-3-642-33876-2_12)
     [\[web site\]](http://wit.istc.cnr.it/stlab-tools/fred)
