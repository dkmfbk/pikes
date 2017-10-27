# Getting PIKES

- [Download](#download)
- [Running PIKES on GNU/Linux (Interactive Mode)](#inter)
- [Running PIKES on GNU/Linux (Batch Processing Mode)](#batch)
- [Running PIKES on a Mac OS X](#mac)
- [Recompiling PIKES from sources](#sources)

<br/>
<br/>
## <a class="anchor" id="download"></a>Download
<br/>
PIKES works out-of-the-box on GNU/Linux machines (tested on Debian, Ubuntu and Red Hat). It works also on Mac OS X,
but the UKB module (word sense disambiguation) should be installed separately (see below).

The software needs Java 1.8 and at least 8GB of RAM (better 12G) for the models.

We provide a single [full package](https://knowledgestore.fbk.eu/files/pikes/download/pikes-all.tar.gz), containing all modules, models, and configurations needed to run PIKES straightaway.
The package includes:

* [PIKES Java library](https://knowledgestore.fbk.eu/files/pikes/download/pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar) which includes, among other Java libraries and models from:
    * [Stanford CoreNLP](http://stanfordnlp.github.io/CoreNLP/) - a popular set of human language technology tools (including part-of-speech tagger, named entity recognizer, (dependency) parser, coreference resolution system, etc.).
[(Source code)](https://github.com/stanfordnlp/CoreNLP)
[(License)](https://stanfordnlp.github.io/CoreNLP/#license)
    * [Semafor](http://www.cs.cmu.edu/~ark/SEMAFOR/) - a tool for automatic analysis of the frame-semantic structure of English text.
[(Source code)](https://github.com/Noahs-ARK/semafor)
[(License)](https://github.com/Noahs-ARK/semafor/blob/master/LICENSE)
    * [Mate-tools](https://code.google.com/archive/p/mate-tools/) - a pipeline of modules that carry out lemmatization, part-of-speech tagging, dependency parsing, and semantic role labeling of a sentence.
[(Source code)](https://code.google.com/archive/p/mate-tools/source/default/source)
[(License)](https://code.google.com/archive/p/mate-tools/)
    * [PredicateMatrix](http://adimen.si.ehu.es/web/PredicateMatrix) - a lexical resource integrating multiple sources of predicate information. 
[(License)](http://adimen.si.ehu.es/web/PredicateMatrix)
 
* [WordNet 3.0](https://knowledgestore.fbk.eu/files/pikes/download/wordnet.tar.gz) - a large lexical database of English developed at Princeton University. Nouns, verbs, adjectives and adverbs are grouped into sets of cognitive synonyms (synsets), each expressing a distinct concept. Synsets are interlinked by means of conceptual-semantic and lexical relations.
[(More info)](https://wordnet.princeton.edu/wordnet/)
[(License)](https://wordnet.princeton.edu/wordnet/license/)
 
* [UKB](https://knowledgestore.fbk.eu/files/pikes/download/ukb.tar.gz) - a collection of programs for performing graph-based Word Sense Disambiguation and lexical similarity/relatedness using a pre-existing knowledge base.
[(More info)](http://ixa2.si.ehu.es/ukb/)
[(Source code)](https://github.com/asoroa/ukb)
[(License)](https://github.com/asoroa/ukb/blob/master/src/LICENSE)

Execute the following commands on a Bash shell to download and extract PIKES full package:

```
wget https://knowledgestore.fbk.eu/files/pikes/download/pikes-all.tar.gz # Download the full package
tar xzf pikes-all.tar.gz
cd pikes
```
<br/> 
<br/> 
## <a class="anchor" id="inter"></a>Running PIKES on GNU/Linux (Interactive Mode)
<br/>
If you want to run PIKES on GNU/Linux in Interactive Mode, just execute the `run.sh` script. After a minute, PIKES should be active on port 8011 (you can change the port modifying the PORT variable in the `run.sh` script file).
 

Various API methods are available:
<br/>
#### `text2naf`
Given some text, this returns a [NAF](http://www.newsreader-project.eu/files/2013/01/techreport.pdf) file containing the linguistic annotations produced by the annotators used in PIKES. Call example (`server` is the name of the server hosting PIKES - e.g. `localhost`):

```
http://server:8011/text2naf?text=G.%20W.%20Bush%20and%20Bono%20are%20very%20strong%20supporters%20of%20the%20fight%20of%20HIV%20in%20Africa.%20Their%20March%202002%20meeting%20resulted%20in%20a%205%20billion%20dollar%20aid.
```

<br/>
#### `text2rdf`
Given some text, this returns the [RDF](https://www.w3.org/RDF/) content (in [TriG](https://www.w3.org/TR/trig/) format) extracted by PIKES from the given text. Call example:

```
http://server:8011/text2rdf?text=G.%20W.%20Bush%20and%20Bono%20are%20very%20strong%20supporters%20of%20the%20fight%20of%20HIV%20in%20Africa.%20Their%20March%202002%20meeting%20resulted%20in%20a%205%20billion%20dollar%20aid.
```

<br/>
#### `webdemo`
PIKES comes with a web interface (such as the web demo available on the [PIKES web site](https://knowledgestore2.fbk.eu/pikes-demo/));
you need [graphviz](http://www.graphviz.org/) to be installed on the server to run it.
With Debian/Ubuntu, just run `apt-get install graphviz` and restart PIKES.
The demo interface (with input textbox for text) is written in php and available under the `src/webdemo/` folder in the project.
To access it, just surf to

```
http://server:8011/webdemo
```

<br/><br/>
PIKES execution and processing (e.g., which annotators are used) is configurable via a property file. See e.g. the `config-pikes.prop` file included in the PIKES package.
If you want to pass Stanford CoreNLP configurations, just prepend `stanford.` to the name of the preference (see e.g. `stanford.dcoref.maxdist` in the provided configuration file).
To select the annotators to be applied, just change the value of `stanford.annotators`. For example, with `stanford.annotators = tokenize, ssplit` you'll have only tokenizer and sentence splitter.

The maximum length of text (in characters) processed by PIKES is limited by the `max_text_len` property in the configuration file. Set this value according to you needs.

If you experience problems or very slow performances, try increasing the `RAM` value in the `run.sh` script.<br/>
<br/>
## <a class="anchor" id="batch"></a>Running PIKES on GNU/Linux (Batch Processing Mode)
<br/>
PIKES can efficiently (parallel) process large quantities of files, in the so-called Batch Processing Mode.



<br/>
#### STEP 0 - Create the input NAF files

The documents to be processed by PIKES have to be provided as "input NAF" files. An "input NAF" file is a NAF file which contains the text to be processed within the `<raw>` tags, and with a minimal set of header attributes.

Example of minimal input NAF file:

```
<?xml version="1.0" encoding="UTF-8"?>
<NAF xml:lang="en" version="v3">
  <nafHeader>
    <fileDesc title="Juan Bautista de Anza and the Route to San Francisco Bay" />
    <public publicId="d331" uri="http://blog.yovisto.com/juan-bautista-de-anza-and-the-route-to-san-francisco-bay/" />
  </nafHeader>
  <raw><![CDATA[Juan Bautista de Anza and the Route to San Francisco Bay.
 
Juan Bautista de Anza, from a portrait in oil by Fray Orsi in 1774.  On March 28, 1776, Basque New-Spanish...]]></raw>
</NAF>
```

To automatically build the input NAF files for your document collection, have a look at some of the [converters we have developed](https://github.com/dkmfbk/pikes/tree/develop/pikes-resources/src/main/java/eu/fbk/dkm/pikes/resources).
A generic java code for converting plain text files to input NAFs can be found [here](https://github.com/dkmfbk/pikes/blob/develop/pikes-resources/src/main/java/eu/fbk/dkm/pikes/resources/Txt2Naf.java).

<br/>
#### STEP 1 - Process the input NAF files with PIKES

To process the input NAF files with PIKES you can adapt the script `process.sh` provided in the PIKES full package: 

```
#!/bin/bash
CLASSPATH=pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar:Semafor-3.0-alpha-04.jar:models/stanford-corenlp-3.7.0-models.jar
CONF=config-pikes.prop
INPUT_FOLDER=path-to-input-naf-out-folder
OUTPUT_FOLDER=path-to-output-naf-out-folder
PARALLEL_INSTANCES=1
MAXLENGTH=4500000
RAM=-Xmx12G

java $RAM -cp $CLASSPATH eu.fbk.dkm.pikes.tintop.FolderOrchestrator -c $CONF -i $INPUT_FOLDER -o $OUTPUT_FOLDER -s $PARALLEL_INSTANCES -z $MAXLENGTH
```

Adapt the script with the actual (full path) containing the input NAF files (`INPUT_FOLDER`) and where you want the processed files to be placed (`OUTPUT_FOLDER`).
Input NAF files may be organized in subfolders, and PIKES will preserve in the the output folder the subfolder structure.
`PARALLEL_INSTANCES` tells PIKES how many instances to be run in parallel.
`MAXLENGTH` tells PIKES to skip (i.e. not process) files with more than MAXLENGTH characters (very large files may take long time to be processed).
`RAM` tells pikes how much memory to use (you may have to increase it, based on the size of the files to be processed).

PIKES is configured to process only those input NAF files for which no corresponding processed NAF file is available in the `OUTPUT_FOLDER`. 
That is, re-running `process.sh` on the same `INPUT_FOLDER` will not overwrite any file already in the `OUTPUT_FOLDER`.
At the same time, if processing of the `INPUT_FOLDER` stops for any reason, by re-running `process.sh` PIKES will process only the not-yet processed input NAF files. 

You can test the the processing in this step with the input NAF files available [here](https://knowledgestore.fbk.eu/files/ke4ir/docs-raw-texts.zip).

<br/>
#### STEP 2 - Generate the RDF file(s)

To generate the RDF content from the processed NAF files you can adapt the script `generateRDF.sh` provided in the PIKES full package:

```
#!/bin/bash
CLASSPATH=pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar
NAF=path-to-naf-out-folder
RDF=path-to-rdf-folder
OUTPUT_FILE=$RDF/output.tql.gz

java -cp $CLASSPATH eu.fbk.dkm.pikes.rdf.Main rdfgen $NAF -o $OUTPUT_FILE -n -m -r
```

Adapt the script with the actual (full path) containing the input NAF files (`NAF`) and where you want the RDF files to be placed (`RDF`).
`MERGED` defines the name of the file containing the RDF content from all the NAFs considered (unless the `-i` flag is used). It is also used to decide, via the file extension provided, the RDF serialization used (e.g., `tql` in the example).
 The flags at the end of the java call affect the behaviour of PIKES and the generated RDF content:
 
 -  `[-r,--recursive]` : convert also files recursively nested in specified directories
 -  `[-i,--intermediate]` : produce single RDF files (one for each input NAF) instead of a single output file; output path and format are extracted from `MERGED`
 -  `[-m,--merge]` : merge instances (smushing plus filtering of group instances)
 -  `[-n,--normalize]` : normalize/compact output so to use less metadata statements
 
You can test the the generation of RDF content with the processed NAF files available [here](https://knowledgestore.fbk.eu/files/ke4ir/docs-nlp-annotations.zip).
<br/>
<br/>
## <a class="anchor" id="mac"></a>Running PIKES on a Mac OS X
<br/>
To execute PIKES on a Mac OS X machine, you need to recompile UKB, that needs [boost](http://www.boost.org/)
version 1.44 or higher.
If you have [Homebrew](http://brew.sh/) installed, just run `brew install boost`, otherwise you need to download
and compile boost.

Then run:

```
git clone https://github.com/asoroa/ukb
cd ukb/src/
./configure
make
```

Finally, copy `compile_kb`, `convert2.0`, `ukb_ppv` and `ukb_wsd` to the `ukb/` folder in the running directory.

During the `./configure` command, you may need to specify where boost has been installed using the
`--with-boost-include` parameter. If you used Homebrew, you should add
`--with-boost-include=/usr/local/Cellar/boost/1.63.0/include` (replace `1.63.0` with the version you installed).

Then, follow the instructions in [Running PIKES on GNU/Linux (Interactive Mode)](#inter) or [Running PIKES on GNU/Linux (Batch Processing Mode)](#batch) for using PIKES.
<br/>
<br/>
## <a class="anchor" id="sources"></a>Recompiling PIKES from sources
<br/>
If you want to generate the PIKES Java Library from sources, just execute:

```
git clone https://github.com/fbk/utils
cd utils
mvn clean install
cd ..
 
git clone https://github.com/fbk/fcw
cd fcw
git checkout develop
mvn clean install
cd ..
 
git clone https://github.com/dhfbk/tint
cd tint
git checkout develop
mvn clean install
cd ..
 
git clone https://github.com/dkmfbk/pikes
cd pikes
git checkout develop
mvn clean package -DskipTests -Prelease
```

You'll get the `pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar` package into the `pikes-tintop/target/` folder.
Just copy it to the running folder and restart PIKES.