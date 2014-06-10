import groovy.json.*;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;
import java.util.jar.JarFile;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference
import org.osgi.util.tracker.ServiceTracker
import org.osgi.util.tracker.ServiceTrackerCustomizer
import java.text.SimpleDateFormat;
import java.util.regex.Matcher
import java.util.regex.Pattern

class JobRunOneShot{
	def properties = MonitorJobConfigLoader.getProperties()
	BundleContext ctx;
	JsonSlurper sluper = new JsonSlurper()
	def jobResult
	JobRunOneShot(BundleContext ctx){
		this.ctx = ctx;
	}
	public Object create(Request request, Response response){
		def listData = []
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		response.setContentType("application/json")
		//Get list file from xwiki
		def dataReq =  (new ChannelBufferInputStream(request.getBody())).getText()
		def objectData = sluper.parseText(dataReq)
		def jobFilePath 
		def schedule 

		if(objectData != null ) {
			if(objectData.job != null) {
				jobFilePath = objectData.job
			} else {
				println "Missing job file path"
				return null
			}
			schedule = objectData.schedule
		}

		if(jobFilePath != null && !jobFilePath.equals("")) {

		}
		def jobRunnerMain
		jobResult = null
		def timeout = 60000 
		def expirationTime = System.currentTimeMillis() + timeout
		def currentTime = System.currentTimeMillis()
		def startRunJobTime = System.currentTimeMillis()
		while(jobRunnerMain == null ) {
			jobRunnerMain = ctx.getService(ctx.getServiceReference("JobRunnerMainService"))	
			if(jobRunnerMain != null ) {
				jobRunnerMain.removeJob(jobFilePath)
				jobRunnerMain.executeJob(jobFilePath,schedule)
			}
		}
		//Set timeout to waiting for job result in 60s
		while(jobResult == null && currentTime < expirationTime) {
			currentTime = System.currentTimeMillis()
			Thread.sleep(1000)
		}
		def returnData = [:]

		if(jobResult == null ) {
			StringBuilder logStr = new StringBuilder()
			//Get log message from wiperdog/log
			def logFile = new File(System.getProperty("felix.home") + File.separator + "log" + File.separator + "wiperdog.log")
			Pattern pattern = Pattern.compile("\\[\\d{4}\\/\\d{2}\\/\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d{3}\\]");
			SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS")
			def acceptLineNum 
			logFile.eachLine{ line,line_num ->
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					def logTime = matcher[0].replace("[","").replace("]","")
					def logTimeInMilis = sf.parse(logTime).getTime()
					if( ( logTimeInMilis >= startRunJobTime ) &&  ( logTimeInMilis <= expirationTime ) ) {
						acceptLineNum = line_num
						logStr.append(line + "\n")
					} else {
						if( logTimeInMilis > expirationTime ) {
							acceptLineNum = null
						}
					}
				} else {
					if(acceptLineNum != null && line_num > acceptLineNum ) {
						logStr.append(line + "\n")	
					}
				}
			}
			returnData["log"] = logStr.toString()
		} else {
			returnData["data"] = jobResult
		}
		return returnData
	}
	//runjob/data
	public String update(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		def responseData = [:]
		def dataReq =  (new ChannelBufferInputStream(request.getBody())).getText()
		def objectData = sluper.parseText(dataReq)		
		def builder = new JsonBuilder(objectData)
		jobResult = builder.toPrettyString()
		return null
	}

	public Map<String,Object> read(Request request, Response response){
		return objectData
	}
	public String delete(Request request, Response response){
		return null
	}
}
