package eu.fbk.dkm.pikes.resources.darmstadt;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 10/04/15.
 */

public class CorpusAnnotator {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusAnnotator.class);
	private static Pattern spanPattern = Pattern.compile("word_([0-9]+)");
	private static String TERM_PREFIX = "t";

	private static void getFilesRecursive(File pFile, HashSet<String> folders) {
		for (File file : pFile.listFiles()) {
			if (file.isDirectory()) {
				folders.add(file.getAbsolutePath());
				getFilesRecursive(file, folders);
			}
		}
	}

	private static Integer getTermFromSpan(String span) {
		Matcher matcher = spanPattern.matcher(span);
		if (matcher.find()) {
			Integer id = Integer.parseInt(matcher.group(1));
			return id - 1;
		}

		return null;
	}

	private static Span<Term> getTermsFromSpan(KAFDocument document, String span) {
		String[] parts = span.split("[^a-z0-9A-Z_]+");
		Span<Term> termSpan = KAFDocument.newTermSpan();

		if (parts.length == 1) {
			Integer id = getTermFromSpan(parts[0]);
			termSpan.addTarget(document.getTerms().get(id));
		}
		else if (parts.length > 1) {
			Integer id1 = getTermFromSpan(parts[0]);
			Integer id2 = getTermFromSpan(parts[parts.length - 1]);
			for (int i = id1; i <= id2; i++) {
				termSpan.addTarget(document.getTerms().get(i));
			}
		}

		return termSpan;
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.darmstadt-loader")
					.withHeader("Load eu.fbk.dkm.pikes.resources.darmstadt-service-review-corpus")
					.withOption("i", "input-folder", "the folder of the corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("f", "force", "Force opinion")
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputFile = cmd.getOptionValue("i", File.class);
			boolean forceOpinion = cmd.hasOption("f");

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setValidating(false);
			dbFactory.setNamespaceAware(true);
			dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
			dbFactory.setFeature("http://xml.org/sax/features/validation", false);
			dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			DocumentBuilder dBuilder;
			Document doc;

			HashSet<String> folders = new HashSet<>();
			getFilesRecursive(inputFile, folders);

			HashSet<String> okFolders = new HashSet<>();
			okLoop:
			for (String folder : folders) {
				for (String pattern : CorpusLoader.MMAX_PATTERN) {
					StringBuffer newFolder = new StringBuffer();
					newFolder.append(folder);
					newFolder.append(File.separator);
					newFolder.append(pattern);

					if (!folders.contains(newFolder.toString())) {
						continue okLoop;
					}
				}

				okFolders.add(folder);
			}

			for (String folder : okFolders) {
				LOGGER.info("Entering folder {}", folder);

				String markableDir = folder + File.separator + CorpusLoader.MMAX_PATTERN[1];
				String basedataDir = folder + File.separator + CorpusLoader.MMAX_PATTERN[0];
				File nafDir = new File(folder + File.separator + "naf-parsed");

				Iterator<File> fileIterator;
				fileIterator = FileUtils.iterateFiles(nafDir, new String[]{"naf"}, false);
				while (fileIterator.hasNext()) {
					File file = fileIterator.next();
					String fileBaseName = FilenameUtils.removeExtension(file.getName());
					LOGGER.info(fileBaseName);

					File annotatedFile = new File(markableDir + File.separator + fileBaseName + CorpusLoader.MMAX_SUFFIXES[1] + ".xml");
					if (!annotatedFile.exists()) {
						LOGGER.warn("File {} does not exist", annotatedFile.getAbsolutePath());
						continue;
					}

					File basedataFile = new File(basedataDir + File.separator + fileBaseName + CorpusLoader.MMAX_SUFFIXES[0] + ".xml");
					if (!basedataFile.exists()) {
						LOGGER.warn("File {} does not exist", basedataFile.getAbsolutePath());
						continue;
					}

					KAFDocument document = KAFDocument.createFromFile(file);

					boolean hasGoldOpinions = false;
					for (Opinion opinion : document.getOpinions()) {
					    if ("gold-eu.fbk.dkm.pikes.resources.darmstadt".equals(opinion.getLabel())) {
					        hasGoldOpinions = true;
					        break;
					    }
					}
					
					if (hasGoldOpinions && !forceOpinion) {
					    LOGGER.info("Opinions already present, skipping...");
					
					} else {
					    String fileContent;
    					fileContent = Files.toString(basedataFile, Charsets.UTF_8);
    					fileContent = fileContent.replaceAll("&", "&amp;");
    					dBuilder = dbFactory.newDocumentBuilder();
    					doc = dBuilder.parse(new ByteArrayInputStream(fileContent.getBytes(Charsets.UTF_8)));
    
    					int origWordCount = doc.getElementsByTagName("word").getLength();
    					int nafWordCount = document.getWFs().size();
    
    					if (origWordCount != nafWordCount) {
    						LOGGER.warn("Word counts differ ({}/{})", origWordCount, nafWordCount);
    					}
    
    					HashMap<String, HashMap<String, String>> markables = new HashMap<>();
    
    					fileContent = Files.toString(annotatedFile, Charsets.UTF_8);
    					dBuilder = dbFactory.newDocumentBuilder();
    					doc = dBuilder.parse(new ByteArrayInputStream(fileContent.getBytes(Charsets.UTF_8)));
    					NodeList nList = doc.getElementsByTagName("markable");
    					for (int temp = 0; temp < nList.getLength(); temp++) {
    						Element nNode = (Element) nList.item(temp);
    						NamedNodeMap attributes = nNode.getAttributes();
    						if (attributes != null) {
    							HashMap<String, String> thisMarkable = new HashMap<>();
    							for (int i = 0; i < attributes.getLength(); i++) {
    								thisMarkable.put(attributes.item(i).getNodeName(), attributes.item(i).getNodeValue());
    							}
    
    							if (thisMarkable.get("id") != null) {
    								markables.put(thisMarkable.get("id"), thisMarkable);
    							}
    						}
    					}
    
    					for (HashMap<String, String> markable : markables.values()) {
    						if (markable.get("annotation_type").equals("opinionexpression")) {
    
    							String holderString = markable.get("opinionholder");
    							String targetString = markable.get("opiniontarget");
    
    							HashMap<String, String> holder = null;
    							HashMap<String, String> target = null;
    
    							if (holderString != null && !holderString.equals("empty")) {
    								holder = markables.get(holderString);
    							}
    							if (targetString != null && !targetString.equals("empty")) {
    								target = markables.get(targetString);
    							}
    
    							Span<Term> termSpan;
    
    							try {
    								termSpan = getTermsFromSpan(document, markable.get("span"));
    							} catch (Exception e) {
    								continue;
    							}
    
    							Opinion opinion = document.createOpinion();
    							opinion.setLabel("gold-eu.fbk.dkm.pikes.resources.darmstadt");
    							Opinion.OpinionExpression expression = opinion.createOpinionExpression(termSpan);
    							if (markable.get("polarity") != null) {
    								expression.setPolarity(markable.get("polarity"));
    							}
    							if (markable.get("strength") != null) {
    								expression.setStrength(markable.get("strength"));
    							}
    
    							if (holder != null) {
    								Span<Term> terms = getTermsFromSpan(document, holder.get("span"));
    								opinion.createOpinionHolder(terms);
    							}
    							if (target != null) {
    								Span<Term> terms = getTermsFromSpan(document, target.get("span"));
    								opinion.createOpinionTarget(terms);
    							}
    						}
    					}
					}
					
                    NAFFilter.builder(false).withSRLRoleLinking(true, true)
                            .withOpinionLinking(true, true).build().filter(document);
					
					document.save(file.getAbsolutePath());
				}
			}

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}

}
