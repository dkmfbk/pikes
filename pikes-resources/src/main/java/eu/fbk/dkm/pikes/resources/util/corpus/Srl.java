package eu.fbk.dkm.pikes.resources.util.corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessio on 12/11/15.
 */

public class Srl implements Serializable {

    private List<Word> target = new ArrayList<>();
    private List<Role> roles = new ArrayList<>();
    private String label;
    private String source;

    public Srl(List<Word> target, String label, String source) {
        this.target = target;
        this.label = label;
        this.source = source;
    }

    public Srl(Word target, String label, String source) {
        this.target.add(target);
        this.label = label;
        this.source = source;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public List<Word> getTarget() {
        return target;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public String getLabel() {
        return label;
    }

    public String getSource() {
        return source;
    }

    @Override public String toString() {
        return "Srl{" +
                "target=" + target +
                ", roles=" + roles +
                ", label='" + label + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
