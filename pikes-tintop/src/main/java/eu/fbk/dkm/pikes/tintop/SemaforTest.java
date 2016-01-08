package eu.fbk.dkm.pikes.tintop;

import edu.cmu.cs.lti.ark.fn.Semafor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alessio on 27/12/15.
 */

public class SemaforTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemaforTest.class);

    public static void main(String[] args) {
        try {
            final Semafor semafor = Semafor.getSemaforInstance("/Users/alessio/Documents/scripts/semafor/training/data/15_new");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
