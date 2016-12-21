package eu.fbk.dkm.pikes.twm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:16
 * To change this template use File | Settings | File Templates.
 */

public class LinkingTag implements Serializable {

    public enum Category {DBPEDIA, SCHEMA}

    private int offset;
    private String page;
    private double score;
    private String originalText;
    private int length;
    private String source;
    private boolean spotted = true;
    private String image;

    private HashMap<Category, HashSet<String>> types = new HashMap<>();

    public LinkingTag(int offset, String page, double score, String originalText, int length, String source) {
        this.offset = offset;
        this.page = page;
        this.score = score;
        this.originalText = originalText;
        this.length = length;
        this.source = source;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void addType(Category category, String type) {
        if (types.get(category) == null) {
            types.put(category, new HashSet<>());
        }
        this.types.get(category).add(type);
    }

    public void addTypesFromML(ArrayList classes) {
        for (Object aClass : classes) {
            LinkedHashMap map = (LinkedHashMap) aClass;
            String type = (String) map.get("label");
            type = type.replaceAll("\\s+", "");
            addType(Category.DBPEDIA, type);
        }
    }

    public void addTypesFromDBpedia(String types) {
        String[] parts = types.split(",");
        for (String catType : parts) {
            catType = catType.trim();
            String[] typeParts = catType.split(":");
            if (typeParts.length < 2) {
                continue;
            }

            String cat = typeParts[0].trim();
            String type = typeParts[1].trim();

            switch (cat.toLowerCase()) {
            case "dbpedia":
                addType(Category.DBPEDIA, type);
                break;
            case "schema":
                addType(Category.SCHEMA, type);
            }
        }
    }

    public HashMap<String, HashSet<String>> getStringTypes() {
        HashMap<String, HashSet<String>> ret = new HashMap<>();
        for (Category category : types.keySet()) {
            String catString = category.toString();
            ret.put(catString, new HashSet<>());
            ret.get(catString).addAll(types.get(category));
        }

        return ret;
    }

    public HashMap<Category, HashSet<String>> getTypes() {
        return types;
    }

    public void setTypes(
            HashMap<Category, HashSet<String>> types) {
        this.types = types;
    }

    @Override
    public String toString() {
        return "DBpediaSpotlightTag{" +
                "offset=" + offset +
                ", page='" + page + '\'' +
                ", score=" + score +
                ", originalText='" + originalText + '\'' +
                ", length=" + length +
                ", source=" + source +
                ", spotted=" + spotted +
                ", types=" + types +
                '}';
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setSpotted(boolean spotted) {
        this.spotted = spotted;
    }

    public boolean isSpotted() {
        return spotted;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     * {@code x}, {@code x.equals(x)} should return
     * {@code true}.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     * {@code x} and {@code y}, {@code x.equals(y)}
     * should return {@code true} if and only if
     * {@code y.equals(x)} returns {@code true}.
     * <li>It is <i>transitive</i>: for any non-null reference values
     * {@code x}, {@code y}, and {@code z}, if
     * {@code x.equals(y)} returns {@code true} and
     * {@code y.equals(z)} returns {@code true}, then
     * {@code x.equals(z)} should return {@code true}.
     * <li>It is <i>consistent</i>: for any non-null reference values
     * {@code x} and {@code y}, multiple invocations of
     * {@code x.equals(y)} consistently return {@code true}
     * or consistently return {@code false}, provided no
     * information used in {@code equals} comparisons on the
     * objects is modified.
     * <li>For any non-null reference value {@code x},
     * {@code x.equals(null)} should return {@code false}.
     * </ul>
     * <p>
     * The {@code equals} method for class {@code Object} implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values {@code x} and
     * {@code y}, this method returns {@code true} if and only
     * if {@code x} and {@code y} refer to the same object
     * ({@code x == y} has the value {@code true}).
     * <p>
     * Note that it is generally necessary to override the {@code hashCode}
     * method whenever this method is overridden, so as to maintain the
     * general contract for the {@code hashCode} method, which states
     * that equal objects must have equal hash codes.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     * @see #hashCode()
     * @see HashMap
     */
    @Override public boolean equals(Object obj) {
        if (obj instanceof LinkingTag) {
            return ((LinkingTag) obj).getPage().equals(this.getPage());
        }

        return false;
    }
}
