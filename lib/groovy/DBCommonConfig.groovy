import java.io.IOException;
import groovy.json.*
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DBCommonConfigServlet extends HttpServlet{

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
				def builderListJob = new JsonBuilder(list_job)
				out.println(builderListJob.toPrettyString());
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
				def builderParams = new JsonBuilder(params)
				out.println(builderParams.toPrettyString());
			}
		}catch(Exception ex){
			println ex;
		}
	}

	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
				resp.setContentType("text/html")
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

def dbCommonServlet
try {
	dbCommonServlet = new DBCommonConfigServlet()
} catch (e) {

}

if (dbCommonServlet != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/DBCommonConfig"

	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", dbCommonServlet, props)
}
