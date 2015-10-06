package eu.fbk.dkm.pikes.raid.mdfsa.parser;

import edu.stanford.nlp.trees.Tree;
import eu.fbk.dkm.pikes.raid.mdfsa.FileManager;
import eu.fbk.dkm.pikes.raid.mdfsa.FileManager.Mode;
import eu.fbk.dkm.pikes.raid.mdfsa.MaxEntTagger;
import eu.fbk.shell.mdfsa.data.structures.SentenceStructuredRepresentation;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLexicalizer;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLoader;

import java.util.ArrayList;
import java.util.Properties;

public class DatasetInstanceParser {

  private Properties prp;
  private WordNetLoader wnl;
  private WordNetLexicalizer wnlex;
  private DependenciesBuilder db;
  private MaxEntTagger met;
  
  public DatasetInstanceParser(Properties prp, WordNetLoader wnl) {
    this.prp = prp;
    this.wnl = wnl;
    if(this.wnl != null) {
      this.wnlex = new WordNetLexicalizer(wnl.getAllTerms(), this.wnl.getAllExceptions());
   }
   this.db = new DependenciesBuilder();
   this.met = new MaxEntTagger(this.prp);
   this.db.init();
  }
  
  
    
  /**
   * Loads a review-format file during a simple execution
   */
  public SentenceStructuredRepresentation createSentenceStructuredRepresentation(String originalText, String id) {
      
    //System.out.println("Loading sentence " + originalText);
          
    /* Gets the review text */
    String text = originalText.replaceAll("\\.", " \\. ");
    text = text.replaceAll("\\:", " \\: ");
    text = text.replaceAll("\\,", " \\, ");
    text = text.replaceAll("\\!", " \\! ");
    text = text.replaceAll("\\?", " \\? ");
    text = text.replaceAll("( )+", " ");
    
    /*
    currentReview = currentReview.replace(".", " . ");
    currentReview = currentReview.replace("\"", " ");
    currentReview = currentReview.replace("!", " ! ");
    currentReview = currentReview.replace("?", " ? ");
    currentReview = currentReview.replace(":", " : ");
    currentReview = currentReview.replace(";", " ; ");
    currentReview = currentReview.replace(",", " , ");
    currentReview = currentReview.replace("(", " ");
    currentReview = currentReview.replace(")", " ");
    currentReview = currentReview.replace("[", " ");
    currentReview = currentReview.replace("]", " ");
    currentReview = currentReview.replace("\\", " ");
    currentReview = currentReview.replace("$", " ");
    currentReview = currentReview.replace("%", " ");
    currentReview = currentReview.replace("=", " ");
    currentReview = currentReview.replace("_", " ");
    currentReview = currentReview.replace("+", " ");
    currentReview = currentReview.replace("&", " ");
    currentReview = currentReview.replace("^", " ");
    currentReview = currentReview.replace("|", " ");
    currentReview = currentReview.replace("@", " ");
    currentReview = currentReview.replace("`", " ");
    currentReview = currentReview.trim();
    */
    
    text = text.replaceAll("\t", "");

    
    /* Tags the review with the part-of-speech tags */
    String taggedReview = null;
              
    /* Builds the dependent tree of the text */
    ArrayList<DependencyTree> curDt = null;

    /* Extracts the parser tree of the sentence */
    ArrayList<Tree> parsedTree = null;
      
    /* Tags the review with the part-of-speech tags */
    taggedReview = this.met.tag(text);
    taggedReview = taggedReview.replaceAll("/\\.", "/\\. ");
        
    /* Builds the dependent tree of the text */
    this.db.buildDependeciesTree(text.toLowerCase());
    curDt = this.db.getDependencyTrees();
  
    /* Extracts the parser tree of the sentence */
    parsedTree = this.db.getParsedTrees();
     
    
    /* Creates and sets the sentence object */
    SentenceStructuredRepresentation ssr = new SentenceStructuredRepresentation(this.prp);
    ssr.setUri(id);
    ssr.setOriginalText(originalText);
    ssr.setPosTaggedString(taggedReview);
    ssr.setDependencyTree(curDt);
    ssr.setParsedTree(parsedTree);
    
    
    if(this.wnlex != null) {
      ssr.createLexicalizedRepresentation(this.wnlex);
      ssr.extractSemanticConcepts(this.wnl, this.wnlex);
      ssr.extractAspects(this.wnl);
    }
    
   
    return ssr;
  }
  
  
  
  
  /**
   * Utility method that convert the blitzer review in the eswc2014 challenge format
   */
  public void convertReviewToESWCChallenge(String filename, String datasetName) {
    FileManager fm = new FileManager(filename, Mode.READ);
    //String content = fm.importFullTextContent();
    ArrayList<String> contents = fm.importSimpleTextContent();
    
    FileManager rdfOut = new FileManager("/home/drago/Documents/java_projects/research/nlp/multi_domain_fuzzy_sentiment_analysis/eswc2014_challenge_mdfsa_dragoni/task3/" + datasetName + ".validation.rdf.xml", Mode.WRITE);
    
    rdfOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    rdfOut.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
    int reviewId = 1;
    //int startText = content.indexOf("<review_text");
    //int endText = content.indexOf("</review_text", startText + 10);
    //while (startText != -1) {
    for(String currentReview: contents) {
      /* Gets the review text */
      //String currentReview = content.substring(startText + 14, endText - 1).replaceAll("\n", "");
      currentReview = currentReview.replace("&", "&amp;");
      //currentReview = "I gave up to go to supermarkets yesterday.";
            
      /* Write the review in the RDF format */
      rdfOut.write("\t<rdf:Description rdf:about=\"http://sentic.net/challenge/sentence_" + reviewId + "\">");
      //rdfOut.write("\t\t<sentence xmlns=\"http://sentic.net/challenge/\" rdf:resource=\"http://sentic.net/challenge/sentence_" + reviewId + "\">");
      rdfOut.write("\t\t\t<text xmlns=\"http://sentic.net/challenge/\" rdf:datatype=\"http://www.w3.org/TR/rdf-text/\">");
      //rdfOut.write("\t\t\t<![CDATA[" + currentReview + "]]>");
      rdfOut.write("\t\t\t" + currentReview + "");
      rdfOut.write("\t\t\t</text>");
      //rdfOut.write("\t\t</sentence>");
      rdfOut.write("\t</rdf:Description>");
      
      
      /* Gets next text to analyze */
      //startText = content.indexOf("<review_text", endText + 10);
      //endText = content.indexOf("</review_text", startText + 10);
      //System.out.println(reviewId + " - " + startText + " - " + endText);
      reviewId++;
    }
    rdfOut.write("</rdf:RDF>");
    rdfOut.close();
    fm.close();
  }
  
  
}
