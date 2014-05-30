import java.io.BufferedReader
import java.io.InputStreamReader
import groovy.json.JsonBuilder
import java.util.regex.*
def checkExists(mapParams,paramName){
	def check = false
	if(mapParams != null && paramName != null) {
		mapParams.each{key,value ->
			if(paramName.equalsIgnoreCase(key)) {
				check = true	
				return 			
			}
		}
	}
	return check
}
def shell = new GroovyShell()
BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
println ""
println "Please input data for job params creation: "
println "***Note : Field with (*) is mandatory ,another is option"
println "Leave job param name empty to exit input !"
println "---------------------------"

def jobFileName 
while(jobFileName == null || jobFileName.trim().equals("")) {
	print "Enter job file name for param creation (*) : " ;
	jobFileName = reader.readLine()
}
def mapParams = [:]
def basedir = new File(getClass().protectionDomain.codeSource.location.path).parent
def paramFile = new File(basedir + "/../var/job", jobFileName+".params")
if(paramFile.exists()) {
	def currentParams =  shell.evaluate(paramFile)
	if(currentParams != null){
		mapParams << currentParams
	}
} else {
	paramFile.createNewFile()
}

while(true){
	label1:
	print "Enter param name : " ; 
	def paramKey = reader.readLine()
	if(paramKey == null || paramKey.trim().equals("")) {
		break;
	} else {
		if(checkExists(mapParams,paramKey)){
			print "Param key existed ! ,do you want to update ? (y/n) ! \n"
			def update = reader.readLine() 
			if(!update.equalsIgnoreCase("y")) {
				continue label1
			}

		}
	}
	def valid = false;
	print "Enter param value  (value can be a collection : [a:1,b:2]):" ; 
	def paramVal = reader.readLine()
	if(paramVal.trim().startsWith("[")) {
		try{
			paramVal = shell.evaluate(paramVal)
			mapParams[paramKey] = paramVal
		}finally{
		}
	}
	mapParams[paramKey] = paramVal
	
	
}
def builder = new JsonBuilder(mapParams)
def stringToFile = builder.toPrettyString().replaceAll("\\{\\s*\\}","[:]").replace("{","[").replace("}","]")
paramFile.setText(stringToFile)
println "---------------------------"
println "Finished! Params file create at : ${paramFile.getCanonicalPath()}"
