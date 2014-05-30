import java.io.BufferedReader
import java.io.InputStreamReader

def jobClass 
def concurrency
def maxRunTime
def maxWaitTime

BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
println ""
println "Please input data for job class creation: "
println "***Note : Field with (*) is mandatory ,another is option"
println "Leave job class name empty to exit input !"
println "---------------------------"
print "Enter job class file name:" ;
def jobClassFileName =  reader.readLine()
while(jobClass == null || jobClass.trim().equals("")){
	print "Enter job class name (*): "
	jobClass = reader.readLine()
}
def whileCondition = false
while(!whileCondition){
	try{
		print "Enter job concurrency :" ;
		concurrency =  reader.readLine()
		if(!concurrency.trim().equals("")) {
			concurrency = Integer.parseInt(concurrency);
			whileCondition = true
		} else {
			concurrency = null
			break
		}
	}catch(Exception ex){
		println "Job concurrency must be numberic ,please try again !"				
	}
}
whileCondition = false
while(!whileCondition){
	try{
		print "Enter job max run time :" ;
		maxRunTime =  reader.readLine()
		if(!maxRunTime.trim().equals("")) {
			maxRunTime = Integer.parseInt(maxRunTime);
			whileCondition = true
		} else {
			maxRunTime = null
			break
		}
	}catch(Exception ex){
		println "Max run time must be numberic ,please try again !"				
	}
}

whileCondition = false
while(!whileCondition){
	try{
		print "Enter job max wait time :" ;
		maxWaitTime =  reader.readLine()
		if(!maxWaitTime.trim().equals("")) {
			maxWaitTime = Integer.parseInt(maxWaitTime);
			whileCondition = true
		} else {
			maxWaitTime = null
			break
		}
	}catch(Exception ex){
		println "Max wait time must be numberic ,please try again !"				
	}
}

if(jobClassFileName == null || jobClassFileName.trim().equals("") ){
	jobClassFileName = jobClass
}
if(!jobClassFileName.endsWith(".cls")){
	jobClassFileName += ".cls"
}
def basedir = new File(getClass().protectionDomain.codeSource.location.path).parent
File jobClassFile = new File(basedir + "/../var/job",jobClassFileName);
if(!jobClassFile.exists()){
	jobClassFile.createNewFile()
} 
def jobClsTxt = jobClassFile.getText()
def listJC = []
jobClsTxt.eachLine{
	if(!it.trim().equals("")) {
		def trgElement = [:]
		def tmpList = it.split(",")
		tmpList.each{ e->
			trgElement[e.split(":")[0].trim().replace('"',"")] = e.split(":")[1].trim().replace('"',"")
		}
		listJC.add(trgElement)
	}

}
def mapJC = ["name" : jobClass ,"concurrency" : concurrency, "maxrun" : maxRunTime, "maxwait" : maxWaitTime]
if(listJC != []) {
	//Update trigger if existed
	def existed = false
	listJC.collect{
		if(it["name"].equals(jobClass)) {
			//it[""] = schedule
			it["concurrency"] = concurrency
			it["maxrun"] = maxRunTime
			it["maxwait"] = maxWaitTime
			existed = true
		}
	}
	if(!existed) {
		listJC.add(mapJC)
	}

} else {
	listJC.add(mapJC)
}

def jcStr = ""
listJC.each{ 
	jcStr+= "name: \"${it.name}\""
	if(it.concurrency != null && !it.concurrency.equals("")) {
		jcStr += ", concurrency: ${it.concurrency}"
	}
	if(it.maxrun != null && !it.maxrun.equals("")) {
		jcStr += ", maxrun: ${it.maxrun}"
	}
	if(it.maxwait != null && !it.maxwait.equals("")) {
		jcStr += ", maxwait: ${it.maxwait}"
	}
}



jobClassFile.setText(jcStr)
println "---------------------------"
println "Finished! Job class file create at : ${jobClassFile.getCanonicalPath()}"



