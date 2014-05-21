// job loader

import org.wiperdog.directorywatcher.Listener
import org.osgi.framework.ServiceReference
import org.osgi.util.tracker.ServiceTracker
import org.osgi.util.tracker.ServiceTrackerCustomizer
import org.wiperdog.jobmanager.JobFacade
import org.apache.log4j.Logger
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil

/**
 * job 専用loader
 * (未実装）
 * job の記述が簡潔になるよう、いろいろコチラ側で処理してあげる。
 *  IST_HOME/var/jobs/*.job を監視してロードする。
 */
class JobLoader implements ServiceTrackerCustomizer {
	def shell
	def dir
	def interval
	def context
	def trackerObj
	def jobdsl
	def jobfacade
	def properties

	public JobLoader(BundleContext ctx, GroovyShell shell) {
		this.shell = shell
		this.context = ctx
		MonitorJobConfigLoader configLoader = new MonitorJobConfigLoader(context)
		properties = configLoader.getProperties();
		dir = properties.get(ResourceConstants.JOB_DIRECTORY)
		interval = 5000
		trackerObj = new ServiceTracker(context, JobFacade.class.getName(), this)
		trackerObj.open()
	}

	/**
	 * ServiceTrackerCustomizer.addingService
	 * 以下は、ServiceTrackerCustomizerの実装部
	 *  (http://www.osgi.org/javadoc/r4v42/org/osgi/util/tracker/ServiceTrackerCustomizer.html)
	 */
	public Object addingService(ServiceReference reference) {
		def oservice = context.getService(reference);
		if (oservice instanceof JobFacade) {
			jobfacade = oservice
			// prepare jobdsl object here.
			def JobDsl = shell.getClassLoader().loadClass("JobDsl")
			jobdsl = JobDsl.newInstance(shell, jobfacade, context)
		}
		this.context.registerService(JobDSLService.class.getName(),jobdsl,null)
		return oservice
	}

	/**
	 * ServiceTrackerCustormizer.modifiedService
	 */
	public void modifiedService(ServiceReference reference, Object service) {
	}

	/**
	 * ServiceTrackerCustomizer.removedService
	 */
	public void removedService(ServiceReference reference, Object service)  {
		if (service == jobfacade) {
			jobfacade = null
			jobdsl = null
		}
	}
}


