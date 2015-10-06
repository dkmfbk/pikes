package eu.fbk.shell.mdfsa.data.structures;

import java.io.Serializable;

public class DomainEdge implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private long nodeId;
  private double weight;
  
  /*
   * 0: SenticNet
   * 1: WordNet
   * 2: SN->WN
   */
  private int creator;
  
  public DomainEdge(long nodeId, double weight, int creator) {
    this.nodeId = nodeId;
    this.weight = weight;
    this.creator = creator;
  }

  public long getNodeId() {
    return nodeId;
  }

  public void setNodeId(long nodeId) {
    this.nodeId = nodeId;
  }

  public double getWeight() {
    return weight;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public int getCreator() {
    return creator;
  }

  public void setCreator(int creator) {
    this.creator = creator;
  }
  
}
