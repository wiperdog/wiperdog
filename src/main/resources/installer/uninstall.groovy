@Grapes(@Grab(group='org.mongodb',module='mongo-java-driver',version='2.12.2'))
	
import java.io.BufferedReader
import java.io.InputStreamReader
import com.mongodb.*

def buffReader = new BufferedReader(new InputStreamReader(System.in))
def serviceState = this.args[0]
def rmService
//Asking user for remove Wiperdog's service
if(serviceState == "TRUE") {
	println "Do you want to remove Wiperdog service? (y/n)"
	rmService = buffReader.readLine()
	while(rmService != "y" && rmService != "n" && rmService != "Y" && rmService != "N") {
		println "Do you want to remove Wiperdog service? (y/n)"
		rmService = buffReader.readLine()
	}
}
def uninstaller = new Uninstaller()
def rmMongoData = null
//Asking user for remove Wiperdog's data
File paramFile = new File(System.getProperty("WIPERDOG_HOME") +  "/var/conf/default.params")
def mongoDbInfo = uninstaller.checkMongoDBHost(paramFile)
//Checking mongodb host , if host is localhost -> asking user to remove ,otherwise ,do not remove it
if(mongoDbInfo != null && mongoDbInfo["host"] != null && mongoDbInfo["port"]  != null && mongoDbInfo["dbName"] != null) {
	if("localhost".equals(mongoDbInfo["host"]) || "127.0.0.1".equals(mongoDbInfo["host"])) {
		println "Do you want to delete all wiperdog's data in mongodb? (y/n)"
		rmMongoData = buffReader.readLine()
		while(rmMongoData != "y" && rmMongoData != "n" && rmMongoData != "Y" && rmMongoData != "N") {
			println "Do you want to delete all wiperdog's data in mongodb? (y/n)"
			rmMongoData = buffReader.readLine()
		}
	} else {
		println "*** Wiperdog data put at : ${mongoDbInfo['host']} . We do not remove it !"
	}
}



//Asking user for remove Wiperdog's files
println "Do you want to delete all wiperdog's files? (y/n)"
def rmFiles = buffReader.readLine()
while(rmFiles != "y" && rmFiles != "n" && rmFiles != "Y" && rmFiles != "N") {
	println "Do you want to delete all wiperdog's files? (y/n)"
	rmFiles = buffReader.readLine()
}

println "======================================================================================"
println "You decide to uninstall the followings:"
if(rmService == "Y" || rmService == "y"){
	println "Uninstall service: ${rmService}"
}
if(rmMongoData != null ) {
	println "Delete data in mongodb: ${rmMongoData}"
}
println "Delete Wiperdog's files: ${rmFiles}"
println "======================================================================================="
println "Press any key to continue or CTRL+C to exit..."

def goFoward = buffReader.readLine()
if(goFoward == "") {

	if(rmService == "Y" || rmService == "y"){
		uninstaller.uninstallService()
	}
	sleep 2
	if(rmMongoData == "Y" || rmMongoData == "y"){
		uninstaller.uninstallMongoData(mongoDbInfo["host"],mongoDbInfo["port"],mongoDbInfo["dbName"])
	}
	sleep 2
	if(rmFiles == "Y" || rmFiles == "y"){
		uninstaller.uninstallFile()
	}
	sleep 2
	uninstaller.printInfoLog("*** WIPERDOG UNINSTALLED SUCCESSFULL !")
	uninstaller.printInfoLog("*** Uninstall log file put at : ${uninstaller.logfile.getCanonicalPath()}")
}
class Uninstaller {
	def logfile =  new File(System.getProperty("WIPERDOG_HOME") + "/../WiperdogUninstaller.log");  
	def fileOutputStream = new FileOutputStream(logfile);

