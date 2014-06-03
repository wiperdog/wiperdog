@Grab(group='com.gmongo', module='gmongo', version='1.0')
@Grab(group='com.google.code.gson', module='gson', version='2.2.4')
import groovy.json.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.lang.GroovyShell
import com.gmongo.GMongo
import com.google.gson.Gson

public class ProcessGeneratePolicy {
	def static reader = new BufferedReader(new InputStreamReader(System.in));
	def static POLICY_DIR = ""
	def static HOMEPATH = ""
	def static confirmLoop = false
	def static jobName
	def static istIid
	def static group
	def static level
	def static condition
	def static message
	def static type
	def static lstKeySubtyped
	def static data = [:]
	def static mappolicy = [:]
	def static mapPolicyForKey = [:]
	def static finalConditionLevel = [:]
	def static mapConditionLevelForKey = [:]
	public static void main(String[] args) throws Exception {
		println ">>>>> ENTER POLICY'S INFORMATION COMMON <<<<<"
		if (args[0] == ".") {
			HOMEPATH = System.getProperty("user.dir")
		} else {
			HOMEPATH = args[0]
		}
		POLICY_DIR = HOMEPATH.replace("bin", "/var/job/policy")
		def folderPolicy = new File(POLICY_DIR)
		if (!folderPolicy.exists()) {
			folderPolicy.mkdirs()
		}
		def mongo = new GMongo("localhost", 27017)
		def dbConn = mongo.getDB("wiperdog")
		def lstJobMongo = dbConn.getCollectionNames()
		println "LIST JOB IN MONGODB: "
		lstJobMongo.eachWithIndex {collNm, index ->
			if (collNm.contains(".")) {
				def namecoll = collNm.substring(0, collNm.lastIndexOf("."))
				def istiid = collNm.substring(collNm.lastIndexOf(".") + 1, collNm.size())
				println index + 1 + ". Job: " + namecoll + " , IstIid: " + istiid
			}
		}

		def checCollExits = false
		while (!checCollExits) {
			print "Enter Job Name: "
			jobName = reader.readLine()
			data['jobName'] = jobName
			print "Enter IstIid: "
			istIid = reader.readLine()
			data['instanceName'] = istIid
			if (istIid != "") {
				jobName += "." + istIid
			} else {
				jobName += ".null"
			}
			if (lstJobMongo.contains(jobName)) {
				checCollExits = true
			} else {
				println "This job is not exists, please re-enter !!!"
				println "To cancel, click Ctrl+C"
			}
		}
		def listKey = []
		def sampleRecord = dbConn[jobName].find().limit(1)
		def mapRecord
		println "LIST KEY AND DATA SAMPLE OF COLLECTION: $jobName"
		while(sampleRecord.hasNext()) {
			def dataRecord = sampleRecord.next()
			type = dataRecord['type']
			if (type == "Store") {
				mapRecord = dataRecord['data'][0]
				listKey = (ArrayList) mapRecord.keySet()
				if (listKey != []) {
					if (listKey.contains("RECORD_SEQ")) {
						listKey.remove("RECORD_SEQ")
					}
					listKey.each {
						println "    - Field: " + it + ", Unit: " + dataRecord['KEYEXPR']['_unit'][it] + ", Data Sample: " + mapRecord[it]
					}
				} else {
					println "No data in mongodb !!!"
					return
				}
			} else if (type == "Subtyped") {
				lstKeySubtyped = dataRecord['data'].keySet()
				lstKeySubtyped.each {key ->
					def dataForKey = dataRecord['data'][key][0]
					listKey = (ArrayList) dataForKey.keySet()
					if (listKey != []) {
						if (listKey.contains("RECORD_SEQ")) {
							listKey.remove("RECORD_SEQ")
						}
						listKey.each {
							println "    - Group: " + key + ", Field: " + it + ", Unit: " + dataRecord['KEYEXPR']['_unit'][it] + ", Data Sample: " + dataForKey[it]
						}
					} else {
						println "No data in mongodb !!!"
						return
					}
				}
			}
		}

		def checkProcessPolicy = ""
		def checkProcessParam = ""
		// PROCESS POLICY FILE
		print "DO YOU WANT CREATE/UPDATE POLICY (y|n)? "
		checkProcessPolicy = reader.readLine()
		while (checkProcessPolicy != "y" && checkProcessPolicy != "n") {
			print "PLEASE CHOICE AN OPTION (y|n)? "
			checkProcessPolicy = reader.readLine()
		}
		if (checkProcessPolicy == "y") {
			processPolicy(listKey)
		}
		// PROCESS PARAM POLICY FILE
		print "\nDO YOU WANT CREATE/UPDATE PARAM (y|n)? "
		checkProcessParam = reader.readLine()
		while (checkProcessParam != "y" && checkProcessParam != "n") {
			print "PLEASE CHOICE AN OPTION (y|n)? "
			checkProcessParam = reader.readLine()
		}
		if (checkProcessParam == "y") {
			processPolicyParam()
		} else {
			return
		}
	}

