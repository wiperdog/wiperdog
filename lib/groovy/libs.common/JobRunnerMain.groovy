import org.wiperdog.jobmanager.JobFacade
import org.osgi.framework.BundleContext
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.osgi.framework.ServiceReference
import org.osgi.util.tracker.ServiceTracker
import org.osgi.util.tracker.ServiceTrackerCustomizer
import groovy.json.JsonBuilder
public class JobRunnerMain implements JobRunnerMainService{
	GroovyShell shell;
	def binding
	def context
	JobFacade jobfacade
	def jobdsl

	public JobRunnerMain(shell, context){
		this.shell = shell
		binding = shell.getContext()
		this.context = context
		def trackerJF = new ServiceTracker(context, JobFacade.class.getName(),new JFServiceTracker())
		trackerJF.open()
		def trackerJobDsl = new ServiceTracker(context, JobDSLService.class.getName(), new JobDslServiceTracker())
		trackerJobDsl.open()
	}
	
	public void executeJob(String jobFileName, String trig){
		try {
			def jobName
			def jobFile = new File(jobFileName) 
			jobFile.getText().eachLine{ 
				if(it.trim().replace(" ","").contains("JOB=")){
					jobName = shell.evaluate(it.split("=")[1])["name"]
				}
			}
			def dest = '[[http:"http://localhost:8089/runjob/data"]]'
			
			def jobText = jobFile.getText().replaceAll("\nDEST.*=.*","\nDEST=${dest}")
			if(!jobText.contains("\nDEST")) {
				jobText+="\nDEST=${dest}"
			}
			def tmpJobFile = new File(System.getProperty("felix.home") + File.separator + "tmp" + File.separator + jobFile.getName())
			if(!tmpJobFile.exists()) {
				tmpJobFile.createNewFile()
			}
			tmpJobFile.setText(jobText)
			jobdsl.processJob(tmpJobFile)
			def jobObject = jobfacade.getJob(jobName)
			if(trig == null || trig == "" ) {
				trig = "now"
			} 
			def trigger = jobdsl.processCreateTrigger(trig, jobName)
			if(jobObject != null ) {
				jobfacade.scheduleJob(jobObject,trigger)
			}

		} catch (Exception ex) {
			ex.printStackTrace()
		}
	}

	public void removeJob(String jobFileName){
		try {
			def jobName 
			new File(jobFileName).getText().eachLine{ 
				if(it.trim().replace(" ","").contains("JOB=")){
					jobName = shell.evaluate(it.split("=")[1])["name"]
					def jobRemove = jobfacade.getJob(jobName)
					if(jobRemove != null) {
						jobfacade.removeJob(jobRemove)
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace()
		}
	}
	class JFServiceTracker implements ServiceTrackerCustomizer {
	public Object addingService(ServiceReference reference) {
		def oservice = context.getService(reference);
		if (oservice instanceof JobFacade) {
			jobfacade = oservice
		}
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

	}
}
class JobDslServiceTracker implements ServiceTrackerCustomizer {
	public Object addingService(ServiceReference reference) {
		def oservice = context.getService(reference);
		jobdsl = oservice
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
	}
}

}
