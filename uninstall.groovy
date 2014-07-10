@Grapes(@Grab(group='org.mongodb',module='mongo-java-driver',version='2.12.2'))

import com.mongodb.*

println "========================="
println "Uninstall service:" + this.args[0]
println "Delete MongoDB data:" + this.args[1]
println "Delete wiperdog's files:" + this.args[2]
println "========================="

def rmService = (this.args[0] == "TRUE")?true:false
def rmMongoData = (this.args[1] == "TRUE")?true:false
def rmFiles = (this.args[2] == "TRUE")?true:false

if(rmService){
	uninstallService()
}
sleep 2
if(rmMongoData){
	uninstallMongoData()
}
sleep 2
if(rmFiles){
	uninstallFile()
}
sleep 2
println "*** WIPERDOG UNINSTALLED SUCCESSFULL !"
def executeCommand(List<String> listCmd){
	File workDir = new File(System.getProperty("user.dir"));
	ProcessBuilder builder = new ProcessBuilder(listCmd);
	builder.redirectErrorStream(true);
	builder.directory(workDir);
	Process proc = builder.start();		
	proc.waitFor();	
	println proc.in.text
	// Not use because redirect stderr to stdout already
	//println proc.err.text
}
def uninstallService(){
	println "*** UNINSTALL WIPERDOG SERVICE..."
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

		//-- Wait for kill command completed
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
		println "*** WIPERDOG'S SERVICE REMOVED !"	

	}
	
}

def uninstallFile(){
	println "*** REMOVED WIPERDOG'S FILES..."
	File WDHome = new File(System.getProperty("WIPERDOG_HOME"))
	deleteAllFileAndFolder(WDHome)
	println "*** WIPERDOG'S FILES REMOVED !"

}

def deleteAllFileAndFolder(path){
	if(path.exists()){
		// Recursively delete files
		path.listFiles().each{file->
			if(file.isDirectory()){
				deleteAllFileAndFolder(file)
			}else{
				file.delete()
			}
		}
	}
	//Delete leftover empty folder
	println "Delete $path: " + path.delete()
}

def uninstallMongoData(){
	println "*** REMOVE MONGODB DATA..."
	// Remove data in localhost
	Mongo mongo = new Mongo()
	DB db = mongo.getDB('wiperdog')
	db.dropDatabase()
	
	// Check and remove data in remote host
	File paramFile = new File(System.getProperty("WIPERDOG_HOME") +  "/var/conf/default.params")
	if(paramFile.exists()){
		def param = (new GroovyShell()).evaluate(paramFile)
		if(param != null){
			//Check dest param map: dest: [ [ file: "stdout" ], [mongoDB: "localhost:27017/wiperdog" ] ]
			if(param.dest != null && param.dest instanceof List){
				param.dest.each{des->
					//Check config: [mongoDB: "localhost:27017/wiperdog"] 
					if(des.mongoDB != null){
						try{
							def add 
							def dbName 
							if(des.mongoDB.contains("/")){
								add = des.mongoDB.substring(0,des.mongoDB.indexOf("/") )
								dbName = des.mongoDB.substring(des.mongoDB.lastIndexOf("/") + 1)
							}

							if(add != null && dbName != null ) {
								mongo = new Mongo(add)
								db = mongo.getDB(dbName)
								db.dropDatabase()
								println "*** WIPERDOG'S MONGODB DATA REMOVED..."
							} else {
								println "Failed to access to MongoDB at ${add} with database name is:  ${dbName}"
							}
						}catch(ex){
							println "Failed to delete wiperdog's data from MongoDB at ${des}... Please delete manually !"
							ex.printStackTrace()
						}
					}
				}
			}
		}	
	} else {
		println " Params file not existed : ${paramFile.getAbsolutePath()}"
	}
}