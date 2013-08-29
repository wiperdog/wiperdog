import org.wiperdog.jobmanager.JobExecutable
import org.quartz.JobDataMap
import groovy.lang.GroovyShell
import java.io.*
import groovy.json.*
import org.codehaus.jackson.*
import org.apache.log4j.Logger;

/**
 *
 */
class GroovyScheduledJob implements JobExecutable {
	String argumentString
	String fullPathOfFile
	String fileName

	def classOfJob
	def jobCaller	
	def vJob = [:]
	
	def persistentDataMap = [:]
	
	def prevOUTPUTMap = [:]
	
	def lastExecutionMap = [:]			
	def properties
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")

	def paramsInstances = null
	def rootJobName = null
	def instanceName = null
	
	DefaultSender sender
	List<Sender> senderList = new ArrayList<Sender>()
	
	protected boolean isJobFinishedSuccessfully = true

	/**
	 * constructor
	 */
	GroovyScheduledJob(fullPathOfFile, classOfJob, DefaultSender sender) {
		this.classOfJob = classOfJob
		this.fullPathOfFile = fullPathOfFile
		this.fileName = new	File(this.fullPathOfFile).getName()
		properties = MonitorJobConfigLoader.getProperties()
		this.sender = sender
	}
	
	GroovyScheduledJob(fullPathOfFile, classOfJob, paramsInstances, rootJobName, instanceName, DefaultSender sender) {
		this.classOfJob = classOfJob		
		this.fullPathOfFile = fullPathOfFile
		this.fileName = new	File(this.fullPathOfFile).getName()	
		this.paramsInstances = paramsInstances
		this.rootJobName = rootJobName
		this.instanceName = instanceName
		properties = MonitorJobConfigLoader.getProperties()
		this.sender = sender
	}

	Object getJobInstance() {	
		def shell = new GroovyShell()
		def binding
		def jobName		
		def jobFileName = (new File(this.fullPathOfFile)).getName().replaceFirst(~/\.[^\.]+$/, '')
		def params = []
		
		//Get instance job name
		def instJobName = null
		if ((this.rootJobName != null) && (this.instanceName != null)) {
			instJobName = this.rootJobName + "_" + this.instanceName
		}
		
		//Get job name		
		if ( vJob[ResourceConstants.DEF_JOB_NAME] == null ) {
			//def jobFileFirstLine
			def JOBvariable = null
			//read into file to get job name
			new File(this.fullPathOfFile).eachLine { 
				if(it.trim().contains("JOB=") || it.trim().contains("JOB =")) {
					JOBvariable = it	
					return ;
				}	
			}
			//2012-11-19 Luvina update end
			def jobParams = null
				if(JOBvariable != null){
					jobParams = shell.evaluate(JOBvariable)
				}
			if ( jobParams != null && jobParams["name"] != null  ) {
				jobName = jobParams["name"]
			} else {
				jobName = jobFileName
			}
		} else {
			jobName = vJob[ResourceConstants.DEF_JOB_NAME]
		}
		// ------Create instance-------
		def o = classOfJob.newInstance()
		// initialize variables
		binding = o.getBinding()
		params = loadParams(jobName, instJobName, paramsInstances, properties)
		binding.setVariable('parameters', params)
		o.run()
		if (binding.hasVariable(ResourceConstants.DEF_JOB)) {
			vJob = binding.getVariable(ResourceConstants.DEF_JOB)
			if ((this.rootJobName == null) && (vJob[ResourceConstants.DEF_JOB_NAME] != null)) {
				this.rootJobName = vJob[ResourceConstants.DEF_JOB_NAME]
			}
		} else {
			if(instJobName != null) {
				vJob[ResourceConstants.DEF_JOB_NAME] = instJobName
			} else {
				vJob[ResourceConstants.DEF_JOB_NAME] = jobFileName
				this.rootJobName = jobFileName
			}
		}
		return o
	}

	def getJOBDefinition() {
		//Get instance job name
		def instJobName = null
		if ((this.rootJobName != null) && (this.instanceName != null)) {
			instJobName = this.rootJobName + "_" + this.instanceName
		}
		
		if (vJob ==[:]) {
		def jobFileName = (new File(this.fullPathOfFile)).getName().replaceFirst(~/\.[^\.]+$/, '')
			def oJob = getJobInstance()
			def binding = oJob.getBinding()
			if (binding.hasVariable(ResourceConstants.DEF_JOB)) {
				vJob = binding.getVariable(ResourceConstants.DEF_JOB)
				if( vJob[ResourceConstants.DEF_JOB_NAME] == null ){
					vJob[ResourceConstants.DEF_JOB_NAME] = jobFileName
				}
			} else {
				// このjob定義ファイルは JOB 変数を持たない。
				// こういうときは、name だけを持つJOB変数があるものとして振る舞う。
				if(instJobName != null) {
					vJob[ResourceConstants.DEF_JOB_NAME] = instJobName
				} else {
					vJob[ResourceConstants.DEF_JOB_NAME] = jobFileName			
				}
			}
		}
	}

