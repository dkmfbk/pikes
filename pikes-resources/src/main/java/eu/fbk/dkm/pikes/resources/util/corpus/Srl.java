package eu.fbk.dkm.pikes.resources.util.corpus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessio on 12/11/15.
 */

public class Srl {

    private List<Word> target = new ArrayList<>();
    private List<Role> roles = new ArrayList<>();
    private String label;

    public Srl(List<Word> target, String label) {
        this.target = target;
        this.label = label;
    }

    public Srl(Word target, String label) {
        this.target.add(target);
        this.label = label;
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

    @Override public String toString() {
        return "Srl{" +
                "target=" + target +
                ", roles=" + roles +
                ", label='" + label + '\'' +
                '}';
    }
}
