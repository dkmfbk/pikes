package eu.fbk.dkm.pikes.tintop;

import eu.fbk.dkm.pikes.tintop.server.PipelineServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class TintTest {
    public static void main(String[] args) throws IOException {
        File configFile = new File("/Users/marcorospocher/CodeRepository/pikes/config-pikes.prop");
        Properties properties = new Properties();

        properties.load(new FileInputStream(configFile));




//        properties.setProperty("standord.annotators","tokenize, ssplit, dbps, pos, simple_pos, lemma, ukb, ner_custom, parse, conll_parse, mate, mst_fake, semafor, fake_dep, dcoref")


        //ITALIANO
//        properties.setProperty("stanford.annotators","ita_toksent, dbps, pos, ita_upos, ita_morpho, ita_lemma, ita_semafor, depparse, fake_dep, ner");
//
//        //pos,linking, ner, timex?, semafor, dp.
//
//
//        properties.setProperty("stanford.ita_semafor.yandex.key", "trnsl.1.1.20171010T091318Z.1e87b765f05f625b.099f046a438c4dd1efac8bbbd3a6f9f7d80fc866");
//        properties.setProperty("stanford.ita_semafor.engine", "yandex");
//
//        properties.setProperty("stanford.ita_semafor.stanford.annotators", "tokenize, ssplit, pos, lemma, conll_parse, semafor");
//        properties.setProperty("stanford.ita_semafor.stanford.semafor.model_dir", "/Users/marcorospocher/Downloads/models");
//        properties.setProperty("stanford.ita_semafor.stanford.semafor.use_conll", "true");
//        properties.setProperty("stanford.ita_semafor.stanford.customAnnotatorClass.mst_fake", "eu.fbk.dkm.pikes.depparseannotation.FakeMstParserAnnotator");
//        properties.setProperty("stanford.ita_semafor.stanford.customAnnotatorClass.semafor", "eu.fbk.fcw.semafor.SemaforAnnotator");
//        properties.setProperty("stanford.ita_semafor.stanford.customAnnotatorClass.conll_parse", "eu.fbk.fcw.mate.AnnaParseAnnotator");
//        properties.setProperty("stanford.ita_semafor.stanford.conll_parse.model", "/Users/marcorospocher/CodeRepository/pikes/models/anna_parse.model");
//
//        properties.setProperty("stanford.ita_semafor.aligner.host", "dh-server");
//        properties.setProperty("stanford.ita_semafor.aligner.port", "9010");
//
//        properties.setProperty("stanford.ita_semafor.includeOriginal", "1");
//
//        properties.setProperty("stanford.pos.model", "models/italian-big.tagger");
//        properties.setProperty("stanford.ner.model", "models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser.gz");
//        properties.setProperty("stanford.depparse.model", "models/parser-model-1.txt.gz");
//        properties.setProperty("stanford.dbps.annotator", "ml-annotate");
//        properties.setProperty("stanford.dbps.address", "http://ml.apnetwork.it/annotate");
//        properties.setProperty("stanford.dbps.min_confidence", "0.3");
//        properties.setProperty("stanford.dbps.first_confidence", "0.5");
//        properties.setProperty("stanford.dbps.extract_types", "0");
//        properties.setProperty("stanford.customAnnotatorClass.ita_toksent", "eu.fbk.dh.tint.tokenizer.annotators.ItalianTokenizerAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_lemma", "eu.fbk.dh.tint.digimorph.annotator.DigiLemmaAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_morpho", "eu.fbk.dh.tint.digimorph.annotator.DigiMorphAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_verb", "eu.fbk.dh.tint.verb.VerbAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_upos", "eu.fbk.dh.tint.upos.UPosAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_semafor", "eu.fbk.fcw.semafortranslate.SemaforTranslateAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_derivation", "eu.fbk.dh.tint.derived.DerivationAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.readability", "eu.fbk.dh.tint.readability.ReadabilityAnnotator");
        properties.setProperty("stanford.customAnnotatorClass.fake_dep", "eu.fbk.dkm.pikes.depparseannotation.StanfordToConllDepsAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.timex", "eu.fbk.dh.tint.heideltime.annotator.HeidelTimeAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.dbps", "eu.fbk.dkm.pikes.twm.LinkingAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ml", "eu.fbk.dkm.pikes.twm.LinkingAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.geoloc", "eu.fbk.dh.tint.geoloc.annotator.GeolocAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.stem", "eu.fbk.fcw.stemmer.corenlp.StemAnnotator");




        // add annotators here... order matters
//        properties.setProperty("stanford.annotators", "udpipe, ner, simple_pos, ukb, stanford2conll");
//        properties.setProperty("stanford.annotators", "udpipe, dbps, simple_pos, ukb, ner_custom, conll_parse, mst_fake, semafor");
//        properties.setProperty("stanford.annotators", "udpipe, dbps, ner_custom, simple_pos, ukb, stanford2conll, mst_fake, semafor");
//        properties.setProperty("stanford.udpipe.server", "gardner");
//        properties.setProperty("stanford.udpipe.port", "50021");
//        properties.setProperty("stanford.udpipe.alreadyTokenized", "0");
//        properties.setProperty("stanford.customAnnotatorClass.udpipe", "eu.fbk.fcw.udpipe.api.UDPipeAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.stanford2conll", "eu.fbk.dkm.pikes.depparseannotation.StanfordToConllDepsAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_toksent", "eu.fbk.dh.tint.tokenizer.annotators.ItalianTokenizerAnnotator");
//        properties.setProperty("stanford.ner.model", "models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser.gz");
//        properties.setProperty("stanford.ner.useSUTime", "0");
        properties.setProperty("enable_naf_filter", "0");

        PipelineServer pipelineServer = new PipelineServer("0.0.0.0", 8011, configFile, properties);
    }
}
