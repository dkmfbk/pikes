package eu.fbk.dkm.pikes.tintop.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by alessio on 16/03/15.
 */

public class GenericCommandLine {

	static Logger logger = Logger.getLogger(GenericCommandLine.class.getName());

	public static ArrayList<String> genericRun(String baseDir, String... command) throws IOException, InterruptedException {
		logger.debug(Arrays.toString(command));

		ArrayList<String> ret = new ArrayList<>();

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(new File(baseDir));
		final Process p = pb.start();

		String line;

		BufferedReader bri = new BufferedReader
				(new InputStreamReader(p.getInputStream()));
		while ((line = bri.readLine()) != null) {
			ret.add(line);
		}
		bri.close();

		p.waitFor();

		return ret;
	}


}
