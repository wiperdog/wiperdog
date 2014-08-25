import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;
import com.google.gson.*

class RealtimeService {
	def shell = new GroovyShell()
	def dataBu = DataBusiness.getInstance()

	public def create(Request request, Response response){
		return "Create"
	}

	public String update(Request request, Response response){
		return "update_METHOD"
	}

	public def read(Request request, Response response){
		def responseData = [:]
		try{
			request.addHeader("Access-Control-Allow-Origin", "*")
			response.addHeader("Access-Control-Allow-Origin", "*")
			def job = request.getUrlDecodedHeader("jobname")
			def istIid = request.getUrlDecodedHeader("istIid")
			def from_date = request.getRawHeader('fromDate')
			def to_date = request.getRawHeader('toDate')
			def drawMethod = request.getRawHeader('drawMethod')
			def type = request.getRawHeader('type')
			def reqLimit = request.getRawHeader('limit')
			def limit
			if(reqLimit != null){
				limit = Integer.valueOf(reqLimit)
			} 
			
			if (job != null && istIid != null && from_date != null && drawMethod != null) {
				def result
				Gson gson = new GsonBuilder().setPrettyPrinting().create()
				if (drawMethod == "overWrite") {
					result = dataBu.getDataAllFields(job, 10, istIid)
					def finalData = []
					def finalMapData = [:]
					def typeChart = result[0].type
					if (result[0].type == "Subtyped") {
						def dataSubtype = DataToDrawChart.getDataToDrawSubtype(result)
						for(def key in dataSubtype.keySet()){
							def value = dataSubtype[key]
							finalMapData = dataBu.drawChart(key, value, typeChart)
							finalData.add(finalMapData)
							finalMapData = [:]
						}
					} else if (result[0].type == "Store") {
						def key = ""
						finalMapData = dataBu.drawChart(key, result, typeChart)
						finalData.add(finalMapData)
						finalMapData = [:]
					}

					// Response data
					if (finalData != []) {
						return finalData
					}
				} else if (drawMethod == "addPoint") {
					result = dataBu.getDataInPeriod(job, from_date, '', 10, istIid)
					def chartData = []
					if(result[0] != null){
						def jobType = result[0]['type']
						if (jobType == "Store") {
							chartData = dataBu.getAdditionData(result, '')
						} else if (jobType == "Subtyped") {
							def dataSubtype = DataToDrawChart.getDataToDrawSubtype(result) 
							for(key in dataSubtype.keySet()){
								def value = dataSubtype[key]
								def chartDataSubtyped = dataBu.getAdditionData(value, key)
								for(dat in chartDataSubtyped){
									chartData.add(dat)
								}
							}
						}
					}
					if (chartData == null) {
						return []
					}
					if (chartData != []) {
						return chartData
					}
				} else if(drawMethod == "updateChart"){
					if(from_date != "" || to_date != ""){
						result = dataBu.getDataInPeriod(job,from_date,to_date,limit,istIid)
					} else {
						result = dataBu.getDataAllFields(job,limit,istIid)
					}
					for(item in result){
						def newdate = new Date().parse("yyyyMMddHHmmssz", item['fetchAt'])
						item['fetchAt'] = newdate.format('yyyy/MM/dd HH:mm:ss z')
					}
					def dataUpdate = []
					def dataUpdateSubtyped = []
					if(result[0] != null){
						def jobType = result[0]['type']
						if(jobType == "Store") {
							if(type == 'pie'){
								dataUpdate = DataToDrawChart.getDataToDrawPie(result)
							} else if(type == 'line'){
								dataUpdate = DataToDrawChart.getDataToDrawLine(result)
							} else if(type == 'bar'){
								dataUpdate = DataToDrawChart.getDataToDrawBar(result)
							} else if(type == 'area'){
								dataUpdate = DataToDrawChart.getDataToDrawArea(result)		
							}
							if (dataUpdate != []) {
								return dataUpdate
							}
						} else if (jobType == "Subtyped") {
							dataUpdateSubtyped = DataToDrawChart.getDataToDrawSubtype(result)
							def finalMap = [:]
							for(key in dataUpdateSubtyped.keySet()){
								def value = dataUpdateSubtyped[key]
								if(type == 'pie'){
									dataUpdate = DataToDrawChart.getDataToDrawPie(value)
								} else if(type == 'line'){
									dataUpdate = DataToDrawChart.getDataToDrawLine(value)
								} else if(type == 'bar'){
									dataUpdate = DataToDrawChart.getDataToDrawBar(value)
								} else if(type == 'area'){
									dataUpdate = DataToDrawChart.getDataToDrawArea(value)		
								}
								finalMap[key] = dataUpdate
							}
							return finalMap
						}
					}
				}
			}
		}catch(ex){
			ex.printStackTrace()
		}
		return responseData
	}

	public String delete(Request request, Response response){
		return "delete_METHOD"
	}
	
	
}
