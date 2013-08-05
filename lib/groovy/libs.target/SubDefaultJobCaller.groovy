import org.apache.log4j.Logger;

class SubDefaultJobCaller extends DefaultJobCaller{
	
	SubDefaultJobCaller(objJob,fileName, jobName){
		super(objJob,fileName, jobName, null)
	}
	
	public Object start(Object sccontext) {
		try {
			def resultData = null
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
			
			// TODO: process FINALLY
			if(strFinally != null ) {
				strFinally.call(resultData)
			}
			
			// Set lastexecution
			binding.setVariable("lastexecution", now)
			return resultData
		}catch (Exception ex) {
			logger.debug(ex.toString())
			if(!(ex instanceof JobControlException)){
				isJobFinishedSuccessfully = false
			}else{
				throw ex
			}
		}

	}

}
