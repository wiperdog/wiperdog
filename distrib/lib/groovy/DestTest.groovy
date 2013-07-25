/*
 *  Copyright 2013 Insight technology,inc. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import java.io.IOException;
import groovy.json.*

import com.gmongo.GMongo
import com.mongodb.util.JSON

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DestTestServlet extends HttpServlet{
	static final String MONGODB_HOST = "10.0.0.24"
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
		def db = mongo.getDB(MONGODB_DBNAME)
		//Get data of Job
		def contentText = req.getInputStream().getText()
		//Parse to Json
		def obj = JSON.parse(contentText)
		def jobName = obj.sourceJob
		def col = db.getCollection(jobName)
		col.insert(obj)
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
