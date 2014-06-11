import java.io.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.io.File
import groovy.json.*

public class ProcessGenDefaultParam {
	def static reader = new BufferedReader(new InputStreamReader(System.in));
	/**
	 * main: main process
	 * @param args: data receive from batch/bash file
	*/
	public static void main(String[] args) throws Exception {
		// Get map default params
		def shell = new GroovyShell()
		def filePath = System.getProperty("user.dir")
		def defaultParamsFile = new File(filePath.replace("bin", "var/conf/default.params"))
		def mapDefaultParams = shell.evaluate(defaultParamsFile.text)
		def argsSize = args.size()
		// Get key root: dbinfo, dest, datadirectory, programdirectory, dbmsversion or dblogdir
		def keyRoot = args[0]
		if (keyRoot == "dbinfo") { // Update dbinfo
			println "This command will set up the database's information to connect to DBMS."
			println "!!Be carefull, incorrect setting will make many jobs inworkable!!"
			println "CTRL-C will quit without saving ..."
			def map_db_info = [:]
			def map_db_info_inner = [:]
			def dbtype = ""
			def hostId = ""
			def hostName = ""
			def port = ""
			def userName = ""
			def sid = ""
			def hidik = ""
			def sidik = ""
			print "Enter DB Type (@MYSQL|@PGSQL|@MSSQL|@ORACLE)(*): "
			dbtype = reader.readLine()
			while (dbtype != "@MYSQL" && dbtype != "@PGSQL" && dbtype != "@MSSQL" && dbtype != "@ORACLE") {
				print "DB Type is incorrect. Please re-enter (@MYSQL|@PGSQL|@MSSQL|@ORACLE)(*): "
				dbtype = reader.readLine()
			}
			print "Enter Host ID (*): "
			hostId = reader.readLine()
			while (hostId == "" || hostId.contains(".")) {
				if(hostId == "" ) {
					print "Host ID cannot be empty. Please re-enter (*): "
				} else {
					if(hostId.contains(".")) {
						print "Host ID cannot contains '.' character. Please re-enter (*): "
					}
				}
				hostId = reader.readLine()
				
			}
			print "Enter Host Name (*): "
			hostName = reader.readLine()
			while (hostName == "") {
				print "Host name cannot be empty. Please re-enter (*): "
				hostName = reader.readLine()
			}
			print "Enter Port (Port must be number)(*): "
			port = reader.readLine()
			while (!port.isNumber()) {
				print "Port must be number. Please re-enter (*): "
				port = reader.readLine()
			}
			print "Set host ID as a DBinfo element (y|Y|n|N): "
			hidik = reader.readLine()
			while (hidik != "y" && hidik != "Y" && hidik != "n" && hidik != "N") {
				print "Set host ID as a DBinfo element (y|Y|n|N): "
				hidik = reader.readLine()
			}
			print "Enter User Name (*): "
			userName = reader.readLine()
			while (userName == "") {
				print "User name cannot be empty. Please re-enter (*): "
				userName = reader.readLine()
			}
			print "Enter Sid: "
			sid = reader.readLine()
			print "Set Sid as a DBinfo element (y|Y|n|N): "
			sidik = reader.readLine()
			while (sidik != "y" && sidik != "Y" && sidik != "n" && sidik != "N") {
				print "Set Sid as a DBinfo element (y|Y|n|N): "
				sidik = reader.readLine()
			}

			// Create key for db information
			def key = dbtype
			if (hidik == "y" || hidik == "Y") {
				key = hostId + "-" + key
			}
			if (sidik == "y" || sidik == "Y") {
				key = key + "-" + sid
			}
			// DB information
			def db_conn_str = ""
			if (dbtype == "@MYSQL") {
				db_conn_str = "jdbc:mysql://" + hostName + ":" + port
			}
			if (dbtype== "@PGSQL") {
				db_conn_str = "jdbc:postgresql://" + hostName + ":" + port
			}
			if (dbtype== "@MSSQL") {
				db_conn_str = "jdbc:sqlserver://" + hostName + ":" + port
			}
			map_db_info_inner["dbconnstr"] = db_conn_str
			map_db_info_inner["user"] = userName
			map_db_info_inner["dbHostId"] = hostId
			map_db_info_inner["dbSid"] = sid
			mapDefaultParams['dbinfo'][key] = map_db_info_inner
		} else if (keyRoot == "dest") { // Update destination
			println "This command will set up the dest's information to send data monitoring."
			println "!!Be carefull, incorrect setting will make many jobs inworkable!!"
			println "CTRL-C will quit without saving ..."
			def action = ""
			def keyDest = ""
			def keyVal = ""
			println "*** Current destination's info: "
			println mapDefaultParams['dest']
			print "*** Do you want (add|delete) dest? "
			action = reader.readLine()
			while (action != "add" && action != "delete") {
				print "Please choose an action (add|delete): "
				action = reader.readLine()
			}
			if (action == "add") {
				def newDest = [:]
				print "Enter key of dest (file|http|mongoDB)(*): "
				keyDest = reader.readLine()
				while(keyDest != "file" && keyDest != "http" && keyDest != "mongoDB") {
					print "Key of dest is incorrect. Please re-enter (file|http|mongoDB)(*): "
					keyDest = reader.readLine()
				}
				print "Enter correnponding value(*): "
				keyVal = reader.readLine()
				while(keyVal == "") {
					print "Value of key cannot be empty. Please re-enter (*): "
					keyVal = reader.readLine()
				}
				newDest[keyDest] = keyVal
				def checkExistDest = false
				mapDefaultParams['dest'].each {eDest ->
					if (eDest[newDest.keySet()[0]] != null) {
						eDest[newDest.keySet()[0]] = newDest[newDest.keySet()[0]]
						checkExistDest = true
					}
				}
				if (!checkExistDest) {
					mapDefaultParams['dest'].add(newDest)
				}
			} else if (action == "delete") {
				def removeIndex
				def destRm = [:]
				print "Enter delete key: "
				keyDest = reader.readLine()
				def checkExitsKeyDest = false
				mapDefaultParams['dest'].eachWithIndex {eDest, indexRm ->
					if (eDest[keyDest] != null) {
						destRm[keyDest] = eDest[keyDest]
						checkExitsKeyDest = true
						removeIndex = indexRm
					}
				}
				while (!checkExitsKeyDest) {
					print "This key is not exist. Please re-enter (*): "
					keyDest = reader.readLine()
					mapDefaultParams['dest'].each {eDest ->
						if (eDest[keyDest] != null) {
							destRm[keyDest] = eDest[keyDest]
							checkExitsKeyDest = true
						}
					}
				}
				if (checkExitsKeyDest) {
					def iterator = mapDefaultParams['dest'].remove(destRm)
				}
			}
		} else if (keyRoot == "datadirectory") {
			println "This command will set up the information to get data directory of specified DBMS."
			println "!!Be carefull, incorrect setting will make many jobs inworkable!!"
			println "CTRL-C will quit without saving ..."
			def dbType = ""
			def defaultStr = ""
			def sqlStr = ""
			def appendStr = ""
			print "Enter DB Type (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
			dbType = reader.readLine()
			while (dbType != "ORACLE" && dbType != "SQLS" && dbType != "MYSQL" && dbType != "POSTGRES") {
				print "DB Type is incorrect. Please re-enter (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
				dbType = reader.readLine()
			}
			print "Enter default directory [/usr/local/lib/mysql/data]: "
			defaultStr = reader.readLine()
			print "Enter query to get datadirectory [SELECT @@datadir]: "
			sqlStr = reader.readLine()
			print "Enter append data []: "
			appendStr = reader.readLine()
			if (mapDefaultParams['datadirectory'][dbType] != null) {
				mapDefaultParams['datadirectory'][dbType]['default'] = defaultStr
				mapDefaultParams['datadirectory'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['datadirectory'][dbType]['getData']['append'] = appendStr
			} else {
				mapDefaultParams['datadirectory'][dbType] = [:]
				mapDefaultParams['datadirectory'][dbType]['default'] = defaultStr
				mapDefaultParams['datadirectory'][dbType]['getData'] = [:]
				mapDefaultParams['datadirectory'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['datadirectory'][dbType]['getData']['append'] = appendStr
			}
		} else if (keyRoot == "programdirectory") {
			println "This command will set up the information to get program directory of specified DBMS."
			println "!!Be carefull, incorrect setting will make many jobs inworkable!!"
			println "CTRL-C will quit without saving ..."
			def dbType = ""
			def defaultStr = ""
			def sqlStr = ""
			def appendStr = ""
			print "Enter DB Type (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
			dbType = reader.readLine()
			while (dbType != "ORACLE" && dbType != "SQLS" && dbType != "MYSQL" && dbType != "POSTGRES") {
				print "DB Type is incorrect. Please re-enter (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
				dbType = reader.readLine()
			}
			print "Enter default directory [/usr/local/lib/mysql/data]: "
			defaultStr = reader.readLine()
			print "Enter query to get programdirectory [SELECT @@basedir]: "
			sqlStr = reader.readLine()
			print "Enter append data []: "
			appendStr = reader.readLine()
			if (mapDefaultParams['programdirectory'][dbType] != null) {
				mapDefaultParams['programdirectory'][dbType]['default'] = defaultStr
				mapDefaultParams['programdirectory'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['programdirectory'][dbType]['getData']['append'] = appendStr
			} else {
				mapDefaultParams['programdirectory'][dbType] = [:]
				mapDefaultParams['programdirectory'][dbType]['default'] = defaultStr
				mapDefaultParams['programdirectory'][dbType]['getData'] = [:]
				mapDefaultParams['programdirectory'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['programdirectory'][dbType]['getData']['append'] = appendStr
			}
		} else if (keyRoot == "dbmsversion") {
			println "This command will set up the information to get version of specified DBMS."
			println "!!Be carefull, incorrect setting will make many jobs inworkable!!"
			println "CTRL-C will quit without saving ..."
			def dbType = ""
			def defaultStr = ""
			def sqlStr = ""
			def appendStr = ""
			print "Enter DB Type (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
			dbType = reader.readLine()
			while (dbType != "ORACLE" && dbType != "SQLS" && dbType != "MYSQL" && dbType != "POSTGRES") {
				print "DB Type is incorrect. Please re-enter (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
				dbType = reader.readLine()
			}
			print "Enter default directory [/usr/local/lib/mysql/data]: "
			defaultStr = reader.readLine()
			print "Enter query to get dbmsversion [SELECT version()]: "
			sqlStr = reader.readLine()
			print "Enter append data []: "
			appendStr = reader.readLine()
			if (mapDefaultParams['dbmsversion'][dbType] != null) {
				mapDefaultParams['dbmsversion'][dbType]['default'] = defaultStr
				mapDefaultParams['dbmsversion'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['dbmsversion'][dbType]['getData']['append'] = appendStr
			} else {
				mapDefaultParams['dbmsversion'][dbType] = [:]
				mapDefaultParams['dbmsversion'][dbType]['default'] = defaultStr
				mapDefaultParams['dbmsversion'][dbType]['getData'] = [:]
				mapDefaultParams['dbmsversion'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['dbmsversion'][dbType]['getData']['append'] = appendStr
			}
		} else if (keyRoot == "dblogdir") {
			println "This command will set up the information to get dblog directory of specified DBMS."
			println "!!Be carefull, incorrect setting will make many jobs inworkable!!"
			println "CTRL-C will quit without saving ..."
			def dbType = ""
			def defaultStr = ""
			def sqlStr = ""
			def appendStr = ""
			print "Enter DB Type (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
			dbType = reader.readLine()
			while (dbType != "ORACLE" && dbType != "SQLS" && dbType != "MYSQL" && dbType != "POSTGRES") {
				print "DB Type is incorrect. Please re-enter (ORACLE|SQLS|MYSQL|POSTGRES)(*): "
				dbType = reader.readLine()
			}
			print "Enter default directory [/usr/local/lib/mysql/data]: "
			defaultStr = reader.readLine()
			print "Enter query to get dblogdir [SELECT @@general_log_file;]: "
			sqlStr = reader.readLine()
			print "Enter append data []: "
			appendStr = reader.readLine()
			if (mapDefaultParams['dblogdir'][dbType] != null) {
				mapDefaultParams['dblogdir'][dbType]['default'] = defaultStr
				mapDefaultParams['dblogdir'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['dblogdir'][dbType]['getData']['append'] = appendStr
			} else {
				mapDefaultParams['dblogdir'][dbType] = [:]
				mapDefaultParams['dblogdir'][dbType]['default'] = defaultStr
				mapDefaultParams['dblogdir'][dbType]['getData'] = [:]
				mapDefaultParams['dblogdir'][dbType]['getData']['sql'] = sqlStr
				mapDefaultParams['dblogdir'][dbType]['getData']['append'] = appendStr
			}
		}

		FileWriter fw = new FileWriter(defaultParamsFile)
		BufferedWriter bw = new BufferedWriter(fw);
		def builder = new JsonBuilder(mapDefaultParams)
		def str_params =  builder.toPrettyString().replaceAll("\\{","\\[").replaceAll("\\}","\\]")
		try {
			bw.write(str_params);
			println "[SUCCESS] DEFAULT.PARAMS FILE IS UPDATED !!!"
			bw.close();
		} catch (Exception ex) {
			println "[ERROR] " + ex
		}
	}
}