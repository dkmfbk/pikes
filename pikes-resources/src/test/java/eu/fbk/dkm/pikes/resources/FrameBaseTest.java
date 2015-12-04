package eu.fbk.dkm.pikes.resources;

import java.util.List;

import com.google.common.collect.ImmutableList;

import org.openrdf.model.URI;

import eu.fbk.dkm.pikes.resources.FrameBase.POS;

public class FrameBaseTest {

    public static void main(final String[] args) {
        final String frame = "Possibilities";
        final String lemma = "alternative";
        final String pos = "n";
        final List<String> roles = ImmutableList.of();
        System.out.println(FrameBase.classFor(frame, lemma, POS.forFrameNetTag(pos))
                .getLocalName());
        for (final String role : roles) {
            System.out.println("  " + role + " -> "
                    + FrameBase.propertyFor(frame, role).getLocalName());
        }
        for (final String synsetID : WordNet.getSynsetsForLemma("romanticism", "n")) {
            final URI uri = YagoTaxonomy.getDBpediaYagoURI(synsetID);
            if (uri != null) {
                System.out.println("--> " + uri);
            }
        }
    }
}
