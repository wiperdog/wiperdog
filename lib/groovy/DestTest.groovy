import java.io.IOException;
import groovy.json.*

import com.gmongo.GMongo
import com.mongodb.util.JSON

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DestTestServlet extends HttpServlet{

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html")
		def struser = req.getParameter("user")
		PrintWriter out = resp.getWriter()
		out.println("Put to insert data in MongoDB!!!");	
	}
	
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
				resp.setContentType("text/html")
		try{
			def mapMongoDb = MongoDBConnection.getWiperdogConnection()
			def mongo = mapMongoDb['gmongo']
			def db = mapMongoDb['db']
			if(db != null){
				//Get data of Job
				def contentText = req.getInputStream().getText()
				//Parse to Json
				def obj = JSON.parse(contentText)
				// Get collection in mongodb
				def jobName = obj.sourceJob
				def istIid = obj.istIid
				def col = db.getCollection(jobName + "." + istIid)
				// Insert data to collection
				col.insert(obj)
			}else{
				println "tfm_test Servlet: Cannot connect to MongoDB!"
			}
		}catch(Exception ex){
			println ex
		}
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
