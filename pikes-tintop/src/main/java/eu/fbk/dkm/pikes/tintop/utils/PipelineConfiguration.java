package eu.fbk.dkm.pikes.tintop.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 07/08/14
 * Time: 10:29
 * To change this template use File | Settings | File Templates.
 */
public class PipelineConfiguration {
	private static PipelineConfiguration ourInstance = null;
	private Properties properties = new Properties();

	public static PipelineConfiguration getInstance() {
		return ourInstance;
	}

	public Properties getProperties() {
		return properties;
	}

	public static PipelineConfiguration getInstance(String configurationFile) throws IOException {
		ourInstance = new PipelineConfiguration(configurationFile);

		return ourInstance;
	}

	private PipelineConfiguration(String configurationFile) throws IOException {
		InputStream input = new FileInputStream(configurationFile);
		properties.load(input);
	}

	private PipelineConfiguration() {
		// Exists only to avoid instantiation.
	}
}
