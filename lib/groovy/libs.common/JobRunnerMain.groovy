import org.wiperdog.jobmanager.internal.JobFacadeImpl
import org.wiperdog.jobmanager.JobFacade
import org.wiperdog.jobmanager.Constants;
import org.wiperdog.jobmanager.JobClass;
import org.wiperdog.jobmanager.JobResult;
import org.osgi.framework.BundleContext
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;

public class JobRunnerMain{
	GroovyShell shell;
	def binding
	SchedulerFactory sf;
	Scheduler scheduler;
	def context
	JobFacade jobfacade
	TriggerKey trgKey = null
	
	public JobRunnerMain(shell, context){
		this.shell = shell
		binding = shell.getContext()
		this.context = context
	}
	
	public void executeJob(String jobFileName, String trig){
		try {
			// Initialize jobfacade
			sf = new StdSchedulerFactory();
			scheduler = sf.getScheduler();
			PropertySettingJobFactory jfactory = new PropertySettingJobFactory();
			jfactory.setWarnIfPropertyNotFound(false);
			scheduler.setJobFactory(jfactory);
			scheduler.start();
			jobfacade = new JobFacadeImpl(scheduler);
			
			// Init JobDsl
			Class jobDslClass = shell.getClassLoader().loadClass('JobDsl');
			Object jobDsl_obj = jobDslClass.newInstance(shell, jobfacade, context)

			Class jobLoaderClass = shell.getClassLoader().loadClass('JobLoader');
			Object jobLoader_obj = jobLoaderClass.newInstance(context, shell)
				jobLoader_obj.jobdsl = jobDsl_obj
			// Process job file
			// def jobFile = new File(binding.getVariable('felix_home') + "/" + jobFileName)

			def jobFile = new File(jobFileName)
			if(!jobFile.isAbsolute()){
				jobFile = new File(binding.getVariable('felix_home') + "/" + jobFileName)
			}
			if( jobFile.exists() ) {
	
				jobLoader_obj.processFile(jobFile)
				// Get job name and schedule job
				def JOBvariable = null
				jobFile.eachLine { 
						if(it.trim().contains("JOB=") || it.trim().contains("JOB =")) {
							JOBvariable = it
							return ;
						}
				}
				def jobParams = null
				def jobName = null
				if(JOBvariable != null){
					jobParams = shell.evaluate(JOBvariable)
					if(jobParams != null && jobParams['name'] != null){
						jobName = jobParams['name']
					}
				}else{
					def fileName = jobFile.getName()
					jobName = fileName.substring(0, fileName.indexOf('.job'))
				}
				if(jobName != null){
					Trigger trigger = null
					if(trig != null){
						if (trig ==~ /[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+/ ||
							trig ==~ /[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+[ ]+[^ ]+/ ) {
							// create trigger.
							// created trigger will be registered automatically.
							trigger = jobfacade.createTrigger(jobName, trig)
						} else if (trig == "now" || trig == "NOW") {
							// create trigger.
							// created trigger will be registered automatically.
							trigger = jobfacade.createTrigger(jobName, 0)
						} else if (trig.endsWith('i')) {
							long interval = Long.parseLong(trig.substring(0,trig.lastIndexOf('i')))*1000
							trigger = jobfacade.createTrigger(jobName, 0, interval)
						}else if (trig == 'delete'){
							trigger = jobfacade.getTrigger(jobName)
							jobfacade.unscheduleJob(trigger)							
						}else {
							long delay = Long.parseLong(trig)
							trigger = jobfacade.createTrigger(jobName, delay)
						}
					}else{
						trigger = jobfacade.createTrigger(jobName, 0)
					}
					if(trigger != null){
						trgKey = trigger.getKey()
						def job = jobfacade.getJob(jobName)
						jobfacade.scheduleJob(job, trigger);
					}
				}
			} else {
				println "--------- File " + jobFile + " not found ! ---------" 
				return
			}
		} catch (Exception ex) {
			println ex
		}
	}
}