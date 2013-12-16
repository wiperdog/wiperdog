import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.regex.Matcher
import java.util.regex.Pattern

public class CreateFork {
	static final String FORK_FOLDER_NAME = "fork"
	static final String TMP_FOLDER_NAME = "tmp"
	static final String ETC_FOLDER_NAME = "etc"
	static final String VAR_FOLDER_NAME = "var"
	static final String LOG_FOLDER_NAME = "log"
	static final String CONFIG_FILE_NAME = "config.properties"
	static final String SYSTEM_FILE_NAME = "system.properties"
	static final String CONFIGLOG_FILE_NAME = "org.ops4j.pax.logging.cfg"
	static final String MONITORJOBCFG_FILE_NAME = "monitorjobfw.cfg"
	
	/*
	 * Main function, create wiperdog data fork directory
	 * Input: portForFork
	 * Output: fork data directory for etc, tmp, var folder
	 */
	public static void main(String[] args) throws Exception {
		// Get args
		def portForFork
		def felix_home
		
		//Check parameter
		if((args.length == 2 ) && (args[0] =='-f')) {
			portForFork = args[1]
			if (checkPortIsNumber(portForFork)) {
				//Get felix home
				felix_home = getFelixHome()
				def portInt = Integer.parseInt(portForFork)
				//Check if has 0 in head
				if (portInt.toString() != portForFork) {
					println "Port has no \"0\" in head."
					println "The process will be continues with port = " + portInt.toString()
					portForFork = portInt.toString()
				}
				//Check port different to parents
				if (checkPortParent(felix_home, portForFork)) {
					//Check fork data folder for portForFork exists or not
					def forkFolderPath = felix_home + System.getProperty("file.separator") + FORK_FOLDER_NAME
					def forkFolderForPortPath = forkFolderPath + System.getProperty("file.separator") + portForFork
					def forkFolderForPort = new File(forkFolderForPortPath)
					if (!forkFolderForPort.exists()) {
						try {
							println "Start process ......"
							
							//Create fork data directory
							createDir(felix_home, FORK_FOLDER_NAME)
							createDir(forkFolderPath, portForFork)
							
							//Create input/output file for copy
							File tmpDirSrc = new File(felix_home, TMP_FOLDER_NAME)
							File etcDirSrc = new File(felix_home, ETC_FOLDER_NAME)
							File varDirSrc = new File(felix_home, VAR_FOLDER_NAME)
							File logDirSrc = new File(felix_home, LOG_FOLDER_NAME)
							File tmpDirDest = new File(forkFolderForPortPath, TMP_FOLDER_NAME)
							File etcDirDest = new File(forkFolderForPortPath, ETC_FOLDER_NAME)
							File varDirDest = new File(forkFolderForPortPath, VAR_FOLDER_NAME)
							File logDirDest = new File(forkFolderForPortPath, LOG_FOLDER_NAME)
							
							//Copy data
							println "Copy data from " + tmpDirSrc.getPath() + " to " + tmpDirDest.getPath()
							copyFolder(tmpDirSrc, tmpDirDest)
							
							println "Copy data from " + etcDirSrc.getPath() + " to " + etcDirDest.getPath()
							copyFolder(etcDirSrc, etcDirDest)
							
							//Do not copy felix folder in var
							def exceptionFiles = []
							exceptionFiles.push(felix_home + System.getProperty("file.separator") + VAR_FOLDER_NAME + System.getProperty("file.separator") + "felix")
							println "Copy data from " + varDirSrc.getPath() + " to " + varDirDest.getPath() + ", except " + exceptionFiles
							copyFolder(varDirSrc, varDirDest, exceptionFiles)
							
							//Do not copy log data but create dir
							println "Create log folder in " + logDirDest.getPath()
							exceptionFiles = []
							exceptionFiles.push(felix_home + System.getProperty("file.separator") + LOG_FOLDER_NAME)
							copyFolder(logDirSrc, logDirDest, exceptionFiles)
							
							//Update config info
							println "Reconfig data to fork......"
							updateForkConfig(forkFolderForPortPath, portForFork)
							
							println "Create fork data successfully !!!"
						} catch(Exception ex) {
							println "Create fork data fail !!!"
							println ex
						}
					} else {
						println "Fork folder for this port exists. Please choose another port."
					}
				} else {
					println "This port is used by Wiperdog root. Please choose another port."
				}
			} else {
				println "Incorrect parameters !!!"
				println "Port has to be a number with max data is 65535."
			}
		} else {
			println "Incorrect parameters !!!"
			println "Correct format of commmand: createFork -f portForFork"
			println "Example: createFork.sh -f 23111"
		}
	}
	
