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
		//Insert to Mongo
		def mongo = new GMongo(MONGODB_HOST, MONGODB_PORT)
		//def mongo = new GMongo()
		def db = mongo.getDB(MONGODB_DBNAME)
		//Get data of Job
		def contentText = req.getInputStream().getText()
		//Parse to Json
		def obj = JSON.parse(contentText)
		def jobName = obj.sourceJob
		def istIid = obj.istIid
		def col = db.getCollection(jobName + "." + istIid)
		//def col = db.getCollection(jobName)
		col.insert(obj)
		mongo.close()
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
