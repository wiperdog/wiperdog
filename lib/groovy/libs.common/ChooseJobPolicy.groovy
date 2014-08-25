

import com.gmongo.GMongo
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

import java.text.SimpleDateFormat

import groovy.json.*

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher
import java.util.regex.Pattern

import groovy.lang.GroovyShell

class ChooseJobPolicy {
	def dataBussines = DataBusiness.getInstance()
	def create(Request request, Response response){
		return "CREATE";
	}
	def update(Request request, Response response){
		return "UPDATE";
	}
	def delete(Request request, Response response){
		return "DELETE";
	}
	def read(Request request, Response response){
		try {
			//gmongoObject = services.MongoDBConnection
			//gmongoObject.getConnection(services.WiperdogConfig.getDataFromConfig())
			request.addHeader("Access-Control-Allow-Origin", "*")
			response.addHeader("Access-Control-Allow-Origin", "*")
	
			def dbConn = MongoDBConnection.getWiperdogConnection()['db']
			def lstJobMongo = dbConn.getCollectionNames()
			def slurper = new JsonSlurper()
			def builder
			response.setContentType('application/json')
			def returnData = [:]
			def jobName = request.getUrlDecodedHeader("jobName")
			if(jobName == null || jobName == "") {
				def mapNameAndType = [:]
				def type
				for(eCollec in lstJobMongo){
					type = dataBussines.getDataType(eCollec)
					mapNameAndType[eCollec] = type
				}
				returnData["status"] = "success"
				returnData["data"] = mapNameAndType
				returnData["message"] = ""
				return returnData
			} else {
				def mapFinalPolicy = [:]
				lstJobMongo.find {eCollection ->
					if(eCollection == jobName || eCollection.contains(jobName)) {
						// Sample Data
						def mapSampleData = [:]
						def mapSampleUnit = [:]
						def resultJob = dataBussines.getDataAllFields(eCollection)[0]
						def mapUnit = [:]
						try {
							mapUnit = resultJob.KEYEXPR._unit
						} catch (Exception ex) {
						}
						if(resultJob.type == "Store") {
							def cloneSampleData = [:]
							mapSampleData = resultJob.data[0]
							for(key in mapSampleData.keySet()){
								def value = mapSampleData[key]
								if(value.toString().contains("\\")){
									value = value.replaceAll("\\\\","/")
								}
								cloneSampleData[key] = value
								def checkExits = false
								for(keyUnit in mapUnit.keySet()){
									def valueUnit = mapUnit[keyUnit]
									if(key == keyUnit) {
										checkExits = true
										def keyResp = key + " (" + valueUnit + ")"
										mapSampleUnit[key] = valueUnit
									}
								}
								if(!checkExits) {
									mapSampleUnit[key] = ""
								}
							}
							mapFinalPolicy["SAMPLE"] = cloneSampleData
							mapFinalPolicy["UNIT"] = mapSampleUnit
							mapFinalPolicy["TYPE"] = resultJob.type
						} else if(resultJob.type == "Subtyped") {
							def lstKey = resultJob.data.keySet()
							for(eKey in lstKey){
								def tmpMapUnit = [:]
								mapSampleData[eKey] = resultJob.data[eKey][0]
								for(key in resultJob.data[eKey][0].keySet()){
									def value = resultJob.data[eKey][0][key]
									def checkExits = false
									for(keyUnit in mapUnit.keySet()){
										def valueUnit = mapUnit[keyUnit]
										if(key == keyUnit) {
											checkExits = true
											def keyResp = key + " (" + valueUnit + ")"
											tmpMapUnit[key] = valueUnit
										}
									}
									if(!checkExits) {
										tmpMapUnit[key] = ""
									}
								}
								mapSampleUnit[eKey] = tmpMapUnit
							}
							mapFinalPolicy["SAMPLE"] = mapSampleData
							mapFinalPolicy["UNIT"] = mapSampleUnit
							mapFinalPolicy["TYPE"] = resultJob.type
							//mapSampleData = resultJob.data[resultJob.data.keySet()[0]][0]
						}
					}
				}
				returnData["status"] = "success"
				returnData["data"] = mapFinalPolicy
				returnData["message"] = "Get data from MongoDB successfull !"
				return returnData
			}
		} catch (com.mongodb.MongoException ex){
			returnData = [:]
			returnData["status"] = "failed"
			returnData["data"] = ""
			returnData["message"] = "Can not get data from MongoDB !"
			return returnData
		} finally {
//			if(gmongoObject != null){
//				gmongoObject.closeConnection()
//			}
		}
	}

}