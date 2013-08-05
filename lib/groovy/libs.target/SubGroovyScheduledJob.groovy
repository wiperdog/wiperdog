import com.insight_tec.pi.logging.*
import org.quartz.JobDataMap;

class SubGroovyScheduledJob extends GroovyScheduledJob{
	public SubGroovyScheduledJob(fullPathOfFile, classOfJob){
		super(fullPathOfFile, classOfJob, null)
		classOfJob = classOfJob
		fullPathOfFile = fullPathOfFile
		fileName = new	File(this.fullPathOfFile).getName()
		properties = MonitorJobConfigLoader.getProperties()
	}
	public Object executeJob(){
		def objJob = getJobInstance()
		def binding = objJob.getBinding()
		// Get jobname
		def jobName = getJobName()		
		logger.debug("fileName: " + fileName + " ---START PROCESS JOB---")
		def PERSISTENTDATA_File = new File(properties.get(ResourceConstants.MONITORJOBDATA_DIRECTORY) + "/monitorjobdata/PersistentData/" + jobName + ".txt")
		def prevOUTPUT_File = new File(properties.get(ResourceConstants.MONITORJOBDATA_DIRECTORY) + "/monitorjobdata/PrevOUTPUT/" + jobName + ".txt")
		def lastExecution_File = new File(properties.get(ResourceConstants.MONITORJOBDATA_DIRECTORY) + "/monitorjobdata/LastExecution/" + jobName + ".txt")		

		// get JOBCALLER if exist
		// at this point, handling JOBCALLER is not imp.emented yet.
		def rv = null
		try {
			// call Process to load and store Data
			initMonitoringJobData(binding, PERSISTENTDATA_File, prevOUTPUT_File, lastExecution_File);
			logger.debug("fileName: " + fileName + " ---Start Execute Job---")
			def jobCaller = new SubDefaultJobCaller(objJob, fileName, vJob[ResourceConstants.DEF_JOB_NAME])
			rv = jobCaller.start(null)
			isJobFinishedSuccessfully = jobCaller.isJobFinishedSuccessfully
			logger.debug("fileName: " + fileName + " ---Finish Execute Job---")
			// call Write Data Monitoring to File
			finishMonitoringJobData(binding, PERSISTENTDATA_File, prevOUTPUT_File, lastExecution_File);
		} catch (Exception e) {
			logger.debug(e.getMessage())
			if(e instanceof JobControlException){
				throw e
			}	
		}
		logger.debug("fileName: " + fileName + " ---FINISH PROCESS JOB---")
		return rv;
	}
	
}
