package eu.fbk.dkm.pikes.kv;

import java.io.File;

import eu.fbk.rdfpro.util.Statements;

public class KeyQuadIndexTest {

    public static void main(final String[] args) {
        final KeyQuadSource source = new KeyQuadIndex(new File("/mnt/data/pikes/yovisto/yago10k"));
        System.out.println(source.get(Statements.VALUE_FACTORY
                .createIRI("http://dbpedia.org/resource/Geneva")));
    }

}
