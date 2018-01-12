package eu.fbk.dkm.pikes.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.google.common.io.Resources;
import eu.fbk.rdfpro.util.Environment;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.dictionary.Dictionary;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

public final class WordNet {

    public static final String POS_NOUN = "n";

    public static final String POS_VERB = "v";

    public static final String POS_ADJECTIVE = "a";

    public static final String POS_ADVERB = "r";

    private static final Map<String, String> BBN_TO_SYNSET;

    private static final Map<String, List<String>> SYNSET_TO_BBN; // built from bbnToSynset

    private static final Map<String, String> BBN_TO_SST;

    private static final Map<String, String> SYNSET_TO_SST; // contains partial mapping overriding
    // lexicographer file

    private static String dictionaryPath = Objects.firstNonNull(
            Environment.getProperty("wordnet.home"), "wordnet");

    private static Dictionary dictionary;

    static {
        // TODO: need better mapping
        final Map<String, String> bbnToSynset = Maps.newLinkedHashMap();
        bbnToSynset.put("person", "00007846-n");
        bbnToSynset.put("organization", "08008335-n"); // was 07950920
        bbnToSynset.put("gpe", "00027167-n");
        bbnToSynset.put("location", "00027167-n");
        bbnToSynset.put("event", "00029378-n");
        bbnToSynset.put("product", "04007894-n");
        bbnToSynset.put("fac", "03315023-n");
        bbnToSynset.put("work_of_art", "02743547-n");
        bbnToSynset.put("law", "06532330-n");
        bbnToSynset.put("language", "06282651-n");
        bbnToSynset.put("quantity", "00033615-n");
        bbnToSynset.put("date", "15113229-n");
        bbnToSynset.put("time", "15113229-n");
        bbnToSynset.put("percent", "13817526-n");
        bbnToSynset.put("money", "13384557-n");
        bbnToSynset.put("ordinal", "14429985-n");
        bbnToSynset.put("cardinal", "13582013-n");
        BBN_TO_SYNSET = ImmutableMap.copyOf(bbnToSynset);

        final Map<String, List<String>> synsetToBbn = Maps.newHashMap();
        for (final Map.Entry<String, String> entry : bbnToSynset.entrySet()) {
            final String bbn = entry.getKey();
            final String synset = entry.getValue();
            final List<String> list = synsetToBbn.get(synset);
            if (list == null) {
                synsetToBbn.put(synset, ImmutableList.of(bbn));
            } else {
                synsetToBbn.put(
                        synset,
                        Ordering.natural().immutableSortedCopy(
                                Iterables.concat(list, ImmutableList.of(bbn))));
            }
        }
        SYNSET_TO_BBN = ImmutableMap.copyOf(synsetToBbn);

        final Map<String, String> bbnToSst = Maps.newLinkedHashMap();
        bbnToSst.put("person", "B-noun.person");
        bbnToSst.put("organization", "B-noun.group");
        bbnToSst.put("gpe", "B-noun.location");
        bbnToSst.put("location", "B-noun.location");
        bbnToSst.put("event", "B-noun.event");
        bbnToSst.put("product", "B-noun.artifact");
        bbnToSst.put("fac", "B-noun.artifact");
        bbnToSst.put("work_of_art", "B-noun.artifact");
        bbnToSst.put("law", "B-noun.communication");
        bbnToSst.put("language", "B-noun.communication");
        bbnToSst.put("quantity", "B-noun.quantity");
        bbnToSst.put("date", "B-noun.time");
        bbnToSst.put("time", "B-noun.time");
        bbnToSst.put("percent", "B-noun.relation");
        bbnToSst.put("money", "B-noun.possession");
        bbnToSst.put("ordinal", "B-noun.state");
        bbnToSst.put("cardinal", "B-noun.quantity");
        BBN_TO_SST = ImmutableMap.copyOf(bbnToSst);

        final Map<String, String> synsetToSst = Maps.newHashMap();
        synsetToSst.put("00007846-n", "B-noun.person");
        synsetToSst.put("00027167-n", "B-noun.location");
        synsetToSst.put("00033615-n", "B-noun.quantity");
        SYNSET_TO_SST = ImmutableMap.copyOf(synsetToSst);
    }

