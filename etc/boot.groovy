//
// groovy bootstrap script
// このスクリプトを修正した後、再実行したい時は、groovyrunner bundleを
//    stop -> start
// すること。
//
import org.osgi.util.tracker.ServiceTrackerCustomizer
import org.osgi.util.tracker.ServiceTracker
import org.osgi.framework.ServiceReference
import org.codehaus.groovy.tools.RootLoader

/**
 * The body of bootup work
 */
def doBootStep() {
        def binding = new Binding();
        // BundleContextを "ctx" としてセット
        binding.setVariable("ctx", ctx);
        // loggerを "logger"としてセット
        binding.setVariable("logger", logger);
        // 永続データ用共有map を "sharedMap" としてセット
        // OSGiサービスの2重登録防止などに使用する。
        binding.setVariable("sharedMap", [:]);
        // groovy.home を設定
        //println "groovy.home"
        def homedir = java.lang.System.getProperty("felix.home");
        java.lang.System.setProperty("groovy.home", homedir);

        URL [] scriptpath123 = [new File(homedir + "/" + "lib/groovy/libs.common").toURL(),new File(homedir + "/" + "lib/groovy/libs.target").toURL()]
        RootLoader rootloader = new RootLoader(scriptpath123, this.getClass().getClassLoader())

        // Loaderに渡す為のGroovyShell
        def shell = new GroovyShell(rootloader,binding)

//        def jettyLoader = new JettyLoader(ctx)
      //  def jettyLoader = rootloader.loadClass("JettyLoader").newInstance([ctx] as Object[] )
      def nettyLoader = rootloader.loadClass("NettyLoader").newInstance([ctx] as Object[] )

        // IST_HOME/lib/groovy の直下のgroovyファイルを自動でロードするLoader
//        def loader = new DefaultLoader(ctx, shell)
        def loader = rootloader.loadClass("DefaultLoader").newInstance([ctx, shell] as Object[] )
        // OSGi serviceに渡すproperty
        def props = new java.util.Hashtable();
	def clsListener = rootloader.loadClass("org.wiperdog.directorywatcher.Listener")
        props.put(clsListener.PROPERTY_HANDLERETRY, "true");
        // 通常のgroovyスクリプト用loaderをdirectory-watcherとして登録
        ctx.registerService(clsListener.getName(), loader, props)

        //
        // IST_HOME/var/job 直下のjobファイルを自動でロードするLoader
//        def jobLoader = new JobLoader(ctx, shell)
		// Registering job,trigger,jobcls,instances directory watcher listener 
        def jobLoader = rootloader.loadClass("JobLoader").newInstance([ctx, shell] as Object[] )
		def jobListener = rootloader.loadClass("JobListener").newInstance([ctx] as Object[] )		
		def trgListener = rootloader.loadClass("TriggerListener").newInstance([ctx] as Object[] )
		def instListener = rootloader.loadClass("JobInstanceListener").newInstance([ctx] as Object[] )
		def jobClassListener = rootloader.loadClass("JobClassListener").newInstance([ctx] as Object[] )
		def jobRunnerMain = rootloader.loadClass("JobRunnerMain").newInstance([shell,ctx] as Object[] )
		def jobRunnerMainService = rootloader.loadClass("JobRunnerMainService")
        // OSGi serviceに渡すproperty
        def props_jobsvc = new java.util.Hashtable(props);
        // 監視系ジョブ専用loaderをdirectory-watcherとして登録(未実装)
        //ctx.registerService(clsListener.getName(), jobLoader, props_jobsvc)
		 ctx.registerService(clsListener.getName(), jobListener, null)
		 ctx.registerService(clsListener.getName(), trgListener, null)
		 ctx.registerService(clsListener.getName(), instListener, null)
		 ctx.registerService(clsListener.getName(), jobClassListener, null)		 
		 ctx.registerService(jobRunnerMainService.getName(), jobRunnerMain, null)	
		 def restServiceLoader = rootloader.loadClass("RestServiceLoader").newInstance([ctx] as Object[] )
}

class MyCustomizer implements ServiceTrackerCustomizer {
	def bundleStart

	Object addingService(ServiceReference reference){
		bundleStart = "true"
		return null
	}

	void modifiedService(ServiceReference reference, Object service){
	}

	void removedService(ServiceReference reference, Object service){
	}
}

try {
	def waiter = new Runnable() {
		def ctx
		def startTime
		def elapsedTime
		def th_elapsedtime = 120000

		public void run() {
			def myCustomizer = new MyCustomizer()
			def className = "org.wiperdog.jobmanager.JobFacade"
			def tracker = new ServiceTracker(ctx, className, myCustomizer)
			tracker.open()

			while ( myCustomizer.bundleStart != "true" ){
				elapsedTime = System.currentTimeMillis() - startTime 
				if ( elapsedTime > th_elapsedtime ) {
					println "boot.groovy has been terminaed by elapsedtime over.(exceeded " + th_elapsedtime + " millisec)"
					break
				} 
				try {
					Thread.sleep(1000)
				} catch (InterruptedException e) {}
			}
			tracker.close()
			if( myCustomizer.bundleStart == "true" ) { 
				doBootStep()
			}
		}
	}

	waiter.ctx = ctx
	new Thread(waiter).start()
	waiter.startTime = System.currentTimeMillis()

} catch (Exception e) {
	println e
}