	public static void processPolicy(listKey) {
		println "***********************"
		println "* PROCESS POLICY FILE *"
		println "***********************"
		def policyStr
		def policyFile = new File(POLICY_DIR + "/" + jobName + ".policy")
		try {
			if (policyFile.isFile()) {
				def dataPolicyFromFile = readFromFile(jobName, type)
				println "\nPOLICY FILE IS EXISTS, CURRENT DATA: "
				println "===================="
				println dataPolicyFromFile
				println "====================\n"
				def mapCurrentPolicy = [:]
				def mapCurrentConditionLevel = [:]
				// Loop enter data
				while(!confirmLoop) {
					enterData()
				}
				if (type == "Store") {
					dataPolicyFromFile.each {objPoli ->
						mapCurrentPolicy[objPoli['condition'].replace("data.", "").replace(")", "").replace("(", "")] = objPoli['message']
						mapCurrentConditionLevel[objPoli['condition'].replace("data.", "").replace(")", "").replace("(", "")] = objPoli['level']
					}
					mappolicy.each {key, value ->
						mapCurrentPolicy[key] = value
					}
					finalConditionLevel.each {key, value ->
						mapCurrentConditionLevel[key] = value
					}
				} else if (type == "Subtyped") {
					lstKeySubtyped.each {eKey ->
						mapCurrentPolicy[eKey] = [:]
						mapCurrentConditionLevel[eKey] = [:]
						dataPolicyFromFile[eKey].each {objPoli ->
							mapCurrentPolicy[eKey][objPoli['condition'].replace("data.", "").replace(")", "").replace("(", "")] = objPoli['message']
							mapCurrentConditionLevel[eKey][objPoli['condition'].replace("data.", "").replace(")", "").replace("(", "")] = objPoli['level']
						}
						mappolicy[eKey].each {key, value ->
							mapCurrentPolicy[eKey][key] = value
						}
						finalConditionLevel[eKey].each {key, value ->
							mapCurrentConditionLevel[eKey][key] = value
						}
					}
				}
				data['mappolicy'] = mapCurrentPolicy
				policyStr = generatePolicyString(data, listKey, type, mapCurrentConditionLevel)
				policyFile.setText(policyStr)
				println "[SUCCESS] POLICY FILE IS UPDATED !!!"
			} else {
				// Loop enter data
				while(!confirmLoop) {
					enterData()
				}
				data['mappolicy'] = mappolicy
				policyStr = generatePolicyString(data, listKey, type, finalConditionLevel)
				policyFile.setText(policyStr)
				println "[SUCCESS] POLICY FILE IS CREATED !!!"
			}
		} catch (Exception ex) {
			println "[ERROR] " + ex
		}
	}

	public static void processPolicyParam() {
		println "*****************************"
		println "* PROCESS PARAM POLICY FILE *"
		println "*****************************"
		def checkCreateParam = ""
		def paramKey = ""
		def paramValue = ""
		def mapParam = [:]
		def checkAddNewParams = false
		def paramStr = ""
		def mapCurrentParam = [:]
		def paramPolicyFile = new File(POLICY_DIR + "/" + jobName + ".params")
		if (paramPolicyFile.isFile()) {
			println jobName + ".param FILE IS EXISTS"
			def paramFileContent = paramPolicyFile.getText()
			def slurper = new JsonSlurper()
			mapCurrentParam = slurper.parseText(paramFileContent)
			println "======= CURRENT PARAM INFO ======="
			println mapCurrentParam
			println "=================================="
		}
		while(!checkAddNewParams) {
			print "Enter param key: "
			paramKey = reader.readLine()
			while (paramKey == "") {
				print "Key of param can not be empty, please re-enter: "
				paramKey = reader.readLine()
			}
			print "Enter param value: "
			paramValue = reader.readLine()
			def confirmAdd = ""
			print "Add more param for policy (y|n)? "
			confirmAdd = reader.readLine()
			while (confirmAdd != "y" && confirmAdd != "n") {
				print "Add more param for policy (y|n)? "
				confirmAdd = reader.readLine()
			}
			if (confirmAdd == "n") {
				checkAddNewParams = true
			}
			mapParam[paramKey] = paramValue
		}
		if (mapCurrentParam != []) {
			print "UPDATED "
		} else {
			print "CREATE "
		}
		mapParam.each {key, value ->
			mapCurrentParam[key] = value
		}
		paramStr = new Gson().toJson(mapCurrentParam)
		paramPolicyFile.setText(paramStr)
		print "SUCCESS !!!\n"
	}

