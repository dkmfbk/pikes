package eu.fbk.dkm.pikes.resources.mpqa;

import com.google.common.collect.HashMultimap;
import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.pikes.resources.reader.*;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by alessio on 20/03/15.
 */

public class JohanssonAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(JohanssonAnnotator.class);
	public static final String DEFAULT_NAF_PARSED_DIR = "NAF-parsed";

	static Pattern keyValuePatt = Pattern.compile("^([^=]+)=(.*)$");
	static Pattern spanPatt = Pattern.compile("^([^,]*),([^,]*)$");
	public static List<String> DEFAULT_NAF_EXTENSIONS = new ArrayList<>();

	public static String GOLD_LABEL = "gold-eu.fbk.dkm.pikes.resources.mpqa";

	static {
		DEFAULT_NAF_EXTENSIONS.add("xml");
		DEFAULT_NAF_EXTENSIONS.add("naf");
	}

	private static Span<Term> getSpanFromEntity(LKAnnotationEntity entity, KAFDocument document) {

		Span<Term> returnSpan = KAFDocument.newTermSpan();

		if (entity.referred != null) {
			for (LKAnnotationEntity referredEntity : entity.referred) {
				Integer termID = Integer.parseInt(referredEntity.localURI);
				Term term = document.getTerms().get(termID - 1);
				returnSpan.addTarget(term);
			}
		}

		return returnSpan;
	}

	@Nullable
	private static Integer sentenceForSpan(Span<Term> termSpan) {
		HashSet<Integer> sentences = new HashSet<>();
		for (Term term : termSpan.getTargets()) {
			sentences.add(term.getSent());
		}

		if (sentences.size() != 1) {
			return null;
		}

		for (Integer sentence : sentences) {
			return sentence;
		}

		return null;
	}

	public static void main(String[] args) {
		CommandLine cmd = null;
		try {
			cmd = CommandLine
					.parser()
					.withName("eu.fbk.dkm.pikes.resources.mpqa-annotator")
					.withHeader("Annotated files with MPQA annotations")
					.withOption("i", "input-path", "the J-M dataset input dir", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("o", "output-path", "the NAF dir", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
//					.withOption("e", "extensions", String.format("Input extensions (default %s)", DEFAULT_NAF_EXTENSIONS), "EXTS", CommandLine.Type.STRING, true, true, false)
//					.withOption("t", "test", "test only on this file", "FILE", CommandLine.Type.STRING, true, false, false)
//					.withOption("f", "force", "Force opinion")
//					.withOption("F", "fake", "Fake mode, do not write to files")
//					.withOption("s", "exclude-source-local-null", "Exclude opinion if source is null")
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File lkFolder = cmd.getOptionValue("i", File.class);
			File outputFolder = cmd.getOptionValue("o", File.class);

			LKCollectionReader r = new LKCollectionReader(lkFolder.getAbsolutePath());
			while (r.hasNext()) {
				LKAnnotatedText annotatedText = r.next();
				LKAnnotationLayer agentsLayer = annotatedText.getLayer("MPQA-agents");
				String fileName = agentsLayer.scopeFile;
				File nafFile = new File(outputFolder.getAbsolutePath() + File.separator + fileName);

				if (!nafFile.exists()) {
					LOGGER.error("File {} does not exist", nafFile.getCanonicalPath());
					continue;
				}

				LOGGER.debug("Loading file {}", nafFile.getAbsolutePath());
				KAFDocument document = KAFDocument.createFromFile(nafFile);

				HashMap<String, String> hiddenAgents = new HashMap<>();

				HashMultimap<String, Span<Term>> agents = HashMultimap.create();
				for (LKAnnotationEntity entity : agentsLayer.entityList) {
					DataElementNode expressionNode = (DataElementNode) entity.data.children.get(0);

					String implicit = expressionNode.attributes.get("imp");
					if (implicit != null && implicit.equals("true")) {
						hiddenAgents.put(entity.localURI, "implicit");
						continue;
					}

					String writer = expressionNode.attributes.get("w");
					if (writer != null && writer.equals("true")) {
						hiddenAgents.put(entity.localURI, "writer");
						continue;
					}

					Span<Term> agentSpan = getSpanFromEntity(entity, document);

					if (agentSpan.size() == 0) {
						LOGGER.debug("Agent span is empty [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					agents.put(entity.localURI, agentSpan);

					String ns = expressionNode.attributes.get("ns");
					if (ns != null) {
						String[] ids = ns.split(",");
						String last = ids[ids.length - 1].replaceAll("#", "");
						agents.put(last, agentSpan);
					}
				}

				LKAnnotationLayer targetsLayer = annotatedText.getLayer("MPQA-target");
				HashMap<String, Span<Term>> targets = new HashMap<>();
				for (LKAnnotationEntity entity : targetsLayer.entityList) {
					Span<Term> agentSpan = getSpanFromEntity(entity, document);

					if (agentSpan.size() == 0) {
						LOGGER.debug("Agent span is empty [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					targets.put(entity.localURI, agentSpan);

//					DataElementNode expressionNode = (DataElementNode) entity.data.children.get(0);
//					String ns = expressionNode.attributes.get("ns");
//					if (ns != null) {
//						String[] ids = ns.split(",");
//						String last = ids[ids.length - 1].replaceAll("#", "");
//						targets.put(last, agentSpan);
//					}
				}

				LKAnnotationLayer attLayer = annotatedText.getLayer("MPQA-attitude");
				HashMap<String, HashMap<String, Object>> attitudes = new HashMap<>();
				for (LKAnnotationEntity entity : attLayer.entityList) {
					HashMap<String, Object> thisAttitude = new HashMap<>();
					DataElementNode expressionNode = (DataElementNode) entity.data.children.get(0);
					Span<Term> expressionSpan = getSpanFromEntity(entity, document);

					thisAttitude.put("type", expressionNode.attributes.get("type"));
					thisAttitude.put("expression", expressionSpan);

					String targetID = expressionNode.attributes.get("tl");
					if (targetID != null) {
						targetID = targetID.replaceAll("#", "");
						Span<Term> targetSpan = targets.get(targetID);
						if (targetSpan != null) {
							thisAttitude.put("target", targetSpan);
						}
					}

					attitudes.put(entity.localURI, thisAttitude);
				}

				LKAnnotationLayer dseLayer = annotatedText.getLayer("MPQA-direct-subjective");
				for (LKAnnotationEntity entity : dseLayer.entityList) {

					Span<Term> expressionSpan = getSpanFromEntity(entity, document);
					Span<Term> holderSpan = KAFDocument.newTermSpan();
//					Span<Term> targetSpan = KAFDocument.newTermSpan();

					if (expressionSpan.size() == 0) {
						LOGGER.debug("Expression span is empty [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					Integer sentence = sentenceForSpan(expressionSpan);
					if (sentence == null) {
						LOGGER.warn("Expression span is not in sentence [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					Opinion opinion = document.newOpinion();
					opinion.setLabel("gold-eu.fbk.dkm.pikes.resources.mpqa-subjective");

					Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(expressionSpan);
					DataElementNode expressionNode = (DataElementNode) entity.data.children.get(0);
					opinionExpression.setPolarity(expressionNode.attributes.get("pol"));
					opinionExpression.setStrength(expressionNode.attributes.get("int"));

					String holderString = expressionNode.attributes.get("ns");
					if (holderString != null) {
						String[] parts = holderString.split(",");

						holders:
						for (int i = parts.length - 1; i >= 0; i--) {
							String agentID = parts[i].replaceAll("[^0-9]", "");

							if (hiddenAgents.containsKey(agentID)) {
								opinionExpression.setSentimentProductFeature(hiddenAgents.get(agentID));
								break holders;
							}

							Set<Span<Term>> spans = agents.get(agentID);
							for (Span<Term> termSpan : spans) {
								Integer agentSentence = sentenceForSpan(termSpan);
								if (agentSentence == null) {
									continue;
								}
								if (!agentSentence.equals(sentence)) {
									continue;
								}
								if (termSpan == null) {
									continue;
								}
								holderSpan = termSpan;
								break holders;
							}
						}
					}

					if (holderSpan.size() > 0) {
						Opinion.OpinionHolder opinionHolder = opinion.createOpinionHolder(holderSpan);
					}

					String al = expressionNode.attributes.get("al");
					if (al != null) {
						al = al.replaceAll("#", "");

						HashMap<String, Object> target = attitudes.get(al);
						if (target != null && target.get("expression") != null) {
							Opinion attitude = document.newOpinion();
							String type = (String) target.get("type");
							attitude.setLabel("gold-eu.fbk.dkm.pikes.resources.mpqa-attitude-" + type);
							Opinion.OpinionExpression attitudeExpression = attitude.createOpinionExpression((Span<Term>) target.get("expression"));
							attitudeExpression.setPolarity(expressionNode.attributes.get("pol"));
							attitudeExpression.setStrength(expressionNode.attributes.get("int"));
//							attitudeExpression.setSentimentSemanticType((String) target.get("type"));

							if (holderSpan.size() > 0) {
								Opinion.OpinionHolder opinionHolder = attitude.createOpinionHolder(holderSpan);
							}

							if (target.get("target") != null) {
								Opinion.OpinionTarget opinionTarget = attitude.createOpinionTarget((Span<Term>) target.get("target"));
							}
						}
					}

				}

				LKAnnotationLayer eseLayer = annotatedText.getLayer("MPQA-expressive-subjectivity");
				for (LKAnnotationEntity entity : eseLayer.entityList) {

					Span<Term> expressionSpan = getSpanFromEntity(entity, document);
					Span<Term> holderSpan = KAFDocument.newTermSpan();

					if (expressionSpan.size() == 0) {
						LOGGER.debug("Expression span is empty [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					Integer sentence = sentenceForSpan(expressionSpan);
					if (sentence == null) {
						LOGGER.warn("Expression span is not in sentence [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					Opinion opinion = document.newOpinion();
					opinion.setLabel("gold-eu.fbk.dkm.pikes.resources.mpqa-expressive");

					Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(expressionSpan);
					DataElementNode expressionNode = (DataElementNode) entity.data.children.get(0);
					opinionExpression.setPolarity(expressionNode.attributes.get("pol"));
					opinionExpression.setStrength(expressionNode.attributes.get("int"));

					String holderString = expressionNode.attributes.get("ns");
					if (holderString != null) {
						String[] parts = holderString.split(",");

						holders:
						for (int i = parts.length - 1; i >= 0; i--) {
							String agentID = parts[i].replaceAll("[^0-9]", "");

							if (hiddenAgents.containsKey(agentID)) {
								opinionExpression.setSentimentProductFeature(hiddenAgents.get(agentID));
								break holders;
							}

							Set<Span<Term>> spans = agents.get(agentID);
							for (Span<Term> termSpan : spans) {
								Integer agentSentence = sentenceForSpan(termSpan);
								if (agentSentence == null) {
									continue;
								}
								if (!agentSentence.equals(sentence)) {
									continue;
								}
								if (termSpan == null) {
									continue;
								}
								holderSpan = termSpan;
								break holders;
							}
						}
					}

					if (holderSpan.size() > 0) {
						Opinion.OpinionHolder opinionHolder = opinion.createOpinionHolder(holderSpan);
					}
				}

				LKAnnotationLayer oseLayer = annotatedText.getLayer("MPQA-objective-speech-event");
				for (LKAnnotationEntity entity : oseLayer.entityList) {

					Span<Term> expressionSpan = getSpanFromEntity(entity, document);
					Span<Term> holderSpan = KAFDocument.newTermSpan();

					if (expressionSpan.size() == 0) {
						LOGGER.debug("Expression span is empty [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					Integer sentence = sentenceForSpan(expressionSpan);
					if (sentence == null) {
						LOGGER.warn("Expression span is not in sentence [{}/{}]", nafFile.getName(), entity.localURI);
						continue;
					}

					Opinion opinion = document.newOpinion();
					opinion.setLabel("gold-eu.fbk.dkm.pikes.resources.mpqa-objective");

					Opinion.OpinionExpression opinionExpression = opinion.createOpinionExpression(expressionSpan);
					DataElementNode expressionNode = (DataElementNode) entity.data.children.get(0);

					String holderString = expressionNode.attributes.get("ns");
					if (holderString != null) {
						String[] parts = holderString.split(",");

						holders:
						for (int i = parts.length - 1; i >= 0; i--) {
							String agentID = parts[i].replaceAll("[^0-9]", "");

							if (hiddenAgents.containsKey(agentID)) {
								opinionExpression.setSentimentProductFeature(hiddenAgents.get(agentID));
								break holders;
							}

							Set<Span<Term>> spans = agents.get(agentID);
							for (Span<Term> termSpan : spans) {
								Integer agentSentence = sentenceForSpan(termSpan);
								if (agentSentence == null) {
									continue;
								}
								if (!agentSentence.equals(sentence)) {
									continue;
								}
								if (termSpan == null) {
									continue;
								}
								holderSpan = termSpan;
								break holders;
							}
						}
					}

					if (holderSpan.size() > 0) {
						Opinion.OpinionHolder opinionHolder = opinion.createOpinionHolder(holderSpan);
					}

				}

				NAFFilter.builder(false).withSRLRoleLinking(true, true)
						.withOpinionLinking(true, true).build().filter(document);

				document.save(nafFile.getAbsolutePath());

//				System.out.println(document.toString());
//				break;
			}

		} catch (Throwable ex) {
			CommandLine.fail(ex);
			System.exit(1);
		}

	}
}
