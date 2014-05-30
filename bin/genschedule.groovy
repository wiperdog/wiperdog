def trgFileName ;
def jobName;
def schedule;		
for(int i=0 ; i< args.length; i++ ){
	//get trigger file name
	if(args[i].equalsIgnoreCase("-f")){
		if(!args[i+1].trim().equals("") ){
			trgFileName = args[i+1]
		}
	}
	//get job name
	if(args[i].equalsIgnoreCase("-j")){
		if(!args[i+1].trim().equals("") ){
			jobName = args[i+1]
		}
	}
	
	//get Schedule name
	if(args[i].equalsIgnoreCase("-s")){
		if(!args[i+1].trim().equals("") ){
			schedule = args[i+1]
		}
	}
}

if(jobName == null) {
	println "Missing job name !"
	return
}

if(schedule == null) {
	println "Missing schedule !"
	return
}
if(trgFileName == null){
	trgFileName = jobName + ".trg"
}
if(!trgFileName.endsWith(".trg")) {
	trgFileName += ".trg"
}
def mapTrg = [job:jobName,schedule:schedule]

println getClass().protectionDomain.codeSource
def basedir = new File(getClass().protectionDomain.codeSource.location.path).parent
File trgFile = new File(basedir + "/../var/job/",trgFileName);
if(!trgFile.exists()){
	trgFile.createNewFile()
} 
def trgTxt = trgFile.getText()

def listTrg = []
trgTxt.eachLine{
	if(!it.trim().equals("")) {
		def trgElement = [:]
		def tmpList = it.split(",")
		tmpList.each{ e->
			trgElement[e.split(":")[0].trim().replace('"',"")] = e.split(":")[1].trim().replace('"',"")
		}
		listTrg.add(trgElement)
	}

}
if(listTrg != []) {
	//Update trigger if existed
	def existed = false
	listTrg.collect{
		if(it["job"].equals(jobName)) {
			it["schedule"] = schedule
			existed = true
		}
	}
	if(!existed) {
		listTrg.add(mapTrg)
	}

} else {
	listTrg.add(mapTrg)
}
def trgStr = ""
listTrg.each{ 
	trgStr+= "job: \"${it.job}\" , schedule : \"${it.schedule}\"\n"				
}
trgFile.setText(trgStr)
println "Finished! Trigger file create at : ${trgFile.getCanonicalPath()}"

