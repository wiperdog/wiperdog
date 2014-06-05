import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File
import com.strategicgains.restexpress.Format;
import com.strategicgains.restexpress.Parameters;
import com.strategicgains.restexpress.RestExpress;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

import com.strategicgains.restexpress.pipeline.SimpleConsoleLogMessageObserver;
import com.strategicgains.restexpress.plugin.route.RoutesMetadataPlugin;
import java.util.jar.JarFile;
import groovy.json.JsonSlurper;
import javax.swing.text.html.CSS.LengthUnit;
import java.util.jar.JarFile;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.osgi.framework.BundleContext;
import org.wiperdog.bundleextractor.BundleExtractor;


public class RestServiceLoader{
	BundleContext context ;
	public RestServiceLoader(BundleContext ctx){
		this.context = ctx;
		RestExpress server = new RestExpress()
				.setName("RestExpress")
				.setPort(8089)
				.setDefaultFormat("json")
				.addMessageObserver(new SimpleConsoleLogMessageObserver());
		defineRoutes(server);
		server.setExecutorThreadCount(0);
		server.setExecutorThreadCount(0);
		new RoutesMetadataPlugin().register(server)
				.parameter(Parameters.Cache.MAX_AGE, 86400);	// Cache for 1 day (24 hours).
		server.bind();
		server.awaitShutdown();

	}
	private void defineRoutes(RestExpress server) {
		def jobInstallCtrler = new JobInstallController(context)
		server.uri("/bundle/install", jobInstallCtrler).method(HttpMethod.POST)
		server.uri("/bundle/{groupId}/{artifactId}/{version}", jobInstallCtrler).method(HttpMethod.GET)
		def jobRunnerService = new JobRunOneShot(context)
		server.uri("/runjob", jobRunnerService).method(HttpMethod.POST)
		server.uri("/runjob/data", jobRunnerService).method(HttpMethod.PUT)

	}

}