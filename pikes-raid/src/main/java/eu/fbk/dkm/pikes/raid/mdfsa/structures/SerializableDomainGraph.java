package eu.fbk.dkm.pikes.raid.mdfsa.structures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class SerializableDomainGraph implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private Properties prp;
  private Graph graph;
  private HashMap<Long, FuzzyMembership> polarities;
  private HashMap<Long, ArrayList<Double>> conceptsConvergenceIterationsValues;
  private HashMap<Long, Double> startPolarities;
  private HashMap<Long, Double> tempInDomainStartPolarities;
  private HashMap<Long, Double> tempOutDomainStartPolarities;
  private HashMap<Long, Double> currentPolarities;
  private HashMap<Long, Double> tokensCounter;
  private HashMap<Long, Double> inDomainTokensCounter;
  private HashMap<Long, Double> outDomainTokensCounter;
  private double currentGraphConvergenceValue;
  private double currentAveragePolarity;
  private int iteration;
  private double propagationRate;
  private double convergenceLimit;
  private double deadzone;
  private double annealingRate;
  
  public SerializableDomainGraph() {}

  public Properties getPrp() {
    return prp;
  }

  public void setPrp(Properties prp) {
    this.prp = prp;
  }

  public Graph getGraph() {
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  public HashMap<Long, FuzzyMembership> getPolarities() {
    return polarities;
  }

  public void setPolarities(HashMap<Long, FuzzyMembership> polarities) {
    this.polarities = polarities;
  }

  public HashMap<Long, ArrayList<Double>> getConceptsConvergenceIterationsValues() {
    return conceptsConvergenceIterationsValues;
  }

  public void setConceptsConvergenceIterationsValues(HashMap<Long, ArrayList<Double>> conceptsConvergenceIterationsValues) {
    this.conceptsConvergenceIterationsValues = conceptsConvergenceIterationsValues;
  }

  public HashMap<Long, Double> getStartPolarities() {
    return startPolarities;
  }

  public void setStartPolarities(HashMap<Long, Double> startPolarities) {
    this.startPolarities = startPolarities;
  }

  public HashMap<Long, Double> getTempInDomainStartPolarities() {
    return tempInDomainStartPolarities;
  }

  public void setTempInDomainStartPolarities(HashMap<Long, Double> tempInDomainStartPolarities) {
    this.tempInDomainStartPolarities = tempInDomainStartPolarities;
  }

  public HashMap<Long, Double> getTempOutDomainStartPolarities() {
    return tempOutDomainStartPolarities;
  }

  public void setTempOutDomainStartPolarities(HashMap<Long, Double> tempOutDomainStartPolarities) {
    this.tempOutDomainStartPolarities = tempOutDomainStartPolarities;
  }

  public HashMap<Long, Double> getCurrentPolarities() {
    return currentPolarities;
  }

  public void setCurrentPolarities(HashMap<Long, Double> currentPolarities) {
    this.currentPolarities = currentPolarities;
  }

  public HashMap<Long, Double> getTokensCounter() {
    return tokensCounter;
  }

  public void setTokensCounter(HashMap<Long, Double> tokensCounter) {
    this.tokensCounter = tokensCounter;
  }

  public HashMap<Long, Double> getInDomainTokensCounter() {
    return inDomainTokensCounter;
  }

  public void setInDomainTokensCounter(HashMap<Long, Double> inDomainTokensCounter) {
    this.inDomainTokensCounter = inDomainTokensCounter;
  }

  public HashMap<Long, Double> getOutDomainTokensCounter() {
    return outDomainTokensCounter;
  }

  public void setOutDomainTokensCounter(HashMap<Long, Double> outDomainTokensCounter) {
    this.outDomainTokensCounter = outDomainTokensCounter;
  }

  public double getCurrentGraphConvergenceValue() {
    return currentGraphConvergenceValue;
  }

  public void setCurrentGraphConvergenceValue(double currentGraphConvergenceValue) {
    this.currentGraphConvergenceValue = currentGraphConvergenceValue;
  }

  public double getCurrentAveragePolarity() {
    return currentAveragePolarity;
  }

  public void setCurrentAveragePolarity(double currentAveragePolarity) {
    this.currentAveragePolarity = currentAveragePolarity;
  }

  public int getIteration() {
    return iteration;
  }

  public void setIteration(int iteration) {
    this.iteration = iteration;
  }

  public double getPropagationRate() {
    return propagationRate;
  }

  public void setPropagationRate(double propagationRate) {
    this.propagationRate = propagationRate;
  }

  public double getConvergenceLimit() {
    return convergenceLimit;
  }

  public void setConvergenceLimit(double convergenceLimit) {
    this.convergenceLimit = convergenceLimit;
  }

  public double getDeadzone() {
    return deadzone;
  }

  public void setDeadzone(double deadzone) {
    this.deadzone = deadzone;
  }

  public double getAnnealingRate() {
    return annealingRate;
  }

  public void setAnnealingRate(double annealingRate) {
    this.annealingRate = annealingRate;
  }

  public static long getSerialversionuid() {
    return serialVersionUID;
  }
}
