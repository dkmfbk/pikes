package eu.fbk.dkm.pikes.resources.ecb;

import com.google.common.collect.HashMultimap;
import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.Coref;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marcorospocher on 12/03/16.
 */
public class ECBevaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ECBevaluator.class);
    private static final Pattern tokenPattern = Pattern.compile("/([0-9]+)/([0-9])\\.ecb#char=([0-9]+)");
    //    private static final Boolean removeAloneClusters = false;
//    private static final Pattern chainPattern = Pattern.compile("CHAIN=\"([0-9]+)\"");
    private static Integer FOLDER = null;

    public static void printToken(Appendable writer, Term token, int i, String last) throws IOException {
        writer.append(String.format("%d", i)).append("\t");
        writer.append(token.getForm()).append("\t");
        writer.append("_").append("\t");
        writer.append(token.getForm()).append("\t");
        writer.append("_").append("\t");
        writer.append(token.getMorphofeat()).append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append("_").append("\t");
        writer.append(last);
        writer.append("\n");

    }

    public static void main(String[] args) {
        try {

            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./ecb-evaluator")
                    .withHeader("Evaluator event extractor")
                    .withOption("n", "input-naf", "Input NAF folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("i", "input-csv", "Input CSV file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("g", "output-gold", "Output gold file", "FILE",
                            CommandLine.Type.FILE, true, false, true)
                    .withOption("b", "output-baseline", "Output baseline file", "FILE",
                            CommandLine.Type.FILE, true, false, true)
                    .withOption("o", "output", "Output file", "FILE",
                            CommandLine.Type.FILE, true, false, true)
                    .withOption("l", "input-lemmas", "Lemmas CSV file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, false)
//                    .withOption("r", "remove-alone", "Remove alone clusters")
                    .withOption("c", "check-gold", "Use only events annotated in gold standard")
                    .withOption("s", "add-single", "Add single clusters")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputCsv = cmd.getOptionValue("input-csv", File.class);
            File inputNaf = cmd.getOptionValue("input-naf", File.class);
            File inputLemmas = cmd.getOptionValue("input-lemmas", File.class);

            File outputGold = cmd.getOptionValue("output-gold", File.class);
            File outputBaseline = cmd.getOptionValue("output-baseline", File.class);
            File output = cmd.getOptionValue("output", File.class);

//            Boolean removeAloneClusters = cmd.hasOption("remove-alone");
            Boolean checkGold = cmd.hasOption("check-gold");
            Boolean addSingleClusters = cmd.hasOption("add-single");

            Reader in;
            Iterable<CSVRecord> records;

            HashMap<String, Integer> lemmas = null;
            int lemmaIndex = 0;
            if (inputLemmas != null) {
                lemmas = new HashMap<>();
                in = new FileReader(inputLemmas);
                records = CSVFormat.EXCEL.withHeader().parse(in);
                for (CSVRecord record : records) {
                    String lemma = record.get(1);
                    lemma = lemma.replaceAll("\"", "").trim();
                    if (lemma.length() > 0) {
                        lemmas.put(lemma, ++lemmaIndex);
                    }
                }
            }

            LOGGER.info("Lemmas: {}", lemmas.size());

            BufferedWriter goldWriter = new BufferedWriter(new FileWriter(outputGold));
            BufferedWriter baselineWriter = new BufferedWriter(new FileWriter(outputBaseline));
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));

            HashMultimap<String, String> goldTmpClusters = HashMultimap.create();
            HashMap<String, String> goldClusters = new HashMap<>();
            Set<String> okEvents = new HashSet<>();

            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputNaf)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                String path = file.getParentFile().toString();
                Integer folder = Integer.parseInt(path.substring(path.lastIndexOf("/")).substring(1));
                Integer fileNum = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));

                if (FOLDER != null && !folder.equals(FOLDER)) {
                    continue;
                }

                LOGGER.debug(file.getAbsolutePath());
                KAFDocument document = KAFDocument.createFromFile(file);

                for (Coref coref : document.getCorefs()) {
                    if (coref.getType() == null) {
                        continue;
                    }
                    if (!coref.getType().equals("event-gold")) {
                        continue;
                    }

                    Integer cluster = Integer.parseInt(coref.getCluster());
                    String idCluster = String.valueOf(1000 * folder + cluster);

                    for (Span<Term> termSpan : coref.getSpans()) {
                        Term term = termSpan.getTargets().get(0);
                        String lemma = term.getLemma();
                        if (lemmas == null || lemmas.containsKey(lemma)) {
                            String text = folder + "_" + fileNum + "_" + term.getOffset();
                            goldTmpClusters.put(idCluster, text);
                            goldClusters.put(text, idCluster);
                            okEvents.add(text);
                        }
                    }
                }

                goldWriter.append(String.format("#begin document %d_%d", folder, fileNum)).append("\n");
                baselineWriter.append(String.format("#begin document %d_%d", folder, fileNum)).append("\n");

                Integer numSentences = document.getNumSentences();
                for (int i = 1; i <= numSentences; i++) {

                    boolean useThis = false;
                    StringBuilder goldBuilder = new StringBuilder();
                    StringBuilder baselineBuilder = new StringBuilder();

                    List<Term> sentenceTerms = document.getSentenceTerms(i);
                    int n = 0;
                    for (Term token : sentenceTerms) {
                        String id = String.format("%d_%d_%d", folder, fileNum, token.getOffset());
                        String last;
                        n++;

                        last = "_";
                        if (goldClusters.containsKey(id)) {
                            last = String.format("(%s)", goldClusters.get(id));
                            useThis = true;
                        }
                        printToken(goldBuilder, token, n, last);

                        last = "_";
                        String lemma = token.getLemma();
                        if (goldClusters.containsKey(id) && lemmas.containsKey(lemma)) {
                            last = String.format("(%d)", lemmas.get(lemma));
                        }
                        printToken(baselineBuilder, token, n, last);
                    }

                    goldBuilder.append("\n");
                    baselineBuilder.append("\n");

                    if (useThis) {
                        goldWriter.append(goldBuilder.toString());
                        baselineWriter.append(baselineBuilder.toString());
                    }
                }
            }

            goldWriter.close();
            baselineWriter.close();

