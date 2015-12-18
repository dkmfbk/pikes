package eu.fbk.dkm.pikes.tintop.annotators.raw;

import eu.fbk.dkm.pikes.tintop.annotators.UKBResourcePool;
import eu.fbk.dkm.pikes.tintop.server.ResourcePool;
import eu.fbk.dkm.pikes.tintop.util.PikesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 08/08/14
 * Time: 15:43
 * To change this template use File | Settings | File Templates.
 */

public class UKB_MT {

    private static final Logger LOGGER = LoggerFactory.getLogger(UKB_MT.class);

    private Map config = new PikesProperties();
    private UKBResourcePool resourcePool = null;

    private int numOfRestarts = 0;
    private final int DEFAULT_MAX_NUM_OF_RESTARTS = 50;
    private int maxNumOfRestarts;

    public UKB_MT(Map properties) throws IOException {
        this.config = properties;
        init();
    }

    private void init() throws IOException {
        String baseDir = (String) config.get("folder");
        if (!baseDir.endsWith(File.separator)) {
            baseDir += File.separator;
        }

        String model = (String) config.get("model");
        String dict = (String) config.get("dict");

        Integer numResources = null;
        try {
            numResources = Integer.parseInt((String) config.get("instances"));
        } catch (Exception e) {
            numResources = ResourcePool.MAX_RESOURCES;
        }

        try {
            maxNumOfRestarts = Integer.parseInt((String) config.get("restarts"));
        } catch (Exception e) {
            maxNumOfRestarts = DEFAULT_MAX_NUM_OF_RESTARTS;
        }

        LOGGER.info("Loading UKB with {} instances", numResources);
        resourcePool = new UKBResourcePool(model, dict, baseDir, numResources);
    }

    private static void addTokenToContext(HashMap<String, String> term, char pos, int index, StringBuffer sb,
            HashMap<String, HashMap<String, String>> backupTerms) {
        String thisID = "w" + index;
        sb.append(term.get("lemma").toLowerCase().replace(' ', '-').replace('#', '.'));
        sb.append("#");
        sb.append(pos);
        sb.append("#");
        sb.append(thisID);
        sb.append("#1");
        sb.append(" ");
        backupTerms.put(thisID, term);
    }

    public void run(ArrayList<HashMap<String, String>> terms) throws IOException {

        HashMap<String, HashMap<String, String>> backupTerms = new HashMap<>();

        StringBuffer sb = new StringBuffer();
        sb.append("ctx_01\n");

        StringBuffer sbTokens = new StringBuffer();
        int index = 0;
        for (HashMap<String, String> t : terms) {
            switch (t.get("simple_pos").toLowerCase()) {
            case "n":
                addTokenToContext(t, 'n', ++index, sbTokens, backupTerms);
                break;
            case "r":
                addTokenToContext(t, 'n', ++index, sbTokens, backupTerms);
                break;
            case "v":
                addTokenToContext(t, 'v', ++index, sbTokens, backupTerms);
                break;
            case "a":
                addTokenToContext(t, 'r', ++index, sbTokens, backupTerms);
                break;
            case "g":
                addTokenToContext(t, 'a', ++index, sbTokens, backupTerms);
                break;
            default:
                break;
            }
        }

        if (sbTokens.toString().trim().length() == 0) {
            return;
        }

        sb.append(sbTokens.toString());

        sb.append("\n");

        // Workaround to get last line to read in the output
        sb.append("ctx_02\n");
        sb.append("be#v#workaround#1\n");

        String transformedStr = sb.toString();
        LOGGER.debug(transformedStr);

        Process process = null;

        try {
            process = resourcePool.getResource(10000);
            if (process == null) {
                throw new NullPointerException("Process is null");
            }

            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();

            try {
                stdin.write(transformedStr.getBytes());
                stdin.flush();
            } catch (IOException e) {
                if (maxNumOfRestarts <= 0 || numOfRestarts < maxNumOfRestarts) {
                    numOfRestarts++;
                    if (maxNumOfRestarts > 0) {
                        LOGGER.info(String.format("Trying to restart UKB [%d/%d]", numOfRestarts, maxNumOfRestarts));
                    } else {
                        LOGGER.info(String.format("Trying to restart UKB [%d]", numOfRestarts));
                    }
                    process = resourcePool.createResource();

                    stdin = process.getOutputStream();
                    stdout = process.getInputStream();
                    stdin.write(transformedStr.getBytes());
                    stdin.flush();
                }
            }

            BufferedReader brCleanUp = new BufferedReader(new InputStreamReader(stdout));
            String line;
            while ((line = brCleanUp.readLine()) != null) {
                LOGGER.trace(line);
                String[] parts = line.split("\\s+");
                String context = parts[0];
                if (context.equals("ctx_02")) {
                    break;
                }

                String tokenID = parts[1];
                HashMap<String, String> thisTerm = backupTerms.get(tokenID);
                if (thisTerm == null) {
                    continue;
                }

                for (int i = 2; i < parts.length; i++) {
                    String[] scores = parts[i].split("/");
                    if (scores.length < 2) {
                        break;
                    }
                    String wn = scores[0];

                    thisTerm.put("wordnet", wn);

                    break;

                }
            }

        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
            e.printStackTrace();
        } finally {
            if (process != null) {
                resourcePool.returnResource(process);
            }
        }

    }
}
