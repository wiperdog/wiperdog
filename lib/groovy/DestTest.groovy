import java.io.IOException;
import groovy.json.*

import com.gmongo.GMongo
import com.mongodb.util.JSON

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DestTestServlet extends HttpServlet{
	static final String MONGODB_HOST = "153.122.22.111"
	static final int MONGODB_PORT = 27017
	static final String MONGODB_DBNAME = "wiperdog"
	def properties = MonitorJobConfigLoader.getProperties()
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")

		PrintWriter out = resp.getWriter()
		def list_job = []
		try{
			def strDBType = req.getParameter("dbtype")
			def strJobName = req.getParameter("jobName")
            if (strDBType != null && strDBType != ""){
				def  job_dir = new File(properties.get(ResourceConstants.JOB_DIRECTORY))
				if(job_dir.isDirectory()){
					job_dir.listFiles().each{
						def fileName = it.getName()
						if( fileName.startsWith(strDBType) && fileName.endsWith('.job')){
							list_job.add(fileName.substring(0,fileName.indexOf('.job')))
						}
					}
				} else {
				  println "Job directory not found !"
				}
				out.println(JSON.serialize(list_job));
			}

			if (strJobName != null && strJobName != ""){
				def shell = new GroovyShell()
				def params = [:]
				def jobFileParams = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/${strJobName}.params")
				def jobFileInstance = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/${strJobName}.instances")
				if (jobFileParams.exists()) {
					params['params'] = shell.evaluate(jobFileParams)
				}else{
					params['params'] = [:]
				}
				if (jobFileInstance.exists()) {
					params['instances'] = shell.evaluate(jobFileInstance)
				}else{
					params['instances'] = [:]
				}
				out.println(JSON.serialize(params))
			}
		}catch(Exception ex){
			println ex;
		}
	}

	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
				resp.setContentType("text/html")
		//Insert to Mongo
		def mongo = new GMongo(MONGODB_HOST, MONGODB_PORT)
		def db = mongo.getDB(MONGODB_DBNAME)
		//Get data of Job
		def contentText = req.getInputStream().getText()
		//Parse to Json
		def obj = JSON.parse(contentText)
		def jobName = obj.sourceJob
		def istIid = obj.istIid
		def col = db.getCollection(jobName + "." + istIid)
		col.insert(obj)
		mongo.close()
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
				resp.setContentType("text/html")
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")

		def properties = MonitorJobConfigLoader.getProperties()
		def contentText = req.getInputStream().getText()
		def slurper = new JsonSlurper()
      	def object = slurper.parseText(contentText)
		def strJobName = object.job

		def jobFileParams = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/${strJobName}.params")
		def jobFileInstance = new File(properties.get(ResourceConstants.JOB_DIRECTORY) + "/${strJobName}.instances")

		FileWriter fw = new FileWriter(jobFileParams)
        BufferedWriter bw = new BufferedWriter(fw);
		def builder = new JsonBuilder(object.data.params)
		def str_params = builder.toPrettyString().replaceAll("\\{","\\[").replaceAll("\\}","\\]").replaceAll("\\\\", "\\\\\\\\")
        bw.write(str_params);
        bw.close();

        FileWriter fwInstance = new FileWriter(jobFileInstance)
        BufferedWriter bwInstance = new BufferedWriter(fwInstance);
		def builderInstance = new JsonBuilder(object.data.instances)
		def str_paramsInstance = builderInstance.toPrettyString().replaceAll("\\{","\\[").replaceAll("\\}","\\]").replaceAll("\\\\", "\\\\\\\\")
        bwInstance.write(str_paramsInstance);
        bwInstance.close();

		PrintWriter out = resp.getWriter()
		def message = [:]
		message["status"] = "OK"
		def builder2 = new JsonBuilder(message)
		out.print(builder2.toPrettyString())
	}
}

def servletRoot
try {
	servletRoot = new DestTestServlet()
} catch (e) {

}

if (servletRoot != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/spool/tfm_test"

	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", servletRoot, props)
}
