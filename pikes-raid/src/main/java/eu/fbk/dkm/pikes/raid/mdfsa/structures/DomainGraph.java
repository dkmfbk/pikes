package eu.fbk.dkm.pikes.raid.mdfsa.structures;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class DomainGraph {
    
  private String id;
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
  private Iterator<Long> nodeIterator;
  private int iteration;
  private double propagationRate;
  private double convergenceLimit;
  private double deadzone;
  private double annealingRate;
  
  
  public DomainGraph(Properties prp) {
    this.prp = prp;
  }
  
  
  public DomainGraph(String id, Properties prp, Graph g, double p, double c, double d, double a) {
    this.id = id;
    this.prp = prp;
    this.graph = g;
    this.polarities = new HashMap<Long, FuzzyMembership>();
    this.conceptsConvergenceIterationsValues = new HashMap<Long, ArrayList<Double>>();
    this.startPolarities = new HashMap<Long, Double>();
    this.tempInDomainStartPolarities = new HashMap<Long, Double>();
    this.tempOutDomainStartPolarities = new HashMap<Long, Double>();
    this.currentPolarities = new HashMap<Long, Double>();
    this.tokensCounter = new HashMap<Long, Double>();
    this.inDomainTokensCounter = new HashMap<Long, Double>();
    this.outDomainTokensCounter = new HashMap<Long, Double>();
    this.currentGraphConvergenceValue = 0.0;
    this.currentAveragePolarity = 0.0;
    this.propagationRate = p;
    this.convergenceLimit = c;
    this.deadzone = d;
    this.annealingRate = a;
  }
  
  
  /**
   * Initializes the graph with the polarities representing the sentiment of each concept in the current domain
   * @param instances
   */
  public void polarityInitialization(ArrayList<DatasetInstance> instances) {
    
    /*
     * Puts the dataset information in the starting maps
     */
    //System.out.println("Reading dataset polarities.");
    for(DatasetInstance di: instances) {
      ArrayList<String> features = di.getFeatures();
      int polarity = di.getPolarity();
      
      for(String currentFeature: features) {
        
        ArrayList<Long> featureIds = this.graph.getFeatureIds(currentFeature);
        if(featureIds != null) {
          for(Long featureId: featureIds) {
            Double currentPolarity = this.startPolarities.get(featureId);
            if(currentPolarity == null) {
              currentPolarity = new Double(polarity);
              this.startPolarities.put(featureId, currentPolarity);
              this.currentPolarities.put(featureId, currentPolarity);
            } else {
              currentPolarity += polarity;
              this.startPolarities.put(featureId, currentPolarity);
              this.currentPolarities.put(featureId, currentPolarity);
            }
            
            Double currentTokensCounter = this.tokensCounter.get(featureId);
            if(currentTokensCounter == null) {
              currentTokensCounter = new Double(1.0);
              this.tokensCounter.put(featureId, currentTokensCounter);
            } else {
              currentTokensCounter++;
              this.tokensCounter.put(featureId, currentTokensCounter);
            }
          }
        }
      }
    }
    
    
    /*
     * Computes the starting polarities of each feature
     */
    //System.out.println("Computing starting polarities and creating starting fuzzy membership functions.");
    for(long featureId: this.startPolarities.keySet()) {
      double startPolarity = (double) this.startPolarities.get(featureId) / (double) this.tokensCounter.get(featureId);
      this.startPolarities.put(featureId, startPolarity);
      this.currentPolarities.put(featureId, startPolarity);
      FuzzyMembership fm = new FuzzyMembership(-1.0, startPolarity, startPolarity, 1.0);
      this.polarities.put(featureId, fm);
    }
  }
  
  
  
  
  
  /**
   * Initializes the graph with the polarities representing the sentiment of each concept in the current domain
   * @param instances
   */
  public void forcedPolarityInitialization(String concept, FuzzyMembership fm) {
    
    /*
     * Puts the dataset information in the starting maps
     */
    ArrayList<Long> featureIds = this.graph.getFeatureIds(concept);
    if(featureIds != null) {
      for(Long featureId: featureIds) {
        this.polarities.put(featureId, fm);
      }
    }
  }
  
  
  
  
  
  
  
  
  
  
  /**
   * Initializes the graph with truth value related to the belonging of each concept to the current domain
   * @param datasets the list of the domains
   * @param instances the list of the training instances for each domain
   * @param currentDomain the current domain
   */
  public void domainInitialization(String[] datasets, HashMap<String, ArrayList<DatasetInstance>> allInstances, String currentDomain) {
    
    /*
     * Statistics variables
     */
    int featuresOverlap = 0;
    
    
    /*
     * Puts the dataset information in the starting maps
     */
    //System.out.println("Reading dataset polarities.");
    for(String currentDataset: datasets) {
      double polarity = 0.0;
      if(currentDataset.compareTo(currentDomain) == 0) {
        polarity = 1.0;
      } else {
        continue;
        //polarity = -0.01 / (datasets.length - 1);
        //polarity = -1.0;
      }
      
      ArrayList<DatasetInstance> domainInstances = allInstances.get(currentDataset);
      
      for(DatasetInstance di: domainInstances) {
        ArrayList<String> features = di.getFeatures();
        for(String currentFeature: features) {
          
          ArrayList<Long> featureIds = this.graph.getFeatureIds(currentFeature);
          if(featureIds != null) {
            for(Long featureId: featureIds) {

              /*
               * Generates the default values for the polarity maps
               */
              Double currentPolarity = this.startPolarities.get(featureId);
              if(currentPolarity == null) {
                currentPolarity = new Double(polarity);
                this.startPolarities.put(featureId, currentPolarity);
                this.currentPolarities.put(featureId, currentPolarity);
              } else {
                currentPolarity += polarity;
                this.startPolarities.put(featureId, currentPolarity);
                this.currentPolarities.put(featureId, currentPolarity);
              }
              
              
              /*
               * Manages the in-domain and out-domain maps for the computation of the starting polarity values.
               * The two different maps are used in order to manage separately the contributions coming from features
               * defined in the in-domain and out-domain instances.
               */
              if(polarity > 0.0) {
                Double currentTokensCounter = this.inDomainTokensCounter.get(featureId);
                currentPolarity = this.tempInDomainStartPolarities.get(featureId);
                if(currentPolarity == null) {
                  currentPolarity = new Double(polarity);
                  this.tempInDomainStartPolarities.put(featureId, currentPolarity);
                } else {
                  currentPolarity += polarity;
                  this.tempInDomainStartPolarities.put(featureId, currentPolarity);
                }
                if(currentTokensCounter == null) {
                  currentTokensCounter = new Double(1.0);
                  this.inDomainTokensCounter.put(featureId, currentTokensCounter);
                } else {
                  currentTokensCounter++;
                  this.inDomainTokensCounter.put(featureId, currentTokensCounter);
                }
              } else {
                Double currentTokensCounter = this.outDomainTokensCounter.get(featureId);
                currentPolarity = this.tempOutDomainStartPolarities.get(featureId);
                if(currentPolarity == null) {
                  currentPolarity = new Double(polarity);
                  this.tempOutDomainStartPolarities.put(featureId, currentPolarity);
                } else {
                  currentPolarity += polarity;
                  this.tempOutDomainStartPolarities.put(featureId, currentPolarity);
                }
                if(currentTokensCounter == null) {
                  currentTokensCounter = new Double(1.0);
                  this.outDomainTokensCounter.put(featureId, currentTokensCounter);
                } else {
                  currentTokensCounter++;
                  this.outDomainTokensCounter.put(featureId, currentTokensCounter);
                }
              }
              
            }
          }
        }
      }
    }
    
    
    
    /*
     * Computes the starting polarities of each feature
     */
    //System.out.println("Computing starting polarities and creating starting fuzzy membership functions.");
    for(long featureId: this.startPolarities.keySet()) {
      double inDomainPolarity;
      double outDomainPolarity;
      try {
        inDomainPolarity = (double) this.tempInDomainStartPolarities.get(featureId) / 
                           (double) this.inDomainTokensCounter.get(featureId);
      } catch (NullPointerException e) {
        inDomainPolarity = 0.0;
      }
      try {
        outDomainPolarity = (double) this.tempOutDomainStartPolarities.get(featureId) / 
                            (double) this.outDomainTokensCounter.get(featureId);
      } catch (NullPointerException e) {
        outDomainPolarity = 0.0;
      }
      
      if(inDomainPolarity > 0.0 && outDomainPolarity < 0.0) {
        featuresOverlap++;
      }
      
      double startPolarity = inDomainPolarity + outDomainPolarity;
      //double startPolarity = inDomainPolarity;
      this.startPolarities.put(featureId, startPolarity);
      this.currentPolarities.put(featureId, startPolarity);
      FuzzyMembership fm = new FuzzyMembership(-1.0, startPolarity, startPolarity, 1.0);
      this.polarities.put(featureId, fm);
    }
    //System.out.println(this.tempInDomainStartPolarities.size() + " - " + this.tempOutDomainStartPolarities.size() + " - " +
    //                   featuresOverlap);
  }
  
  
  
  
  
  
  
  
  
  /**
   * Computes the fuzzy membership functions of each node of the graph starting from the values read in the dataset
   * and by propagating them through the entire graph
   */
  public void polaritiesPropagation() {
    this.iteration = 0;
    
    /* Prints some starting graph statistics */
    int positive = 0;
    int negative = 0;
    int neutral = 0;
    for(long featureId: this.currentPolarities.keySet()) {
      double currentPolarity = this.currentPolarities.get(featureId);
      if(currentPolarity > 0.0) {
        positive++;
      } else if(currentPolarity < 0.0) {
        negative++;
      } else {
        neutral++;
      }
    }
    //System.out.println("Positive: " + positive + " - Negative: " + negative + " - Neutral: " + neutral);
    
    
    do {
      this.nodeIterator = this.graph.getLabels().values().iterator();
      int threadNumber = Integer.valueOf((String) this.prp.get("mdfsa.tasks"));
      this.currentGraphConvergenceValue = 0.0;
      this.currentAveragePolarity = 0.0;
      NodeElaborator[] ne = new NodeElaborator[threadNumber];
     
      for(int i = 0; i < threadNumber; i++) {
        ne[i] = new NodeElaborator(this, i);
        ne[i].start();
      }
      for(int i = 0; i < threadNumber; i++) {
        try {
          ne[i].join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
          
      //System.out.println(this.currentGraphConvergenceValue);
      this.iteration++;
      this.propagationRate *= this.annealingRate;
      this.applyRepulsionFactor();
      //System.out.println(this.currentGraphConvergenceValue);
    } while (this.currentGraphConvergenceValue > this.convergenceLimit && 
             this.iteration < Integer.valueOf((String) this.prp.get("mdfsa.graph.iterationlimit")));
    
    
    /* Prints some final graph statistics */
    positive = 0;
    negative = 0;
    neutral = 0;
    for(long featureId: this.currentPolarities.keySet()) {
      double currentPolarity = this.currentPolarities.get(featureId);
      if(currentPolarity > 0.0) {
        positive++;
      } else if(currentPolarity < 0.0) {
        negative++;
      } else {
        neutral++;
      }
    }
    //System.out.println("Iterations: " + iteration);
    //System.out.println("Positive: " + positive + " - Negative: " + negative + " - Neutral: " + neutral);
    
    
    /*
     * Computes the FuzzyMembership functions of each concept
     */
    for(long featureId: this.currentPolarities.keySet()) {
      Double conceptStartPolarity = this.startPolarities.get(featureId);
      double conceptEndPolarity = 0.0;
      if(conceptStartPolarity == null) {
        conceptStartPolarity = 0.0;
      }
      ArrayList<Double> conceptConvergenceHistory = this.conceptsConvergenceIterationsValues.get(featureId);
      
      double avgPolarity = conceptStartPolarity;
      double variance = 0.0;
      if(conceptConvergenceHistory == null) {
        variance = 2.0;
      } else {
        for(double value: conceptConvergenceHistory) {
          avgPolarity += value;
          conceptEndPolarity = value;
        }
        avgPolarity /= ((double) (conceptConvergenceHistory.size() + 1));
        variance = Math.pow((conceptStartPolarity - avgPolarity), 2.0);
        for(double value: conceptConvergenceHistory) {
          variance += Math.pow((value - avgPolarity), 2.0);
        }
      }
      
      double a = 0.0;
      double b = conceptStartPolarity;
      double c = conceptEndPolarity;
      double d = 0.0;
      if(conceptStartPolarity > conceptEndPolarity) {
        a = b;
        b = c;
        c = a;
      }
      a = b - (variance / 2.0);
      d = c + (variance / 2.0);
      if(a < -1.0) {
        a = -1.0;
      }
      if(d > 1.0) {
        d = 1.0;
      }
      
      this.polarities.put(featureId, new FuzzyMembership(a, b, c, d));
      //System.out.println(variance + " - " + Math.sqrt(variance));
    }
  }
  
  
  
  
  /**
   * Validates the learned graph on the predicting polarity test set
   * @param instances the set of test instances
   */
  public double[] polarityTest(ArrayList<DatasetInstance> instances) {
    //HashMap<String, Long> labels = this.graph.getLabels();
    //System.out.println("Validating graph.");
    
    double deadzone = this.deadzone;
    
    int judged = 0;
    double precision = 0.0;
    double recall = 0.0;
    
    for(DatasetInstance di: instances) {
      ArrayList<String> features = di.getFeatures();
      int testPolarity = di.getPolarity();
      
      double inferredPolarity = 0.0;
      double a = 0.0;
      double b = 0.0;
      double c = 0.0;
      double d = 0.0;
      int mappedFeatures = 0;
      for(String currentFeature: features) {
        //Long featureId = labels.get(currentFeature);
        ArrayList<Long> featureIds = this.graph.getFeatureIds(currentFeature);
        if(featureIds != null) {
          for(Long featureId: featureIds) {
            mappedFeatures++;
            FuzzyMembership fm = this.polarities.get(featureId);
            if(fm != null) {
              inferredPolarity += fm.getCentroid();
              a += fm.getA();
              b += fm.getB();
              c += fm.getC();
              d += fm.getD();
            }
          }
        }
      }
      a /= (double) mappedFeatures;
      b /= (double) mappedFeatures;
      c /= (double) mappedFeatures;
      d /= (double) mappedFeatures;
      //inferredPolarity /= (double) mappedFeatures;
      inferredPolarity = (b + c) / 2.0;
      //System.out.println("Test polarity: " + testPolarity + "; Inferred Polarity: " + inferredPolarity);
      
      int validationRestrictionFlag = Integer.valueOf(this.prp.getProperty("mdfsa.graph.validationrestriction"));
      
      if((validationRestrictionFlag == 2 && ((a < 0.0 && b > 0.0) || (c < 0.0 && d > 0.0) || (b < 0.0 && c > 0.0))) ||
         (validationRestrictionFlag == 1 && (b < 0.0 && c > 0.0))) {
        // do nothing
      } else if((inferredPolarity < (0.0 - deadzone) && testPolarity == -1) ||
                (inferredPolarity > (0.0 + deadzone) && testPolarity == 1)) {
        precision += 1.0;
        judged++;
      } else if(inferredPolarity < (0.0 - deadzone) || inferredPolarity > (0.0 + deadzone)) {
        judged++;
      }
    }
    
    /* Print test results */
    precision /= ((double)judged);
    recall = ((double)judged) / (double)instances.size();
    double fmeasure = 2.0 * ((precision * recall) / (precision + recall));
    //System.out.println("********************************************");
    //System.out.println("Precision: " + precision);
    //System.out.println("Recall: " + recall);
    //System.out.println("F-Measure: " + fmeasure);
    
    double[] results = new double[3];
    results[0] = precision;
    results[1] = recall;
    results[2] = fmeasure;
    return results;
  }
  
  
  
  
  /**
   * Validates the learned graph on unknown polarity validation set by returning only the predicted polarity value
   * @param instances the set of test instances
   */
  public ArrayList<DatasetInstance> polarityValidation(ArrayList<DatasetInstance> instances) {

    ArrayList<DatasetInstance> results = new ArrayList<DatasetInstance>();
    double deadzone = this.deadzone;
    
    int judged = 0;
    double precision = 0.0;
    double recall = 0.0;
    
    for(DatasetInstance di: instances) {
      ArrayList<String> features = di.getFeatures();
      int testPolarity = di.getPolarity();
      
      double inferredPolarity = 0.0;
      double a = 0.0;
      double b = 0.0;
      double c = 0.0;
      double d = 0.0;
      int mappedFeatures = 0;
      for(String currentFeature: features) {
        //Long featureId = labels.get(currentFeature);
        ArrayList<Long> featureIds = this.graph.getFeatureIds(currentFeature);
        if(featureIds != null) {
          for(Long featureId: featureIds) {
            mappedFeatures++;
            FuzzyMembership fm = this.polarities.get(featureId);
            if(fm != null) {
              inferredPolarity += fm.getCentroid();
              a += fm.getA();
              b += fm.getB();
              c += fm.getC();
              d += fm.getD();
            }
          }
        }
      }
      a /= (double) mappedFeatures;
      b /= (double) mappedFeatures;
      c /= (double) mappedFeatures;
      d /= (double) mappedFeatures;

      inferredPolarity = (b + c) / 2.0;
      di.setInferredPolarity(inferredPolarity);
      results.add(di);
    }
    return results;
  }
  
  
  
  /**
   * Validates the learned graph on the predicting domain test set
   * @param instances the set of test instances
   */
  public double domainTest(ArrayList<String> features, HashMap<String, DomainGraph> graphs) {
    double inferredMembership = 0.0;
    double a = 0.0;
    double b = 0.0;
    double c = 0.0;
    double d = 0.0;
    int mappedFeatures = 0;
    for (String currentFeature : features) {
      // Long featureId = labels.get(currentFeature);
      ArrayList<Long> featureIds = this.graph.getFeatureIds(currentFeature);
      if (featureIds != null) {
        for (Long featureId : featureIds) {
          Iterator<String> it = graphs.keySet().iterator();
          ArrayList<Double> domainValues = new ArrayList<Double>();
          while(it.hasNext()) {
            String currentDomain = it.next();
            DomainGraph domain = graphs.get(currentDomain);
            double inferredValue = domain.domainTest(currentFeature, domain);
            domainValues.add(inferredValue);
          }
          /*
          int significativityFlag = this.getSignificativityFlag(domainValues);
          if(significativityFlag == 0) {
            continue;
          }
          */
          mappedFeatures++;
          FuzzyMembership fm = this.polarities.get(featureId);
          if (fm != null) {
            inferredMembership += fm.getCentroid();
            a += fm.getA();
            b += fm.getB();
            c += fm.getC();
            d += fm.getD();
          }
        }
      }
    }
    inferredMembership /= (double) mappedFeatures;
    a /= (double) mappedFeatures;
    b /= (double) mappedFeatures;
    c /= (double) mappedFeatures;
    d /= (double) mappedFeatures;
    return inferredMembership;
  }
  
  
  
  
  
  /**
   * Get the single feature graph polarity
   * @param feature the feature to analyze
   */
  public double domainTest(String currentFeature, DomainGraph g) {
    double inferredMembership = 0.0;
    double a = 0.0;
    double b = 0.0;
    double c = 0.0;
    double d = 0.0;
    int mappedFeatures = 0;
    
    // Long featureId = labels.get(currentFeature);
    ArrayList<Long> featureIds = g.getGraph().getFeatureIds(currentFeature);
    if (featureIds != null) {
      for (Long featureId : featureIds) {
        mappedFeatures++;
        FuzzyMembership fm = g.getPolarities().get(featureId);
        if (fm != null) {
          inferredMembership += fm.getCentroid();
          a += fm.getA();
          b += fm.getB();
          c += fm.getC();
          d += fm.getD();
        }
      }
    }

    inferredMembership /= (double) mappedFeatures;
    a /= (double) mappedFeatures;
    b /= (double) mappedFeatures;
    c /= (double) mappedFeatures;
    d /= (double) mappedFeatures;
    return inferredMembership;
  }
  
  
  
  
  
  
  
  
  
  /*
   * Methods for the elaboration and propagation of data 
   */
  
  
  public synchronized Long getNextNodeId() {
    Long nextNodeId = null;
    if(this.nodeIterator.hasNext()) {
      nextNodeId = this.nodeIterator.next();
    }
    return nextNodeId; 
  }
  
  public synchronized void updatePolarities(long nodeId, FuzzyMembership fm) {
    return;
  }
  
  
  /**
   * Updates the value representing the total polarities update of the graph during the running iteration. 
   * @param contribution the contribution given by the changes of each node.
   */
  public synchronized void updateCurrentGraphConvergenceValue(double contribution) {
    this.currentGraphConvergenceValue += contribution;
    return;
  }
  
  
 
  
  /**
   * Saves the new polarity value computed for a node. Such value is saved both in the map
   * containing the current polarities of each node as well as in the map containing the historical convergence
   * values of each node.
   * @param nodeId the id of the node.
   * @param value the new polarity value of the node.
   */
  public synchronized void setIterationResult(long nodeId, double value) {
    
    /* Updates the current polarity value of the node */
    this.currentPolarities.put(nodeId, value);
    
    /* Updates the iteration value list of the node */
    ArrayList<Double> iterationValues = this.conceptsConvergenceIterationsValues.get(nodeId);
    if(iterationValues == null) {
      iterationValues = new ArrayList<Double>();
    }
    iterationValues.add(value);
    this.conceptsConvergenceIterationsValues.put(nodeId, iterationValues);
    
    /*  Updates the value representing the average polarity of the graph after the execution of a complete iteration. */
    this.currentAveragePolarity += value;
    
    return;
  }
  
  
  
  private void applyRepulsionFactor() {
    this.currentAveragePolarity /= this.graph.getLabels().values().size();
    int numberOfNodes = this.graph.getNumberOfNodes();
    int numberOfEdges = this.graph.getNumberOfEdges();
    
    Iterator<Long> it = this.currentPolarities.keySet().iterator();
    while(it.hasNext()) {
      Long nodeId = (Long) it.next();
      Double value = this.currentPolarities.get(nodeId);
      
      /* Applies the repulsion factor */
      double lambda = this.propagationRate;
      //double lambda = ((double) numberOfEdges / (((double) numberOfNodes * (double) numberOfNodes) - (double) numberOfNodes));
      if(value != null) {
        value +=  lambda * (value - this.currentAveragePolarity);
      }
      
      /* Updates the current polarity value of the node */
      this.currentPolarities.put(nodeId, value);
    }
  }
  
  
  /* Computes the difference between the two top values of polarities between a set them 
   * for deciding if the difference is statistically significant or not.
   */
  private int getSignificativityFlag(ArrayList<Double> values) {
    int flag = 0;
    double first = Double.MIN_VALUE;
    double second = Double.MIN_VALUE;
    for(Double d: values) {
      if(d > first) {
        first = d;
      } else if(d > second) {
        second = d;
      }
    }
    
    if((first - second) > 0.1) {
      flag = 1;
    }
    
    return flag;
  }
  
  
  
  /**
   * Internal class used as thread for speeding-up the propagation of information through the graph
   */
  private class NodeElaborator extends Thread implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private DomainGraph dg;
    private double propagationRate;
    private Iterator<Long> nodes;
    private int threadId;
    
    public NodeElaborator(DomainGraph dg, int threadId) {
      this.dg = dg;
      this.propagationRate = this.dg.propagationRate;
      this.nodes = this.dg.nodeIterator;
      this.threadId = threadId;
    }
    
    /*
    private Long getNextNodeId() {
      Long nextNodeId = null;
      if(this.nodes.hasNext()) {
        nextNodeId = this.nodes.next();
      }
      return nextNodeId; 
    }
    */
    
    public void run() {
      Long nextNodeId = this.dg.getNextNodeId();
      //Long nextNodeId = this.getNextNodeId();
      //System.out.println("Thread: " + this.threadId + " - Node: " + nextNodeId);
      
      /*
       * Updates node polarity
       */
      while(nextNodeId != null) {
        //System.out.println("Thread: " + this.threadId + " - Node: " + nextNodeId);
        //System.out.print(this.dg.graph.getIds().get(nextNodeId));
        
        /* Gets all edges */
        ArrayList<DomainEdge> des = this.dg.graph.getNodes().get(nextNodeId);
        if(des != null) {
          
          /* Gets current polarity */
          Double startNodePolarity = this.dg.currentPolarities.get(nextNodeId);
          Double actualPropagationRate = this.propagationRate;
          if(startNodePolarity == null) {
            startNodePolarity = 0.0;
            actualPropagationRate = 1.0;
          }
          
          //System.out.print(" (" + startNodePolarity + "): ");
          /* Loops through all edges and reads the current polarity of each of them */
          double edgesPolarities = 0.0;
          for(DomainEdge de: des) {
            long edgeNodeId = de.getNodeId();
            //System.out.print(this.dg.graph.getIds().get(edgeNodeId));
            Double currentEdgePolarity = this.dg.currentPolarities.get(edgeNodeId);
            if(currentEdgePolarity != null) {
              //System.out.print(" (" + currentEdgePolarity + ")");
              edgesPolarities += (currentEdgePolarity * de.getWeight());
            }
            //System.out.print(" - ");
          }
          
          /* Current node polarity is "merged" with the one coming from the neighborhoods */
          double currentNodePolarity = (startNodePolarity * (1 - actualPropagationRate)) +
                                       ((edgesPolarities / des.size()) * actualPropagationRate);
          //System.out.println(currentNodePolarity);
          
          /* Updates the total propagation difference of the graph */
          this.dg.updateCurrentGraphConvergenceValue(Math.abs(startNodePolarity - currentNodePolarity));
          
          /* Updates the polarity of the node and its convergence history */
          this.dg.setIterationResult(nextNodeId, currentNodePolarity);
        }
        
        nextNodeId = this.dg.getNextNodeId();
      }
    }
  }

  

  /**
   * Produces a serializable version of the DomainGraph object
   */
  public SerializableDomainGraph getSerializableDomainGraph() {
    SerializableDomainGraph sdg = new SerializableDomainGraph();
    sdg.setPrp(this.prp);
    sdg.setGraph(this.graph);
    sdg.setPolarities(this.polarities);
    sdg.setConceptsConvergenceIterationsValues(this.conceptsConvergenceIterationsValues);
    sdg.setStartPolarities(this.startPolarities);
    sdg.setTempInDomainStartPolarities(this.tempInDomainStartPolarities);
    sdg.setTempOutDomainStartPolarities(this.tempOutDomainStartPolarities);
    sdg.setCurrentPolarities(this.currentPolarities);
    sdg.setTokensCounter(this.tokensCounter);
    sdg.setInDomainTokensCounter(this.inDomainTokensCounter);
    sdg.setOutDomainTokensCounter(this.outDomainTokensCounter);
    sdg.setCurrentGraphConvergenceValue(this.currentGraphConvergenceValue);
    sdg.setCurrentAveragePolarity(this.currentAveragePolarity);
    sdg.setIteration(this.iteration);
    sdg.setPropagationRate(this.propagationRate);
    sdg.setConvergenceLimit(this.convergenceLimit);
    sdg.setDeadzone(this.deadzone);
    sdg.setAnnealingRate(this.annealingRate);
    return sdg;
  }
  
  
  /** Initializes the DomainGraph object with serialized data **/
  public void setDomainGraphFromSerializedData(String currentDataset, String modelPath, String type) {
    try {
      ObjectInputStream objectInputStream = new ObjectInputStream(
          new FileInputStream(modelPath + currentDataset + "." + type + ".mdfsa"));
      SerializableDomainGraph sdg = (SerializableDomainGraph) objectInputStream.readObject();
      this.prp = sdg.getPrp();
      this.graph = sdg.getGraph();
      this.polarities = sdg.getPolarities();
      this.conceptsConvergenceIterationsValues = sdg.getConceptsConvergenceIterationsValues();
      this.startPolarities = sdg.getStartPolarities();
      this.tempInDomainStartPolarities = sdg.getTempInDomainStartPolarities();
      this.tempOutDomainStartPolarities = sdg.getTempOutDomainStartPolarities();
      this.currentPolarities = sdg.getCurrentPolarities();
      this.tokensCounter = sdg.getTokensCounter();
      this.inDomainTokensCounter = sdg.getInDomainTokensCounter();
      this.outDomainTokensCounter = sdg.getOutDomainTokensCounter();
      this.currentGraphConvergenceValue = sdg.getCurrentGraphConvergenceValue();
      this.currentAveragePolarity = sdg.getCurrentAveragePolarity();
      this.iteration = sdg.getIteration();
      this.propagationRate = sdg.getPropagationRate();
      this.convergenceLimit = sdg.getConvergenceLimit();
      this.deadzone = sdg.getDeadzone();
      this.annealingRate = sdg.getAnnealingRate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  
  public void setDomainGraphFromSerializedData(String modelPath) {
    try {
      ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(modelPath));
      SerializableDomainGraph sdg = (SerializableDomainGraph) objectInputStream.readObject();
      this.prp = sdg.getPrp();
      this.graph = sdg.getGraph();
      this.polarities = sdg.getPolarities();
      this.conceptsConvergenceIterationsValues = sdg.getConceptsConvergenceIterationsValues();
      this.startPolarities = sdg.getStartPolarities();
      this.tempInDomainStartPolarities = sdg.getTempInDomainStartPolarities();
      this.tempOutDomainStartPolarities = sdg.getTempOutDomainStartPolarities();
      this.currentPolarities = sdg.getCurrentPolarities();
      this.tokensCounter = sdg.getTokensCounter();
      this.inDomainTokensCounter = sdg.getInDomainTokensCounter();
      this.outDomainTokensCounter = sdg.getOutDomainTokensCounter();
      this.currentGraphConvergenceValue = sdg.getCurrentGraphConvergenceValue();
      this.currentAveragePolarity = sdg.getCurrentAveragePolarity();
      this.iteration = sdg.getIteration();
      this.propagationRate = sdg.getPropagationRate();
      this.convergenceLimit = sdg.getConvergenceLimit();
      this.deadzone = sdg.getDeadzone();
      this.annealingRate = sdg.getAnnealingRate();
      objectInputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public HashMap<Long, FuzzyMembership> getPolarities() {
    return polarities;
  }


  public void setPolarities(HashMap<Long, FuzzyMembership> polarities) {
    this.polarities = polarities;
  }


  public Graph getGraph() {
    return graph;
  }


  public void setGraph(Graph graph) {
    this.graph = graph;
  }
}
