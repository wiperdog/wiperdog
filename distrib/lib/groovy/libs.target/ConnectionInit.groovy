/*
 *  Copyright 2013 Insight technology,inc. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import java.sql.SQLException;


/**
 * @author hkurohara
 *
 */
class ConnectionInit {    
	/**
	 * Get database connection
	 * @param db instance of database connection
	 * @param mapDBConnections contains all active connection
	 * @param binding Jobs binding
	 * @param strDbType database type as string
	 * @param connectionString connection string
	 * @param userString user name
	 * @return instance of database connection
	 */
	public Object getDbConnection(mapDBConnections, iDBConnectionSource, binding, strDbType, connectionString, userString){
		def db = null
		def params = binding.getVariable("parameters")
		def datadir_params = params.datadirectory
		def dbversion_params = params.dbmsversion
		def programdir_params = params.programdirectory
		def logdir_params = params.dblogdir				
		
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
						db = iDBConnectionSource.newSqlInstance(strDbType, connectionString, null, userString,datadir_params,dbversion_params,programdir_params,logdir_params)
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
		if (strDbType == 'POSTGRES') {
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
