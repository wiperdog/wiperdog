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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity 

public class TestJob extends HttpServlet {
	static final String JOB_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOB_DIRECTORY)
	static final String PARAMFILE = "var/conf/default.params"
	static final String CHARSET = 'utf-8'

	void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
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
					def data2CreateMenu = GenerateTreeMenu.getData2CreateMenu(JOB_DIR)
					def treeItem = GenerateTreeMenu.getMenuItemsStr(data2CreateMenu['root'], data2CreateMenu['output'])
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
			def jobDirPath = JOB_DIR
			if(action == 'run') {
				jobContent = getTestJobScript(object['data'])
				jobDirPath = JOB_DIR +"/tmpTestJob"
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
				def restPort = System.getProperty("rest.port")
				String url = "http://localhost:${restPort}/runjob"
				HttpClient client = new DefaultHttpClient();
				// initialze a new builder and give a default URL
				def postBody = [job: jobFile.getCanonicalPath()]
				def strBody = (new JsonBuilder(postBody)).toString()
				HttpPost post = new HttpPost(url);
				post.addHeader("accept", "application/json");
				post.addHeader("Connection", "close");
				post.addHeader("Access-Control-Allow-Origin", "*")
				StringEntity se = new StringEntity(strBody); 
				post.setEntity(se);
				def response = client.execute(post);
		        def responseData = getTextFromStream(response.getEntity().getContent())
		        def objData = slurper.parseText(responseData)
				if(objData != null && objData.data != null) {
					respMessage = ["status":"success" , "jobData":objData.data,"log" : ""]
				} else {
					if(objData != null && objData.log != null ) {
						respMessage = ["status":"failed" , "jobData": "","log": objData.log]
					} else {
						respMessage = ["status":"failed" , "jobData": "","log": "Failed to run job - Unknown error"]
					}
					
				}
				
				jobFile.delete()
			} else {
				respMessage = ["status":"success", "log": ""]
			}
		}
		catch(Exception e) {
			e.printStackTrace()
			respMessage = ["status":"failed","jobData":e , "log": ""]
		}

		def builder = new JsonBuilder(respMessage)
		out.print(builder.toPrettyString())
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
	def getTextFromStream (InputStream inputStream ){
		BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
		StringBuffer result = new StringBuffer();
		String line = null;
		def returnStr = ""
		while ((line = rd.readLine()) != null) {
			returnStr+= line
		}
		inputStream.close()
		return returnStr

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
