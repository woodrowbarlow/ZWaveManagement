package me.wbarlow.zwavemanagement.logging;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * The OpenHAB Z-Wave module has extensive logging. Originally, it was
 * importing a third-party logging library. Since we're trying to keep
 * everything stripped down, I took out the library and pointed all the
 * logging to this class instead. We can extend this in the future to log
 * to a file or whatever stream we want.
 * @author Woodrow Barlow
 *
 */
public class Logger {

	private String packageName;
	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}

	private String simpleName;
	private String logFilename;
	private Writer fileout;

	public enum LogLevel {
		INFO, TRACE,
		DEBUG, WARN,
		ERROR
	}

	public enum LogStream {
		STDOUT, STDERR,
		SYSLOG, FILE
	}

	private Map<LogLevel, LogStream[]> streamsConfig = new HashMap<LogLevel, LogStream[]>();

	public Logger(String packageName) {
		this(packageName, packageName + "_log.txt");
	}

	public Logger(String packageName, String logFilename) {

		this.packageName = packageName;
		try { this.simpleName = packageName.substring(packageName.lastIndexOf('.') + 1); }
		catch(StringIndexOutOfBoundsException e) { this.simpleName = this.packageName; }
		this.logFilename = logFilename;

		/* Set some sensible defaults for stream configuration. */
		streamsConfig.put(LogLevel.INFO, new LogStream[] {LogStream.STDOUT});
		streamsConfig.put(LogLevel.TRACE, new LogStream[] {LogStream.STDOUT});
		streamsConfig.put(LogLevel.DEBUG, new LogStream[] {LogStream.STDOUT});
		streamsConfig.put(LogLevel.WARN, new LogStream[] {LogStream.STDERR, LogStream.FILE});
		streamsConfig.put(LogLevel.ERROR, new LogStream[] {LogStream.STDERR, LogStream.FILE, LogStream.SYSLOG});

		try {
			this.fileout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.logFilename), "utf-8"));
		}
		// when the error logging mechanism encounters an error... who watches the watchmen?
		catch (UnsupportedEncodingException e) {
			//System.err.println("Logging Mechanism Error: Unsupported filetype encoding (utf-8).");
			//System.err.println(e.toString());
		}
		catch (FileNotFoundException e) {
			//System.err.println("Logging Mechanism Error: Unable to create the logging file.");
			//System.err.println(e.toString());
		}
	}

	public void configLogStreamsForLevel(LogLevel level, LogStream ... streams) {
		this.streamsConfig.put(level, streams);
	}

	/**
	 * Log the given message to stdout.
	 * @param s Message to be logged.
	 */
	private void writeToStdout(String s) {
		System.out.print(s);
	}

	/**
	 * Log the given message to stderr.
	 * @param s Message to be logged.
	 */
	private void writeToStderr(String s) {
		System.err.print(s);
	}

	/**
	 * Log the given message to syslog.
	 * @param s Message to be logged.
	 */
	private void writeToSyslog(String s) {
		// TODO ¯\_(ツ)_/¯
		return;
	}

	/**
	 * Log the given message to file.
	 * @param s Message to be logged.
	 */
	private void writeToFile(String s) {
		try {
			this.fileout.write(s);
		} catch (IOException e) {
			//System.err.println("Logging Mechanism Error: Failed to write to logging file.");
			//System.err.println(e.toString());
		}
	}

	/**
	 * Log the given message to the appropriate streams.
	 * @param message Message to be logged.
	 * @param level The log level.
	 */
	private void logToStreams(String message, LogLevel level) {
		message = "[" + level.toString().toLowerCase() + "] [" + this.simpleName + "] " + message;
		LogStream[] streams = this.streamsConfig.get(level);
		if(streams != null) for(LogStream stream : streams) {
			switch(stream) {
				case STDOUT:
					writeToStdout(message);
					break;
				case STDERR:
					writeToStderr(message);
					break;
				case SYSLOG:
					writeToSyslog(message);
					break;
				case FILE:
					writeToFile(message);
					break;
			}
		}
	}

	/**
	 * Given a collection of objects, construct a single String.
	 * @param objs A collection of objects with meaningful String
	 * representations (variatic).
	 * @return A sequential String representation of the given objects.
	 */
	private String constructMessage(Object...objs) {
		String s = "";
		for(Object obj : objs) {
			s += String.valueOf(obj);
		}
		s += "\n";
		return s;
	}

	public void close() {
		try {
			this.fileout.close();
		} catch (IOException e) {
			//System.err.println("Logging Mechanism Error: Failed to close logging file.");
			//System.err.println("e.toString()");
		}
	}

	/**
	 * Log an info message.
	 * @param objs A collection of objects with meaningful String
	 * representations (variatic).
	 */
	public void info(Object...objs) {
		String s = constructMessage(objs);
		logToStreams(s, LogLevel.INFO);
	}

	/**
	 * Log a trace message.
	 * @param objs A collection of objects with meaningful String
	 * representations (variatic).
	 */
	public void trace(Object...objs) {
		String s = constructMessage(objs);
		logToStreams(s, LogLevel.TRACE);
	}

	/**
	 * Log a debug message.
	 * @param objs A collection of objects with meaningful String
	 * representations (variatic).
	 */
	public void debug(Object...objs) {
		String s = constructMessage(objs);
		logToStreams(s, LogLevel.DEBUG);
	}

	/**
	 * Log a warning message.
	 * @param objs A collection of objects with meaningful String
	 * representations (variatic).
	 */
	public void warn(Object...objs) {
		String s = constructMessage(objs);
		logToStreams(s, LogLevel.WARN);
	}

	/**
	 * Log an error message.
	 * @param objs A collection of objects with meaningful String
	 * representations (variatic).
	 */
	public void error(Object...objs) {
		String s = constructMessage(objs);
		logToStreams(s, LogLevel.ERROR);
	}

}
