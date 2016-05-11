package eu.fbk.dkm.pikes.resources.ecb;

import com.google.common.collect.HashMultimap;
import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.eval.PrecisionRecall;
import ixa.kaflib.Coref;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
                    .withOption("l", "input-lemmas", "Lemmas CSV file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, false)
                    .withOption("r", "remove-alone", "Remove alone clusters")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputCsv = cmd.getOptionValue("input-csv", File.class);
            File inputNaf = cmd.getOptionValue("input-naf", File.class);
            File inputLemmas = cmd.getOptionValue("input-lemmas", File.class);

            Boolean removeAloneClusters = cmd.hasOption("remove-alone");

            Reader in;
            Iterable<CSVRecord> records;

            HashSet<String> lemmas = null;
            if (inputLemmas != null) {
                lemmas = new HashSet<>();
                in = new FileReader(inputLemmas);
                records = CSVFormat.EXCEL.withHeader().parse(in);
                for (CSVRecord record : records) {
                    String lemma = record.get(1);
                    lemma = lemma.replaceAll("\"", "").trim();
                    if (lemma.length() > 0) {
                        lemmas.add(lemma);
                    }
                }
            }

            HashMultimap<String, String> goldTmpClusters = HashMultimap.create();
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
                    String idCluster = folder + "_" + cluster;

                    for (Span<Term> termSpan : coref.getSpans()) {
                        Term term = termSpan.getTargets().get(0);
                        String lemma = term.getLemma();
                        if (lemmas == null || lemmas.contains(lemma)) {
                            String text = folder + "_" + fileNum + "_" + term.getOffset();
                            goldTmpClusters.put(idCluster, text);
                            okEvents.add(text);
                        }
                    }
                }
            }

            Set<Set> goldClusters = new HashSet<>();
            for (String key : goldTmpClusters.keySet()) {
                Set<String> cluster = goldTmpClusters.get(key);
                if (cluster.size() > 1 || !removeAloneClusters) {
                    goldClusters.add(cluster);
                }
            }

            LOGGER.info("Gold clusters: {}", goldClusters.size());

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
                    if (okEvents.contains(id2)) {
                        tmpClusters.put(clusterID, id2);
                        clusterIndexes.put(id2, clusterID);
                    }
                    if (okEvents.contains(id1)) {
                        tmpClusters.put(clusterID, id1);
                        clusterIndexes.put(id1, clusterID);
                    }
                }
                if (index1 == null && index2 != null) {
                    if (okEvents.contains(id1)) {
                        tmpClusters.put(index2, id1);
                        clusterIndexes.put(id1, index2);
                    }
                }
                if (index2 == null && index1 != null) {
                    if (okEvents.contains(id2)) {
                        tmpClusters.put(index1, id2);
                        clusterIndexes.put(id2, index1);
                    }
                }
                if (index2 != null && index1 != null) {
                    if (!index1.equals(index2)) {
                        clusterIndexes.put(id2, index1);
                        tmpClusters.putAll(index1, tmpClusters.get(index2));
                        tmpClusters.removeAll(index2);
                    }
                }
            }

            Set<Set> clusters = new HashSet<>();
            for (Integer key : tmpClusters.keySet()) {
                Set<String> cluster = tmpClusters.get(key);
                if (cluster.size() > 1 || !removeAloneClusters) {
                    clusters.add(cluster);
                }
            }
            LOGGER.info("Classification clusters: {}", clusters.size());

            System.out.println(goldClusters);
            System.out.println(clusters);

            Map<PrecisionRecall.Measure, Double> precisionRecall = ClusteringEvaluation
                    .pairWise(clusters, goldClusters);

            System.out.println(precisionRecall);

//            Map<PrecisionRecall.Measure, Double> measureDoubleMap = ClusteringEvaluation
//                    .pairWise(goldClusters, clusters);
//            System.out.println(measureDoubleMap);

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }

}
