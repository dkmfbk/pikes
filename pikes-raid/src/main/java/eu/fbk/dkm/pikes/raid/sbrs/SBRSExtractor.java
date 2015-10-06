package eu.fbk.dkm.pikes.raid.sbrs;

import eu.fbk.dkm.pikes.raid.Component;
import eu.fbk.dkm.pikes.raid.Extractor;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;

import java.util.EnumSet;

/**
 * Created by alessio on 20/08/15.
 */

public class SBRSExtractor extends Extractor {


	@Override
	protected Iterable<Opinion> doExtract(KAFDocument document, int sentenceID, EnumSet<Component> components) {
		return null;
	}

	@Override
	protected Iterable<Opinion> doRefine(KAFDocument document, int sentenceID, EnumSet<Component> components, Opinion opinion) {
		return null;
	}
}
