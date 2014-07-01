import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.json.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.lang.GroovyShell
import com.gmongo.GMongo
import com.mongodb.util.JSON
	
class ServletPolicy extends HttpServlet{
	static final String JOB_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOB_DIRECTORY)
	static final String POLICY_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOB_DIRECTORY) + "/policy"
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		def list_job = []
		def builder
		try{
			def jobName = req.getParameter("job_name")
			def jobTyped = req.getParameter("type")
			if(jobName != "" && jobName != null) {
				// Policy Data
				def listPolicy = readFromFile(jobName, jobTyped)
				def params = readFileParam(jobName)
				def mapFinalPolicy = [:]
				mapFinalPolicy["POLICY"] = [:]
				if(listPolicy != null) {
					mapFinalPolicy["POLICY"]["listpolicy"] = listPolicy
				}
				mapFinalPolicy["POLICY"]["params"] = params
				builder = new JsonBuilder(mapFinalPolicy)
				out.println(builder.toPrettyString());
			}
		} catch(Exception ex){
			println "ERROR GET: " + ex
		}
	}
	
	@Override
	void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		def builder
		def message
		def errorMsg = ""
		try {
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
	      	def data = slurper.parseText(contentText)
	      	def filename
	      	if(data.instanceName != null && data.instanceName != "" && data.instanceName != "noChoice"){
	      		filename = data.jobName + "." + data.instanceName
	      	} else {
	      		filename = data.jobName
	      	}
	      	
	      	// Try to convert number string to number
			data.params.each{key,value->
				try{
					data.params[key] = Double.valueOf(value)
				}catch(NumberFormatException nfex){
					// Do nothing with it
				}
			}
			
	      	builder = new JsonBuilder(data.params)
	      	
      		if(write2File(filename + ".policy", data.policyStr) &&  write2File(filename + ".params", builder.toString())) {
	      		message = [status:"OK", message:"Process policy file successfully !!!"]
				builder = new JsonBuilder(message)
				out.print(builder.toString())
	      	} else {
	      		errorMsg = "Process policy file error !!!"
	      	}
	      	
	      	if (errorMsg != "") {
				message = [status:"failed", message:errorMsg]
				builder = new JsonBuilder(message)
				out.print(builder.toString())
			}
		} catch(Exception ex) {
			println "ERROR POST: " + ex
			errorMsg = "Error when post data: " + ex
			errorMsg += org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ex)
			message = [status:"failed", message:errorMsg]
			builder = new JsonBuilder(message)
			out.print(builder.toString())
		}
	}

	def readFromFile(filename, type){
		def ret
		if(type == "Store"){
			ret = readFromFileStore(filename)
		}
		if(type == "Subtyped"){
			ret = readFromFileSubtyped(filename)
		}
		return ret
	}

	// READ POLICY FILE TO GET POLICY INFORMATION
	def readFromFileStore(filename){
		def splittedStr = []
		def macherPattern = "((if\\()((?:(?!if\\().)*))(\\n)"
		def pattern = Pattern.compile(macherPattern, Pattern.DOTALL)

		def filePath = POLICY_DIR  + "/" + filename
		File policyFile = new File(filePath + ".policy")
		if(policyFile.isFile()) {
			String policyStr = policyFile.getText()
			def matcher = pattern.matcher(policyStr)
			while(matcher.find()){
				splittedStr.add(matcher.group(1))
			}

			def listpolicy = []
			def mapPolicy
			macherPattern = "(if\\()((?:(?!if\\().)*)([)]{1}\\{.*\\(\\[)(level: )(\\d*)(, message: \"\"\")(.*)(\"\"\"\\]\\))"
			pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
			splittedStr.each{str->
				matcher = pattern.matcher(str);
				while(matcher.find()){
					mapPolicy = [:]
					mapPolicy["condition"] = matcher.group(2)
					mapPolicy["level"] = matcher.group(5)
					mapPolicy["message"] = matcher.group(7)
					listpolicy.add(mapPolicy)
				}
			}
			return listpolicy
		} else {
			return null
		}
	}
	
	def readFromFileSubtyped(filename){	
		println "Begin load from file $filename with subtyped" 
		def filePath = POLICY_DIR +"/"+ filename
		File policyFile = new File(filePath + ".policy")
		if(policyFile.isFile()) {
			String policyStr = policyFile.getText()
			String macherPattern = "(if\\(key == \")((?:(?!(\")).)*)(\"\\) \\{)((?:(?!(if\\(key == \")).)*)(\\})"
			Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
			Matcher matcher = pattern.matcher(policyStr);
	
			Map groupData = [:]
			while(matcher.find()) {
				groupData[matcher.group(2)] = matcher.group(5)
			}
	
			Map returnData = [:]
			List splittedStr = []
			groupData.each {key, value ->
				splittedStr = []
				returnData[key] = []
				macherPattern = "((if\\()((?:(?!if\\().)*))(\\n)"
				pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
				matcher = pattern.matcher(value)
	
				while(matcher.find()){
					splittedStr.add(matcher.group(1))
				}
				splittedStr.each { str ->
					macherPattern = "(if\\()((?:(?!if\\().)*)([)]{1}\\{.*\\(\\[)(level: )(\\d*)(, message: \"\"\")(.*)(\"\"\"\\]\\))"
					pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
					matcher = pattern.matcher(str);
					while(matcher.find()){
						def mapPolicy = [:]
						mapPolicy["condition"] = matcher.group(2)
						mapPolicy["level"] = matcher.group(5)
						mapPolicy["message"] = matcher.group(7)
						returnData[key].add(mapPolicy)
					}
				}
			}
	
			return returnData
		} else {
			return null
		}
	}

	def readFileParam(jobname){
		def params
		def params_file = new File(POLICY_DIR +"/"+ jobname + ".params")
		if(params_file.exists()){
			def json = params_file.getText()
		    def slurper = new JsonSlurper()
			params = slurper.parseText(json)
		}
		return params
	}
	
	// WRITE DATA TO POLICY FILE
	def write2File(filename, data){
		def filePath = POLICY_DIR +"/" + filename
		File policyFile = new File(filePath)
		if((data != "") && (data != "{}")) {
			try {
				policyFile.setText(data)
				return true
			} catch(Exception ex) {
				println "ERROR WRITE2FILE: " + ex
				return false
			}
		} else {
			if(policyFile.exists()) {
				try {
					policyFile.delete()
					return true
				} catch(Exception ex) {
					println "ERROR WRITE2FILE: " + ex
					return false
				}
			} 
			return true
		}
	}

	// process to decorate input
	// modify and format input
	def mixString(String input){
		String ret
		// trim before processing
		if(input != null) {
			input = input.trim()
			ArrayList cloneAsListCharacter = input.value
			// process '&' character and '|' character
			for(int i = 0;i < input.value.length; i++){
				if(input.value[i] == '&'
				&& (input.value[i - 1] != '&' && input.value[i + 1] != '&')){
					cloneAsListCharacter.add(i, '&')
				}
				if(input.value[i] == '|'
				&& (input.value[i - 1] != '|' && input.value[i + 1] != '|')){
					cloneAsListCharacter.add(i, '|')
				}
			}
			// get field and add turn it into data['field'] or data.field
			ret = (new String((char[])cloneAsListCharacter)).trim()
		}
		return ret
	}
}

def servletRoot
try {
	servletRoot = new ServletPolicy()
} catch (e) {
	
}

if (servletRoot != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/policyServlet"
	
	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", servletRoot, props)
}