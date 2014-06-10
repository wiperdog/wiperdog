
import org.wiperdog.directorywatcher.Listener
import org.apache.log4j.Logger
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceReference
import org.osgi.util.tracker.ServiceTracker
import org.osgi.util.tracker.ServiceTrackerCustomizer

/**
 * Listener using for watching change from jobs file
 */
class JobListener implements Listener,ServiceTrackerCustomizer {
	def dir
	def interval
	def context
	def trackerObj
	def properties
	def jobdsl
	public JobListener(BundleContext ctx) {
		this.context = ctx
		MonitorJobConfigLoader configLoader = new MonitorJobConfigLoader(context)
		properties = configLoader.getProperties();
		dir = properties.get(ResourceConstants.JOB_DIRECTORY)
		interval = 5000
		trackerObj = new ServiceTracker(context, JobDSLService.class.getName(), this)
		trackerObj.open()

	}

	public boolean filterFile(File file) {
		return file.getName().endsWith(".job") ;
	}

	public String getDirectory() {
		return dir;
	}

	public long getInterval() {
		return interval;
	}

	public boolean notifyAdded(File target) throws IOException {
		return jobdsl.processJob(target);
	}

	public boolean notifyDeleted(File target) throws IOException {
		return jobdsl.removeJob(target)
	}

	public boolean notifyModified(File target) throws IOException {
		return jobdsl.processJob(target);
	}
	public Object addingService(ServiceReference reference) {
		jobdsl = context.getService(reference);
		return jobdsl
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
	}

}
