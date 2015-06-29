package eu.fbk.dkm.pikes.tintop.old;

import eu.fbk.dkm.pikes.tintop.util.GenericCommandLine;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.SSTspan;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fbk.cit.hlt.thewikimachine.util.CommandLineWithLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 07/08/14
 * Time: 12:22
 * To change this template use File | Settings | File Templates.
 */

public class SST extends GenericCommandLine {

	static Logger logger = Logger.getLogger(SST.class.getName());
	static Pattern typeLabelPattern = Pattern.compile(".-([a-zA-Z0-9]+)\\.([a-zA-Z0-9]+)");
	private String baseDir;

	public SST(String baseDir) {
		super();
		if (!baseDir.endsWith(File.separator)) {
			baseDir += File.separator;
		}
		this.baseDir = baseDir;
	}

	public ArrayList<String> runFromStrings(ArrayList<String> input) throws IOException, InterruptedException {
		StringBuffer sb = new StringBuffer();
		for (String s : input) {
			sb.append(s);
		}

		File inputFile = File.createTempFile("sst-parse-1-", "wpt");
		FileUtils.writeStringToFile(inputFile, sb.toString());
		ArrayList<String> out = run(inputFile);

		inputFile.delete();

		return out;
	}

	public ArrayList<String> run(File sourceFile) throws IOException, InterruptedException {

		String sstExecutable = "./sst";
		String sstModel = "MODELS/SEM07_base_12";
		String sstTagset = "DATA/WNSS_07.TAGSET";
		String sstGaz = "DATA/GAZ/gazlistall_minussemcor";

		String[] step1 = {sstExecutable, "basic-feats", sourceFile.getAbsolutePath(), "1", "BIO", sstGaz, "0"};
		ArrayList<String> fv = genericRun(baseDir, step1);
		File fvFile = File.createTempFile("sst-parse-2-", "fv");
		StringBuilder sb = new StringBuilder();
		for (String s : fv) {
			sb.append(s);
			sb.append("\n");
		}

		FileUtils.writeStringToFile(fvFile, sb.toString());

		String[] step2 = {sstExecutable, "tag", sstModel, fvFile.getAbsolutePath(), sstTagset, "0", "0"};
		ArrayList<String> out = genericRun(baseDir, step2);

		return out;

//		File outFile = File.createTempFile("sst-parse", "fv");
//		FileUtils.writeStringToFile(outFile, out);
//
//		fvFile.delete();
//		outFile.delete();
	}

	public static void main(String[] args) {
		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();

		commandLineWithLogger.addOption(OptionBuilder.withDescription("Base folder").isRequired().hasArg().withArgName("folder").withLongOpt("folder").create("f"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("Input file").isRequired().hasArg().withArgName("file").withLongOpt("input").create("i"));

		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		String folder = commandLine.getOptionValue("folder");
		String fileName = commandLine.getOptionValue("input");

		SST sst = new SST(folder);
		try {
			File file = new File(fileName);
			ArrayList<String> out = sst.run(file);
			System.out.println(out);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public static String getSentenceString(int sentIndex, ArrayList<Term> terms, ArrayList<String> ners) {
		StringBuffer sb = new StringBuffer();
		sb.append("S-");
		sb.append(sentIndex);
		for (int i = 0; i < terms.size(); i++) {
			sb.append("\t");
			sb.append(terms.get(i).getStr().replace(' ', '-'));
			sb.append(" ");
			sb.append(terms.get(i).getMorphofeat());
			sb.append(" ");
			sb.append(ners.get(i));
		}
		sb.append("\n");

		return sb.toString();
	}

	public static void updateTerms(ArrayList<String> out, ArrayList<Term> allTerms, KAFDocument document) {
		int index = 0;
		Span<Term> terms = null;
		String type = null, label = null;

		for (String s : out) {
			String[] parts = s.split("\t");
			for (int i = 1; i < parts.length; i++) {
				if (!parts[i].equals("0")) {
					if (parts[i].startsWith("B")) {
						if (terms != null) {
							SSTspan span = document.newSST(terms, type, label);
							terms = null;
						}

						Matcher m = typeLabelPattern.matcher(parts[i]);
						if (m.find()) {
							terms = KAFDocument.newTermSpan();
							terms.addTarget(allTerms.get(index));
							type = m.group(1);
							label = m.group(2);
						}
					}
					if (parts[i].startsWith("I")) {
						if (terms != null) {
							terms.addTarget(allTerms.get(index));
						}
					}
					allTerms.get(index).setSupersenseTag(parts[i]);
				}
				else {
					if (terms != null) {
						SSTspan span = document.newSST(terms, type, label);
						terms = null;
					}
				}
				index++;
			}
		}
	}
}
