package ixa.kaflib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 07/08/14
 * Time: 21:41
 * To change this template use File | Settings | File Templates.
 */

public class GenericId {
	private String prefix;
	private int counter = 0;
	private boolean inconsistent = false;

	public String getNext() {
		if (inconsistent) {
			throw new IllegalStateException("Inconsistent WF IDs. Can't create new WF IDs.");
		}
		return prefix + Integer.toString(++counter);
	}

	public void update(String id) {
		try {
			Integer idNum = extractCounterFromId(id);
			if (counter < idNum) {
				counter = idNum;
			}
		} catch (IllegalStateException e) {
			inconsistent = true;
		}
	}

	GenericId(String prefix) {
		this.prefix = prefix;
	}

	private static int extractCounterFromId(String id) {
		Matcher matcher = Pattern.compile("\\d+$").matcher(id);
		if (!matcher.find()) {
			throw new IllegalStateException("GenericId doesn't recognise the given id's (" + id + ") format.");
		}
		return Integer.valueOf(matcher.group(0));
	}

	public String getPrefix() {
		return prefix;
	}
}
