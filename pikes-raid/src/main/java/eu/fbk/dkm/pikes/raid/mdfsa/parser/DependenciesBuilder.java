package eu.fbk.dkm.pikes.raid.mdfsa.parser;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.*;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DependenciesBuilder {

  private LexicalizedParser lp;
  private TreebankLanguagePack tlp;
  private GrammaticalStructureFactory gsf;
  private ArrayList<DependencyTree> parsedTree;
  private ArrayList<Tree> trees;
  
  public DependenciesBuilder() {
  }
  
  public void init() {
    this.lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
                                          "-maxLength", "80", "-retainTmpSubcategories");
    this.tlp = new PennTreebankLanguagePack();
    this.gsf = this.tlp.grammaticalStructureFactory();
    //this.parsedTree = new ArrayList<DependencyTree>();
    //this.trees = new ArrayList<Tree>();
  }
  
  public ArrayList<Tree> getParsedTrees() {
    return this.trees;
  }
  
  public ArrayList<DependencyTree> getDependencyTrees() {
    return this.parsedTree;
  }
  
  public void buildDependeciesTree(String text) {
    this.parsedTree = new ArrayList<DependencyTree>();
    this.trees = new ArrayList<Tree>();
    
    Reader reader = new StringReader(text);
    DocumentPreprocessor dp = new DocumentPreprocessor(reader);
    for (List<HasWord> sentence : new DocumentPreprocessor(reader)) {

      //String[] sent = text.split(" ");
      //this.parsedTree = this.lp.apply(Sentence.toWordList(sent));
      Tree parsedTree = this.lp.apply(sentence);
      //TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
      //tp.printTree(parse);
      GrammaticalStructure gs = this.gsf.newGrammaticalStructure(parsedTree);
      Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
      
      DependencyTree dt = new DependencyTree();
      for(TypedDependency td: tdl) {
        TreeGraphNode dep = td.dep();
        TreeGraphNode gov = td.gov();
        GrammaticalRelation gr = td.reln();
        String depString = gr.toString() + "^^^" + gov.toString() + "^^^" + dep.toString();
        //System.out.println(depString);
        dt.addDependency(depString);
      }
      this.parsedTree.add(dt);
      this.trees.add(parsedTree);
    }
  }
  
  
  
  public ArrayList<DependencyTree> buildDependeciesTrees(ArrayList<String> texts) {
    ArrayList<DependencyTree> dtList = new ArrayList<DependencyTree>();
    int textId = 1;
    for(String text: texts) {
     System.out.println(textId);
     String[] sent = text.split(" ");
      Tree parse = this.lp.apply(Sentence.toWordList(sent));
      GrammaticalStructure gs = this.gsf.newGrammaticalStructure(parse);
      Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
      DependencyTree curDT = new DependencyTree();
      for(TypedDependency td: tdl) {
        TreeGraphNode dep = td.dep();
        TreeGraphNode gov = td.gov();
        GrammaticalRelation gr = td.reln();
        String depString = gr.toString() + "^^^" + gov.toString() + "^^^" + dep.toString();
        curDT.addDependency(depString);
      }
      textId++;
      dtList.add(curDT);
    }

    return dtList;
  }

}
