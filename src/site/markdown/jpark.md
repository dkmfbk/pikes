
<div style="text-align: center;">
<img src="images/jpark_logo.png" alt="JPARK logo" width="250"/>
</div>

Joint Posterior Revision of NLP Annotations via Ontological Knowledge
===

This page provides additional details on __JPARK__, an ontological knowledge powered probabilistic approach for jointly
revising multiple NLP entity annotations. 

The proposed approach is fully implemented and evaluated in the following paper:

  * **Joint Posterior Revision of NLP Annotations via Ontological Knowledge**<br/>
    By Marco Rospocher and Francesco Corcoglioniti.<br/>
    In Proceedings of the 27th International Joint Conference on Artificial Intelligence and the 23rd European Conference on Artificial Intelligence, IJCAI-ECAI 2018, Stockholm, Sweden, July 13-19, 2018<br/>
    [\[bib\]](https://dkm-static.fbk.eu/people/rospocher/bibtexbrowser.php?key=2018ijcai&amp;bib=my_pub.bib)
    [\[pre-print/mirror\]](https://dkm-static.fbk.eu/people/rospocher/files/pubs/2018ijcai.pdf)

__JPARK__ has been evaluated on three reference datasets for Named Entity Recognition and Classification (NERC) and Entity Linking (EL):

  * [AIDA CoNLL-YAGO](https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/aida/downloads/): 
  This dataset consists of 1,393 English news wire articles from Reuters, with 34,999 mentions 
  hand-annotated with named entity types (PER, ORG, LOC, MISC) for the [CONLL2003](https://www.clips.uantwerpen.be/conll2003/ner/) 
  shared task on 
  named entity recognition, and later hand-annotated with the [YAGO2](http://www.yago-knowledge.org/) entities and corresponding 
  [Wikipedia](https://en.wikipedia.org/) page URLs. It is split in three parts: eng.train (946 docs), eng.testa (216 docs), 
  eng.testb (231 docs).
  * [MEANTIME](http://www.newsreader-project.eu/results/data/wikinews/): The NewsReader MEANTIME 
  corpus consists of 480 news articles from [Wikinews](https://en.wikinews.org/), in four languages. 
  In our evaluation, we 
  used only the English section and its 120 articles. The dataset, used as part of the [SemEval 
  2015 task on TimeLine extraction](http://alt.qcri.org/semeval2015/task4/), includes manual annotations for named entity types 
  (only PER, ORG, LOC) and DBpedia entity links.
  * [TAC-KBP](https://tac.nist.gov/2011/KBP/): Developed for the TAC KBP 2011 Knowledge Base 
  Population Track, this dataset consists of 2,231 English documents, including newswire articles 
  and posts to blogs, newsgroups, and discussion fora. For each document, it is known that all the 
  mentions of one or a few query entities can be linked to a certain [Wikipedia](https://en.wikipedia.org/) page and to a specific 
  NERC type (only PER, ORG, LOC), giving rise to a (partially) annotated gold standard for NERC and EL.
  
The following __JPARK__ resources used in the IJCAI-18 paper are made available:

  * [TSV](https://knowledgestore.fbk.eu/files/jpark/IJCAI2018model.tsv.gz) (~39MB) containing the model (used in the IJCAI-18 experiments) built with [YAGO](http://www.yago-knowledge.org/) as background knowledge, and trained on AIDA CoNLL-YAGO (eng.train). Its columns contain:
    1. a YAGO Class Set (classes in the set are space separated)
    2. the conditional probability --- cf. eq. (6) in the paper --- of having that class set given a NERC PER annotation
    3. the conditional probability --- cf. eq. (6) in the paper --- of having that class set given a NERC ORG annotation
    4. the conditional probability --- cf. eq. (6) in the paper --- of having that class set given a NERC LOC annotation
    5. the conditional probability --- cf. eq. (6) in the paper --- of having that class set given a NERC MISC annotation
    6. the prior probability of that class set estimated from the ontological background knowledge --- cf. eq. (7) in the paper
    7. all the entities (space separated) having as types exactly the classes in that class set
  * [PDF](https://knowledgestore.fbk.eu/files/jpark/IJCAI2018addendum.pdf) (~83KB) file containing all evaluation metrics computed for all measures of IJCAI-18 paper, with and without using __JPARK__, by
    * micro-averaging, considering only mentions in the gold standard;
    * micro-averaging, considering all mentions returned by the system;
    * macro-averaging by document;
    * macro-averaging by NERC type.
  
  * [FOLDER](https://knowledgestore.fbk.eu/files/jpark/IJCAI2018evaluation-package.zip) (~985KB) package of the IJCAI-18 evaluation folder, containing:
    * the official [TAC scorer](https://github.com/wikilinks/neleval);
    * commands for computing scores (and statistical significance) for all metrics and measures considered (cf. the paper for details on interpreting the values);
    * gold, standard, and __JPARK__ annotations for all datasets (excluding TAC-KBP, under LDC copyright). 
    
<br/>    
### Additional evaluation material (manuscript describing the new developments and experiments currently under review):
  
  * __JPARK__ models:
    * [TSV](https://knowledgestore.fbk.eu/files/jpark-ext/JPARK-yago_model.tsv.gz) (~37MB) containing the model (the same used in the IJCAI-18 experiments) built with [YAGO](http://www.yago-knowledge.org/) as background knowledge, and trained on AIDA CoNLL-YAGO (eng.train).
    * [TSV](https://knowledgestore.fbk.eu/files/jpark-ext/JPARK-dbo_model.tsv.gz) (~29MB) containing the model (NOT used in the IJCAI-18 experiments) built with [DBpedia Ontology](https://wiki.dbpedia.org/services-resources/ontology/) as background knowledge, and trained on AIDA CoNLL-YAGO (eng.train). It contains the same information as the YAGO model, but with DBpedia Ontology class sets instead. 
    * [TSV](https://knowledgestore.fbk.eu/files/jpark-ext/JPARK-wikidata_model.tsv.gz) (~20MB) containing the model (NOT used in the IJCAI-18 experiments) built with [Wikidata](https://www.wikidata.org/) as background knowledge, and trained on AIDA CoNLL-YAGO (eng.train). It contains the same information as the YAGO model, but with Wikidata class sets instead.
  * [TSV](https://knowledgestore.fbk.eu/files/jpark-ext/NILpriors.tsv.gz) (~94B) NIL Priors, trained on AIDA CoNLL-YAGO (eng.train), for the different NERC categories
  * [LINK](http://pikes.fbk.eu/download.html) to download the PIKES binary and models
  * NAF files annotated (also with NIL confidences) with PIKES (as the PIKES annotated files contain the whole text, due to copyright restrictions, only the MEANTIME annotated files can be made available)
    * [FOLDER](https://knowledgestore.fbk.eu/files/jpark-ext/MEANTIME-PIKES-NAFs.tgz) (~10MB), with Stanford NER and DBpedia Spotlight independently spotting named entities
    * [FOLDER](https://knowledgestore.fbk.eu/files/jpark-ext/MEANTIME-PIKES-NAFs_SAME.tgz) (~7MB), using the Stanford NER spotter also for DBpedia Spotlight
  * [JAR](https://knowledgestore.fbk.eu/files/jpark-ext/jpark.jar.gz) (~64MB) JAR binary of __JPARK__
  * [FOLDER](https://knowledgestore.fbk.eu/files/jpark-ext/evaluation.tgz) (~3MB) package of the evaluation folder, containing:
    * the official [TAC scorer](https://github.com/wikilinks/neleval);
    * commands for computing scores (and statistical significance) for all metrics and measures considered;
    * gold standard, baselins, and __JPARK__ (with and without NIL extension) annotations for all datasets (excluding TAC-KBP, under LDC copyright).
  * Full evaluation results (with baseline and __JPARK__ scores, as well as p-values):
    * [TXT](https://knowledgestore.fbk.eu/files/jpark-ext/results.txt): using separate named entity spotters for NERC and EL;
    * [TXT](https://knowledgestore.fbk.eu/files/jpark-ext/results_samespoting.txt): using same named entity spotter (Stanford NER) for NERC and EL;
    * [TXT](https://knowledgestore.fbk.eu/files/jpark-ext/results_upperbounds.txt): performance upper bounds for the posterior revision of the annotations. 