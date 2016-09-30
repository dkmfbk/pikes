package eu.fbk.dkm.pikes.resources.ecb;

import com.google.common.collect.HashMultimap;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Created by marcorospocher on 12/03/16.
 */
public class ECBmerger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ECBmerger.class);
    private static final Pattern mentionPattern = Pattern.compile("<([^>]*)>");
    private static final Pattern chainPattern = Pattern.compile("CHAIN=\"([0-9]+)\"");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./taol-extractor")
                    .withHeader("Add mentions to NAF folder for ECB resource")
                    .withOption("i", "input-txt", "Input TXT folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("n", "input-naf", "Input NAF folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output folder", "FOLDER",
                            CommandLine.Type.DIRECTORY, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input-txt", File.class);
            File nafFolder = cmd.getOptionValue("input-naf", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);

            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            // uncomment to get the manual mention spans
            //Pattern MY_PATTERN = Pattern.compile("\\\">[^<]*</MENTION>");

            String tags;

            int i = 0;
            for (final File file : Files.fileTreeTraverser().preOrderTraversal(inputFolder)) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }

                String path = file.getParentFile().toString();
                String folder = path.substring(path.lastIndexOf("/"));
                String local_name = folder + File.separator + file.getName();

                String naf = nafFolder + File.separator + folder + File.separator + file.getName()
                        .replace("ecb.txt", "naf.gz");

                FileInputStream bais = new FileInputStream(naf);
                GZIPInputStream gzis = new GZIPInputStream(bais);
                InputStreamReader reader = new InputStreamReader(gzis);
                BufferedReader in = new BufferedReader(reader);

                KAFDocument nafDocument = KAFDocument.createFromStream(in);

//                Map<Integer, Predicate> predicateHashMap = new HashMap<>();
//                for (Predicate predicate : nafDocument.getPredicates()) {
//                    for (Term term : predicate.getTerms()) {
//                        predicateHashMap.put(term.getOffset(), predicate);
//                    }
//                }

                Map<Integer, Term> termsHashMap = new HashMap<>();
                for (Term term : nafDocument.getTerms()) {
                    termsHashMap.put(term.getOffset(), term);
                }

                HashMultimap<String, Integer> clusterOffsets = HashMultimap.create();

                String content = FileUtils.readFileToString(file, "utf-8");

                Matcher matcher = mentionPattern.matcher(content);
                int offset = 0;
                int lastEnd = 0;
                while (matcher.find()) {
                    offset += matcher.start() - lastEnd;
                    if (!matcher.group().startsWith("</")) {
                        Matcher chainMatcher = chainPattern.matcher(matcher.group());
                        if (!chainMatcher.find()) {
                            LOGGER.error("No chain found!");
                            continue;
                        }

                        String chain = chainMatcher.group(1);
                        clusterOffsets.put(chain, offset);
                    }
                    lastEnd = matcher.end();
                }

                for (String clusterId : clusterOffsets.keySet()) {
                    Set<Integer> terms = clusterOffsets.get(clusterId);
                    List<Span<Term>> termsList = new ArrayList<>();
                    for (Integer termOffset : terms) {
                        Term term = termsHashMap.get(termOffset);
                        if (term == null) {
                            LOGGER.error("Term is null!");
                            continue;
                        }
                        Span<Term> termSpan = KAFDocument.newTermSpan();
                        termSpan.addTarget(term);
                        termsList.add(termSpan);
                    }
                    Coref coref = nafDocument.newCoref(termsList);
                    coref.setCluster(clusterId);
                    coref.setType("event-gold");
                }

                File outputFile = new File(
                        outputFolder.getAbsolutePath() + File.separator +
                                file.getAbsolutePath().substring(
                                        inputFolder.getAbsolutePath().length()).replace(".ecb.txt", ".naf"));
                Files.createParentDirs(outputFile);
                nafDocument.save(outputFile);

            }

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }

}
