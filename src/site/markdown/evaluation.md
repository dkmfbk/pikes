Evaluation (May 2015)
===

Evaluating KE tools is a challenging task, especially when the goal is to extract all the knowledge conveyed by a text.
No gold standard to compare with is available as basically there is not a canonical proper way to formalize, in a
Semantic Web representation, the knowledge of a text: different modeling choices can be taken for different
applications, domains, requirements, etc.

We conducted the evaluation on a subset of pages from the Simple English Wikipedia, investigating how good is our
approach in extracting triples about the entities corresponding to these pages.
That is, we used a real corpus, made independently from the task, on which no pre-processing for cleaning the text
documents was performed (besides removing the wiki markups, see the [resource page](sew-rdf.html)).

* [DBpedia entities selected for the evaluation](https://knowledgestore.fbk.eu/files/pikes/eval/dpb-entities-eval.pdf)
* [Plain text of the selected pages](https://knowledgestore.fbk.eu/files/pikes/eval/txt-eval.tar.gz)
* [Output of PIKES (RDF)](https://knowledgestore.fbk.eu/files/pikes/eval/dataset.virtuoso.tql.gz)
* [Evaluation Dataset and Questionnaire](https://knowledgestore.fbk.eu/files/pikes/eval/eval-ds-and-q.pdf)
* [Results and Inter-annotator Agreement](https://knowledgestore.fbk.eu/files/pikes/eval/eval-results.pdf)
