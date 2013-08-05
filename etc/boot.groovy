//
// groovy bootstrap script
// このスクリプトを修正した後、再実行したい時は、groovyrunner bundleを
//    stop -> start
// すること。
//

import org.wiperdog.directorywatcher.Listener
import org.codehaus.groovy.tools.RootLoader;
try {
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
	
	URL [] scriptpath123 = [new File(homedir + "/" + "lib/groovy/libs.manager").toURL(),new File(homedir + "/" + "lib/groovy/libs.target").toURL()]
	RootLoader rootloader = new RootLoader(scriptpath123, this.getClass().getClassLoader())

	// Loaderに渡す為のGroovyShell
	def shell = new GroovyShell(rootloader,binding)
	
	def jettyLoader = new JettyLoader(ctx)
	
	// IST_HOME/lib/groovy の直下のgroovyファイルを自動でロードするLoader
	def loader = new DefaultLoader(ctx, shell)
	// OSGi serviceに渡すproperty
	def props = new java.util.Hashtable();
	props.put(Listener.PROPERTY_HANDLERETRY, "true");
	// 通常のgroovyスクリプト用loaderをdirectory-watcherとして登録
	ctx.registerService(Listener.class.getName(), loader, props)

	//
	// IST_HOME/var/job 直下のjobファイルを自動でロードするLoader
	def jobLoader = new JobLoader(ctx, shell)
	// OSGi serviceに渡すproperty
	def props_jobsvc = new java.util.Hashtable(props);
	// 監視系ジョブ専用loaderをdirectory-watcherとして登録(未実装)
	ctx.registerService(Listener.class.getName(), jobLoader, props_jobsvc)
} catch (Exception ex) {
	println ex
}