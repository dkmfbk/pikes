package eu.fbk.dkm.pikes.raid;

import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by alessio on 08/05/15.
 */

public class SimpleEvaluation {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SimpleEvaluation.class);

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("yamcha-evaluator")
					.withHeader("Evaluate YAMCHA classification")
					.withOption("i", "input-file", "the test file annotated", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
//					.withOption("g", "gold", "gold column (starting from 0)", "NUM", CommandLine.Type.POSITIVE_INTEGER, true, false, true)
//					.withOption("t", "test", "test column (starting from 0)", "NUM", CommandLine.Type.POSITIVE_INTEGER, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File testFile = cmd.getOptionValue("i", File.class);
			BufferedReader reader = new BufferedReader(new FileReader(testFile));

			String line;

			int total = 0;
			int correct = 0;

			while ((line = reader.readLine()) != null) {

				String[] parts = line.split("\\s");
				if (parts.length < 2) {
					continue;
				}

				int testCol = parts.length - 1;
				int goldCol = parts.length - 2;

				total++;
				if (parts[testCol].equals(parts[goldCol])) {
					correct++;
				}
			}

			reader.close();

			System.out.println("Results: " + correct + "/" + total);
		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}
}
