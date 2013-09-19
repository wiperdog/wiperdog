import javax.servlet.ServletException
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.json.*
public class DBConfigurationServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setContentType("json")
			resp.addHeader("Access-Control-Allow-Origin", "*")
			def shell = new GroovyShell()
			def fileParams
			def params = []
			def properties = MonitorJobConfigLoader.getProperties()
			fileParams = new File(properties.get(ResourceConstants.DEFAULT_PARAMETERS_DIRECTORY) + "/default.params")
			if (fileParams.exists()) {
				params = shell.evaluate(fileParams)
			}
			PrintWriter out = resp.getWriter()
			def builder = new JsonBuilder(params.dbinfo)
		    out.print(builder.toString());
		} catch(Exception ex) {
			println ex
		}
	}

	@Override
	void doPut(HttpServletRequest req, HttpServletResponse resp) {
			resp.setContentType("json")
			resp.addHeader("Access-Control-Allow-Origin", "*")
			def contentText = req.getInputStream().getText()
	}

	@Override
	void doPost(HttpServletRequest req, HttpServletResponse resp) {
		    def message = [:]

		    resp.setContentType("json")
			resp.addHeader("Access-Control-Allow-Origin", "*")
			def shell = new GroovyShell()
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
			def object = slurper.parseText(contentText)
			def properties = MonitorJobConfigLoader.getProperties()
			def params = [:]
		try {
				def fileParams = new File(properties.get(ResourceConstants.DEFAULT_PARAMETERS_DIRECTORY) + "/default.params")
				if (fileParams.exists()) {
					params = shell.evaluate(fileParams)
					params.dbinfo = object
				} else {
					fileParams.createNewFile()
					params["dbinfo"] = object
				}

				FileWriter fw = new FileWriter(fileParams)
				BufferedWriter bw = new BufferedWriter(fw);
				def builder = new JsonBuilder(params)
				def str_params =  builder.toPrettyString().replaceAll("\\{","\\[").replaceAll("\\}","\\]")
				bw.write(str_params);
				bw.close();
				message = [status:"success"]
			}catch(Exception ex){
				println  " Error to write file" + ex
				message = [status:"failed"]
			}
			PrintWriter out = resp.getWriter()
			def builder2 = new JsonBuilder(message)
			out.print(builder2.toPrettyString())
	}
}
def DBConfig
try {
	DBConfig = new DBConfigurationServlet()
} catch (e) {

}

if (DBConfig != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/DBConfigServlet"

	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", DBConfig, props)
}
