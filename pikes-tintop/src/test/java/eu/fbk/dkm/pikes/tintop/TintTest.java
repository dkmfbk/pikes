package eu.fbk.dkm.pikes.tintop;

import eu.fbk.dkm.pikes.tintop.server.PipelineServer;

import java.io.File;
import java.util.Properties;

public class TintTest {
    public static void main(String[] args) {
        File configFile = new File("/Volumes/Dati/Resources/pikes/config-pikes.prop");
        Properties properties = new Properties();

        properties.setProperty("stanford.annotators", "udpipe, ner");
        properties.setProperty("stanford.udpipe.server", "gardner");
        properties.setProperty("stanford.udpipe.port", "50021");
        properties.setProperty("stanford.udpipe.alreadyTokenized", "0");
        properties.setProperty("stanford.customAnnotatorClass.udpipe", "eu.fbk.fcw.udpipe.api.UDPipeAnnotator");
//        properties.setProperty("stanford.customAnnotatorClass.ita_toksent", "eu.fbk.dh.tint.tokenizer.annotators.ItalianTokenizerAnnotator");
//        properties.setProperty("stanford.ner.model", "models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser.gz");
//        properties.setProperty("stanford.ner.useSUTime", "0");
        properties.setProperty("enable_naf_filter", "0");

        PipelineServer pipelineServer = new PipelineServer("0.0.0.0", 8011, configFile, properties);
    }
}
