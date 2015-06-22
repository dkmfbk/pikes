package eu.fbk.dkm.pikes.raid.mdfsa.wordnet;

public class WordNetRelation {

  private int relationType;
  private long targetSynset;
  private double relationWeight;
  
  public WordNetRelation(int r, long t, double w) {
    this.relationType = r;
    this.targetSynset = t;
    this.relationWeight = w;
  }

  public int getRelationType() {
    return relationType;
  }

  public void setRelationType(int relationType) {
    this.relationType = relationType;
  }

  public long getTargetSynset() {
    return targetSynset;
  }

  public void setTargetSynset(long targetSynset) {
    this.targetSynset = targetSynset;
  }

  public double getRelationWeight() {
    return relationWeight;
  }

  public void setRelationWeight(double relationWeight) {
    this.relationWeight = relationWeight;
  }
}
