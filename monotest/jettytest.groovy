import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*
import groovy.servlet.*

@Grapes([
    @Grab(group='org.eclipse.jetty.orbit', module='javax.servlet', version='3.0.0.v201112011016', ext='jar'),
    @Grab(group='org.eclipse.jetty', module='jetty-server', version='8.1.8.v20121106', transitive=false),
    @Grab(group='org.eclipse.jetty', module='jetty-continuation', version='8.1.8.v20121106'),
    @Grab(group='org.eclipse.jetty', module='jetty-http', version='8.1.8.v20121106'),
    @Grab(group='org.eclipse.jetty', module='jetty-servlet', version='8.1.8.v20121106', transitive=false),
    @Grab(group='org.eclipse.jetty', module='jetty-security', version='8.1.8.v20121106', transitive=false)
])

def startJetty() {
    def jetty = new Server(9090)

    def context = new ServletContextHandler(jetty, '/', ServletContextHandler.SESSIONS)  // Allow sessions.
    context.resourceBase = '.'  // Look in current dir for Groovy scripts.
    // context.addServlet(GroovyServlet, '*.groovy')  // All files ending with .groovy will be served.
    context.addServlet(MongoStore, '/tfm_dbmonitor')  // All files ending with .groovy will be served.
    context.setAttribute('version', '1.0')  // Set an context attribute.

    jetty.start()
}

println "Starting Jetty, press Ctrl+C to stop."
startJetty()

