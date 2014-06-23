import java.io.IOException;
import groovy.json.*
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DBCommonConfigServlet extends HttpServlet{
	static final String HOMEPATH = System.getProperty("felix.home")
	static final String JOB_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOB_DIRECTORY)
	def static final listKey = [
		"JOB",
		"GROUPKEY",
		"QUERY",
		"QUERY_VARIABLE",
		"DBEXEC",
		"DBEXEC_VARIABLE",
		"COMMAND",
		"FORMAT",
		"FETCHACTION",
		"ACCUMULATE",
		"FINALLY",
		"KEYEXPR",
		"KEYEXPR._root",
		"KEYEXPR._sequence",
		"KEYEXPR._unit",
		"KEYEXPR._chart",
		"SENDTYPE",
		"RESOURCEID",
		"MONITORINGTYPE",
		"DBTYPE",
		"DEST",
		"HOSTID",
		"SID"
	]
	def properties = MonitorJobConfigLoader.getProperties()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		def job_dir = new File(JOB_DIR)
		PrintWriter out = resp.getWriter()
		def list_job = []
		try{
			def strDBType = req.getParameter("dbtype")
			def strJobFileName = req.getParameter("jobName")
            if (strDBType != null && strDBType != ""){
				if(job_dir.isDirectory()){
					job_dir.listFiles().each{
						def fileName = it.getName()
						if( fileName.startsWith(strDBType) && fileName.endsWith('.job')){
							list_job.add(fileName.substring(0,fileName.lastIndexOf('.job')))
						}
					}
				} else {
				  println "Job directory not found !"
				}
				def builderListJob = new JsonBuilder(list_job)
				out.println(builderListJob.toPrettyString());
			}

			if (strJobFileName != null && strJobFileName != ""){
				// Get jobName
				File jobFile = new File(JOB_DIR + "/" + strJobFileName + ".job")
				def mapJobScript = getJobScript(jobFile)
					
				def jobName = mapJobScript['JOB']
					
				if(jobName == null){
					jobName = strJobFileName
				}
				def shell = new GroovyShell()
				def params = [:]
				def jobFileParams = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/${jobName}.params")
				def jobFileInstance = new File(properties.get(ResourceConstants.JOBINST_DIRECTORY) + "/${jobName}.instances")
				if (jobFileParams.exists()) {
					params['params'] = shell.evaluate(jobFileParams)
				}else{
					params['params'] = [:]
				}
				if (jobFileInstance.exists()) {
					params['instances'] = shell.evaluate(jobFileInstance)
				}else{
					params['instances'] = [:]
				}
				params['jobName'] = jobName
				def builderParams = new JsonBuilder(params)
				out.println(builderParams.toPrettyString());
			}
		}catch(Exception ex){
			println ex;
		}
	}

	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
				resp.setContentType("text/html")
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
				resp.setContentType("text/html")
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")

		def properties = MonitorJobConfigLoader.getProperties()
		def contentText = req.getInputStream().getText()
		def slurper = new JsonSlurper()
		def object = slurper.parseText(contentText)
		def strJobName = object.job

		def jobFileParams = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/${strJobName}.params")
		def jobFileInstance = new File(properties.get(ResourceConstants.JOBINST_DIRECTORY) + "/${strJobName}.instances")

		FileWriter fw = new FileWriter(jobFileParams)
		BufferedWriter bw = new BufferedWriter(fw);
		def builder = new JsonBuilder(object.data.params)
		def str_params = builder.toPrettyString().replaceAll("\\{","\\[").replaceAll("\\}","\\]").replaceAll("\\\\", "\\\\\\\\")
		bw.write(str_params);
		bw.close();
		FileWriter fwInstance = new FileWriter(jobFileInstance)
		BufferedWriter bwInstance = new BufferedWriter(fwInstance);
		def builderInstance = new JsonBuilder(object.data.instances)
		def str_paramsInstance = builderInstance.toPrettyString().replaceAll("\\{","\\[").replaceAll("\\}","\\]").replaceAll("\\\\", "\\\\\\\\")
		bwInstance.write(str_paramsInstance);
		bwInstance.close();

		PrintWriter out = resp.getWriter()
		def message = [:]
		message["status"] = "OK"
		def builder2 = new JsonBuilder(message)
		out.print(builder2.toPrettyString())
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
		def temp
		if(mapResult['JOB'] != null){
			temp = shell.evaluate(mapResult['JOB'])
			mapResult['JOB'] = temp['name']
			mapResult['jobclass'] = temp['jobclass']
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
		return mapResult
	}
}

def dbCommonServlet
try {
	dbCommonServlet = new DBCommonConfigServlet()
} catch (e) {

}

if (dbCommonServlet != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/DBCommonConfig"

	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", dbCommonServlet, props)
}
