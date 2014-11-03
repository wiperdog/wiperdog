import com.gmongo.GMongo

class MONGODBConnectionSource extends IDBConnectionSource{
	def gmongo;
	def dbInfo
	def mapMongo = [:]
	
	def MONGODBConnectionSource(){
		super()
	}
	
	def newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params, logdir_params){
		this.dbInfo = dbInfo
		def connStr = dbInfo.dbconnstr
		def dbtype = dbInfo.strDbType.replaceAll('@','')
		def userMongo = dbInfo.user
		def pwdMongo = dbInfo.pass
		def dbMongo = dbInfo.db
		// variables connect to mongodb
		gmongo = new GMongo(connStr)
		mapMongo["gmongo"] = gmongo
		gmongo.metaClass.mixin(DBMSInfo)
		
		// define variables connect to database
		def db = ""
		// get database
		if(dbMongo != null && dbMongo != '') {
			db = gmongo.getDB(dbMongo)
		}
		// connect with user and password
		if(userMongo != null && userMongo != '') {
			char[] passArray = []
			if(pwdMongo != null && pwdMongo != ''){
				//pwdMongo = decryptPassword(pwdMongo)
				passArray = pwdMongo.toCharArray()
			}
			// Authenticate user and password connect to database
			db.authenticate(userMongo, passArray)
		}
		mapMongo["sql"] = db
		
		if (datadir_params.dbtype != null && datadir_params.dbtype.size() > 0) {
			gmongo.datadirectory = getIst_params(dbtype,gmongo,datadir_params)
		}
		if (dbversion_params.dbtype != null && dbversion_params.dbtype.size() > 0) {
			gmongo.dbmsversion = getIst_params(dbtype,gmongo,dbversion_params)
		}
		if (programdir_params.dbtype != null && programdir_params.dbtype.size() > 0) {
			gmongo.programdirectory = getIst_params(dbtype,gmongo,programdir_params)
		}
		if (logdir_params.dbtype != null && logdir_params.dbtype.size() > 0) {
			gmongo.logdirectory = getIst_params(dbtype,gmongo,logdir_params)
		}
		return mapMongo
	}
	
	def getIst_params(dbtype,sqlConnection,listParams){
		def defaultData = listParams.MONGO['default']
		def sqlData = listParams.MONGO.getData.sql
		def appendData = listParams.MONGO.getData.append
		return getParamsData(defaultData,sqlConnection,sqlData,appendData)
	}
	
	def getParamsData(defaultData,sqlConnectionData,sqlData,appendData){
		// sqlConnectionData process sqlData like rows, query or command
		// Adding appendData if needed
		// Return defaultData if it encounter any problem
		return defaultData
	}
	
	def savePassword(dbtype, instanceId, dbuser, passwd){
	}
	
	def closeConnection(){
		if(gmongo != null){
			gmongo.close()
		}
	}
	
	def getConnection(){
		return mapMongo
	}
	
	/**
	 * For MongoDb, we will bind these variables for job to use
	 * 
	 */ 
	def getBinding(){
		def conn = getConnection()
		def user = dbInfo.user
		def passStr = getPassword(dbInfo)
		return [gmongo : conn.gmongo, sql : conn.sql, mongoUser : user, mongoPassword : passStr]
	}
}
