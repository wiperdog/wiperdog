import groovy.json.JsonSlurper;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;
import java.util.jar.JarFile;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.osgi.framework.BundleContext;

class DbmsInfoRestService {
	def dbmsInfoFile = new File(MonitorJobConfigLoader.getProperties().get(ResourceConstants.USE_FOR_XWIKI))
	def static dbmsInfo = null
	def shell = new GroovyShell()
	BundleContext ctx;

	DbmsInfoRestService(BundleContext ctx){
		this.ctx = ctx;
	}

	public def create(Request request, Response response){
		return "create_METHOD"
	}

	public String update(Request request, Response response){
		return "update_METHOD"
	}

	public def read(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		def responseData
		def keyConfigXwiki = request.getUrlDecodedHeader("keyConfigXwiki")
		if (dbmsInfo == null) {
			dbmsInfo = shell.evaluate(dbmsInfoFile.getText())
		}
		if (keyConfigXwiki == null) {
			responseData = dbmsInfo
		} else {
			responseData = dbmsInfo[keyConfigXwiki]
		}
		return responseData
	}

	public String delete(Request request, Response response){
		return "delete_METHOD"
	}
}
