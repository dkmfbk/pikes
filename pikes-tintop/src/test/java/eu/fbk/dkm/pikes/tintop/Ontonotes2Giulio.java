package eu.fbk.dkm.pikes.tintop;

import eu.fbk.dkm.pikes.resources.util.corpus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by alessio on 20/01/16.
 */

public class Ontonotes2Giulio {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ontonotes2Giulio.class);

    public static void main(String[] args) throws IOException {
        String inputFile = args[0];
        String outputFile = args[1];

        Corpus corpus = Corpus.readDocumentFromFile(inputFile, "ontonotes-5");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

        for (Sentence sentence : corpus) {

            HashMap<String, StringBuffer> buffers = new HashMap<>();
            ArrayList<StringBuffer> additionalBuffers = new ArrayList<>();

            buffers.put("id", new StringBuffer());
            buffers.put("form", new StringBuffer());
            buffers.put("lemma", new StringBuffer());
            buffers.put("pos", new StringBuffer());
            buffers.put("deplabel", new StringBuffer());
            buffers.put("depparent", new StringBuffer());

            for (Word word : sentence) {
                buffers.get("id").append(word.getId()).append('\t');
                buffers.get("form").append(word.getForm()).append('\t');
                buffers.get("lemma").append(word.getLemma()).append('\t');
                buffers.get("pos").append(word.getPos()).append('\t');
                buffers.get("deplabel").append(word.getDepLabel()).append('\t');
                buffers.get("depparent").append(word.getDepParent()).append('\t');
            }

            for (Srl srl : sentence.getSrls()) {
                StringBuffer stringBuffer = new StringBuffer();
                HashMap<Integer, String> tokens = new HashMap<>();

                for (Word word : srl.getTarget()) {
                    tokens.put(word.getId(), srl.getLabel());
                }
                for (Role role : srl.getRoles()) {
                    for (Word word : role.getSpan()) {
                        Set<Integer> descendants = sentence.getDescendants(word.getId());
                        for (Integer descendant : descendants) {
                            tokens.put(descendant, role.getLabel());
                        }
                    }
                }

                for (int i = 0; i < sentence.getWords().size(); i++) {
                    String s = tokens.get(i + 1);
                    if (s != null) {
                        stringBuffer.append(s);
                    }
                    else {
                        stringBuffer.append("-");
                    }
                    stringBuffer.append('\t');
                }

                additionalBuffers.add(stringBuffer);
            }

            writer.append(buffers.get("id").toString().trim()).append('\n');
            writer.append(buffers.get("form").toString().trim()).append('\n');
            writer.append(buffers.get("lemma").toString().trim()).append('\n');
            writer.append(buffers.get("pos").toString().trim()).append('\n');
            writer.append(buffers.get("deplabel").toString().trim()).append('\n');
            writer.append(buffers.get("depparent").toString().trim()).append('\n');

            for (StringBuffer buffer : additionalBuffers) {
                String s = buffer.toString();
                s = s.substring(0, s.length() - 1);
                writer.append(s).append('\n');
            }

            writer.append('\n');
        }

        writer.close();

    }
}
