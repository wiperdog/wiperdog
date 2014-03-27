import java.io.*
import java.nio.charset.Charset
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessGenJob {
	static final String CHARSET = 'utf-8'
	static final List listKey = ["JOB", "GROUPKEY", "QUERY", "QUERY_VARIABLE", "DBEXEC", "DBEXEC_VARIABLE", "COMMAND", "FORMAT", "FETCHACTION", "ACCUMULATE", "FINALLY", "KEYEXPR", "KEYEXPR._root", "KEYEXPR._sequence", "KEYEXPR._unit", "KEYEXPR._chart", "KEYEXPR._description", "SENDTYPE", "RESOURCEID", "MONITORINGTYPE", "DBTYPE", "DEST", "HOSTID", "SID"]
	static final Map mapKeyInput = ["-n": "JOBNAME", "-f": "FETCHACTION", "-q": "QUERY", "-c": "COMMAND", "-d": "DBEXEC"]
	static final Map mapDefaultValue = ["JOBNAME": "\"\"", "FETCHACTION": "/*code FETCHACTION here*/", "QUERY": "\"\"", "COMMAND": "\"\"", "DBEXEC": "\"\""]

	/**
	 * main: main process
	 * @param args: data receive from batch/bash file
	*/
	public static void main(String[] args) throws Exception {
		// Process Job File
		def filePath = System.getProperty("user.dir")
		def checkDefaultPath = true
		def listProcessKey = []	
		listProcessKey.add("-fp")	
		
		mapKeyInput.each { key, value ->
			listProcessKey.add(key) 
		}

		args.eachWithIndex {item, index ->
			if ((index < (args.size() - 1)) && (item == "-fp") && (args[index+1] != null) && (!listProcessKey.contains(args[index+1]))) {
				filePath = args[index+1]
				checkDefaultPath = false
				if (!(new File(filePath).exists())) {
					new File(filePath).mkdirs()
				}
			}
		}
		filePath += "/"
		String fileName = ""
		if (args.size() > 1 && !args[1].contains("-") && args[1].trim() != "") {
			if (args[1].trim().contains(".job")) {
				fileName = args[1].trim()
			} else {
				fileName = "${args[1].trim()}.job"
			}
		} else {
			println "Incorrect format !!!"
			println "Correct format of command: "
			println "genjob -n <jobName> [-f <strFetchAction>] [-q <strQuery>] [-c <strCommand>] [-d <strDbExec>] [-fp <pathToFile>]"
			return
		}

		// Declare map jobData
		def jobData = [:]
		args.eachWithIndex {item, index ->
			if (mapKeyInput[item] != null) {
				//If data in mapKeyInput and next data not in mapKeyInput, get this next data to value
				if (index < args.size() - 1 && (args[index+1] != null && args[index+1] != "" && !args[index+1].contains("-")) && (mapKeyInput[args[index+1]] == null)) {
					jobData[mapKeyInput[item]] = args[index+1]
				} else {
					jobData[mapKeyInput[item]] = mapDefaultValue[mapKeyInput[item]]
				}
			}
		}

		// Check job file exists to process create/update
		if (new File(filePath + fileName).exists()) { // Update job
			if (updateDataToJobFile(filePath, fileName, jobData)) {
				println ">>>>>>>>>> [SUCCESS] JOB {$filePath$jobData.JOBNAME} WAS UPDATED <<<<<<<<<<"
			} else {
				println ">>>>>>>>>> [FAILURE] CAN NOT UPDATE JOB <<<<<<<<<<"
			}
		} else { // Create job
			if (writeDataToJobFile(filePath, fileName, jobData)) {
				println ">>>>>>>>>> [SUCCESS] JOB {$jobData.JOBNAME} WAS CREATED IN FOLDER {$filePath} <<<<<<<<<<"
				if (checkDefaultPath) {
					println "Note: To specify the folder contains job, you can use \"-fp\" command !"
				}
			} else {
				println ">>>>>>>>>> [FAILURE] CAN NOT CREATE JOB <<<<<<<<<<"
			}
		}
	}

	/**
	 * updateDataToJobFile: process update job if it exits
	 * @param filePath: path to file
	 * @param fileName: job name
	 * @param jobData: data fill to job
	 * @return true/false
	*/
	public static boolean updateDataToJobFile(filePath, fileName, jobData){
		def oldData = getDataJob(filePath, fileName)
		def newData = oldData.clone()
		jobData.each {key, value ->
			if (oldData["//" + key] != null) {
				newData.remove("//" + key)
			}
			newData.put(key,value)
		}

		if (!writeDataToJobFile(filePath, fileName, newData)) {
			return false
		} else {
			return true
		}
	}

	/**
	 * getDataJob: process get data from job (exists)
	 * @param filePath: path to file
	 * @param fileName: job name
	 * @return mapResult
	*/
	public static getDataJob(filePath, fileName) {
		def jobFile = new File(filePath + fileName)
		def stringOfJob = jobFile.getText()
		def strKeyPattern = ""
		listKey.each {key->
			strKeyPattern += key + "|"
			strKeyPattern += "//" + key + "|"
		}
		strKeyPattern = strKeyPattern.subSequence(0, strKeyPattern.length() - 1)
		//Create macher
		String macherPattern = "("+ strKeyPattern + ")([ ]*=[ ]*)((?:(?!(" + strKeyPattern + ")([ ]*=[ ]*)).)*)"

		Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(stringOfJob);

		def mapResult = [:]
		def shell = new GroovyShell()
		def temp
		while(matcher.find())
		{
			mapResult[matcher.group(1)] = matcher.group(3).trim()
		}
		return mapResult
	}
	
	/**
	 * writeDataToJobFile: process create job if it's not exits
	 * @param filePath: path to file
	 * @param fileName: job name
	 * @param jobData: data fill to job
	 * @return true/false
	*/
	public static boolean writeDataToJobFile(filePath, fileName, jobData){
		def jobStr = ""

		// process JOB variable
		def mapJOB = [:]
		mapJOB['name'] = "\"" + jobData.JOBNAME.replace(".job", "") + "\"" + "/*, jobclass: Job class name here*/"
		jobStr += "JOB = " + mapJOB.toString() + "\n"
		
		// process QUERY variable
		if (jobData.QUERY != null) {
			def queryStr = ""
			if(jobData.QUERY.substring(0,1) == "\""
				|| jobData.QUERY.substring(0,1) == "\'"
				|| jobData.QUERY.substring(0,3) == "\'''"
				|| jobData.QUERY.substring(0,3) == "\"\"\"" ) {
					queryStr = jobData.QUERY + "\n"
			} else {
				queryStr = "\'''" + jobData.QUERY + "'''\n"
			}
			jobStr += "QUERY = " + queryStr
		} else {
			jobStr += "//QUERY = \"\" // alternative to FETCHACTION, write your query here. \n"
		}
		
		// process queryVariable variable
		jobStr += "//QUERY_VARIABLE = // Variable to binding. \n"

		// process dbExec variable
		if (jobData.DBEXEC != null) {
			def dbExecStr = ""
			if (jobData.DBEXEC.substring(0,1) == "\""
				|| jobData.DBEXEC.substring(0,1) == "\'"
				|| jobData.DBEXEC.substring(0,3) == "\'''"
				|| jobData.DBEXEC.substring(0,3) == "\"\"\"" ) {
					dbExecStr = jobData.DBEXEC + "\n"
			} else {
				dbExecStr = "\'''" + jobData.DBEXEC + "'''\n"
			}
			jobStr += "DBEXEC = " + dbExecStr
		} else {
			jobStr += "//DBEXEC = /*fill DBEXEC here*/\n"
		}
		
		// process dbExecVariable variable
		jobStr += "//DBEXEC_VARIABLE = /*fill DBEXEC_VARIABLE here*/\n"
		
		// process command variable
		if (jobData.COMMAND != null) {
			def dbcommandStr = ""
			if(jobData.COMMAND.substring(0,1) == "\""
				|| jobData.COMMAND.substring(0,1) == "\'"
				|| jobData.COMMAND.substring(0,3) == "\'''"
				|| jobData.COMMAND.substring(0,3) == "\"\"\"" ){
					dbcommandStr = jobData.COMMAND + "\n"
			} else{
				dbcommandStr = "\'''" + jobData.COMMAND + "'''\n"
			}
			jobStr += "COMMAND = " + dbcommandStr
		} else {
			jobStr += "//COMMAND = /*fill COMMAND here*/\n"
		}
		
		// process format variable
		jobStr += "//FORMAT = /*fill FORMAT here*/\n"
		
		// process FETCHACTION variable
		if (jobData.FETCHACTION != null) {
			//if (jobData.FETCHACTION)
			def startFetch = jobData.FETCHACTION[0]
			def endFetch = jobData.FETCHACTION[jobData.FETCHACTION.size() - 1]
			if (startFetch != "{" && endFetch != "}") {
				jobStr += "FETCHACTION = {\n\t" + jobData.FETCHACTION + "\n}\n"
			} else {
				jobStr += "FETCHACTION = " + jobData.FETCHACTION + "\n"
			}
		} else {
			jobStr += "//FETCHACTION = {\n\t/*code FETCHACTION here*/\n//}\n"
		}
		
		// process accumulate variable
		jobStr += "//ACCUMULATE = {\n\t/*code ACCUMULATE here*/\n//}\n"

		// process GROUPKEY variable
		jobStr += "//GROUPKEY = /*group key here*/\n"
		
		// process finally variable
		jobStr += "//FINALLY = {\n\t/*code FINALLY here*/\n//}\n"
		
		// process KEYEXPR variable
		jobStr += "//KEYEXPR = [:] /*Map config for KEYEXPR*/\n"
		
		// process SENDTYPE variable
		jobStr += "//SENDTYPE = /*Config for SENDTYPE*/\n"
		
		// process resourceId variable
		jobStr += "//RESOURCEID = /*Config for RESOURCEID*/\n"
		
		// process MONITORINGTYPE variable
		jobStr += "//MONITORINGTYPE = /*Config for MONITORINGTYPE*/\n"
		
		// process DBTYPE variable
		jobStr += "//DBTYPE = /*Config for DBTYPE*/\n"
		
		// process dest variable
		jobStr += "DEST = parameters.dest\n"
		
		// Set Job's String into file
		if (!writeToFile(filePath, fileName, jobStr)) {
			return false
		} else {
			return true
		}
	}
	
	/**
	 * writeToFile: Write data to file with CHARSET encode
	 * @param filePath: path to file
	 * @param fileName: job name
	 * @param data: data fill to job
	 * @return true: if write success/false: if write failure
	 */
	public static boolean writeToFile(filePath, fileName, data) {
		try {
			def dataFile = new File(filePath, fileName);
			dataFile.write(data, CHARSET)
			return true
		} catch (Exception ex) {
			println "[ERROR]writeToFile: " + ex
			return false
		}
	}
}