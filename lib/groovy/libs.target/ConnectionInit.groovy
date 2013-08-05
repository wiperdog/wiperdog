import java.sql.SQLException;


/**
 * @author hkurohara
 *
 */
class ConnectionInit {    
	/**
	 * Get database connection
	 * @param mapDBConnections contains all active connection
	 * @param binding Jobs binding
	 * @param iDBConnectionSource IDBConnectionSource object
	 * @param dbInfo connect DB information
	 * @return instance of database connection
	 */
	public Object getDbConnection(mapDBConnections, iDBConnectionSource, binding, dbInfo){
		def db = null
		def params = binding.getVariable("parameters")
		def datadir_params = params.datadirectory
		def dbversion_params = params.dbmsversion
		def programdir_params = params.programdirectory
		def logdir_params = params.dblogdir				
		
		def strDbType = dbInfo.strDbType
		def userString = dbInfo.user
		def connectionString = dbInfo.dbconnstr
		//Check DB Connection
		def listRemove = []
		for (objConnect in mapDBConnections) {
			if (objConnect.isHasConnect(strDbType, userString, connectionString)) {
				db = objConnect.getConnection()
				def connectionIsValid = checkValidDbConnection(db, strDbType)
				if (!connectionIsValid) {
					listRemove.add(objConnect)
					db = null
				}
			}
		}
		mapDBConnections.removeAll(listRemove)
		
		if ((db == null) || (db.connection == null) || (db.connection.isClosed())) {
			for(int i = 0; i < 20; i++){
				db = iDBConnectionSource.newSqlInstance(dbInfo, datadir_params,dbversion_params,programdir_params,logdir_params)
				if (db != null) {
					def objConnect = new DBConnections(strDbType, userString, connectionString)
					objConnect.setConnection(db)
					mapDBConnections.push(objConnect)
					break
				}
				sleep(500)
			}
		}
		return db
	}
	
	/**
	 * Check if a database connection is valid 
	 * @param db database instance
	 * @param strDbType Type of Database (POSTGRES, MYSQL, SQLS, ORACLE)
	 * @return false if not valid and true for vice versa
	 */
	public boolean checkValidDbConnection(db, strDbType){
		def connectionIsValid = true
		if (strDbType == ResourceConstants.POSTGRES) {
			try{
				db.rows('SELECT 1')
			} catch(SQLException e){
				connectionIsValid = false
			}
		} else {
			connectionIsValid = db.connection.isValid(0)
		}		
		
		return connectionIsValid;
	}
	
	class DBConnections {
		private String dbType;
		private String dbUser;
		private String connStr;
		private Object dbConnect;
		
		DBConnections (dbType, dbUser, connStr) {
			this.dbType = dbType
			this.dbUser = dbUser
			this.connStr = connStr
		}
		def setConnection(Object dbConnect) {
			this.dbConnect = dbConnect
		}
		def getConnection() {
			return this.dbConnect
		}
		def isHasConnect(dbType, dbUser, connStr) {
			if ((this.dbType == dbType) && (this.dbUser == dbUser) && (this.connStr == connStr)) {
				return true
			} else {
				return false
			}
		}
	}
}
