import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.json.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder

public class JobDeclared extends HttpServlet {
	static final String JOB_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOB_DIRECTORY)
	static final String HOMEPATH = System.getProperty("felix.home")
	static final String INST_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOBINST_DIRECTORY)
	
	static final String PARAMFILE = "var/conf/default.params"
	static final List listKey = ["JOB", "GROUPKEY", "QUERY", "QUERY_VARIABLE", "DBEXEC", "DBEXEC_VARIABLE", "COMMAND", "FORMAT", "FETCHACTION", "ACCUMULATE", "FINALLY", "KEYEXPR", "KEYEXPR._root", "KEYEXPR._sequence", "KEYEXPR._unit", "KEYEXPR._chart", "KEYEXPR._description", "SENDTYPE", "RESOURCEID", "MONITORINGTYPE", "DBTYPE", "OSINFO", "DEST", "HOSTID", "SID"]
	static final String CHARSET = 'utf-8'
	def errorMsg = ""

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		def list_job = []
		def builder
		def message
		errorMsg = ""
		def job_dir = new File(JOB_DIR)
		try{
			def getListHeader = {
				def listHeaderJob = []
				def config_file = new File(MonitorJobConfigLoader.getProperties().get(ResourceConstants.USE_FOR_XWIKI))
				def config_info = (new GroovyShell()).evaluate(config_file)
				listHeaderJob = config_info['DbType'].keySet()
				return listHeaderJob
			}
			
			def listHeaderJob = getListHeader()
			def strMorType = req.getParameter("morType")
			
			def checkInOthersGroup = { fileName , list ->
				def result = true
				list.find{
					if(fileName.startsWith(it)){
						result = false
						return
					}
				}
				return result
			}
			
			if(strMorType != null && strMorType != ""){
				if(strMorType == "@DB"){
					list_job = []
				} else if (strMorType != "Others"){
					def groupMonitoringType = strMorType.replace("@","")
					job_dir.listFiles().each{
						def fileName = it.getName()
						if((fileName.endsWith('.job') && fileName.startsWith(groupMonitoringType))) {
							list_job.add(fileName.substring(0,fileName.lastIndexOf('.job')))
						}
					}
				} else {
					job_dir.listFiles().each{
						def fileName = it.getName()
						if(fileName.endsWith('.job') && checkInOthersGroup(fileName,listHeaderJob)) {
							list_job.add(fileName.substring(0,fileName.lastIndexOf('.job')))
						}
					}
				}
				
				def result = [:]
				result['listJob'] = list_job
				builder = new JsonBuilder(result)
				out.println(builder.toPrettyString());
				return
			}
			def strDBType = req.getParameter("dbtype")
            if (strDBType != null && strDBType != ""){
				if(job_dir.isDirectory()){
					job_dir.listFiles().each{
						def fileName = it.getName()
						if( fileName.startsWith(strDBType) && fileName.endsWith('.job')){
							list_job.add(fileName.substring(0,fileName.lastIndexOf('.job')))
						}
					}

					builder = new JsonBuilder(list_job)
					out.println(builder.toPrettyString());
				} else {
					errorMsg = "Error when get Job file: Job directory not found!"
				}
			} else {
				errorMsg = "Error when get Job file: No choice DBType!"
			}

			if (errorMsg != "") {
				message = [status:"failed", message:errorMsg]
				builder = new JsonBuilder(message)
				out.print(builder.toString())
			}
		} catch(Exception ex){
			errorMsg = "Error when get Job file: \n"
			errorMsg+= ex.getStackTrace();
			message = [status:"failed", message:errorMsg]
			builder = new JsonBuilder(message)
			out.print(builder.toString())
		}
	}

	@Override
	void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html")
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		def builder
		def message
		errorMsg = ""
		try {
			// Get JobFile
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
	      	def object = slurper.parseText(contentText)

	      	// command = Read -> Read job's file
	      	if(object.COMMAND == "Read"){
				def jobFileName = object.job
				def jobPath = JOB_DIR + "/" + jobFileName
				def jobFile = new File(jobPath + ".job")

				// resultRet returning result [Job:~~, instances:~~, params:~~]
				def resultRet = [:]
				// Get job's script
				def stringOfJob = getJobScript(jobFile)
				def realJobName = stringOfJob.JOB
				def filePath = JOB_DIR + "/" +  realJobName
				def instanceFile = new File(filePath + ".instances")
				def paramFile = new File(filePath + ".params")
				// Get instance file's script in Object type(Map)
				def instanceResult = getJobInstanceScript(instanceFile)
				// Get param file's script in Object type(Map)
				def paramResult = getJobParamScript(paramFile)
				// Set data into returning Result
				resultRet['Job'] = stringOfJob
				resultRet['instances'] = instanceResult
				resultRet['params'] = paramResult
				// Generate json String to response
				builder = new JsonBuilder(resultRet)
				out.print(builder.toString())
			}else if(object.COMMAND == "Write"){
				// command = Write -> Write job's file
				if(!(writeDataToJobFile(object) && writeDataToInstanceFile(object) && writeDataToParamFile(object))){
					errorMsg = "Error when post data: Fail to write files!"
				} else {
					message = [status:"OK", message:"Finish process successfully"]
					builder = new JsonBuilder(message)
					out.print(builder.toString())
				}
			}else{
				errorMsg = "Error when post data: Command is not valid!"
			}
			if (errorMsg != "") {
				message = [status:"failed", message:errorMsg]
				builder = new JsonBuilder(message)
				out.print(builder.toString())
			}
		}catch (Exception ex) {
			errorMsg = "Error when post data: " + ex
			errorMsg+= org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ex)
			message = [status:"failed", message:errorMsg]
			builder = new JsonBuilder(message)
			out.print(builder.toString())
		}
	}

	boolean writeDataToJobFile(data){
		def jobStr = ""
		def jobData = data.JOB
		if(jobData.jobName != null){
			// Process Job File
			String fileName
			if(jobData.jobFileName != null){
				String s = jobData.jobFileName
				if (jobData.jobFileName ==~ ".*\\.job") {
					fileName = JOB_DIR + "/${jobData.jobFileName}"
				} else {
					fileName = JOB_DIR + "/${jobData.jobFileName}.job"
				}
			}else{
				fileName = JOB_DIR + "/${jobData.jobName}.job"
			}

			// Process Comment
			if(jobData.commentForJob != null){
				jobStr += jobData.commentForJob + "\n"
			}

			// process JOB variable
			def mapJOB = [:]
			mapJOB['name'] = "\"" + jobData.jobName + "\""
			if(jobData.jobClassName != null){
				mapJOB['jobclass'] = "\"" + jobData.jobClassName + "\""
			}
			jobStr += "JOB = " + mapJOB.toString() + "\n"

			// process GROUPKEY variable
			if(jobData.groupKey != null){
				jobStr += "GROUPKEY = " + jobData.groupKey + "\n"
			}

			// process QUERY variable
			if(jobData.query != null){
				def queryStr = ""
				if(jobData.query.substring(0,1) == "\""
					|| jobData.query.substring(0,1) == "\'"
					|| jobData.query.substring(0,3) == "\'''"
					|| jobData.query.substring(0,3) == "\"\"\"" ){
						queryStr = jobData.query + "\n"
				}else{
					queryStr = "\'''" + jobData.query + "'''\n"
				}
				jobStr += "QUERY = " + queryStr
			}

			// process queryVariable variable
			if(jobData.queryVariable != null){
				jobStr += "QUERY_VARIABLE = " + jobData.queryVariable + "\n"
			}

			// process dbExec variable
			if(jobData.dbExec != null){
				def dbExecStr = ""
				if(jobData.dbExec.substring(0,1) == "\""
					|| jobData.dbExec.substring(0,1) == "\'"
					|| jobData.dbExec.substring(0,3) == "\'''"
					|| jobData.dbExec.substring(0,3) == "\"\"\"" ){
						dbExecStr = jobData.dbExec + "\n"
					}else{
						dbExecStr = "\'''" + jobData.dbExec + "'''\n"
					}
				jobStr += "DBEXEC = " + dbExecStr
			}

			// process dbExecVariable variable
			if(jobData.dbExecVariable != null){
				jobStr += "DBEXEC_VARIABLE = " + jobData.dbExecVariable + "\n"
			}

			// process command variable
			if(jobData.command != null){
				def dbcommandStr = ""
				if(jobData.command.substring(0,1) == "\""
					|| jobData.command.substring(0,1) == "\'"
					|| jobData.command.substring(0,3) == "\'''"
					|| jobData.command.substring(0,3) == "\"\"\"" ){
						dbcommandStr = jobData.command + "\n"
					}else{
						dbcommandStr = "\'''" + jobData.command + "'''\n"
					}
				jobStr += "COMMAND = " + dbcommandStr
			}

			// process format variable
			if(jobData.format != null){
				jobStr += "FORMAT = " + jobData.format + "\n"
			}

			// process fetchAction variable
			if(jobData.fetchAction != null){
				jobStr += "FETCHACTION = " + jobData.fetchAction + "\n"
			}

			// process accumulate variable
			if(jobData.accumulate != null){
				jobStr += "ACCUMULATE = " + jobData.accumulate + "\n"
			}

			// process finally variable
			if(jobData.finally != null){
				jobStr += "FINALLY = " + jobData.finally + "\n"
			}

			// process KEYEXPR variable
			if(jobData.KEYEXPR != null){
				def KEYEXPRData = getKeyExprData(jobData.KEYEXPR)

				if((KEYEXPRData.keyExpr != "[:]")
					|| (KEYEXPRData.keyExpr == "[:]" && (KEYEXPRData.keyExprUnit != "" || KEYEXPRData.keyExprChart != ""))){
					jobStr += "KEYEXPR = " + KEYEXPRData.keyExpr + "\n"
				}
				if(KEYEXPRData.keyExprUnit != "" && KEYEXPRData.keyExprUnit != "[[:]]"){
					jobStr += "KEYEXPR._unit = " + KEYEXPRData.keyExprUnit + "\n"
				}
				if(KEYEXPRData.keyExprChart != "" && KEYEXPRData.keyExprChart != "[]"){
					jobStr += "KEYEXPR._chart = " + KEYEXPRData.keyExprChart + "\n"
				}
			}

			// process SENDTYPE variable
			if(jobData.sendType != null && jobData.sendType != ""){
				jobStr += "SENDTYPE = \"" + jobData.sendType + "\"\n"
			}

			// process resourceId variable
			if(jobData.resourceId != null){
				jobStr += "RESOURCEID = \"" + jobData.resourceId + "\"\n"
			}

			// process MONITORINGTYPE variable
			if(jobData.monitoringType != null){
				jobStr += "MONITORINGTYPE = \"" + jobData.monitoringType + "\"\n"
			}

			// process DBTYPE variable
			println jobData.dbType
			if((jobData.dbType != null) && (jobData.dbType != "")){
			    def shell = new GroovyShell()
				def finalDBType = ""
				def dbmsInfoFile = new File(MonitorJobConfigLoader.getProperties().get(ResourceConstants.USE_FOR_XWIKI))
				def mapDbType = shell.evaluate(dbmsInfoFile.getText())['DbType']
				mapDbType.each {keyType, valType ->
					if(jobData.dbType == keyType) {
						finalDBType = valType
					}
				}
				jobStr += "DBTYPE = \"" + finalDBType + "\"\n"
			}
			
			// process OSINFO variable
			if(jobData.osInfo != null && jobData.osInfo != ""){
				jobStr += "OSINFO = " + jobData.osInfo + "\n"
			}else{
				if ((jobData.monitoringType != null) && (jobData.monitoringType == "@OS")) {
				    jobStr += "OSINFO = parameters.osinfo\n"
				}
			}
			
			// process dest variable
			if(jobData.dest != null && jobData.dest != ""){
				jobStr += "DEST = " + jobData.dest + "\n"
			}else{
				jobStr += "DEST = parameters.dest\n"
			}

			// Set Job's String into file
			writeToFile(fileName, jobStr)
		}else{
			println "Job's name is required!"
			return false
		}
		return true
	}

	boolean writeDataToInstanceFile(data){
		def jobData = data.JOB
		def instanceStr = ""
		def instanceElementStr = ""
		def instanceDataStr = ""
		if(jobData.jobName != null && jobData.jobName != ""){
			def instanceData = data.INSTANCES
			if(instanceData != null && instanceData != [:]){
				instanceData.each{key, value->
					if (instanceStr != "") {
						instanceStr += ", \n"
					}
					instanceElementStr = ""
					instanceDataStr = ""
					//Instance name
					instanceElementStr += "\"" + key + "\":["
					//Instance data
					if (value.schedule != null){
						instanceDataStr += "\"schedule\": \"" + value.schedule + "\""
					}
					if (value.params != null){
						if (instanceDataStr != "") {
							instanceDataStr += ", "
						}
						instanceDataStr += "\"params\":"
						instanceDataStr += value.params
					}
					instanceElementStr += instanceDataStr + "]"
					instanceStr += "\t" + instanceElementStr
				}
				instanceStr = "[\n" + instanceStr + "\n]"
				instanceStr= regularExpressionValidate(instanceStr)

				writeToFile(INST_DIR + "/${jobData.jobName}.instances", instanceStr)
			}else{
				File instFile = new File(INST_DIR + "/${jobData.jobName}.instances")
				if(instFile.exists()){
					return instFile.delete()
				}
			}
		}else{
			println "job'name is required!"
			return false
		}
		return true
	}

	boolean writeDataToParamFile(data){
		def jobData = data.JOB
		if(jobData.jobName != null && jobData.jobName != ""){
			def paramData = data.PARAMS
			if(paramData != null && paramData != [:]){
				def paramStr = ""
				paramData.each {key, value ->
					if (paramStr != "") {
						paramStr += ", "
					}
					paramStr += "\"" + key + "\":" + value
				}
				paramStr = "[" + paramStr + "]"
				paramStr = regularExpressionValidate(paramStr)
				writeToFile(JOB_DIR + "/${jobData.jobName}.params", paramStr)
			}else{
				File paramFile = new File(JOB_DIR + "/${jobData.jobName}.params")
				if(paramFile.exists()){
					return paramFile.delete()
				}
			}
		}else{
			println "job'name is required!"
			return false
		}
		return true
	}

	// convert data from number string to number
	// by a regular expression
	String regularExpressionValidate(String object){
		//convert data from number string to number
		String macherPattern = "(\")([0-9]{1,}[\\.]{0,1}[0-9]{0,})(\")"
		Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(object);
		while(matcher.find()){
			object = object.replace("\"" + matcher.group(2) + "\"", matcher.group(2))
		}

		//Except schedule
		macherPattern = "(\"schedule\": )([0-9]{1,}[\\.]{0,1}[0-9]{0,})([,|\n|\\]| |\t|]{1})"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		matcher = pattern.matcher(object);
		while(matcher.find()){
			object = object.replace( matcher.group(1) + matcher.group(2) + matcher.group(3), matcher.group(1) + "\"" + matcher.group(2) + "\"" + matcher.group(3))
		}
		return object
	}

	String convertData(data){
		def gson = new GsonBuilder().setPrettyPrinting().create()
		def jsonData = gson.toJson(data)
		jsonData = jsonData.replaceAll("\\{", "[").replaceAll("\\}", "]")
		String finalData = new String(jsonData);
		finalData = finalData.replaceAll(Pattern.quote('"\\'), "");
		finalData = finalData.replaceAll(Pattern.quote('\\"'), "");
		return finalData
	}

	String convertDataNotPretty(data){
		def gson = new GsonBuilder().create()
		def jsonData = gson.toJson(data)
		jsonData = jsonData.replaceAll("\\{", "[").replaceAll("\\}", "]")
		String finalData = new String(jsonData);
		finalData = finalData.replaceAll(Pattern.quote('"\\'), "");
		finalData = finalData.replaceAll(Pattern.quote('\\"'), "");
		return finalData
	}

	def getKeyExprData(KEYEXPR){
		def keyExprStr = "[:]"
		def keyExprUnitStr = ""
		def keyExprChartStr = ""

		GroovyShell shell = new GroovyShell()
		if(KEYEXPR != null){
			// Get KEYEXPR._root
			if(KEYEXPR._root != null && KEYEXPR._root != ""){
				def _root = KEYEXPR._root
				if(_root instanceof Map){
					keyExprStr = convertDataNotPretty(_root)
					keyExprStr = keyExprStr.substring(1, keyExprStr.length() - 1)
				}else{
					if((KEYEXPR._sequence == null || KEYEXPR._sequence == "")
					&& (KEYEXPR._unit == null || KEYEXPR._unit == "[:]" || KEYEXPR._unit == "")
					&& (KEYEXPR._chart == null || KEYEXPR._chart == [:])){
						keyExprStr = convertDataNotPretty(_root)
						keyExprStr = keyExprStr.substring(1, keyExprStr.length() - 1)
					} else {
						keyExprStr = "_root:" + convertDataNotPretty(_root)
					}
				}
			}

			// Get KEYEXPR._sequence
			if(KEYEXPR._sequence != null && KEYEXPR._sequence != ""){
				if (keyExprStr == "[:]") {
					keyExprStr = "_sequence:" + KEYEXPR._sequence
				} else {
					keyExprStr += ", _sequence:" + KEYEXPR._sequence
				}
			}
			if (keyExprStr != "[:]"){
				keyExprStr = "[" + keyExprStr +"]"
			}

			// Get KEYEXPR._unit
			if(KEYEXPR._unit != null && KEYEXPR._unit != "[:]" && KEYEXPR._unit != "" && KEYEXPR._unit != "[\"\n\"]"){
				keyExprUnitStr = KEYEXPR._unit
				keyExprUnitStr = regularExpressionValidate(keyExprUnitStr)
			}

			// Get KEYEXPR._chart
			if(KEYEXPR._chart != [:]){
				def chartMap = convertChartFormat(KEYEXPR._chart)
				keyExprChartStr = convertData(chartMap)
				keyExprChartStr = regularExpressionValidate(keyExprChartStr)
			}
		}
		return [keyExpr:keyExprStr, keyExprChart:keyExprChartStr, keyExprUnit:keyExprUnitStr]
	}

	def convertChartFormat(dataChart){
		def isSubtyped = false
		dataChart.find{key, value->
			if(value.group != ''){
				isSubtyped = true
			}
			return true
		}
		if(isSubtyped){
			// Subtyped
			def retChart = [:]
			dataChart.each{key, value->
				if(retChart[value.group] == null) {
					retChart[value.group] = []
				}

				def chartItem = [:]
				chartItem.type = value.type
				chartItem.name = value.name
				chartItem.chart_columns = value.chart_columns
				chartItem.hint_columns = value.hint_columns

				retChart[value.group].add(chartItem)
			}
			return retChart
		}else{
			// Store
			def retChart = []
			dataChart.each{key, value->
				def chartItem = [:]
				chartItem.type = value.type
				chartItem.name = value.name
				chartItem.chart_columns = value.chart_columns
				chartItem.hint_columns = value.hint_columns
				retChart.add(chartItem)
			}
			return retChart
		}
	}


	/**
	 * Get param file's script
	 * @param paramFile
	 * @return object params / null when params file not existed
	 */
	def getJobParamScript(paramFile){
		if(paramFile.exists()){
			def shell = new GroovyShell()
			def param_eval = shell.evaluate(paramFile)
			param_eval.each{ key, value ->
				param_eval[key] = value.toString()
			}
			return param_eval
		}
		return null;
	}

	/**
	 * Get instance file's script
	 * @param instanceFile
	 * @return object instances / null when instance file not existed
	 */
	def getJobInstanceScript(instanceFile){
		if(instanceFile.exists()){
			def shell = new GroovyShell()
			def inst_eval = shell.evaluate(instanceFile)
			return inst_eval
		}
		return null;
	}

	/**
	 * Get job file's script
	 * @param jobFile
	 * @return job script
	 */
	def getJobScript(jobFile){
		def stringOfJob = jobFile.getText()
			def strKeyPattern = ""
			listKey.each {key->
				strKeyPattern += key + "|"
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
			if(mapResult['JOB'] != null){
				temp = shell.evaluate(mapResult['JOB'])
				mapResult['JOB'] = temp['name']
				mapResult['jobclass'] = temp['jobclass']
			}else{
				mapResult['JOB'] = jobFile.getName().substring(0, jobFile.getName().lastIndexOf(".job"))
				mapResult['jobclass'] = ""
			}
			if(mapResult['KEYEXPR'] != null){
				temp = shell.evaluate(mapResult['KEYEXPR'])
				mapResult['KEYEXPR'] = temp
			}
			if(mapResult['KEYEXPR._unit'] != null){
				temp = shell.evaluate(mapResult['KEYEXPR._unit'])
				mapResult['KEYEXPR._unit'] = temp
			}
			if(mapResult['KEYEXPR._chart'] != null){
				temp = shell.evaluate(mapResult['KEYEXPR._chart'])
				mapResult['KEYEXPR._chart'] = temp
			}
			if(mapResult['KEYEXPR._description'] != null){
				temp = shell.evaluate(mapResult['KEYEXPR._description'])
				mapResult['KEYEXPR._description'] = temp
			}
			// Get comment
			// Create macher for job's comment
			String commentMacherPattern = "((?:(?!" + strKeyPattern + "[ ]*=[ ]*).)*)" + "("+ strKeyPattern + ")"
			Pattern commentPattern = Pattern.compile(commentMacherPattern, Pattern.DOTALL);
			Matcher commentMatcher = commentPattern.matcher(stringOfJob);
			def commentStr = ""
			while(commentMatcher.find()){
				commentStr = commentMatcher.group(1)
				break;
			}
			mapResult['comment'] = commentStr
			decorateResult(mapResult)
			return mapResult
	}

	/**
	 * Decorate result. Currently just replace \t = spaces
	 * @param paramFile
	 * @return
	 */
	def decorateResult(result){
		if(result instanceof Map){
			result.each{
				if(it.value instanceof String){
					it.value = ((String)it.value).replaceAll("\t", "    ")
				}
			}
		}
	}

	/**
	 * Write data to file with CHARSET encode
	 * @param paramFile
	 * @return
	 */
	def writeToFile(fileName, data) {
		def dataFile = new File(fileName);
		dataFile.write(data, CHARSET)
	}
}
def JobDeclared
try {
	JobDeclared = new JobDeclared()
} catch (e) {
	println e
}

if (JobDeclared != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/JobDeclared"
	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", JobDeclared, props)
}