//            Set<Set> goldClusters = new HashSet<>();
//            for (String key : goldTmpClusters.keySet()) {
//                Set<String> cluster = goldTmpClusters.get(key);
//                if (cluster.size() > 1 || !removeAloneClusters) {
//                    goldClusters.add(cluster);
//                }
//            }

//            LOGGER.info("Gold clusters: {}", goldClusters.size());

            in = new FileReader(inputCsv);
            records = CSVFormat.EXCEL.withHeader().parse(in);

            // Size must be always 4!
            int clusterID = 0;
            HashMap<String, Integer> clusterIndexes = new HashMap<>();
            HashMultimap<Integer, String> tmpClusters = HashMultimap.create();
            for (CSVRecord record : records) {
                Matcher matcher;

                String id1 = null;
                String id2 = null;
                matcher = tokenPattern.matcher(record.get(1));
                if (matcher.find()) {
                    id1 = matcher.group(1) + "_" + matcher.group(2) + "_" + matcher.group(3);
                }
                matcher = tokenPattern.matcher(record.get(3));
                if (matcher.find()) {
                    id2 = matcher.group(1) + "_" + matcher.group(2) + "_" + matcher.group(3);
                }

                Integer index1 = clusterIndexes.get(id1);
                Integer index2 = clusterIndexes.get(id2);

                if (index1 == null && index2 == null) {
                    clusterID++;
                    if (!checkGold || okEvents.contains(id2)) {
                        if (id2 != null) {
                            tmpClusters.put(clusterID, id2);
                            clusterIndexes.put(id2, clusterID);
                        }
                    }
                    if (!checkGold || okEvents.contains(id1)) {
                        if (id1 != null) {
                            tmpClusters.put(clusterID, id1);
                            clusterIndexes.put(id1, clusterID);
                        }
                    }
                }
                if (index1 == null && index2 != null) {
                    if (!checkGold || okEvents.contains(id1)) {
                        if (id1 != null) {
                            tmpClusters.put(index2, id1);
                            clusterIndexes.put(id1, index2);
                        }
                    }
                }
                if (index2 == null && index1 != null) {
                    if (!checkGold || okEvents.contains(id2)) {
                        if (id2 != null) {
                            tmpClusters.put(index1, id2);
                            clusterIndexes.put(id2, index1);
                        }
                    }
                }
                if (index2 != null && index1 != null) {
                    if (!index1.equals(index2)) {
                        if (id2 != null) {
                            clusterIndexes.put(id2, index1);
                            tmpClusters.putAll(index1, tmpClusters.get(index2));
                            tmpClusters.removeAll(index2);
                        }
                    }
                }
            }

            System.out.println(tmpClusters);

            int otherClusterID = 10000;
            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputNaf)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                String path = file.getParentFile().toString();
                Integer folder = Integer.parseInt(path.substring(path.lastIndexOf("/")).substring(1));
                Integer fileNum = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));

                LOGGER.debug(file.getAbsolutePath());
                KAFDocument document = KAFDocument.createFromFile(file);

                if (FOLDER != null && !folder.equals(FOLDER)) {
                    continue;
                }

                writer.append(String.format("#begin document %d_%d", folder, fileNum)).append("\n");
                Integer numSentences = document.getNumSentences();
                for (int i = 1; i <= numSentences; i++) {

                    boolean useThis = false;
                    StringBuilder outBuilder = new StringBuilder();

                    List<Term> sentenceTerms = document.getSentenceTerms(i);
                    int n = 0;
                    for (Term token : sentenceTerms) {
                        String id = String.format("%d_%d_%d", folder, fileNum, token.getOffset());
                        if (okEvents.contains(id)) {
                            useThis = true;
                        }
                        String last;
                        if (clusterIndexes.containsKey(id)) {
                            last = String.format("(%s)", clusterIndexes.get(id));
                        } else {
                            if (okEvents.contains(id) && addSingleClusters) {
                                last = String.format("(%d)", ++otherClusterID);
                            } else {
                                last = "_";
                            }
                        }
                        printToken(outBuilder, token, ++n, last);
                    }

                    outBuilder.append("\n");

                    if (useThis) {
                        writer.append(outBuilder.toString());
                    }
                }

            }
            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);

        }
    }

}
