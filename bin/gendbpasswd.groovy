import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;

public class AES {
	static final String COMMON_UTIL_FILE = "/lib/groovy/libs.target/CommonUltis.groovy"
	static final String PASSWD_DIR = "/var/conf/"
	static final String FORK_DIR = "/fork/"
	
	public static void main(String[] args) throws Exception {
		//get CommonUtils.groovy file to parse
		def felix_home = getFelixHome()
		def commonUtilFile = new File( felix_home + COMMON_UTIL_FILE)
		//Parse file
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class commonClass = gcl.parseClass(commonUtilFile);
		Object commonUtil_obj = commonClass.newInstance()
		def DBTypeList = [
			'@ORA' ,
			'@MYSQL' ,
			'@PGSQL' ,
			'@MSSQL'
		]
		def DBType = null
		def username = null
		def fileCSV = null
		def hostId = ""
		def sid = ""
		def password = null
		def encryptedString = null
		def forkFolderPath = ""
		def numOfParams = args.length
		
		// Get args
		if((numOfParams == 3 ) && (args[0] =='-f')) {
			//get file csv from 2nd params
			fileCSV = new File(args[1])
			if(!fileCSV.isAbsolute()){
				def dir = args[2]
				fileCSV = new File(dir + fileCSV)
			}
			//Get data from csv
			def listOfCSVElement = processCSVFile(fileCSV,DBTypeList)
			if(listOfCSVElement == null || listOfCSVElement.size() == 0){
				return
			}else {
				//process write to .dbpassword file
				listOfCSVElement.each{
					def dbType = escapeChar(it['DBTYPE'])
					def userName = escapeChar(it['USERNAME'])
					def passwordCSV = it['PASSWORD']
					def hostIdCSV = escapeChar(it['HOSTID'])
					def sId = escapeChar(it['SID'])
					try{
						def passwdFilename = commonUtil_obj.getPasswdFileName(dbType, hostIdCSV, sId)
						updatePwdFile(commonUtil_obj,passwdFilename,userName,passwordCSV)
						println "Create password successfully for [${dbType},${userName}, ${hostIdCSV}, ${sId}]"
						return

					} catch (Exception ex){
						println "Create password failed for [${dbType},	${userName}, ${hostIdCSV}, ${sId} ]"
						println "Reason: " + ex
						return
					}
				}
			}
		} else {
			for (int i = 0; i < (numOfParams - 1); i++) {
				if(args[i] == '-t') {
					DBType = args[i + 1]
				}else if(args[i] == '-u') {
					username = args[i + 1]
				}else if(args[i] == '-h') {
					hostId = args[i + 1]
				}else if(args[i] == '-s') {
					sid = args[i + 1]
				}else if(args[i] == '-f') {
					forkFolderPath = args[i + 1]
				}
			}
			println "DBType:" + DBType
			println "Username:" + username
			println "HostId:" + hostId
			println "Sid:" + sid

			if ((DBType == null) || (username == null) || (DBType == "") || (username == "")) {
				println "Incorrect parameters !!!"
				println "Correct format of commmand: gendbpasswd -t DBType -u username [-h hostId] [-s sid] [-f fork_folder]"
				println "      OR  gendbpasswd -f 'pathFile'"
				return
			}

			if (!DBTypeList.contains(DBType)) {
				println "DBType is incorrect. DBType may accept following value: @ORA , @MYSQL ,@PGSQL ,@MSSQL"
				return
			}
			if ((forkFolderPath != null) && (forkFolderPath != "")) {
				//Check fork folder exit
				File forkFolder = new File(felix_home + FORK_DIR + forkFolderPath)
				if (!(forkFolder.exists() && forkFolder.isDirectory())){
					println "Fork folder does not exist !!!"
					return
				}
			}
			try {
				println "-----------------------------"
				println""
				// creates a console object
				Console cnsl = System.console();
				// if console is not null
				if (cnsl != null) {
					// get password from user input
					def passwordArray = cnsl.readPassword("Password: ");
					password = new String(passwordArray)
					//get password file
					def passwdFilename = commonUtil_obj.getPasswdFileName(DBType, hostId, sid)

					//Update password to file
					updatePwdFile(commonUtil_obj,passwdFilename, username, password, forkFolderPath)
					println "Create password successfully for [${DBType},${username}, ${hostId}, ${sid}]"
				}
			} catch (Exception ex){
				println "Create password failed for [${DBType},	${username}, ${hostId}, ${sid} ]"
				println "Reason: " + ex
			}
		}
	}

