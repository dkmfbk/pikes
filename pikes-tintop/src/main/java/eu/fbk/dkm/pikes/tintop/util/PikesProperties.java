package eu.fbk.dkm.pikes.tintop.util;

import org.ini4j.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by alessio on 27/05/15.
 */

public class PikesProperties extends Options {

	private static Character separator = '.';

	public static Character getSeparator() {
		return separator;
	}

	public static void setSeparator(Character separator) {
		PikesProperties.separator = separator;
	}

	/**
	 * Creates an empty property list with no default values.
	 */
	public PikesProperties() {
		super();
	}

	public PikesProperties(Properties defaults) throws IOException {
		super();

		for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
			if (entry.getKey() instanceof String) {
				put(entry.getKey().toString(), entry.getValue());
			}
		}

	}

	public PikesProperties(File input) throws IOException {
		super(input);
	}

	public PikesProperties(URL input) throws IOException {
		super(input);
	}

	public PikesProperties(InputStream input) throws IOException {
		super(input);
	}

	public PikesProperties(Reader input) throws IOException {
		super(input);
	}

	public boolean isTrue(String key) {
		Object value = get(key);
		if (value == null) {
			return false;
		}

		if (value.toString() == null) {
			return false;
		}

		if (value.toString().length() == 0) {
			return false;
		}

		if (value.toString().equals("0")) {
			return false;
		}

		if (value.toString().toLowerCase().equals("false")) {
			return false;
		}

		return true;
	}

	public boolean isFalse(String key) {
		return !isTrue(key);
	}

	public PikesProperties filter(String prefix) {
		PikesProperties ret = new PikesProperties();
		for (Map.Entry<String, String> entry : entrySet()) {
			String key = entry.getKey();
			String[] parts = key.split(Pattern.quote(separator.toString()));
			if (parts.length < 1) {
				continue;
			}

			if (parts[0].equals(prefix)) {
				String newKey = key.substring(prefix.length() + separator.toString().length());
				ret.put(newKey, entry.getValue());
			}
		}

		return ret;
	}

	public Properties toProperties() {
		Properties ret = new Properties();
		for (Map.Entry<String, String> entry : entrySet()) {
			ret.put(entry.getKey(), entry.getValue());
		}
		return ret;
	}
}
