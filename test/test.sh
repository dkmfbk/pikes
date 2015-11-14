#!/bin/bash

export CLASSPATH=./lib/*

source ./config.sh

mkdir -p naf
mkdir -p rdf
mkdir -p pikes

rm -r naf/*
rm -r rdf/*
rm -r pikes/*

count=0
while read p; do
    if [ -n "$p" ]; then
        ((count=count+1))
        curl -XPOST https://knowledgestore2.fbk.eu/pikes-demo/api/text2naf \
            --data-urlencode "text=$p" \
            --data-urlencode "meta_uri=file:///eswc.$count.ttl/" \
            --data "rdf_fusion=on&rdf_compaction=on&meta_author=&meta_title=&meta_id=&meta_date=2015-11-10T15%3A55%3A28%2B01%3A00&outputformat=output_naf&annotator_tokenize=on&annotator_ssplit=on&annotator_anna_pos=on&annotator_simple_pos=on&annotator_lemma=on&annotator_ukb=on&annotator_sst=on&annotator_ner=on&annotator_parse=on&annotator_dep_parse=on&annotator_dcoref=on&annotator_sentiment=on&annotator_srl=on&annotator_cross_srl=on&annotator_linking=on&annotator_naf_filter=on" > naf/eswc.$count.naf
        java eu.fbk.dkm.pikes.rdf.Main -V rdfgen -n -o rdf/eswc.$count.trig naf/eswc.$count.naf
        $RDFPRO_PATH @read rdf/eswc.$count.trig @transform '=c <ex:a> -p ks:expressedBy' @prefix @unique @write pikes/temp.ttl
        cat pikes/temp.ttl | sed -E 's/<file:\/\/\/eswc.'$count'.ttl\/\#/<file:\/\/\/eswc.'$count'.ttl\//g' > pikes/eswc.$count.ttl
        rm pikes/temp.ttl
    fi
done < sentences.txt

filelist=
for f in pikes/eswc*.ttl; do
    filelist="$filelist $f"
done
java eu.fbk.dkm.pikes.eval.Converter -f pikes -o pikes/pikes_converted.trig $filelist eswc.patch.trig
java eu.fbk.dkm.pikes.eval.Aligner -o pikes/aligned.trig pikes/pikes_converted.trig gold_converted.trig
java eu.fbk.dkm.pikes.eval.Evaluation pikes/aligned.trig
