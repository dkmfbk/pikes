package eu.fbk.dkm.pikes.twm;

import eu.fbk.dh.tint.json.JSONable;

import java.util.ArrayList;

/**
 * Created by alessio on 27/09/16.
 */

public class LinkingList<T> extends ArrayList<T> implements JSONable {

    @Override public String getName() {
        return "linkings";
    }

}
