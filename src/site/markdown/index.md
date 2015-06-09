<div class="row">
<br/>
<br/>
<div class="span12">
<div class="well sidebar" style="text-align: center">
<h1 style="font-size:400%">PIKES</h1><br/>
<p style="font-size:200%">Pikes is a Knowledge Extraction Suite</p><br/>
<form method="GET" action="https://knowledgestore2.fbk.eu/pikes-demo/">
    <button class="btn btn-primary btn-large" type="submit" style="font-size:150%">Demo</button>
</form>
</div>
</div>
</div>

---------------------------------------

### About

**PIKES** is a Java-based suite that extracts knowledge from textual resources.
The tool implements a rule-based strategy that reinterprets the output of semantic role labelling (SRL) tools in light
of other linguistic analyses, such as dependency parsing or co-reference resolution, thus properly capturing and
formalizing in RDF important linguistic aspects such as argument nominalization, frame-frame relations, and group
entities.

### Features

- Argument nominalization using semantic role labelling
- Frame-frame relations
- Entity grouping exploiting entity linking and co-reference
- Extensible and replaceable NLP pipeline
- Interlinked three-layer representation model exposed as RDF
- Instance RDF triples annotated with detailed information of the mentions (via named graph)
- REST API service included, built on top of [Grizzly](https://grizzly.java.net/)
- Based on [Java 8](http://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html) and [RDFpro](http://rdfpro.fbk.eu/)
- open source software ([GNU General Public License](http://www.gnu.org/licenses/gpl-3.0.html))

### News

- 2015-06-08 Version 0.1 has been released.