/**
 *
 */
package eu.fbk.dkm.pikes.resources.reader;

import java.util.ArrayList;
import java.util.HashMap;


public class LKAnnotationLayer {
	public String provides = null;
	public String scopeFile = null;
	public ArrayList<String> onFiles = null;

	public ArrayList<LKAnnotationEntity> entityList = new ArrayList();
	public HashMap<String, Integer> idToIndex = new HashMap();

}