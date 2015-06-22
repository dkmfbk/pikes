package eu.fbk.dkm.pikes.raid.mdfsa.wordnet;

import java.util.HashMap;

public class WordNetLexicalizer
{

  private String[] SufxNoun = {"", "s", "ses", "xes", "zes", "ches", "shes", "men", "ies"};
  private String[] EndNoun = {"", "", "s", "x", "z", "ch", "sh", "man", "y"};
  private String[] SufxVerb = {"", "s", "ies", "es", "es", "ed", "ed", "ing", "ing"};
  private String[] EndVerb = {"", "", "y", "e", "", "e", "", "e", ""};
  private String[] SufxAdj = {"", "er", "est", "er", "est"};
  private String[] EndAdj = {"", "", "", "e", "e"};
  private String S;
  private String CurWord;
  
  private HashMap<String, Integer> allNouns;
  private HashMap<String, Integer> allVerbs;
  private HashMap<String, Integer> allAdjs;
  private HashMap<String, Integer> allAdvs;
  private HashMap<String, String> nounExceptions;
  private HashMap<String, String> verbExceptions;
  private HashMap<String, String> adjExceptions;
  private HashMap<String, String> advExceptions;
  
  private HashMap HNoun;
  private HashMap HVerb;
  private HashMap HAdj;
  private int WordType = -1;  //0: Noun; 1: Verb; 2: Adjective
  private int POSWordType = -1;

  private boolean IsNounAdj;

  
  public WordNetLexicalizer(HashMap<String, Integer>[] allTerms, HashMap<String, String>[] exceptions)
  {
    this.allNouns = allTerms[0];
    this.allVerbs = allTerms[1];
    this.allAdjs = allTerms[2];
    this.allAdvs = allTerms[3];
    this.nounExceptions = exceptions[0];
    this.verbExceptions = exceptions[1];
    this.adjExceptions = exceptions[2];
    this.advExceptions = exceptions[3];
    
    this.HAdj = null;
    this.HNoun = null;
    this.HVerb = null;
  }
  

  public boolean getIsNounAdj()
  {
    return this.IsNounAdj;
  }

  public int getWordType()
  {
    return this.WordType;
  }
  
  public void setPOSWordType(int t)
  {
  	this.POSWordType = t;
  }
  
  public void setWord(String word)
  {
    this.CurWord = word.substring(0);
    this.WordType = -1;
  }

  
  /**
   * Returns the lexicalization of a word for which the part-of-speech tag is known
   * @param word the word to lexicalize
   * @param posTag the part of speach tag
   * @return the lexicalized word
   */
  public String getWordLexicalizationByType(String word, String posTag) {
    String lexWord = null;
    if(posTag.compareTo("N") == 0) {
      lexWord = this.lexicalizeNoun(word);
    } else if(posTag.compareTo("V") == 0) {
      lexWord = this.lexicalizeVerb(word);
    } else if(posTag.compareTo("AJ") == 0) {
      lexWord = this.lexicalizeAdjective(word);
    } else if(posTag.compareTo("AV") == 0) {
      lexWord = this.lexicalizeAdverb(word);
    } else if(posTag.compareTo("MIX") == 0) {
      
      lexWord = this.lexicalizeNoun(word);
      if(lexWord == null) {
        lexWord = this.lexicalizeVerb(word);
      }
      if(lexWord == null) {
        lexWord = this.lexicalizeAdjective(word);
      }
      if(lexWord == null) {
        lexWord = this.lexicalizeAdverb(word);
      }
      
    }
    return lexWord;
  }
  
  
  
