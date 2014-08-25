import com.gmongo.GMongo;
import com.mongodb.DB
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;


class MonitoringDataService {
	// Temporary generate Mongo connection
	// When implement into WIperdog, please delete this connection
	def db = MongoDBConnection.getWiperdogConnection()['db']
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
		DataBusiness bu = DataBusiness.getInstance()
		bu.db = db
		def finalResult = [:]
		try{
			def result
			String job = request.getUrlDecodedHeader("job")
			String istIid = request.getUrlDecodedHeader("istIid")
			String fromDate = request.getRawHeader("fromDate") 
			String toDate = request.getRawHeader("toDate")
			int limit = Integer.valueOf(request.getRawHeader("limit"))
			
			if((fromDate != null && fromDate != "") || (toDate != null && toDate != "")){
				result = bu.getDataInPeriod(job,fromDate,toDate,limit,istIid)
			} else {
				result = bu.getDataAllFields(job,limit,istIid)
			}
			if(result != null && result.size() > 0){
				finalResult['data'] = result
				finalResult['listKey'] = []
				if(result[0].type == "Subtyped"){
					def dataSubtyped = DataToDrawChart.getDataToDrawSubtype(result)
					dataSubtyped.each{key, value->
						finalResult['listKey'].add(key)
						finalResult[key] = [:]
						finalResult[key]['Chart'] = [:]
						finalResult[key]['Chart'].line = DataToDrawChart.getDataToDrawLine(value)
						finalResult[key]['Chart'].bar = DataToDrawChart.getDataToDrawBar(value)
						finalResult[key]['Chart'].pie = DataToDrawChart.getDataToDrawPie(value)
						finalResult[key]['Chart'].area = DataToDrawChart.getDataToDrawArea(value)
					}
				} else if(result[0].type == "Store"){
					def sizeResult = result.size() - 1
					if(!result[sizeResult].KEYEXPR.getClass().toString().contains("List") 
						&& result[sizeResult].KEYEXPR != null 
						&& result[sizeResult].KEYEXPR._chart != null 
						&& result[sizeResult].KEYEXPR._chart.size() > 0) { 
						finalResult['Chart'] = [:]
						finalResult['Chart'].line = DataToDrawChart.getDataToDrawLine(result)
						finalResult['Chart'].bar = DataToDrawChart.getDataToDrawBar(result)
						finalResult['Chart'].pie = DataToDrawChart.getDataToDrawPie(result)
						finalResult['Chart'].area = DataToDrawChart.getDataToDrawArea(result)
					} else {
						finalResult['Chart'] = [:]
						finalResult['Chart'].line = []
						finalResult['Chart'].bar = []
						finalResult['Chart'].pie = []
						finalResult['Chart'].area = []
					}
				}
			} 
		}catch(ex){
			ex.printStackTrace()
		}
		return finalResult;
	}
}
