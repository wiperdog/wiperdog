import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

class MenuGeneratorRestService {
	def shell = new GroovyShell()

	public def create(Request request, Response response){
		return "Create"
	}

	public String update(Request request, Response response){
		println "update"
		return "update_METHOD"
	}

	public def read(Request request, Response response){
		def responseData
		try{
			request.addHeader("Access-Control-Allow-Origin", "*")
			response.addHeader("Access-Control-Allow-Origin", "*")
			def data2CreateMenu = GenerateTreeMenu.getData2CreateMenu(getCollections())
			responseData = GenerateTreeMenu.getMenuItemsStr(data2CreateMenu['root'], data2CreateMenu['output'])
		}catch(ex){
			ex.printStackTrace()
		}
		return responseData
	}

	public String delete(Request request, Response response){
		println "delete"
		return "delete_METHOD"
	}
	
	def getCollections(){
		def tmp = []
		def result = []
	    def mongoDBConn = MongoDBConnection.getWiperdogConnection()
	    def collections = mongoDBConn.db.getCollectionNames()
	    for(def collection in collections){
			if(collection.lastIndexOf(".") > 0 && collection.split("\\.").size() > 2){
				def jobname = collection.substring(0, collection.lastIndexOf("."))
				if(!result.contains(jobname)){
					result.add(jobname)
				}
			}else{
				if(!result.contains(collection)){
					result.add(collection)
				}
			}
		}
		return result
	}
}