	public static String getFelixHome(){
		def felix_home = System.getProperty("felix.home")
		if ((felix_home == null) || (felix_home == "")) {
			File currentDir = new File(System.getProperty("bin_home"))
			felix_home = currentDir.getParent()
		}
		return felix_home
	}
	public static def processCSVFile(fileCSV,DBTypeList){
			def checkHeader = false
			def headers = null
			def listData = []
			if(!fileCSV.exists()){
				println  "File csv not found !"
				return
			}
			def exampleMess = "\n\tDBTYPE,USERNAME,PASSWORD,HOSTID,SID\n\t@MYSQL,root,myPassword,hostID1,sid1\n\t@MSSQL,sa,sa,,"
			//Read csv lines and valid
			fileCSV.readLines().find { lineCSV->
				if(!checkHeader){
					headers = lineCSV.split(",")
					checkHeader = true
					if(headers.length !=5){
						checkHeader = false
					}
					if(headers[0] != "DBTYPE") {
						checkHeader = false
					}
					if(headers[1] != "USERNAME") {
						checkHeader = false
					}

					if(headers[2] != "PASSWORD") {
						checkHeader = false
					}

					if(headers[3] != "HOSTID") {
						checkHeader = false
					}

					if(headers[4] != "SID") {
						checkHeader = false
					}
					if(!checkHeader){
						println "Incorrect headers format - Format header must be same as following example :" + exampleMess
						return true
						
					}

				} else {
					def tmpRecordList = lineCSV.split(",",-1)
					if(tmpRecordList.length < headers.length){
						println "Missing params: Params required ${headers.length} element(s) (PASSWORD,HOSTID,SID accept empty string)- Line: ${(fileCSV.readLines().indexOf(lineCSV) +1)} \n Example: " + exampleMess
						return true
					}
					listData.add(tmpRecordList)
					tmpRecordList = []
				}
			}
			if(!checkHeader){
				return
			}
			def tmpListOfMaps = []
			listData.each{ data->
				def tmpMaps = [:]
				for(int i = 0 ; i< headers.length ; i++){
					tmpMaps[headers[i]] = data[i]
				}
				tmpListOfMaps.add(tmpMaps)
			}
			def checkValidData = true
			tmpListOfMaps.find{
				//check if DBTYPE in ['@ORA' , '@MYSQL' ,'@PGSQL' ,'@MSSQL']
				def dbType = escapeChar(it['DBTYPE'])
				def userName = escapeChar(it['USERNAME'])
				def passwordCSV = it['PASSWORD']
				def hostIdCSV = escapeChar(it['HOSTID'])
				def sId = escapeChar(it['SID'])
				if ((dbType == "") || (userName == "")) {
					println "Incorrect parameters !!!"
					println "DBTYPE and USERNAME can not be empty - Line: " + (tmpListOfMaps.indexOf(it) + 2)
					checkValidData = false
					return  true
				}
				if(!DBTypeList.contains(dbType)){
					println "DBType is incorrect. DBType may accept following value: @ORA , @MYSQL ,@PGSQL ,@MSSQL - " + "Line : ${(tmpListOfMaps.indexOf(it) + 2)}"
					checkValidData = false
					return true
				}
				if(hostIdCSV.contains(".")){
					println "HOSID can not accept punctuation character '.'" + "Line : ${(tmpListOfMaps.indexOf(it) + 2)}"
					checkValidData = false
					return true
				}
				if(hostIdCSV == "" && sId != ""){
					println "HOSID can not be empty if sid avaiable Line : ${(tmpListOfMaps.indexOf(it) + 2)}"
					checkValidData = false					
					return true
				}

			}
			if(!checkValidData){
				return null
			} else  {
				return tmpListOfMaps
			}
	}
	public static void updatePwdFile(commonUtil_obj,passwdFilename, username, password, forkFolderPath = ""){
		//encryped password
		def encryptedPasswd = commonUtil_obj.encrypt(password);
		//get password file
		def mapPasswd = [:]
		def felix_home = getFelixHome()
		try {
			def pwdFile
			if ((forkFolderPath != null) && (forkFolderPath != "")) {
				pwdFile = new File(felix_home + FORK_DIR + forkFolderPath +  PASSWD_DIR + passwdFilename)
			} else {
				pwdFile = new File(felix_home + PASSWD_DIR + passwdFilename)
			}
			if (!pwdFile.exists()) {
				pwdFile.createNewFile()
			}

			// Write encrypted password into password file
			def tempArray
			pwdFile.eachLine {
				tempArray = it.split(":")
				mapPasswd[tempArray[0]] = tempArray[1].trim()
			}
			mapPasswd[username] = encryptedPasswd
			StringBuilder strBuilder = new StringBuilder()
			mapPasswd.each{
				strBuilder.append(it.key + ":" + it.value + "\n")
			}
			if (pwdFile != null) {
				pwdFile.setText(strBuilder.toString())
			}
		} catch (Exception ex) {
			println ex
		}
	}
	public static String escapeChar(str){
				return str.replace("'","").replace('"',"").trim()
	}

}