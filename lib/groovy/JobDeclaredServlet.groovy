import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.json.*
	
public class JobDeclared extends HttpServlet {
	def properties = MonitorJobConfigLoader.getProperties()
	static final String JOB_DIR = "var/job/"
	static final String HOMEPATH = System.getProperty("felix.home")
	static final String PARAMFILE = "var/conf/default.params"
	def static final listKey = ["JOB", "GROUPKEY", "QUERY", "QUERY_VARIABLE", "DBEXEC", "DBEXEC_VARIABLE", "COMMAND", "FORMAT", "FETCHACTION", "ACCUMULATE", "FINALLY", "KEYEXPR", "KEYEXPR._root", "KEYEXPR._sequence", "KEYEXPR._unit", "KEYEXPR._chart", "KEYEXPR._description", "SENDTYPE", "RESOURCEID", "MONITORINGTYPE", "DBTYPE", "DEST", "HOSTID", "SID"]
	@Override
	void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html")
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		
		try {
			// Get JobFile
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
	      	def object = slurper.parseText(contentText)
			def jobFileName = object.job
			def jobPath = JOB_DIR + jobFileName
			def jobFile = new File(HOMEPATH, jobPath + ".job")
			
			// resultRet returning result [Job:~~, instances:~~, params:~~]
			def resultRet = [:]
			// Get job's script
			def stringOfJob = getJobScript(jobFile)
			def realJobName = stringOfJob.JOB
			def filePath = JOB_DIR + realJobName
			def instanceFile = new File(HOMEPATH, filePath + ".instances")
			def paramFile = new File(HOMEPATH, filePath + ".params")
			// Get instance file's script in Object type(Map)
			def instanceResult = getJobInstanceScript(instanceFile)
			// Get param file's script in Object type(Map)
			def paramResult = getJobParamScript(paramFile)
				
			// Set data into returning Result
			resultRet['Job'] = stringOfJob
			resultRet['instances'] = instanceResult
			resultRet['params'] = paramResult
			// Generate json String to response
			def builder = new JsonBuilder(resultRet)
			//println builder.toPrettyString()
			out.print(builder.toString())
		}catch (Exception ex) {
			println "ERROR DOPOST: " + ex
			println org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ex)
			def message = [status:"failed"]
			def builder = new JsonBuilder(message)
			out.print(builder.toString())
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
			String macherPattern = "("+ strKeyPattern + ")([ ]*=[ ]*)((?:(?!" + strKeyPattern + "[ ]*=[ ]*).)*)"

			Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
			Matcher matcher = pattern.matcher(stringOfJob);

			def mapResult = [:]

			while(matcher.find())
			{
				mapResult[matcher.group(1)] = matcher.group(3)
			}
			def shell = new GroovyShell()
			def temp = shell.evaluate(mapResult['JOB'])
			mapResult['JOB'] = temp['name']
			mapResult['jobclass'] = temp['jobclass']
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