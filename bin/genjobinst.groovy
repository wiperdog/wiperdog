import java.io.BufferedReader
import java.io.InputStreamReader
import groovy.json.JsonBuilder

def checkExistsInst(mapInstances,instName){
	def check = false
	if(mapInstances != null && instName != null) {
		mapInstances.each{key,value ->
			if(instName.equalsIgnoreCase(key)) {
				check = true	
				return 			
			}
		}
	}
	return check
}
def shell = new GroovyShell()
def mapInstances = [:]
BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
println ""
println "Please input data for job instance creation: "
println "***Note : Field with (*) is mandatory ,another is option"
println "Leave instance name empty to exit input !"
println "---------------------------"
def jobName = null
while(jobName == null || jobName.equals("")) {
 print "Enter job name need to create instance (*) :" ;
 jobName =  reader.readLine()
}
def basedir = new File(getClass().protectionDomain.codeSource.location.path).parent

def instFile = new File(basedir + "/../var/job/" , jobName + ".instances")
def instTxt = ""
if(!instFile.exists()){
	instFile.createNewFile()
} else {
	def currentInstance = shell.evaluate(instFile)
	if(currentInstance != null && currentInstance != [:])  {
		mapInstances << currentInstance 
	}
}
def leaveInput = false
while(!leaveInput) {
	def instName = null
	def instance = [:]
	label1:
	print "Enter instance name (*) :" ;
	instName =  reader.readLine()
	if(instName.trim().equals("")){
		leaveInput = true
		break;
	} else {
		if(checkExistsInst(mapInstances,instName)) {
			print "Instance name exists,do you want to update ? (y/n) !\n" ;
			def update = reader.readLine() 
			if(!update.equalsIgnoreCase("y")) {
				continue label1
			}
		}
	}

	mapInstances[instName] = [:];
	print "Enter schedule :" ;
	def schedule =  reader.readLine()

	if(schedule != null && schedule != ""){
		mapInstances[instName]["schedule"] = schedule
	}
	while(true) {
		print "Enter params (ex: [a:1,b:2] ) :" ;
		def params =  reader.readLine()
		if(params != null && !params.equals("")) {
			try{
				params = shell.evaluate(params)				
				mapInstances[instName]["params"] = params
				break;
		    } catch(Exception ex) {
		    	ex.printStackTrace()
		   		println "Incorrect params input ,try again !"
		    }
		} else {
			break;
		}
	}

}

def builder = new groovy.json.JsonBuilder(mapInstances)
def stringToFile = builder.toPrettyString().replaceAll("\\{\\s*\\}","[:]").replace("{","[").replace("}","]")
instFile.setText(stringToFile)
println "---------------------------"
println "Finished! Instance file create at : ${instFile.getCanonicalPath()}"






