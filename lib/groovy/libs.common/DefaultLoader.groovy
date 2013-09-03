/**
 * 汎用Groovy script loader
 */
import org.wiperdog.directorywatcher.Listener
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil;



/**
 * default loader class for ordinary script
 * 通常スクリプト用loader,これまでとほぼ同じ動作
 * IST_HOME/lib/groovy/*.groovyを監視してロード（実行）する。
 */
class DefaultLoader implements Listener {
	def shell
	def dir
	def interval
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner");
	def properties

	public DefaultLoader(BundleContext context, GroovyShell shell) {
		this.shell = shell
		MonitorJobConfigLoader configLoader = new MonitorJobConfigLoader(context)
		properties = configLoader.getProperties()
		dir = properties.get(ResourceConstants.GROOVY_FILE_DIRECTORY)
		interval = 5000
	}

	public boolean filterFile(File file) {
		return file.getAbsolutePath().endsWith(".groovy");
	}

	public String getDirectory() {
		return dir;
	}

	public long getInterval() {
		return interval;
	}

	public boolean notifyAdded(File target) throws IOException {
		try {
			use (OSGiCompanionCategory, LoggingCategory) {
				shell.evaluate target
			}
		} catch (Throwable t) {
			logger.info("Loading groovy file caused an error", t);
		}
		return true;
	}

	public boolean notifyDeleted(File target) throws IOException {
		return true;
	}

	public boolean notifyModified(File target) throws IOException {
		try {
			use (OSGiCompanionCategory, LoggingCategory) {
				shell.evaluate target
			}
		} catch (Throwable t) {
			logger.info("Loading groovy file caused an error", t);
		}
		return true;
	}
}
