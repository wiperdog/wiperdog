import org.codehaus.groovy.tools.RootLoader;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ConfigurationException;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.BundleContext;
import org.quartz.Trigger.TriggerState;

public class JobRunner implements ManagedService{
	private static Framework m_fwk = null;
	private static GroovyClassLoader gcl = new GroovyClassLoader();
	
	public static void main(String[] args) throws Exception {
		println args
		def binding = new Binding()
		def felix_home = getFelixHome()
		binding.setVariable("felix_home", felix_home)
		boolean isSingleRun = false
		def listExecutingJobs = []
		
		URL [] scriptpath123 = [new File(felix_home + "/" + "lib/groovy/libs.manager").toURL(), new File(felix_home + "/" + "lib/groovy/libs.target").toURL(), new File(felix_home + "/" + "lib/groovy/libs.common").toURL()]
		RootLoader rootloader = new RootLoader(scriptpath123, gcl)
		
		def shell = new GroovyShell(rootloader,binding)
		
		Properties configProps = new Properties();
		FrameworkFactory factory = getFrameworkFactory();
        m_fwk = factory.newFramework(configProps);
        // Initialize the framework, but don't start it yet.
        m_fwk.init();
		m_fwk.start();
        BundleContext context = m_fwk.getBundleContext();
		
		try {
          	context.installBundle((new File(felix_home + "/" + "lib/java/bundle.a/org.apache.felix.configadmin-1.2.8.jar")).toURI().toString())
          	context.installBundle((new File(felix_home + "/" + "lib/java/bundle.a/org.osgi.compendium-1.4.0.jar")).toURI().toString())
          	context.installBundle((new File(felix_home + "/" + "lib/java/bundle/org.wiperdog.configloader-0.1.0.jar")).toURI().toString())
		}catch(Exception ex){
			println "Exception:" + ex
		}
		def lstinstalledBundles = context.getBundles()
		lstinstalledBundles.each{bund->
			try{
			bund.start()
			}catch(Exception ex){
				println "Exception:" + ex
			}
		}
        Class jobRunnerMainClass = shell.getClassLoader().loadClass('JobRunnerMain');
		Object jobRunnerMain_obj = jobRunnerMainClass.newInstance(shell, context)
		
		if(args.length == 2 && args[0] == '-f'){
			isSingleRun = true;
			jobRunnerMain_obj.executeJob(args[1], null)
		}else if((args.length == 4) && (args[0] == '-f') && (args[2] == '-s')){
			jobRunnerMain_obj.executeJob(args[1], args[3])
		}else{
			println "Wrong format!!!"
			println "Try jobrunner -f <path>"
			println "Or jobrunner -f <path> -s <crontab>"
			return
		}
		
		if(isSingleRun){
			while(true){
				def triggerState = jobRunnerMain_obj.jobfacade.sched.getTriggerState(jobRunnerMain_obj.trgKey)
				if(triggerState == TriggerState.NONE){
					jobRunnerMain_obj.jobfacade.sched.shutdown(true)
	       			m_fwk.stop()
					System.exit(1)
				}
			}
		}
	}
	public static String getFelixHome(){
		//	get felix home
		//2013-03-06 Luvina update start
		File currentDir = new File(System.getProperty("bin_home"))
		//2013-03-06 Luvina update end
		def felix_home = currentDir.getParent()
		return felix_home
	}
	
	private static FrameworkFactory getFrameworkFactory() throws Exception
    {
        URL url = gcl.getResource(
            "META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if (url != null)
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            try
            {
                for (String s = br.readLine(); s != null; s = br.readLine())
                {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ((s.length() > 0) && (s.charAt(0) != '#'))
                    {
                        return (FrameworkFactory) Class.forName(s).newInstance();
                    }
                }
            }
            finally
            {
                if (br != null) br.close();
            }
        }

        throw new Exception("Could not find framework factory.");
    }
    
    public void updated(Dictionary properties) throws ConfigurationException {
		// TODO Auto-generated method stub
	}
}