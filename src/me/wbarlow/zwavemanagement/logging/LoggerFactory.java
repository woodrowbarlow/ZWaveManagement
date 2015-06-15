package me.wbarlow.zwavemanagement.logging;

import java.util.HashMap;
import java.util.Map;

import me.wbarlow.zwavemanagement.logging.Logger.LogLevel;
import me.wbarlow.zwavemanagement.logging.Logger.LogStream;

/**
 * The OpenHAB Z-Wave module has extensive logging. Originally, it was
 * importing a third-party logging library. Since we're trying to keep
 * everything stripped down, I took out the library and pointed all the
 * logging to this class instead. We can extend this in the future to log
 * to a file or whatever stream we want.
 * @author Woodrow Barlow
 *
 */
public class LoggerFactory {

	private static Map<String, Logger> loggers = new HashMap<String, Logger>();

	/*
	 * I guess the original logging library created a different logger for each
	 * class (possibly to configure log level output on a per-class basis). We
	 * probably won't bother with that.
	 */
	public static Logger getLogger(Class<?> c) {
		String pkg = c.getPackage().getName();
		if(loggers.get(pkg) == null) {
			// create the logger
			Logger logger = new Logger(pkg);
			// add additional logging configuration
			if((pkg.startsWith("me.wbarlow.zwavemanagement"))
					|| pkg.startsWith("fi.iki.elonen")) {
				logger.configLogStreamsForLevel(LogLevel.INFO, LogStream.STDERR);
				logger.configLogStreamsForLevel(LogLevel.TRACE, LogStream.STDERR);
				logger.configLogStreamsForLevel(LogLevel.DEBUG, LogStream.STDERR);
			}
			loggers.put(pkg, logger);
		}
		return loggers.get(pkg);
	}

	// TODO: somehow guarantee that streams get closed before the bundle stops.
	public static void close() {
		for(Logger logger : loggers.values())
			logger.close();
	}

}
