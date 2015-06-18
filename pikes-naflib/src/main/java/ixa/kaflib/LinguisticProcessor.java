package ixa.kaflib;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 19/08/14
 * Time: 17:39
 * To change this template use File | Settings | File Templates.
 */

public class LinguisticProcessor implements Serializable {
	String name;
	String timestamp;
	String beginTimestamp;
	String endTimestamp;
	String version;
	String layer;

	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}

	/**
	 * Returns current timestamp.
	 */
	public static String createTimestamp() {
		Date date = new Date();
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		String formattedDate = sdf.format(date);
		return formattedDate;
	}

	public LinguisticProcessor(String layer, String name) {
		this.name = name;
		this.layer = layer;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean hasTimestamp() {
		return this.timestamp != null;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public void setTimestamp() {
		String timestamp = createTimestamp();
		this.timestamp = timestamp;
	}

	public String getTimestamp() {
		return this.timestamp;
	}

	public boolean hasBeginTimestamp() {
		return beginTimestamp != null;
	}

	public void setBeginTimestamp(String timestamp) {
		this.beginTimestamp = timestamp;
	}

	public void setBeginTimestamp() {
		String timestamp = createTimestamp();
		this.beginTimestamp = timestamp;
	}

	public String getBeginTimestamp() {
		return beginTimestamp;
	}

	public boolean hasEndTimestamp() {
		return endTimestamp != null;
	}

	public void setEndTimestamp(String timestamp) {
		this.endTimestamp = timestamp;
	}

	public void setEndTimestamp() {
		String timestamp = createTimestamp();
		this.endTimestamp = timestamp;
	}

	public String getEndTimestamp() {
		return endTimestamp;
	}

	public boolean hasVersion() {
		return version != null;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

}
