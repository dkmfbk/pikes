package eu.fbk.dkm.pikes.tintop.old;

import com.machinelinking.api.client.AnnotationResponse;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.tintop.annotators.raw.DBpediaSpotlightTag;
import ixa.kaflib.LinguisticProcessor;
import org.apache.log4j.Logger;
import se.lth.cs.srl.corpus.Sentence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 17/07/14
 * Time: 17:35
 * To change this template use File | Settings | File Templates.
 */

public class CachedParsedText implements Serializable {

	static Logger logger = Logger.getLogger(CachedParsedText.class.getName());

	List<CoreMap> stanford;
	List<Sentence> mate, mateBe;
	Map<Integer, CorefChain> coreference;
	AnnotationResponse ml;
	List<DBpediaSpotlightTag> dbpTags;
	List<LinguisticProcessor> lps = new ArrayList<>();

	public List<LinguisticProcessor> getLps() {
		return lps;
	}

	public void setLps(List<LinguisticProcessor> lps) {
		this.lps = lps;
	}

	public void addLinguisticProcessor(LinguisticProcessor l) {
		this.lps.add(l);
	}

	public List<DBpediaSpotlightTag> getDbpTags() {
		return dbpTags;
	}

	public void setDbpTags(List<DBpediaSpotlightTag> dbpTags) {
		this.dbpTags = dbpTags;
	}

	public AnnotationResponse getMl() {
		return ml;
	}

	public void setMl(AnnotationResponse ml) {
		this.ml = ml;
	}

	public Map<Integer, CorefChain> getCoreference() {
		return coreference;
	}

	public void setCoreference(Map<Integer, CorefChain> coreference) {
		this.coreference = coreference;
	}

	public List<Sentence> getMate() {
		return mate;
	}

	public void setMate(List<Sentence> mate) {
		this.mate = mate;
	}

	public List<CoreMap> getStanford() {
		return stanford;
	}

	public void setStanford(List<CoreMap> stanford) {
		this.stanford = stanford;
	}

	public List<Sentence> getMateBe() {
		return mateBe;
	}

	public void setMateBe(List<Sentence> mateBe) {
		this.mateBe = mateBe;
	}

	public CachedParsedText() {

	}
}
