import groovy.json.*
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.lang.GroovyShell
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

class PolicyStringService{
	def slurper = new JsonSlurper()
	
	public def create(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		def form = request.getBodyFromUrlFormEncoded()
		def datajson = form["data"] != null ? form["data"][0] : null
		def listKeyJson = form["listKey"] != null ? form["listKey"][0] : null
		def listKey
		if(listKeyJson != null && listKeyJson != ""){
			 listKey = slurper.parseText(listKeyJson)
		 }
		def action = form["action"] != null ? form["action"][0] : null
		def sampleData = form["sampleData"] != null ? form["sampleData"][0] : null
		def type = form["type"] != null ? form["type"][0] : null

		def mapConditionLevel = form["mapConditionLevel"] != null ? form["mapConditionLevel"][0] : null
		if(mapConditionLevel != null) {
			mapConditionLevel = slurper.parseText(mapConditionLevel)
		}
		def paramsjson = form["params"] != null ? form["params"][0] : null
		def params
		if(paramsjson != null && paramsjson != ""){
			params = slurper.parseText(paramsjson)
		}
		
		def data
		def closureStr = ""
		if(datajson != null && datajson != ""){
			if(action == "STANDARDCONDITION") {
				def standardData = getDataConditionsAfterEdit(datajson, listKey)
				return standardData
			} else if(action == "STANDARDMESSAGE") {
				def standardData = getDataMessageAfterEdit(datajson, listKey)
				return standardData
			} else {
				data = slurper.parseText(datajson)
				def resp = [:]
				closureStr = generatePolicyString(data, listKey, type, mapConditionLevel)
			}

			if(action == "PREVIEW") {
				return closureStr
			} else if(action == "RUNTEST") {
				try {
					// Data policy
					def mapSampleData = slurper.parseText(sampleData)
					// Closure
					GroovyShell shell = new GroovyShell()
					def obj = shell.evaluate(closureStr)
					def binding = obj.getBinding()
					for(def key in params.keySet()){
						def value = params[key]
						try{
							value = Double.valueOf(value)
						}catch(NumberFormatException nfe){
							// Do nothing
						}
						binding.setVariable(key, value)
					}
					def policyClos = binding.getVariable("POLICY")
					def resultResponse = policyClos(mapSampleData)
					def builder = new JsonBuilder(resultResponse)
					return builder.toString()
				} catch(Exception ex) {
					def respData = ['status':'failed']
					def errorMsg = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ex)
					respData['message'] = errorMsg
					//def builder = new JsonBuilder(respData)
					return respData
				}
			} else if(action == "WRITE2FILE") {
				def respData = generatePolicyString(data,listKey, type, mapConditionLevel)
				return respData
			}
		}
	}

	public String update(Request request, Response response){
		return "update_METHOD"
	}

	public def read(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
	}
	
	public def delete(Request request, Response response){
		return "DELETE"
	}
	
	def getLevel(condition, mapConditionLevel, group = null){
		def ret = ''
		def intRet
		if(mapConditionLevel != null && mapConditionLevel.size() > 0){
			if(group == null){
				// Store
				for(def key in mapConditionLevel.keySet()){
					def value = mapConditionLevel[key]
					if(condition == key){
						ret = value
					}
				}
			}else{
			// Subtyped
				for(def key in mapConditionLevel[group].keySet()){
					def value = mapConditionLevel[group][key]
					if(condition == key){
						ret = value
					}
				}
			}
		}
		if(ret == "Low"){
			intRet = 1
		} 
		if(ret == "Medium"){
			intRet = 2
		}
		if(ret == "High"){
			intRet = 3
		}
		return intRet 
	}

	// GENERATE DATA TO POLICY STRING
	def generatePolicyString(data, listKey, type, mapConditionLevel){
		def policyStr = ""
		try {
			if(type == "store") {
				if(data.mappolicy != null && data.mappolicy.size() > 0){
					policyStr += "POLICY = {resultData->\n"
					policyStr += "\tdef listMess = []\n"
					policyStr += "\tdef ret = ['jobName' : '" + data.jobName + "', 'istIid' : '" + data.instanceName + "']\n"
					def mapPolicy = data.mappolicy
					policyStr += "\tresultData.each{data->\n"
					for(def key in mapPolicy.keySet()){
						def value = mapPolicy[key]
						key = key.trim()
						if(key[0] != "("){
							key = "(" + key
						}
						if(key[key.size() - 1] != ")"){
							key = key + ")"
						}

						// if statement
						policyStr += "\t\tif(" + getDataConditionsAfterEdit(key, listKey) + "){\n"
						// message print statement
						policyStr += "\t\t\tlistMess.add([level: " + getLevel(key, mapConditionLevel) + ", message: \"\"\""+ value +"\"\"\"])\n\t\t}\n"
					}
					policyStr += "\t}\n"
					policyStr += "\tret['message'] = listMess\n"
					policyStr += "\treturn ret\n}"
				}
			} else if(type == "subtyped") {
				policyStr += "POLICY = {resultData->\n"
				policyStr += "\tdef listMess = []\n"
				policyStr += "\tdef ret = ['jobName' : '" + data.jobName + "', 'istIid' : '" + data.instanceName + "']\n"
				policyStr += "\tresultData.each {key,value ->\n"
				for(def keyData in data.mappolicy.keySet()){
					def valueData = data.mappolicy[keyData]
					if(valueData != [:]) {
						policyStr += "\t\tif(key == \"" + keyData + "\") {\n"
						policyStr += "\t\t\tvalue.each {data ->\n"
						for(def key in valueData.keySet()){
							def value = valueData[key]
							// If
							policyStr += "\t\t\t\tif(" + getDataConditionsAfterEdit(key, listKey) + "){\n"
							// Message
							policyStr += "\t\t\t\t\tlistMess.add([level: " + getLevel(key, mapConditionLevel, keyData) + ", message: \"\"\""+ value +"\"\"\"])\n"
							policyStr += "\t\t\t\t}\n"
						}
						policyStr += "\t\t\t}\n"
						policyStr += "\t\t}\n"
					}
				}
				policyStr += "\t}\n"
				policyStr += "\tret['message'] = listMess\n"
				policyStr += "\treturn ret\n"
				policyStr += "}"
			}
			return policyStr
		} catch(Exception ex) {
			return "ex:" + ex
		}
	}

	def getDataConditionsAfterEdit(String stringOfPolicy, dataKey){
		List OperatorList = [" ", "\\(", "\\)", "=", "\\+|\\-|\\*|\\/|%", ">|<|=|!", "\\|\\||&&|\\?\\:", "\\~|<<|>>|>>>|&|\\^|\\|"]

		//Replace all unnecessary space
		String macherPattern = "([ ]{2,})"
		Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		stringOfPolicy = "(" + stringOfPolicy.replaceAll(pattern, " ").trim() + ")"

		String strKeyPattern = convertListToString(dataKey, "|")
		String strOperator = convertListToString(OperatorList, "|")
	 
		//Create macher
		macherPattern = "(" + strOperator + ")(" + strKeyPattern + ")(" + strOperator + "|\\.)"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(stringOfPolicy);
		def oldData
		def newData
		while(matcher.find()){
			oldData = matcher.group()
			newData = matcher.group(1) + "data." + matcher.group(2) + matcher.group(3)
			stringOfPolicy = stringOfPolicy.replace(oldData, newData)
		}
		stringOfPolicy = stringOfPolicy.substring(1, stringOfPolicy.length() -1)
		return stringOfPolicy
	}

	def getDataMessageAfterEdit(String stringOfMessage, dataKey){
		//Replace all unnecessary space
		String macherPattern = "([ ]{2,})"
		Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		stringOfMessage = stringOfMessage.replaceAll(pattern, " ").trim()
		stringOfMessage = " " + stringOfMessage + " "
		stringOfMessage = stringOfMessage.replaceAll('"""', '\'\'\'')
		String strKeyPattern = convertListToString(dataKey, "|")
		
		//Remove unneed data
		macherPattern = "(\\\$\\{data\\.)(" + strKeyPattern + ")(\\})"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(stringOfMessage);
		while(matcher.find()){
			stringOfMessage = stringOfMessage.replace(matcher.group(), matcher.group(2))
		}
		
		//Create macher
		macherPattern = "((?:(?!(\\d|[a-zA-Z]|\'|\")).)+)(" + strKeyPattern + ")((?:(?!(\\d|[a-zA-Z]|\'|\")).)+)"
		pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		matcher = pattern.matcher(stringOfMessage);
		def oldData
		def newData
		while(matcher.find()){
			oldData = matcher.group()
			newData = matcher.group(1) + '${data.' + matcher.group(3) + '}' + matcher.group(4)
			stringOfMessage = stringOfMessage.replace(oldData, newData)
		}
		stringOfMessage = stringOfMessage.substring(1, stringOfMessage.length() -1)
		stringOfMessage = stringOfMessage.replace('"', '') 
		stringOfMessage = stringOfMessage.replace('\'\'\'', '')
		return stringOfMessage
	}
	
	def convertListToString (List listData, String concatStr = "|"){
		def strRet = ""
		for(def key in listData){
			strRet += key + concatStr
		}
		if (strRet != "") {
			strRet = strRet.subSequence(0, strRet.length() - concatStr.length())
		}
	}
}
