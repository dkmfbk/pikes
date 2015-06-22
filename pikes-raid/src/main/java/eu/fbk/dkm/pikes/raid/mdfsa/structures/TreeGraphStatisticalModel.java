package eu.fbk.dkm.pikes.raid.mdfsa.structures;

import eu.fbk.dkm.pikes.raid.mdfsa.parser.DependencyTree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class TreeGraphStatisticalModel implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private Properties prp;
  private ArrayList<DatasetInstance> trainingInstances;
  private ArrayList<DatasetInstance> testInstances;
  private HashMap<String, HashMap<String, Tuple>> domainDependentModelsPositive;
  private HashMap<String, HashMap<String, Tuple>> domainDependentModelsNegative;
  private HashMap<String, HashMap<String, Tuple>> domainDependentModelsNeutral;
  private HashMap<String, Tuple> domainIndependentModelPositive;
  private HashMap<String, Tuple> domainIndependentModelNegative;
  private HashMap<String, Tuple> domainIndependentModelNeutral;
  private HashMap<String, Integer> domainInstanceCounter;
  private HashMap<String, Integer> domainLevels;
  
  public TreeGraphStatisticalModel(Properties p) {
    this.prp = p;
    this.trainingInstances = new ArrayList<DatasetInstance>();
    this.domainDependentModelsPositive = new HashMap<String, HashMap<String, Tuple>>();
    this.domainDependentModelsNegative = new HashMap<String, HashMap<String, Tuple>>();
    this.domainDependentModelsNeutral = new HashMap<String, HashMap<String, Tuple>>();
    this.domainIndependentModelPositive = new HashMap<String, Tuple>();
    this.domainIndependentModelNegative = new HashMap<String, Tuple>();
    this.domainIndependentModelNeutral = new HashMap<String, Tuple>();
    this.domainInstanceCounter = new HashMap<String, Integer>();
    this.domainInstanceCounter.put("POL-POSITIVE", new Integer(0));
    this.domainInstanceCounter.put("POL-NEGATIVE", new Integer(0));
    this.domainInstanceCounter.put("POL-NEUTRAL", new Integer(0));
    this.domainLevels = new HashMap<String, Integer>();
  }
  
  
  public void setTrainingInstances(ArrayList<DatasetInstance> training) {
    this.trainingInstances = training;
  }
  
  
  public void setTestInstances(ArrayList<DatasetInstance> test) {
    this.testInstances = test;
  }
  
  public ArrayList<DatasetInstance> getTestInstances() {
    return this.testInstances;
  }
  
  
  public void buildModel() {
    
    for(DatasetInstance di: this.trainingInstances) {
      
      /* Checks if the domain has been already analyzed, otherwise the new maps are created */
      String domain = di.getDomain();
      String[] domains = domain.split("\\#");
      int k = 0;
      for(String cDomain: domains) {
        this.domainLevels.put(cDomain.trim(), k);
        
        HashMap<String, Tuple> domainModel = this.domainDependentModelsPositive.get(cDomain);
        if(domainModel == null) {
          domainModel = new HashMap<String, Tuple>();
          this.domainDependentModelsPositive.put(cDomain, domainModel);
          domainModel = new HashMap<String, Tuple>();
          this.domainDependentModelsNegative.put(cDomain, domainModel);
          domainModel = new HashMap<String, Tuple>();
          this.domainDependentModelsNeutral.put(cDomain, domainModel);
          //this.domainInstanceCounter.put(domain, new Integer(0));
        }
        
        
        /* Sets the HashMap to use based on the domain and on the polarity of the current DatasetInstance */
        int polarity = di.getPolarity();
        HashMap<String, Tuple> domainIndependentModel = null;
        HashMap<String, HashMap<String, Tuple>> polarizedModel = null;
        if(polarity == 1) {
          polarizedModel = this.domainDependentModelsPositive;
          domainIndependentModel = this.domainIndependentModelPositive;
        } else if(polarity == -1) {
          polarizedModel = this.domainDependentModelsNegative;
          domainIndependentModel = this.domainIndependentModelNegative;
        } else if(polarity == 0) {
          polarizedModel = this.domainDependentModelsNeutral;
          domainIndependentModel = this.domainIndependentModelNeutral;
        }
        
        domainModel = polarizedModel.get(cDomain);
        
        
        /* Extracts the list of dependencies from the current DatasetInstance and populates the model */
        SentenceStructuredRepresentation ssr = di.getSentenceStructuredRepresentation();
        ArrayList<DependencyTree> dts = ssr.getDependencyTree();
        for(DependencyTree dt: dts) {
          
          ArrayList<String> dependencies = dt.getDependecies();
          for(String dep: dependencies) {
            
            /* Gets the number of instances of the current domain models (both domain dependent and independent) and updates them */  
            Integer domainCounter = this.domainInstanceCounter.get(cDomain.trim() + "_" + di.getPolarity());
            if(domainCounter == null) {
              this.domainInstanceCounter.put(cDomain.trim() + "_" + di.getPolarity(), new Integer(0));
              domainCounter = this.domainInstanceCounter.get(cDomain.trim() + "_" + di.getPolarity());
            }
            domainCounter++;
            this.domainInstanceCounter.put(cDomain.trim() + "_" + di.getPolarity(), domainCounter);
            domainCounter = this.domainInstanceCounter.get(cDomain.trim());
            if(domainCounter == null) {
              this.domainInstanceCounter.put(cDomain.trim(), new Integer(0));
              domainCounter = this.domainInstanceCounter.get(cDomain.trim());
            }
            domainCounter++;
            this.domainInstanceCounter.put(cDomain.trim(), domainCounter);
            
            
            if(di.getPolarity() == 1.0) {
              Integer counter = this.domainInstanceCounter.get("POL-POSITIVE");
              counter++;
              this.domainInstanceCounter.put("POL-POSITIVE", counter);
            } else if(di.getPolarity() == -1.0) {
              Integer counter = this.domainInstanceCounter.get("POL-NEGATIVE");
              counter++;
              this.domainInstanceCounter.put("POL-NEGATIVE", counter);
            } else {
              Integer counter = this.domainInstanceCounter.get("POL-NEUTRAL");
              counter++;
              this.domainInstanceCounter.put("POL-NEUTRAL", counter);
            }
            
            
            String[] d = dep.split("\\^\\^\\^");
            d[1] = d[1].substring(0, d[1].indexOf("-"));
            d[2] = d[2].substring(0, d[2].indexOf("-"));
            dep = d[0] + "^^^" + d[1] + "^^^" + d[2];
            
            if(d.length == 3) {
              /*
               * Checks if the following four tuples exists:
               * key: governor term;
               * key: dependent term;
               * key: the entire rule relation-governor-dependent
               * key: the inverse entire rule relation-dependent-governor (used with frequency 0.5)
               * If not, they are created and put in the HashMap; if Yes, they are updated with the statistical information
               */
              
              /* Key: relation-governor-dependent */
              Tuple t = domainModel.get(dep);
              Tuple tI = domainIndependentModel.get(dep);
              if(t == null) {
                t = new Tuple(1);
                t.setToken(dep);
                t.setFrequency(1.0);
              } else {
                t.setFrequency(t.getFrequency() + 1.0);
              }
              if(tI == null) {
                tI = new Tuple(1);
                tI.setToken(dep);
                tI.setFrequency(1.0);
              } else {
                tI.setFrequency(t.getFrequency() + 1.0);
              }
              domainModel.put(dep, t);
              domainIndependentModel.put(dep, tI);
              
              /* Key: governor */
              t = domainModel.get(d[1]);
              tI = domainIndependentModel.get(d[1]);
              if(t == null) {
                t = new Tuple(1);
                t.setToken(d[1]);
                t.setFrequency(1.0);
              } else {
                t.setFrequency(t.getFrequency() + 1.0);
              }
              if(tI == null) {
                tI = new Tuple(1);
                tI.setToken(d[1]);
                tI.setFrequency(1.0);
              } else {
                tI.setFrequency(t.getFrequency() + 1.0);
              }
              domainModel.put(d[1], t);
              domainIndependentModel.put(d[1], tI);
              
              /* Key: dependent */
              t = domainModel.get(d[2]);
              tI = domainIndependentModel.get(d[2]);
              if(t == null) {
                t = new Tuple(1);
                t.setToken(d[2]);
                t.setFrequency(1.0);
              } else {
                t.setFrequency(t.getFrequency() + 1.0);
              }
              if(tI == null) {
                tI = new Tuple(1);
                tI.setToken(d[2]);
                tI.setFrequency(1.0);
              } else {
                tI.setFrequency(t.getFrequency() + 1.0);
              }
              domainModel.put(d[2], t);
              domainIndependentModel.put(d[2], tI);
              
              /* Key: relation-dependent-governor */
              String key = new String(d[0] + "^^^" + d[2] + "^^^" + d[1]);
              t = domainModel.get(key);
              tI = domainIndependentModel.get(key);
              if(t == null) {
                t = new Tuple(1);
                t.setToken(key);
                t.setFrequency(1.0);
              } else {
                t.setFrequency(t.getFrequency() + 1.0);
              }
              if(tI == null) {
                tI = new Tuple(1);
                tI.setToken(key);
                tI.setFrequency(1.0);
              } else {
                tI.setFrequency(t.getFrequency() + 1.0);
              }
              domainModel.put(key, t);
              domainIndependentModel.put(key, tI);
              
              /* Key: governor-dependent */
              key = new String(d[1] + "^^^" + d[2]);
              t = domainModel.get(key);
              tI = domainIndependentModel.get(key);
              if(t == null) {
                t = new Tuple(1);
                t.setToken(key);
                t.setFrequency(1.0);
              } else {
                t.setFrequency(t.getFrequency() + 1.0);
              }
              if(tI == null) {
                tI = new Tuple(1);
                tI.setToken(key);
                tI.setFrequency(1.0);
              } else {
                tI.setFrequency(t.getFrequency() + 1.0);
              }
              domainModel.put(key, t);
              domainIndependentModel.put(key, tI);
              
              /* Key: dependent-governor */
              key = new String(d[2] + "^^^" + d[1]);
              t = domainModel.get(key);
              tI = domainIndependentModel.get(key);
              if(t == null) {
                t = new Tuple(1);
                t.setToken(key);
                t.setFrequency(1.0);
              } else {
                t.setFrequency(t.getFrequency() + 1.0);
              }
              if(tI == null) {
                tI = new Tuple(1);
                tI.setToken(key);
                tI.setFrequency(1.0);
              } else {
                tI.setFrequency(t.getFrequency() + 1.0);
              }
              domainModel.put(key, t);
              domainIndependentModel.put(key, tI);
            }
          }
        }
        
        polarizedModel.put(cDomain, domainModel);
        k++;
      }
    }
    
    /*
    System.out.println(this.domainDependentModelsPositive);
    System.out.println();
    System.out.println(this.domainDependentModelsNegative);
    System.out.println();
    System.out.println(this.domainDependentModelsNeutral);
    System.out.println();
    System.out.println(this.domainIndependentModelPositive);
    System.out.println();
    System.out.println(this.domainIndependentModelNegative);
    System.out.println();
    System.out.println(this.domainIndependentModelNeutral);
    System.out.println();
    System.out.println(this.domainInstanceCounter);
    */
  }
  
  
  
  
  /*
   * Output prediction procedure
   */
  public void computeResults(int flagEvaluateDomain, int flagEvaluateDoubleDomain, int fineGranedPolarity) {
        
    int positiveCounter = this.domainInstanceCounter.get("POL-POSITIVE");
    int negativeCounter = this.domainInstanceCounter.get("POL-NEGATIVE");
    int neutralCounter = this.domainInstanceCounter.get("POL-NEUTRAL");
    
    for(DatasetInstance di: this.testInstances) {
      
      /*
       * POLARITY EVALUATION
       */
      double positiveCoefficient = this.computeMembershipCoefficient(this.domainIndependentModelPositive, di, positiveCounter);
      double negativeCoefficient = this.computeMembershipCoefficient(this.domainIndependentModelNegative, di, negativeCounter);
      double neutralCoefficient = this.computeMembershipCoefficient(this.domainIndependentModelNeutral, di, neutralCounter);
      
      if(fineGranedPolarity == 1) {
        double delta = positiveCoefficient - negativeCoefficient;
        double finePolarity = 0.0;
        
        if(Math.max(positiveCoefficient, negativeCoefficient) != 0.0) {
          finePolarity = (delta / Math.max(positiveCoefficient, negativeCoefficient)) * 5.0;
        }
        
        if(neutralCoefficient != 0.0) {
          finePolarity = finePolarity - ((1.0 - (1.0 / neutralCoefficient)) * Math.signum(finePolarity));
        }
        //di.setInferredPolarity(Math.abs(finePolarity) - Math.abs(di.getInferredPolarity()));
        if(finePolarity > 0.0) {
          di.setInferredPolarity(Math.ceil(finePolarity));
        } else if(finePolarity < 0.0) {
          di.setInferredPolarity(Math.floor(finePolarity));
        } else {
          di.setInferredPolarity(finePolarity);
        }
        di.setInferredPolarity(finePolarity);
        
      } else {
        if(positiveCoefficient > negativeCoefficient && positiveCoefficient > neutralCoefficient) {di.setInferredPolarity(1.0);}
        if(negativeCoefficient > positiveCoefficient && negativeCoefficient > neutralCoefficient) {di.setInferredPolarity(-1.0);}
        if(neutralCoefficient > negativeCoefficient && neutralCoefficient > positiveCoefficient) {di.setInferredPolarity(0.0);}
      }
        
      
      
      /*
       * DOMAIN EVALUATION
       */
      if(flagEvaluateDomain == 1) {
        di.setInferredDomain("");
        Iterator<String> iD = this.domainLevels.keySet().iterator();
        HashMap<String, Double> domainMembership = new HashMap<String, Double>();
        
        double maxDomainCoeff = Double.NEGATIVE_INFINITY;
        while(iD.hasNext()) {
          String d = iD.next();
          double domainCoeff = 0.0;
          positiveCoefficient = 0.0;
          negativeCoefficient = 0.0;
          neutralCoefficient = 0.0;
          
          Integer currentDomainCounter = this.domainInstanceCounter.get(d.trim());
          
          HashMap<String, Tuple> currentDomainModel = this.domainDependentModelsPositive.get(d);
          if(currentDomainModel != null && currentDomainCounter != null) {
            positiveCoefficient = this.computeMembershipCoefficient(currentDomainModel, di, currentDomainCounter);
          }
          
          currentDomainModel = this.domainDependentModelsNegative.get(d);
          if(currentDomainModel != null && currentDomainCounter != null) {
            negativeCoefficient = this.computeMembershipCoefficient(currentDomainModel, di, currentDomainCounter);
          }
          
          currentDomainModel = this.domainDependentModelsPositive.get(d);
          if(currentDomainModel != null && currentDomainCounter != null) {
            neutralCoefficient = this.computeMembershipCoefficient(currentDomainModel, di, currentDomainCounter);
          }
          
          domainCoeff = positiveCoefficient + negativeCoefficient + neutralCoefficient;

          if(domainCoeff > maxDomainCoeff) {
            di.setInferredDomain(d);
            maxDomainCoeff = domainCoeff;
          }
          domainMembership.put(d, domainCoeff);
        }
        
        
        /*
         * Double DOMAIN Validation
         * This evaluation is performed contextually to the single domain evaluation in order to exploit the same
         * objects preliminary filled during the single domain evaluation
         */
        if(flagEvaluateDoubleDomain == 1) {
          String levelZero = new String("");
          String levelOne = new String("");
          iD = this.domainLevels.keySet().iterator();
          double maxDomainCoeffLevelZero = Double.NEGATIVE_INFINITY;
          double maxDomainCoeffLevelOne = Double.NEGATIVE_INFINITY;
          while(iD.hasNext()) {
            String d = iD.next();
            int level = this.domainLevels.get(d);
            double membership = domainMembership.get(d);
            if(level == 0) {
              if(membership > maxDomainCoeffLevelZero) {
                levelZero = new String(d);
                maxDomainCoeffLevelZero = membership;
              }
            } else if(level == 1) {
              if(membership > maxDomainCoeffLevelOne) {
                levelOne = new String(d);
                maxDomainCoeffLevelOne = membership;
              }
            }
          }
          di.setInferredDomain(levelZero + "#" + levelOne);
        }
      }
    }
  }
  
  
  
  
  private double computeMembershipCoefficient(HashMap<String, Tuple> h, DatasetInstance di, int normCounter) {
    
    double coeff = 0.0;
    
    SentenceStructuredRepresentation ssr = di.getSentenceStructuredRepresentation();
    ArrayList<DependencyTree> dts = ssr.getDependencyTree();
    for(DependencyTree dt: dts) {
      ArrayList<String> dependencies = dt.getDependecies();
      for(String dep: dependencies) {
        String[] d = dep.split("\\^\\^\\^");
        d[1] = d[1].substring(0, d[1].indexOf("-"));
        d[2] = d[2].substring(0, d[2].indexOf("-"));
        dep = d[0] + "^^^" + d[1] + "^^^" + d[2];
        
        if(d.length == 3) {
          
          String governor = d[1];
          String dependent = d[2];
          String rdg = new String(d[0] + "^^^" + d[2] + "^^^" + d[1]);
          String gd = new String(d[1] + "^^^" + d[2]);
          String dg = new String(d[2] + "^^^" + d[1]);
          
          /* Compute coefficient */
          Tuple tDep = h.get(dep);
          Tuple tG = h.get(governor);
          Tuple tD = h.get(dependent);
          Tuple tRDG = h.get(rdg);
          Tuple tGD = h.get(gd);
          Tuple tDG = h.get(dg);
          
          double axiomCoeff = 0.0;
          if(tDep != null) {
            axiomCoeff += 1.0 / Math.log((double) normCounter / tDep.getFrequency());
          }
          if(tDep != null && tG != null) {
            axiomCoeff += ((1.0 / Math.log((double) normCounter / tDep.getFrequency())) /
                           (1.0 / Math.log((double) normCounter / tG.getFrequency())));
          }
          if(tDep != null && tD != null) {
            axiomCoeff += ((1.0 / Math.log((double) normCounter / tDep.getFrequency())) /
                           (1.0 / Math.log((double) normCounter / tD.getFrequency())));
          }
          if(tRDG != null) {
            axiomCoeff += 1.0 / Math.log((double) normCounter / tRDG.getFrequency());
          }
          if(tRDG != null && tGD != null) {
            axiomCoeff += ((1.0 / Math.log((double) normCounter / tRDG.getFrequency())) /
                           (1.0 / Math.log((double) normCounter / tGD.getFrequency())));
          }
          if(tRDG != null && tDG != null) {
            axiomCoeff += (((1.0 / Math.log((double) normCounter / tRDG.getFrequency())) /
                            (1.0 / Math.log((double) normCounter / tDG.getFrequency()))) * 0.5);
          }
          coeff += (axiomCoeff / dependencies.size());
        }
      }
    }
    
    return coeff;
  }
  
  
  
  
  
  private class Tuple implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String token;
    private double frequency;
    private double weight;  
    private int type;
    private double counter;
    
    public Tuple(int type) {
      this.frequency = 0.0;
      this.counter = 1.0;
      this.weight = 1.0;
      this.type = type;
    }
    
    public String getToken() {
      return token;
    }
    public void setToken(String token) {
      this.token = token;
    }
    public double getFrequency() {
      return frequency;
    }
    public void setFrequency(double frequency) {
      this.frequency = frequency;
    }
    public double getWeight() {
      return weight;
    }
    public void setWeight(double weight) {
      this.weight = weight;
    }
    public int getType() {
      return type;
    }
    public void setType(int type) {
      this.type = type;
    }
  }
}
