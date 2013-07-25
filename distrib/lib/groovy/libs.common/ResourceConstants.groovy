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
/**
 * Contain all CONSTANTS of PIEX system
 */
class ResourceConstants{
	public static final String CHARTDEF = "CHARTDEF"
	public static final String REPOSITORYDEF = "REPOSITORYDEF"
	/** JobDsl */
	public static final String DEF_TRIGGER_JOB = "job"
	public static final String DEF_TRIGGER_SCHEDULE = "schedule"
	public static final String DEF_CLS_NAME = "name"
	public static final String DEF_CLS_CONCURRENCY = "concurrency"
	public static final String DEF_CLS_MAXRUN = "maxrun"
	public static final String DEF_CLS_MAXWAIT = "maxwait"
	
	/** GroovyScheduledJob*/
	public static final String DEF_JOB = "JOB"
	public static final String DEF_JOB_NAME = "name"
	public static final String DEF_JOB_CLASS = "jobclass"
	public static final String DEF_JOB_MAXRUN = "maxrun"
	public static final String DEF_JOB_MAXWAIT = "maswait"
	public static final String JOB_DIRECTORY= "monitorjobfw.directory.job"
	public static final String JOB_PARAMETERS_DIRECTORY= "monitorjobfw.directory.jobparameters"
	public static final String DEFAULT_PARAMETERS_DIRECTORY= "monitorjobfw.directory.defaultparameters"
	public static final String MONITORJOBDATA_DIRECTORY= "monitorjobfw.directory.monitorjobdata"
	
	/** EncryptedDBConnectionSourceImpl **/
	public static final String DEF_ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver"
	public static final String DEF_MYSQL_DRIVER = "com.mysql.jdbc.Driver";
	public static final String DEF_POSTGRES_DRIVER = "org.postgresql.Driver";
	public static final String DEF_SQLS_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	public static final String MESSAGE_FILE_DIRECTORY= "monitorjobfw.diretory.messagefile"
	public static final String DBPASSWORD_FILE_DIRECTORY= "monitorjobfw.directory.dbpasswordfile"
		
	/** MonitorJobConfigLoader */
	public static final String SERVICE_PID = "monitorjobfw"
	public static final String SYSTEM_PROPERTIES_FILE_DIRECTORY= "monitorjobfw.directory.systempropertiesfile"
	public static final String GROOVY_FILE_DIRECTORY= "monitorjobfw.directory.groovyfile"
	
	/** JobLoader */
	public static final String DEF_POLICY = "POLICY"
	public static final String DEF_ACCEPTS = "ACCEPTS"
		
	/** DefaultJobCaller */
	public static final String DEF_COMMAND = "COMMAND"
	public static final String DEF_FORMAT = "FORMAT"
	public static final String DEF_DEST = "DEST"
	public static final String DEF_FETCHACTION = "FETCHACTION"
	public static final String DEF_QUERY = "QUERY"
	public static final String DEF_QUERY_VARIABLE = "QUERY_VARIABLE"
	public static final String DEF_FINALLY = "FINALLY"
	public static final String DEF_DBCONNSTR = "DBCONNSTR"
	public static final String DEF_DBTYPE = "DBTYPE"
	public static final String DEF_DBUSER = "DBUSER"
	public static final String ORACLE = "ORACLE"
	public static final String MYSQL= "MYSQL"
	public static final String POSTGRES = "POSTGRES"
	public static final String SQLS = "SQLS"	
	public static final String DEFAULTUSER = "#DEFAULTUSER"	
	public static final String DEF_ACCUMULATE = "ACCUMULATE"	
	public static final String DEF_GROUPKEY = "GROUPKEY"	
	public static final String MONITORINGTYPE="MONITORINGTYPE"
	public static final String KEYEXPR = "KEYEXPR"
	public static final String SENDTYPE = "SENDTYPE"
	public static final String RESOURCEID = "RESOURCEID"
	public static final String DBINSTANCE = "DBINSTANCE"
	public static final String DBTYPE = "DBTYPE"
	
	/** DefaultSender */
	public static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss+SSSZ"
	
	/** HTTPSender */
	public static final String MONITORSWAP_DIRECTORY = "monitorjobfw.directory.monitorswap"
	public static final String MAX_NUMBER_SWAP_FILE = "monitorjobfw.max.number.swapfile"
	public static final String MAX_SWAP_DIRECTORY_SIZE = "monitorjobfw.directory.swap.maxsize.mb"
	public static final String SLEEP_TIME_MS = "monitorjobfw.sleep.time.millisecond"
}