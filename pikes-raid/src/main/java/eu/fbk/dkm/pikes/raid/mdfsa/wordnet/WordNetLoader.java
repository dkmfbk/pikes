package eu.fbk.dkm.pikes.raid.mdfsa.wordnet;

import eu.fbk.dkm.pikes.raid.mdfsa.FileManager;
import eu.fbk.dkm.pikes.raid.mdfsa.FileManager.Mode;
import eu.fbk.shell.mdfsa.data.structures.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class WordNetLoader {
  
  Properties prp;
  private HashMap<String, ArrayList<Long>> wordSynsets;
  private HashMap<Long, ArrayList<String>> synsetWords;
  private HashMap<String, String> allowedRelations;
  private HashMap<String, Double> weightsRelations;
  private HashMap<Long, ArrayList<WordNetRelation>> synsetRelations;
  
  /*
   * Maps containing WordNet terms lists without applying filters
   */
  private HashMap<String, Integer>[] allTerms;
  private HashMap<String, Integer> allNouns;
  private HashMap<String, Integer> allVerbs;
  private HashMap<String, Integer> allAdjs;
  private HashMap<String, Integer> allAdvs;
  private HashMap<String, Integer> stopwords;
  private HashMap<String, String>[] allExceptions;
  private HashMap<String, String> nounExceptions;
  private HashMap<String, String> verbExceptions;
  private HashMap<String, String> adjExceptions;
  private HashMap<String, String> advExceptions;
  
  
  /**
   * Class contructor
   * @param prp properties file
   */
  @SuppressWarnings("unchecked")
  public WordNetLoader(Properties prp) {
    this.prp = prp;
    this.wordSynsets = new HashMap<String, ArrayList<Long>>();
    this.synsetWords = new HashMap<Long, ArrayList<String>>();
    this.allowedRelations = new HashMap<String, String>();
    this.weightsRelations = new HashMap<String, Double>();
    this.synsetRelations = new HashMap<Long, ArrayList<WordNetRelation>>();
    
    this.allNouns = new HashMap<String, Integer>();
    this.allVerbs = new HashMap<String, Integer>();
    this.allAdjs = new HashMap<String, Integer>();
    this.allAdvs = new HashMap<String, Integer>();
    this.stopwords = new HashMap<String, Integer>();
    this.nounExceptions = new HashMap<String, String>();
    this.verbExceptions = new HashMap<String, String>();
    this.adjExceptions = new HashMap<String, String>();
    this.advExceptions = new HashMap<String, String>();
    
    this.allTerms = new HashMap[4];
    this.allTerms[0] = this.allNouns;
    this.allTerms[1] = this.allVerbs;
    this.allTerms[2] = this.allAdjs;
    this.allTerms[3] = this.allAdvs;
    this.allExceptions = new HashMap[4];
    this.allExceptions[0] = this.nounExceptions;
    this.allExceptions[1] = this.verbExceptions;
    this.allExceptions[2] = this.adjExceptions;
    this.allExceptions[3] = this.advExceptions;
  }
  
  
  /**
   * Loads all data related to the ConceptNet knowledge base:
   * - loads the ConceptNet parameters related to the allowed relations and uri
   * - loads the json representation of ConceptNet
   */
  public void load() {
    this.loadWordNetParameters();
//    System.out.println(this.allowedRelations);
//    System.out.println(this.weightsRelations);
    this.loadWordNetRawData();
  }
  
  
  
  private void loadWordNetParameters() {
    
    /* Loads the set of allowed relations */
    FileManager fm = new FileManager(prp.getProperty("mdfsa.wordnet.relations"), Mode.READ);
    ArrayList<String> relations = fm.importSimpleTextContent();
    Iterator<String> it = relations.iterator();
    while(it.hasNext()) {
      String currentRelation = it.next();
      String[] tokens = currentRelation.split("\\^\\^\\^");
      if(tokens[0].compareTo("1") == 0) {
        this.allowedRelations.put(tokens[1], tokens[1]);
        this.weightsRelations.put(tokens[1], Double.valueOf(tokens[2]));
      }
    }
  }
  
  
  
  private void loadWordNetRawData() {

    String[] r;
    try {
      
      /*
       * LOADS SYNSETS
       */
      FileManager fm = new FileManager(this.prp.getProperty("mdfsa.wordnet.unambiguoussynsets"), Mode.READ);
      ArrayList<String> content = fm.importSimpleTextContent();
      
      for(String row: content) {
        if(row.compareTo("") == 0) {
          continue;
        }
        r = row.split("\\^\\^\\^");
        String currentWord = r[1];
        Long currentSynset = Long.valueOf(r[2]);
        
        /* Add the current synset to the word ones */
        ArrayList<Long> synsets = this.wordSynsets.get(currentWord);
        if(synsets == null) {
          synsets = new ArrayList<Long>();
        }
        synsets.add(currentSynset);
        this.wordSynsets.put(currentWord, synsets);
        
        /* Add the current word to the synsets ones */
        ArrayList<String> words = this.synsetWords.get(currentSynset);
        if(words == null) {
          words = new ArrayList<String>();
        }
        words.add(currentWord);
        this.synsetWords.put(currentSynset, words);
      }

      
      /*
       * LOADS SYNSETS RELATIONSHIPS
       */
      fm = new FileManager(this.prp.getProperty("mdfsa.wordnet.links"), Mode.READ);
      content = fm.importSimpleTextContent();      
      for(String row: content) {
        if(row.compareTo("") == 0) {
          continue;
        }
        r = row.split("\\^\\^\\^");
        long currentSynSource = Long.valueOf(r[0]);
        long currentSynTarget = Long.valueOf(r[1]);
        int currentRelation = Integer.valueOf(r[2]);
        
        ArrayList<WordNetRelation> currentRelations = this.synsetRelations.get(currentSynSource);
        if(currentRelations == null) {
          currentRelations = new ArrayList<WordNetRelation>();
        }
        WordNetRelation wnr = new WordNetRelation(currentRelation, currentSynTarget, 
                                                  this.weightsRelations.get(String.valueOf(currentRelation)));
        currentRelations.add(wnr);
        this.synsetRelations.put(currentSynSource, currentRelations);
      }
      
      
      
      /*
       * LOADS INDEX FILES
       */
      String[] indexFiles = new String[4];
      indexFiles[0] = this.prp.getProperty("mdfsa.extraction.nounlist");
      indexFiles[1] = this.prp.getProperty("mdfsa.extraction.verblist");
      indexFiles[2] = this.prp.getProperty("mdfsa.extraction.adjlist");
      indexFiles[3] = this.prp.getProperty("mdfsa.extraction.advlist");
      for(int i = 0; i < 4; i++) {
        fm = new FileManager(indexFiles[i], Mode.READ);
        content = fm.importSimpleTextContent();
        for(String row: content) {
          if(row.startsWith("  ")) {
            continue;
          }
          String[] data = row.split(" ");
          this.allTerms[i].put(data[0], new Integer(1));
        }
      }
      
      
      /*
       * LOADS EXCEPTIONS FILES
       */
      String[] excFiles = new String[4];
      excFiles[0] = this.prp.getProperty("mdfsa.extraction.nounexc");
      excFiles[1] = this.prp.getProperty("mdfsa.extraction.verbexc");
      excFiles[2] = this.prp.getProperty("mdfsa.extraction.adjexc");
      excFiles[3] = this.prp.getProperty("mdfsa.extraction.advexc");
      for(int i = 0; i < 4; i++) {
        fm = new FileManager(excFiles[i], Mode.READ);
        content = fm.importSimpleTextContent();
        for(String row: content) {
          String[] curExc = row.split(" ");
          this.allExceptions[i].put(curExc[0], curExc[1]);
        }
      }
            
      
      /*
       * LOADS STOPWORDS FILE
       */
      String stopwordsFile = this.prp.getProperty("mdfsa.extraction.stopwords");
      fm = new FileManager(stopwordsFile, Mode.READ);
      content = fm.importSimpleTextContent();
      for(String row: content) {
        this.stopwords.put(row, 1);
      }
      
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  
  
  /**
   * Updates the knowledge graph with information coming from WordNet
   * @param g the knowledge graph
   * @return the updated knowledge graph
   */
  public Graph putInGraph(Graph g) {
    
    /* Adds the mappings between terms and synsets to the graph. It is used during the propagation phase because information
     * from the dataset comes through text-terms and not by using synsets. */
    g.setWnWordSynsets(this.wordSynsets);
    
    /* Creates the relations between synsets */
    Iterator<Long> synsets = this.synsetRelations.keySet().iterator();
    while(synsets.hasNext()) {
      long currentSynset = synsets.next();
      ArrayList<WordNetRelation> synRelations = this.synsetRelations.get(currentSynset);
      for(WordNetRelation rel: synRelations) {
        long targetSynset = rel.getTargetSynset();
        double weight = rel.getRelationWeight();
        g.addEdge(String.valueOf(currentSynset), String.valueOf(targetSynset), Double.MAX_VALUE, weight, 1);
      }
    }
    
    /* Creates the relations between SenticNet and the unambiguous synsets of WordNet.
     * Such relations are created directly in the Graph object due to the fact that all information are already
     * defined in it. */
    g.createSenticNetWordNetRelations();
    
    return g;
  }


  public HashMap<String, Integer>[] getAllTerms() {
    return allTerms;
  }


  public void setAllTerms(HashMap<String, Integer>[] allTerms) {
    this.allTerms = allTerms;
  }


  public HashMap<String, Integer> getAllNouns() {
    return allNouns;
  }


  public void setAllNouns(HashMap<String, Integer> allNouns) {
    this.allNouns = allNouns;
  }


  public HashMap<String, Integer> getAllVerbs() {
    return allVerbs;
  }


  public void setAllVerbs(HashMap<String, Integer> allVerbs) {
    this.allVerbs = allVerbs;
  }


  public HashMap<String, Integer> getAllAdjs() {
    return allAdjs;
  }


  public void setAllAdjs(HashMap<String, Integer> allAdjs) {
    this.allAdjs = allAdjs;
  }


  public HashMap<String, Integer> getAllAdvs() {
    return allAdvs;
  }


  public void setAllAdvs(HashMap<String, Integer> allAdvs) {
    this.allAdvs = allAdvs;
  }


  public HashMap<String, String>[] getAllExceptions() {
    return allExceptions;
  }


  public void setAllExceptions(HashMap<String, String>[] allExceptions) {
    this.allExceptions = allExceptions;
  }


  public HashMap<String, String> getNounExceptions() {
    return nounExceptions;
  }


  public void setNounExceptions(HashMap<String, String> nounExceptions) {
    this.nounExceptions = nounExceptions;
  }


  public HashMap<String, String> getVerbExceptions() {
    return verbExceptions;
  }


  public void setVerbExceptions(HashMap<String, String> verbExceptions) {
    this.verbExceptions = verbExceptions;
  }


  public HashMap<String, String> getAdjExceptions() {
    return adjExceptions;
  }


  public void setAdjExceptions(HashMap<String, String> adjExceptions) {
    this.adjExceptions = adjExceptions;
  }


  public HashMap<String, String> getAdvExceptions() {
    return advExceptions;
  }


  public void setAdvExceptions(HashMap<String, String> advExceptions) {
    this.advExceptions = advExceptions;
  }


  public HashMap<String, Integer> getStopwords() {
    return this.stopwords;
  }


  public void setStopwords(HashMap<String, Integer> stopwords) {
    this.stopwords = stopwords;
  }
  
}
