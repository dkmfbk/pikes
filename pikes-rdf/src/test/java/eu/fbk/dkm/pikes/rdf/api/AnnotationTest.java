package eu.fbk.dkm.pikes.rdf.api;

import org.junit.Test;

import eu.fbk.dkm.pikes.rdf.api.Annotation.Format;

public class AnnotationTest {

    @Test
    public void test() throws Throwable {
        for (final Format format : Format.values()) {
            System.out.println(format);
        }
    }

}