	/*
	 * Check port is number or not
	 * Input: Port from commandline parameter
	 * Output: True if port is number, false if others
	 */
	public static Boolean checkPortIsNumber(portForFork) {
		try {
			int portInt = Integer.parseInt(portForFork)
			if (portInt > 65535) {
				return false
			} else {
				return true
			}
		} catch(Exception ex){
			return false
		}
	}
	
	/*
	 * Check port is the same with parents or not
	 * Input: Port from commandline parameter
	 * Output: True if port is different to parents, false if the same
	 */
	public static Boolean checkPortParent(felix_home, portForFork) {
		try {
			def parentsPort = ""
			def parentSysProFile = new File(felix_home + System.getProperty("file.separator") + ETC_FOLDER_NAME + System.getProperty("file.separator") + SYSTEM_FILE_NAME)
			def fileText = parentSysProFile.text
			String macherPattern = "(jetty.port=)((?:(?!\\n).)*)"
			Pattern pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
			Matcher matcher = pattern.matcher(fileText);
			while(matcher.find()) {
				parentsPort = matcher.group(2)
			}
			if (parentsPort == portForFork) {
				return false
			} else {
				return true
			}
		} catch(Exception ex){
			println ex
			return false
		}
	}
	
	/*
	 * Get wiperdog root home
	 */
	public static String getFelixHome(){
		def felix_home = System.getProperty("felix.home")
		if ((felix_home == null) || (felix_home == "")) {
			File currentDir = new File(System.getProperty("bin_home"))
			felix_home = currentDir.getParent()
		}
		return felix_home
	}
	
	/*
	 * Create folder if not exists
	 * Input:
	 *     home: directory home
	 *     folderName: folder will be created in home
	 * Output:
         *     create folderName in home directory
	 */
	public static void createDir(home, folderName) {
		File folder = new File(home, folderName); 
	        if(! folder.exists()) {
	            folder.mkdirs();
		}
	}
	
