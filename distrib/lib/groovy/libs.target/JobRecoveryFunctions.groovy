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
import org.apache.log4j.Logger;
class JobRecoveryFunctions {
	public static JobRecoveryFunctions instance = new JobRecoveryFunctions()
	SubGroovyScheduledJob scheduledJob
	def properties = MonitorJobConfigLoader.getProperties()
	def logger = Logger.getLogger("com.insight_tec.pi.scriptsupport.groovyrunner")
	
	public JobRecoveryFunctions(){
		
	}
	
	public static JobRecoveryFunctions getInstance(){
		if(this.instance == null){
			this.instance = new JobRecoveryFunctions()
		}
		return instance
	}
	
	public static void doBind(binding){
		instance.onFail.delegate = binding
		binding.setVariable('onFail', instance.onFail)
		instance.controlAssert.delegate = binding
		binding.setVariable('controlAssert', instance.controlAssert)
	}
	
	def onFail = {firstJob, recoveryJob->
		// Execute firstjob
		// Catch JobControlException
		def isSuccess = true
		GroovyShell shell = new GroovyShell()
		def loader = shell.getClassLoader()
		loader.clearCache()
		def resultData
		resultData = executeJob(loader, firstJob ,isSuccess)
		isSuccess = resultData.status
		if(!isSuccess){
			resultData = executeJob(loader, recoveryJob, isSuccess)
		}
		return resultData.data
	}
	
	def controlAssert = {jobName, condition, message->
		if(!condition){
			throw new JobControlException(jobName, message)
		}
	}
	
	def executeJob = {loader, jobname, isSuccess ->
		try {
			File jobfile = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/${jobname}.job")
			def clsJob = loader.parseClass(jobfile)
			scheduledJob = new SubGroovyScheduledJob(jobfile.absolutePath, clsJob)
			def jobName = scheduledJob.getJobName()
			def jobClassName = scheduledJob.getJobClassName()
			def data = scheduledJob.executeJob()
			isSuccess = scheduledJob.getJobExecutedStatus()
			return [data:data, status:isSuccess]
		} catch (Exception ex) {
			if(ex instanceof JobControlException){
				isSuccess = true
				logger.debug(ex.getExceptionMessage())
			}else{
				isSuccess = false
			}
			return [data:null, status:isSuccess]
		}
	}
}