	String getJobClassName() {
		getJOBDefinition()
		return vJob[ResourceConstants.DEF_JOB_CLASS]
	}

	String getJobName() {
		getJOBDefinition()
		return vJob[ResourceConstants.DEF_JOB_NAME]
	}

	long getMaxRuntime() {
		getJOBDefinition()
		return Long.parseLong(vJob[ResourceConstants.DEF_JOB_MAXRUN] != null ? vJob[ResourceConstants.DEF_JOB_MAXRUN] : "-1")
	}

	long getMaxWaittime() {
		getJOBDefinition()
		return Long.parseLong(vJob[ResourceConstants.DEF_JOB_MAXWAIT] != null ? vJob[ResourceConstants.DEF_JOB_MAXWAIT] : "-1")
	}

	/**
	 * JobExecutable.execute
	 * @param params
	 * @return
	 */
	Object execute(JobDataMap params) throws InterruptedException {
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
			def jobCaller = new DefaultJobCaller(objJob,fileName, this.rootJobName, this.instanceName, this.sender)
			rv = jobCaller.start(null, senderList)
			isJobFinishedSuccessfully = jobCaller.isJobFinishedSuccessfully
			logger.debug("fileName: " + fileName + " ---Finish Execute Job---")
			// call Write Data Monitoring to File
			finishMonitoringJobData(binding, PERSISTENTDATA_File, prevOUTPUT_File, lastExecution_File);
		} catch (Exception e) {
			logger.debug(e.getMessage())
			isJobFinishedSuccessfully = false
		}
		logger.debug("fileName: " + fileName + " ---FINISH PROCESS JOB---")
		return rv;
	}

	/**
	 * JobExecutable.getName
	 * get name used to uniquely recognize this job.
	 * @return
	 */
	String getName() {
		return getJobName();
	}
	
	boolean getJobExecutedStatus(){
		return isJobFinishedSuccessfully
	}

	/**
	 * JobExecutable.getArgumentString
	 * 
	 * @return
	 */
	String getArgumentString() {
		// do nothing
		return null;
	}

	/**
	 * JobExecutable.stop
	 * stop execution immediately.
	 * @param thread
	 */
	void stop(Thread thread) {
		thread.interrupt()
	}
	
	/**
	 * Process to load and store Data
	 * @param binding Job's binding
	 * @param PERSISTENTDATA_File File's path of PERSISTENTDATA
	 * @param prevOUTPUT_File File's path of prevOUTPUT
	 * @param lastExecution_File File's path of lastExecution
	 */
	void initMonitoringJobData(binding, PERSISTENTDATA_File, prevOUTPUT_File, lastExecution_File){
		logger.debug("fileName: " + fileName + " ---Start Load Data Monitor of Last Execute---")
		def jobName = getJobName()
		// Process to load and store PERSISTENTDATA
		// Get PERSISTENTDATA based on jobName
		def PERSISTENTDATA = [:]
		PERSISTENTDATA = loadData(PERSISTENTDATA_File, persistentDataMap, jobName)
		binding.setVariable("PERSISTENTDATA", PERSISTENTDATA)
		//End process to load and store PERSISTENTDATA
		
		// Process to load and store prevoutput
		// Get PrevOUTPUT of current job and set variable into objJob
		def prevOUTPUT = [:] 
		prevOUTPUT = loadData(prevOUTPUT_File, prevOUTPUTMap, jobName)
		binding.setVariable('prevOUTPUT', prevOUTPUT)
		//End process to load and store prevoutput
		
		// Process to load and store lastExecution
		// Get lastexecution and interval of the current job
		def lastExecution = loadData(lastExecution_File, lastExecutionMap, jobName, true);
		binding.setVariable('lastexecution', lastExecution)
		//End process to load and store lastExecution
		logger.debug("fileName: " + fileName + " ---Finish Load Data Monitor of Last Execute---")
	}
	
	/**
	 * Write Data Monitoring to File
	 * @param binding Job's binding
	 * @param PERSISTENTDATA_File File's path of PERSISTENTDATA
	 * @param prevOUTPUT_File File's path of prevOUTPUT
	 * @param lastExecution_File File's path of lastExecution
	 */
	void finishMonitoringJobData(binding, PERSISTENTDATA_File, prevOUTPUT_File, lastExecution_File){
		logger.debug("fileName: " + fileName + " ---Start Write Data Monitoring to File---")
		def jobName = getJobName()
		def PERSISTENTDATA
		def prevOUTPUT
		def lastExecution
		// write data PERSISTENTDATA
		PERSISTENTDATA = binding.getVariable("PERSISTENTDATA")
		writeData(PERSISTENTDATA, persistentDataMap, PERSISTENTDATA_File, jobName)
		// write data prevOUTPUT
		prevOUTPUT = binding.getVariable("prevOUTPUT")
		writeData(prevOUTPUT, prevOUTPUTMap, prevOUTPUT_File, jobName)
		// write data lastExecution
		lastExecution = binding.getVariable("lastexecution")
		writeData(lastExecution, lastExecutionMap, lastExecution_File, jobName)
		logger.debug("fileName: " + fileName + " ---End Write Data Monitoring to File---")
	}
	
	/**
	 * Load data PERSISTENTDATA and PrevOUTPUT
	 * @param dataFile File's path to store data
	 * @param dataMap DataMap to store data on memory
	 * @param jobName Job's name
	 * @return Data was read from file and set into dataMap
	 */
	def loadData(dataFile, dataMap, jobName, isLastexecution = false) {
		def slurper = new JsonSlurper()
		def data = [:]
		data = dataMap[jobName]
		if (data == null){
			// check data file exist
			if (dataFile.exists()) {
				// if don't have Lastexecution
				if(!isLastexecution){
					// Get data of current job and set variable into objJob
					def line = dataFile.getText()
					if(!line.isEmpty()) {
						data = slurper.parseText(line)
						dataMap[jobName] = data
					}
				} else {
					// have Lastexecution, Get lastexecution and interval of the current job
					def line = null
					dataFile.withReader { line = it.readLine() }
					if(!line.isEmpty()) {
						data = Integer.parseInt(line)
						dataMap[jobName] = data
					}
				}
			}else if(!isLastexecution) {
				data = [:]
			}
		} 
		return data
	}
	
	/**
	 * Get data after processing and Set data of current Job
	 * @param data Data to write into file 
	 * @param dataMap DataMap to store data on memory
	 * @param dataFile File's path
	 * @param jobName Job's name
	 */
	void writeData(data, dataMap, dataFile, jobName){
		def jsonoutput = new JsonOutput()
		def outputFile
		dataMap[jobName] = data
		
		// datatype is Long when data is lastexecution
		// write to file all the time
		// return to prevent exception at getSize(). (Long doesn't have getSize method)
		if(data instanceof Long || data instanceof Integer){
			outputFile = jsonoutput.toJson(data)
			dataFile.setText(outputFile)
			return;
		}
		// process when data is prevOUTPUT or PERSISTENTDATA
		// Set Data of current job
		if(dataMap[jobName] != null && dataMap[jobName].size() != 0) {
			outputFile = jsonoutput.toJson(data)
			dataFile.setText(outputFile)
		}
	}
	
	/**
	 * load parameters
	 * @param jobName Job's name
	 * @param instJobName instancesJob's name
	 * @param paramsInstances instance's params
	 * @param properties properties
	 * @return params
	 */
	public loadParams(jobName, instJobName, paramsInstances, properties){
		def shell = new GroovyShell()
		def fileParams
		def instFileParams
		def params = []
		//default params file
		fileParams = new File(properties.get(ResourceConstants.DEFAULT_PARAMETERS_DIRECTORY) + "/default.params")
		if (fileParams.exists()) {
			params = shell.evaluate(fileParams)
		}
		// file storing parameters
		fileParams = new File(properties.get(ResourceConstants.JOB_PARAMETERS_DIRECTORY) + "/" + jobName +  ".params")
		if(fileParams.exists()){
			def paramsJob
			paramsJob = shell.evaluate(fileParams)
			paramsJob.each {
				params[it.key] = it.value
			}
		}
		// load name instance
		if(instJobName != null) {
			instFileParams = new File(properties.get(ResourceConstants.JOB_PARAMETERS_DIRECTORY) + "/" + instJobName +  ".params")
			if(instFileParams.exists()){
				def paramsInst
				paramsInst = shell.evaluate(instFileParams)
				paramsInst.each {
					params[it.key] = it.value
				}
			}
		}
		
		if(paramsInstances != null) {
			paramsInstances.each {
				params[it.key] = it.value
			}
		}
		return params;
	}
}

