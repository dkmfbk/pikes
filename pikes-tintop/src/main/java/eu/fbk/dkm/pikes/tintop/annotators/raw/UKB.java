package eu.fbk.dkm.pikes.tintop.annotators.raw;

import eu.fbk.dkm.pikes.tintop.util.PikesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 08/08/14
 * Time: 15:43
 * To change this template use File | Settings | File Templates.
 */

public class UKB {

	private static final Logger LOGGER = LoggerFactory.getLogger(UKB.class);

	private OutputStream stdin = null;
	private InputStream stdout = null;
	private Process process;
	private BufferedReader brCleanUp;
	private ProcessBuilder pb;

	private Map config = new PikesProperties();
	private int numOfRestarts = 0;
	private final int MAX_NUM_OF_RESTARTS = 5;

	public UKB(Map properties) throws IOException {
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
		String[] command = {"./ukb_wsd", "--ppr", "-K", model, "-D", dict, "--allranks", "-"};
		LOGGER.trace(Arrays.toString(command));

		pb = new ProcessBuilder(command);
		pb.directory(new File(baseDir));
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);

		process = pb.start();

		stdin = process.getOutputStream();
		stdout = process.getInputStream();
		brCleanUp = new BufferedReader(new InputStreamReader(stdout));

		// This is the first line of output
		String line = brCleanUp.readLine();
	}

	private static void addTokenToContext(HashMap<String, String> term, char pos, int index, StringBuffer sb, HashMap<String, HashMap<String, String>> backupTerms) {
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

		try {
			stdin.write(transformedStr.getBytes());
			stdin.flush();
		} catch (Exception e) {
			LOGGER.warn(e.getMessage());

			if (numOfRestarts < MAX_NUM_OF_RESTARTS) {
				numOfRestarts++;
				LOGGER.info(String.format("Trying to restart UKB [%d/%d]", numOfRestarts, MAX_NUM_OF_RESTARTS));
				init();
				stdin.write(transformedStr.getBytes());
				stdin.flush();
			}
		}

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

	}
}