    private static Dictionary getDictionary() {
        synchronized (WordNet.class) {
            if (dictionary == null) {
                JWNL.shutdown(); // in case it was previously initialized
                try {
                    final String properties = Resources.toString(
                            WordNet.class.getResource("jwnl.xml"), Charsets.UTF_8).replace(
                            "DICTIONARY_PATH_PLACEHOLDER", dictionaryPath);
                    final InputStream stream = new ByteArrayInputStream(
                            properties.getBytes(Charsets.UTF_8));
                    JWNL.initialize(stream);
                    dictionary = Dictionary.getInstance();
                } catch (final Throwable ex) {
                    JWNL.shutdown();
                    throw new Error("Cannot initialize JWNL using dictionary path '"
                            + dictionaryPath + "'", ex);
                }
            }
            return dictionary;
        }
    }

    private static void releaseDictionary() {
        synchronized (WordNet.class) {
            dictionary = null;
            JWNL.shutdown(); // safe to call it multiple times
        }
    }

    private static Synset getSynset(final String id) {
        final POS pos = POS.getPOSForKey(getPOS(id));
        final long offset = getOffset(id);
        try {
            synchronized (WordNet.class) {
                return getDictionary().getSynsetAt(pos, offset);
            }
        } catch (final JWNLException ex) {
            throw new Error(ex);
        }
    }

    // synsetID has the form offset-x, where x is n for nouns, a for adjectives, v for verbs, r
    // for adverbs

    public static void init() {
        getDictionary();
    }

    public static List<String> getSynsetsForLemma(String lemma, String pos) {
        try {
            synchronized (WordNet.class) {
                IndexWord indexWord = getDictionary().lookupIndexWord(POS.getPOSForKey(pos), lemma);
                if (indexWord == null) {
                    return new ArrayList<>();
                }
                Synset[] synsets = indexWord.getSenses();
                ArrayList<String> ret = new ArrayList<>();
                for (int i = 0; i < synsets.length; i++) {
                    Synset synset = synsets[i];
                    ret.add(getSynsetID(synset.getOffset(), synset.getPOS().getKey()));
                }

                return ret;
            }
        } catch (final JWNLException ex) {
            throw new Error(ex);
        }
    }


    public static Iterator getNounSynsets() {
        try {
            synchronized (WordNet.class) {
                return WordNet.getDictionary().getSynsetIterator(POS.NOUN);
            }
        } catch (final JWNLException ex) {
            throw new Error(ex);
        }
    }


    public static Iterator getADJSynsets() {
        try {
            synchronized (WordNet.class) {
                return WordNet.getDictionary().getSynsetIterator(POS.ADJECTIVE);
            }
        } catch (final JWNLException ex) {
            throw new Error(ex);
        }
    }

    public static Iterator getADVSynsets() {
        try {
            synchronized (WordNet.class) {
                return WordNet.getDictionary().getSynsetIterator(POS.ADVERB);
            }
        } catch (final JWNLException ex) {
            throw new Error(ex);
        }
    }

    public static Iterator getVerbSynsets() {
        try {
            synchronized (WordNet.class) {
                return WordNet.getDictionary().getSynsetIterator(POS.VERB);
            }
        } catch (final JWNLException ex) {
            throw new Error(ex);
        }
    }

    public static String getPath() {
        synchronized (WordNet.class) {
            return dictionaryPath;
        }
    }

    public static void setPath(final String dictionaryPath) {
        Preconditions.checkNotNull(dictionaryPath);
        synchronized (WordNet.class) {
            if (!WordNet.dictionaryPath.equals(dictionaryPath)) {
                releaseDictionary();
                WordNet.dictionaryPath = dictionaryPath;
            }
        }
    }

