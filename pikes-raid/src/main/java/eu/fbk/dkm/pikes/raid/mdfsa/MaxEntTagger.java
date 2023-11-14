package eu.fbk.dkm.pikes.raid.mdfsa;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class MaxEntTagger
{
  private Properties prp;
	private String modelName;
	private MaxentTagger tagger;
	
	public MaxEntTagger(Properties prp) {
		try {
		  this.prp = prp;
			this.modelName = this.prp.getProperty("mdfsa.extraction.taggermodel");
			this.tagger = new MaxentTagger(this.modelName);
		} catch(Exception e) {
		  e.printStackTrace();
		  System.out.println("Impossible to initialize the tagger model.");
		}
	}
	
	
	public String tag(String fn) {
		String taggedString = new String();
		try	{
	    List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new StringReader(fn)));
	    for (List<HasWord> sentence : sentences) {
	    	ArrayList<TaggedWord> tSentence = (ArrayList<TaggedWord>) tagger.tagSentence(sentence);
	      taggedString = taggedString.concat(SentenceUtils.listToString(tSentence, false));
	    }
	    return taggedString; 
		} catch(Exception e) {
		  e.printStackTrace();
		  System.out.println("Error during the text tagging operation.");
		}
		return null;
	}
	
}
