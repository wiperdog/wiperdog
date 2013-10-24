import org.osgi.framework.BundleContext
import org.osgi.service.cm.Configuration
import org.osgi.service.cm.ConfigurationAdmin
import org.osgi.framework.ServiceReference

/**
 * get config information
 *
 */
class MonitorJobConfigLoader{

	static Dictionary properties
	/**
	 * constructor
	 */
	public MonitorJobConfigLoader(BundleContext context) {
		ServiceReference caRef = context.getServiceReference(ConfigurationAdmin.class.getName())
		def configAdmin = context.getService(caRef)
		def config = configAdmin.getConfiguration(ResourceConstants.SERVICE_PID)
		Dictionary props = config.getProperties()
		if (props != null) {
			this.properties = props
		} else {
			props =	setDefault()
			this.properties = props
		}
		this.properties = props
	}
	
	/**
	 * get config information
	 * @return properties
	 */
	public static Dictionary getProperties() {
		if(this.properties == null){
			this.properties = setDefault()
		}
		return this.properties;
	}
	
	/**
	 * set default value
	 * @param properties: properties
	 */
	private static Dictionary setDefault() {
		Dictionary newProperties = new Hashtable()
		
		def rootDir = System.getProperty("felix.home")
		if(rootDir == null){
			rootDir = (new File(System.getProperty("bin_home"))).getParent()
		}
		newProperties.put(ResourceConstants.JOB_DIRECTORY, rootDir + '/var/job')
		newProperties.put(ResourceConstants.MONITORJOBDATA_DIRECTORY, rootDir + '/tmp')
		newProperties.put(ResourceConstants.JOB_PARAMETERS_DIRECTORY, rootDir + '/var/job')
		newProperties.put(ResourceConstants.DEFAULT_PARAMETERS_DIRECTORY, rootDir + '/var/conf')
		newProperties.put(ResourceConstants.MESSAGE_FILE_DIRECTORY, rootDir + '/var/conf')
		newProperties.put(ResourceConstants.DBPASSWORD_FILE_DIRECTORY, rootDir + '/var/conf')
		newProperties.put(ResourceConstants.SYSTEM_PROPERTIES_FILE_DIRECTORY, rootDir + '/etc')
		newProperties.put(ResourceConstants.GROOVY_FILE_DIRECTORY, rootDir + '/lib/groovy')
		newProperties.put(ResourceConstants.MONITORSWAP_DIRECTORY, rootDir + '/var/swap/monitorjob')
		newProperties.put(ResourceConstants.MAX_NUMBER_SWAP_FILE, 1024)
		newProperties.put(ResourceConstants.MAX_SWAP_DIRECTORY_SIZE, 10)
		newProperties.put(ResourceConstants.SLEEP_TIME_MS, 5000)
		newProperties.put(ResourceConstants.MONGODB_HOST, '127.0.0.1')
		newProperties.put(ResourceConstants.MONGODB_PORT, 27017)
		newProperties.put(ResourceConstants.MONGODB_DBNAME, 'wiperdog')
		newProperties.put(ResourceConstants.MONGODB_USER, '')
		newProperties.put(ResourceConstants.MONGODB_PASS, '')
		newProperties.put(ResourceConstants.MONGODB_PASS_CONFIG, rootDir + '/etc/mongodbpass.cfg')
		return newProperties
	}
}
