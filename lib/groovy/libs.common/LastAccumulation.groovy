import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

class LastAccumulation{
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
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")

		try{
			def db = MongoDBConnection.getWiperdogConnection()['db']

			def jobs = []
			for(job in db.getCollectionNames().findAll({it != "system.indexes"})){
				jobs.add(job)
			}
			def dataSet = [:]
			for(jobName in jobs){
				def col = db[jobName]
				def getStorageSize = col.getStats().size
				def getCumCount = col.getStats().count
				def cumStatus = 'antena3'


				def nowDate = System.currentTimeMillis()
				def gridData = [:]
				
				for(document in col.find().sort(_id:-1).limit(1)){
					def unixTime = (document['fetchedAt_bin'] != null)? document['fetchedAt_bin'].toLong()*1000 : 0
					def diffTime = nowDate - unixTime
					def unixToDate = new Date(unixTime).format("yyyy/MM/dd HH:mm:ss")
					if(diffTime >= 28800000){
						cumStatus = 'antena2'
					}
					if(diffTime >= 57600000){
						cumStatus = 'antena1'
					}
					if(diffTime >= 86400000){
						cumStatus = 'antena0'
					}
					
					gridData['cumStatus'] = cumStatus
					gridData['jobName'] = jobName
					gridData['unixToDate'] = unixToDate
					gridData['getStorageSize'] = getStorageSize
					gridData['getCumCount'] = getCumCount
					gridData =  com.mongodb.util.JSON.serialize(gridData)
				}
				dataSet[jobName] = gridData
			}
			return dataSet
		} catch (Exception ex){
			ex.printStackTrace();
			return ex
		} finally {
		
		}
	}
}