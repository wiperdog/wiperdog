//
// groovy bootstrap script
// このスクリプトを修正した後、再実行したい時は、groovyrunner bundleを
//    stop -> start
// すること。
//
import org.osgi.framework.BundleListener
import org.osgi.framework.BundleEvent
import org.osgi.framework.Bundle
import org.codehaus.groovy.tools.RootLoader

// The list of bundles to be waited for presense.
// Groovy scripts have to wait for dependency presense in OSGi environment.
// The bundle in this waitList must not be fragment bundle, because fragment bundle can't be ACTIVE.
def waitList = [ "org.wiperdog.directorywatcher": false, "org.wiperdog.jobmanager":false ]

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
        def jobLoader = rootloader.loadClass("JobLoader").newInstance([ctx, shell] as Object[] )
        // OSGi serviceに渡すproperty
        def props_jobsvc = new java.util.Hashtable(props);
        // 監視系ジョブ専用loaderをdirectory-watcherとして登録(未実装)
        ctx.registerService(clsListener.getName(), jobLoader, props_jobsvc)
}

/**
 * At startup moment, it is not guaranteed the dependency bundles are already loaded.
 * So We have to wait for their presense.
 */
try {
	//
	// Create bundle listener.
	// 
	def bundleListener = new BundleListener() {
		public synchronized void bundleChanged(BundleEvent ev) {
			if (ev.getType() != BundleEvent.STARTED) {
				return
			}
			def b = ev.getBundle()
			def symbolicName = b.getSymbolicName()
			def bAll = true
			waitList.each { name ->
//				println name
				if (name.key.equals(symbolicName)) {
					name.value = true
				}
				if (!name.value) {
					bAll = false
				}
			}
			if (bAll) {
				notifyAll()
			}
		}
	}

	//
	// waiter thread waits for presense of all dependecy bundles, then call boot process.
	//
	def waiter = new Runnable() {
		def ctx
		public void run() {
			synchronized(bundleListener) {
				bundleListener.wait()
			}
			ctx.removeBundleListener(bundleListener)
			doBootStep()
		}
	}
	waiter.ctx = ctx

	new Thread(waiter).start()

	// 
	// before registering bundle listener, we have to see what bundles are already present.
	//
	def installedBundles = ctx.getBundles()
	installedBundles.each { bundle -> 
		if (bundle.getState() == Bundle.ACTIVE) {
			def symbolicName = bundle.getSymbolicName()
			waitList.each { name ->
				if (name.key.equals(symbolicName)) {
					name.value = true
				}
			}
		}
	}
	// register bundle listener
	ctx.addBundleListener(bundleListener)

} catch (Exception ex) {
	println ex
}
