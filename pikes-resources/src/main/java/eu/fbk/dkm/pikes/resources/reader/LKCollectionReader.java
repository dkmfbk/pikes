
package eu.fbk.dkm.pikes.resources.reader;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import se.lth.cs.nlp.nlputils.core.Ax;
import se.lth.cs.nlp.nlputils.core.ListMap;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* SAX stuff. */

public class LKCollectionReader {

	private ArrayList<File> textFiles = new ArrayList();
	private int nextFile = -1;

	private HashMap<String, ArrayList<String>> annFileNames = new HashMap();

	private static final Pattern BASE_PAT = Pattern.compile("name=\"base\">(.*)</tag>");

	private static final Pattern ON_FILE_PAT = Pattern.compile("scope=\"(.*?)\"");
	private static final Pattern ON_FILES_PAT = Pattern.compile("on-files=\"(.*?)\"");

	private String workDir = null;

	private HashSet<String> usedAnnotations; // unimplemented

	public LKCollectionReader(String dir) throws IOException {
		this(null, dir, null);
	}

	public LKCollectionReader(String dir, List<String> fileList) throws IOException {
		this(null, dir, fileList);
	}

	private LKCollectionReader(Collection<String> usedAnnotations, String dir,
							   List<String> fileList) throws IOException {
		workDir = dir;
		if (usedAnnotations != null) {
			this.usedAnnotations = new HashSet(usedAnnotations);
		}

		if (fileList == null) {
			makeFileList(dir);
		}
		else {
			makeFileList(dir, fileList);
		}

		nextFile = 0;

		//System.out.println(textFiles);
		//System.out.println(annFileNames);

	}

	private void makeFileList(String dir) throws IOException {
		File df = new File(dir);
		if (!df.isDirectory()) {
			throw new IllegalArgumentException("Must specify a directory");
		}

		File[] list = df.listFiles();
		for (File f : list) {

			//Scanner sc = new Scanner(f);
			BufferedReader br = Ax.openFileReader(f.getAbsolutePath());
			String line = br.readLine();

			int count = 0;
			while (count < 3 && line != null) {
				//String line = sc.nextLine();
				count++;
				if (line.contains("<lk-text")) {
					textFiles.add(f);
					//System.out.println("Text file: " + f);
					break;
				}
				if (line.contains("<lk-annotation")) {
					String base = null;
					while (line != null) {
						//String line2 = sc.nextLine();
						Matcher m = BASE_PAT.matcher(line);
						if (m.find()) {
							base = m.group(1);

							ArrayList<String> afns = annFileNames.get(base);
							if (afns == null) {
								afns = new ArrayList();
								annFileNames.put(base, afns);
								//System.out.println("Annotation file: " + f + "->" + base);
							}
							afns.add(f.getName());
							break;
						}
						line = br.readLine();
					}
					break;
				}
				line = br.readLine();
			}
			//sc.close();
			br.close();
		}
	}

	private void makeFileList(String dir, List<String> listedFiles) throws IOException {
		ArrayList<String> copy = new ArrayList(listedFiles);
		File df = new File(dir);
		if (!df.isDirectory()) {
			throw new IllegalArgumentException("Must specify a directory");
		}

		for (ListIterator<String> iter = copy.listIterator(); iter.hasNext(); ) {
			String fn = iter.next();
			//if(fn.startsWith("database/docs/")) {
			//	fn = fn.replaceFirst("database/docs/", "");
			//}
			if (!fn.endsWith(".xml")) // TODO gz/bz2???
			{
				fn = fn + ".lktext.xml";
			}
			File f = new File(dir + File.separator + fn);
			if (!f.exists() && fn.startsWith(dir)) {
				fn = fn.substring(dir.length());
				f = new File(dir + File.separator + fn);
			}
			if (!f.exists()) {
				String fn2 = fn.replaceAll("/", "_");
				f = new File(dir + File.separator + fn2);
				if (!f.exists()) {
					throw new IllegalArgumentException("file " + fn + " does not exist");
				}
				fn = fn2;
			}
			iter.set(fn);
			textFiles.add(f);
		}

		// TODO should only include annotation for the selected files
		File[] list = df.listFiles();
		for (File f : list) {
			Scanner sc = new Scanner(f);
			int count = 0;
			while (count < 3 && sc.hasNextLine()) {
				String line = sc.nextLine();
				count++;
				if (line.contains("<lk-text")) {
					//textFiles.add(f);
					break;
				}
				if (line.contains("<lk-annotation")) {
					String base = null;
					while (sc.hasNextLine()) {
						String line2 = sc.nextLine();
						Matcher m = BASE_PAT.matcher(line2);
						if (m.find()) {
							base = m.group(1);
							if (copy.contains(base)) {
								ArrayList<String> afns = annFileNames.get(base);
								if (afns == null) {
									afns = new ArrayList();
									annFileNames.put(base, afns);
								}
								afns.add(f.getName());
							}
							break;
						}
					}
					break;
				}
			}
			sc.close();
		}
	}

