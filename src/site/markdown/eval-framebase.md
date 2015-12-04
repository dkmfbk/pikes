Evaluation using FrameBase TBox
===


<h3 id="overview">Overview</h3>

We evaluate the performances of PIKES as an ontology population approach for the [FrameBase](http://framebase.org/) ontological schema, reporting precision and recall against a manually annotated gold standard based on the text used in [\[1\]](#eswc2013) .
We experiment with three different configurations of the linguistic feature extraction phase that differ for the Semantic Role Labeling (SRL) tools used:

  * [Semafor](http://www.cs.cmu.edu/~ark/SEMAFOR/) only, performing SRL w.r.t. [FrameNet](https://framenet.icsi.berkeley.edu/fndrupal/);
  * [Mate-tools](https://code.google.com/p/mate-tools/) only, performing SRL w.r.t. [PropBank](https://verbs.colorado.edu/~mpalmer/projects/ace.html) and [NomBank](http://nlp.cs.nyu.edu/meyers/NomBank.html); and,
  * both Semafor and Mate-tools, relying on the automatic combinations of the respective annotations in the mention graph.


<h3 id="data">Sentences and graphs</h3>

The following table lists the sentences of the gold standard used for the evaluation, each one associated to four knowledge graphs containing type and role triples using the FrameBase vocabulary: a gold graph manually built by two annotators, and three graphs produced by PIKES for the three configurations considered.

In order to simplify the manual construction of gold graphs, the link between an instance in a gold graph and the corresponding mention is implicit and given by the instance URI, whose local name corresponds to the head token of the mention in the text. In case of ambiguities, i.e., if there are multiple occurrences of a word in the sentence, a sequential index is added (e.g., in sentence S7, `:syria_1` and `:syria_2` refer respectively to the first and second occurrences of Syria).

PIKES graphs were obtained using the public demo of [PIKES](https://knowledgestore2.fbk.eu/pikes-demo/), converting the output from TriG to Turtle to get rid of provenance information not needed for this evaluation.

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th rowspan="2">Sentence</th><th rowspan="2">Text</th><th rowspan="2" width="55px">Gold graph</th><th colspan="3">PIKES graphs</th></tr>
<tr><th width="55px">Semafor</th><th width="55px">Mate</th><th width="55px">Both</th></tr>
</thead>
<tbody>
<tr>
<td>S1</td>
<td>The lone Syrian rebel group with an explicit stamp of approval from Al Qaeda has become one of the uprising most effective fighting forces, posing a stark challenge to the United States and other countries that want to support the rebels but not Islamic extremists.</td>
<td><a href="eval-framebase/gold/gold.1.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.1.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.1.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.1.ttl">.ttl</a></td>
</tr>
<tr>
<td>S2</td>
<td>Money flows to the group, the Nusra Front, from like-minded donors abroad.</td>
<td><a href="eval-framebase/gold/gold.2.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.2.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.2.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.2.ttl">.ttl</a></td>
</tr>
<tr>
<td>S3</td>
<td>Its fighters, a small minority of the rebels, have the boldness and skill to storm fortified positions and lead other battalions to capture military bases and oil fields.</td>
<td><a href="eval-framebase/gold/gold.3.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.3.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.3.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.3.ttl">.ttl</a></td>
</tr>
<tr>
<td>S4</td>
<td>As their successes mount, they gather more weapons and attract more fighters.</td>
<td><a href="eval-framebase/gold/gold.4.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.4.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.4.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.4.ttl">.ttl</a></td>
</tr>
<tr>
<td>S5</td>
<td>The group is a direct offshoot of Al Qaeda in Iraq, Iraqi officials and former Iraqi insurgents say, which has contributed veteran fighters and weapons.</td>
<td><a href="eval-framebase/gold/gold.5.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.5.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.5.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.5.ttl">.ttl</a></td>
</tr>
<tr>
<td>S6</td>
<td>This is just a simple way of returning the favor to our Syrian brothers that fought with us on the lands of Iraq, said a veteran of Al Qaeda in Iraq, who said he helped lead the Nusra Front's efforts in Syria.</td>
<td><a href="eval-framebase/gold/gold.6.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.6.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.6.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.6.ttl">.ttl</a></td>
</tr>
<tr>
<td>S7</td>
<td>The United States, sensing that time may be running out for Syria president Bashar al-Assad, hopes to isolate the group to prevent it from inheriting Syria.</td>
<td><a href="eval-framebase/gold/gold.7.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.7.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.7.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.7.ttl">.ttl</a></td>
</tr>
<tr>
<td>S8</td>
<td>As the United States pushes the Syrian opposition to organize a viable alternative government, it plans to blacklist the Nusra Front as a terrorist organization, making it illegal for Americans to have financial dealings with the group and prompting similar sanctions from Europe.</td>
<td><a href="eval-framebase/gold/gold.8.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-semafor/pikes-semafor.8.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-mate/pikes-mate.8.ttl">.ttl</a></td>
<td><a href="eval-framebase/pikes-both/pikes-both.8.ttl">.ttl</a></td>
</tr>
</tbody>
</table>


<h3 id="results">Evaluation results</h3>

The table below reports the results of the evaluation for the three configuration, both against FrameBase type triples for frame instances (first three columns), FrameBase role triples (next three columns), and all triples undifferentiated (next three columns).

As one could expect, F1 scores using Mate-tools are lower than the ones obtained using Semafor, reflecting the fact that the latter, being specifically designed for FrameNet SRL, is more suitable for use with FrameBase. However, the combination of both tools in PIKES leads to an increase of recall for role triples with respect to Semafor (or Mate-tools) alone. Note that precision scores for Mate-tools are on par with the ones for Semafor, with a gap in
terms of recall that could be potentially addressed with further work on PropBank/NomBank to FrameBase mapping resources.

<table class="table table-striped table-bordered table-hover table-condensed">
<thead>
<tr><th width="12%" rowspan="2">Configuration</th><th colspan="3">Type triples</th><th colspan="3">Role triples</th><th colspan="3">All triples</th></tr>
<tr>
<th width="8%">Precision</th><th width="8%">Recall</th><th width="8%">F1</th>
<th width="8%">Precision</th><th width="8%">Recall</th><th width="8%">F1</th>
<th width="8%">Precision</th><th width="8%">Recall</th><th width="8%">F1</th>
</tr>
</thead>
<tbody>
<tr>
<td>Semafor</td>
<td>.617</td><td>.698</td><td>.655</td>
<td>.594</td><td>.352</td><td>.442</td>
<td>.605</td><td>.466</td><td>.526</td>
</tr>
<tr>
<td>Mate</td>
<td>.792</td><td>.358</td><td>.494</td>
<td>.633</td><td>.176</td><td>.275</td>
<td>.704</td><td>.236</td><td>.353</td>
</tr>
<tr>
<td>Both</td>
<td>.603</td><td>.717</td><td>.655</td>
<td>.595</td><td>.435</td><td>.503</td>
<td>.599</td><td>.528</td><td>.561</td>
</tr>
</tbody>
</table>


<h3>References</h3>

  1. <span id="eswc2013"/>
     **A Comparison of Knowledge Extraction Tools for the Semantic Web.**<br/>
     By Aldo Gangemi.<br/>
     In ESWC 2013 Proceedings, Springer Berlin Heidelberg, volume 7882, pages 351-366, 2013.<br/>
     [\[online version\]](http://eswc-conferences.org/sites/default/files/papers2013/gangemi.pdf)
