
// To download GMongo on the fly and put it at classpath
@Grab(group='com.gmongo', module='gmongo', version='1.0')
import com.gmongo.GMongo
import com.mongodb.util.JSON

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

public class MongoStore extends HttpServlet {
	// Instantiate a com.gmongo.GMongo object instead of com.mongodb.Mongo
	// The same constructors and methods are available here
	def mongo = new GMongo()
	
	void doPut(HttpServletRequest req, HttpServletResponse resp) {
		// Get a db reference in the old fashion way
		def db = mongo.getDB("wiperdog")
		
		def contentText = req.getInputStream().getText()
		assert contentText != null
		assert contentText.length() > 0
		
		// prepare data
		def obj = JSON.parse(contentText)
		
		assert obj != null
		
		// Insert a document
		def jobName = obj.sourceJob
		def col = db[jobName]
		col.insert(obj)
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().println('{ "result" : "ok" }')
	}
}


