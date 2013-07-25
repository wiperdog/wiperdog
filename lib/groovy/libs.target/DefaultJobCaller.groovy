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
   	String jobName
	
	def properties = MonitorJobConfigLoader.getProperties()
	
	static mapDBConnections = []
	
	def logger = Logger.getLogger("com.insight_tec.pi.scriptsupport.groovyrunner")
	
	//def messageFile = new File(System.getProperty("felix.home") + "/var/conf/message.properties")
	def messageFile = new File(properties.get(ResourceConstants.MESSAGE_FILE_DIRECTORY) + "/message.properties")
	
	def mapMessage = [:]
		
	def iDBConnectionSource = new EncryptedDBConnectionSourceImpl()
	
	static int RECORD_SEQ = 0
	
	DefaultSender sender
	
	// Mark job is executed successfully or not
	def isJobFinishedSuccessfully 
	
	//Pass job filename to constructor
	def DefaultJobCaller(objJob,fileName, jobName, DefaultSender sender) {
		
		this.instanceJob = objJob
		this.fileName = fileName
		this.jobName = jobName
		this.isJobFinishedSuccessfully = true
		this.sender = sender
	}

	def runCommand(commandline, mapFormat) {
		def recordarray = []
		try{
			def proc
			try {
				proc = commandline.execute()
			} catch (Exception ex) {
				logger.info (MessageFormat.format(mapMessage['ERR005'],"'" + commandline + "'"))
				isJobFinishedSuccessfully = false
				return null
			}
			if (mapFormat != null) {
				def rxMatch = mapFormat['match']
				def rxSplit = mapFormat['split']
				proc.getInputStream().eachLine { it ->
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
	 * @param connectionString Connection
	 * @param driverString Driver string
	 * @param userString Username
	 * @param queryString Query string
	 * @param strQueryVariable Query variables
	 * @param strDbType Database's type
	 * @return resultData data after executing query
	 */
	def runQuery(connectionString, driverString, userString, queryString, strQueryVariable, strDbType) {	
		List resultData = null		
		def binding = instanceJob.getBinding()
				
		def conInt = new ConnectionInit()
		def db = conInt.getDbConnection(mapDBConnections, iDBConnectionSource, binding, strDbType, connectionString, userString)
		
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
			logger.info('after of 20 times of connections, connect to database falsed.')
			isJobFinishedSuccessfully = false
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
	// start method is called by GroovyScheduledJob.execute (<- com.insight_tec.pi.jobmanager) 
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
			def strFinally = null
			def strQueryVariable = null
			def strCon = null
			def strDbType = null
			def strDbTypeDriver = null
			def strUser = null
			def strPwd = null
			def strAccumulate = null
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
			//Get FINALLY
			strFinally = getVarFromBinding(binding, ResourceConstants.DEF_FINALLY)
			//Get QUERY_VARIABLE
			strQueryVariable = getVarFromBinding(binding, ResourceConstants.DEF_QUERY_VARIABLE)
			//Get DBCONNSTR
			strCon = getVarFromBinding(binding, ResourceConstants.DEF_DBCONNSTR)
			//Get type of DB (ORACLE, MYSQL...)
			strDbType = getVarFromBinding(binding, ResourceConstants.DEF_DBTYPE)
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
			//Get DB user
			strUser = getVarFromBinding(binding, ResourceConstants.DEF_DBUSER)
			//If DB user haven't set, get default user
			if (strUser == null && strDbType != null) {
				strUser = iDBConnectionSource.getDefaultUser(strDbType)
			}
			if (cFetchAction == null && strCommand != null) {
				// start command
				logger.debug("fileName: " + fileName + " ---Start Process Command---")
				resultData = runCommand(strCommand, mapFormat)
				logger.debug("fileName: " + fileName + " ---Finish Process Command---")
			}
			if (cFetchAction != null) {
				logger.debug("fileName: " + fileName + " ---Start Process FetchAction---")
				resultData = runFetchAction (cFetchAction, strCon, strUser, strDbType, strDbTypeDriver)
				logger.debug("fileName: " + fileName + " ---End Process FetchAction---")
			}
			if (cFetchAction == null && strQuery != null) {
				if (strCon == null || strDbType == null || strUser == null) {
					logger.info (mapMessage['ERR006'])
				} else {
					logger.debug("fileName: " + fileName + " ---Start Process Query---")
					resultData = runQuery(strCon, strDbTypeDriver, strUser, strQuery, strQueryVariable, strDbType)
					logger.debug("fileName: " + fileName + " ---End Process Query---")
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
	 * @param connectionString Connection
	 * @param userString Username
	 * @param dbTypeString Database type
	 * @param driverString Driver String
	 * @return resultData result after running FETCHACTION
	 */
	def runFetchAction (fetchActionString, connectionString, userString, dbTypeString, driverString) {
		def binding = instanceJob.getBinding()
		def resultData = null
		
		//If has data for connect, create connection
		if(userString == null) {
			userString = iDBConnectionSource.getDefaultUser(dbTypeString)
		}
		if((connectionString != null) && (dbTypeString != null) && (userString != null)) {
			def conInt = new ConnectionInit()				            
			def db = conInt.getDbConnection(mapDBConnections, iDBConnectionSource, binding, dbTypeString, connectionString, userString)
			if (db == null) {
				logger.info(this.fileName + ': After of 20 times of connections, connect to database falsed.')
			}
			binding.setVariable('sql', db)
		}
		
		//Run FetchAction
		try {
			resultData = fetchActionString.call()
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
		def strResourceId
		def keyExpr
		
		//Get fetchAtTime & fetchedAt_bin
		fetchAtTime = nowDate.format('yyyyMMddHHmmssz')
		fetchAtTime_bin = (nowDate.getTime()/1000).intValue()
		//Get sourceJob
		strSourceJob = this.jobName
		//Get hostId
		envelopedResultData['hostId'] = getHostId()
		//Get type
		envelopedResultData['type'] = getType(binding,resultData)
		//Get sid
		envelopedResultData['sid'] = getSid(binding)
		//Get istIid
		if(envelopedResultData['sid'] != null){
			istIid = envelopedResultData['hostId'] + "-" + envelopedResultData['sid']
		}else{
			istIid = envelopedResultData['hostId']
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
		envelopedResultData['data'] = resultData
        
		return envelopedResultData
	}
	
	/**
	 * Process to get sid
	 * @param binding Binding get from instanceJob
	 * @return sid
	 */
	def getSid(binding){
		def instanceName
		def sid
		sid = binding.hasVariable(ResourceConstants.MONITORINGTYPE) ? binding.getVariable(ResourceConstants.MONITORINGTYPE): "@SYS"
		if ('@DB'.equals(sid)) {
			try{
				def strDbType = binding.hasVariable(ResourceConstants.DBTYPE) ? binding.getVariable(ResourceConstants.DBTYPE) : null
				def formatedSid
				switch (strDbType) {
					case ResourceConstants.ORACLE:
						formatedSid = '@ORA'
						break;
					case ResourceConstants.MYSQL:
						formatedSid = '@MYSQL'
						break;
					case ResourceConstants.POSTGRES:
						formatedSid = '@PGSQL'
						break;
					case ResourceConstants.SQLS:
						formatedSid = '@MSSQL'
						break;
					default:
						break;
				}
				instanceName = binding.hasVariable(ResourceConstants.DBINSTANCE) ? binding.getVariable(ResourceConstants.DBINSTANCE) : null
				def trimmedInstanceName = ""
				if(instanceName != null){
					trimmedInstanceName = instanceName.trim()
				}
				if(!trimmedInstanceName.isEmpty()){
					sid = formatedSid + '-' + trimmedInstanceName
				}else{
					sid = formatedSid
				}
			}catch(Exception ex){
				logger.debug(ex.toString())
				isJobFinishedSuccessfully = false
			}
		}else if( (!'@SYS'.equals(sid)) && (!'@NET'.equals(sid)) && (!'@DB'.equals(sid)) ){
			sid = null
		}
		return sid
	}
	
	/**
	 * Process to get HostId
	 * @return hostId
	 */
	def getHostId(){
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
}