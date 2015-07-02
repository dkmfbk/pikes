package eu.fbk.dkm.pikes.tintop;

import org.apache.commons.cli.*;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class CommandLineWithLogger {

	private Options options;
	private String logConfig;
	private Properties loggerProps;
	private String version;

	private boolean debug;
	private boolean trace;

	public CommandLineWithLogger() {
		logConfig = System.getProperty("log-config");
		if (logConfig == null) {
			logConfig = "log-config.txt";
		}

		options = new Options();
		options.addOption("h", "help", false, "Print this message");
		options.addOption(OptionBuilder.withDescription("trace mode").withLongOpt("trace").create());
		options.addOption(OptionBuilder.withDescription("debug mode").withLongOpt("debug").create());
	}

	public void addOption(Option option) {
		options.addOption(option);
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isDebug() {
		return debug;
	}

	public boolean isTrace() {
		return trace;
	}

	public Options getOptions() {
		return options;
	}

	public void setLogConfig(String logConfig) {
		this.logConfig = logConfig;
	}

	public void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(150, "java <CLASS>", "\n", options, "\n", true);
	}

	public Properties getLoggerProps() {
		return loggerProps;
	}

	public CommandLine getCommandLine(String[] args) throws ParseException {
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		if (version != null) {
			options.addOption(OptionBuilder.withDescription("Output version information and exit").withLongOpt("version").create());
		}
		try {
			commandLine = parser.parse(options, args);

			debug = false;
			debug = false;

			loggerProps = new Properties();
			try {
				loggerProps.load(new InputStreamReader(new FileInputStream(logConfig), "UTF-8"));
			} catch (Exception e) {
				loggerProps.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
				loggerProps.setProperty("log4j.appender.stdout.layout.ConversionPattern", "[%t] %-5p (%F:%L) - %m %n");
				loggerProps.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
				loggerProps.setProperty("log4j.appender.stdout.Encoding", "UTF-8");
			}

			if (commandLine.hasOption("trace")) {
				loggerProps.setProperty("log4j.rootLogger", "trace,stdout");
				trace = true;
			}
			else if (commandLine.hasOption("debug")) {
				loggerProps.setProperty("log4j.rootLogger", "debug,stdout");
				debug = true;
			}
			else {
				if (loggerProps.getProperty("log4j.rootLogger") == null) {
					loggerProps.setProperty("log4j.rootLogger", "info,stdout");
				}
			}

			if (commandLine.hasOption("help")) {
				throw new ParseException("");
			}
			if (commandLine.hasOption("version")) {
				throw new ParseException("Version: " + version);
			}

		} catch (ParseException exp) {
			String message = exp.getMessage();
			if (message != null && message.length() > 0) {
				System.err.println("Parsing failed: " + message + "\n");
			}
			printHelp();
			throw exp;
		}

		return commandLine;
	}
}