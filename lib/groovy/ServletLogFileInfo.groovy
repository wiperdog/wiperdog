import java.io.IOException;
import groovy.json.*
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LogFileInformation extends HttpServlet{
	def properties = MonitorJobConfigLoader.getProperties()
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		try{
			// Get list log file
			def logDirectory = new File(properties.get(ResourceConstants.LOG_DIRECTORY))
			def lstLogFile = []
			if(logDirectory.isDirectory()){
				logDirectory.listFiles().each {file ->
					def logFileName = file.getName()
					if(logFileName.contains("wiperdog")) {
						lstLogFile.add(logFileName)
					}
				}
			}
			def builderListLogFile = new JsonBuilder(lstLogFile)
			out.println(builderListLogFile.toPrettyString());
		}catch(Exception ex){
			println "ERROR DOGET: " + ex;
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
				resp.setContentType("text/html")
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html")
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		try {
			// Get name of log file
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
	      	def object = slurper.parseText(contentText)
			def logFileName = object.log
			def logPath = properties.get(ResourceConstants.LOG_DIRECTORY) + "/" + logFileName
			def logFile = new File(logPath)
			def lstContent = []
			logFile.eachLine {line ->
				lstContent.add(line)
			}
			def contentLog = new JsonBuilder(lstContent)
			out.print(contentLog.toString());
		} catch (Exception ex) {
			println "ERROR DOPOST: " + ex
			def message = [status:"failed"]
			def builder = new JsonBuilder(message)
			out.print(builder.toPrettyString())
		}
	}
}

def logFileServlet
try {
	logFileServlet = new LogFileInformation()
} catch (e) {

}

if (logFileServlet != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/LogFileInfoServlet"
	
	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", logFileServlet, props)
}