    // MANIPULATION OF SYNSET IDS

    public static String getSynsetID(final long offset, final String pos) {
        return String.format("%08d-%s", offset, pos);
    }

    /**
     * Return the synset ID starting from a readable format:
     * <ul>
     * <li>lemma</li>
     * <li>"-" (dash)</li>
     * <li>synset number</li>
     * <li>POS</li>
     * </ul>
     * <p>
     * For example: look-3v
     *
     * @param readableSynsetID an absolute URL giving the base location of the image
     */
    @Nullable
    public static String getSynsetID(@Nullable final String readableSynsetID) {
        if (readableSynsetID == null) {
            return null;
        }
        try {
            final int length = readableSynsetID.length();
            final int offset = readableSynsetID.lastIndexOf('-');
            final String lemma = readableSynsetID.substring(0, offset);
            final int index = Integer.parseInt(readableSynsetID.substring(offset + 1, length - 1)) - 1;
            final POS pos = POS.getPOSForKey(readableSynsetID.substring(length - 1, length));
            final IndexWord word;
            synchronized (WordNet.class) {
                word = getDictionary().getIndexWord(pos, lemma);
            }
            final Synset synset = word.getSenses()[index];
            return getSynsetID(synset.getOffset(), pos.getKey());
        } catch (final JWNLException ex) {
            throw new Error(ex);
        } catch (final Throwable ex) {
            throw new IllegalArgumentException("Illegal (readable) synset ID " + readableSynsetID,
                    ex);
        }
    }

    @Nullable
    public static String getReadableSynsetID(@Nullable final String synsetID) {
        if (synsetID == null) {
            return null;
        }
        final Synset synset = getSynset(synsetID);
        if (synset == null) {
            throw new IllegalArgumentException("Illegal synset ID " + synsetID);
        }
        final String lemma = synset.getWords()[0].getLemma();
        final POS pos = POS.getPOSForKey(getPOS(synsetID));
        try {
            final IndexWord word;
            synchronized (WordNet.class) {
                word = getDictionary().lookupIndexWord(pos, lemma);
            }
            final Synset[] senses = word.getSenses();
            for (int i = 0; i < senses.length; ++i) {
                if (senses[i].equals(synset)) {
                    return lemma + "-" + (i + 1) + pos.getKey();
                }
            }
            throw new Error("Could not determine sense index for lemma " + lemma + " and synset "
                    + synsetID);
        } catch (final JWNLException ex) {
            throw new Error(ex);
        }
        // return synset.getSenseKey(lemma); // TODO
    }

    public static String getPOS(final String synsetID) {
        Preconditions.checkNotNull(synsetID);
        final int index = synsetID.lastIndexOf('-');
        if (index == synsetID.length() - 1 || synsetID.isEmpty()) {
            throw new IllegalArgumentException("Cannot extract POS from '" + synsetID
                    + "' - invalid string");
        }
        return ""
                + Character.toLowerCase(index < 0 ? synsetID.charAt(0) : synsetID
                .charAt(index + 1));
    }

    public static long getOffset(String synsetID) {
        Preconditions.checkNotNull(synsetID);
        try {
            final int index = synsetID.lastIndexOf('-');
            if (index > 0) {
                synsetID = synsetID.substring(0, index);
            }
            return Long.parseLong(synsetID);
        } catch (final Throwable ex) {
            throw new IllegalArgumentException("Cannot extract offset from '" + synsetID + "'", ex);
        }
    }

    public static Set<String> getLemmas(final String synsetID) {
        final Set<String> lemmas = Sets.newLinkedHashSet();
        final Synset synset = getSynset(synsetID);
        if (synset != null) {
            for (final Word word : synset.getWords()) {
                lemmas.add(word.getLemma());
            }
        }
        return lemmas;
    }