	public boolean hasNext() {
		return (nextFile >= 0 && nextFile < textFiles.size());
		//return false;
	}

	public LKAnnotatedText next() {

		try {

			//System.out.println("getNext: current = " + textFiles.get(nextFile));

			// open input stream to file
			File file = (File) textFiles.get(nextFile);

			//System.out.println("its annotations: " + annFileNames.get(file.getName()));

			LKTextParserCallback tcb = null;

			XMLReader reader = makeXMLReader();
			tcb = new LKTextParserCallback();
			reader.setContentHandler(tcb);

			InputSource is = new InputSource(new FileInputStream(file));

			reader.parse(is);

			String text = tcb.getText();

//			String sourceFile = tcb.getMetaInfo().get("source");
//			System.out.println("source = " + sourceFile);

			ArrayList<LKAnnotationLayer> layers
					= readAnnotations(file.getName(),
					annFileNames.get(file.getName()));

			nextFile++;

			LKAnnotatedText out = new LKAnnotatedText();
			out.rawText = text;
			out.layers = layers;
			out.metaInfo = tcb.metaInfo;
			return out;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	LKAnnotationLayer getLayer(String provides, ArrayList<LKAnnotationLayer> ls) {
		for (LKAnnotationLayer l : ls) {
			if (l.provides.equals(provides)) {
				return l;
			}
		}
		return null;
	} 

	/* SAX stuff. */

	private XMLReader makeXMLReader()
			throws SAXException, ParserConfigurationException {

		javax.xml.parsers.SAXParserFactory saxParserFactory =
				javax.xml.parsers.SAXParserFactory.newInstance();

		final javax.xml.parsers.SAXParser saxParser
				= saxParserFactory.newSAXParser();

		final XMLReader parser = saxParser.getXMLReader();

		return parser;
	}

	private static class LKTextParserCallback extends DefaultHandler
			implements ContentHandler {

		private boolean insideText = false;
		private StringBuilder sb = new StringBuilder();
		private HashMap<String, String> metaInfo = new HashMap();

		private boolean insideTag = false;
		private StringBuilder tag = null;
		private String currentTagName = null;

		public void startElement(String namespace, String localname,
								 String type, Attributes attributes) {
			if (type.equals("tag")) {
				String name = attributes.getValue("name");
				if (name == null) {
					throw new RuntimeException("no name for tag");
				}
				currentTagName = name;
				tag = new StringBuilder();
				insideTag = true;
			}
			else if (type.equals("text")) {
				insideText = true;
				//sb.clear();
			}
			else if (type.matches("lk-text|meta-info")) {
				// do nothing
			}
			else {
				throw new RuntimeException("illegal type: " + type);
			}

		}

		public void endElement(String namespace, String localname,
							   String type) {
			if (type.equals("text")) {
				insideText = false;
			}
			else if (type.equals("tag")) {
				insideTag = false;
				metaInfo.put(currentTagName, tag.toString());
			}
		}

		public void characters(char[] ch, int start, int len) {
			if (insideText) {
				String s = new String(ch, start, len);
				sb.append(s);
			}
			else if (insideTag) {
				String s = new String(ch, start, len);
				tag.append(s);
			}
		}

		String getText() {
			return sb.toString();
		}

		HashMap<String, String> getMetaInfo() {
			return metaInfo;
		}

	}

	private ArrayList<LKAnnotationLayer> readAnnotations(String baseText, ArrayList<String> files) throws IOException {
		if (files == null) {
			throw new RuntimeException("file list = null");
		}

		ListMap<String, LKAnnotationLayer> layerMap = new ListMap();

		ArrayList<String> sorted = sortAnnFiles(files);

		ArrayList<LKAnnotationLayer> out = new ArrayList();

		if (sorted.get(0).equals(baseText)) {
			sorted.remove(0);
		}

		for (String file : sorted) {
			try {
				//System.out.println("Processing file " + file);
				XMLReader reader = makeXMLReader();
				LKAnnotationParserCallback acb
						= new LKAnnotationParserCallback(baseText, layerMap);
				reader.setContentHandler(acb);
				String fullFileName = workDir + File.separatorChar + file;
				InputSource is = new InputSource(fullFileName);
				reader.parse(is);
				layerMap.putAll(file, acb.getLayers());
				out.addAll(acb.getLayers());
			} catch (SAXException e) {
				throw new IOException(e);
			} catch (ParserConfigurationException e) {
				throw new IOException(e);
			}
		}

		return out;
	}

	private static class LKAnnotationParserCallback extends DefaultHandler
			implements ContentHandler {

		private String baseTextFile;

		private ListMap<String, LKAnnotationLayer> layerMap;
		private ArrayList<LKAnnotationLayer> layers = new ArrayList();
		private LKAnnotationLayer current;

		private boolean onTextFile = false;
		//private LKAnnotationLayer currentOn = null;
		private ArrayList<LKAnnotationLayer> currentScope = null;

		private boolean insideTag = false;
		private boolean insideEntity = false;
		private StringBuilder tag = null;
		private String currentTagName = null;

		private HashMap<String, String> metaInfo = new HashMap();

		private LinkedList<DataElementNode> stack;

		LKAnnotationParserCallback(String baseTextFile,
								   ListMap<String, LKAnnotationLayer> layerMap) {
			this.baseTextFile = baseTextFile;
			this.layerMap = layerMap;
		}

		public void startElement(String namespace, String localname,
								 String type, Attributes attributes) {

			if (insideEntity) {
				DataElementNode parent = stack.getLast();
				DataElementNode n = new DataElementNode(type);
				for (int i = attributes.getLength() - 1; i >= 0; i--) {
					String k = attributes.getQName(i);
					String v = attributes.getValue(i);
					n.attributes.put(k, v);
				}
				parent.children.add(n);
				stack.add(n);
			}
			else if (type.equals("e")) {
				LKAnnotationEntity e = new LKAnnotationEntity();

				String on = attributes.getValue("on");
				String start = attributes.getValue("start");
				String end = attributes.getValue("end");
				String from = attributes.getValue("from");
				String to = attributes.getValue("to");

				if (on != null) {
					if (start != null) {
						throw new RuntimeException("on!=null => start=null");
					}
					if (end != null) {
						throw new RuntimeException("on!=null => end=null");
					}
					if (from != null) {
						throw new RuntimeException("on!=null => from=null");
					}
					if (to != null) {
						throw new RuntimeException("on!=null => to=null");
					}
				}

				if (from != null || to != null) {
					if (from == null) {
						throw new RuntimeException("to!=null => from!=null");
					}
					if (to == null) {
						throw new RuntimeException("from!=null => to!=null");
					}
					if (end != null) {
						throw new RuntimeException("from!=null => end=null");
					}
					if (start != null) {
						throw new RuntimeException("from!=null => start=null");
					}
				}
				if (start != null || end != null) {
					if (end == null) {
						throw new RuntimeException("start!=null => end!=null");
					}
					if (start == null) {
						throw new RuntimeException("end!=null => start!=null");
					}
				}

				if (onTextFile && start != null) {
					if (!start.startsWith("#")) {
						throw new RuntimeException("start must begin with #");
					}
					if (!end.startsWith("#")) {
						throw new RuntimeException("start must begin with #");
					}
					e.cstart = Integer.parseInt(start.substring(1));
					e.cend = Integer.parseInt(end.substring(1)) + 1;
				}
				else if (start != null) {
					LKAnnotationLayer[] l1 = new LKAnnotationLayer[1];
					int ix1 = dereferenceId(start, l1);
					LKAnnotationLayer[] l2 = new LKAnnotationLayer[1];
					int ix2 = dereferenceId(end, l2);
					if (l1[0] != l2[0]) {
						throw new RuntimeException("different layers in start-end");
					}
					e.referred = new ArrayList();
					for (int i = ix1; i <= ix2; i++) {
						e.referred.add(l1[0].entityList.get(i));
					}
				}
				else if (on != null) {
					String[] set = on.split("\\,\\s*");
					e.referred = new ArrayList();
					LKAnnotationLayer[] l = new LKAnnotationLayer[1];
					for (String s : set) {
						int ix = dereferenceId(s, l);
						e.referred.add(l[0].entityList.get(ix));
					}
				}
				else if (from != null) {
					LKAnnotationLayer[] l = new LKAnnotationLayer[1];
					int ix1 = dereferenceId(from, l);
					e.from = l[0].entityList.get(ix1);
					int ix2 = dereferenceId(to, l);
					e.to = l[0].entityList.get(ix2);
				}

				String id = attributes.getValue("id");
				if (id == null) {
					throw new RuntimeException("no id");
				}
				if (current.idToIndex.containsKey(id)) {
					throw new RuntimeException("id must be unique");
				}

				e.localURI = id;

				//e.label = attributes.getValue("l");

				current.idToIndex.put(id, current.entityList.size());
				current.entityList.add(e);

				insideEntity = true;
				stack = new LinkedList();
				DataElementNode n = new DataElementNode("__ROOT__");
				stack.add(n);
				e.data = n;

			}
			else if (type.equals("tag")) {
				String name = attributes.getValue("name");
				if (name == null) {
					throw new RuntimeException("no name for tag");
				}
				currentTagName = name;
				tag = new StringBuilder();
				insideTag = true;
			}
			else if (type.equals("annotation")) {
				current = new LKAnnotationLayer();
				String scopeFile = attributes.getValue("scope");
				String onFiles = attributes.getValue("on-files");
				if (onFiles != null) {
					throw new RuntimeException("on-files is unimplemented: currently, we can only handle annotation layers with scope");
				}

				if (scopeFile != null && !scopeFile.equals("")) {
					if (scopeFile.contains("lktext")) {
						currentScope = null;
					}
					else {
						currentScope = layerMap.get(scopeFile);
						if (currentScope == null) {
							throw new RuntimeException("scope not found: |" + scopeFile + "|");
						}
					}
				}
				else {
					currentScope = layers;
				}


				onTextFile = scopeFile != null && scopeFile.equals(baseTextFile);
				current.scopeFile = scopeFile;
				current.provides = attributes.getValue("provides");
				layers.add(current);
			}
			else if (type.matches("lk-annotation|meta-info")) {
				// do nothing
			}
			else {
				throw new RuntimeException("illegal type: " + type);
			}
		}

		private int dereferenceId(String ref, LKAnnotationLayer[] lout) {
			if (ref == null) {
				throw new IllegalArgumentException("null reference");
			}
			ref = ref.trim();
			int ix = ref.indexOf('#');
			LKAnnotationLayer l = null;
			String fileRef = null;
			String idRef = null;
			if (ix == -1) {
				//idRef = ref;
				throw new RuntimeException("No fragment identifier");
			}
			else {
				fileRef = ref.substring(0, ix);
				idRef = ref.substring(ix + 1);
			}

			ArrayList<LKAnnotationLayer> scope;
			if (fileRef == null || fileRef.equals("")) {
				scope = currentScope;
			}
			//else if(fileRef.equals("$"))
			//	scope = layers;
			else {
				//throw new RuntimeException("sorry, this type of file reference is still unimplemented");
				scope = layerMap.get(fileRef);
				if (scope == null) {
					throw new RuntimeException("scope not found: " + fileRef);
				}
			}
			for (LKAnnotationLayer ll : scope) {
				Integer llIx = ll.idToIndex.get(idRef);
				if (llIx != null) {
					lout[0] = ll;
					return llIx;
				}
			}
			throw new RuntimeException("entity " + idRef + " not found");
		}

		public void endElement(String namespace, String localname,
							   String type) {

			if (insideEntity) {
				if (stack.size() == 1) {
					insideEntity = false;
					// 
				}
				else {
					stack.removeLast();
				}
			}
			else if (type.equals("tag")) {
				insideTag = false;
				metaInfo.put(currentTagName, tag.toString());
			}
			else if (type.equals("annotation")) {
				current = null;
			}
		}

		public void characters(char[] ch, int start, int len) {
			if (insideEntity) {
				String s = new String(ch, start, len);
				DataTextNode n = new DataTextNode(s);
				stack.getLast().children.add(n);
			}
			else if (insideTag) {
				String s = new String(ch, start, len);
				tag.append(s);
			}
		}

		ArrayList<LKAnnotationLayer> getLayers() {
			return layers;
		}

		HashMap<String, String> getMetaInfo() {
			return metaInfo;
		}

	}

	private ArrayList<String> sortAnnFiles(ArrayList<String> files) throws IOException {
		//System.out.println("Before sorting: files = " + files);
		ListMap<String, String> dg = createAnnDepGraph(files);
		//System.out.println("dg = " + dg);
		ArrayList<String> out = tsort(dg);
		//System.out.println("After sorting: files = " + out);
		return out;
	}

	private <T> ArrayList<T> tsort(ListMap<T, T> depGraph) {

		HashSet<T> starts = new HashSet<T>();
		for (T k : depGraph.keySet()) {
			starts.add(k);
		}

		for (T k : depGraph.keySet()) {
			starts.removeAll(depGraph.get(k));
		}

		if (starts.size() == 0) {
			throw new RuntimeException("cyclic or empty graph!");
		}

		LinkedList<T> q = new LinkedList(starts);
		ArrayList<T> out = new ArrayList();

		while (!q.isEmpty()) {
			T t = q.removeFirst();
			out.add(t);
			ArrayList<T> sl = depGraph.get(t);
			if (sl != null) {
				starts.clear();
				starts.addAll(sl);
				for (T s : sl) {
					ArrayList<T> sl2 = depGraph.get(s);
					if (sl2 != null) {
						starts.removeAll(sl2);
					}
				}
				q.addAll(starts);
			}
		}
		HashSet<T> seen = new HashSet();
		for (Iterator<T> it = out.iterator(); it.hasNext(); ) {
			T t = it.next();
			if (seen.contains(t)) {
				it.remove();
			}
			else {
				seen.add(t);
			}
		}
		return out;
	}

	private <T> ArrayList<T> tsort_orig(ListMap<T, T> depGraph) {
		HashSet<T> starts = new HashSet<T>();
		for (T k : depGraph.keySet()) {
			starts.add(k);
		}

		for (T k : depGraph.keySet()) {
			starts.removeAll(depGraph.get(k));
		}

		if (starts.size() == 0) {
			throw new RuntimeException("cyclic or empty graph!");
		}

		LinkedList<T> q = new LinkedList(starts);
		ArrayList<T> out = new ArrayList();

		while (!q.isEmpty()) {
			T t = q.removeFirst();
			out.add(t);
			ArrayList<T> sl = depGraph.get(t);

			if (sl != null) {
				q.addAll(sl);
			}
		}
		HashSet<T> seen = new HashSet();
		for (Iterator<T> it = out.iterator(); it.hasNext(); ) {
			T t = it.next();
			if (seen.contains(t)) {
				it.remove();
			}
			else {
				seen.add(t);
			}
		}

		return out;
	}

	private ListMap<String, String> createAnnDepGraph(ArrayList<String> files) throws IOException {
		ListMap<String, String> out = new ListMap();
		for (String fn : files) {
			String full = workDir + File.separatorChar + fn;
			BufferedReader br = new BufferedReader(new FileReader(full));
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.startsWith("<e")) {
					Matcher m1 = ON_FILE_PAT.matcher(line);
					if (m1.find()) {
						String ref = m1.group(1);
						out.put(ref, fn);
					}
					else {
						Matcher m2 = ON_FILES_PAT.matcher(line);
						if (m2.find()) {
							throw new RuntimeException("on-files is unimplemented...");
						}
					}
				}

				line = br.readLine();
			}
			br.close();
		}
		return out;
	}

	public static void main(String[] argv) {
		try {
			LKCollectionReader r = new LKCollectionReader(argv[0]);
			int i = 0;
			while (r.hasNext()) {
				i++;
				LKAnnotatedText annotatedText = r.next();
				LKAnnotationLayer layer = annotatedText.getLayer("MPQA-expressive-subjectivity");
				System.out.println(layer.onFiles);
				System.out.println(layer.scopeFile);
				for (LKAnnotationEntity entity : layer.entityList) {
					System.out.println(entity);
				}
				break;
//				System.out.println(r);
				//if(i == 50)
				//	break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}