import com.gmongo.GMongo
import com.mongodb.DB
import com.mongodb.MongoException
import com.mongodb.DBObject
import com.mongodb.util.JSON
import org.apache.log4j.Logger

class MongoDBConnection {
	def static logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	def static final DEFAULT_HOST = "localhost"
	def static final DEFAULT_PORT = 27017
	def static final DEFAULT_DBNAME = "wiperdog"
	def static final DEFAULT_USER = ''
 	def static final DEFAULT_PASS = ''
 	def static final COMMON_UTIL_FILE = "/libs.target/CommonUltis.groovy"
 	def static gmongo
 	def static db
 	def static properties = MonitorJobConfigLoader.getProperties()
	/**
	 * getConnection: create a connection with default params
	 * @return mapMongoDb: map connect db info
	*/
	synchronized def static getWiperdogConnection() {
		def mapMongoDb = [:]
		def authFlg = true
		try {
			def decidedHost = properties.get(ResourceConstants.MONGODB_HOST) != null ? properties.get(ResourceConstants.MONGODB_HOST) : DEFAULT_HOST
			def decidedPort = properties.get(ResourceConstants.MONGODB_PORT) != null ? properties.get(ResourceConstants.MONGODB_PORT) : DEFAULT_PORT
			def decidedDbName = properties.get(ResourceConstants.MONGODB_DBNAME) != null ? properties.get(ResourceConstants.MONGODB_DBNAME) : DEFAULT_DBNAME
			def decidedUser = properties.get(ResourceConstants.MONGODB_USER) != null ? properties.get(ResourceConstants.MONGODB_USER) : DEFAULT_USER
			def decidedPass = properties.get(ResourceConstants.MONGODB_PASS) != null ? properties.get(ResourceConstants.MONGODB_PASS) : DEFAULT_PASS
			logger.debug("Try to connect to mongoDB with host: $decidedHost, port: $decidedPort, dbName: $decidedDbName, user: $decidedUser, pass: $decidedPass")
			if(MongoDBConnection.gmongo == null || MongoDBConnection.db == null) {
				if(decidedHost == "localhost" && decidedPort == null) {
					MongoDBConnection.gmongo = new GMongo()
				} else if(decidedHost == "localhost" && decidedPort != null) {
					MongoDBConnection.gmongo = new GMongo(decidedHost + ":" + decidedPort)
				} else if(decidedHost != null && decidedPort != null) {
					MongoDBConnection.gmongo = new GMongo(decidedHost, Integer.valueOf(decidedPort))
				}
			}
			MongoDBConnection.db = MongoDBConnection.gmongo.getDB(decidedDbName)
			if(decidedPass != null && decidedPass != ''){
				decidedPass = decryptPassword(decidedPass)
			}
			//check user and db authen
			if(decidedUser != null && decidedUser != '' && !MongoDBConnection.db.isAuthenticated()) {
				// Authenticate user and password connect to database
				char[] passArray = []
				if(decidedPass != null && decidedPass != ''){
					passArray = decidedPass.toCharArray()
				}
				authFlg = MongoDBConnection.db.authenticate(decidedUser, passArray)
			}
			if (authFlg) {
				mapMongoDb['gmongo'] = gmongo
				mapMongoDb['db'] = db
			} else {
				println "Can't authenticate MongoDB(" + decidedHost + ") with user : " + decidedUser
				db = null
				gmongo = null
				mapMongoDb = [:]
			}
		} catch(Exception ex) {
			println "getWiperdogConnection error: " + ex
			db = null
			gmongo = null
			mapMongoDb = [:]
		}
		return mapMongoDb
	}
	/**
	 * createConnection: create a connection with host, port, db
	 * @param mapDetailDestination: format [host: a, port: b, db: c, user: d, pass: e]
	 * @return mongo: gmongo object
	*/
	def createConnection(mapDetailDestination){
		def mongo
		def mapMongoDb = [:]
		def authFlg = true
		logger.debug("Try to connect to mongoDB with $mapDetailDestination")
		try {
			def port = mapDetailDestination['port']
			def host = mapDetailDestination['host']
			def dbName = mapDetailDestination['db']
			def user = mapDetailDestination['user']
			def pass = mapDetailDestination['pass']
			if(host == "localhost" && port == null){
				mongo = new GMongo()
			}else if(host == "localhost" && port != null) {
				mongo = new GMongo(host + ":" + port)
			} else if(host != null && port != null) {
				mongo = new GMongo(host, Integer.valueOf(port))
			}
	        if(pass != null && pass != ""){
	        	//decryped password
				pass = decryptPassword(pass)
			}
			def dbConn = mongo.getDB(dbName)
			if(user != null && user != '' && !dbConn.isAuthenticated()) {
				// Authenticate user and password connect to database
				char[] passArray = []
				if(pass != null && pass != ''){
					passArray = pass.toCharArray()
				}
				authFlg = dbConn.authenticate(user, passArray)
			}
			if (authFlg) {
				mapMongoDb['gmongo'] = mongo
				mapMongoDb['db'] = dbConn
			} else {
				println "Can't authenticate MongoDB(" + host + ") with user : " + user
				mongo = null
				dbConn = null
				mapMongoDb = [:]
			}
		} catch(Exception ex) {
			logger.debug("Can not connect to mongoDB with $mapDetailDestination")
			logger.debug(ex)
			mongo = null
			mapMongoDb = [:]
		}
		return mapMongoDb
	}
	/**
	 * decrypt password connect to mongodb
	 * @param pass password encrypted
	 * @return password password decrypted
	*/
	def static decryptPassword(pass) {
		//get wiperdog home
		def wiperdogHome = properties.get(ResourceConstants.GROOVY_FILE_DIRECTORY)
		//get CommonUtil file
	    def commonUtilFile = new File( wiperdogHome + COMMON_UTIL_FILE)
	    //Parse file
		GroovyClassLoader gcl = new GroovyClassLoader()
		Class commonClass = gcl.parseClass(commonUtilFile)
		Object commonUtil_obj = commonClass.newInstance()
		//decrypt password
		def password = commonUtil_obj.decrypt(pass)
		return password
	}
	/**
	 * closeConnection: close a connection
	 * @param gmongoObj: gmongo object
	*/
	def closeConnection(gmongoObj) {
		gmongoObj.close()
	}
}