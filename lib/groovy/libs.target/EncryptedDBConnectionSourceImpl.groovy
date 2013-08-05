import groovy.sql.Sql
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.sql.SQLClientInfoException;
import java.sql.SQLException
import java.text.MessageFormat
	
public class EncryptedDBConnectionSourceImpl implements IDBConnectionSource{
	
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")	
	
	def properties = MonitorJobConfigLoader.getProperties()		
	
	def messageFile = new File(properties.get(ResourceConstants.MESSAGE_FILE_DIRECTORY) + "/message.properties")
	
	def mapMessage = [:]
	
	/**
	* Get new instances sql
	* @param dbInfo
	* @param datadir_params
	* @param dbversion_params
	* @param programdir_params
	* @param logdir_params
	* @return sql
	*/
	public Sql newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params,logdir_params){
		def sql = null
		def strPassword = null
		
		def connstr = dbInfo.dbconnstr
		def dbuser = dbInfo.user
		def dbtype = dbInfo.strDbType
		def strDriver = dbInfo.strDbTypeDriver
		
		messageFile.each {
			def toArray = it.split(" = ")
			mapMessage[toArray[0]] = toArray[1]
		}
		// Get password
		strPassword = getPassword(dbInfo)
		// Get decryptedPassword
		def decryptedPassword = ""
		try{
			if ((strPassword != null) && (strPassword != "")) {
				decryptedPassword = CommonUltis.decrypt(strPassword)
			}
		}catch (Exception ex){
			logger.info (mapMessage['ERR007'])
			return null
		}
		// Get sql
		try{
			sql = Sql.newInstance(connstr, dbuser, decryptedPassword, strDriver)
			Sql.metaClass.invokeMethod = {name, args->
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
		}catch (SQLException ex) {
			logger.info (MessageFormat.format(mapMessage['ERR001'], "'" + connstr, dbuser, replacePasswdString(decryptedPassword) + "'"))
		}
		return sql
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
	* Get password
	* @param mapDbInfo connect DB information
	* @return passwordString
	*/
	public String getPassword(mapDbInfo) {
		def pwdFile = null
		def map=[:]
		def tempArray = [:]
		def passwordString = null
		try{
			pwdFile = getPwdFile(mapDbInfo)
			if (pwdFile != null) {
				pwdFile.eachLine {
						tempArray = it.split(":")
						map[tempArray[0]] = tempArray[1].trim()
				}
				map.each {it->
					if (it.key == mapDbInfo.user) {
						passwordString = it.value
					}
				}
			}
			if(passwordString == null) {
				logger.info (mapMessage['ERR007'])
			}
		}catch (FileNotFoundException e) {
			logger.error(e.toString())
			return null
		} catch(Exception e) {
			logger.debug(e.toString())
			return null
		}
		return passwordString
	}

	/**
	* If DB user haven't set, get default user
	* @param mapDbInfo connect DB information
	* @return strUserName
	*/
	public String getDefaultUser(mapDbInfo)	{
		def strUserName = null
		def pwdFile = null
		def array=[:]
		def map=[:]
		def tempArray = [:]
		try{
			pwdFile = getPwdFile(mapDbInfo)
			if (pwdFile != null) {
				pwdFile.eachLine {
						tempArray = it.split(":")
						map[tempArray[0]] = tempArray[1].trim()
				}
				strUserName = map[ResourceConstants.DEFAULTUSER]
			}
		}catch (FileNotFoundException e) {
			logger.error(e.toString())
		} catch(Exception e) {
			logger.debug(e.toString())
		}
		return strUserName
	}

	/**
	* Save password
	* @param dbtype
	* @param instanceId
	* @param dbuser
	* @param passwd
	*/
	public void savePassword(dbtype, instanceId, dbuser, passwd){
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
	private String getParamsData(defaultData,sqlConnectionData,sqlData,appendData) {
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
	private String getIst_params(dbtype,sqlConnection,listParams) {
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

	/**
	* Get DBMS data directory
	* @param dbtype
	* @param sqlConnection
	* @param datadir_params
	* @return datadirectory
	*/
	public String getIst_datadirectory(dbtype,sqlConnection,datadir_params){
		def datadirectory = null
		try{
			datadirectory = getIst_params(dbtype,sqlConnection,datadir_params)
		} catch(Exception ex) {
			logger.info (MessageFormat.format("Error when get data directory : " + ex))
		}
		return datadirectory
	}

	/**
	* Get DBMS db version
	* @param dbtype
	* @param sqlConnection
	* @param dbversion_params
	* @return dbmsversion
	*/
	public String getIst_dbmsversion(dbtype,sqlConnection,dbversion_params){
		def dbmsversion = null
		try{
			dbmsversion = getIst_params(dbtype,sqlConnection,dbversion_params)
		} catch (Exception ex) {
			logger.info (MessageFormat.format("Error when get dbms version : " + ex))
		}
		return dbmsversion
	}

	/**
	* Get DBMS program directory
	* @param dbtype
	* @param sqlConnection
	* @param programdir_params
	* @return programdirectory
	*/
	public String getIst_programdirectory(dbtype,sqlConnection,programdir_params){
		def programdirectory = null
		try{
			programdirectory = getIst_params(dbtype,sqlConnection,programdir_params)
			return programdirectory
		} catch (Exception ex) {
			logger.info (MessageFormat.format("Error when get program directory : " + ex))
		}
		return programdirectory
	}

	/**
	* Get DBMS log directory
	* @param dbtype
	* @param sqlConnection
	* @param logdir_params
	* @return logdirectory
	*/
	public String getIst_logdirectory(dbtype,sqlConnection,logdir_params){
		def logdirectory = null
		try{
			logdirectory = getIst_params(dbtype,sqlConnection,logdir_params)
		} catch (Exception ex) {
			logger.info (MessageFormat.format("Error when get log directory : " + ex))
		}
		return logdirectory
	}
	
	/**
	* Get pwdFile
	* @param mapDbInfo connect DB information
	* @return pwdFileOutput password file
	*/
	public File getPwdFile(mapDbInfo) {
		def pwdFileName = CommonUltis.getPasswdFileName(mapDbInfo.strDbType, mapDbInfo.dbHostId, mapDbInfo.dbSid)
		File pwdFileOutput = new File(properties.get(ResourceConstants.DBPASSWORD_FILE_DIRECTORY) + "/" + pwdFileName)
		return pwdFileOutput
	}
}

// 2013-04-15 Luvina Insert Start
public class DBMSInfo {
	def datadirectory = ""
	def programdirectory = ""
	def dbmsversion = ""
	def logdirectory = ""
}
// 2013-04-15 Luvina Insert End
