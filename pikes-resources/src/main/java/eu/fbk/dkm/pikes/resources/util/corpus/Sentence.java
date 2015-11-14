package eu.fbk.dkm.pikes.resources.util.corpus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alessio on 12/11/15.
 */

public class Sentence {

    private List<Word> words = new ArrayList<>();
    private List<Srl> srls = new ArrayList<>();
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Sentence(List<Word> words) {
        this.words = words;
    }

    public void addWord(Word word) {
        this.words.add(word);
    }

    public void addSrl(Srl srl) {
        this.srls.add(srl);
    }

    public Sentence() {
    }

    public List<Word> getWords() {
        return words;
    }

    public List<Srl> getSrls() {
        return srls;
    }

    @Override public String toString() {
        return "Sentence{" +
                "words=" + words +
                ", srls=" + srls +
                '}';
    }
}
