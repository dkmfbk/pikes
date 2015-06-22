package eu.fbk.dkm.pikes.tintop.annotators.raw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:16
 * To change this template use File | Settings | File Templates.
 */

public class DBpediaSpotlightTag implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DBpediaSpotlightTag.class);
	private int offset;
	private String page;
	private double score;
	private String originalText;
	private int length;

	public DBpediaSpotlightTag(int offset, String page, double score, String originalText, int length) {
		this.offset = offset;
		this.page = page;
		this.score = score;
		this.originalText = originalText;
		this.length = length;
	}

	public String getOriginalText() {
		return originalText;
	}

	public void setOriginalText(String originalText) {
		this.originalText = originalText;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	@Override
	public String toString() {
		return "DBpediaSpotlightTag{" +
				"offset=" + offset +
				", page='" + page + '\'' +
				", score=" + score +
				", originalText='" + originalText + '\'' +
				", length=" + length +
				'}';
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
}
