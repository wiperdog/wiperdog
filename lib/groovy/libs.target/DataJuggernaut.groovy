import com.google.gson.Gson;
import com.google.gson.GsonBuilder
import com.gmongo.GMongo
import org.apache.log4j.Logger;
import groovy.json.*

class DataJuggernaut{
	def logger = Logger.getLogger("org.wiperdog.scriptsupport.groovyrunner")
	Gson gson = new GsonBuilder().setPrettyPrinting().create()
	GroovyShell shell = new GroovyShell()
	def properties = MonitorJobConfigLoader.getProperties()
	def policyPath = properties.get(ResourceConstants.JOB_DIRECTORY) + "/policy"
	def final DEFAULT_COLLECTION = "policy_message"

	def judgeData(data){
		def mongo
		def ret // Format: [jobName:"aaaa", instanceName:"bbbb", message:[...]]
		File policyFile = getpolicyFile(data.sourceJob, data.istIid)
		File polParamFile = getParamFile(data.sourceJob, data.istIid)
		if(policyFile != null){
			def policyObj = shell.evaluate(policyFile)
			def binding = policyObj.getBinding()
			
			if(polParamFile != null && polParamFile.exists()){
				def slurper = new JsonSlurper()
				def params = slurper.parseText(polParamFile.getText())
				params.each{key, value->
					binding.setVariable(key, value)
				}
			}
			
			def policyClos = binding.getVariable("POLICY")
			ret = policyClos(data.data)
			ret['fetchedAt_bin'] = data.fetchedAt_bin
			if(ret.message != null && ret.message.size() > 0){
				def mapMongoDb = MongoDBConnection.getWiperdogConnection()
				mongo = mapMongoDb['gmongo']
				def db = mapMongoDb['db']
				if(db != null){
					def collection = db.getCollection(DEFAULT_COLLECTION)
					
					// With each message in policy's result, insert into mongo
					ret.message.each{
						def tempRec = [:]
						tempRec['jobName'] = ret['jobName']
						tempRec['istIid'] = ret['istIid']
						tempRec['fetchedAt_bin'] = ret['fetchedAt_bin']
						tempRec['level'] = it['level']
						tempRec['message'] = it['message']
						collection.insert(tempRec)
					}
					logger.info("--------Finished judgement data and save message into mongoDB.${DEFAULT_COLLECTION}------------")
				}else{
					println "Run Policy: Cannot connect to MongoDB!"
				}
			}else{
				logger.info("--------Nothing is wrong by now...--------")
			}
		}else {
			logger.info("--------There is no suitable policy file to judge data...--------")
		}
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
	
	def getParamFile(jobName, istIid){
		File paramsFile = new File("${policyPath}/${jobName}.${istIid}.params")
		
		// If params file of instance is not exists then get the common policy file
		if(!paramsFile.exists()){
			paramsFile = new File("${policyPath}/${jobName}.params")
			if(!paramsFile.exists()){
				paramsFile = null
			}
		}
		return paramsFile
	}
}
