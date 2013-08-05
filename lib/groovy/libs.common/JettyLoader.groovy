import org.osgi.framework.ServiceReference
import org.osgi.util.tracker.ServiceTracker
import org.osgi.util.tracker.ServiceTrackerCustomizer
import org.apache.log4j.Logger
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil

import javax.servlet.Servlet
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

class JettyLoader implements ServiceTrackerCustomizer {
	def context
	def trackerObj	
	def server
	def servletCtxHandler
	
	public static final String JETTY_PORT = "jetty.port";
	
	public JettyLoader(BundleContext ctx) {
		def port = Integer.parseInt(System.getProperty(JETTY_PORT))
		println "Deploy jetty on port " + port
		this.context = ctx		
		server = new Server(port)
		servletCtxHandler = new ServletContextHandler(ServletContextHandler.SESSIONS)
		servletCtxHandler.setContextPath("/")
		server.setHandler(servletCtxHandler)
		server.start()
		trackerObj = new ServiceTracker(this.context, javax.servlet.Servlet.class.getName(), this)
		trackerObj.open()
	}
	
	/**
	 * ServiceTrackerCustomizer.addingService
	 * 以下は、ServiceTrackerCustomizerの実装部
	 *  (http://www.osgi.org/javadoc/r4v42/org/osgi/util/tracker/ServiceTrackerCustomizer.html)
	 */
	public Object addingService(ServiceReference reference) {
		def oservice = context.getService(reference)
		String alias = reference.getProperty("alias")
		servletCtxHandler.addServlet(new ServletHolder(oservice), alias)
		return oservice
	}
	
	/**
	 * ServiceTrackerCustormizer.modifiedService
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		def oservice = context.getService(reference)
		String alias = reference.getProperty("alias")
		servletCtxHandler.addServlet(new ServletHolder(oservice), alias)
	}
	
	/**
	 * ServiceTrackerCustomizer.removedService
	 */
	public void removedService(ServiceReference reference, Object service)  {
	}
}