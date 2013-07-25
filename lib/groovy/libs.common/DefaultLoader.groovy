/*
 *  Copyright 2013 Insight technology,inc. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * 汎用Groovy script loader
 */
import com.insight_tec.pi.directorywatcher.Listener
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil;

/**
 * ctx (BundleContext) を少し楽に使えるようにCategoryを作成
 */
class OSGiCompanionCategory {
	static def registMap = [:]
	static def safeRegisterService(BundleContext self, String name, Object service, properties) {
		println registMap
		// def key = [name: name, props: properties]
		def key = service.class.getName()
		def reg = registMap[key]
		if (reg != null) {
			reg.unregister()
		}
		reg = self.registerService(name, service, properties)
		registMap[key] = reg
		return reg
	}
}

/**
 * log出力機能追加用Category
 */
class LoggingCategory {
	static def logger = Logger.getLogger("com.insight_tec.pi.scriptsupport.groovyrunner");
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


/**
 * default loader class for ordinary script
 * 通常スクリプト用loader,これまでとほぼ同じ動作
 * IST_HOME/lib/groovy/*.groovyを監視してロード（実行）する。
 */
class DefaultLoader implements Listener {
	def shell
	def dir
	def interval
	def logger = Logger.getLogger("com.insight_tec.pi.scriptsupport.groovyrunner");
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

