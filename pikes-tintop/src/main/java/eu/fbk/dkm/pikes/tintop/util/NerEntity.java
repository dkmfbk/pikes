package eu.fbk.dkm.pikes.tintop.util;

import org.apache.log4j.Logger;

import edu.stanford.nlp.stats.Counter;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 18/07/14
 * Time: 16:04
 * To change this template use File | Settings | File Templates.
 */

public class NerEntity {

	static Logger logger = Logger.getLogger(NerEntity.class.getName());

	private String label;
    private Counter<String> scoredLabels = null;
	private int startToken;
	private int endToken;
	private String normalizedValue = null;

	public NerEntity(String label, int startToken) {
		this.label = label;
		this.startToken = startToken;
		this.endToken = startToken;
	}

	public NerEntity(String label, int startToken, String normalizedValue) {
		this.label = label;
		this.startToken = startToken;
		this.endToken = startToken;
		this.normalizedValue = normalizedValue;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
    public Counter<String> getScoredLabels() {
        return scoredLabels;
    }
    
    public void setScoredLabels(Counter<String> scoredLabels) {
        this.scoredLabels = scoredLabels;
    }
    
	public int getStartToken() {
		return startToken;
	}

	public void setStartToken(int startToken) {
		this.startToken = startToken;
	}

	public int getEndToken() {
		return endToken;
	}

	public void setEndToken(int endToken) {
		this.endToken = endToken;
	}

	public String getNormalizedValue() {
		return normalizedValue;
	}

	public void setNormalizedValue(String normalizedValue) {
		this.normalizedValue = normalizedValue;
	}

	@Override
	public String toString() {
		return "NerEntity{" +
				"label='" + label + '\'' +
				", startToken=" + startToken +
				", endToken=" + endToken +
				", normalizedValue='" + normalizedValue + '\'' +
				'}';
	}
}
