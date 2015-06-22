package eu.fbk.dkm.pikes.raid.mdfsa.structures;

import java.io.Serializable;
import java.util.ArrayList;

public class DatasetInstance implements Serializable {

  private static final long serialVersionUID = 1L;

  private String instanceId;
  private ArrayList<String> features;
  private String instanceOriginalText;
  
  /*
   * -1: negative
   *  0: neutral
   *  1: positive
   */
  private int polarity;
  private double inferredPolarity;
  private String domain;
  private String inferredDomain;
  private SentenceStructuredRepresentation ssr;
  
  public DatasetInstance() {}
  
  public DatasetInstance(ArrayList<String> features, int polarity) {
    this.features = features;
    this.polarity = polarity;
  }
  
  public DatasetInstance(String id, ArrayList<String> features, int polarity) {
    this.instanceId = id;
    this.features = features;
    this.polarity = polarity;
    this.inferredPolarity = 0.0;
  }
  
  public ArrayList<String> getFeatures() {
    return this.features;
  }
  
  public void setPolarity(int polarity) {
    this.polarity = polarity;
  }
  
  public int getPolarity() {
    return this.polarity;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public double getInferredPolarity() {
    return this.inferredPolarity;
  }

  public void setInferredPolarity(double inferredPolarity) {
    this.inferredPolarity = inferredPolarity;
  }

  public String getDomain() {
    return this.domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getInferredDomain() {
    return inferredDomain;
  }

  public void setInferredDomain(String inferredDomain) {
    this.inferredDomain = inferredDomain;
  }

  public String getInstanceOriginalText() {
    return instanceOriginalText;
  }

  public void setInstanceOriginalText(String instanceOriginalText) {
    this.instanceOriginalText = instanceOriginalText;
  }

  public SentenceStructuredRepresentation getSentenceStructuredRepresentation() {
    return ssr;
  }

  public void setSentenceStructuredRepresentation(SentenceStructuredRepresentation ssr) {
    this.ssr = ssr;
  }
}