	public static void enterData() {
		if (type == "Store") {
			print "Enter Level (Low|Medium|High): "
			level = reader.readLine()
			while (level != "Low" && level != "Medium" && level != "High") {
				print "Please choose level for policy: "
				level = reader.readLine()
			}
			print "Enter Condition (a > 3): "
			condition = reader.readLine()
			while(condition == "") {
				print "Condition can not be empty, please re-enter: "
				condition = reader.readLine()
			}
			print "Enter Message: "
			message = reader.readLine().trim()
			while(condition == "") {
				print "Message can not be empty, please re-enter: "
				message = reader.readLine()
			}
			mappolicy[condition] = message
			finalConditionLevel[condition] = level
			print "Do you want add more condition (y|Y|n|N) ? "
			def check = reader.readLine()
			if (check == "n" || check == "N") {
				confirmLoop = true
			}
		} else if (type == "Subtyped") {
			print "Enter Group: "
			group = reader.readLine()
			while (group == "" || !lstKeySubtyped.contains(group)) {
				print "Group is incorrent, please re-enter: "
				group = reader.readLine()
			}
			if (mappolicy[group] != null) {
				mapPolicyForKey = mappolicy[group]
			}
			if (finalConditionLevel[group] != null) {
				mapConditionLevelForKey = finalConditionLevel[group]
			}
			print "Enter Level (Low|Medium|High): "
			level = reader.readLine()
			while (level != "Low" && level != "Medium" && level != "High") {
				print "Please choose level for policy: "
				level = reader.readLine()
			}
			print "Enter Condition (a > 3): "
			condition = reader.readLine()
			while(condition == "") {
				print "Condition can not be empty, please re-enter: "
				condition = reader.readLine()
			}
			print "Enter Message: "
			message = reader.readLine().trim()
			while(message == "") {
				print "Message can not be empty, please re-enter: "
				message = reader.readLine()
			}
			mapPolicyForKey[condition] = message
			mappolicy[group] = mapPolicyForKey
			mapPolicyForKey = [:]
			mapConditionLevelForKey[condition] = level
			finalConditionLevel[group] = mapConditionLevelForKey
			mapConditionLevelForKey = [:]
			print "Do you want add more condition (y|Y|n|N) ? "
			def check = reader.readLine()
			if (check == "n" || check == "N") {
				confirmLoop = true
			}
		}
	}

