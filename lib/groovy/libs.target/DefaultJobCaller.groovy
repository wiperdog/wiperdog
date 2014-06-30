import groovy.sql.Sql
import java.lang.AssertionError
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.Map
import java.util.Scanner
import groovyx.net.http.*
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.HTML
import groovy.json.*
import groovyx.net.http.RESTClient

import org.apache.log4j.Logger;

import java.sql.SQLException
import groovy.lang.MissingPropertyException
import java.text.MessageFormat
import java.net.URI.Parser;
import java.text.SimpleDateFormat;

import com.google.gson.Gson
import com.google.gson.GsonBuilder;
import groovy.transform.Synchronized

class DefaultJobCaller {
	def clsJob
	def instanceJob	
	
   	String fileName
   	String rootJobName
   	String instanceName
	
	def properties = MonitorJobConfigLoader.getProperties()
	
	static mapDBConnections = []
	
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	
	//def messageFile = new File(System.getProperty("felix.home") + "/var/conf/message.properties")
	def messageFile = new File(properties.get(ResourceConstants.MESSAGE_FILE_DIRECTORY) + "/message.properties")
	
	def mapMessage = [:]
		
	def iDBConnectionSource = new EncryptedDBConnectionSourceImpl()
	
	static int RECORD_SEQ = 0
	def dbInfo
	
	DefaultSender sender
	
	// Mark job is executed successfully or not
	def isJobFinishedSuccessfully 
	DataJuggernaut juggernaut = new DataJuggernaut()

	//Pass job filename to constructor
	def DefaultJobCaller(objJob,fileName, rootJobName, instanceName, DefaultSender sender) {
		
		this.instanceJob = objJob
		this.fileName = fileName
		this.rootJobName = rootJobName
		this.instanceName = instanceName
		this.isJobFinishedSuccessfully = true
		this.sender = sender
	}

