package eu.fbk.dkm.pikes.resources;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class PredicateMatrix {


	final private int ID_LANG = 0;
	final private int ID_POS = 1;
	final private int ID_PRED = 2;
	final private int ID_ROLE = 3;
	final private int VN_CLASS = 4;
	final private int VN_CLASS_NUMBER = 5;
	final private int VN_SUBCLASS = 6;
	final private int VN_SUBCLASS_NUMBER = 7;
	final private int VN_LEMA = 8;
	final private int VN_ROLE = 9;
	final private int WN_SENSE = 10;
	final private int MCR_iliOffset = 11;
	final private int FN_FRAME = 12;
	final private int FN_LE = 13;
	final private int FN_FRAME_ELEMENT = 14;
	final private int PB_ROLESET = 15;
	final private int PB_ARG = 16;
	final private int MCR_BC = 17;
	final private int MCR_DOMAIN = 18;
	final private int MCR_SUMO = 19;
	final private int MCR_TO = 20;
	final private int MCR_LEXNAME = 21;
	final private int MCR_BLC = 22;
	final private int WN_SENSEFREC = 23;
	final private int WN_SYNSET_REL_NUM = 24;
	final private int ESO_CLASS = 25;
	final private int ESO_ROLE = 26;
	final private int NWREVENT = 27;


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

//				for (int i=0;i<pmFields.length;i++)
				
				
				
//				System.out.println(Arrays.toString(pmFields));
				//skip header
				if (!pmFields[ID_LANG].equals("1_ID_LANG")) {
					//consider only those line for english verbs
					if ((pmFields[ID_LANG].equals("id:eng"))&&(pmFields[ID_POS].equals("id:v"))) {

						//the entry key for us is the propbank roleset

						//remove namespaces
						for (int i=2;i<pmFields.length;i++)
							pmFields[i]=removePredicateMatrixNamespace(pmFields[i]);

						

						if (!pmFields[PB_ROLESET].equals("NULL")) {


							//VN class

							String pb_roleset = pmFields[PB_ROLESET];

							if ((!pmFields[VN_CLASS].equals("NULL"))&&(!pmFields[VN_LEMA].equals("NULL"))) {

								String vnFull =  pmFields[VN_LEMA]+ "-" +pmFields[VN_CLASS];
								ArrayList<String> array = new ArrayList<String>();
								if (vnClass.containsKey(pb_roleset)) {
									array = vnClass.get(pb_roleset);
								}
								if (newElement(array, vnFull)) {
									array.add(vnFull);
								}
								vnClass.put(pb_roleset, array);
							}

							//VN subclass
							if ((!pmFields[VN_SUBCLASS].equals("NULL"))&&(!pmFields[VN_LEMA].equals("NULL"))) {

								String vnFull =  pmFields[VN_LEMA]+ "-" +pmFields[VN_SUBCLASS];
								ArrayList<String> array = new ArrayList<String>();
								if (vnSubClass.containsKey(pb_roleset)) {
									array = vnSubClass.get(pb_roleset);
								}
								if (newElement(array, vnFull)) {
									array.add(vnFull);
								}
								vnSubClass.put(pb_roleset, array);

								//vn2fn mappings only here because at the level of subclass????
								if (!vnToFn.containsKey(vnFull)) {
									vnToFn.put(vnFull, new ArrayList<>());
								}
								if (!vnToFn.get(vnFull).contains(pmFields[FN_FRAME]) && !pmFields[FN_FRAME].equals("NULL")) {
									vnToFn.get(vnFull).add(pmFields[FN_FRAME]);
									//							System.out.println(pmFields[6]);
									//							System.out.println(pmFields[12]);
									//							System.out.println();
								}
							}

							//frameNet frames
							if (!pmFields[FN_FRAME].equals("NULL")) {
								ArrayList<String> array = new ArrayList<String>();
								if (fnFrame.containsKey(pb_roleset)) {
									array = fnFrame.get(pb_roleset);
								}
								if (newElement(array, pmFields[FN_FRAME])) {
									array.add(pmFields[FN_FRAME]);
								}
								fnFrame.put(pb_roleset, array);
							}


							//pb next block is a hack, may not be needed...
							if (!pb_roleset.equals("NULL")) {
								ArrayList<String> array = new ArrayList<String>();
								if (pbPredicate.containsKey(pb_roleset)) {
									array = pbPredicate.get(pb_roleset);
								}
								if (newElement(array, pb_roleset)) {
									array.add(pb_roleset);
								}
								pbPredicate.put(pb_roleset, array);
							}

							//ESO
							if (!pmFields[ESO_CLASS].equals("NULL")) {
								ArrayList<String> array = new ArrayList<String>();
								if (esoClass.containsKey(pb_roleset)) {
									array = esoClass.get(pb_roleset);
								}
								if (newElement(array, pmFields[ESO_CLASS])) {
									array.add(pmFields[ESO_CLASS]);
								}
								esoClass.put(pb_roleset, array);
							}


//							not any more in PM 1.3
//							if (!pmFields[NWREVENT].equals("NULL")) {
//								ArrayList<String> array = new ArrayList<String>();
//								if (eventType.containsKey(pmFields[2])) {
//									array = eventType.get(pmFields[2]);
//								}
//								if (newElement(array, pmFields[27])) {
//									array.add(pmFields[27]);
//								}
//								eventType.put(pmFields[2], array);
//							}

							if (!pmFields[MCR_iliOffset].equals("NULL")) {
								ArrayList<String> array = new ArrayList<String>();
								if (wnSense.containsKey(pb_roleset)) {
									array = wnSense.get(pb_roleset);
								}
								if (newElement(array, pmFields[MCR_iliOffset])) {
									array.add(pmFields[MCR_iliOffset]);
								}
								wnSense.put(pb_roleset, array);
							}


							//ROLES
							//PB_ARG has to be not null
							if (!pmFields[PB_ARG].equals("NULL")) {

								String pb_arg = fixPbArg(pmFields[PB_ARG]);




								//wn thematic role
								if (!pmFields[VN_ROLE].equals("NULL")) {
									ArrayList<String> array = new ArrayList<String>();
									if (vnThematicRole.containsKey(pb_roleset + "@"
											+ pb_arg)) {
										array = vnThematicRole.get(pb_roleset + "@"
												+ pb_arg);
									}

									//add both to VN_CLASS and VN_SUBCLASS

									if ((!pmFields[VN_CLASS].equals("NULL"))&&(!pmFields[VN_LEMA].equals("NULL"))) {
										String vnFull =  pmFields[VN_LEMA]+ "-" +pmFields[VN_CLASS];
										if (newElement(array, vnFull + "@"
												+ pmFields[VN_ROLE])) {
											array.add(vnFull + "@" + pmFields[VN_ROLE]);
										}
									}

									if ((!pmFields[VN_SUBCLASS].equals("NULL"))&&(!pmFields[VN_LEMA].equals("NULL"))) {
										String vnFull =  pmFields[VN_LEMA]+ "-" +pmFields[VN_SUBCLASS];
										if (newElement(array, vnFull + "@"
												+ pmFields[VN_ROLE])) {
											array.add(vnFull + "@" + pmFields[VN_ROLE]);
										}
									}

									vnThematicRole.put(pb_roleset + "@"
											+ pb_arg, array);
								}

								//fn fe
								if (!pmFields[FN_FRAME_ELEMENT].equals("NULL")) {
									ArrayList<String> array = new ArrayList<String>();
									if (fnFrameElement.containsKey(pb_roleset + "@"
											+ pb_arg)) {
										array = fnFrameElement.get(pb_roleset + "@"
												+ pb_arg);
									}

									if (!pmFields[FN_FRAME].equals("NULL")) {

										if (newElement(array, pmFields[FN_FRAME] + "@"
												+ pmFields[FN_FRAME_ELEMENT])) {
											array.add(pmFields[FN_FRAME] + "@"
													+ pmFields[FN_FRAME_ELEMENT]);
										}

									}

									fnFrameElement.put(pb_roleset + "@"
											+ pb_arg, array);
								}





								if (!pb_arg.equals("NULL")) {
									ArrayList<String> array = new ArrayList<String>();
									if (pbArgument.containsKey(pb_roleset + "@"
											+ pb_arg)) {
										array = pbArgument.get(pb_roleset + "@"
												+ pb_arg);
									}
									if (newElement(array, pb_roleset + "@"
											+ pb_arg)) {
										array.add(pb_roleset + "@"
												+ pb_arg);
									}
									pbArgument.put(pb_roleset + "@"
											+ pb_arg, array);
								}

								//eso
								if (!pmFields[ESO_ROLE].equals("NULL")) {
									ArrayList<String> array = new ArrayList<String>();
									if (esoRole.containsKey(pb_roleset + "@"
											+ pb_arg)) {
										array = esoRole.get(pb_roleset + "@"
												+ pb_arg);
									}
									if (!pmFields[ESO_CLASS].equals("NULL")) {
										if (newElement(array, pmFields[ESO_CLASS] + "@" + pmFields[ESO_ROLE])) {
											array.add(pmFields[ESO_CLASS] + "@" + pmFields[ESO_ROLE]);
										}
									}
									esoRole.put(pb_roleset + "@"
											+ pb_arg, array);
								}
							}
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


	public static String removePredicateMatrixNamespace(String entry) {

		if (!entry.equals("NULL")) {
			int index = entry.indexOf( ':' );
			entry=entry.substring(index+1);
		}
		return entry;

	}

	public static String fixPbArg(String entry) {


		entry=entry.replace("C-","").replace("R-","");
		if (entry.equals("DV")) entry="ADV";
		if (entry.length()==1) entry="A"+entry;  //0,1,...,5 cases
		if (entry.length()==3) entry="AM-"+entry;  //DIS,TMP,MNR,... cases
		return entry;

	}

}
