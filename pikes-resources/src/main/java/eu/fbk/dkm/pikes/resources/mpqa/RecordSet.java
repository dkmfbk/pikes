package eu.fbk.dkm.pikes.resources.mpqa;

import com.glaforge.i18n.io.CharsetToolkit;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by alessio on 24/03/15.
 */
public final class RecordSet {

	public static final RecordSet EMPTY = new RecordSet(ImmutableList.<Record>of());
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordSet.class);

	private final List<Record> records;

	public RecordSet(final Reader reader) throws IOException {
		final List<Record> records = Lists.newArrayList();
		for (final String line : CharStreams.readLines(reader)) {
			final String trimmedLine = line.trim();
			if (!trimmedLine.startsWith("#")) {
				final Record record = new Record(line);
				records.add(record);
			}
		}
		this.records = ImmutableList.copyOf(records);
	}

	public RecordSet(final Iterable<? extends Record> records) {
		this.records = ImmutableList.copyOf(records);
	}

	public List<Record> getRecords() {
		return this.records;
	}

	public List<Record> getRecords(final String recordName) {
		final List<Record> result = Lists.newArrayList();
		for (final Record record : this.records) {
			if (record.getName().equals(recordName)) {
				result.add(record);
			}
		}
		return result;
	}

	@Nullable
	public Record getRecord(final String recordName, final String key, final String value) {
		for (final Record record : this.records) {
			if (record.getName().equals(recordName) && value.equals(record.getValue(key))) {
				return record;
			}
		}
		return null;
	}

	public String getRecordValue(final String recordName, final @Nullable String defaultValue) {
		for (final Record record : this.records) {
			if (record.getName().equals(recordName)) {
				return record.getValue();
			}
		}
		return defaultValue;
	}

	public static RecordSet readFromFile(@Nullable final File file) throws IOException {
		if (file == null || !file.exists()) {
			return RecordSet.EMPTY;
		}
		try (Reader reader = read(file)) {
			return new RecordSet(reader);
		}
	}

	public static BufferedReader read(final File mpqaFile) throws IOException {
		final Charset charset = CharsetToolkit.guessEncoding(mpqaFile, 4096, Charsets.UTF_8);
		return new BufferedReader(new InputStreamReader(new FileInputStream(mpqaFile), charset));
	}

	public static void main(String[] args) throws IOException {
		final CommandLine cmd = CommandLine
				.parser()
				.withName("file-test")
				.withHeader("Check eu.fbk.dkm.pikes.resources.mpqa file")
				.withOption("i", "input-file", "the MPQA file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
				.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

		final File inputFile = cmd.getOptionValue("i", File.class);
		final RecordSet annotations = RecordSet.readFromFile(inputFile);

		for (Record record : annotations.getRecords()) {
			System.out.println(record.getName());
			System.out.println(record.getSpan());
			for (String attr : record.getValueMap().keySet()) {
				System.out.println(attr + " = " + record.getValueMap().get(attr));
			}
			System.out.println();
		}
	}

}
