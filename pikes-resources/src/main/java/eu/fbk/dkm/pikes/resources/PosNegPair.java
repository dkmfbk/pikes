package eu.fbk.dkm.pikes.resources;

/**
 * Created by alessio on 21/05/15.
 */

public class PosNegPair {

	private Double posScore;
	private Double negScore;

	public PosNegPair(Double posScore, Double negScore) {
		this.posScore = posScore;
		this.negScore = negScore;
	}

	public Double getPosScore() {
		return posScore;
	}

	public Double getNegScore() {
		return negScore;
	}

	@Override
	public String toString() {
		return "eu.fbk.dkm.pikes.resources.PosNegPair{" +
				"posScore=" + posScore +
				", negScore=" + negScore +
				'}';
	}
}
