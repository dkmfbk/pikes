package eu.fbk.dkm.pikes.resources.util.corpus;

/**
 * Created by alessio on 12/11/15.
 */

public class Word {

    private int id;
    private String form;
    private String lemma;
    private String pos;

    private int depParent;
    private String depLabel;

    private int begin;
    private int end;

    public static Word readFromArray(String[] parts) {

        //todo: better management of possibilities
        if (parts.length >= 12) {
            return new Word(
                    Integer.parseInt(parts[0]),
                    parts[1],
                    parts[2],
                    parts[4],
                    Integer.parseInt(parts[8]),
                    parts[10]
            );
        }
        return new Word(
                Integer.parseInt(parts[0]),
                parts[1],
                parts[2],
                parts[4]
        );
    }

    public Word(int id, String form, String lemma, String pos) {
        this.id = id;
        this.form = form;
        this.lemma = lemma;
        this.pos = pos;
    }

    public Word(int id, String form, String lemma, String pos, int depParent, String depLabel) {
        this.id = id;
        this.form = form;
        this.lemma = lemma;
        this.pos = pos;
        this.depParent = depParent;
        this.depLabel = depLabel;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public int getDepParent() {
        return depParent;
    }

    public void setDepParent(int depParent) {
        this.depParent = depParent;
    }

    public String getDepLabel() {
        return depLabel;
    }

    public void setDepLabel(String depLabel) {
        this.depLabel = depLabel;
    }

    public int getId() {
        return id;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override public String toString() {
        return "Word{" +
                "id=" + id +
                ", form='" + form + '\'' +
                ", lemma='" + lemma + '\'' +
                ", pos='" + pos + '\'' +
                ", depParent=" + depParent +
                ", depLabel='" + depLabel + '\'' +
                '}';
    }
}
