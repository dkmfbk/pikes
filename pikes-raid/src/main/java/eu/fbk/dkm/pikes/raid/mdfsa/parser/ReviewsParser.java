package eu.fbk.dkm.pikes.raid.mdfsa.parser;

import com.hp.hpl.jena.rdf.model.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;
import eu.fbk.dkm.pikes.raid.mdfsa.FileManager;
import eu.fbk.dkm.pikes.raid.mdfsa.FileManager.Mode;
import eu.fbk.dkm.pikes.raid.mdfsa.MaxEntTagger;
import eu.fbk.shell.mdfsa.data.structures.SentenceStructuredRepresentation;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLexicalizer;
import eu.fbk.dkm.pikes.raid.mdfsa.wordnet.WordNetLoader;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Properties;

public class ReviewsParser {

  private Properties prp;
  private WordNetLoader wnl;
  
  public ReviewsParser(Properties prp, WordNetLoader wnl) {
    this.prp = prp;
    this.wnl = wnl;
  }
  
  
  /**
   * Loads the current dataset name during the full simulation execution
   * @param datasetName dataset name
   */
  public Document[] loadFull(String datasetName) {
    String positiveReviews = ((String) this.prp.getProperty("mdfsa.dataset.basepath")).concat(datasetName + "/positive.review");
    String negativeReviews = ((String) this.prp.getProperty("mdfsa.dataset.basepath")).concat(datasetName + "/negative.review");
    String allReviews = ((String) this.prp.getProperty("mdfsa.dataset.basepath")).concat(datasetName + "/all.review");
    return null;
  }
  
  
   
  /**
   * Loads a review-format file during a simple execution
   */
  public ArrayList<SentenceStructuredRepresentation> load(String filename) {
    
    WordNetLexicalizer wnlex = new WordNetLexicalizer(this.wnl.getAllTerms(), this.wnl.getAllExceptions());
    ArrayList<SentenceStructuredRepresentation> ssrList = new ArrayList<SentenceStructuredRepresentation>();
    LexicalizedParser treeParser;
    DependenciesBuilder db = new DependenciesBuilder();
    MaxEntTagger met = new MaxEntTagger(this.prp);
    db.init();
    FileManager fm = new FileManager(filename, Mode.READ);
    Model content = fm.importRDFContent();
    
    String task = prp.getProperty("mdfsa.task");
    
    int reviewId = 1;
    // Lists the statements in the Model
    StmtIterator iter = content.listStatements();

    System.out.println(content.size());
    int stmtID = 0;
    // Prints out the predicate, subject, and object of each statement
    while (iter.hasNext()) {
      System.out.println("Loading sentence " + reviewId);
      Statement stmt      = iter.nextStatement();  // get next statement
      Resource  subject   = stmt.getSubject();     // get the subject
      Property  predicate = stmt.getPredicate();   // get the predicate
      RDFNode   object    = stmt.getObject();      // get the object
      
      /* Gets the review text */
      int endText = object.toString().indexOf("^^");
      String currentReviewOriginal = object.toString().substring(1, endText).replaceAll("\n", "");
      String currentReview = currentReviewOriginal.replaceAll("\\.", " \\. ");
      currentReview = currentReview.replaceAll("\\:", " \\: ");
      currentReview = currentReview.replaceAll("\\,", " \\, ");
      currentReview = currentReview.replaceAll("\\!", " \\! ");
      currentReview = currentReview.replaceAll("\\?", " \\? ");
      currentReview = currentReview.replaceAll("( )+", " ");
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
      currentReview = currentReview.replaceAll("\t", "");
      //System.out.println(currentReview);
      //currentReview = "I gave up to go to supermarkets yesterday.";
      
      /* Tags the review with the part-of-speech tags */
      String taggedReview = null;
                
      /* Builds the dependenct tree of the text */
      ArrayList<DependencyTree> curDt = null;
  
      /* Extracts the parser tree of the sentece */
      ArrayList<Tree> parsedTree = null;
      
      //if(task.compareTo("AdvancedOne") == 0 || task.compareTo("AdvancedTwo") == 0) {
      
        /* Tags the review with the part-of-speech tags */
        taggedReview = met.tag(currentReview);
        taggedReview = taggedReview.replaceAll("/\\.", "/\\. ");
          
        /* Builds the dependenct tree of the text */
        db.buildDependeciesTree(currentReview.toLowerCase());
    
        /* Extracts the parser tree of the sentece */
        curDt = db.getDependencyTrees();
        parsedTree = db.getParsedTrees();
        
      /*} else {
        
        /* Tags the review with the part-of-speech tags */
        //taggedReview = null;
                  
        /* Builds the dependenct tree of the text */
        //curDt = null;
    
        /* Extracts the parser tree of the sentece */
        //parsedTree = null;
      //}
      
      /* Creates and sets the sentence object */
      SentenceStructuredRepresentation ssr = new SentenceStructuredRepresentation(this.prp);
      ssr.setUri(subject.toString());
      ssr.setOriginalText(currentReviewOriginal);
      ssr.setPosTaggedString(taggedReview);
      ssr.setDependencyTree(curDt);
      ssr.setParsedTree(parsedTree);
      //ssr.extractTree(parsedTree);
      //System.out.println(parsedTree);
      //System.exit(0);
      
      //if(task.compareTo("AdvancedOne") == 0 || task.compareTo("AdvancedTwo") == 0) {
        ssr.createLexicalizedRepresentation(wnlex);
        ssr.extractSemanticConcepts(this.wnl, wnlex);
        ssr.extractAspects(this.wnl);
      //}
      
      ssrList.add(ssr);
      
      /* Gets next text to analyze */
      //startText = content.indexOf("<review_text", endText + 10);
      //endText = content.indexOf("</review_text", startText + 10);
      //System.out.println(reviewId + " - " + startText + " - " + endText);
      reviewId++;
    }
    return ssrList;
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
