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
	def properties = MonitorJobConfigLoader.getProperties()
	static final String JOB_DIR = "/var/job/"
	static final String HOMEPATH = System.getProperty("felix.home")
	static final String PARAMFILE = "var/conf/default.params"
	static final String CHARSET = 'utf-8'

	void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		try {
			def jobFileName = req.getParameter("jobFileName")
			if(jobFileName != null && jobFileName != "") {
				def jobPath = JOB_DIR + jobFileName
				def jobFile = new File(HOMEPATH, jobPath)
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
					def  job_dir = new File(properties.get(ResourceConstants.JOB_DIRECTORY))
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
						"MySQL",
						"SQL_Server",
						"Postgres"
					]
					def child = [
						"Database_Area",
						"Database_Statistic",
						"Database_Structure",
						"FaultManagement",
						"Performance",
						"Proactive_Check",
						"Others"
					]

					def output = [:]
					root.each{
						r->
						output[r]=[:]
						child.each{
							c->
							output[r][c] = []
						}
					}

					def tmpOther = []
					list_job.each {
						def tmpArray = it.split("\\.")
						if (tmpArray.size() == 4) {
							if (root.contains(tmpArray[0])) {
								if (child.contains(tmpArray[1])) {
									output[tmpArray[0]][tmpArray[1]].add(it)
								} else {
									output[tmpArray[0]]["Others"].add(it)
								}
							} else {
								tmpOther.add(it)
							}
						} else {
							tmpOther.add(it)
						}
					}
					output["Other"] = tmpOther
					def builderListJob = new JsonBuilder(output)
					out.println(builderListJob.toPrettyString());
				}
			}
		}
		catch(Exception e) {
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
			def jobDirPath = HOMEPATH + JOB_DIR
			if(action == 'run') {
				jobContent = getTestJobScript(object['data'])
				jobDirPath = HOMEPATH + JOB_DIR + "tmpTestJob/"
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