  /**
   * Lexicalizes the noun-term given as input
   * @param noun the noun to lexicalize
   * @return the lexicalized noun
   */
  private String lexicalizeNoun(String noun) {
    String lexNoun = null;
    String tempString;
    Integer existFlag;
    
    lexNoun = this.nounExceptions.get(noun);
    if(lexNoun != null) {
      return lexNoun;
    }
    
    /* Checks all possible suffixes */
    for (int I = 0; I < this.SufxNoun.length; I++) {
      if(noun.length() > this.SufxNoun[I].length()) {
        if (noun.substring(noun.length() - this.SufxNoun[I].length()).compareTo(this.SufxNoun[I]) == 0) {
          tempString = new String(noun.substring(0, noun.length() - this.SufxNoun[I].length()) + this.EndNoun[I]);
          existFlag = this.allNouns.get(tempString.toLowerCase());
          if (existFlag != null) {
            lexNoun = tempString.substring(0);
            break;
          }
        }
      }
    }
    return lexNoun;
  }
  
  
  /**
   * Lexicalizes the verb-term given as input
   * @param verb the verb to lexicalize
   * @return the lexicalized verb
   */
  private String lexicalizeVerb(String verb) {
    String lexVerb = null;
    String tempString;
    Integer existFlag;
    
    lexVerb = this.verbExceptions.get(verb);
    if(lexVerb != null) {
      return lexVerb;
    }
    
    /* Checks all possible suffixes */
    for (int I = 0; I < this.SufxVerb.length; I++) {
      if(verb.length() > this.SufxVerb[I].length()) {
        if (verb.substring(verb.length() - this.SufxVerb[I].length()).compareTo(this.SufxVerb[I]) == 0) {
          tempString = new String(verb.substring(0, verb.length() - this.SufxVerb[I].length()) + this.EndVerb[I]);
          existFlag = this.allVerbs.get(tempString.toLowerCase());
          if (existFlag != null) {
            lexVerb = tempString.substring(0);
            break;
          }
        }
      }
    }
    return lexVerb;
  }
  
  
  
  /**
   * Lexicalizes the adjective-term given as input
   * @param verb the adjective to lexicalize
   * @return the lexicalized adjective
   */
  private String lexicalizeAdjective(String adj) {
    String lexAdj = null;
    String tempString;
    Integer existFlag;
    
    lexAdj = this.adjExceptions.get(adj);
    if(lexAdj != null) {
      return lexAdj;
    }
    
    /* Checks all possible suffixes */
    for (int I = 0; I < this.SufxAdj.length; I++) {
      if(adj.length() > this.SufxAdj[I].length()) {
        if (adj.substring(adj.length() - this.SufxAdj[I].length()).compareTo(this.SufxAdj[I]) == 0) {
          tempString = new String(adj.substring(0, adj.length() - this.SufxAdj[I].length()) + this.EndAdj[I]);
          existFlag = this.allAdjs.get(tempString.toLowerCase());
          if (existFlag != null) {
            lexAdj = tempString.substring(0);
            break;
          }
        }
      }
    }
    return lexAdj;
  }
  
  
  /**
   * Lexicalizes the adverb-term given as input
   * @param verb the adverb to lexicalize
   * @return the lexicalized adverb
   */
  private String lexicalizeAdverb(String adv) {
    String lexAdv = null;
    String tempString;
    Integer existFlag;
    
    lexAdv = this.advExceptions.get(adv);
    if(lexAdv != null) {
      return lexAdv;
    }
    
    /* Checks all possible suffixes */
    for (int I = 0; I < this.SufxNoun.length; I++) {
      if(adv.length() > this.SufxNoun[I].length()) {
        if (adv.substring(adv.length() - this.SufxNoun[I].length()).compareTo(this.SufxNoun[I]) == 0) {
          tempString = new String(adv.substring(0, adv.length() - this.SufxNoun[I].length()) + this.EndNoun[I]);
          existFlag = this.allAdvs.get(tempString.toLowerCase());
          if (existFlag != null) {
            lexAdv = tempString.substring(0);
            break;
          }
        }
      }
    }
    return lexAdv;
  }
  
  
  
  
  
  
  