	def runCommand(osInfo,commandline, mapFormat) {
		def recordarray = []
		try{
			def procData = null
			try {
				//proc = commandline.execute()
				def proc = new ProcessRunner(osInfo)
				procData = proc.procExecute(commandline,true)					
			} catch (Exception ex) {
				logger.info (MessageFormat.format(mapMessage['ERR005'],"'" + commandline + "'"))
				isJobFinishedSuccessfully = false
				return null
			}
			if (mapFormat != null) {
				def rxMatch = mapFormat['match']
				def rxSplit = mapFormat['split']
				if(procData.out != null) {
					procData.out.split("\n").each { it ->
						// make one record
						if (rxMatch != null) {
							def s = new Scanner(it)
							s.findInLine(rxMatch)
							def newrecord = [:]
							def colindex = 1
							def result = s.match()
							def grpCount = result.groupCount()
							for (i in 1..grpCount) {
								def keyname = mapFormat[colindex]
								if (keyname != null) {
									newrecord[keyname] = result.group(i)
								}
								++colindex
							}
							s.close();
							recordarray.push(newrecord)
						// make one record
						} else if(rxSplit != null) {
							def arySplitResults = it.split(rxSplit)
							def colindex = 1
							def	recordData = [:]
							mapFormat.each {
								// Get key for recordData
								def keyname = mapFormat[colindex]
								if(colindex > 0) {
									if(keyname != null) {
										// Set value for recordData
										recordData[keyname] = arySplitResults[colindex - 1]
									}
								}
								++colindex
							}
							recordarray.push(recordData)
						} else {
							recordarray.push([it])
						}
					}
				}
			} else {
				return procData
			}
		} catch (Exception ex) {
			String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ex)
			logger.info (MessageFormat.format(stackTrace))
			isJobFinishedSuccessfully = false
			return null
		}
		return recordarray
	}
	
	/**
	 * Run QUERY
	 * @param queryString Query string
	 * @param strQueryVariable Query variables
	 * @param dbInfo connect DB information
	 * @return resultData data after executing query
	 */
	
	def runQuery(queryString, strQueryVariable, dbInfo) {
		List resultData = null
		def binding = instanceJob.getBinding()
				
		def params = binding.getVariable("parameters")
		def datadir_params = params.datadirectory
		def dbversion_params = params.dbmsversion
		def programdir_params = params.programdirectory
		def logdir_params = params.dblogdir
		
		iDBConnectionSource.newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params, logdir_params)
		def db = iDBConnectionSource.getConnection()
		
		if (db != null) {
			try {
				//Get data
				if (strQueryVariable == null) {
					resultData = db.rows(queryString)
				} else {
					resultData = db.rows(queryString, strQueryVariable)
				}
			} catch (SQLException e) {
				logger.info (MessageFormat.format(mapMessage['ERR002'], "'" + queryString + "'", e.getErrorCode(), e.getMessage()))
				isJobFinishedSuccessfully = false
				return null
			}
		
		} else {
			logger.info('Cannot connect to database')
			isJobFinishedSuccessfully = false
		}
		
		return resultData
	}
	
	def runDbExec(queryString, strQueryVariable, dbInfo){
		def resultData = [:]
		def binding = instanceJob.getBinding()
				
		def params = binding.getVariable("parameters")
		def datadir_params = params.datadirectory
		def dbversion_params = params.dbmsversion
		def programdir_params = params.programdirectory
		def logdir_params = params.dblogdir
		
		iDBConnectionSource.newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params, logdir_params)
		def db = iDBConnectionSource.getConnection()
		
		if (db != null) {
			try {
				//Get data
				if (strQueryVariable == null) {
					db.execute(queryString)
				} else {
					db.execute(queryString, strQueryVariable)
				}
				resultData["Result"] = "SUCCESS"
			} catch (SQLException e) {
				logger.info (MessageFormat.format(mapMessage['ERR002'], "'" + queryString + "'", e.getErrorCode(), e.getMessage()))
				isJobFinishedSuccessfully = false
				resultData["Result"] = "FAIL"
			}
		
		} else {
			logger.info('after of 20 times of connections, connect to database falsed.')
			isJobFinishedSuccessfully = false
			resultData["Result"] = "FAIL"
		}
		
		return resultData
	}
	/**
	 * Prepare data to run job
	 * Error messages, math functions, interval, etc..
	 * @param binding Binding variable to be set
	 * @param now Need to prepare interval
	 */
	protected void prepareData(binding, now){
		// Prepare error messages
		messageFile.each {
			def toArray = it.split(" = ")
			mapMessage[toArray[0]] = toArray[1]
		}
		
		// Prepare math functions
		MathFuncUltilities.doBindCdiv(binding)
		
		// Prepare recovery functions
		JobRecoveryFunctions.doBind(binding)
		
		// Prepare iDBConnectionSource for test connection job(Localconnect, ListenerConnect)
		binding.setVariable('iDBConnectionSource',iDBConnectionSource)
		
		// Prepare date format
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
		binding.setVariable('dateFormat',dateFormat)
		
		// Calculate and prepare interval
		def interval
		if(binding.getVariable("lastexecution") != null) {
			interval = now - binding.getVariable("lastexecution")
		} else {
			interval = null
		}
		binding.setVariable("interval", interval)
	}
	
	/**
	 * Get variable from binding
	 * @param binding Binding to get variables from
	 * @param varKey Key to get variable from binding
	 * @return var Variable
	 */
	def getVarFromBinding(binding, varKey){
		def var = null
		if (binding.hasVariable(varKey)) {
			var = binding.getVariable(varKey)
		}
		return var
	}

	//
	// start method is called by GroovyScheduledJob.execute (<- org.wiperdog.jobmanager) 
	// 
	def start(sccontext, senderList) {
		try{
			def now = (new Date()).getTime().intdiv(1000)
			def binding = instanceJob.getBinding()
			prepareData(binding, now)
			
			def strCommand = null
			def mapFormat = null
			def mapDest = null
			def cFetchAction = null
			def strQuery = null
			def strDbExec = null			
			def strFinally = null
			def strQueryVariable = null
			def strDbExecVariable = null			
			def strCon = null
			def strDbType = null
			def strHostId = null
			def strSid = null
			def strDbTypeDriver = null
			def strUser = null
			def strPwd = null
			def strAccumulate = null
			def strOSInfo = null
			def strMorType = null
			// groupKeys keys for mapping records
			def groupKeys = null
			def resultData = null
			
			//Get COMMAND
			strCommand = getVarFromBinding(binding, ResourceConstants.DEF_COMMAND)
			//Get FORMAT
			mapFormat = getVarFromBinding(binding, ResourceConstants.DEF_FORMAT)
			//Get DEST
			mapDest = getVarFromBinding(binding, ResourceConstants.DEF_DEST)
			//Get FETCHACTION
			cFetchAction = getVarFromBinding(binding, ResourceConstants.DEF_FETCHACTION)
			//Get QUERY
			strQuery = getVarFromBinding(binding, ResourceConstants.DEF_QUERY)
			//Get DBEXEC
			strDbExec = getVarFromBinding(binding, ResourceConstants.DEF_DBEXEC)			
			//Get FINALLY
			strFinally = getVarFromBinding(binding, ResourceConstants.DEF_FINALLY)
			//Get QUERY_VARIABLE
			strQueryVariable = getVarFromBinding(binding, ResourceConstants.DEF_QUERY_VARIABLE)
			//Get DBEXEC_VARIABLE
			strDbExecVariable = getVarFromBinding(binding, ResourceConstants.DEF_DBEXEC_VARIABLE)			
			//Get type of DB (ORACLE, MYSQL...)
			strDbType = getVarFromBinding(binding, ResourceConstants.DEF_DBTYPE)
			// Get params of job
			def paramsJob = binding.getVariable('parameters')
			
			// Get DBHOSTID
			strHostId = getVarFromBinding(binding, ResourceConstants.DBHOSTID)
			//Get Monitoring type of job
			strMorType = getVarFromBinding(binding, ResourceConstants.MONITORINGTYPE)
			def osInfo = null
			if(strMorType.equals(ResourceConstants.MONITORINGTYPE_OS)){
				//Get OSINFO
				osInfo = getVarFromBinding(binding, ResourceConstants.OSINFO)
				//Set process runner for os monitoring job
				def procRunner = new ProcessRunner(osInfo)
				binding.setVariable("procRunner", procRunner)
			}
			if(strHostId == null) {
				strHostId = paramsJob.dbHostId
			}
			// Get DBSID
			strSid = getVarFromBinding(binding, ResourceConstants.DBSID)
			if(strSid == null) {
				strSid = paramsJob.dbSid
			}
			//Get driver based on type of DB
			if(strDbType != null){
				if(strDbType == ResourceConstants.ORACLE) {
					strDbTypeDriver = ResourceConstants.DEF_ORACLE_DRIVER
				}
				if(strDbType == ResourceConstants.MYSQL) {
					strDbTypeDriver = ResourceConstants.DEF_MYSQL_DRIVER
				}
				if(strDbType == ResourceConstants.POSTGRES) {
					strDbTypeDriver = ResourceConstants.DEF_POSTGRES_DRIVER
				}
				if(strDbType == ResourceConstants.SQLS) {
					strDbTypeDriver = ResourceConstants.DEF_SQLS_DRIVER
				}
			}
			// Get DBInfo
			dbInfo = getDbInfo(paramsJob, strDbType, strHostId, strSid)
			if (dbInfo != null) {
				dbInfo['strDbType'] = strDbType
				dbInfo['strDbTypeDriver'] = strDbTypeDriver
				//Get DBCONNSTR
				strCon = dbInfo.dbconnstr
				//Get DB user
				strUser = dbInfo.user
				//If DB user haven't set, get default user
				if (strUser == null && strDbType != null) {
					strUser = iDBConnectionSource.getDefaultUser(dbInfo)
					dbInfo.user = strUser
				}
			}
			if (cFetchAction == null && strCommand != null) {
				// start command
				logger.debug("fileName: " + fileName + " ---Start Process Command---")
				resultData = runCommand(osInfo,strCommand, mapFormat)
				logger.debug("fileName: " + fileName + " ---Finish Process Command---")
			}
			if (cFetchAction != null) {
				logger.debug("fileName: " + fileName + " ---Start Process FetchAction---")
				resultData = runFetchAction (cFetchAction, dbInfo)
				logger.debug("fileName: " + fileName + " ---End Process FetchAction---")
			}
			if (cFetchAction == null && strQuery != null) {
				if (strCon == null || strDbType == null || strUser == null) {
					logger.info (mapMessage['ERR006'])
				} else {
					logger.debug("fileName: " + fileName + " ---Start Process Query---")
					resultData = runQuery(strQuery, strQueryVariable, dbInfo)
					logger.debug("fileName: " + fileName + " ---End Process Query---")
				}
			}

			if (cFetchAction == null && strDbExec != null) {
				if (strCon == null || strDbType == null || strUser == null) {
					logger.info (mapMessage['ERR006'])
				} else {
					logger.debug("fileName: " + fileName + " ---Start Process DBEXEC---")
					resultData = runDbExec(strDbExec, strDbExecVariable, dbInfo)
					logger.debug("fileName: " + fileName + " ---End Process DBEXEC---")
				}
			}            
            			
			// Get ACCUMULATE
			strAccumulate = getVarFromBinding(binding, ResourceConstants.DEF_ACCUMULATE)
			// Get GROUPKEY
			groupKeys = getVarFromBinding(binding, ResourceConstants.DEF_GROUPKEY)
			// Execute ACCUMULATE
			resultData = runAccumulate(binding, resultData, strAccumulate, groupKeys)
			if(strFinally != null ) {
				strFinally.call(resultData)
			}
			// Set lastexecution
			binding.setVariable("lastexecution", now)
			if (resultData != null) {
				if (senderList.isEmpty()) {
					sender.mergeSender(mapDest, senderList);
				}
				processSendData(senderList, resultData)
			}
			return resultData
		} catch(FileNotFoundException e) {
			logger.error(e.toString())
			isJobFinishedSuccessfully = false
			return null
		} catch (Exception e){
			logger.debug(e.toString())
			if(!(e instanceof JobControlException)){
				isJobFinishedSuccessfully = false
			} else {
				throw e
			}
			return null
		}
	}
	
	/**
	 * Run Accumulate closure
	 * @param binding Binding
	 * @param resultData Result data from job
	 * @param strAccumulate Accumulate closure
	 * @param groupKeys Group keys for mapping records
	 * @return resultData Result after running Accumulate
	 */
	def runAccumulate(binding, resultData, strAccumulate, groupKeys){
		def OUTPUT = [:]
		// Set variable to Accumulate command and execute ACCUMULATE
		if((strAccumulate != null) && (resultData != null)){
			// After run QUERRY/COMMAND/FFETCHACTION, set resultData to OUTPUT
			OUTPUT = resultData
			binding.setVariable('OUTPUT', OUTPUT)
			binding.setVariable('groupKeys', groupKeys)
			MathFuncUltilities.doBindSimpleDiff(binding)

			// Get prevOUTPUT after running FETCHACTION/QUERY/COMMAND
			// Run ACCUMULATE
			// Set new prevOUTPUT into binding after running ACCUMULATE
			def temp = [:]
			def prevOUTPUT = binding.getVariable('prevOUTPUT')
			
			//Catch Exception from ACCUMULATE
			try {
				strAccumulate.call()
				resultData = binding.getVariable('OUTPUT')
			} catch (Exception e) {
			   String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e)
			   logger.info (MessageFormat.format(mapMessage['ERR008'],"ACCUMULATE",stackTrace))
			   resultData = null
			   if(!(e instanceof JobControlException)){
				   isJobFinishedSuccessfully = false
			   } else {
				   throw e
			   }
			} catch (AssertionError ae) {
				String error_assertion = ae.getMessage()
				String line_number_error = ae.getStackTrace()[2].getLineNumber()
				logger.info(MessageFormat.format(mapMessage['ERR009'],this.fileName,line_number_error,error_assertion))
				resultData = null
				isJobFinishedSuccessfully = false
			}
			if(groupKeys == null){
				prevOUTPUT = OUTPUT
			} else {
				OUTPUT.each {rec->
					String prevOutputKey = ''
					for(groupkey in groupKeys){
						prevOutputKey += rec[groupkey]
					}
					temp[prevOutputKey] = rec
				}
				prevOUTPUT = temp
			}
			binding.setVariable('prevOUTPUT', prevOUTPUT)
		}
		return resultData
	}

	/**
	 * Run FETCHACTION closure
	 * @param fetchActionString Fetchaction closure
	 * @param dbInfo connect DB information
	 * @return resultData result after running FETCHACTION
	 */
	def runFetchAction (fetchActionString, dbInfo) {
		def binding = instanceJob.getBinding()
		def resultData = null
		
		def pluginDBSource
		if (dbInfo !=null) {
			// ConnectionInit & EncryptedDBConnectionSource is for normal connection (MySQL, MSSQL, Postgres, ORACLE)
			// Other connection like MongoDB, MariaDB,... it needs a mechanism to load PluginDBConnectionSource
			def dbTypeStr = dbInfo.strDbType
			
			// PluginDBConnectionSource
			// If there is no implementation of specific DBConnectionSource then use
			// the common DBConnectionSource : EncryptedDBConnectionSource
			try{
				def dbsourceClassNm = dbTypeStr.replaceAll('@','').toUpperCase() + "DBConnectionSource"
				pluginDBSource = this.class.getClassLoader().loadClass(dbsourceClassNm).newInstance()
			}catch(ClassNotFoundException clnfEx){
				pluginDBSource = iDBConnectionSource //new EncryptedDBConnectionSourceImpl() 
			}
			
			def params = binding.getVariable("parameters")
			def datadir_params = params.datadirectory
			def dbversion_params = params.dbmsversion
			def programdir_params = params.programdirectory
			def logdir_params = params.dblogdir
			pluginDBSource.newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params,logdir_params)
			pluginDBSource.getBinding().each{key, value->
				binding.setVariable(key, value)
			}
		}
		
		//Run FetchAction
		try {
			resultData = fetchActionString.call()
			if(pluginDBSource != null){
				pluginDBSource.closeConnection()
			}
		} catch (SQLException e) {
			logger.info (MessageFormat.format(mapMessage['ERR003'], e.getErrorCode(), e.getMessage()))
			isJobFinishedSuccessfully = false
		} catch (Exception e) {
			String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e)
			logger.info (MessageFormat.format(mapMessage['ERR008'],"FETCHACTION",stackTrace))
			if(!(e instanceof JobControlException)){
				isJobFinishedSuccessfully = false
			}else{
				throw e
			}
		} catch (AssertionError ae) {
			String error_assertion = ae.getMessage()
			String line_number_error = ae.getStackTrace()[2].getLineNumber()
			logger.info(MessageFormat.format(mapMessage['ERR009'],this.fileName,line_number_error,error_assertion))	
			isJobFinishedSuccessfully = false
		}
		return resultData
	}
	
	/**
	 * Process to send data
	 * @param destination : Dest given by job
	 * @param resultData : Data for sending
	 * @return
	 */
	def processSendData(destination, resultData){
		def dataAttachRecordSeq = attachRecordSeq(resultData)
		def envelopedResultData = envelopeData(dataAttachRecordSeq)
		// Judge data by policyFile and save judgement(messages) into mongoDB
		juggernaut.judgeData(envelopedResultData)

		if (destination != null) {
			for (concreteSender in destination) {
				concreteSender.send(envelopedResultData)
			}
		}
	}

	/**
	 * Envelope result with extra informations
	 * @param destination Destination of data
	 * @param resultData Result data from jobs
	 * @return envelopedResultData Data which is enveloped
	 */
	def envelopeData(resultData){
		def envelopedResultData = [:]
		def binding = instanceJob.getBinding()
		def nowDate = new Date()
		def fetchAtTime
		def fetchAtTime_bin
		def istIid
		def strSourceJob
		def strInstance
		def strResourceId
		def strDBType
		def keyExpr
		
		//Get fetchAtTime & fetchedAt_bin
		fetchAtTime = nowDate.format('yyyyMMddHHmmssz')
		fetchAtTime_bin = (nowDate.getTime()/1000).intValue()
		//Get sourceJob
		strSourceJob = this.rootJobName
		//Get instance name
		strInstance = this.instanceName
		//Get hostId
		envelopedResultData['hostId'] = getHostId()
		//Get type
		envelopedResultData['type'] = getType(binding,resultData)
		//Get sid
		envelopedResultData['sid'] = getSid()
		//Get dbType
		strDBType = (binding.hasVariable(ResourceConstants.DEF_DBTYPE) ? binding.getVariable(ResourceConstants.DEF_DBTYPE) : null)
		//Get istIid
		istIid = envelopedResultData['hostId']
		if(strDBType != null){
			istIid += "-" + strDBType
		}
		if(envelopedResultData['sid'] != null){
			istIid += "-" + envelopedResultData['sid']
		}
		//Get resourceId
		strResourceId = (binding.hasVariable(ResourceConstants.RESOURCEID) ? binding.getVariable(ResourceConstants.RESOURCEID) : "")
		//Get KEYEXPR
		keyExpr = (binding.hasVariable(ResourceConstants.KEYEXPR) ? binding.getVariable(ResourceConstants.KEYEXPR) : null)
		if(keyExpr != null){
			envelopedResultData[ResourceConstants.KEYEXPR] = keyExpr
		}
			
		envelopedResultData['version'] = '1.0'
		envelopedResultData['fetchAt'] = fetchAtTime
		envelopedResultData['fetchedAt_bin'] = fetchAtTime_bin
		envelopedResultData['istIid'] = istIid
		envelopedResultData['resourceId'] = strResourceId
		envelopedResultData['sourceJob'] = strSourceJob
		envelopedResultData['instanceName'] = strInstance
		envelopedResultData['data'] = resultData
        
		return envelopedResultData
	}
	
	/**
	 * Process to get sid
	 * @return sid
	 */
	def getSid(){
		def sid
		if ((dbInfo != null) && (dbInfo.params != null)) {
			sid = dbInfo.params.DBSID
		}
		return sid
	}
	
	/**
	 * Process to get HostId
	 * @return hostId
	 */
	def getHostId(){
		if ((dbInfo != null) && (dbInfo.params != null)) {
			return dbInfo.params.DBHOSTID
		} else {
			def hostId
			def sysPropFile = new File(properties.get(ResourceConstants.SYSTEM_PROPERTIES_FILE_DIRECTORY) + "/system.properties")
			if(sysPropFile != null){
				def tempArray = []
				def tempMap = [:]
				try {
					sysPropFile.eachLine {
						tempArray = it.split("=")
						tempMap[tempArray[0]] = ((tempArray.size() >= 2) ? tempArray[1] : "")
					}
				}catch (Exception ex){
					logger.debug(ex.toString())
					isJobFinishedSuccessfully = false
				}
				hostId = tempMap['pi.local.hostid']
			}
			return hostId
		}
	}
	
	/**
	 * Process to get type of job data
	 * @param binding Binding get from instanceJob
	 * @param resultData Data from job
	 * @return strType Type of job data
	 */
	def getType(binding,resultData){
		def strType
		strType = (binding.hasVariable(ResourceConstants.SENDTYPE) ? binding.getVariable(ResourceConstants.SENDTYPE) : null)
		if(strType == null) {
			if(resultData instanceof List){
				boolean canBeStore = true
				resultData.each{resultDat->
					canBeStore = canBeStore && (resultDat instanceof Map)
				}
				if(canBeStore){
					strType = 'Store'
				}else{
					strType = ''
				}
			}else if(resultData instanceof Map){
				boolean canBeSubtyped = true
				resultData.each{resultDat->
					canBeSubtyped = canBeSubtyped && (resultDat.value instanceof List)
				}
				if(canBeSubtyped){
					strType = 'Subtyped'
				}else{
					strType = 'Gathered'
				}
			}
		}
		return strType
	}
	
	/**
	 * Process to attach RECORD_SEQ data
	 * @param data Data before attach
	 * @return Data after attach
	 */
	def attachRecordSeq(data) {
		def recordSeq
		def binding = instanceJob.getBinding()
		try {
			def type = getType(binding,data)
			if(type == "Store") {
				data.each {eStore ->
					recordSeq = DefaultJobCaller.getNextRecordSeq()
					eStore["RECORD_SEQ"] = recordSeq
				}
			}
			if(type == "Subtyped") {
				data.each { eSubtyped ->
					eSubtyped.value.each {
						recordSeq = DefaultJobCaller.getNextRecordSeq()
						it["RECORD_SEQ"] = recordSeq
					}
				}
			}
			if(type == "Gathered") {
				if(data instanceof Map){
					data.each { eGathered ->
						eGathered.value.each { element ->
							if(element.key == "DetailExecution" || element.key == "DetailExecutions") {
								element.value.each {
									recordSeq = DefaultJobCaller.getNextRecordSeq()
									it["RECORD_SEQ"] = recordSeq
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.debug("We don't need to fix this data")
			isJobFinishedSuccessfully = false
		}
		return data
	}
	
	/**
	 * Process to get next RECORD_SEQ
	 * @return Data used for attach
	 */
	@Synchronized
	public static int getNextRecordSeq() {
		if (RECORD_SEQ == Integer.MAX_VALUE) {
			RECORD_SEQ =  1
		} else {
			RECORD_SEQ += 1
		}
		return RECORD_SEQ
	}
	
	/**
	 * getDbInfo: get connect DB information
	 * @param paramsJob
	 * @param dbTypeString type of DB was configed in Job
	 * @param hostIdString hostId was configed in Job
	 * @param sidString sid was configed in Job
	 * @return connect DB information
	 */
	def getDbInfo(paramsJob, dbTypeString, hostIdString, sidString) {
		def dbInformation
		def params = [:]
		// Key which management connect information of DB
		def keyManager
		if(hostIdString != null) {
			keyManager = hostIdString + "-" + dbTypeString
		} else {
			keyManager = dbTypeString
		}
		if(sidString != null) {
			keyManager += "-" + sidString
		}
		dbInformation = paramsJob.dbinfo[keyManager]
		if (dbInformation != null) {
			params['DBHOSTID'] = dbInformation.dbHostId
			params['DBSID'] = dbInformation.dbSid
			dbInformation.params = params
			dbInformation.dbHostId = hostIdString
			dbInformation.dbSid = sidString
		}
		return dbInformation
	}
}

//Run command by java process
class ProcessRunner{
	//build list command for connect to remote host
	List<String> listCmd = null
	def osInfo = null
	ProcessRunner(){
	}
	ProcessRunner(osInfo){
		this.osInfo = osInfo
	}

	// build list command to remote host 
	private buildListCmdRemote(mapCommand){
		if(this.osInfo.os == "win" ){
			//Commands  for remote Windows host
			if(this.osInfo.host != "" && this.osInfo.host != "localhost" ) {
				// if command format is Map : [type:'wmic or remote' ,commandStr:"command"]
				if(mapCommand instanceof Map){
					if(mapCommand.type == "wmic") {
						listCmd.add("wmic")
						listCmd.add("/NODE:" + this.osInfo.host)
						listCmd.add("/user:" + this.osInfo.user)
						listCmd.add("/password:" + this.osInfo.pass)
					} else {
						this.netUse()
						this.listCmd = new ArrayList<String>()		
						//PSExec place in %Wiperdog_Home%/bin
						this.listCmd.add("PsExec.exe")
						this.listCmd.add("\\\\" +this.osInfo.host )
						this.listCmd.add("-u")
						this.listCmd.add(this.osInfo.user)
						this.listCmd.add("-p")
						this.listCmd.add(this.osInfo.pass)
					}
				}
				//If command format is String ,set default remote using PsExec
				if(mapCommand instanceof String){
					this.netUse()
					this.listCmd = new ArrayList<String>()		
					//PSExec place in %Wiperdog_Home%/bin
					this.listCmd.add("PsExec.exe")
					this.listCmd.add("\\\\" +this.osInfo.host )
					this.listCmd.add("-u")
					this.listCmd.add(this.osInfo.user)
					this.listCmd.add("-p")
					this.listCmd.add(this.osInfo.pass)
				}
				
			} else {
				if(mapCommand instanceof Map ) {
					if(mapCommand.type == "wmic") {
						listCmd.add("wmic")
					}
				}
			}
		} else {
			//Commands for remote Linux host
			if(this.osInfo.host != "" && this.osInfo.host != "localhost" ) {
				listCmd.add("/usr/bin/ssh")
				listCmd.add(this.osInfo.host)
			}
		}
		return this.listCmd
	}
	//Access to shared resources of remote host
	private netUse(){
		this.listCmd = new ArrayList<String>()
		this.listCmd.add("net")
		this.listCmd.add("use")
		this.listCmd.add("\\\\"+this.osInfo.host+"\\ipc\$")
		this.listCmd.add("/user:"+this.osInfo.user)
		this.listCmd.add(this.osInfo.pass)
		def process = listCmd.execute()
		this.listCmd = null

	}
	//Run process closure
	def procExecute(mapCommand,boolean isWaitFor){
		def resultData = [:]
		def tmpList = null
		try {
			this.listCmd = new ArrayList<String>()		
			if(this.osInfo != null) {
				if((this.osInfo.host != null) && (this.osInfo.host != "") || (this.osInfo.host != "localhost")){
					this.listCmd = buildListCmdRemote(mapCommand)
				}
				if(mapCommand instanceof Map){					
					tmpList = (mapCommand.commandStr.trim().split(" ") as List)
				}
				if(mapCommand instanceof String){					
					tmpList = (mapCommand.split(" ") as List)
				}
				
			} else {
				if(mapCommand instanceof Map){
					if(mapCommand.type =="wmic"){
						listCmd.add("wmic")
					}
					tmpList = (mapCommand.commandStr.trim().split(" ") as List)
				}
				if(mapCommand instanceof String){
					tmpList = (mapCommand.split(" ") as List)
				}
			}			
			listCmd.addAll(tmpList)
			ProcessBuilder builder = new ProcessBuilder(listCmd)
			Process proc = builder.start()
			resultData['exitVal'] = proc.waitFor()
			resultData['err'] = proc.err.text
			resultData['out'] = proc.in.text
		} catch (Exception ex){
			resultData['err'] = ex.getMessage()
			println ex.getMessage()
		}
		return resultData
	}
}
