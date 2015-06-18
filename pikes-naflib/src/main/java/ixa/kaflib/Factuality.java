package ixa.kaflib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Factuality layer
 */
public class Factuality implements Serializable {

	Term word;
	ArrayList<FactualityPart> factualityParts = new ArrayList<FactualityPart>();

	public Factuality(Term word) {
		this.word = word;
	}

	public Term getWord() {
		return word;
	}

	public void setWord(Term word) {
		this.word = word;
	}

	public ArrayList<FactualityPart> getFactualityParts() {
		return factualityParts;
	}

	public void addFactualityPart(FactualityPart part) {
		this.factualityParts.add(part);
	}

	public void addFactualityPart(String prediction, double confidence) {
		this.addFactualityPart(new FactualityPart(prediction, confidence));
	}

	public String getId() {
		return word.getWFs().get(0).getId();
	}

	public List<WF> getWFs() {
		return word.getWFs();
	}

	public FactualityPart getMaxPart() {
		FactualityPart ret = null;
		double base = 0;

		for (FactualityPart p : factualityParts) {
			if (p.getConfidence() > base) {
				ret = p;
				base = p.getConfidence();
			}
		}

		return ret;
	}

	public class FactualityPart {

		String prediction;
		double confidence;

		public FactualityPart(String prediction, double confidence) {
			this.prediction = prediction;
			this.confidence = confidence;
		}

		public String getPrediction() {
			return prediction;
		}

		public void setPrediction(String prediction) {
			this.prediction = prediction;
		}

		public double getConfidence() {
			return confidence;
		}

		public void setConfidence(double confidence) {
			this.confidence = confidence;
		}

		@Override
		public String toString() {
			return "FactualityPart{" +
					"prediction='" + prediction + '\'' +
					", confidence=" + confidence +
					'}';
		}
	}

}
