import org.apache.log4j.Logger;

/**
 * log出力機能追加用Category
 */
class LoggingCategory {
	static def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner");
	static def error(Script self, msg, Throwable t) {
		t != null ? logger.error(msg, t) : logger.error(msg)
	}

	static def debug(Script self, msg, Throwable t) {
		t != null ? logger.debug(msg, t) : logger.debug(msg)
	}

	static def warn(Script self, msg, Throwable t) {
		t != null ? logger.warn(msg, t) : logger.warn(msg)
	}

	static def trace(Script self, msg, Throwable t) {
		t != null ? logger.trace(msg, t) : logger.trace(msg)
	}

	static def error(Script self, msg) {
		error(self, msg, null)
	}

	static def debug(Script self, msg) {
		debug(self, msg, null)
	}

	static def warn(Script self, msg) {
		warn(self, msg, null)
	}

	static def trace(Script self, msg) {
		trace(self, msg, null)
	}

	static def info(Script self, msg) {
		info(self, msg, null)
	}
}