	def printInfoLog(String content) throws Exception{
		if(fileOutputStream == null)
			fileOutputStream = new FileOutputStream(loggingFile, true);		
		fileOutputStream.write((content+ "\n").getBytes());
		System.out.println(content);
	}
	def executeCommand(List<String> listCmd){
		File workDir = new File(System.getProperty("user.dir"));
		ProcessBuilder builder = new ProcessBuilder(listCmd);
		builder.redirectErrorStream(true);
		builder.directory(workDir);
		Process proc = builder.start();		
		proc.waitFor();	
		printInfoLog(proc.in.text)
		// Not use because redirect stderr to stdout already
		//println proc.err.text
	}
	def uninstallService(){
		printInfoLog("*** UNINSTALL WIPERDOG SERVICE...")
		String osName = System.getProperty("os.name").toLowerCase() 
		if(osName.indexOf("win") == -1){//-- LINUX
			//Stop service
			List<String> listCmd = new LinkedList<String>()
			listCmd.add("sudo")
			listCmd.add("service")
			listCmd.add("wiperdog")
			listCmd.add("stop")			
			executeCommand(listCmd)
				
			//Remove service
			listCmd = new LinkedList<String>()
			listCmd.add("sudo")
			listCmd.add("rm")
			listCmd.add("-f")
			listCmd.add("/etc/init.d/wiperdog")			
			executeCommand(listCmd)
			listCmd = new LinkedList<String>()
			listCmd.add("sudo")
			listCmd.add("update-rc.d")
			listCmd.add("-f")
			listCmd.add("wiperdog")			
			listCmd.add("remove")
			executeCommand(listCmd)
			
			//executeCommand("""sudo update-rc.d -f wiperdog remove""")
		} else {//-- WINDOWS		
			List<String> listCmd = new LinkedList<String>();
			listCmd.add("net");
			listCmd.add("stop");
			listCmd.add("wiperdog");
			executeCommand(listCmd)
				
			//-- kill process
			listCmd = new LinkedList<String>();
			listCmd.add("taskkill");
			listCmd.add("/F");
			listCmd.add("/IM");
			listCmd.add("wiperdog_service*");
			executeCommand(listCmd)

			//-- Wait fileOutputStreamr kill command completed
			listCmd = new LinkedList<String>();
			listCmd.add("cmd.exe");    	    	
			listCmd.add("/c");
			listCmd.add("sleep");
			listCmd.add("3");
			executeCommand(listCmd)
				
			//Remove service
			listCmd = new LinkedList<String>()
			listCmd.add("sc")
			listCmd.add("delete")
			listCmd.add("wiperdog")
			executeCommand(listCmd)
		}
		printInfoLog("*** WIPERDOG'S SERVICE REMOVED !")
	}

	def uninstallFile(){
		printInfoLog("*** REMOVED WIPERDOG'S FILES...")
		File WDHome = new File(System.getProperty("WIPERDOG_HOME"))
		deleteAllFileAndfileOutputStreamlder(WDHome)
		printInfoLog("*** WIPERDOG'S FILES REMOVED !")

	}

	def deleteAllFileAndfileOutputStreamlder(path){
		if(path.exists()){
			// Recursively delete files
			path.listFiles().each{file->
				if(file.isDirectory()){
					deleteAllFileAndfileOutputStreamlder(file)
				}else{
					file.delete()
				}
			}
		}
		//Delete leftover empty fileOutputStreamlder
		printInfoLog("Delete $path: " + path.delete())
	}

	Object checkMongoDBHost(File paramFile) {
		
		if(paramFile.exists()){
			def param = (new GroovyShell()).evaluate(paramFile)
			if(param != null){
				//Check dest param map: dest: [ [ file: "stdout" ], [mongoDB: "localhost:27017/wiperdog" ] ]
				if(param.dest != null && param.dest instanceof List){
					def mongoDbInfo = [:]
					param.dest.each{des->
						//Check config: [mongoDB: "localhost:27017/wiperdog"] 
						if(des.mongoDB != null){
							if(des.mongoDB.contains("/")){
								def host = des.mongoDB.substring(0,des.mongoDB.indexOf(":") )
								def port = des.mongoDB.substring(des.mongoDB.indexOf(":") + 1,des.mongoDB.indexOf("/") )
								def dbName = des.mongoDB.substring(des.mongoDB.lastIndexOf("/") + 1)
								mongoDbInfo["host"] = host
								mongoDbInfo["port"] = port
								mongoDbInfo["dbName"] = dbName
							}
						}
					}	
					return mongoDbInfo
			
				} else {
					printInfoLog("[Error] - Failed to get MongoDB host : Destination configuration not found in : ${paramFile.getAbsolutePath()} ")
					return null
				}
			}	
		} else {
			printInfoLog("[Error] - Failed to get MongoDB host : Params file not existed : ${paramFile.getAbsolutePath()} ")
			return null
		}
	}

	def uninstallMongoData(host,port,dbName){
		printInfoLog("*** REMOVE MONGODB DATA...")
		if(host != null && dbName != null ) {
			try{
				def mongo = new Mongo(host,Integer.parseInt(port))
				def db = mongo.getDB(dbName)
				db.dropDatabase()
				printInfoLog("*** WIPERDOG'S MONGODB DATA REMOVED...")
			}catch(ex){
				ex.printStackTrace()
				printInfoLog("[Error] - Failed to delete wiperdog's data from MongoDB at ${host}... Please delete manually !")
			}
		}
	}

}
