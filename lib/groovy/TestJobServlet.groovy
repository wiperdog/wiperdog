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

public class TestJob extends HttpServlet {
	static final String JOB_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOB_DIRECTORY)
	static final String PARAMFILE = "var/conf/default.params"
	static final String CHARSET = 'utf-8'

	void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		def job_dir = new File(JOB_DIR)
		try {
			def jobFileName = req.getParameter("jobFileName")
			if(jobFileName != null && jobFileName != "") {
				def jobPath = JOB_DIR + "/" + jobFileName
				def jobFile = new File(jobPath)
				def stringOfJob = ""
				jobFile.eachLine{
					stringOfJob+= it + "\n"
				}
				def mapData = [:]
				mapData['jobContent'] = stringOfJob
				def builder = new JsonBuilder(mapData)
				out.print(builder.toString())
			} else {
				if(jobFileName == null){
					def list_job = []
					if(job_dir.isDirectory()){
						job_dir.listFiles().each {
							file ->
							def fileName = file.getName()
							if(fileName.endsWith('.job')){
								list_job.add(fileName)
							}
						}
					}
					def root = [
						"MySQL": ["Database_Area":[],"Database_Statistic":[],"Database_Structure":[],"FaultManagement":[],"Performance":[],"Proactive_Check":[],"Others":[]],
						"SQL_Server":["Database_Area":[],"Database_Statistic":[],"Database_Structure":[],"FaultManagement":[],"Performance":[],"Proactive_Check":[],"Others":[]],
						"Postgres":["Database_Area":[],"Database_Statistic":[],"Database_Structure":[],"FaultManagement":[],"Performance":[],"Proactive_Check":[],"Others":[]],
						"OS":[],
						"Others":[]
					]
					
					//Initial
					def output = [:]
					def tmpKey = ""
					root.each{k,v->
						if (!v.isEmpty()) {
							output[k]=[:]
							v.each{c, valuec->
								tmpKey = k + "." + c
								output[tmpKey] = []
							}
						} else {
							output[k] = []
						}
					}
					
					//Bind job name to create tree menu
					def isOthersJob
					def isOthersJobInGroup
					list_job.each {
						isOthersJob = true
						def tmpArray = it.split("\\.")
						if (tmpArray.size() >= 2) {
							root.each{k,v->
								if (tmpArray[0] == k) {
									if (!v.isEmpty()) {
										if (tmpArray.size() >= 3) {
											isOthersJobInGroup = true
											v.each{c, valuec->
												if (tmpArray[1] == c) {
													tmpKey = k + "." + c
													output[tmpKey].add(it)
													isOthersJobInGroup = false
													//Set to not add in others group
													isOthersJob = false
												}
											}
											if (isOthersJobInGroup) {
												tmpKey = k + ".Others"
												output[tmpKey].add(it)
												//Set to not add in others group
												isOthersJob = false
											}
										}
									} else {
										if ((tmpArray.size() == 3) && (output[k] instanceof List)) {
											output[k].add(it)
											//Set to not add in others group
											isOthersJob = false
										}
									}
								}
							}
						}
						if (isOthersJob) {
							output["Others"].add(it)
						}
					}
					def treeItem = getMenuItemsStr(root, output)
					def builderListJob = new JsonBuilder(treeItem)
					out.println(builderListJob.toPrettyString());
				}
			}
		} catch(Exception e) {
			println e
		}
	}

	@Override
	void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")

		PrintWriter out = resp.getWriter()
		def respMessage = [:]
		def resultRun	= ""

		try {
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
			def object = slurper.parseText(contentText)
			def jobFileName = object['jobFileName']
			def jobContent = object['data']
			def mapJob = getTestJobScript("")
			def action = object['action']
			def jobDirPath = ""
			if(action == 'run') {
				jobContent = getTestJobScript(object['data'])
				jobDirPath = JOB_DIR +"/tmpTestJob/"
			}
			File jobDir = new File(jobDirPath)
			if(!jobDir.exists()){
				jobDir.mkdirs()
			}
			def jobPath = jobDir.toString() +"/"+ jobFileName
			def jobFile = new File(jobPath)
			if(!jobFile.exists()){
				jobFile.createNewFile()
			}
			//Save to file
			writeToFile(jobFile, jobContent)

			if(action == 'run') {
				def command = ""
				if (System.properties['os.name'].toLowerCase().contains('windows')) {
					command = 'cmd /c jobrunner -f ' + jobFile.toString()
				} else {
					command = './jobrunner.sh -f ' + jobFile.toString()
				}
				Process p = command.execute();
				InputStream is = p.getInputStream()
				// Get encoding
				def encoding = "SJIS"
				InputStreamReader isr = new InputStreamReader(is, encoding)
				BufferedReader reader = new BufferedReader(isr)
				String resultData = ""
				String line = reader.readLine();
				while(line != null) {
					resultData+= line + "\n"
					line = reader.readLine()
				}
				//Create macher to get Job data
				String macherPattern = "(\\n\\{\\n)(.*)(\\n\\}\\n)"
				Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
				Matcher matcher = pattern.matcher(resultData);

				String jobData = ""
				while(matcher.find())
				{
					jobData = matcher.group(2)
				}
				respMessage = ["status":"success","jobData":jobData , "log": resultData]
				jobFile.delete()
			} else {
				respMessage = ["status":"success","jobData":"" , "log": ""]
			}
		}
		catch(Exception e) {
			println e
			respMessage = ["status":"failed","jobData":e , "log": ""]
		}

		def builder = new JsonBuilder(respMessage)
		out.print(builder.toString())
	}
	
	/**
	 * Recursively function, used for gen tree menu data 
	 * Input treeItem: root tree map of menu (not leaf)
	 *       mapCollection: Map of collections, Item of map has key is a job group and value is list job which is applied for that group
	 *       parentList: used for recursively to canculate key if data if leaf
	 * Output: If data is leaf, create key of group data, get list of job which is applied for group from mapCollection and create menu Item
	 *         If data isn't leaf, create node and call recursively function with sub data
	 **/
	def getMenuItemsStr(treeItem, mapCollection, parentList = []) {     
	     def ul_open = false
	     def result = ""
	     def parentStr = ""
	     def parentLstforChild = []

	     //If data isn't leaf, create node and call recursively function with sub data
	     if (treeItem instanceof Map) {
	         result += "<ul id='treemenu2' class='treeview'>"
	         treeItem.each{itemKey, itemVal -> 
	             parentList.each{parentListItem->
	                 parentLstforChild.add(parentListItem)
	             }
	             parentLstforChild.add(itemKey)
		     result += "<li>"+ itemKey
	             result += getMenuItemsStr(itemVal, mapCollection, parentLstforChild)
	             result +="</li>"
	             parentLstforChild = []
	         }
	         result += "</ul>"
	     }
	     
	     //If data is leaf, create key of group data, get list of job which is applied for group from mapCollection and create menu Item
	     if (treeItem instanceof List) {
	         result += "<ul>"
	         parentList.each{parentItem -> 
	              if (parentStr != ""){
	                  parentStr += "."
	              }
	              parentStr += parentItem
	         }
	         if (mapCollection[parentStr] != null) {
	             mapCollection[parentStr].each {item->
	                 result += "<li><a>" + item +"</a></li>"
	             }
	         }
	         result += "</ul>"
	     }
	     return result
	}
	
	def getTestJobScript(stringOfJob){
		List listKey = [
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
			"KEYEXPR._description",
			"SENDTYPE",
			"RESOURCEID",
			"MONITORINGTYPE",
			"DBTYPE",
			"DEST",
			"HOSTID",
			"SID"
		]

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
			mapResult[matcher.group(1)] = matcher.group(3)
		}
		mapResult["DEST"] = "[[file:\"stdout\"]]\n"

		String testJobStr = ""
		mapResult.each{key, value->
			testJobStr += key + " = " + value
		}
		return testJobStr
	}

	/**
	 * Write data to file with CHARSET encode
	 * @param paramFile
	 * @return
	 */
	def writeToFile(dataFile, data) {
		dataFile.write(data, CHARSET)
	}
}

def testJobServlet
try {
	testJobServlet = new TestJob()
} catch (e) {
	println e
}

if (testJobServlet != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/TestJobServlet"
	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", testJobServlet, props)
}