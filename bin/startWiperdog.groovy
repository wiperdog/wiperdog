import org.osgi.framework.*;
import org.osgi.service.startlevel.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class JobRunner{
	private static Framework m_fwk = null;
	private static GroovyClassLoader gcl = new GroovyClassLoader();
	/**
     * The property name used to specify an URL to the system
     * property file.
    **/
	public static final String SYSTEM_PROPERTIES_PROP = "felix.system.properties";
	/**
     * The default name used for the system properties file.
    **/
	public static final String SYSTEM_PROPERTIES_FILE_VALUE = "system.properties";
	/**
     * Name of the configuration directory.
     */
    public static final String CONFIG_DIRECTORY = "etc";
    /**
     * The default name used for the configuration properties file.
     */
    public static final String CONFIG_PROPERTIES_FILE_VALUE = "config.properties";
    /**
     * The property name used to specify an URL to the configuration
     * property file to be used for the created the framework instance.
     */
    public static final String CONFIG_PROPERTIES_PROP = "felix.config.properties";
	
	public static void main(String[] args) throws Exception {
		// Load system properties.
		JobRunner.loadSystemProperties()
		
		// Read configuration properties.
        Properties configProps = JobRunner.loadConfigProperties();
        // If no configuration properties were found, then create
        // an empty properties object.
        if (configProps == null) {
            println("No " + CONFIG_PROPERTIES_FILE_VALUE + " found.");
            configProps = new Properties();
        }
        
        // Copy framework properties from the system properties.
        JobRunner.copySystemProperties(configProps);
        
		// Create an instance of the framework.
		FrameworkFactory factory = getFrameworkFactory();
		m_fwk = factory.newFramework(configProps);
        // Initialize the framework, but don't start it yet.
        m_fwk.init();
        // Use the system bundle context to process the auto-deploy
        // and auto-install/auto-start properties.
        AutoProcessor.processAuto(configProps, m_fwk.getBundleContext());
		m_fwk.start();
		//User for install jar which has need to be wrap
		AutoProcessor.processCustom(configProps, m_fwk.getBundleContext());
		// Wait for framework to stop to exit the VM.
        m_fwk.waitForStop(0);
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
	
	public static void loadSystemProperties() {
		URL propURL = null;
        String custom = System.getProperty(SYSTEM_PROPERTIES_PROP);
        if (custom != null){
            try {
                propURL = new URL(custom);
            } catch (MalformedURLException ex) {
                println("Start, loadSystemProperties: " + ex);
                return;
            }
        } else {
            // Determine where the configuration directory is by figuring
            // out where felix.jar is located on the system class path.
            File confDir = null;
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("felix.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index >= start) {
                // Get the path of the felix.jar file.
                String jarLocation = classpath.substring(start, index);
                // Calculate the conf directory based on the parent
                // directory of the felix.jar directory.
                confDir = new File( new File(new File(new File(new File(jarLocation).getAbsolutePath()).getParent()).getParent()).getParent(), CONFIG_DIRECTORY);
            } else {
                // Can't figure it out so use the current directory as default.
                confDir = new File(System.getProperty("user.dir"), CONFIG_DIRECTORY);
            }
            
            try {
                propURL = new File(confDir, SYSTEM_PROPERTIES_FILE_VALUE).toURL();
            } catch (MalformedURLException ex) {
                println("Start, loadSystemProperties: " + ex);
                return;
            }
        }
        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = propURL.openConnection().getInputStream();
            props.load(is);
            is.close();
        } catch (FileNotFoundException ex) {
            // Ignore file not found.
        } catch (Exception ex) {
            println("Start: Error loading system properties from " + propURL);
            println("Start, loadSystemProperties: " + ex);
            try {
                if (is != null) is.close();
            } catch (IOException ex2)  {
                // Nothing we can do.
            }
            return;
        }
        
        // Perform variable substitution on specified properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            System.setProperty(name,
                Util.substVars(props.getProperty(name), name, null, null));
        }
	}
	
	public static Properties loadConfigProperties() {
        // The config properties file is either specified by a system
        // property or it is in the conf/ directory of the Felix
        // installation directory.  Try to load it from one of these
        // places.
		
        // See if the property URL was specified as a property.
        URL propURL = null;
        String custom = System.getProperty(CONFIG_PROPERTIES_PROP);
        if (custom != null) {
            try {
                propURL = new URL(custom);
            } catch (MalformedURLException ex) {
                println("Start, loadConfigProperties: " + ex);
                return null;
            }
        } else {
            // Determine where the configuration directory is by figuring
            // out where felix.jar is located on the system class path.
            File confDir = null;
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("felix.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index >= start) {
                // Get the path of the felix.jar file.
                String jarLocation = classpath.substring(start, index);
                // Calculate the conf directory based on the parent
                // directory of the felix.jar directory.
                confDir = new File( new File(new File(new File(new File(jarLocation).getAbsolutePath()).getParent()).getParent()).getParent(), CONFIG_DIRECTORY);
            } else {
                // Can't figure it out so use the current directory as default.
                confDir = new File(System.getProperty("user.dir"), CONFIG_DIRECTORY);
            }
            try {
                propURL = new File(confDir, CONFIG_PROPERTIES_FILE_VALUE).toURL();
            } catch (MalformedURLException ex) {
                println("Start, loadConfigProperties: " + ex);
                return null;
            }
        }
        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try {
            // Try to load config.properties.
            is = propURL.openConnection().getInputStream();
            props.load(is);
            is.close();
        } catch (Exception ex) {
            // Try to close input stream if we have one.
            try {
                if (is != null) is.close();
            } catch (IOException ex2) {
                // Nothing we can do.
            } 
            return null;
        }
        // Perform variable substitution for system properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            props.setProperty(name,
                Util.substVars(props.getProperty(name), name, null, props));
        }
        return props;
    }
    
    public static void copySystemProperties(Properties configProps) {
        for (Enumeration e = System.getProperties().propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            if (key.startsWith("felix.") || key.startsWith("org.osgi.framework.")) {
                configProps.setProperty(key, System.getProperty(key));
            }
        }
    }
    
    /**
	 * @see org.apache.felix.main.AutoProcessor
	 */
	public class AutoProcessor
	{
	    /**
	     * The property name used for the bundle directory.
	    **/
	    public static final String AUTO_DEPLOY_DIR_PROPERY = "felix.auto.deploy.dir";
	    /**
	     * The default name used for the bundle directory.
	    **/
	    public static final String AUTO_DEPLOY_DIR_VALUE = "bundle";
	    /**
	     * The property name used to specify auto-deploy actions.
	    **/
	    public static final String AUTO_DEPLOY_ACTION_PROPERY = "felix.auto.deploy.action";
	    /**
	     * The name used for the auto-deploy install action.
	    **/
	    public static final String AUTO_DEPLOY_INSTALL_VALUE = "install";
	    /**
	     * The name used for the auto-deploy start action.
	    **/
	    public static final String AUTO_DEPLOY_START_VALUE = "start";
	    /**
	     * The name used for the auto-deploy update action.
	    **/
	    public static final String AUTO_DEPLOY_UPDATE_VALUE = "update";
	    /**
	     * The name used for the auto-deploy uninstall action.
	    **/
	    public static final String AUTO_DEPLOY_UNINSTALL_VALUE = "uninstall";
	    /**
	     * The property name prefix for the launcher's auto-install property.
	    **/
	    public static final String AUTO_INSTALL_PROP = "felix.auto.install";
	    /**
	     * The property name prefix for the launcher's auto-start property.
	    **/
	    public static final String AUTO_START_PROP = "felix.auto.start";
	    /**
	     * The property name used to specify directory of jar which need to be wrap
	    **/
	    public static final String WRAP_JAR_DIR = "felix.wrapjarinstall.dir";
	    /**
	     * The property name used to specify action for each jar file which need to be wrap
	    **/
	    public static final String WRAP_JAR_ACTION = "felix.wrapjarinstall.action";
	    /**
	     * The property name used to specify list jar file which need to be wrap first
	    **/
	    public static final String WRAP_JAR_INSTALL_FIRST = "felix.wrapjarinstall.firststart"
	    /**
	     * Used to instigate auto-deploy directory process and auto-install/auto-start
	     * configuration property processing during.
	     * @param configMap Map of configuration properties.
	     * @param context The system bundle context.
	    **/
	    public static void processAuto(Map configMap, BundleContext context) {
	        configMap = (configMap == null) ? new HashMap() : configMap;
	        processAutoDeploy(configMap, context);
	        processAutoProperties(configMap, context);
	    }
	    
	    /**
	     * Used to instigate wrap jar file and install
	     * @param configMap Map of configuration properties.
	     * @param context The system bundle context.
	    **/
	    public static void processCustom(Map configMap, BundleContext context) {
	        configMap = (configMap == null) ? new HashMap() : configMap;
	        processWrapJar(configMap, context);
	    }
	    
	    /**
	     * <p>
	     * Processes bundles in the auto-deploy directory, installing and then
	     * starting each one.
	     * </p>
	     */
	    private static void processAutoDeploy(Map configMap, BundleContext context) {
	        // Determine if auto deploy actions to perform.
	        String action = (String) configMap.get(AUTO_DEPLOY_ACTION_PROPERY);
	        action = (action == null) ? "" : action;
	        List actionList = new ArrayList();
	        StringTokenizer st = new StringTokenizer(action, ",");
	        while (st.hasMoreTokens()) {
	            String s = st.nextToken().trim().toLowerCase();
	            if (s.equals(AUTO_DEPLOY_INSTALL_VALUE)
	                || s.equals(AUTO_DEPLOY_START_VALUE)
	                || s.equals(AUTO_DEPLOY_UPDATE_VALUE)
	                || s.equals(AUTO_DEPLOY_UNINSTALL_VALUE)) {
	                actionList.add(s);
	            }
	        }

	        // Perform auto-deploy actions.
	        if (actionList.size() > 0) {
	            // Get list of already installed bundles as a map.
	            Map installedBundleMap = new HashMap();
	            Bundle[] bundles = context.getBundles();
	            for (int i = 0; i < bundles.length; i++) {
	                installedBundleMap.put(bundles[i].getLocation(), bundles[i]);
	            }

	            // Get the auto deploy directory.
	            String autoDir = (String) configMap.get(AUTO_DEPLOY_DIR_PROPERY);
	            autoDir = (autoDir == null) ? AUTO_DEPLOY_DIR_VALUE : autoDir;
	            // Look in the specified bundle directory to create a list
	            // of all JAR files to install.
	            File[] files = new File(autoDir).listFiles();
	            List jarList = new ArrayList();
	            if (files != null) {
	                Arrays.sort(files);
	                for (int i = 0; i < files.length; i++) {
	                    if (files[i].getName().endsWith(".jar")) {
	                        jarList.add(files[i]);
	                    }
	                }
	            }

	            // Install bundle JAR files and remember the bundle objects.
	            final List startBundleList = new ArrayList();
	            for (int i = 0; i < jarList.size(); i++) {
	                // Look up the bundle by location, removing it from
	                // the map of installed bundles so the remaining bundles
	                // indicate which bundles may need to be uninstalled.
	                Bundle b = (Bundle) installedBundleMap.remove(((File) jarList.get(i)).toURI().toString());
	                try {
	                    // If the bundle is not already installed, then install it
	                    // if the 'install' action is present.
	                    if ((b == null) && actionList.contains(AUTO_DEPLOY_INSTALL_VALUE)) {
	                        b = context.installBundle( ((File) jarList.get(i)).toURI().toString());
	                    }
	                    // If the bundle is already installed, then update it
	                    // if the 'update' action is present.
	                    else if (actionList.contains(AUTO_DEPLOY_UPDATE_VALUE)) {
	                        b.update();
	                    }

	                    // If we have found and/or successfully installed a bundle,
	                    // then add it to the list of bundles to potentially start.
	                    if (b != null) {
	                        startBundleList.add(b);
	                    }
	                } catch (BundleException ex) {
	                    println("Auto-deploy install: "
	                        + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
	                }
	            }

	            // Uninstall all bundles not in the auto-deploy directory if
	            // the 'uninstall' action is present.
	            if (actionList.contains(AUTO_DEPLOY_UNINSTALL_VALUE)) {
	                for (Iterator it = installedBundleMap.entrySet().iterator(); it.hasNext(); ) {
	                    Map.Entry entry = (Map.Entry) it.next();
	                    Bundle b = (Bundle) entry.getValue();
	                    if (b.getBundleId() != 0) {
	                        try {
	                            b.uninstall();
	                        } catch (BundleException ex) {
	                         	println("Auto-deploy uninstall: "
	                            	+ ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
	                        }
	                    }
	                }
	            }

	            // Start all installed and/or updated bundles if the 'start'
	            // action is present.
	            if (actionList.contains(AUTO_DEPLOY_START_VALUE)) {
	                for (int i = 0; i < startBundleList.size(); i++) {
	                    try {
	                        if (!isFragment((Bundle) startBundleList.get(i))) {
	                            ((Bundle) startBundleList.get(i)).start();
	                        }
	                    } catch (BundleException ex) {
	                        println("Auto-deploy start: "
	                            + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
	                    }
	                }
	            }
	        }
	    }

	    /**
	     * <p>
	     * Processes the auto-install and auto-start properties from the
	     * specified configuration properties.
	     * </p>
	     */
	    private static void processAutoProperties(Map configMap, BundleContext context)
	    {
	        // Retrieve the Start Level service, since it will be needed
	        // to set the start level of the installed bundles.
	        StartLevel sl = (StartLevel) context.getService(
	            context.getServiceReference(org.osgi.service.startlevel.StartLevel.class.getName()));

	        // Retrieve all auto-install and auto-start properties and install
	        // their associated bundles. The auto-install property specifies a
	        // space-delimited list of bundle URLs to be automatically installed
	        // into each new profile, while the auto-start property specifies
	        // bundles to be installed and started. The start level to which the
	        // bundles are assigned is specified by appending a ".n" to the
	        // property name, where "n" is the desired start level for the list
	        // of bundles. If no start level is specified, the default start
	        // level is assumed.
	        for (Iterator i = configMap.keySet().iterator(); i.hasNext(); ) {
	            String key = ((String) i.next()).toLowerCase();

	            // Ignore all keys that are not an auto property.
	            if (!key.startsWith(AUTO_INSTALL_PROP) && !key.startsWith(AUTO_START_PROP)) {
	                continue;
	            }

	            // If the auto property does not have a start level,
	            // then assume it is the default bundle start level, otherwise
	            // parse the specified start level.
	            int startLevel = sl.getInitialBundleStartLevel();
	            if (!key.equals(AUTO_INSTALL_PROP) && !key.equals(AUTO_START_PROP)) {
	                try {
	                    startLevel = Integer.parseInt(key.substring(key.lastIndexOf('.') + 1));
	                } catch (NumberFormatException ex) {
	                    println("Invalid property: " + key);
	                }
	            }

	            // Parse and install the bundles associated with the key.
	            StringTokenizer st = new StringTokenizer((String) configMap.get(key), "\" ", true);
	            for (String location = nextLocation(st); location != null; location = nextLocation(st)) {
	                try {
	                    Bundle b = context.installBundle(location, null);
	                    sl.setBundleStartLevel(b, startLevel);
	                } catch (Exception ex) {
	                    println("Auto-properties install: " + location + " ("
	                        + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : "") + ")");
						if (ex.getCause() != null)
						    ex.printStackTrace();
	                }
	            }
	        }

	        // Now loop through the auto-start bundles and start them.
	        for (Iterator i = configMap.keySet().iterator(); i.hasNext(); ) {
	            String key = ((String) i.next()).toLowerCase();
	            if (key.startsWith(AUTO_START_PROP)) {
	                StringTokenizer st = new StringTokenizer((String) configMap.get(key), "\" ", true);
	                for (String location = nextLocation(st); location != null; location = nextLocation(st)) {
	                    // Installing twice just returns the same bundle.
	                    try {
	                        Bundle b = context.installBundle(location, null);
	                        if (b != null) {
	                            b.start();
	                        }
	                    } catch (Exception ex)  {
	                        println("Auto-properties start: " + location + " ("
	                            + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : "") + ")");
	                    }
	                }
	            }
	        }
	    }
		
		/**
	     * <p>
	     * Processes wrap url jar file
	     * starting each one as OSGI bundle.
	     * </p>
	     */
	    private static void processWrapJar(Map configMap, BundleContext context) {
	        // Determine if auto deploy actions to perform.
	        String action = (String) configMap.get(WRAP_JAR_ACTION);
	        action = (action == null) ? "" : action;
	        List actionList = new ArrayList();
	        StringTokenizer st = new StringTokenizer(action, ",");
	        while (st.hasMoreTokens()) {
	            String s = st.nextToken().trim().toLowerCase();
	            if (s.equals(AUTO_DEPLOY_INSTALL_VALUE)
	                || s.equals(AUTO_DEPLOY_START_VALUE)
	                || s.equals(AUTO_DEPLOY_UPDATE_VALUE)
	                || s.equals(AUTO_DEPLOY_UNINSTALL_VALUE)) {
	                actionList.add(s);
	            }
	        }
	        
	        // Perform auto-deploy actions.
	        if (actionList.size() > 0) {
	        	// Install bundle JAR files and remember the bundle objects.
	            final List startBundleList = new ArrayList();
	            //Install and start the jar which is configed as first
		        for (Iterator i = configMap.keySet().iterator(); i.hasNext(); ) {
		            String key = ((String) i.next()).toLowerCase();
		            if (key.startsWith(WRAP_JAR_INSTALL_FIRST)) {
		                st = new StringTokenizer((String) configMap.get(key), "\" ", true);
		                for (String location = nextLocation(st); location != null; location = nextLocation(st)) {
		                    // Installing twice just returns the same bundle.
		                    try {
		                        def wrapStr = "wrap:" + location
	                        	Bundle bfirst = context.installBundle(wrapStr);
		                        startBundleList.add (bfirst)
		                    } catch (Exception ex)  {
		                        println("Auto-properties start: " + location + " ("
		                            + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : "") + ")");
		                    }
		                }
		            }
		        }
	            
	            
	            // Get list of already installed bundles as a map.
	            Map installedBundleMap = new HashMap();
	            Bundle[] bundles = context.getBundles();
	            for (int i = 0; i < bundles.length; i++) {
	            	def locate = bundles[i].getLocation().replace("\\", "/");
	            	if (!locate.contains("wrap:file:/")) {
	            		locate = locate.replace("wrap:file:", "wrap:file:/")
	            	}
	                installedBundleMap.put(locate, bundles[i]);
	            }

	            // Get the wrap jar file directory.
	            String wrapJarDir = (String) configMap.get(WRAP_JAR_DIR);
	            // Look in the specified directory to create a list
	            // of all JAR files to wrap and install.
	            File[] files = new File(wrapJarDir).listFiles();
	            List jarList = new ArrayList();
	            if (files != null) {
	                Arrays.sort(files);
	                for (int i = 0; i < files.length; i++) {
	                    if (files[i].getName().endsWith(".jar")) {
	                        jarList.add(files[i]);
	                    }
	                }
	            }
				

	            for (int i = 0; i < jarList.size(); i++) {
	                // Look up the bundle by location, removing it from
	                // the map of installed bundles so the remaining bundles
	                // indicate which bundles may need to be uninstalled.
	                def wrapStr = "wrap:" + ((File) jarList.get(i)).toURI().toString()
	                Bundle b = (Bundle) installedBundleMap.remove(wrapStr);
	                try {
	                    // If the jar is not already installed, then install it
	                    // if the 'install' action is present.
	                    if ((b == null) && actionList.contains(AUTO_DEPLOY_INSTALL_VALUE)) {
	                        b = context.installBundle(wrapStr);
	                    }
	                    // If the bundle is already installed, then update it
	                    // if the 'update' action is present.
	                    else if (actionList.contains(AUTO_DEPLOY_UPDATE_VALUE)) {
	                        b.update();
	                    }

	                    // If we have found and/or successfully installed a bundle,
	                    // then add it to the list of bundles to potentially start.
	                    if (b != null) {
	                        startBundleList.add(b);
	                    }
	                } catch (BundleException ex) {
	                    println("Deploy wrap jar install: "
	                        + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
	                }
	            }

	            // Uninstall all bundles not in the auto-deploy directory if
	            // the 'uninstall' action is present.
	            if (actionList.contains(AUTO_DEPLOY_UNINSTALL_VALUE)) {
	                for (Iterator it = installedBundleMap.entrySet().iterator(); it.hasNext(); ) {
	                    Map.Entry entry = (Map.Entry) it.next();
	                    Bundle b = (Bundle) entry.getValue();
	                    if (b.getBundleId() != 0) {
	                        try {
	                            b.uninstall();
	                        } catch (BundleException ex) {
	                         	println("Deploy wrap jar uninstall: "
	                            	+ ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
	                        }
	                    }
	                }
	            }

	            // Start all installed and/or updated bundles if the 'start'
	            // action is present.
	            if (actionList.contains(AUTO_DEPLOY_START_VALUE)) {
	                for (int i = 0; i < startBundleList.size(); i++) {
	                    try {
	                        if (!isFragment((Bundle) startBundleList.get(i))) {
	                            ((Bundle) startBundleList.get(i)).start();
	                        }
	                    } catch (BundleException ex) {
	                        println("Deploy wrap jar start: "
	                            + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
	                    }
	                }
	            }
	        }
	    }
		
	    private static String nextLocation(StringTokenizer st) {
	        String retVal = null;
	        if (st.countTokens() > 0) {
	            String tokenList = "\" ";
	            StringBuffer tokBuf = new StringBuffer(10);
	            String tok = null;
	            boolean inQuote = false;
	            boolean tokStarted = false;
	            boolean exit = false;
	            while ((st.hasMoreTokens()) && (!exit)) {
	                tok = st.nextToken(tokenList);
	                if (tok.equals("\"")) {
	                    inQuote = ! inQuote;
	                    if (inQuote) {
	                        tokenList = "\"";
	                    } else {
	                        tokenList = "\" ";
	                    }

	                } else if (tok.equals(" ")) {
	                    if (tokStarted) {
	                        retVal = tokBuf.toString();
	                        tokStarted=false;
	                        tokBuf = new StringBuffer(10);
	                        exit = true;
	                    }
	                }
	                else {
	                    tokStarted = true;
	                    tokBuf.append(tok.trim());
	                }
	            }
	            // Handle case where end of token stream and
	            // still got data
	            if ((!exit) && (tokStarted)) {
	                retVal = tokBuf.toString();
	            }
	        }
	        return retVal;
	    }
	    private static boolean isFragment(Bundle bundle) {
	        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
	    }
	}
    
}