	def static readFromFile(filename, type){
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
	def static readFromFileStore(filename){
		def splittedStr = []
		def macherPattern = "((if\\()((?:(?!if).)*))(\\n)"
		def pattern = Pattern.compile(macherPattern, Pattern.DOTALL)

		def filePath = POLICY_DIR + "/" + filename
		File policyFile = new File(filePath + ".policy")
		if(policyFile.isFile()) {
			String policyStr = policyFile.getText()
			def matcher = pattern.matcher(policyStr)
			while(matcher.find()){
				splittedStr.add(matcher.group(1))
			}

			def listpolicy = []
			def mapPolicy
			macherPattern = "(if\\()((?:(?!if).)*)([)]{1}\\{.*\\(\\[)(level: )(\\d*)(, message: \"\"\")(.*)(\"\"\"\\]\\))"
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
	
	def static readFromFileSubtyped(filename){	
		def filePath = POLICY_DIR + "/" + filename
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
				macherPattern = "((if\\()((?:(?!if).)*))(\\n)"
				pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
				matcher = pattern.matcher(value)
	
				while(matcher.find()){
					splittedStr.add(matcher.group(1))
				}
				splittedStr.each { str ->
					macherPattern = "(if\\()((?:(?!if).)*)([)]{1}\\{.*\\(\\[)(level: )(\\d*)(, message: \"\"\")(.*)(\"\"\"\\]\\))"
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

	/**
	 * Get level of the policy from String to int
	 * @param condition Condition's key
	 * @param mapConditionLevel Map contains all condition-level
	 * @return
	 */
	def static getLevel(condition, mapConditionLevel, group = null){
		def ret = ''
		def intRet
		if(mapConditionLevel != null && mapConditionLevel.size() > 0){
			if(group == null) {
				// Store
				mapConditionLevel.each{key, value->
					if(condition.replace("(", "").replace(")", "") == key){
						ret = value
					}
				}
			} else {
				// Subtyped
				mapConditionLevel[group].each{key, value->
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

	/**
	 * GENERATE DATA TO POLICY STRING
	 * @param data Data for generating policy's String
	 * @param listKey Keys of jobs
	 * @param type Job's type(Store/Subtyped)
	 * @param mapConditionLevel Map contains all condition-level
	 * @return policy's String
	 */
	def static String generatePolicyString(data, listKey, type, mapConditionLevel){
		def policyStr = ""
		if(data == null || data == [:])
			return ""
		//return data
		try {
			if(type == "Store") {
				if(data.mappolicy != null && data.mappolicy.size() > 0){
					policyStr += "POLICY = {resultData->\n"
					policyStr += "\tdef listMess = []\n"
					policyStr += "\tdef ret = ['jobName' : '" + data.jobName + "', 'istIid' : '" + data.instanceName + "']\n"
					def mapPolicy = data.mappolicy
					policyStr += "\tresultData.each{data->\n"
					mapPolicy.each {key,value ->
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
			} else if(type == "Subtyped") {
				if(data.mappolicy != null && data.mappolicy.size() > 0){
					policyStr += "POLICY = {resultData->\n"
					policyStr += "\tdef listMess = []\n"
					policyStr += "\tdef ret = ['jobName' : '" + data.jobName + "', 'istIid' : '" + data.instanceName + "']\n"
					policyStr += "\tresultData.each {key,value ->\n"
					data.mappolicy.each {keyData,valueData ->
						if(valueData != [:]) {
							policyStr += "\t\tif(key == \"" + keyData + "\") {\n"
							policyStr += "\t\t\tvalue.each {data ->\n"
							valueData.each {key,value ->
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
			}
		} catch(Exception ex) {
			println "[ERROR] " + ex
		}
		return policyStr
	}

	/**
	 * Read and isolate the condition
	 * @param stringOfPolicy Policy's String
	 * @param dataKey key
	 * @return condition's string
	 */
	def static String getDataConditionsAfterEdit(String stringOfPolicy, dataKey){
		if(stringOfPolicy == null)
			return ""
		List OperatorList = [
			" ",
			"\\(",
			"\\)",
			"=",
			"\\+|\\-|\\*|\\/|%",
			">|<|=|!",
			"\\|\\||&&|\\?\\:",
			"\\~|<<|>>|>>>|&|\\^|\\|"
		]

		//Replace all unnecessary space
		String macherPattern = "([ ]{2,})"
		Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		stringOfPolicy = "(" + stringOfPolicy.replaceAll(pattern, " ").trim() + ")"

		String strKeyPattern = convertListToString(dataKey, "|")
		String strOperator = convertListToString(OperatorList, "|")

		// For compile pattern.
		// If strKeyPattern is empty, it could make mistake to matcher
		// So make it null if strKeyPattern is empty
		if(strKeyPattern == "")
			strKeyPattern = null
		if(strOperator == "")
			strOperator = null
		
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

	/**
	 * Read and isolate the message
	 * @param stringOfMessage Policy's String
	 * @param dataKey key
	 * @return message's string
	 */
	def static String getDataMessageAfterEdit(String stringOfMessage, dataKey){
		if(stringOfMessage == null)
			return ""
		//Replace all unnecessary space
		String macherPattern = "([ ]{2,})"
		Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		stringOfMessage = stringOfMessage.replaceAll(pattern, " ").trim()
		stringOfMessage = " " + stringOfMessage + " "
		stringOfMessage = stringOfMessage.replaceAll('"""', '\'\'\'')
		String strKeyPattern = convertListToString(dataKey, "|")

		// For compile pattern.
		// If strKeyPattern is empty, it could make mistake to matcher
		// So make it null if strKeyPattern is empty 
		if(strKeyPattern == "")
			strKeyPattern = null
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
	
	/**
	 * Convert list data to string
	 * @param listData
	 * @return
	 */
	def static String convertListToString (List listData, String concatStr = "|"){
		def strRet = ""
		if(concatStr == null || concatStr == ""){
			concatStr = "|"
		}
		if(listData != null){
			listData.each {key->
				strRet += key + concatStr
			}
			if (strRet != "") {
				strRet = strRet.subSequence(0, strRet.length() - concatStr.length())
			}
		}
		return strRet
	}
}