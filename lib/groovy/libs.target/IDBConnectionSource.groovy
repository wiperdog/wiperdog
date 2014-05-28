import groovy.sql.Sql
import org.apache.log4j.Logger;
	
public abstract class IDBConnectionSource implements DBConnectionSource{
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	def properties = MonitorJobConfigLoader.getProperties()
	def messageFile = new File(properties.get(ResourceConstants.MESSAGE_FILE_DIRECTORY) + "/message.properties")
	def mapMessage = [:]
	def static listDBConnections = []
		
	def abstract getIst_params(dbtype,sqlConnection,listParams)
	def abstract getParamsData(defaultData,sqlConnectionData,sqlData,appendData)
	
	public IDBConnectionSource(){
		messageFile.each {
			def toArray = it.split(" = ")
			mapMessage[toArray[0]] = toArray[1]
		}
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
		// Get decryptedPassword
		def decryptedPassword = ""
		try{
			if ((passwordString != null) && (passwordString != "")) {
				decryptedPassword = CommonUltis.decrypt(passwordString)
			}
		}catch (Exception ex){
			logger.info (mapMessage['ERR007'])
			return null
		}
		return decryptedPassword
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
	* Get pwdFile
	* @param mapDbInfo connect DB information
	* @return pwdFileOutput password file
	*/
	public File getPwdFile(mapDbInfo) {
		def pwdFileName = CommonUltis.getPasswdFileName(mapDbInfo.strDbType, mapDbInfo.dbHostId, mapDbInfo.dbSid)
		File pwdFileOutput = new File(properties.get(ResourceConstants.DBPASSWORD_FILE_DIRECTORY) + "/" + pwdFileName)
		return pwdFileOutput
	}
	
	/**
	 * Return map for binding into job
	 * Format : [name:value]
	 */
	def getBinding(){
		return [:]
	}

}
