Evaluation using Simple English Wikipedia
===


### Overview

We applied PIKES to process a general domain corpus, namely a dump of the Simple English Wikipedia (SEW).
SEW is a reduced version of Wikipedia, where each page is written using basic English words.
The corpus, which consists of 109,242 text document containing a total of 1,584,406 sentences and 23,877,597 tokens, was chosen for its relatively large size and public availability.

The processing of SEW with PIKES, the evaluation we conducted on the result and corresponding findings are available in [PIKES reference paper](publications.html).
Here we briefly summarize them and make available all the datasets and the evaluation material involved.


### Processing

PIKES processed the whole SEW corpus in ∼507 core hours, with an average of 1.2s per sentence and 16.7s per document.
Using 16 parallel instances of PIKES on the same machine, we ended the whole processing in less than 32 hours.
A total of 357,853,792 triples were produced.

All the input and output datasets are available for download:

* [Original Simple English Wikipedia (SEW) XML dump](https://knowledgestore.fbk.eu/files/pikes/simplewiki-20150406-pages-articles.xml.bz2)
* [Original SEW plain text, parsed with PIKES](https://knowledgestore.fbk.eu/files/pikes/simplewiki-20150406-pages-articles.txt.tar.gz)
* [SEW in RDF format](https://knowledgestore.fbk.eu/files/pikes/simplewiki-20150406-pages-articles.tql.gz)


### Evaluation

To evaluate the quality of the produced knowledge graph, we manually checked a subset of the extracted triples (the ones involving DBpedia entities) - an approach adopted for other knowledge graphs such as [YAGO2](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/yago/).

We randomly sampled 200 triples from the graph involving DBpedia entities, focusing on annotation and name assertions (35 triples), type assertions (35), and PropBank/NomBank frame participations (130).
For each triple, we randomly selected one of the mentions from where the triple was extracted.
The evaluation dataset was provided to three evaluators that were asked to judge if each triple produced by PIKES is actually compatible with the knowledge conveyed by the given mention.
Evaluators were allowed to use three values: 1 - correct, 0.5 - partly correct, and 0 - not correct.
Accuracy was computed for each assertion type, and for each evaluator.

All the evaluation material is available for download:

* [DBpedia entities selected for the evaluation](https://knowledgestore.fbk.eu/files/pikes/eval/dpb-entities-eval.pdf)
* [Plain text of the selected pages](https://knowledgestore.fbk.eu/files/pikes/eval/txt-eval.tar.gz)
* [Output of PIKES (RDF)](https://knowledgestore.fbk.eu/files/pikes/eval/dataset.virtuoso.tql.gz)
* [Evaluation Dataset and Questionnaire](https://knowledgestore.fbk.eu/files/pikes/eval/eval-ds-and-q.pdf)
* [Results and Inter-annotator Agreement](https://knowledgestore.fbk.eu/files/pikes/eval/eval-results.pdf)
