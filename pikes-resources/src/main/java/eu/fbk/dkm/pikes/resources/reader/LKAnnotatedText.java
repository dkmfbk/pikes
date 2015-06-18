package eu.fbk.dkm.pikes.resources.reader;

import java.util.ArrayList;
import java.util.HashMap;

public class LKAnnotatedText {

	public String rawText;
	public ArrayList<LKAnnotationLayer> layers;

	public HashMap<String, String> metaInfo;
	
	public LKAnnotationLayer getLayer(String provides) {
		for(LKAnnotationLayer l: layers)
			if(l.provides.equals(provides))
				return l;
		return null;
	}
	
}

