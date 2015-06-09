Simple English Wikipedia RDF
===

We applied PIKES to process a general domain corpus, namely a dump of the Simple English Wikipedia (SEW).
SEW is a reduced version of Wikipedia, where each page is written using basic English words. The corpus consist of
109,242 text document, containing a total of 1,584,406 sentences and 23,877,597 tokens.
The choice of this corpus was made for several reasons: it is a relatively large, general domain, and publicly
available corpus, and it is aligned with DBpedia (SEW → Wikipedia → DBpedia).

PIKES processed the whole SEW corpus in ∼507 core hours, with an average of 1.2s per sentence and 16.7s per document.
We used 16 parallel instances of PIKES on the same machine, ending the whole processing in less than 32 hours.

In this page, we release the corresponding datasets.

* [Original Simple English Wikipedia (SEW) XML dump](https://knowledgestore.fbk.eu/files/pikes/simplewiki-20150406-pages-articles.xml.bz2)
* [Original SEW plain text, parsed with PIKES](https://knowledgestore.fbk.eu/files/pikes/simplewiki-20150406-pages-articles.txt.tar.gz)
* [SEW in RDF format](https://knowledgestore.fbk.eu/files/pikes/simplewiki-20150406-pages-articles.tql.gz)

