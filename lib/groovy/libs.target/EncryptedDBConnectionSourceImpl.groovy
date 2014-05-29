import groovy.sql.Sql
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import org.apache.commons.codec.binary.Base64;

import java.sql.SQLClientInfoException;
import java.sql.SQLException
import java.text.MessageFormat
	
public class EncryptedDBConnectionSourceImpl extends IDBConnectionSource{
	def sql = null
	
	def EncryptedDBConnectionSourceImpl(){
		super()
	}
	
	/**
	* Get new instances sql
	* @param dbInfo
	* @param datadir_params
	* @param dbversion_params
	* @param programdir_params
	* @param logdir_params
	* @return sql
	*/
	def newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params,logdir_params){
		def strPassword = null
		
		def connstr = dbInfo.dbconnstr
		def dbuser = dbInfo.user
		def dbtype = dbInfo.strDbType
		def strDriver = dbInfo.strDbTypeDriver
		
		// Get password
		strPassword = getPassword(dbInfo)
		
		// Get sql
		sql = tryConnect(dbtype, connstr, dbuser, strPassword, strDriver, 20)
		
		// If we can get connection to the database, we can mixin some properties 
		// for more detail information of this connection
		if(sql != null){
			sql.metaClass.invokeMethod = {name, args->
				if("rows".equals(name) || "execute".equals(name) || "eachRow".equals(name) || "firstRow".equals(name)){
					for(int carg = 0; carg < args.length; carg++){
						if(args[carg] instanceof String){
							args[carg] = "/* SQL by PIEX MONITORJOB */\n" + args[carg]
						}
					}
				}
				def metaMethod = Sql.metaClass.getMetaMethod(name, args)
				metaMethod.invoke(sql, args)
			}
			sql.metaClass.mixin(DBMSInfo)
			if (datadir_params != null) {
				sql.datadirectory = getIst_datadirectory(dbtype,sql,datadir_params)
			}
			if (dbversion_params != null) {
				sql.dbmsversion = getIst_dbmsversion(dbtype,sql,dbversion_params)
			}
			if (programdir_params != null) {
				sql.programdirectory = getIst_programdirectory(dbtype,sql,programdir_params)
			}
			if (logdir_params != null) {
				sql.logdirectory = getIst_logdirectory(dbtype,sql,logdir_params)
			}
		} else {
			logger.info ("After 20 times try to connect with " + connstr + " " + dbuser + "/" + replacePasswdString(strPassword) + ". Cannot connect!")
		}
	}
	
	/**
	 * Trying to connect to database in a numberOfTime
	 * sleep 500ms between each attemp
	 * 
	 */ 
	def tryConnect(dbtype, connstr, dbuser, strPassword, strDriver, numberOfTime){
		def sql
		synchronized (listDBConnections) {
			def listRemove = []
			listDBConnections.each{conn->
				if(conn.dbtype == dbtype && conn.user == dbuser && conn.connstr == connstr && conn.driver == strDriver){
					if(checkValidDbConnection(conn.sql, dbtype)){
						sql = conn.sql
					}else{
						listRemove.add(conn)
					}
				}
			}
			listDBConnections.removeAll(listRemove)
			
			if(sql == null){
				for(int i = 0; i < numberOfTime; i++){
					try{
						sql = Sql.newInstance(connstr, dbuser, strPassword, strDriver)
						if(sql != null){
							def conn = ["sql" : sql, "user" : dbuser, "connstr" : connstr, "driver" : strDriver, "dbtype" : dbtype]
							listDBConnections.add(conn)
							break;
						}
					}catch(SQLException ex){
						logger.info (MessageFormat.format(mapMessage['ERR001'], "'" + connstr, dbuser, replacePasswdString(strPassword) + "'"))
						sleep(500)
					}
				}
			}
		}
		return sql
	}
	
	/**
	 * Return map for binding into job
	 */
	def getBinding(){
		def conn = getConnection()
		return [sql:conn]
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

	/**
	* Get driver string
	* @param dbtypeString type of DBMS
	* @return driverString
	*/
	public String getDriverString(dbtypeString) {
		def driverString
		switch (dbtypeString) {
			case ResourceConstants.ORACLE:
				driverString = ResourceConstants.DEF_ORACLE_DRIVER
				break;
			case ResourceConstants.MYSQL:
				driverString = ResourceConstants.DEF_MYSQL_DRIVER
				break;
			case ResourceConstants.POSTGRES:
				driverString = ResourceConstants.DEF_POSTGRES_DRIVER
				break;
			case ResourceConstants.SQLS:
				driverString = ResourceConstants.DEF_SQLS_DRIVER
				break;
			default:
				break;
		}
		return driverString
	}


	/**
	* Save password
	* @param dbtype
	* @param instanceId
	* @param dbuser
	* @param passwd
	*/
	def savePassword(dbtype, instanceId, dbuser, passwd){
	}

	/**
	* Change password
	* @param passwd
	* @return passwd new password
	*/
	private replacePasswdString(String passwd) {
		def regex = /./
		return passwd.replaceAll(regex,'*')
	}

	/**
	* Get params with sql
	* @param defaultData
	* @param sqlConnectionData
	* @param sqlData
	* @param appendData
	* @return strParams
	*/
	def getParamsData(defaultData,sqlConnectionData,sqlData,appendData) {
		def strParams = ""
		def params = []
		if ((defaultData != null) && (defaultData != "")) {
			strParams = defaultData
		} else {
			if(sqlData != "" && sqlData != null) {
				params= sqlConnectionData.rows(sqlData)
				if ((params != null) && (params.size() > 0) && (params[0] != null)) {
					strParams = params[0][0].toString().replaceAll("[\\[\\]]", "")
					if(appendData != ""){
						strParams = strParams + appendData
					} 
				}
			}
		}
		return strParams
	}

	/**
	* Get params with dbtype
	* @param dbtype
	* @param sqlConnection
	* @param listParams
	* @return istParmas
	*/
	def getIst_params(dbtype,sqlConnection,listParams) {
		def istParmas = null
		def defaultData
		def sqlData
		def appendData
		if(dbtype == ResourceConstants.ORACLE) {
			defaultData = listParams.ORACLE.default
			sqlData = listParams.ORACLE.getData.sql
			appendData = listParams.ORACLE.getData.append
			istParmas = getParamsData(defaultData,sqlConnection,sqlData,appendData)
		} else if(dbtype == ResourceConstants.SQLS) {
			defaultData = listParams.SQLS.default
			sqlData = listParams.SQLS.getData.sql
			appendData = listParams.SQLS.getData.append
			istParmas = getParamsData(defaultData,sqlConnection,sqlData,appendData)
		} else if(dbtype == ResourceConstants.MYSQL) {
			defaultData = listParams.MYSQL.default
			sqlData = listParams.MYSQL.getData.sql
			appendData = listParams.MYSQL.getData.append
			istParmas = getParamsData(defaultData,sqlConnection,sqlData,appendData)
		} else if(dbtype == ResourceConstants.POSTGRES) {
			defaultData = listParams.POSTGRES.default
			sqlData = listParams.POSTGRES.getData.sql
			appendData = listParams.POSTGRES.getData.append
			istParmas = getParamsData(defaultData,sqlConnection,sqlData,appendData)
		}
		return istParmas
	}
	
	def closeConnection(){
		// Nothing to do here
		// This DBConnectionSource has a special mechanism to manage connections
	}
	
	def getConnection(){
		return sql
	}
}

