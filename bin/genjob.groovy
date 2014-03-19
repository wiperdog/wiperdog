import java.io.*
import java.nio.charset.Charset
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessGenJob {
	static final String CHARSET = 'utf-8'
	static final List listKey = ["JOB", "GROUPKEY", "QUERY", "QUERY_VARIABLE", "DBEXEC", "DBEXEC_VARIABLE", "COMMAND", "FORMAT", "FETCHACTION", "ACCUMULATE", "FINALLY", "KEYEXPR", "KEYEXPR._root", "KEYEXPR._sequence", "KEYEXPR._unit", "KEYEXPR._chart", "KEYEXPR._description", "SENDTYPE", "RESOURCEID", "MONITORINGTYPE", "DBTYPE", "DEST", "HOSTID", "SID"]
	static final Map mapKeyInput = ["--n": "JOBNAME", "--f": "FETCHACTION", "--q": "QUERY", "--c": "COMMAND", "--d": "DBEXEC"]
	static final Map mapDefaultValue = ["JOBNAME": "\"\"", "FETCHACTION": "{}", "QUERY": "\"\"", "COMMAND": "\"\"", "DBEXEC": "\"\""]

	/**
	 * main: 
	 * @param args: 
	*/
	public static void main(String[] args) throws Exception {
		println "==================="
		println args
		println "==================="
		// Process Job File
		def filePath = System.getProperty("user.dir")
		args.eachWithIndex {item, index ->
			if (index < args.size() - 1 && item == "--fp" && args[index+1] != null && !args[index+1].contains("--")) {
				filePath = args[index+1]
			}
		}
		filePath += "/"
		String fileName = ""
		if (!args[1].contains("--")) {
			fileName = "${args[1].trim()}.job"
		} else {
			println "Incorrect format !!!"
			println "Correct format of command: "
			println "genjob --n <jobName> [--f <strFetchAction>] [--q <strQuery>] [--c <strCommand>] [--d <strDbExec>] [--fp <pathToFile>]"
			return
		}

		// Declare map jobData
		def jobData = [:]
		args.eachWithIndex {item, index ->
			if (mapKeyInput[item] != null) {
				//If data in mapKeyInput and next data not in mapKeyInput, get this next data to value
				if ((args[index+1] != null && !args[index+1].contains("--")) && (mapKeyInput[args[index+1]] == null)) {
					jobData[mapKeyInput[item]] = args[index+1]
				} else {
					jobData[mapKeyInput[item]] = mapDefaultValue[mapKeyInput[item]]
				}
			}
		}

		// Check job file exists to process create/update
		if (new File(filePath + fileName).exists()) { // Update job
			if (updateDataToJobFile(filePath, fileName, jobData)) {
				println ">>>>>>>>>> UPDATE JOB {$jobData.JOBNAME} SUCCESS <<<<<<<<<<"
			} else {
				println ">>>>>>>>>> CAN NOT UPDATE JOB, PLEASE PUT AGAIN <<<<<<<<<<"
			}
		} else { // Create job
			if (writeDataToJobFile(filePath, fileName, jobData)) {
				println ">>>>>>>>>> CREATE JOB {$jobData.JOBNAME} SUCCESS <<<<<<<<<<"
			} else {
				println ">>>>>>>>>> CAN NOT CREATE JOB, PLEASE PUT AGAIN <<<<<<<<<<"
			}
		}
	}

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
	
	public static boolean writeDataToJobFile(filePath, fileName, jobData){
		def jobStr = ""

		// process JOB variable
		def mapJOB = [:]
		mapJOB['name'] = "\"" + jobData.JOBNAME + "\""
		mapJOB['jobclass'] = "/*Job class name here*/"
		jobStr += "JOB = " + mapJOB.toString() + "\n"
		
		// process GROUPKEY variable
		jobStr += "//GROUPKEY = /*group key here*/\n"
		
		// process QUERY variable
		if (jobData.QUERY != null && jobData.QUERY != "") {
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
		if (jobData.DBEXEC != null && jobData.DBEXEC != "") {
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
		if (jobData.COMMAND != null && jobData.COMMAND != "") {
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
		if(jobData.FETCHACTION != null && jobData.FETCHACTION != ""){
			jobStr += "FETCHACTION = " + jobData.FETCHACTION + "\n"
		} else {
			jobStr += "//FETCHACTION = {\n\t/*code FETCHACTION here*/\n//}\n"
		}
		
		// process accumulate variable
		jobStr += "//ACCUMULATE = {\n\t/*code ACCUMULATE here*/\n//}\n"
		
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
		jobStr += "//DEST = parameters.dest\n"
		
		// Set Job's String into file
		if (!writeToFile(filePath, fileName, jobStr)) {
			return false
		} else {
			return true
		}
	}
	
	/**
	 * Write data to file with CHARSET encode
	 * @param paramFile
	 * @return true/false
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