package eu.fbk.dkm.pikes.resources.ecb;

import eu.fbk.dkm.utils.eval.PrecisionRecall;

import java.util.*;

/**
 * Created by alessio on 28/01/16.
 */

public class ClusteringEvaluation {

    static class Pair<T> {

        T s1, s2;

        public Pair(T s1, T s2) {
            this.s1 = s1;
            this.s2 = s2;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pair)) {
                return false;
            }

            Pair pair = (Pair) o;

            return (s1.equals(pair.s1) && s2.equals(pair.s2)) ||
                    (s1.equals(pair.s2) && s2.equals(pair.s1));
        }

        @Override public int hashCode() {
            int result = s1.hashCode();
            result = 31 * result + s2.hashCode();
            return result;
        }

        @Override public String toString() {
            return "Pair{" +
                    "s1='" + s1 + '\'' +
                    ", s2='" + s2 + '\'' +
                    '}';
        }
    }

    static Map<PrecisionRecall.Measure, Double> pairWise(Set<Set> goldS, Set<Set> classifiedS) {

        Set<List> gold = new HashSet<>();
        Set<List> classified = new HashSet<>();

        for (Set set : goldS) {
            gold.add(new ArrayList<>(set));
        }
        for (Set set : classifiedS) {
            classified.add(new ArrayList<>(set));
        }

        Set<Pair> goldPairs = new HashSet<>();
        Set<Pair> classifiedPairs = new HashSet<>();

        for (List objects : gold) {
            for (int i = 0; i < objects.size(); i++) {
                for (int j = i + 1; j < objects.size(); j++) {
                    goldPairs.add(new Pair(objects.get(i), objects.get(j)));
                }
            }
        }
        for (List objects : classified) {
            for (int i = 0; i < objects.size(); i++) {
                for (int j = i + 1; j < objects.size(); j++) {
                    classifiedPairs.add(new Pair(objects.get(i), objects.get(j)));
                }
            }
        }

        Set<Pair> intersection = new HashSet<>(goldPairs);
        intersection.retainAll(classifiedPairs);

        double commons = intersection.size() * 1.0;
        double goldPairCount = goldPairs.size() * 1.0;
        double classifiedPairCount = classifiedPairs.size() * 1.0;

        double p = commons / classifiedPairCount;
        double r = commons / goldPairCount;

        Map<PrecisionRecall.Measure, Double> ret = new HashMap<>();
        ret.put(PrecisionRecall.Measure.PRECISION, p);
        ret.put(PrecisionRecall.Measure.RECALL, r);
        ret.put(PrecisionRecall.Measure.F1, 2 * p * r / (p + r));

        return ret;
    }

    public static void main(String[] args) {

        Set<Set> m = new HashSet<>();
        Set<String> m1 = new HashSet<>();
        m1.add("a");
        m1.add("b");
        m1.add("c");
        Set<String> m2 = new HashSet<>();
        m2.add("d");
        m2.add("e");
        m2.add("f");
        m.add(m1);
        m.add(m2);

        Set<Set> c = new HashSet<>();
        Set<String> c1 = new HashSet<>();
        c1.add("a");
        c1.add("b");
        Set<String> c2 = new HashSet<>();
        c2.add("c");
        c2.add("d");
        c2.add("e");
        Set<String> c3 = new HashSet<>();
        c3.add("f");
        c.add(c1);
        c.add(c2);
        c.add(c3);

        System.out.println(pairWise(m, c));
    }
}
