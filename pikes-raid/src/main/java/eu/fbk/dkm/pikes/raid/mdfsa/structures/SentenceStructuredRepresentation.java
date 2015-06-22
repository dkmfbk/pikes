package eu.fbk.dkm.pikes.raid.mdfsa.structures;

import edu.stanford.nlp.trees.Tree;
import eu.fbk.dkm.pikes.raid.mdfsa.parser.DependencyTree;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLexicalizer;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLoader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class SentenceStructuredRepresentation implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private Properties prp;
  private String uri;
  private String originalText;
  private String posTaggedString;
  private String lexString;
  private String stemmedString;
  private ArrayList<Tree> parsedTree;
  private HashMap<String, ArrayList<String>> aspects;
  private ArrayList<String> semanticConcepts;
  private ArrayList<DependencyTree> dts;
  private int sentenceMarker;
  
  public SentenceStructuredRepresentation(Properties prp) {
    this.prp = prp;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
  
  public String getUri() {
    return this.uri;
  }
  
  public String getOriginalText() {
    return this.originalText;
  }

  public void setOriginalText(String originalText) {
    this.originalText = originalText;
  }

  public String getPosTaggedString() {
    return this.posTaggedString;
  }

  public void setPosTaggedString(String posTaggedString) {
    this.posTaggedString = posTaggedString;
  }

  public ArrayList<DependencyTree> getDependencyTree() {
    return this.dts;
  }

  public void setDependencyTree(ArrayList<DependencyTree> dts) {
    this.dts = dts;
  }
  
  public HashMap<String, ArrayList<String>> getAspects() {
    return this.aspects;
  }
  
  public ArrayList<String> getSemanticConcepts() {
    return this.semanticConcepts;
  }

  public ArrayList<Tree> getParsedTree() {
    return this.parsedTree;
  }

  public void setParsedTree(ArrayList<Tree> parsedTree) {
    this.parsedTree = parsedTree;
  }
  
  public void createLexicalizedRepresentation(WordNetLexicalizer wnlex) {
    
    /* Checks the tagged string and creates lexicalized version of it */
    String[] posTaggedTerms = this.posTaggedString.split(" ");
    StringBuffer tempLex = new StringBuffer();
    for(String curTaggedTerm: posTaggedTerms) {
      if(curTaggedTerm.compareTo("") == 0) {
        continue;
      }
      try {
        String term = curTaggedTerm.substring(0, curTaggedTerm.indexOf("/"));
        String tag = curTaggedTerm.substring(curTaggedTerm.indexOf("/") + 1);
        
        if(tag.compareTo("NNS") == 0 || tag.compareTo("NNPS") == 0) {
          term = wnlex.getWordLexicalizationByType(term, "N");
        } else if(tag.compareTo("VBD") == 0 || tag.compareTo("VBG") == 0 || tag.compareTo("VBN") == 0 ||
                  tag.compareTo("VBP") == 0 || tag.compareTo("VBZ") == 0) {
          term = wnlex.getWordLexicalizationByType(term, "V");
        } else if(tag.compareTo("JJR") == 0 || tag.compareTo("JJS") == 0) {
          term = wnlex.getWordLexicalizationByType(term, "AJ");
        } else if(tag.compareTo("RBR") == 0 || tag.compareTo("RBS") == 0) {
          term = wnlex.getWordLexicalizationByType(term, "AV");
        }
        if(term == null) {
          term = curTaggedTerm.substring(0, curTaggedTerm.indexOf("/"));
        }
        tempLex.append(term + "/" + tag + " ");
      } catch(Exception e) {
        //System.out.println(this.posTaggedString);
        e.printStackTrace();
        //System.exit(0);
      }
    }
    this.lexString = tempLex.toString().trim();
    //System.out.println(this.originalText);
    //System.out.println(this.posTaggedString);
    //System.out.println(this.lexString);
    //System.out.println();
  }
  
  
  public void createStemmedRepresentation() {
    
  }
  
  
  /**
   * Extracts the set of semantic concepts
   */
  public void extractSemanticConcepts(WordNetLoader wnl, WordNetLexicalizer wnlex) {
    this.semanticConcepts = new ArrayList<String>();
    //String terms = this.lexString.replaceAll("\\./\\.", "");
    //String[] termsList = terms.split(" ");
    String[] termsList = this.lexString.split(" ");
    boolean compoundNounFlag = false;
    for(String currentTerm : termsList) {
      String[] atom = currentTerm.split("/");
      if(atom.length > 1) {
        Integer stopFlag = wnl.getStopwords().get(atom[0]);
        if(stopFlag != null) continue;
        if(atom[1].compareTo("NN") == 0 || atom[1].compareTo("NNP") == 0 || atom[1].compareTo("NNPS") == 0 ||
           atom[1].compareTo("NNS") == 0 || atom[1].compareTo("FW") == 0) {
          String newAspect;
          if(compoundNounFlag == true) {
            newAspect = this.semanticConcepts.get(this.semanticConcepts.size() - 1);
            newAspect = (newAspect + " " + atom[0]).replaceAll(" ", "_").toLowerCase();
            this.semanticConcepts.remove(this.semanticConcepts.size() - 1);
          } else {
            newAspect = atom[0].replaceAll(" ", "_").toLowerCase();
          }
          if(!this.semanticConcepts.contains(newAspect)) {
            this.semanticConcepts.add(newAspect);
          }
          compoundNounFlag = true;
        } else {
          compoundNounFlag = false;
        }
      }
    }
    
    
    
    for(DependencyTree dt: this.dts)
    {
      ArrayList<String> dependencies = dt.getDependecies();
      for(String curDep: dependencies) {
        String[] tokens = curDep.split("\\^\\^\\^");
        if(tokens.length == 3) {
          if(tokens[0].trim().compareTo("dobj") == 0) {
            String[] tokenOne = tokens[1].split("-");
            String[] tokenTwo = tokens[2].split("-");
            String partOne = wnlex.getWordLexicalizationByType(tokenOne[0], "V");
            if(partOne == null) {
              partOne = tokenOne[0];
            }
            String partTwo = wnlex.getWordLexicalizationByType(tokenTwo[0], "N");
            if(partTwo == null) {
              partTwo = tokenTwo[0];
            }
            String newAspect = partOne + "_" + partTwo;
            if(!this.semanticConcepts.contains(newAspect)) {
              this.semanticConcepts.add(newAspect);
            }
          }
        }
      }
    }
    
  }
  
  
  /**
   * Extracts the set of aspects
   */
  public void extractAspects(WordNetLoader wnl) {
    ArrayList<String> tempAspects = new ArrayList<String>();
    this.aspects = new HashMap<String, ArrayList<String>>();
    //String terms = this.lexString.replaceAll("\\./\\.", "");
    //String[] termsList = terms.split(" ");
    
    /* Extracts aspects */
    String[] termsList = this.lexString.split(" ");
    boolean compoundNounFlag = false;
    for(String currentTerm : termsList) {
      String[] atom = currentTerm.split("/");
      if(atom.length > 1) {
        Integer stopFlag = wnl.getStopwords().get(atom[0]);
        if(stopFlag != null) continue;
        if(atom[1].compareTo("NN") == 0 || atom[1].compareTo("NNP") == 0 || atom[1].compareTo("NNPS") == 0 ||
           atom[1].compareTo("NNS") == 0 || atom[1].compareTo("FW") == 0) {
          String newAspect;
          if(compoundNounFlag == true) {
            newAspect = tempAspects.get(tempAspects.size() - 1);
            newAspect = (newAspect + " " + atom[0]).replaceAll(" ", "_").toLowerCase();
            tempAspects.remove(tempAspects.size() - 1);
          } else {
            newAspect = atom[0].replaceAll(" ", "_").toLowerCase();
          }
          if(!tempAspects.contains(newAspect)) {
            tempAspects.add(newAspect);
          }
          compoundNounFlag = true;
        } else {
          compoundNounFlag = false;
        }
      }
    }
    
    
    /* creates the list of features connect with each aspect */
    //ArrayList<Tree> trees = this.extractTree(this.parsedTree);
    HashMap<Integer, ArrayList<String>> featureSentence = new HashMap<Integer, ArrayList<String>>();
    for(Tree pt: this.parsedTree)
    {
      this.sentenceMarker = 0;
      this.extractRelatedFeatures(pt, this.sentenceMarker, featureSentence);
      for(String curAspect: tempAspects) {
        String[] compoundAspect = curAspect.split(" ");
        //HashMap<String, Integer> relatedFeatures = new HashMap<String, Integer>();
        for(String cA: compoundAspect) {
          Iterator<Integer> it = featureSentence.keySet().iterator();
          while(it.hasNext()) {
            int key = it.next();
            ArrayList<String> currentTree = featureSentence.get(key);
            if(currentTree.contains(cA)) {
              ArrayList<String> featuresList = this.aspects.get(curAspect);
              if(featuresList == null) {
                featuresList = new ArrayList<String>();
              }
              for(String currentFeature: currentTree) {
                if(currentFeature.compareTo(cA) != 0) {
                  featuresList.add(currentFeature);
                }
              }
              this.aspects.put(curAspect, featuresList);
            }
          } 
        }
      }
    }
  }

  
  
  public ArrayList<Tree> extractTree(Tree t) {
    ArrayList<Tree> wanted = new ArrayList<Tree>();
    if (t.label().value().equals("S") || t.label().value().equals("SBAR")) {
      wanted.add(t);
      for (Tree child : t.children()) {
        ArrayList<Tree> temp = new ArrayList<Tree>();
        temp = this.extractTree(child);
        if (temp.size() > 0) {
          int o = -1;
          o = wanted.indexOf(t);
          if (o != -1) {
            wanted.remove(o);
          }
        }
        wanted.addAll(temp);
      }
    } else {
      for (Tree child : t.children()) {
        wanted.addAll(this.extractTree(child));
      }
    }
    //if(wanted.size() > 0) {
    //  System.out.println(wanted.toString());
    //}
    return wanted;
  }
  
  
  
  private void extractRelatedFeatures(Tree t, int marker, HashMap<Integer, ArrayList<String>> featureSentence) {
    //HashMap<String, Integer> features = new HashMap<String, Integer>();
    int localmarker = marker;
    if (t.label().value().equals("S") || t.label().value().equals("SBAR")) {
      localmarker = this.sentenceMarker + 1;
      this.sentenceMarker = localmarker;
      marker++;
    }
    if (t.label().value().length() > 1 && t.label().value().equals(t.label().value().toLowerCase())) {
      ArrayList<String> currentFeatures = featureSentence.get(marker);
      if(currentFeatures == null) {
        currentFeatures = new ArrayList<String>();
      }
      currentFeatures.add(t.label().value());
      //features.put(t.label().value(), new Integer(1));
      featureSentence.put(marker, currentFeatures);
    }
    for (Tree child: t.children()) {
      HashMap<String, Integer> temp = new HashMap<String, Integer>();
      this.extractRelatedFeatures(child, localmarker, featureSentence);
      if (temp.size() > 0) {
        Iterator<String> it = temp.keySet().iterator();
        while(it.hasNext()) {
          String currentFeature = (String) it.next();
          //features.put(currentFeature, new Integer(1));
        }
      }
    }
    if(localmarker == 1) {
      marker--;
    }
    //return features;
  }
  
  
}