	/*
	 * Copy directory from src to dest, without file in exceptionFiles
	 */
    	public static void copyFolder(File src, File dest, List<String>exceptionFiles = []){
	 	if(src.isDirectory()){
 			//if directory not exists, create it
    			if(!dest.exists()){
    		   		dest.mkdir();
    			}
			//Copy
			if (!exceptionFiles.contains(src.getPath())) {
	    			//list all the directory contents
	    			def fileLists = src.list();
	 			
	    			for (String file : fileLists) {
	    		   		//construct the src and dest file structure
	    		   		File srcFile = new File(src, file);
	    		   		File destFile = new File(dest, file);
	    		   		//recursive copy
	    		   		copyFolder(srcFile, destFile, exceptionFiles);
	    			}
			}
    		} else {
			if (!exceptionFiles.contains(src.getPath())) {
	    			//if file, then copy it
	    			//Use bytes stream to support all file types
	    			InputStream inputStream = new FileInputStream(src);
	    	        	OutputStream outputStream = new FileOutputStream(dest); 
	 			
	    	        	byte[] buffer = new byte[1024];
	 			
	    	        	int length;
	    	        	//copy the file content in bytes 
	    	        	while ((length = inputStream.read(buffer)) > 0){
	    	    	   		outputStream.write(buffer, 0, length);
	    	        	}
	 			
	    	        	inputStream.close();
	    	        	outputStream.close();
			}
    		}
    	}
	/*
	 * Update config for fork directory
	 */
    	public static void updateForkConfig(forkFolderForPortPath, portForFork){
		def configFilePath
		List listDataUpdate = []
		
		
		//Update config.properties
		configFilePath = forkFolderForPortPath + System.getProperty("file.separator") + ETC_FOLDER_NAME + System.getProperty("file.separator") + CONFIG_FILE_NAME
		
		listDataUpdate = pushNewDataToList(listDataUpdate, "felix.cache.rootdir", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork)
		listDataUpdate = pushNewDataToList(listDataUpdate, "felix.fileinstall.dir", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + ETC_FOLDER_NAME)
		listDataUpdate = pushNewDataToList(listDataUpdate, "felix.fileinstall.tmpdir", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + TMP_FOLDER_NAME)
		
		updateDataFile(configFilePath, listDataUpdate)
		
		//Update system.properties
		listDataUpdate = []
		configFilePath = forkFolderForPortPath + System.getProperty("file.separator") + ETC_FOLDER_NAME + System.getProperty("file.separator") + SYSTEM_FILE_NAME
		
		listDataUpdate = pushNewDataToList(listDataUpdate, "bundles.configuration.location", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + ETC_FOLDER_NAME)
		listDataUpdate = pushNewDataToList(listDataUpdate, "felix.config.properties", "file:\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + ETC_FOLDER_NAME + "/config.properties")
		listDataUpdate = pushNewDataToList(listDataUpdate, "java.io.tmpdir", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + TMP_FOLDER_NAME)
		listDataUpdate = pushNewDataToList(listDataUpdate, "jetty.port", portForFork)
		
		updateDataFile(configFilePath, listDataUpdate)
		
		//Update org.ops4j.pax.logging.cfg
		listDataUpdate = []
		configFilePath = forkFolderForPortPath + System.getProperty("file.separator") + ETC_FOLDER_NAME + System.getProperty("file.separator") + CONFIGLOG_FILE_NAME
		listDataUpdate = pushNewDataToList(listDataUpdate, "log4j.appender.file.file", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + LOG_FOLDER_NAME + "/wiperdog.log")
		updateDataFile(configFilePath, listDataUpdate)
		
		//Update monitorjobfw.cfg
		listDataUpdate = []
		configFilePath = forkFolderForPortPath + System.getProperty("file.separator") + ETC_FOLDER_NAME + System.getProperty("file.separator") + MONITORJOBCFG_FILE_NAME
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.job", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + VAR_FOLDER_NAME + "/job")
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.monitorjobdata", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + TMP_FOLDER_NAME)		
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.jobparameters", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + VAR_FOLDER_NAME + "/job")
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.defaultparameters", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + VAR_FOLDER_NAME + "/conf")
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.dbpasswordfile", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + VAR_FOLDER_NAME + "/conf")
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.systempropertiesfile", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + ETC_FOLDER_NAME)
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.monitorswap", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + VAR_FOLDER_NAME + "/swap/monitorjob")
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.directory.log", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + LOG_FOLDER_NAME)
		listDataUpdate = pushNewDataToList(listDataUpdate, "monitorjobfw.mongodb.pass.config", "\${felix.home}/" + FORK_FOLDER_NAME + "/" + portForFork + "/" + ETC_FOLDER_NAME + "/mongodbpass.cfg")
		
		updateDataFile(configFilePath, listDataUpdate)
	}
	
	/*
	 * Update data file contents
	 * Input:
	 *     filePath: file data will be update
	 *     listDataUpdate: List of data which used for update. Item of list will be a map, which has two data: key and newData for key.
         * Output:
         *     Data in filePath will be replaced by new data in mapData
	 */
    	public static void updateDataFile(filePath, listDataUpdate){
		def configFile = new File(filePath)
		def fileText = configFile.text
		def newFileText = ""
		
		String macherPattern
		Pattern pattern
		Matcher matcher
		listDataUpdate.each{dataItem ->
			if ((dataItem.key != null) && (dataItem.newData != null)) {
				macherPattern = "(" + dataItem.key + "=)((?:(?!\\n).)*)"
				pattern = Pattern.compile(macherPattern, Pattern.DOTALL);
				matcher = pattern.matcher(fileText);
			        while(matcher.find()) {
			            newFileText = fileText.replace(matcher.group(), matcher.group(1) + dataItem.newData)
			        }
			}
			fileText = newFileText
		}
		configFile.write(newFileText)
	}
	
	/*
	 * Push new data to list for update
	 * Input:
	 *     listDataUpdate: list data will be update
	 *     keyNewData: key of new data
	 *     valueNewData: value of new data
         * Output:
         *     new data will be added in listDataUpdate
	 */
	public static List pushNewDataToList(List listDataUpdate, String keyNewData, String valueNewData) {
		def dataUpdateItem = [:]
		dataUpdateItem["key"] = keyNewData
		dataUpdateItem["newData"] = valueNewData
		listDataUpdate.push(dataUpdateItem)
		return listDataUpdate
	}
}
