package eu.fbk.dkm.pikes.resources.util.corpus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessio on 12/11/15.
 */

public class Role {

    private List<Word> span = new ArrayList<>();
    private String label;

    public Role(List<Word> span, String label) {
        this.span = span;
        this.label = label;
    }

    public Role(Word word, String label) {
        this.span.add(word);
        this.label = label;
    }

    public List<Word> getSpan() {
        return span;
    }

    public String getLabel() {
        return label;
    }

    @Override public String toString() {
        return "Role{" +
                "span=" + span +
                ", label='" + label + '\'' +
                '}';
    }
}
