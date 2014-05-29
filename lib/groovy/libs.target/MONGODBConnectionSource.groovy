import com.gmongo.GMongo

class MONGODBConnectionSource extends IDBConnectionSource{
	def gmongo;
	def dbInfo
	
	def MONGODBConnectionSource(){
		super()
	}
	
	def newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params, logdir_params){
		this.dbInfo = dbInfo
		def connStr = dbInfo.dbconnstr
		def dbtype = dbInfo.strDbType.replaceAll('@','')
		gmongo = new GMongo(connStr)
		
		gmongo.metaClass.mixin(DBMSInfo)
		if (datadir_params != null) {
			gmongo.datadirectory = getIst_params(dbtype,gmongo,datadir_params)
		}
		if (dbversion_params != null) {
			gmongo.dbmsversion = getIst_params(dbtype,gmongo,dbversion_params)
		}
		if (programdir_params != null) {
			gmongo.programdirectory = getIst_params(dbtype,gmongo,programdir_params)
		}
		if (logdir_params != null) {
			gmongo.logdirectory = getIst_params(dbtype,gmongo,logdir_params)
		}
		return gmongo
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
		return gmongo
	}
	
	/**
	 * For MongoDb, we will bind these variables for job to use
	 * 
	 */ 
	def getBinding(){
		def conn = getConnection()
		def user = dbInfo.user
		def passStr = getPassword(dbInfo)
		return [gmongo : conn, mongoUser : user, mongoPassword : passStr]
	}
}
