package eu.fbk.dkm.pikes.raid.mdfsa;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.shell.mdfsa.data.structures.DomainGraph;
import eu.fbk.shell.mdfsa.data.structures.FuzzyMembership;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLexicalizer;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLoader;
import org.tartarus.snowball.ext.porterStemmer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class APIManager {

  private Properties prp;
  private DomainGraph domain;
  private WordNetLoader wnl;
  private WordNetLexicalizer wnlex;
  private HashMap<String, Long> labels;
  private HashMap<Long, FuzzyMembership> polarities;

  public APIManager() throws IOException {
    this.prp = new Properties();
    InputStream iS = ClassLoader.getSystemClassLoader().getSystemResourceAsStream("mdfsa.properties");
    prp.load(iS);
  }


  public APIManager(Properties prp) throws IOException {
    this.prp = prp;
  }


  public void loadModel(String modelPath) {
    this.wnl = new WordNetLoader(this.prp);
    this.wnl.load();
    this.domain = new DomainGraph(this.prp);
    this.domain.setDomainGraphFromSerializedData(modelPath);
    this.wnlex = new WordNetLexicalizer(this.wnl.getAllTerms(), this.wnl.getAllExceptions());
    this.labels = this.domain.getGraph().getLabels();
    this.polarities = this.domain.getPolarities();
  }


  public double evaluateSentence(String text) {
    double textPolarity = -2.0;
    double fuzzyShapeFound = 0.0;
    double tempPolarity = 0.0;

    //WordNetLexicalizer wnlex = new WordNetLexicalizer(this.wnl.getAllTerms(), this.wnl.getAllExceptions());
    //HashMap<String, Long> labels = this.domain.getGraph().getLabels();
    //HashMap<Long, FuzzyMembership> polarities = this.domain.getPolarities();
//    System.out.print(text);

    HashMap<String, Integer> sentenceTokens = new HashMap<String, Integer>();
    HashMap<String, Integer> stemmedTokens = new HashMap<String, Integer>();

    FuzzyMembership eT = new FuzzyMembership(1.0, 1.0, -1.0, -1.0);
    String[] tokens = text.split(" ");
    for(int i = 0; i < tokens.length; i++) {
      if(tokens[i].compareTo("") != 0) {
        String lexToken = this.wnlex.getWordLexicalizationByType(tokens[i].toLowerCase(), "MIX");
        //System.out.print(" " + lexToken + " ");
        if(lexToken != null) {
          tokens[i] = lexToken;
        }
        sentenceTokens.put(tokens[i], new Integer(i));

        porterStemmer stemmer = new porterStemmer();
        stemmer.setCurrent(tokens[i].toLowerCase());
        String stemLink = tokens[i].toLowerCase();
        if(stemmer.stem()) {
          stemLink = stemmer.getCurrent();
          //System.out.print(stemLink + " ");
        }
        stemmedTokens.put(stemLink, new Integer(i));
      }
    }
    Iterator<String> it = this.labels.keySet().iterator();
    while(it.hasNext()) {
      String currentConcept = it.next();
      String[] cts = currentConcept.split("_");
      int higherIdx = 0;
      int lowerIdx = tokens.length;
      int foundCT = 0;
      int flagNegation = 1;
      for(String ct: cts) {
        
        /*
        if(ct.compareTo("regret") == 0) {
          int A = 1;
        }
        */

        Integer tempIdx = sentenceTokens.get(ct);
        if(tempIdx == null) {
          tempIdx = stemmedTokens.get(ct);
        }
        if(tempIdx != null) {
          if(tempIdx < lowerIdx) lowerIdx = tempIdx;
          if(tempIdx > higherIdx) higherIdx = tempIdx;
          foundCT++;
        }
      }
      Integer notToken = sentenceTokens.get("not");
      if(notToken == null) {
        //notToken = sentenceTokens.get("no");
      }

      if(notToken != null &&
        (
         (notToken >= (lowerIdx - 2)) ||
         (
          (notToken > lowerIdx) && (notToken < higherIdx)
         )
        )
        ) {
        flagNegation = -1;
      }
      /*
      if(notToken != null && notToken == lowerIdx - 1) {
        flagNegation = -1;
      }
      */
      if(higherIdx >= 0 && foundCT == cts.length && (higherIdx - lowerIdx) < (cts.length + 2)) {

        Long feature = this.labels.get(currentConcept);
        //Long feature = labels.get(stemLink);
        
        /*
        double ratioFactor = 1.0 / (sentenceTokens.size() - lowerIdx);
        if(ratioFactor == Double.NaN || ratioFactor == 0.0) {
          ratioFactor = 1.0;
        }
        */
        double ratioFactor = 1.0;

        FuzzyMembership fm = this.polarities.get(feature);
        double a = 0.0;
        try {
          a = fm.getA() * ratioFactor * flagNegation;
        } catch (Exception e) {
          System.out.println("Error on getting fuzzy shape: " + currentConcept);
          //System.exit(0);
          return -2.0;
        }

        double b = fm.getB() * ratioFactor * flagNegation;
        double c = fm.getC() * ratioFactor * flagNegation;
        double d = fm.getD() * ratioFactor * flagNegation;

        if(flagNegation == -1) {
          double t = a;
          a = d;
          d = t;

          t = b;
          b = c;
          c = t;
        }

        double eA = eT.getA();
        double eB = eT.getB();
        double eC = eT.getC();
        double eD = eT.getD();

        if(a < eA) eA = a;
        if(b < eB) eB = b;
        if(c > eC) eC = c;
        if(d > eD) eD = d;

        eT.setA(eA);
        eT.setB(eB);
        eT.setC(eC);
        eT.setD(eD);

        fuzzyShapeFound += 1.0;
        tempPolarity += fm.getCentroidXAxis();
      }
    }

    if(eT.getA() != 1.0) {
      //textPolarity = eT.getCentroid();
      textPolarity = eT.getCentroidXAxis();
      if(Double.isNaN(textPolarity)) {
        return -2.0;
      }
      //textPolarity = tempPolarity / fuzzyShapeFound;
    }

    return textPolarity;
  }






  public double evaluateSentence(CoreMap sentence, int startNodeId, ArrayList<Integer> blockedNodes) {
    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
    String[] dependenciesList = dependencies.toString(SemanticGraph.OutputFormat.LIST).split("\n");
    return evaluateSentence(dependenciesList, startNodeId, blockedNodes);
  }

  public double evaluateSentence(String[] dependenciesList, int startNodeId, ArrayList<Integer> blockedNodes) {
    HashMap<Integer, String> tokensToPolarize = new HashMap<Integer, String>();
    double textPolarity = -2.0;
    double fuzzyShapeFound = 0.0;
    double tempPolarity = 0.0;

    int tIdx = 0;
    HashMap<Integer, Integer> tokensToBlock = new HashMap<Integer, Integer>();
    ArrayList<Integer> tokensToAnalyze = new ArrayList<Integer>();

    for(Integer bn: blockedNodes) {
      tokensToBlock.put(bn, bn);
    }

    tokensToAnalyze.add(startNodeId);
    while(tokensToAnalyze.size() > 0) {
      int currentNode = tokensToAnalyze.get(0);
      for(int i = 0; i < dependenciesList.length; i++) {
        String[] rel = dependenciesList[i].substring(dependenciesList[i].indexOf("(") + 1, dependenciesList[i].indexOf(")")).split(", ");
        String[] gov = rel[0].split("-");
        String[] dep = rel[1].split("-");
        Integer blockFlag = tokensToBlock.get(Integer.valueOf(gov[gov.length - 1]));
        if(Integer.valueOf(gov[gov.length - 1]) == currentNode && blockFlag == null) {

          String tokenToAdd;

          /**
           * If the map is still empty, it means that the starting node has not been saved. So, this save it.
           */
          if(tokensToPolarize.size() == 0) {
            if(gov.length > 2) {
              tokenToAdd = new String(gov[0]);
              for(int j = 1; j < gov.length - 2; j++) {
                tokenToAdd = tokenToAdd.concat(" " + gov[j]);
              }
              tokenToAdd = tokenToAdd.trim();
            } else {
              tokenToAdd = new String(gov[0]);
            }
            tokensToPolarize.put(Integer.valueOf(gov[gov.length - 1]), tokenToAdd);
            if(Integer.valueOf(gov[gov.length - 1]) > tIdx) {
              tIdx = Integer.valueOf(gov[gov.length - 1]);
            }
          }


          /**
           * Saves the dependent
           */
          if(dep.length > 2) {
            tokenToAdd = new String(dep[0]);
            for(int j = 1; j < dep.length - 2; j++) {
              tokenToAdd = tokenToAdd.concat(" " + dep[j]);
            }
            tokenToAdd = tokenToAdd.trim();
          } else {
            tokenToAdd = new String(dep[0]);
          }
          tokensToPolarize.put(Integer.valueOf(dep[dep.length - 1]), tokenToAdd);
          tokensToAnalyze.add(Integer.valueOf(dep[dep.length - 1]));
          if(Integer.valueOf(dep[dep.length - 1]) > tIdx) {
            tIdx = Integer.valueOf(dep[dep.length - 1]);
          }
        }
      }
      tokensToAnalyze.remove(0);
    }

    String text = new String();
    for(int i = 0; i <= tIdx; i++) {
      String token = tokensToPolarize.get(new Integer(i));
      if(token != null) {
        text = text.concat(token + " ");
      }
    }
    text = text.trim();


    //WordNetLexicalizer wnlex = new WordNetLexicalizer(this.wnl.getAllTerms(), this.wnl.getAllExceptions());
    //HashMap<String, Long> labels = this.domain.getGraph().getLabels();
    //HashMap<Long, FuzzyMembership> polarities = this.domain.getPolarities();
//    System.out.println(text);

    HashMap<String, Integer> sentenceTokens = new HashMap<String, Integer>();
    HashMap<String, Integer> stemmedTokens = new HashMap<String, Integer>();

    FuzzyMembership eT = new FuzzyMembership(1.0, 1.0, -1.0, -1.0);
    String[] tokens = text.split(" ");
    for(int i = 0; i < tokens.length; i++) {
      if(tokens[i].compareTo("") != 0) {
        String lexToken = this.wnlex.getWordLexicalizationByType(tokens[i].toLowerCase(), "MIX");
        //System.out.print(" " + lexToken + " ");
        if(lexToken != null) {
          tokens[i] = lexToken;
        }
        sentenceTokens.put(tokens[i], new Integer(i));

        porterStemmer stemmer = new porterStemmer();
        stemmer.setCurrent(tokens[i].toLowerCase());
        String stemLink = tokens[i].toLowerCase();
        if(stemmer.stem()) {
          stemLink = stemmer.getCurrent();
          //System.out.print(stemLink + " ");
        }
        stemmedTokens.put(stemLink, new Integer(i));
      }
    }
    Iterator<String> it = this.labels.keySet().iterator();
    while(it.hasNext()) {
      String currentConcept = it.next();
      String[] cts = currentConcept.split("_");
      int higherIdx = 0;
      int lowerIdx = tokens.length;
      int foundCT = 0;
      int flagNegation = 1;
      for(String ct: cts) {
        
        /*
        if(ct.compareTo("regret") == 0) {
          int A = 1;
        }
        */

        Integer tempIdx = sentenceTokens.get(ct);
        if(tempIdx == null) {
          tempIdx = stemmedTokens.get(ct);
        }
        if(tempIdx != null) {
          if(tempIdx < lowerIdx) lowerIdx = tempIdx;
          if(tempIdx > higherIdx) higherIdx = tempIdx;
          foundCT++;
        }
      }
      Integer notToken = sentenceTokens.get("not");
      if(notToken == null) {
        //notToken = sentenceTokens.get("no");
      }

      if(notToken != null &&
        (
         (notToken >= (lowerIdx - 2)) ||
         (
          (notToken > lowerIdx) && (notToken < higherIdx)
         )
        )
        ) {
        flagNegation = -1;
      }
      /*
      if(notToken != null && notToken == lowerIdx - 1) {
        flagNegation = -1;
      }
      */
      if(higherIdx >= 0 && foundCT == cts.length && (higherIdx - lowerIdx) < (cts.length + 2)) {

        Long feature = this.labels.get(currentConcept);
        //Long feature = labels.get(stemLink);
        
        /*
        double ratioFactor = 1.0 / (sentenceTokens.size() - lowerIdx);
        if(ratioFactor == Double.NaN || ratioFactor == 0.0) {
          ratioFactor = 1.0;
        }
        */
        double ratioFactor = 1.0;

        FuzzyMembership fm = this.polarities.get(feature);
        double a = 0.0;
        try {
          a = fm.getA() * ratioFactor * flagNegation;
        } catch (Exception e) {
          System.out.println("Error on getting fuzzy shape: " + currentConcept);
          //System.exit(0);
          return -2.0;
        }

        double b = fm.getB() * ratioFactor * flagNegation;
        double c = fm.getC() * ratioFactor * flagNegation;
        double d = fm.getD() * ratioFactor * flagNegation;

        if(flagNegation == -1) {
          double t = a;
          a = d;
          d = t;

          t = b;
          b = c;
          c = t;
        }

        double eA = eT.getA();
        double eB = eT.getB();
        double eC = eT.getC();
        double eD = eT.getD();

        if(a < eA) eA = a;
        if(b < eB) eB = b;
        if(c > eC) eC = c;
        if(d > eD) eD = d;

        eT.setA(eA);
        eT.setB(eB);
        eT.setC(eC);
        eT.setD(eD);

        fuzzyShapeFound += 1.0;
        tempPolarity += fm.getCentroidXAxis();
      }
    }

    if(eT.getA() != 1.0) {
      //textPolarity = eT.getCentroid();
      textPolarity = eT.getCentroidXAxis();
      if(Double.isNaN(textPolarity)) {
        return -2.0;
      }
      //textPolarity = tempPolarity / fuzzyShapeFound;
    }

    return textPolarity;
  }




}
