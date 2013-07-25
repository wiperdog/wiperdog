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
import com.insight_tec.pi.jobmanager.internal.JobFacadeImpl
import com.insight_tec.pi.jobmanager.JobFacade
import com.insight_tec.pi.jobmanager.Constants;
import com.insight_tec.pi.jobmanager.JobClass;
import com.insight_tec.pi.jobmanager.JobResult;
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
				jobLoader_obj.isManager = "false"
				jobLoader_obj.isAgent = "true"
			
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