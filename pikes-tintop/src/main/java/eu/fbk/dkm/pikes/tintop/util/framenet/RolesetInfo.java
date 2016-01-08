package eu.fbk.dkm.pikes.tintop.util.framenet;

import eu.fbk.dkm.pikes.resources.util.propbank.Roleset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by alessio on 14/11/15.
 */

public class RolesetInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolesetInfo.class);

    private File origFile;
    private String label;
    private String baseLemma;
    private String lemma;
    private String type;
    private HashMap<String, HashMap<String, Set>> senses;
    private Set<String> luFrames;
    private Roleset roleset;
    private List<String> synsets;

    public RolesetInfo(File origFile, String label, String baseLemma, String lemma, String type,
            HashMap<String, HashMap<String, Set>> senses, Set<String> luFrames,
            Roleset roleset, List<String> synsets) {
        this.origFile = origFile;
        this.label = label;
        this.baseLemma = baseLemma;
        this.lemma = lemma;
        this.type = type;
        this.senses = senses;
        this.luFrames = luFrames;
        this.roleset = roleset;
        this.synsets = synsets;
    }

    public List<String> getSynsets() {
        return synsets;
    }

    public void setSynsets(List<String> synsets) {
        this.synsets = synsets;
    }

    public File getOrigFile() {
        return origFile;
    }

    public void setOrigFile(File origFile) {
        this.origFile = origFile;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBaseLemma() {
        return baseLemma;
    }

    public void setBaseLemma(String baseLemma) {
        this.baseLemma = baseLemma;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public HashMap<String, HashMap<String, Set>> getSenses() {
        return senses;
    }

    public void setSenses(HashMap<String, HashMap<String, Set>> senses) {
        this.senses = senses;
    }

    public Set<String> getLuFrames() {
        return luFrames;
    }

    public void setLuFrames(Set<String> luFrames) {
        this.luFrames = luFrames;
    }

    public Roleset getRoleset() {
        return roleset;
    }

    public void setRoleset(Roleset roleset) {
        this.roleset = roleset;
    }
}
