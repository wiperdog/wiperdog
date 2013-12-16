import java.security.*
import javax.crypto.*
import javax.crypto.spec.*
import java.io.*
import javax.xml.bind.DatatypeConverter
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern

public class ProcessMongoInfo {
	public static final felix_home = getFelixHome()
	public static final String FORK_FOLDER = "/fork"
	public static final String FILE_COMMON_CONFIG = "/etc/monitorjobfw.cfg"
	public static final String FILE_MANUAL_CONFIG = "/etc/mongodbpass.cfg"
	public static final String FILE_COMMON_UTIL = "/lib/groovy/libs.target/CommonUltis.groovy"
	/**
	 * main: process update information into monitorjobfw.cfg and mongodbpassinfo.cfg
	 * @param args: data receive from batch file
	*/
	public static void main(String[] args) throws Exception {
		// update config information
		def mapConfig = [:]
		mapConfig['fork'] = args[0].trim()
		mapConfig['host'] = args[2].trim()
		mapConfig['port'] = args[3].trim()
		mapConfig['dbName'] = args[4].trim()
		mapConfig['user'] = args[5].trim()
		if(args[6] != null && args[6].trim() != "") {
			mapConfig['pass'] = ProcessMongoInfo.encryptedPassword(args[6].trim())
		} else {
			mapConfig['pass'] = ""
		}
		if(args[1] == "commonConfig") {
			if(ProcessMongoInfo.updateCommonConfig(mapConfig)) {
				println "========== UPDATE COMMON CONFIG SUCCESSFULLY =========="
			}
		} else if(args[1] == "manualConfig") {
			if(ProcessMongoInfo.updateManualConfig(mapConfig)) {
				println "========== UPDATE MANUAL CONFIG SUCCESSFULLY =========="
			}
		}
	}
	
	/**
	 * updateCommonConfig: update information into monitorjobfw.cfg
	 * @param mapCommon: mapdata receive from batch file
	 * @return boolean, true if update success, false if update failed
	*/
	public static updateCommonConfig(mapCommon) {
		def newFileText = ""
		try {
			def commonFile
			//Get config file
			if ((mapCommon['fork'] != null) && (mapCommon['fork'].trim() != "")) {
				commonFile = new File( felix_home + FORK_FOLDER + "/" + mapCommon['fork'] + FILE_COMMON_CONFIG)
			} else {
				commonFile = new File( felix_home + FILE_COMMON_CONFIG)
			}
			if (commonFile.exists()) {
				// host name
				def fileText = commonFile.text
				String macherPattern = "(monitorjobfw.mongodb.host=)((?:(?!\\n).)*)"
		        Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
		        Matcher matcher = pattern.matcher(fileText);
		        while(matcher.find()) {
		            newFileText = fileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + mapCommon['host'])
		        }
		        
		        // port number
		        macherPattern = "(monitorjobfw.mongodb.port=)((?:(?!\\n).)*)"
		        pattern = Pattern.compile(macherPattern, Pattern.DOTALL)
		        matcher = pattern.matcher(newFileText)
		        while(matcher.find()) {
		            newFileText = newFileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + mapCommon['port'])
		        }
		        
		        // database name
		        macherPattern = "(monitorjobfw.mongodb.dbName=)((?:(?!\\n).)*)"
		        pattern = Pattern.compile(macherPattern, Pattern.DOTALL)
		        matcher = pattern.matcher(newFileText)
		        while(matcher.find()) {
		            newFileText = newFileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + mapCommon['dbName'])
		        }
		        
		        // user name
		        macherPattern = "(monitorjobfw.mongodb.user=)((?:(?!\\n).)*)"
		        pattern = Pattern.compile(macherPattern, Pattern.DOTALL)
		        matcher = pattern.matcher(newFileText)
		        while(matcher.find()) {
		            newFileText = newFileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + mapCommon['user'])
		        }
		        
		        // password
		        macherPattern = "(monitorjobfw.mongodb.pass=)((?:(?!\\n).)*)"
		        pattern = Pattern.compile(macherPattern, Pattern.DOTALL)
		        matcher = pattern.matcher(newFileText)
		        while(matcher.find()) {
		            newFileText = newFileText.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + mapCommon['pass'])
		        }
		        commonFile.write(newFileText)
		        return true
	        } else {
	        	println "Configuration file does not exist!!!"
	        	return false
	        }
		} catch (Exception ex) {
			println "ERROR WRITE TO MONITORJOBFW.CFG, REASON: " + ex
			return false
		}
	}
	
	/**
	 * updateManualConfig: update information into mongodbpass.cfg
	 * @param mapManual: mapdata receive from batch file
	 * @return boolean, true if update success, false if update failed
	*/
	public static updateManualConfig(mapManual) {
		def manualFile
		//Get config file
		if ((mapManual['fork'] != null) && (mapManual['fork'].trim() != "")) {
			manualFile = new File( felix_home + FORK_FOLDER + "/" + mapManual['fork'] + FILE_MANUAL_CONFIG)
		} else {
			manualFile = new File( felix_home + FILE_MANUAL_CONFIG)
		}
		def newFileText = ""
		def checkExist = false
		def oldUser
		def destination = mapManual['host'] + ":" + mapManual['port'] + "/" + mapManual['dbName']
		try {
			if (manualFile.exists()) {
				def lstInfo
				def oldLine
				def newLine = destination + "," + mapManual['user'] + "," + mapManual['pass']
				// Find old data
				manualFile.eachLine {line ->
					lstInfo = line.split(",")
					//check data user and pass empty
					if(lstInfo.size() == 1) {
						oldUser = ""
					} else {
						oldUser = lstInfo[1].trim()
					}
					//update row data if user exist
					if(lstInfo[0] == destination && oldUser == mapManual['user']) {
						checkExist = true
						oldLine = line
					}
				}
				//Create new text, replace old data if exists, create new if not exists
				newFileText = manualFile.getText()
				if(checkExist) {
					newFileText = newFileText.replace(oldLine, newLine)
				} else {
					if (newFileText != "") {
						newFileText += "\n"
					}
					newFileText += newLine
				}
				// Write to file
				manualFile.write(newFileText)
				return true
			} else {
	        	println "Configuration file does not exist!!!"
	        	return false
			}
		} catch (Exception ex) {
			println "ERROR WRITE TO MONGODBPASS.CFG, RESION: " + ex
			return false
		}
	}
	/**
	 * encryptedPassword: encrypted password
	 * @param originPass: password is not encrypted
	 * @return encryptedPasswd: password is encrypted
	*/
	public static encryptedPassword(originPass) {
		// get file CommonUtil
		def commonUtilFile = new File(felix_home + FILE_COMMON_UTIL)
		// Parse file
		GroovyClassLoader gcl = new GroovyClassLoader()
		Class commonClass = gcl.parseClass(commonUtilFile)
		Object commonUtil_obj = commonClass.newInstance()
        // encryped password
		def encryptedPasswd = commonUtil_obj.encrypt(originPass)
		return encryptedPasswd
	}
	
	/**
	 * getFelixHome: 
	 * @return felix_home
	*/
	public static String getFelixHome(){
		def felix_home = System.getProperty("felix.home")
		if ((felix_home == null) || (felix_home == "")) {
			File currentDir = new File(System.getProperty("bin_home"))
			felix_home = currentDir.getParent()
		}
		return felix_home
	}
}