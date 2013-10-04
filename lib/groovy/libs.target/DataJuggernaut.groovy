import com.google.gson.Gson;
import com.google.gson.GsonBuilder
import com.gmongo.GMongo
import org.apache.log4j.Logger;
	
class DataJuggernaut{
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	Gson gson = new GsonBuilder().setPrettyPrinting().create()
	GroovyShell shell = new GroovyShell()
	def properties = MonitorJobConfigLoader.getProperties()
	def policyPath = properties.get(ResourceConstants.JOB_DIRECTORY) + "/policy"
	def final DEFAULT_HOST = "localhost"
	def final DEFAULT_PORT = 27017
	def final DEFAULT_DBNAME = "wiperdog"
	def final DEFAULT_COLLECTION = "policy_message"

	def judgeData(data){
		def mongo
		def ret // Format: [jobName:"aaaa", instanceName:"bbbb", message:[...]]
		File policyFile = getpolicyFile(data.sourceJob, data.istIid)
		if(policyFile != null){
			def policyObj = shell.evaluate(policyFile)
			def binding = policyObj.getBinding()
			def policyClos = binding.getVariable("POLICY")
			ret = policyClos(data.data)
			ret['fetchedAt_bin'] = data.fetchedAt_bin
			if(ret.message != null && ret.message.size() > 0){
				mongo = createConnection()
				def dbName = properties.get(ResourceConstants.MONGODB_DBNAME) != null ? properties.get(ResourceConstants.MONGODB_DBNAME) : DEFAULT_DBNAME
				def db = mongo.getDB(dbName)
				def collection = db.getCollection(DEFAULT_COLLECTION)
				collection.insert(ret)
				mongo.close()
				logger.info("--------Finished judgement data and save message into mongoDB.${DEFAULT_COLLECTION}------------")
			}else{
				logger.info("--------Nothing is wrong by now...--------")
			}
		}else {
			logger.info("--------There is no suitable policy file to judge data...--------")
		}
	}
	
	def createConnection(){
		def mongo
		def host = properties.get(ResourceConstants.MONGODB_HOST) != null ? properties.get(ResourceConstants.MONGODB_HOST) : DEFAULT_HOST
		def port = properties.get(ResourceConstants.MONGODB_PORT) != null ? properties.get(ResourceConstants.MONGODB_PORT) : DEFAULT_PORT
		if(host == "localhost" && port == null){
			mongo = new GMongo()
		}else if(host == "localhost" && port != null){
			mongo = new GMongo(host + ":" + port)
		}else if(host != null && port != null){
			mongo = new GMongo(host, Integer.valueOf(port))
		}
		return mongo
	}
	
	def getpolicyFile(jobName, istIid){
		// Get policy file of instance
		File policyFile = new File("${policyPath}/${jobName}.${istIid}.policy")
		
		// If policy file of instance is not exists then get the common policy file
		if(!policyFile.exists()){
			policyFile = new File("${policyPath}/${jobName}.policy")
			if(!policyFile.exists()){
				policyFile = null
			}
		}
		return policyFile
	}
}