  public String getLexicalization()
  {
    this.S = null;
    this.S = new String();
    this.IsNounAdj = false;
    String TempString = null;
    String QueryString = null;

    try
    {

    	if(this.POSWordType == -1 || this.POSWordType == 0)
    	{
	      //Effettuo la scansione della categoria "N"
	      for (int I = 0; I < this.SufxNoun.length; I++)
	      {
	        if (this.CurWord.length() > this.SufxNoun[I].length())
	        {
	          if (this.CurWord.substring(this.CurWord.length() -
	                                     this.SufxNoun[I].length()).compareTo(
	                                     this.SufxNoun[I]) == 0)
	          {
	            TempString = new String(this.CurWord.substring(0, this.CurWord.length() -
	                                    this.SufxNoun[I].length()) +
	                                    this.EndNoun[I]);
	
	            QueryString = (String)this.HNoun.get(TempString.toLowerCase());
	            if (QueryString != null)
	            {
	              this.S = TempString.substring(0);
	              this.WordType = 0;
	              return this.S;
	            }
	          }
	        }
	      }
    	}


    	
    	if(this.POSWordType == -1 || this.POSWordType == 1)
    	{
	      //Effettuo la scansione della categoria "V"
	      for (int I = 0; I < this.SufxVerb.length; I++)
	      {
	        if (this.CurWord.length() > this.SufxNoun[I].length())
	        {
	          if (this.CurWord.substring(this.CurWord.length() -
	                                     this.SufxVerb[I].length()).compareTo(
	                                     this.SufxVerb[I]) == 0)
	
	          {
	            TempString = new String(this.CurWord.substring(0, this.CurWord.length() -
	                                    this.SufxVerb[I].length()) +
	                                    this.EndVerb[I]);
	
	
	            QueryString = (String)this.HVerb.get(TempString.toLowerCase());
	            if (QueryString != null)
	            {
	              this.S = TempString.substring(0);
	              this.WordType = 1;
	              return this.S;
	            }
	
	          }
	        }
	      }
    	}


    	
    	if(this.POSWordType == -1 || this.POSWordType == 2)
    	{
	      //Effettuo la scansione della categoria "A"
	      for (int I = 0; I < this.SufxAdj.length; I++)
	      {
	        if (this.CurWord.length() > this.SufxNoun[I].length())
	        {
	          if (this.CurWord.substring(this.CurWord.length() -
	                                     this.SufxAdj[I].length()).compareTo(
	                                     this.SufxAdj[I]) == 0)
	
	          {
	
	            TempString = new String(this.CurWord.substring(0, this.CurWord.length() -
	                                    this.SufxAdj[I].length()) +
	                                    this.EndAdj[I]);
	
	
	            QueryString = (String)this.HAdj.get(TempString.toLowerCase());
	            if (QueryString != null)
	            {
	              this.S = TempString.substring(0);
	              this.WordType = 2;
	              return this.S;
	            }
	
	          }
	        }
	      }
    	}

    }
    catch(Exception e){}

    return this.S;
  }

  
  
  public String checkCompoundNames(String CurrentString)
  {
  	String Result = "";
  	String[] Words = CurrentString.split(" ");
  	String QueryString, TempString;
  	String RS;
  	int J, CompoundNoun;
  	
  	int I = 0;
  	while(I < Words.length)
  	{
  		QueryString = Words[I];
  		if(QueryString.compareTo("STOPWORD") == 0){I++; continue;}
  		
  		CompoundNoun = 0;
  		J = I + 1;

  		while(CompoundNoun != 0)
  		{
  			if(Words[J].compareTo("STOPWORD") == 0) break;
  			TempString = QueryString.concat(" " + Words[J]);
  			RS = (String)this.HNoun.get(TempString.toLowerCase());
  			if(RS == null) RS = (String)this.HVerb.get(TempString.toLowerCase());
  			if(RS == null) RS = (String)this.HAdj.get(TempString.toLowerCase());
  			
  			if(RS != null) {QueryString = TempString; J++;}
  			else CompoundNoun = 1;
  		}
  		
  		Result = Result.concat(QueryString + " ");
  		I = J;
  	}
      	
  	return Result;
  }
}