    public static Set<String> getGenericSet(final String synsetID,
            final PointerType... pointerTypes) {
        final Set<String> ret = Sets.newHashSet();
        final Synset synset = getSynset(synsetID);
        if (synset != null) {
            for (final PointerType pointerType : pointerTypes) {
                for (final Pointer pointer : synset.getPointers(pointerType)) {
                    try {
                        final Synset target = pointer.getTargetSynset();
                        ret.add(getSynsetID(target.getOffset(), target.getPOS().getKey()));
                    } catch (final Throwable ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        return ret;
    }

    public static Set<String> getGenericSet(final String synsetID, final boolean recursive,
            final PointerType... pointerTypes) {
        if (!recursive) {
            return getGenericSet(synsetID, pointerTypes);
        }
        final Set<String> result = Sets.newHashSet();
        final List<String> queue = Lists.newArrayList(synsetID);
        while (!queue.isEmpty()) {
            final String id = queue.remove(0);
            if (result.add(id)) {
                queue.addAll(getGenericSet(id, pointerTypes));
            }
        }
        return result;
    }

    public static Set<String> getHypernyms(final String synsetID) {
        return getGenericSet(synsetID, PointerType.HYPERNYM);
    }

    public static Set<String> getHyponyms(final String synsetID) {
        return getGenericSet(synsetID, PointerType.HYPONYM);
    }

    public static Set<String> getHypernyms(final String synsetID, final boolean recursive) {
        return getGenericSet(synsetID, recursive, PointerType.HYPERNYM,
                PointerType.INSTANCE_HYPERNYM);
    }

    public static Set<String> getHyponims(final String synsetID, final boolean recursive) {
        return getGenericSet(synsetID, recursive, PointerType.HYPONYM,
                PointerType.INSTANCES_HYPONYM);
    }

    // returns only noun synsets
    @Nullable
    public static String mapBBNToSynset(@Nullable final String bbn) {
        return bbn == null ? null : BBN_TO_SYNSET.get(bbn.trim().toLowerCase());
    }

    // works only for noun synset
    @Nullable
    public static String mapSynsetToBBN(@Nullable final String synsetID) {
        final List<String> ids = Lists.newLinkedList();
        ids.add(synsetID);
        while (!ids.isEmpty()) {
            final String id = ids.remove(0);
            final List<String> bbns = SYNSET_TO_BBN.get(id);
            if (bbns != null && !bbns.isEmpty()) {
                return bbns.get(0); // return only first BBN in case of ambiguity
            }
            try {
                final Synset source = getSynset(id);
                final List<String> hypernymIDs = Lists.newArrayList();
                for (final PointerType type : new PointerType[] { PointerType.HYPERNYM,
                        PointerType.INSTANCE_HYPERNYM }) {
                    for (final Pointer pointer : source.getPointers(type)) {
                        final Synset target = pointer.getTargetSynset();
                        hypernymIDs.add(getSynsetID(target.getOffset(), target.getPOS().getKey()));
                    }
                }
                Collections.sort(hypernymIDs); // necessary in order to get deterministic results
                ids.addAll(hypernymIDs);
            } catch (final JWNLException ex) {
                throw new Error("Unexpected exception (!)", ex);
            }
        }
        return null;
    }

    @Nullable
    public static String mapSynsetToSST(@Nullable final String synsetID) {
        if (synsetID != null) {
            final String sst = SYNSET_TO_SST.get(synsetID);
            if (sst != null) {
                return sst;
            }
            return "B-" + getSynset(synsetID).getLexFileName();
        }
        return null;
    }

    // returns always B-noun.XXX sst
    @Nullable
    public static String mapBBNToSST(@Nullable final String bbn) {
        if (bbn != null) {
            return BBN_TO_SST.get(bbn.trim().toLowerCase());
        }
        return null;
    }

}
