package ixa.kaflib;

import java.io.Serializable;

/**
 * An XML comment. Methods allow the user to get and set the text of the
 * comment.
 *
 * @author  Brett McLaughlin
 * @author  Jason Hunter
 */
public class Comment extends org.jdom2.Comment implements Serializable {

	public Comment(String text) {
		super();

		text = text.trim();
		text = text.replaceAll("-+", "-");
		if (text.endsWith("-")) {
			text += " .";
		}

		setText(text);
	}

}