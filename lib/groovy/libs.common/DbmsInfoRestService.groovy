import groovy.json.JsonSlurper;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;
import java.util.jar.JarFile;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.osgi.framework.BundleContext;

class DbmsInfoRestService {
	def dbmsInfoFile = new File(MonitorJobConfigLoader.getProperties().get(ResourceConstants.DBMS_INFO))
	def static dbmsInfo = null
	def shell = new GroovyShell()
	BundleContext ctx;

	DbmsInfoRestService(BundleContext ctx){
		this.ctx = ctx;
	}

	public def create(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		def responseData
		if (dbmsInfo == null) {
			dbmsInfo = shell.evaluate(dbmsInfoFile.getText())
			responseData = dbmsInfo['DbType']
		} else {
			responseData = dbmsInfo['DbType']
		}
		return responseData
	}

	public String update(Request request, Response response){
		println "update"
		return "update_METHOD"
	}

	public def read(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		def responseData
		def status = request.getRawHeader("status")
		if (dbmsInfo == null) {
			dbmsInfo = shell.evaluate(dbmsInfoFile.getText())
		}
		if(status == null){
		    responseData = dbmsInfo['DbType'].keySet()
		} else if(status == "all"){
			responseData = dbmsInfo
		}
		return responseData
	}

	public String delete(Request request, Response response){
		println "delete"
		return "delete_METHOD"
	}
}
