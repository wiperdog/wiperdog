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
		// DBMS Info Rest Service
		def dbmsInfoRestService = new DbmsInfoRestService(context)
		server.uri("/use_for_xwiki/{keyConfigXwiki}", dbmsInfoRestService).alias("/use_for_xwiki").method(HttpMethod.GET)
		
		// Menugenerator RestAPI service
		def menuGeneratorRestService = new MenuGeneratorRestService()
		server.uri("/menuGenerator", menuGeneratorRestService).method(HttpMethod.GET)
		
		def realtimeService = new RealtimeService()
		server.uri("/realtime/{jobname}/{istIid}", realtimeService).method(HttpMethod.GET)
		
		def realtimeLivetableService = new RealtimeLiveTableService()
		server.uri("/realtimeLivetable/{jobname}/{istIid}", realtimeLivetableService).method(HttpMethod.GET)
		
		def policyStringService = new PolicyStringService()
		server.uri("/PolicyString", policyStringService).method(HttpMethod.POST)
		
		def monitorDataService = new MonitoringDataService()
		server.uri("/monitoringData/{job}/{istIid}", monitorDataService).method(HttpMethod.GET)

		def liveTableAdapter = new LivetableDataAdapter()
		server.uri("/liveTableAdapter", liveTableAdapter).method(HttpMethod.GET)

		def chooseJobPolicy = new ChooseJobPolicy()
		server.uri("/ChooseJobPolicy/{jobname}", chooseJobPolicy).alias("/ChooseJobPolicy").method(HttpMethod.GET)

		def msgPolicy = new MsgPolicy()
		server.uri("/msgPolicy/{action}", msgPolicy).method(HttpMethod.GET)

		def lastAccum = new LastAccumulation()
		server.uri("/LastAccumulation", lastAccum).method(HttpMethod.GET)

	}

}
