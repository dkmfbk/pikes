package eu.fbk.dkm.pikes.tintop.orchestrator;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by alessio on 02/03/15.
 */

public class TintopSession {

	static Logger logger = Logger.getLogger(TintopSession.class.getName());
	private File input;
	private File output;
	private HashSet<String> skipPatterns;
	private Iterator<File> fileIterator;

	public File getInput() {
		return input;
	}

	public File getOutput() {
		return output;
	}

	public HashSet<String> getSkipPatterns() {
		return skipPatterns;
	}

	public Iterator<File> getFileIterator() {
		return fileIterator;
	}

	public TintopSession(File input, File output, Iterator<File> fileIterator) {
		this(input, output, fileIterator, null);
	}
	
	public TintopSession(File input, File output, Iterator<File> fileIterator, HashSet<String> skipPatterns) {

		this.input = input;
		this.output = output;
		this.skipPatterns = skipPatterns;
		this.fileIterator = fileIterator;
	}
}
