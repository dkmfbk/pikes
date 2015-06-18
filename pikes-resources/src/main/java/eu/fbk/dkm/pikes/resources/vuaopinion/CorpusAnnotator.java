package eu.fbk.dkm.pikes.resources.vuaopinion;

import eu.fbk.dkm.pikes.resources.NAFFilter;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Created by alessio on 09/04/15.
 */

public class CorpusAnnotator {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusAnnotator.class);

	public static void main(String[] args) {
		try {
			CommandLine cmd = null;
			cmd = CommandLine
					.parser()
					.withName("corpus-postprocessor")
					.withHeader(
							"Add opinion layers to the parsed NAFs")
					.withOption("i", "input-path", "the base EN path of the corpus", "DIR",
							CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("f", "force", "Force opinion")
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			boolean forceOpinion = cmd.hasOption("f");
			 
			final File inputPath = cmd.getOptionValue("i", File.class);
			if (!inputPath.exists()) {
				throw new IOException(String.format("Folder %s does not exist", inputPath.getAbsolutePath()));
			}

			File kafPath = new File(inputPath.getAbsolutePath() + File.separator + "kaf");
			if (!kafPath.exists()) {
				throw new IOException(String.format("Folder %s does not exist", kafPath.getAbsolutePath()));
			}
			File nafPath = new File(inputPath.getAbsolutePath() + File.separator + "naf-parsed");
			if (!nafPath.exists()) {
				throw new IOException(String.format("Folder %s does not exist", nafPath.getAbsolutePath()));
			}

			Iterator<File> fileIterator;
			fileIterator = FileUtils.iterateFiles(kafPath, new String[]{"kaf"}, false);

			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				String fileBaseName = FilenameUtils.removeExtension(file.getName());
				KAFDocument document = KAFDocument.createFromFile(file);

				File nafFile = new File(nafPath.getAbsolutePath() + File.separator + fileBaseName + ".naf");
				if (!nafFile.exists()) {
					LOGGER.warn(String.format("File %s does not exist", nafFile.getAbsolutePath()));
					continue;
				}
				KAFDocument nafDoc = KAFDocument.createFromFile(nafFile);
				HashMap<String, Term> nafTerms = new HashMap<>();
				for (Term term : nafDoc.getTerms()) {
					nafTerms.put(term.getId(), term);
				}

				HashMap<String, String> idConverter = new HashMap<>();
				int i = 0;
				for (WF wf : document.getWFs()) {
					String id = wf.getId();
					id = id.replace('w', 't');
					idConverter.put(id, "t" + Integer.toString(++i));
				}
				
				boolean hasGoldOpinions = false;
                for (Opinion opinion : document.getOpinions()) {
                    if ("gold-vua-opinion".equals(opinion.getLabel())) {
                        hasGoldOpinions = true;
                        break;
                    }
                }
                
                if (hasGoldOpinions && !forceOpinion) {
                    LOGGER.info("Opinions already present, skipping...");
                
                } else {
    				for (Opinion opinion : document.getOpinions()) {
    					Opinion newOpinion = nafDoc.newOpinion();
    					newOpinion.setLabel("gold-vua-opinion");
    
    					Span<Term> termSpan;
    
    					// Expression
    					if (opinion.getOpinionExpression() != null) {
    						termSpan = KAFDocument.newTermSpan();
    						for (Term term : opinion.getOpinionExpression().getTerms()) {
    							termSpan.addTarget(nafTerms.get(idConverter.get(term.getId())));
    						}
    						Opinion.OpinionExpression expression = newOpinion.createOpinionExpression(termSpan);
    						expression.setPolarity(opinion.getOpinionExpression().getPolarity());
    					}
    
    					// Holder
    					if (opinion.getOpinionHolder() != null) {
    						termSpan = KAFDocument.newTermSpan();
    						for (Term term : opinion.getOpinionHolder().getTerms()) {
    							termSpan.addTarget(nafTerms.get(idConverter.get(term.getId())));
    						}
    						newOpinion.createOpinionHolder(termSpan);
    					}
    
    					// Target
    					if (opinion.getOpinionTarget() != null) {
    						termSpan = KAFDocument.newTermSpan();
    						for (Term term : opinion.getOpinionTarget().getTerms()) {
    							termSpan.addTarget(nafTerms.get(idConverter.get(term.getId())));
    						}
    						newOpinion.createOpinionTarget(termSpan);
    					}
    				}
                }
                
                NAFFilter.builder(false).withSRLRoleLinking(true, true)
                        .withOpinionLinking(true, true).build().filter(document);
                
				nafDoc.save(nafFile.getAbsolutePath());
			}

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
