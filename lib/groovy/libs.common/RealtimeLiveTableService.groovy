import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

class RealtimeLiveTableService {
	def dataBu = DataBusiness.getInstance()
	
	public def create(Request request, Response response){
		return "Create"
	}

	public String update(Request request, Response response){
		return "update_METHOD"
	}

	public def read(Request request, Response response){
		request.addHeader("Access-Control-Allow-Origin", "*")
		response.addHeader("Access-Control-Allow-Origin", "*")
		
		def job = request.getRawHeader("jobName")
		def istIid = request.getRawHeader("IstIid")
		
		if (job != null && istIid != null) {
			def result = dataBu.getDataAllFields(job, 50, istIid)
			def retData
			if(result[0] != null){
				def jobType = result[0]['type']
				if (jobType == "Store") {
					retData = []
					for(def i = result.size() - 1; i >= 0; i--){
						def res = result[i]
						for(def dat in res.data){
							def retObj = [:]
							//retObj = dat
							for(def key in dat.keySet()){
								if(dat[key] instanceof java.lang.String){
									//Replace for parseJSON
									retObj[key] = dat[key].replaceAll("\\\\","/") 
								} else {
									retObj[key] = dat[key]
								}
							}
						retObj['Fetch At'] = (new Date().parse("yyyyMMddHHmmssz", res.fetchAt)).format('yyyy/MM/dd HH:mm:ss z')
						retData.add(retObj)
						}
					}
					return retData
				} else {
					def listKey = []
					retData = [:]
					for(def dat in result[0]['data']){
						listKey.add(dat.key)
					}
					for(def key in listKey){
						retData[key] = []
					}
					for(def i = result.size() - 1; i >= 0; i--){
						def res = result[i]
						def fetchAtValue = (new Date().parse("yyyyMMddHHmmssz", res.fetchAt)).format('yyyy/MM/dd HH:mm:ss z')
						for(def key in listKey){
							def listData = res['data'][key]
							for(def data in listData){
								for(def datakey in data.keySet()){
									if(data[datakey] instanceof java.lang.String){
										data[datakey] = data[datakey].replaceAll("\\\\","/") 
									} else {
										data[datakey] = data[datakey]
									}
								}
								data['Fetch At'] = fetchAtValue
								retData[key].add(data)
							}
						}
					}
					return retData
				}
			}
		}
	}
	
	public String delete(Request request, Response response){
		return "delete_METHOD"
	}
}
