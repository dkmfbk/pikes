package eu.fbk.dkm.pikes.resources;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class PredicateMatrix {

	private HashMap<String, ArrayList<String>> vnClass = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> vnSubClass = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> fnFrame = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> pbPredicate = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> esoClass = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> eventType = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> wnSense = new HashMap<String, ArrayList<String>>();

	private HashMap<String, ArrayList<String>> vnToFn = new HashMap<String, ArrayList<String>>();

	private HashMap<String, ArrayList<String>> vnThematicRole = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> fnFrameElement = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> pbArgument = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> esoRole = new HashMap<String, ArrayList<String>>();

	public PredicateMatrix(String modelFile) {
		try {
			BufferedReader pmReader = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile), Charset.forName("UTF-8")));

			String pmLine;
			String[] pmFields;

			while ((pmLine = pmReader.readLine()) != null) {
				pmFields = pmLine.split("\t");

//				System.out.println(Arrays.toString(pmFields));

				if (!pmFields[2].equals("NULL")) {
					if (!pmFields[4].equals("NULL")) {
						ArrayList<String> array = new ArrayList<String>();
						if (vnClass.containsKey(pmFields[2])) {
							array = vnClass.get(pmFields[2]);
						}
						if (newElement(array, pmFields[4])) {
							array.add(pmFields[4]);
						}
						vnClass.put(pmFields[2], array);
					}
					if (!pmFields[6].equals("NULL")) {
						ArrayList<String> array = new ArrayList<String>();
						if (vnSubClass.containsKey(pmFields[2])) {
							array = vnSubClass.get(pmFields[2]);
						}
						if (newElement(array, pmFields[6])) {
							array.add(pmFields[6]);
						}
						vnSubClass.put(pmFields[2], array);

						if (!vnToFn.containsKey(pmFields[6])) {
							vnToFn.put(pmFields[6], new ArrayList<>());
						}
						if (!vnToFn.get(pmFields[6]).contains(pmFields[12]) && !pmFields[12].equals("NULL")) {
							vnToFn.get(pmFields[6]).add(pmFields[12]);
//							System.out.println(pmFields[6]);
//							System.out.println(pmFields[12]);
//							System.out.println();
						}
					}
					if (!pmFields[12].equals("NULL")) {
						ArrayList<String> array = new ArrayList<String>();
						if (fnFrame.containsKey(pmFields[2])) {
							array = fnFrame.get(pmFields[2]);
						}
						if (newElement(array, pmFields[12])) {
							array.add(pmFields[12]);
						}
						fnFrame.put(pmFields[2], array);
					}
					if (!pmFields[15].equals("NULL")) {
						ArrayList<String> array = new ArrayList<String>();
						if (pbPredicate.containsKey(pmFields[2])) {
							array = pbPredicate.get(pmFields[2]);
						}
						if (newElement(array, pmFields[15])) {
							array.add(pmFields[15]);
						}
						pbPredicate.put(pmFields[2], array);
					}
					if (!pmFields[25].equals("NULL")) {
						ArrayList<String> array = new ArrayList<String>();
						if (esoClass.containsKey(pmFields[2])) {
							array = esoClass.get(pmFields[2]);
						}
						if (newElement(array, pmFields[25])) {
							array.add(pmFields[25]);
						}
						esoClass.put(pmFields[2], array);
					}
					if (!pmFields[27].equals("NULL")) {
						ArrayList<String> array = new ArrayList<String>();
						if (eventType.containsKey(pmFields[2])) {
							array = eventType.get(pmFields[2]);
						}
						if (newElement(array, pmFields[27])) {
							array.add(pmFields[27]);
						}
						eventType.put(pmFields[2], array);
					}
					if (!pmFields[11].equals("NULL")) {
						ArrayList<String> array = new ArrayList<String>();
						if (wnSense.containsKey(pmFields[2])) {
							array = wnSense.get(pmFields[2]);
						}
						if (newElement(array, pmFields[11])) {
							array.add(pmFields[11]);
						}
						wnSense.put(pmFields[2], array);
					}
					if (!pmFields[3].equals("NULL")) {
						if (!pmFields[9].equals("NULL")) {
							ArrayList<String> array = new ArrayList<String>();
							if (vnThematicRole.containsKey(pmFields[2] + ":"
									+ pmFields[3])) {
								array = vnThematicRole.get(pmFields[2] + ":"
										+ pmFields[3]);
							}
							if (newElement(array, pmFields[4] + "@"
									+ pmFields[9])) {
								array.add(pmFields[4] + "@" + pmFields[9]);
							}
							vnThematicRole.put(pmFields[2] + ":"
									+ pmFields[3], array);
						}
						if (!pmFields[14].equals("NULL")) {
							ArrayList<String> array = new ArrayList<String>();
							if (fnFrameElement.containsKey(pmFields[2] + ":"
									+ pmFields[3])) {
								array = fnFrameElement.get(pmFields[2] + ":"
										+ pmFields[3]);
							}
							if (newElement(array, pmFields[12] + "@"
									+ pmFields[14])) {
								array.add(pmFields[12] + "@" + pmFields[14]);
							}
							fnFrameElement.put(pmFields[2] + ":"
									+ pmFields[3], array);
						}
						if (!pmFields[16].equals("NULL")) {
							ArrayList<String> array = new ArrayList<String>();
							if (pbArgument.containsKey(pmFields[2] + ":"
									+ pmFields[3])) {
								array = pbArgument.get(pmFields[2] + ":"
										+ pmFields[3]);
							}
							if (newElement(array, pmFields[15] + "@"
									+ pmFields[16])) {
								array.add(pmFields[15] + "@" + pmFields[16]);
							}
							pbArgument.put(pmFields[2] + ":"
									+ pmFields[3], array);
						}
						if (!pmFields[26].equals("NULL")) {
							ArrayList<String> array = new ArrayList<String>();
							if (esoRole.containsKey(pmFields[2] + ":"
									+ pmFields[3])) {
								array = esoRole.get(pmFields[2] + ":"
										+ pmFields[3]);
							}
							if (newElement(array, pmFields[25] + "@"
									+ pmFields[26])) {
								array.add(pmFields[25] + "@" + pmFields[26]);
							}
							esoRole.put(pmFields[2] + ":"
									+ pmFields[3], array);
						}
					}
				}
			}

			pmReader.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> getVNClasses(String PBSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (vnClass.containsKey(PBSense)) {
			array = vnClass.get(PBSense);
		}
		return array;
	}

	public ArrayList<String> getVNClassesToFN(String vnSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (vnToFn.containsKey(vnSense)) {
			array = vnToFn.get(vnSense);
		}
		return array;
	}

	public ArrayList<String> getVNSubClasses(String PBSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (vnSubClass.containsKey(PBSense)) {
			array = vnSubClass.get(PBSense);
		}
		return array;
	}

	public ArrayList<String> getFNFrames(String PBSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (fnFrame.containsKey(PBSense)) {
			array = fnFrame.get(PBSense);
		}
		return array;
	}

	public ArrayList<String> getPBPredicates(String PBSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (pbPredicate.containsKey(PBSense)) {
			array = pbPredicate.get(PBSense);
		}
		return array;
	}

	public ArrayList<String> getESOClasses(String PBSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (esoClass.containsKey(PBSense)) {
			array = esoClass.get(PBSense);
		}
		return array;
	}

	public ArrayList<String> getEventTypes(String PBSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (eventType.containsKey(PBSense)) {
			array = eventType.get(PBSense);
		}
		return array;
	}

	public ArrayList<String> getWNSenses(String PBSense) {
		ArrayList<String> array = new ArrayList<String>();
		if (wnSense.containsKey(PBSense)) {
			array = wnSense.get(PBSense);
		}
		return array;
	}

	public ArrayList<String> getVNThematicRoles(String PBSenseArgument) {
		ArrayList<String> array = new ArrayList<String>();
		if (vnThematicRole.containsKey(PBSenseArgument)) {
			array = vnThematicRole.get(PBSenseArgument);
		}
		return array;
	}

	public ArrayList<String> getFNFrameElements(String PBSenseArgument) {
		ArrayList<String> array = new ArrayList<String>();
		if (fnFrameElement.containsKey(PBSenseArgument)) {
			array = fnFrameElement.get(PBSenseArgument);
		}
		return array;
	}

	public ArrayList<String> getPBArguments(String PBSenseArgument) {
		ArrayList<String> array = new ArrayList<String>();
		if (pbArgument.containsKey(PBSenseArgument)) {
			array = pbArgument.get(PBSenseArgument);
		}
		return array;
	}

	public ArrayList<String> getESORoles(String PBSenseArgument) {
		ArrayList<String> array = new ArrayList<String>();
		if (esoRole.containsKey(PBSenseArgument)) {
			array = esoRole.get(PBSenseArgument);
		}
		return array;
	}

	private boolean newElement(ArrayList<String> array, String element) {
		boolean isNew = true;
		for (int i = 0; i < array.size(); i++) {
			if (array.get(i).toString().equals(element)) {
				isNew = false;
			}
		}
		return isNew;
	}
}
