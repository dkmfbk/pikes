Using PIKES for Information Retrieval
===

This page provides additional details on the use of Knowledge Extraction techniques, as implemented in PIKES, to improve the performances of [Information Retrieval](https://en.wikipedia.org/wiki/Information_retrieval). The proposed approach is fully implemented in the following paper: 
  
  * **Knowledge Extraction for Information Retrieval**<br/>
    By Francesco Corcoglioniti, Mauro Dragoni, Marco Rospocher, and Alessio Palmero Aprosio.<br/>
    In Proc. of 13th Extended Semantic Web Conference (ESWC 2015), Heraklion, Crete, Greece, 2015 (to appear)<br/>
    [\[bib\]](https://dkm-static.fbk.eu/people/rospocher/bibtexbrowser.php?key=2015iswcdemo1&amp;bib=my_pub.bib)
    [\[pre-print/mirror\]](https://dkm-static.fbk.eu/people/rospocher/files/pubs/2015iswcdemo1.pdf)

Here, we provide a brief overview of the approach, make available for downlaod all the code and data used in the evaluation (so to allow reproducing our results), and provide all the reports and the additional material we produced as part of the evaluation.


## Approach

The goal in Information Retrieval is to determine, for a given text query, the relevant documents in a text collection, ranking them according to their relevance degree for the query.

In our approach, named __KE4IR__ (read: _kee-fer_) and implemented on top of PIKES and Apache Lucene, both queries and documents are processed to extract semantic terms pertaining to the following semantic layers:

  * __URI layer__, containing URIs of entities mentioned in the text, disambiguated against [DBpedia](http://dbpedia.org/);
  * __TYPE layer__, containing [YAGO](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/yago/) classes associated to noun phrases in the text, extracted via disambiguation to [WordNet](https://wordnet.princeton.edu/) (plus WordNet to YAGO mappings) or imported from DBpedia after entity linking;
  * __TIME layer__, containing temporal values explicitly expressed in the text, or imported from DBpedia after entity linking;
  * __FRAME layer__, containing <frame type, participant> pairs where the frame results from knowledge extraction and the participant is a disambiguated entity.

We adopt a retrieval model inspired to the [Vector Space Model (VSM)](https://en.wikipedia.org/wiki/Vector_space_model). We represent both documents and queries as term vectors whose elements are the weight of textual and semantic terms, computed based on [TF / IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf) values opportunely extended for the use with semantic information, plus a weight that allows assigning different importances to distinct layers (in our experiments, we use a setup where textual and semantic information contribute equally). The similarity score between a document and a query is computed via the scalar product of their vectors, and is used to identify and rank relevant document.


## Implementation

We built an evaluation infrastructure that implements the KE4IR approach and allows applying it on arbitrary documents and queries, measuring retrieval performances against gold relevance judgments.

The infrastructure requires in input the NLP annotations and the knowledge graphs extracted with PIKES for both documents and queries of the test dataset, plus an index containing background knowledge that will be injected into the knowledge graphs prior to their processing.
For each document or query, textual terms are extracted starting from the tokenization in the NLP annotations, while semantic terms are extracted from the knowledge graph.
Document terms are indexed in a Lucene inverted index.
At search time, query terms are OR-ed in a Lucene query that locates documents containing at least one term. Then, matched documents are scored and ranked externally to Lucene (for ease of testing) according to KE4IR retrieval model.
The resulting document ranking is compared with the gold relevance judgments to compute a comprehensive set of evaluation metrics, which are averaged along different queries.

We provide the evaluation infrastructure as a separate [GitHub project](http://github.com/dkmfbk/ke4ir-evaluation). A precompiled version is available [http](https://knowledgestore.fbk.eu/files/ke4ir/ke4ir.tar.gz).
Please refer to the documentation on the GitHub project for specific instructions on how to load a dataset and run the code.


## Evaluation

KE4IR has been validated on the ad-hoc IR task, consisting in performing a set of queries over a document collection for which the list of relevance judgments is available, comparing for each query the document rankings produced by KE4IR with the gold rankings.
We adopted the document collection from [http://s16a.org/node/14](http://s16a.org/node/14), described in the paper:

Waitelonis, J., Exeler, C., Sack, H.: Linked Data enabled generalized vector space
model to improve document retrieval

  * **Linked Data enabled Generalized Vector Space Model to improve document retrieval**<br/>
    By Jörg Waitelonis and Claudia Exeler and Harald Sack.<br/>
    In Proc. of NLP & DBpedia 2015 workshop in conjunction with 14th International Semantic Web Conference (ISWC2015), 2015, CEUR Workshop Proceedings<br/>
    [\[pdf\]](https://nlpdbpedia2015.files.wordpress.com/2015/08/nlpdbpedia_2015_submission_7.pdf)
    [\[slides\]](https://nlpdbpedia2015.files.wordpress.com/2015/08/iswc2015-nlpdbpedia-gvsm.pdf)

The collection is composed of 331 documents from a web blog and 35 queries. The relevance of each document is expressed in a multi-value scale with scores going from 5 (the document contains exact information with respect to what the user is looking for) to 1 (the document is of no interest for the query). Document scored 2 to 5 are thus relevant, while document not scored or scored 1 are irrelevant. For NDCG, we directly used the score as is (range 2-5) for computing the metric. 

The raw texts distributed in the dataset at [http://s16a.org/node/14](http://s16a.org/node/14) misses some pieces of text (corresponding to links and other special markup in original HTML pages). We fixed this issue by downloading and converting again the original HTML texts. The obtained texts, together with the queries and the gold relevance judgements from [http://s16a.org/node/14](http://s16a.org/node/14), are all made available here for download using the data formats supported by our evaluation infrastructure ([NAF]() for texts, TSV for relevance judgements); data in the NIF format, including manual entity linking annotations not used here, is available on the original website.

Below we provide all the input, output and intermediate data involved in the evaluation, divided in multiple files for convenience:

  * Dataset files
    * fixed raw texts (NAF format) - [docs-raw-texts.zip](docs-raw-texts.zip) (XXX MB), [queries-raw-texts.zip](queries-raw-texts.zip) (XXX MB)
    * relevance judgments - [relevance-judgments.tsv](relevance-judgments.tsv) (2 KB)
    * pre-populated SOLR instance - [solr.zip](solr.zip) (XXX MB)

  * Knowledge extraction (PIKES) and enrichment results
    * NLP annotations produced with PIKES (NAF format) - [docs-nlp-annotations.zip](docs-nlp-annotations.zip) (33 MB), [queries-nlp-annotations.zip](queries-nlp-annotations.zip) (68 KB)
    * knowledge graphs extracted with PIKES, not enriched (TQL format) - [docs-rdf.zip](docs-rdf.zip) (XXX MB), [queries-rdf.zip](queries-rdf.zip) (XXX MB)
    * knowledge graphs extracted with PIKES, enriched with background knowledge (TQL format) - [docs-rdf-enriched.zip](docs-rdf-enriched.zip) (174 MB), [queries-rdf-enriched.zip](queries-rdf-enriched.zip) (272 KB)
    * background knowledge index (custom key-value binary format) - [bk.zip](bk.zip) (XXX MB)    

  * Term extraction results
    * textual and semantic terms extracted with KE4IR (TSV format) - [docs-extracted-terms.tsv.gz](docs-extracted-terms.tsv.gz) (3 MB), [queries-extracted-terms.tsv.gz](queries-extracted-terms.tsv.gz) (12 KB)
    * lucene index built using textual and semantic terms of documents - [lucene_index.zip](lucene_index.zip) (6 MB)
    
  * Evaluation results
    * CSV files generated by the evaluation infrastructure (redundant data, included because more easily processable for further analyses) - [results_raw.zip](results_raw.zip) (180 KB) 
    * spreadsheet with performances aggregated over queries, using different layer combinations (data related to Table 3) - [results_aggregates.zip](results_aggregates.zip) (60 KB) 
    * spreadsheet with performances (and top 10 results with their scores) of each query, for each significant layer combination (data related to Table 4) - [results_queries.zip](results_queries.zip) (292 KB)
    * spreadsheet with rankings returned by each query considering one semantic layer at a time, with scores obtained before applying layer weight w(l(t)); data not used in the paper but possibly useful to further compare layers' performances - [results_rankings.zip](results_rankings.zip) (212 KB) 
    * CSV file with performances averaged over all queries, measured by varying the total weight assigned to semantic layers in 0.01 steps (data related to Figure 2) - [results_semantic_weight_analysis.csv](results_semantic_weight_analysis.csv) (576 KB)
  
If all you want is to reproduce our results as quickly as possible, we provide a zipped folder with all the required data (subset of the items listed above): [ke4ir_evaluation.zip] (XXX MB)

Instructions:

  * download and extract the [tarball file]([ke4ir_evaluation.zip]) containing the data folder
  * download the [precompiled binaries](https://knowledgestore.fbk.eu/files/ke4ir/ke4ir.tar.gz) of the evaluation infrastructure
  * in a shell, execute `ke4ir-eval /path/to/data/folder/ke4ir.properties`
    
Requirements:

  * a Linux /Mac OS X / Unix-like machine
  * Java 8

Note: the data folder contains the relevance judgments, the NLP annotations of documents and queries, and the knowledge graphs of documents and queries already enriched with background knowledge, so to avoid shipping the (very large) background knowledge index. Document and query terms, the lucene index, and the CSV report files are not included as they are generated by running the command above (the spreadsheets were built manually from the CSV reports, while the CSV file with the semantic weight analysis was produced calling the command multiple times with some script hack).
