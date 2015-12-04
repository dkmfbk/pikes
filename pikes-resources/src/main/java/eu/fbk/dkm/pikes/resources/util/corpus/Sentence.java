package eu.fbk.dkm.pikes.resources.util.corpus;

import java.io.Serializable;
import java.util.*;

/**
 * Created by alessio on 12/11/15.
 */

public class Sentence implements Iterable<Word>, Serializable {

    private static final int LOOP_STOP = 100;
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

    /**
     * Given a word, it gives the list of ancestors in the dependency tree.
     *
     * @param i the index (from 0 to n-1) of the Word
     * @return the ist of ancestors
     */
    public List<Integer> getAncestors(int i) {
        ArrayList<Integer> ret = new ArrayList<>();

        int noLoop = 0;
        while (i >= 0 && words.get(i) != null) {
            ret.add(i);
            i = words.get(i).getDepParent() - 1;
            noLoop++;
            if (noLoop > LOOP_STOP) {
                break;
            }
        }

        return ret;
    }

    public Integer searchHead(List<Integer> span) {
        List<Integer>[] ancestorLists = new List[span.size()];
        for (int i = 0; i < span.size(); i++) {
            Integer integer = span.get(i);
            ancestorLists[i] = getAncestors(integer);
        }

        return giveHead(ancestorLists);
    }

    private Integer giveHead(List<Integer>... ancestorLists) {
        if (ancestorLists.length == 1) {
            return ancestorLists[0].get(0);
        }

        int k = 1;
        int last = 0;
        while (true) {
            Set<Integer> integers = new HashSet<>();
            for (int i = 0; i < ancestorLists.length; i++) {
                try {
                    integers.add(ancestorLists[i].get(ancestorLists[i].size() - k));
                } catch (ArrayIndexOutOfBoundsException e) {
                    return last;
                }
            }
            if (integers.size() == 1) {
                for (Integer integer : integers) {
                    last = integer;
                }
            } else {
                return last;
            }
            k++;
        }
    }

    public Sentence() {
    }

    public List<Word> getWords() {
        return words;
    }

    public List<Srl> getSrls() {
        return srls;
    }

    public String toConllString() {

        Map<Integer, Srl> srlMap = new TreeMap<>();
        for (Srl srl : srls) {
            Word word = srl.getTarget().get(0);
            srlMap.put(word.getId(), srl);
        }

        int additionalColumnsNo = 2 + srlMap.size();
        String[][] additionalColumns = new String[getWords().size()][additionalColumnsNo];
        for (int j = 0; j < getWords().size(); j++) {
            for (int i = 0; i < additionalColumnsNo; i++) {
                additionalColumns[j][i] = "_";
            }
        }

        int roleIndex = 0;
        for (Integer key : srlMap.keySet()) {
            Srl srl = srlMap.get(key);

            additionalColumns[key - 1][0] = "Y";
            additionalColumns[key - 1][1] = srl.getLabel();

            for (Role role : srl.getRoles()) {
                int roleKey = role.getSpan().get(0).getId();
                additionalColumns[roleKey - 1][2 + roleIndex] = role.getLabel();
            }

            roleIndex++;
        }

        StringBuilder builder = new StringBuilder();

        int i = 0;
        for (Word word : getWords()) {
            builder.append(word.getId()).append("\t");
            builder.append(word.getForm()).append("\t");
            builder.append(word.getForm()).append("\t");
            builder.append(word.getLemma()).append("\t");
            builder.append(word.getPos()).append("\t");
            builder.append(word.getPos()).append("\t");
            builder.append("_").append("\t");
            builder.append("_").append("\t");
            builder.append(word.getDepParent()).append("\t");
            builder.append(word.getDepParent()).append("\t");
            builder.append(word.getDepLabel()).append("\t");
            builder.append(word.getDepLabel());
            for (String col : additionalColumns[i]) {
                builder.append("\t").append(col);
            }
            builder.append("\n");

            i++;
        }
        builder.append("\n");

        return builder.toString();
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Sentence {").append("\n");
        builder.append(" Words {").append("\n");
        for (Word word : words) {
            builder.append("  " + word.toString()).append("\n");
        }
        builder.append(" }").append("\n");
        builder.append(" Srls {").append("\n");
        for (Srl srl : srls) {
            builder.append("  " + srl.toString()).append("\n");
        }
        builder.append(" }").append("\n");

        return builder.toString();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override public Iterator<Word> iterator() {
        return words.iterator();
    }
}
