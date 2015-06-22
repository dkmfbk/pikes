package eu.fbk.dkm.pikes.tintop.annotators.raw;

import com.machinelinking.api.client.Keyword;
import com.machinelinking.api.client.Topic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 22/07/14
 * Time: 09:15
 * To change this template use File | Settings | File Templates.
 */

public class AnnotatedEntity {

	private String link;
	private float rel;
	private Object support;
	private String source;
	private int startIndex;
	private int endIndex;

	private List<Topic> topics = new ArrayList<>();

	public List<Topic> getTopics() {
		return topics;
	}

	public void setTopics(List<Topic> topics) {
		this.topics = topics;
	}

	public AnnotatedEntity(Keyword k) {
		this("http://dbpedia.org/resource/" + k.getSensePage(), k.getRel(), k, "machinelinking");
	}

	public AnnotatedEntity(DBpediaSpotlightTag k) {
		this(k.getPage(), (float) k.getScore(), k, "dbpedia_spotlight");
		this.setStartIndex(k.getOffset());
		this.setEndIndex(k.getOffset() + k.getLength());
	}

	@Override
	public String toString() {
		return "AnnotatedEntity{" +
				"link='" + link + '\'' +
				", rel=" + rel +
				", support=" + support +
				", source='" + source + '\'' +
				", startIndex=" + startIndex +
				", endIndex=" + endIndex +
				", topics=" + topics +
				'}';
	}

	public AnnotatedEntity(String link, float rel, Object support, String source) {
		this.link = link;
		this.rel = rel;
		this.support = support;
		this.source = source;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public float getRel() {
		return rel;
	}

	public void setRel(float rel) {
		this.rel = rel;
	}

	public Object getSupport() {
		return support;
	}

	public void setSupport(Object support) {
		this.support = support;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
