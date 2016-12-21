package eu.fbk.dkm.pikes.naflib;

import eu.fbk.utils.core.CommandLine;
import ixa.kaflib.LinguisticProcessor;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by alessio on 04/06/15.
 */

public class CorpusStatistics {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusStatistics.class);
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("statistics")
					.withHeader("Calculate statistics on a corpus")
					.withOption("i", "input-folder", "the folder of the NAF corpus", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("r", "recursive", "parse folder recursively")
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File inputFolder = cmd.getOptionValue("input-folder", File.class);
			Boolean recursive = cmd.hasOption("recursive");

			Corpus corpus = Corpus.create(recursive, inputFolder);

			final AtomicLong tokens = new AtomicLong();
			final AtomicLong documents = new AtomicLong();
			final AtomicLong sentences = new AtomicLong();
			final AtomicLong milliseconds = new AtomicLong();

			final Object lock = new Object();

			corpus.parallelStream().forEach(document -> {
				if (document != null) {
					tokens.addAndGet(document.getTerms().size());
					long numDoc = documents.incrementAndGet();

					synchronized (lock) {
						System.out.print(".");
						if (numDoc % 100 == 0) {
							System.out.print(" ");
							System.out.print(numDoc);
							System.out.println();
						}
					}

					sentences.addAndGet(document.getSentences().size());

					Long start = null;
					Long end = null;

					for (String lpName : document.getLinguisticProcessors().keySet()) {
						List<LinguisticProcessor> linguisticProcessors = document.getLinguisticProcessors().get(lpName);
						for (LinguisticProcessor linguisticProcessor : linguisticProcessors) {
							Date startDate = null, endDate = null;
							try {
								synchronized (lock) {
									startDate = sdf.parse(linguisticProcessor.getBeginTimestamp());
									endDate = sdf.parse(linguisticProcessor.getEndTimestamp());
								}
							} catch (Exception e) {
								continue;
							}

							if (start == null || startDate.getTime() < start) {
								start = startDate.getTime();
							}
							if (end == null || endDate.getTime() > end) {
								end = endDate.getTime();
							}
						}
					}

					if (start != null && end != null) {
						Long diff = end - start;
						if (diff > 0 && diff < 1000000) {
							milliseconds.addAndGet(diff);
						}
//						else {
//							System.out.println(new Date(start));
//							System.out.println(new Date(end));
//							System.out.println(document.getPublic().uri);
//						}
					}
				}
			});

			System.out.println("\n");

			LOGGER.info("Documents: {}", documents.get());
			LOGGER.info("Sentences: {}", sentences.get());
			LOGGER.info("Tokens: {}", tokens.get());
			LOGGER.info("Milliseconds: {}", milliseconds.get());
			LOGGER.info("Milliseconds per token: {}", (milliseconds.get() * 1.0) / (tokens.get() * 1.0));
			LOGGER.info("Milliseconds per sentence: {}", (milliseconds.get() * 1.0) / (sentences.get() * 1.0));
			LOGGER.info("Milliseconds per document: {}", (milliseconds.get() * 1.0) / (documents.get() * 1.0));

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
