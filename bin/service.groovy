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
println """${new Date()}
******************************
* WinNT Service is starting. *
******************************"""
public class WiperDogService{
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
	
	/**
	 * Install bundle
	 */
	private static List installall(context, listURL) {
		def lstBundle = []
		listURL.each { url ->
			def bundle = null
			try {
				bundle = context.installBundle(url)
				lstBundle.add(bundle)
			} catch(Exception e) {
				println org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e)
			}
		}
		return lstBundle
	}
	
	/**
	 * Start bundle
	 */
	private static void startall(listBunlde) {
		listBunlde.each { b ->
			try {
				b.start()
			} catch(Exception e) {
				println e
			}
		}
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
    
    //Read list bundle and runlevel from csv file
    public static List processCSVFile(filePath){
		def listBundleFromCSV = []
		def fileCSV = new File(filePath)
		if(!fileCSV.exists()){
			println "File not found : " + fileCSV.getName()
		} else {
			def checkHeader = false
			def headers = []
			def csvData = fileCSV.readLines()
			csvData.find{ line ->
				if(!checkHeader){
					headers = line.split(",",-1)
					checkHeader = true
					if(headers[0] != "TYPE"){
						checkHeader = false
					}
					if(headers[1] != "PATH"){
						checkHeader = false
					}
					if(headers[2] != "RUNLEVEL"){
						checkHeader = false
					}
					if(headers[3] != "OBJECT"){
						checkHeader = false
					}
					if(!checkHeader){
						println "Incorrect headers file format - Format headers mustbe: TYPE, PATH, LEVEL, OBJECT - Line: " + (csvData.indexOf(line) + 1)
						return true
					}
				} else {
					def value = line.split(",",-1)
					value = value.collect{it = escapeChar(it)}
					if (value.size == 4) {
						if(value[0] == "" || value [1] == "" || value [2] == ""){
							println "Value of TYPE , PATH OR RUNLEVEL can not be empty - Line: " +   (csvData.indexOf(line) + 1)
							return
						}
						def tmpMap = [:]
						for(int i=0 ; i < headers.length;i++){
							tmpMap[headers[i]] = value[i]
						}
						listBundleFromCSV.add(tmpMap)
						tmpMap = [:]
					} else {
							println "Missing params. Need 4 data for TYPE, PATH, RUNLEVEL and OBJECT - Line: " +   (csvData.indexOf(line) + 1)
							return
					}
				}
			}
		}
		return listBundleFromCSV
	}
	
	public static String escapeChar(str){
		return str.replace("'","").replace('"',"").trim()
	}
}


//while(!service.shutdownRequested){
	
		println "=================================== GOOOOOOOOOOOOOOOOOOO ==============================================="
		// Load system properties.
		WiperDogService.loadSystemProperties()
		
		// Read configuration properties.
        Properties configProps = WiperDogService.loadConfigProperties();
        // If no configuration properties were found, then create
        // an empty properties object.
        if (configProps == null) {
            println("No " + WiperDogService.CONFIG_PROPERTIES_FILE_VALUE + " found.");
            configProps = new Properties();
        }
        
        // Copy framework properties from the system properties.
        WiperDogService.copySystemProperties(configProps);
        
		// Create an instance of the framework.
		FrameworkFactory factory = WiperDogService.getFrameworkFactory();
		m_fwk = factory.newFramework(configProps);
        // Initialize the framework and start
        m_fwk.init();
		m_fwk.start();
		
		//Install and start bundle			
		def felix_home = System.getProperty("felix.home").replace("\\", "/");
		def context = m_fwk.getBundleContext()
		//Get list bundle and order by run level
		def bundleList = WiperDogService.processCSVFile(felix_home + "/etc/ListBundle.csv")
		def mapBundle = [:]
		
		bundleList.each { bundleCfg ->
			def bundle = null
			def url = ""
			if (bundleCfg['TYPE'] == "file")  {
				url =  (new File(felix_home, bundleCfg['PATH'])).toURI().toString()
			} else if (bundleCfg['TYPE'] == "wrapfile") {
				url = "wrap:" + (new File(felix_home, bundleCfg['PATH'])).toURI().toString()
			} else if (bundleCfg['TYPE'] == "mvn") {
				url = "mvn:" + bundleCfg['PATH'].replaceAll(":", "/")
			} else if (bundleCfg['TYPE'] == "wrapmvn") {
				url = "wrap:mvn:" + bundleCfg['PATH'].replaceAll(":", "/")
			} else {
				println ("Unknow resource: " + bundleCfg)
			}
			if (url != "") {
				if (mapBundle[bundleCfg["RUNLEVEL"]] == null) {
					mapBundle[bundleCfg["RUNLEVEL"]] = []
				}
				mapBundle[bundleCfg["RUNLEVEL"]].add(url)
			}
		}
		
		//Install and start bundle
		def listBundle = []
		mapBundle.each {runLevel, listURL->
			listBundle = WiperDogService.installall(context, listURL)
			WiperDogService.startall(listBundle)
		}
		
		// Wait for framework to stop to exit the VM.
		try {
        	m_fwk.waitForStop(0);
        }finally {
    		System.exit(0);
		}