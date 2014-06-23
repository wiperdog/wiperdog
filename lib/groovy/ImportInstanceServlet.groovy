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

public class ImportInstance extends HttpServlet {
	def properties = MonitorJobConfigLoader.getProperties()
	static final String JOB_DIR = "/var/job/"
	static final String HOMEPATH = System.getProperty("felix.home")
	static final String CHARSET = 'utf-8'
	def shell = new GroovyShell()

	void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		resp.setContentType("text/html")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		def respMessage = [:]
		def shell = new GroovyShell()
		try {
			def action = req.getParameter("action")
			if(action == "getListJob"){
				def data2CreateMenu = GenerateTreeMenu.getData2CreateMenu(properties.get(ResourceConstants.JOB_DIRECTORY))
				def treeItem = GenerateTreeMenu.getMenuItemsStr(data2CreateMenu['root'], data2CreateMenu['output'])
				respMessage["data"] = treeItem
				respMessage["status"] = "success"
			} else {
				if(action == "getInstance"){
					def jobFileName = req.getParameter("jobFileName")
					if(jobFileName != null && jobFileName != "") {
						File instanceFile = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/" + jobFileName.substring(0,jobFileName.lastIndexOf(".job")) + ".instances")
						def instanceObj = null
						def builderInstances = null
						if(instanceFile.exists()){
							instanceObj = shell.evaluate(instanceFile)
							respMessage["data"] = instanceObj
							respMessage["status"] = "success"
						} else {
							respMessage["data"] = ""
							respMessage["status"] = "failed"
						}
					}
				}
			}
		}
		catch(Exception e) {
			println e
			respMessage["data"] = null
			respMessage["status"] = "failed"
		}
		def builderMessage = new JsonBuilder(respMessage)
		out.print(builderMessage.toPrettyString())
	}

	@Override
	void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		def respMessage = [:]
		PrintWriter out = resp.getWriter()
		def builder = null
		try{
			def resultRun	= ""
			def contentText = req.getInputStream().getText()
			def jobFileName = req.getParameter("jobFileName")
			def slurper = new JsonSlurper()
			def object = slurper.parseText(contentText)
			def csvFile

			if(req.getParameter("action") == "exportFromJobConfig") {
				if(jobFileName == "undefined") {
					jobFileName = "listInstanceFile"
				}
				if(jobFileName.lastIndexOf(".job") > -1) {
					jobFileName = jobFileName.substring(0,jobFileName.lastIndexOf(".job"))
				}
				try {
					csvFile = writeDataToCSVFile(object, jobFileName)
					respMessage["status"] = "OK"
					respMessage["message"] = "Write data of Instance to CSV file at path: <br/>" + csvFile
				}
				catch(Exception ex) {
					respMessage["status"] = "failed"
					respMessage["message"] = ex.toString()
				}
				builder = new JsonBuilder(respMessage)
				out.print(builder.toString())
				return
			}
			def instanceFile = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/" + jobFileName.substring(0,jobFileName.lastIndexOf(".job")) + ".instances")
			def mapInstances = [:]
			object.each{
				def tmpMap = [:]
				if(it['PARAMS'] != null && it['PARAMS'] != [:]){
					tmpMap['params'] = it['PARAMS']
				}
				if(it['SCHEDULE'] != null && it['SCHEDULE'] != "")  {
					tmpMap['schedule'] = it['SCHEDULE']
				}
				mapInstances[it['INST_NAME']] = tmpMap
			}
			if(req.getParameter("action") == "exportCSV"){
				def csvFileName = jobFileName.substring(0,jobFileName.lastIndexOf(".job"))
				csvFile = writeDataToCSVFile(mapInstances,csvFileName)
				respMessage["status"] = "success"
				respMessage["filePath"] = csvFile.toString()
				builder = new JsonBuilder(respMessage)
				out.print(builder.toPrettyString())
				return
			}

			builder = new JsonBuilder(mapInstances)
			def stringToFile = builder.toPrettyString().replace("{","[").replace("}","]")
			if(!instanceFile.exists()){
				instanceFile.createNewFile()
			}
			instanceFile.setText(regularExpressionValidate(stringToFile))
			respMessage["status"] = "success"
			respMessage["filePath"] = instanceFile.getCanonicalPath()
		}
		catch(Exception ex){
			println ex
			respMessage["status"] = "failed"
		}
		builder = new JsonBuilder(respMessage)
		out.print(builder.toString())
	}
	
	def writeDataToCSVFile(data, fileNameCSV) {
		def tmpMap
		def tmpListData = []
		
		//From instances info, convert to [Instance, schedule, params] format (tmpListData)
		data.each{
			tmpMap = [:]
			tmpMap['Instance'] = it.key
			tmpMap += it.value
			tmpListData.add(tmpMap)
		}
		
		//Get all key of params (listKeyParams)
		def listKeyParams = []
		tmpListData.each{fd->
			def paramMap
			if(fd.params instanceof String) {
				paramMap = (new GroovyShell()).evaluate(fd.params)
			} else {
				paramMap = fd.params
			}
			paramMap.each{pr->
				if(!listKeyParams.contains(pr.key)){
					listKeyParams.add(pr.key)
				}
			}
		}
		
		//Convert to create data with format [INST_NAME, SCHEDULE, {param key of list params}]
		def mapData
		def mapParam
		def listData = []
		tmpListData.each{dat->
			mapData = [:]
			mapParam = [:]
			mapData['INST_NAME'] = dat.Instance
			if(dat.schedule != null) {
				mapData['SCHEDULE'] = dat.schedule
			} else {
				mapData['SCHEDULE'] = ""
			}
			if(dat.params instanceof String) {
				mapParam = (new GroovyShell()).evaluate(dat.params)
			} else {
				if(dat.params != null){
					mapParam = dat.params
				}
			}
			listKeyParams.each{lk->
				if(mapParam != [:]){
					mapParam.each{mp->
						if(lk == mp.key){
							mapData[mp.key] = mp.value
							return
						} else {
							mapData[lk] = ""
						}
					}
				} else {
					mapData[lk] = ""
				}
			}
			mapData += mapParam
			listData.add(mapData)
		}
		def listKeyCSV = ["INST_NAME", "SCHEDULE"]
		listKeyCSV += listKeyParams
		def listValueCSV
		def listDataCSV = []
		listDataCSV.add(listKeyCSV)
		listData.each{ld->
			listValueCSV = []
			ld.each{
				listValueCSV.add(it.value)
			}
			listDataCSV.add(listValueCSV)
		}
		def tmpStr = ""
		listDataCSV.each{csv->
			csv.each{cs->
				if(cs instanceof String) {
					tmpStr += cs + ","
				} else {
					throw new Exception("Can not support instance data with params which is not a string as : " + cs);
				}
			}
			tmpStr = tmpStr.substring(0, tmpStr.lastIndexOf(","))
			tmpStr += "\n"
		}
		def fileCSV = writeToFile(HOMEPATH, JOB_DIR + "${fileNameCSV}.csv", tmpStr)

		return fileCSV
	}
	def writeToFile(filePath, fileName, data) {
		def dataFile = new File(filePath, fileName);
		dataFile.write(data, CHARSET)
		return dataFile.getCanonicalPath()
	}
	def regularExpressionValidate(String object){
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
}
def importInstance
try {
	importInstance = new ImportInstance()
} catch (e) {
	println e
}

if (importInstance != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/ImportInstanceServlet"
	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", importInstance, props)
}