package eu.fbk.dkm.pikes.resources;

import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by alessio on 21/05/15.
 */

public class SentiWordNet {

	private static final Logger LOGGER = LoggerFactory.getLogger(SentiWordNet.class);

	private static File path = null;
	private static HashMap<String, PosNegPair> values = null;

	public static void setPath(File path) {
		SentiWordNet.path = path;
	}

	public static void init() throws IOException {
		if (values == null && path != null) {
			values = new HashMap<>();

			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 4) {
					continue;
				}

				String pos = parts[0];
				String id = parts[1];
				Double posScore = Double.parseDouble(parts[2]);
				Double negScore = Double.parseDouble(parts[3]);

				String wnID = id + "-" + pos;

				values.put(wnID, new PosNegPair(posScore, negScore));
			}
			reader.close();
		}
	}

	public static PosNegPair searchValue(String wnID) {
		try {
			init();
		} catch (Exception e) {
			// continue...
		}

		if (values == null) {
			return null;
		}

		return values.get(wnID);
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("sentiwordnet-loader")
					.withHeader(
							"Produces NAF files, a TSV file with sentiment annotations "
									+ "and an HTML file with annotated sentences "
									+ "starting from the MPQA v.2 corpus")
					.withOption("i", "input", "the corpus file", "DIR",
							CommandLine.Type.FILE_EXISTING, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputFile = cmd.getOptionValue("input", File.class);

			SentiWordNet.setPath(inputFile);
			SentiWordNet.init();
			System.out.println(SentiWordNet.searchValue("00478311-a"));

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}
}
