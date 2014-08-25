import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

class MenuGeneratorRestService {
	def shell = new GroovyShell()

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
			def listJobFromMongo = GenerateTreeMenu.getListJobFromMongo()
			def data2CreateMenu = GenerateTreeMenu.getData2CreateMenu(listJobFromMongo)
			responseData.tree = GenerateTreeMenu.getMenuItemsStr(data2CreateMenu['root'], data2CreateMenu['output'])
			responseData.mapIstIid = getMapJobistIid()
		}catch(ex){
			ex.printStackTrace()
		}
		return responseData
	}

	public String delete(Request request, Response response){
		return "delete_METHOD"
	}
	
	def getMapJobistIid(){
		def mongoDBConn = MongoDBConnection.getWiperdogConnection()
	    def collections = mongoDBConn.db.getCollectionNames()
		def mapJobIstIid = [:]
		try{
			def tmp = []
			def tmpSize
			def itemJob
			def itemIst
			for(collection in collections){
				tmp = collection.split("\\.")
				tmpSize = tmp.size()
				if(tmpSize >= 2){
					itemIst = tmp[tmpSize - 1]
					itemJob = collection - ("." + itemIst)
					
					if(mapJobIstIid[itemJob] == null){
						mapJobIstIid[itemJob] = []
					}
					mapJobIstIid[itemJob].add(itemIst)
				}
			}
		}catch(ex){
			ex.printStackTrace()
		}
		//mongoDBConn.close()
		return mapJobIstIid
	}
}
