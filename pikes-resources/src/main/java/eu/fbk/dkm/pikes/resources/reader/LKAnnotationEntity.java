/**
 * 
 */
package eu.fbk.dkm.pikes.resources.reader;

import java.util.ArrayList;

public class LKAnnotationEntity {
	public int cstart = -1, cend = -1;
	//String id = null;
	public ArrayList<LKAnnotationEntity> referred;
	public LKAnnotationEntity from;
	public LKAnnotationEntity to;

	//String label = null;
	public DataElementNode data = null;

	//TOP uimaObject = null;

	public String localURI;

	@Override
	public String toString() {
		return "LKAnnotationEntity{" +
				"cstart=" + cstart +
				", cend=" + cend +
				", referred=" + referred +
				", from=" + from +
				", to=" + to +
				", data=" + data +
				", localURI='" + localURI + '\'' +
				'}';
	}
}