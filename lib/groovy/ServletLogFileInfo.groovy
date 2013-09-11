import java.io.IOException;
import groovy.json.*
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LogFileInformation extends HttpServlet{
	def properties = MonitorJobConfigLoader.getProperties()
	static final int LIMIT_LINES = 200
	static final int MORE_LINES = 100

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
		def message = [:]
		try {
			// Get name of log file
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
			def object = slurper.parseText(contentText)

			// Get data to response
			def lstContent = []
			def logFileName = null
			if(object.log != null) {
				logFileName = object.log
				def logPath = properties.get(ResourceConstants.LOG_DIRECTORY) + "/" + logFileName
				def logFile = new File(logPath)
				def contentLog = [:]
				def linesLimit = 0
				if(object.lines == null) {
					linesLimit = LogFileInformation.LIMIT_LINES
				} else {
					linesLimit += object.lines
				}
				if(object.action == 'moreLogs'){
					linesLimit += LogFileInformation.MORE_LINES
				}
				contentLog['logContent'] = getDataInLog(logFile,linesLimit)
				contentLog['lines'] = linesLimit

				def builderLogs = new JsonBuilder(contentLog)
				out.print(builderLogs.toString());
			}

			// Delete log file
			def deleteFileName = null
			if(object.delete != null) {
				deleteFileName = object.delete
				def logPath = properties.get(ResourceConstants.LOG_DIRECTORY) + "/" + deleteFileName
				def deleteFile = new File(logPath)
				if(deleteFile.delete()){
					message["status"] = deleteFileName + " was deleted"
				}else{
					message["status"] = "Delete operation was failed."
				}
				def builder = new JsonBuilder(message)
				out.print(builder.toPrettyString())
			}
		} catch (Exception ex) {
			println "ERROR DOPOST: " + ex
			message["status"] = "failed"
			def builder = new JsonBuilder(message)
			out.print(builder.toPrettyString())
		}
	}
	public String getDataInLog(File file, int numOfLines) throws Exception {
		List<String> lines = new ArrayList<>();
		try {
			java.io.RandomAccessFile f = new RandomAccessFile(file, "r")
			ByteArrayOutputStream bout = new ByteArrayOutputStream()
			long length = f.length()
			for (long p = length - 1; p > 0 && lines.size() < numOfLines; p--) {
				f.seek(p);
				int b = f.read();
				if (b == 10) {
					if (p < length - 1) {
						lines.add(0, getLine(bout));
						bout.reset();
					}
				} else if (b != 13) {
					bout.write(b);
				}
			}
		}catch (Exception ex){
			println ex
		}
		def logLines = ""
		lines.each{ logLines += it + "\n" }
		return logLines
	}

	static String getLine(ByteArrayOutputStream bout) {
		byte[] a = bout.toByteArray();
		return new String(a).reverse();
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
